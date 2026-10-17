package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.item.MurimItems;
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
 * Development-only real-client film of the weapons' swing trails (gradlew -PgameTests runTrailTour): at night, each
 * Murim weapon in hand, one real M1 filmed from the side (assets/murimblock/item_skins). Frames in
 * build/trail-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TrailTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static volatile float health;
    private static volatile double zombieZ;

    private TrailTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.trailTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-trail", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-trail", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Trail tour crashed", e);
            finish(mc);
        }
    }

    /** The weapons filmed, one swing each. */
    /** A weapon, and the Murim attack it plays (null: a real M1, Epic Fight's or the weapon's own). */
    private record Swing(java.util.function.Supplier<net.minecraft.world.item.Item> item, java.util.function.Supplier<AnimationAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation>> attack, String name) { }
    private static final List<Swing> WEAPONS = List.of(
            new Swing(MurimItems.BANDIT_DAO::get, null, "bandit_dao"), new Swing(MurimItems.SNOW_PLUM_JIAN::get, null, "snow_plum_jian"),
            new Swing(MurimItems.AZURE_CLOUD_SWORD::get, null, "azure_cloud_sword"), new Swing(MurimItems.CHIEF_GREAT_AXE::get, null, "chief_great_axe"),
            new Swing(MurimItems.ALLIANCE_SPEAR::get, null, "alliance_spear"), new Swing(MurimItems.CRIMSON_DRAGON_SPEAR::get, null, "crimson_dragon_spear"),
            new Swing(MurimItems.BISHOU::get, () -> Animations.DAGGER_AUTO2, "bishou"), new Swing(MurimItems.FLYING_KNIFE::get, () -> Animations.DAGGER_AUTO3, "flying_knife"),
            new Swing(MurimItems.IRON_FAN::get, null, "iron_fan"), new Swing(MurimItems.CHAIN_WHIP::get, null, "chain_whip"),
            new Swing(MurimItems.AZURE_CLOUD_SWORD::get, () -> com.murimblock.integration.epicfight.MurimAnimations.DRIFTING_CUT, "azure_drifting_cut"),
            new Swing(MurimItems.AZURE_CLOUD_SWORD::get, () -> com.murimblock.integration.epicfight.MurimAnimations.HEAVEN_SPLITTING_CRESCENT, "azure_crescent"),
            new Swing(MurimItems.ALLIANCE_SPEAR::get, () -> com.murimblock.integration.epicfight.MurimAnimations.CELESTIAL_THOUSAND_SWEEP, "spear_sweep"),
            new Swing(MurimItems.CHIEF_GREAT_AXE::get, () -> com.murimblock.integration.epicfight.MurimAnimations.CHIEF_CLEAVE, "chief_cleave"));
    private static final int FIRST = 60, EACH = 40;

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        if (camera != null && ticks >= 56 && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        if (ticks == 20) server.execute(() -> {
            ServerLevel level = server.overworld();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            ServerPlayer self = self(server);
            CombatService.setCombatMode(self, true);
            self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
        });
        if (ticks == 56) {
            mc.options.hideGui = true;
            yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
            yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
            start = mc.player.position();
            placeSideCamera(mc);
        }
        int end = FIRST + WEAPONS.size() * EACH;
        if (ticks >= FIRST - 4 && ticks < end) {
            int i = (ticks - FIRST + 4) / EACH, t = (ticks - FIRST + 4) % EACH;
            Swing swing = WEAPONS.get(i);
            var item = swing.item().get();
            String name = swing.name();
            if (t == 0) server.execute(() -> {
                ServerPlayer self = self(server);
                self.teleportTo(server.overworld(), start.x, start.y, start.z, 0, 0);
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            });
            if (t == 4 && swing.attack() == null) KeyMapping.click(mc.options.keyAttack.getKey());
            if (t == 4 && swing.attack() != null) server.execute(() -> EpicFightCapabilities.getEntityPatch(self(server), PlayerPatch.class).playAnimationSynchronized(swing.attack().get(), 0.0F));
            if (t >= 4 && t <= 34) capture(mc, String.format("%02d-%s/frame-%03d.png", i, name, t - 4));
        }
        if (ticks == end + 4) finish(mc);
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
        Vec3 wanted = at.add(6.5, 1.8, 1.0);
        Vec3 d = at.add(0, 0.9, 1.0).subtract(wanted);
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
        if (Boolean.getBoolean("murimblock.trailTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        LOGGER.info("Trail tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("trail-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Trail tour report", e);
        }
        LOGGER.info("Trail tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
