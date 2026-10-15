package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.combat.CombatService;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
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
 * Development-only real-client check of the Celestial Peak Spear (gradlew -PgameTests runSpearTour): reading the manual,
 * the Techniques tab, then the Alliance spear in hand in combat mode, filmed from the side: an M1 at the point (Long
 * Reach), Piercing Dragon through two zombies in a line, Plum Blossom Thrusts, the Sweep of a Thousand Soldiers on zombies
 * all round, the three from the air, then the evolved ones. Report in spear-tour-report.txt, pictures in
 * build/spear-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SpearTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static final List<Zombie> zombies = new ArrayList<>();
    private static final List<String> report = new ArrayList<>();
    private static volatile float health;
    private static volatile double zombieZ;

    private SpearTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.spearTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-spear", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-spear", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Spear tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        // Minecraft only sends the player's position to the server while the player is the camera: with the filming
        // camera, the tour sends it itself.
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        if (camera != null && ticks >= 56 && ticks < 400 && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                ItemStack manual = new ItemStack(com.murimblock.martial.CelestialPeakItems.MANUAL.get());
                self.setItemInHand(InteractionHand.MAIN_HAND, manual);
                manual.use(level, self, InteractionHand.MAIN_HAND);
                check(MartialService.data(self).hasTechnique(MartialArts.PIERCING_DRAGON.id()), "reading the manual teaches Piercing Dragon");
                MartialService.unlock(self, MartialArts.PLUM_BLOSSOM_THRUSTS);
                MartialService.unlock(self, MartialArts.THOUSAND_SOLDIER_SWEEP);
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.ALLIANCE_SPEAR.get()));
                CombatService.setCombatMode(self, true);
                QiService.setQiMax(self, 100);
                QiService.setQi(self, 100);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
            });
            case 30 -> mc.setScreen(MurimProfileScreen.techniques());
            case 33 -> capture(mc, "00-library.png");
            case 34 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.LONG_REACH.id());
            case 40 -> capture(mc, "01-passive.png");
            case 41 -> ((MurimProfileScreen) mc.screen).selectForTests(MartialArts.PIERCING_DRAGON.id());
            case 46 -> capture(mc, "02-piercing-dragon-page.png");
            case 48 -> {
                mc.setScreen(null);
                server.execute(() -> zombie(server, 0, 3.4));
            }
            case 56 -> {
                mc.options.hideGui = false;
                yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
                yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
                start = mc.player.position();
                placeSideCamera(mc);
            }
            // A real M1 at the point: Epic Fight's spear thrust, 3.4 blocks out (Long Reach).
            case 60 -> KeyMapping.click(mc.options.keyAttack.getKey());
            case 80 -> server.execute(() -> check(zombies.getFirst().getHealth() < zombies.getFirst().getMaxHealth(), "the spear's M1 lands at the point"));
            // Key 1: Piercing Dragon, two zombies in its line.
            case 90 -> server.execute(() -> { reset(server, 0, 2.0); zombie(server, 0, 3.4); });
            case 94 -> press(mc, 0);
            case 96 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 99 -> check(clientPlaying(mc, MurimAnimations.CELESTIAL_PIERCING_DRAGON), "key 1 plays Piercing Dragon (playing: " + clientAnimation(mc) + ")");
            case 122 -> {
                double moved = mc.player.position().subtract(start).horizontalDistance();
                check(moved > 2.8 && moved < 5.0, String.format("the lunge carries the player about 4 blocks (moved %.2f)", moved));
                server.execute(() -> check(zombies.stream().allMatch(z -> z.getHealth() < z.getMaxHealth()), "the lunge goes through both zombies in its line"));
            }
            // Key 2: Plum Blossom Thrusts at the point.
            case 132 -> server.execute(() -> reset(server, 0, 2.8));
            case 136 -> press(mc, 1);
            case 138 -> mc.options.keyHotbarSlots[1].setDown(false);
            case 141 -> check(clientPlaying(mc, MurimAnimations.CELESTIAL_PLUM_BLOSSOM), "key 2 plays Plum Blossom Thrusts (playing: " + clientAnimation(mc) + ")");
            case 166 -> server.execute(() -> check(health - zombies.getFirst().getHealth() > 2.0, "the thrusts strike again and again (" + (health - zombies.getFirst().getHealth()) + ")"));
            // Key 3: the sweep, zombies all round.
            case 176 -> server.execute(() -> { reset(server, 0, 2.5); zombie(server, 0, -2.5); zombie(server, 2.5, 0); zombie(server, -2.5, 0); });
            case 180 -> press(mc, 2);
            case 182 -> mc.options.keyHotbarSlots[2].setDown(false);
            case 185 -> check(clientPlaying(mc, MurimAnimations.CELESTIAL_THOUSAND_SWEEP), "key 3 plays the Sweep of a Thousand Soldiers (playing: " + clientAnimation(mc) + ")");
            case 206 -> server.execute(() -> check(zombies.stream().allMatch(z -> z.getHealth() < z.getMaxHealth()), "the sweep strikes all round (" + zombies.stream().filter(z -> z.getHealth() < z.getMaxHealth()).count() + " of 4)"));
            // From the air: the dive, the thrusts down, the landing.
            case 216 -> server.execute(() -> { reset(server, 0, 2.0); ServerPlayer self = self(server); self.teleportTo(server.overworld(), self.getX(), self.getY() + 2.5, self.getZ(), 0, 0); });
            case 220 -> press(mc, 0);
            case 222 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 225 -> check(clientPlaying(mc, MurimAnimations.CELESTIAL_PIERCING_DIVE), "key 1 in the air plays the dive (playing: " + clientAnimation(mc) + ")");
            case 248 -> server.execute(() -> { reset(server, 0, 1.6); ServerPlayer self = self(server); self.teleportTo(server.overworld(), self.getX(), self.getY() + 2.5, self.getZ(), 0, 0); });
            case 252 -> press(mc, 1);
            case 254 -> mc.options.keyHotbarSlots[1].setDown(false);
            case 257 -> check(clientPlaying(mc, MurimAnimations.CELESTIAL_PLUM_AIR), "key 2 in the air plays the thrusts down (playing: " + clientAnimation(mc) + ")");
            case 280 -> server.execute(() -> { reset(server, 0, 2.0); ServerPlayer self = self(server); self.teleportTo(server.overworld(), self.getX(), self.getY() + 2.5, self.getZ(), 0, 0); });
            case 284 -> press(mc, 2);
            case 286 -> mc.options.keyHotbarSlots[2].setDown(false);
            case 289 -> check(clientPlaying(mc, MurimAnimations.CELESTIAL_SWEEP_LANDING), "key 3 in the air plays the landing (playing: " + clientAnimation(mc) + ")");
            // The evolved ones, played as the server would at mastery.
            case 306 -> server.execute(() -> { reset(server, 0, 2.0); EpicFightCapabilities.getEntityPatch(self(server), PlayerPatch.class).playAnimationSynchronized(MurimAnimations.CELESTIAL_PIERCING_DRAGON_LONG, 0.0F); });
            case 336 -> server.execute(() -> { reset(server, 0, 2.8); EpicFightCapabilities.getEntityPatch(self(server), PlayerPatch.class).playAnimationSynchronized(MurimAnimations.CELESTIAL_PLUM_RAIN, 0.0F); });
            case 366 -> server.execute(() -> { reset(server, 0, 3.0); EpicFightCapabilities.getEntityPatch(self(server), PlayerPatch.class).playAnimationSynchronized(MurimAnimations.CELESTIAL_TEN_THOUSAND_SWEEP, 0.0F); });
            // The same three from the player's eyes: the spear in view, nothing filling the screen.
            case 400 -> {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                server.execute(() -> reset(server, 0, 3.0));
            }
            case 404 -> capture(mc, "14-first-person-guard.png");
            case 406 -> press(mc, 0);
            case 408 -> mc.options.keyHotbarSlots[0].setDown(false);
            case 436 -> server.execute(() -> reset(server, 0, 2.8));
            case 440 -> press(mc, 1);
            case 442 -> mc.options.keyHotbarSlots[1].setDown(false);
            case 470 -> server.execute(() -> { reset(server, 0, 2.5); zombie(server, 2.5, 0); });
            case 474 -> press(mc, 2);
            case 476 -> mc.options.keyHotbarSlots[2].setDown(false);
            case 504 -> mc.setScreen(new MurimProfileScreen());
            case 510 -> capture(mc, "13-profile.png");
            case 514 -> {
                server.execute(() -> zombies.forEach(Zombie::discard));
                finish(mc);
            }
            default -> { }
        }
        record(mc, "03-m1", 58, 80, 1);
        record(mc, "04-piercing-dragon", 92, 122, 1);
        record(mc, "05-plum-blossom", 134, 166, 1);
        record(mc, "06-sweep", 178, 206, 1);
        record(mc, "07-dive", 218, 246, 1);
        record(mc, "08-plum-air", 250, 278, 1);
        record(mc, "09-sweep-landing", 282, 302, 1);
        record(mc, "10-soaring-dragon", 306, 334, 1);
        record(mc, "11-plum-rain", 336, 364, 1);
        record(mc, "12-ten-thousand", 366, 398, 1);
        record(mc, "15-first-person-dragon", 406, 432, 1);
        record(mc, "16-first-person-plum", 440, 468, 1);
        record(mc, "17-first-person-sweep", 474, 502, 1);
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
        MartialService.resetCooldowns(self);
        QiService.setQi(self, 100);
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
        if (Boolean.getBoolean("murimblock.spearTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        LOGGER.info("Spear tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("spear-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Spear tour report", e);
        }
        LOGGER.info("Spear tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
