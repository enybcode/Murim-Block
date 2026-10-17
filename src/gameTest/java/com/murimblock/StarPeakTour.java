package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.world.structure.StarPeakPlacement;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
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
 * Opt-in real client look at the Star-Plucking Peak in a fresh Murim world: waits for it to be built, then from far off
 * (south, east, and from below), from its summit looking out, and a player at Qi Liquidation meditating on it until the
 * stars give the Seven Stars Sword (the three minutes cut short). Never included in the player JAR.
 * Run: gradlew -PgameTests runStarPeakTour; pictures in build/star-peak-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class StarPeakTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks, waitTicks = -1;
    private static BlockPos summit;
    private static net.minecraft.world.entity.Marker camera;

    private StarPeakTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.starPeakTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("star-peak-tour", GameType.SPECTATOR, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("star-peak-tour-" + System.currentTimeMillis(), settings, new WorldOptions(7L, true, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            var server = mc.getSingleplayerServer();
            if (summit == null) {
                // Wait for the sanctuary, the lair and then the peak to be built (several minutes on a first start).
                if (++waitTicks == 20) {
                    mc.options.hideGui = true;
                    mc.options.renderDistance().set(16);
                    server.execute(() -> {
                        ServerLevel level = server.overworld();
                        level.setDayTime(1000);
                        level.setWeatherParameters(1000000, 0, false, false);
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                    });
                }
                if (waitTicks % 40 == 0) summit = StarPeakPlacement.get(server.overworld()).summit();
                if (waitTicks > 20 * 60 * 20) throw new IllegalStateException("The peak was not built in twenty minutes");
                if (summit != null) LOGGER.info("Star peak tour: the summit is at {}", summit);
                return;
            }
            ticks++;
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            int sx = summit.getX(), sy = summit.getY(), sz = summit.getZ();
            // From far off: south, then east, then from its foot looking up.
            if (ticks == 1) {
                mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.options.renderDistance().set(32);
                teleport(mc, sx + 0.5, 230, sz + 640.5, 180, -4);
            }
            if (ticks == 200) capture(mc, "01-far-south.png");
            if (ticks == 202) teleport(mc, sx + 560.5, 220, sz + 0.5, 90, -6);
            if (ticks == 400) capture(mc, "02-far-east.png");
            if (ticks == 402) teleport(mc, sx + 0.5, 200, sz + 250.5, 180, -18);
            if (ticks == 560) capture(mc, "03-from-below.png");
            // On the summit, looking out over the world, then down at the platform.
            if (ticks == 562) teleport(mc, sx + 0.5, sy + 2.6, sz + 0.5, 0, 12);
            if (ticks == 720) capture(mc, "04-summit-north.png");
            if (ticks == 722) teleport(mc, sx + 0.5, sy + 2.6, sz + 0.5, 180, 12);
            if (ticks == 780) capture(mc, "05-summit-south.png");
            if (ticks == 782) teleport(mc, sx + 5.5, sy + 5, sz + 5.5, 135, 40);
            if (ticks == 840) capture(mc, "06-platform.png");
            // A player at Qi Liquidation sits down on it; the stars answer (the three minutes cut short).
            if (ticks == 842) server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(server.overworld(), sx + 0.5, sy + 1, sz + 0.5, 180, 0);
                CultivationService.setCultivation(player, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
            });
            if (ticks == 870) {
                mc.options.hideGui = false;
                server.execute(() -> MeditationService.start(server.getPlayerList().getPlayers().get(0)));
                // Seen from a marker before it, the world below.
                camera = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityType.MARKER, mc.level);
                camera.moveTo(sx + 0.5, sy + 3.2, sz - 3.5, 0, 25);
                camera.xo = camera.xOld = camera.getX(); camera.yo = camera.yOld = camera.getY(); camera.zo = camera.zOld = camera.getZ();
                camera.yRotO = 0; camera.xRotO = 25;
                mc.setCameraEntity(camera);
            }
            if (ticks == 930) capture(mc, "07-meditating.png");
            if (ticks == 932) server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                StarPeakPlacement.meditating(player, StarPeakPlacement.MEDITATION_TICKS);
            });
            if (ticks == 950) capture(mc, "08-the-stars-answer.png");
            if (ticks == 960) {
                boolean given = StarPeakPlacement.given(server.getPlayerList().getPlayers().get(0));
                Files.writeString(mc.gameDirectory.toPath().resolve("star-peak-tour-report.txt"), (given ? "PASSED" : "FAILED") + "; summit=" + summit + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Star peak tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void teleport(Minecraft mc, double x, double y, double z, float yaw, float pitch) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(server.overworld(), x, y, z, yaw, pitch);
        });
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(name));
        }
    }
}
