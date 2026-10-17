package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.PeakCaptainEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only real-client look at the Captain of the Peak Guard (gradlew -PgameTests runPeakCaptainTour): his
 * front, his back and his face, then a film from the side while he runs one way, turns and runs back, to see his cape,
 * sash and tassel swing (client/ClothLayer). Shots go to build/peak-captain-tour-run/screenshots (01-front.png,
 * 02-back.png, 03-face.png, 04-run/frame-NNN.png), the report to peak-captain-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class PeakCaptainTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile PeakCaptainEntity captain;
    private static double baseX, baseY, baseZ;
    private static final List<String> report = new ArrayList<>();
    /** The film: from RUN, LENGTH ticks, a frame every 2 ticks; he turns at TURN. */
    private static final int RUN = 100, TURN = RUN + 40, LENGTH = 100;
    /** The second phase's film: from CALL, CALL_LENGTH ticks, a frame every 2 ticks. */
    private static final int CALL = RUN + LENGTH + 30, CALL_LENGTH = 110;
    private static volatile net.minecraft.world.entity.monster.Zombie foe;

    private PeakCaptainTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.peakCaptainTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-peak-captain", GameType.CREATIVE, false, Difficulty.EASY, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-peak-captain", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            step(mc, server);
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Peak Captain tour crashed", e);
            finish(mc);
        }
    }

    /** Puts the (spectator) player at (x, eye, z) of the base, looking at (tx, target, tz). */
    private static void look(MinecraftServer server, double x, double eye, double z, double tx, double target, double tz) {
        server.execute(() -> {
            ServerPlayer self = server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
            float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
            float pitch = (float) -Math.toDegrees(Math.atan2(target - eye, Math.hypot(tx - x, tz - z)));
            self.teleportTo(server.overworld(), baseX + x, baseY + eye - self.getEyeHeight(), baseZ + z, yaw, pitch);
        });
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks >= CALL && ticks <= CALL + CALL_LENGTH && (ticks - CALL) % 2 == 0) capture(mc, String.format("05-waves/frame-%03d.png", (ticks - CALL) / 2));
        if (ticks >= RUN && ticks <= RUN + LENGTH && (ticks - RUN) % 2 == 0) capture(mc, String.format("04-run/frame-%03d.png", (ticks - RUN) / 2));
        // The run: he is led along the x axis, then back.
        if (ticks >= RUN - 4 && ticks <= RUN + LENGTH && captain != null) {
            double to = ticks < TURN ? 7 : -7;
            server.execute(() -> {
                captain.getNavigation().moveTo(baseX + to, baseY, baseZ, 1.4);
                // The camera keeps beside him, 5 blocks off, a little behind his run.
                double x = captain.getX() - baseX, z = captain.getZ() - baseZ;
                look(server, x, 1.5, z - 5, x, 1.0, z);
            });
        }
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = server.getPlayerList().getPlayer(mc.player.getUUID());
                    baseX = Math.floor(self.getX()) + 0.5;
                    baseY = self.getY();
                    baseZ = Math.floor(self.getZ()) + 0.5;
                    captain = MurimEntities.PEAK_CAPTAIN.get().spawn(level, BlockPos.containing(baseX, baseY, baseZ), MobSpawnType.COMMAND);
                    captain.moveTo(baseX, baseY, baseZ, 180, 0);
                    captain.setYHeadRot(180);
                    captain.setYBodyRot(180);
                    captain.restrictTo(BlockPos.containing(baseX, baseY, baseZ), 16);
                    captain.setNoAi(true);
                    self.getAbilities().flying = true;
                    self.onUpdateAbilities();
                });
                // In front of him (he faces -z): 4 blocks off, a little above his waist.
                look(server, 0.4, 1.5, -4.2, 0, 1.0, 0);
            }
            case 30 -> server.execute(() -> {
                check(captain != null && captain.isAlive(), "the captain is spawned");
                check(captain.getItemBySlot(EquipmentSlot.CHEST).is(MurimItems.PEAK_GUARD_LAMELLAR.get())
                        && captain.getMainHandItem().is(MurimItems.CRIMSON_DRAGON_SPEAR.get()), "he wears his armour and holds his spear");
            });
            case 44 -> capture(mc, "01-front.png");
            case 46 -> look(server, -0.6, 1.6, 4.4, 0, 1.0, 0);
            case 60 -> capture(mc, "02-back.png");
            case 62 -> look(server, 0.3, 1.75, -1.6, 0, 1.65, 0);
            case 76 -> capture(mc, "03-face.png");
            // From the side, far enough to see the whole run.
            case 80 -> {
                look(server, 0, 1.4, -11, 0, 1.0, 0);
                server.execute(() -> captain.setNoAi(false));
            }
            case RUN + LENGTH + 6 -> server.execute(() -> check(Math.abs(captain.getX() - baseX) > 2, "he ran (at " + (captain.getX() - baseX) + ")"));
            // 2. The second phase: a zombie brings him to half his health; he retreats and calls the Peak Guard.
            case CALL - 20 -> {
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    captain.getNavigation().stop();
                    captain.teleportTo(baseX, baseY, baseZ);
                    foe = net.minecraft.world.entity.EntityType.ZOMBIE.spawn(level, BlockPos.containing(baseX + 2.5, baseY, baseZ - 3), MobSpawnType.COMMAND);
                    foe.setNoAi(true);
                    foe.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
                    foe.setHealth(400);
                });
                look(server, -9, 4.5, -9, 0, 2.5, 0);
            }
            case CALL -> server.execute(() -> {
                captain.setHealth(captain.getMaxHealth() * PeakCaptainEntity.RETREAT_AT + 1);
                captain.invulnerableTime = 0;
                captain.hurt(server.overworld().damageSources().mobAttack(foe), 8);
                check(captain.phase() == PeakCaptainEntity.Phase.RETREAT, "at half his health he retreats");
            });
            case CALL + PeakCaptainEntity.FIRST_CALL + 4 -> server.execute(() -> check(captain.waveMen().size() == PeakCaptainEntity.WAVES[0] && captain.waveMen().get(0).skyFalling(), "the first wave drops from the sky"));
            case CALL + CALL_LENGTH -> server.execute(() -> {
                var men = captain.waveMen();
                check(!men.isEmpty() && men.stream().allMatch(m -> !m.skyFalling() && m.getHealth() == m.getMaxHealth()), "they land unhurt");
                check(men.stream().allMatch(m -> m.getTarget() == foe), "they fight his foe");
            });
            case CALL + CALL_LENGTH + 4 -> finish(mc);
            default -> { }
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Peak Captain tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        mc.execute(() -> {
            try {
                Files.writeString(mc.gameDirectory.toPath().resolve("peak-captain-tour-report.txt"),
                        String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
            } catch (Exception e) {
                LOGGER.error("Could not write the Peak Captain tour report", e);
            }
            LOGGER.info("Peak Captain tour finished: {}", failed ? "FAILED" : "PASSED");
            mc.stop();
        });
    }
}
