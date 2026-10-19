package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Development-only real-client film of the Crescent Moon Halberd (gradlew -PgameTests runCrescentMoonTour): at night, the
 * halberd in hand, zombies before it; Mirror Image's reflection at a dodge, Twin Reflection, Shattered Mirror, Swap and
 * Hall of Mirrors filmed from the side, then the full moon filmed from afar (GeckoLib's moon, Photon's lights). Report in crescent-moon-tour-report.txt, frames in
 * build/crescent-moon-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrescentMoonTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static volatile float health;
    private static volatile double zombieZ;

    private CrescentMoonTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.crescentMoonTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-crescent-moon", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-crescent-moon", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            step(mc, mc.getSingleplayerServer());
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Crescent Moon tour crashed", e);
            finish(mc);
        }
    }

    private static final MartialArts.Technique[] ALL = {MartialArts.TWIN_REFLECTION, MartialArts.SHATTERED_MIRROR, MartialArts.SWAP,
            MartialArts.HALL_OF_MIRRORS, MartialArts.NIGHT_OF_THE_FULL_MOON};

    private static void cast(MinecraftServer server, int slot) {
        server.execute(() -> { var r = MartialService.castSlot(self(server), slot); check(r == MartialService.CastResult.CAST, "cast " + ALL[slot].id().getPath() + " (" + r + ")"); });
    }

    /** The player back at the start, ready, Qi full, a line of fresh zombies ahead. */
    /** Each technique on fresh ground: 24 blocks east of the last. */
    private static int ground;

    private static void line(MinecraftServer server, double... dz) {
        ground++;
        Vec3 at = start.add(24 * ground, 0, 0);
        server.execute(() -> {
            ServerPlayer self = self(server);
            zombies.forEach(Zombie::discard);
            zombies.clear();
            self.teleportTo(server.overworld(), at.x, at.y, at.z, 0, 0);
            self.setDeltaMovement(Vec3.ZERO);
            MartialService.clearCooldownsForTests(self);
            QiService.setQi(self, 2000);
            // At rest before the next technique: the last one, or its stun, is over.
            com.murimblock.integration.epicfight.EpicFightBridge.patch(self).playAnimationSynchronized(Animations.BIPED_IDLE, 0.0F);
            for (double z : dz) zombie(server, 0, z);
            here = at;
        });
    }

    /** Where the current technique is cast. */
    private static Vec3 here;

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        if (camera != null && ticks >= 56 && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(18000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                CultivationService.setCultivation(self, MartialArts.CRESCENT_MOON_REALM, CultivationStage.EARLY);
                new ItemStack(MurimItems.CRESCENT_MOON_HALBERD_SCROLL.get()).use(level, self, InteractionHand.MAIN_HAND);
                MartialData data = MartialService.data(self);
                for (int i = 0; i < ALL.length; i++) data = data.assign(i, ALL[i].id());
                MartialService.setForTests(self, data);
                MartialService.setMainArt(self, MartialArts.CRESCENT_MOON_HALBERD);
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.CRESCENT_MOON_HALBERD.get()));
                QiService.setQiMax(self, 2000);
                CombatService.setCombatMode(self, true);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
            });
            case 50 -> {
                mc.options.hideGui = true;
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                start = mc.player.position();
                line(server, 2.0, 9.0);
            }
            case 56 -> placeSideCamera(mc);
            // Mirror Image: a step aside, the reflection left where the cultivator stood.
            case 60 -> server.execute(() -> com.murimblock.integration.epicfight.EpicFightBridge.patch(self(server)).playAnimationSynchronized(Animations.BIPED_STEP_LEFT, 0.0F));
            case 80 -> server.execute(() -> check(com.murimblock.martial.CrescentMoonHalberd.reflections(self(server)).size() == 1, "the dodge left a reflection"));
            // Twin Reflection on one ahead.
            case 104 -> line(server, 3.0);
            case 108 -> placeSideCamera(mc);
            case 110 -> cast(server, 0);
            case 140 -> server.execute(() -> check(zombies.get(0).getHealth() < zombies.get(0).getMaxHealth(), "the twin struck from behind"));
            // Shattered Mirror: the reflection left by the twin bursts among two zombies.
            case 150 -> server.execute(() -> { zombie(server, 1.5, 4.5); zombie(server, -1.5, 4.5); });
            case 156 -> cast(server, 1);
            case 190 -> server.execute(() -> check(zombies.stream().filter(z -> z.getHealth() < z.getMaxHealth()).count() >= 2, "the shards cut round the reflection"));
            // Swap: a step aside leaves a reflection; it is sent off, then the places change.
            case 200 -> line(server, 6.0);
            case 204 -> placeSideCamera(mc);
            case 206 -> server.execute(() -> com.murimblock.integration.epicfight.EpicFightBridge.patch(self(server)).playAnimationSynchronized(Animations.BIPED_STEP_BACKWARD, 0.0F));
            case 222 -> server.execute(() -> { var list = com.murimblock.martial.CrescentMoonHalberd.reflections(self(server)); if (!list.isEmpty()) { var r = list.get(list.size() - 1); r.moveTo(r.getX() + 4, r.getY(), r.getZ() + 3); } });
            case 228 -> cast(server, 2);
            // Hall of Mirrors among four zombies.
            case 250 -> line(server, 3.5, 6.0);
            case 252 -> server.execute(() -> { zombie(server, 3.5, 2.5); zombie(server, -3.5, 3.0); });
            case 256 -> placeSideCamera(mc);
            case 258 -> cast(server, 3);
            case 330 -> server.execute(() -> check(zombies.stream().filter(z -> z.getHealth() < z.getMaxHealth()).count() >= 3, "the hall struck its foes"));
            // The full moon, filmed from afar.
            case 350 -> line(server, 6, 8, 10);
            case 352 -> server.execute(() -> { zombie(server, 5, 6); zombie(server, -6, 7); });
            case 354 -> placeFarCamera(mc);
            case 356 -> server.execute(() -> { var r = MartialService.castSlot(self(server), 4); check(r == MartialService.CastResult.CAST, "cast the full moon (" + r + ")"); });
            case 520 -> server.execute(() -> check(zombies.stream().filter(z -> !z.isAlive() || z.getHealth() < z.getMaxHealth()).count() >= 3, "the moon's beams struck"));
            case 540 -> {
                server.execute(() -> zombies.forEach(Zombie::discard));
                finish(mc);
            }
            default -> { }
        }
        record(mc, "01-mirror-image", 60, 96, 2);
        record(mc, "02-twin-reflection", 110, 146, 2);
        record(mc, "03-shattered-mirror", 156, 190, 2);
        record(mc, "04-swap", 206, 246, 2);
        record(mc, "05-hall-of-mirrors", 258, 340, 2);
        record(mc, "06-full-moon", 356, 520, 3);
    }

    /** A camera far to the side and up, for the ultimate. */
    private static void placeFarCamera(Minecraft mc) {
        Vec3 at = here.add(0, 0, 3);
        Vec3 eye = at.add(26, 8, -10);
        Vec3 d = at.add(0, 7, 0).subtract(eye);
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        camera.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
    }

    /** A sturdy zombie without AI, `dx` to the side and `dz` in front of the player. */
    private static void zombie(MinecraftServer server, double dx, double dz) {
        ServerPlayer self = self(server);
        Zombie z = EntityType.ZOMBIE.create(server.overworld());
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        z.setHealth(200);
        z.moveTo(self.getX() + dx, self.getY(), self.getZ() + dz, dz >= 0 ? 180 : 0, 0);
        z.setNoAi(true);
        z.setPersistenceRequired();
        // A helmet: no burning in the sun.
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        server.overworld().addFreshEntity(z);
        zombies.add(z);
    }

    /** The player back at the start, facing south, techniques ready; one fresh zombie `dz` ahead. */
    private static void reset(MinecraftServer server, double dx, double dz) {
        ServerPlayer self = self(server);
        zombies.forEach(Zombie::discard);
        zombies.clear();
        self.teleportTo(server.overworld(), start.x, start.y, start.z, 0, 0);
        self.setDeltaMovement(Vec3.ZERO);
        zombie(server, dx, dz);
        health = 200;
    }

    /** Presses a combat bar key (its release, two ticks later, casts). */
    private static void press(Minecraft mc, int slot) {
        mc.options.keyHotbarSlots[slot].setDown(true);
        KeyMapping.click(mc.options.keyHotbarSlots[slot].getKey());
    }

    /** The side camera, beside where the player stands. */
    private static void placeSideCamera(Minecraft mc) {
        Vec3 at = mc.player.position();
        Vec3 wanted = at.add(7.0, 2.2, 3.5);
        Vec3 d = at.add(0, 0.9, 3.5).subtract(wanted);
        Vec3 eye = wanted.add(d.normalize().scale(4));
        if (camera == null) camera = new Marker(EntityType.MARKER, mc.level);
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        camera.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        mc.setCameraEntity(camera);
    }

    private static String clientAnimation(Minecraft mc) {
        var patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
        var main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        return main == null || main.getRealAnimation() == null ? "nothing" : String.valueOf(main.getRealAnimation().get());
    }

    private static boolean clientPlaying(Minecraft mc, AnimationAccessor<?> animation) {
        var patch = EpicFightCapabilities.getEntityPatch(mc.player, PlayerPatch.class);
        if (patch == null) return false;
        var main = patch.getAnimator().getPlayerFor(null);
        return main != null && main.getRealAnimation() != null && main.getRealAnimation().get() == animation.get();
    }

    /** No first-person hand while the side camera films. */
    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (Boolean.getBoolean("murimblock.crescentMoonTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void record(Minecraft mc, String name, int from, int to, int every) throws Exception {
        if (ticks >= from && ticks <= to && (ticks - from) % every == 0) capture(mc, String.format("%s/frame-%03d.png", name, (ticks - from) / every));
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> {
            try (image) { image.writeToFile(file); } catch (Exception e) { LOGGER.error("Frame write failed", e); }
        });
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Crescent Moon tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("crescent-moon-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Crescent Moon tour report", e);
        }
        LOGGER.info("Crescent Moon tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
