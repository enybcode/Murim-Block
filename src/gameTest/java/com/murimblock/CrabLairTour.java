package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.mob.CrabKingEntity;
import com.murimblock.world.structure.CrabLairBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only real-client look at the Crab King's lair, the Root Well (gradlew -PgameTests runCrabLairTour): in a
 * new Murim world, the game places the lair itself (CrabLairPlacement, under a Jiangnan Waterlands), then it is seen from the swamp
 * above its shaft, from high in the cavern, from its floor and beside the king. Shots go to
 * build/crab-lair-tour-run/screenshots, the report to crab-lair-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrabLairTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static int cx, cz, floor, surface;
    /** The tick the lair was found placed (0 until then). */
    private static volatile int ready;
    private static final List<String> report = new ArrayList<>();

    private CrabLairTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.crabLairTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-crab-lair", GameType.SPECTATOR, false, Difficulty.EASY, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-crab-lair", settings, new WorldOptions(7L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(),
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
            LOGGER.error("Crab lair tour crashed", e);
            finish(mc);
        }
    }

    /** Puts the camera at (x, y, z) from the lair's centre on its floor, looking at (tx, ty, tz) from there. */
    private static void look(MinecraftServer server, double x, double y, double z, double tx, double ty, double tz) {
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer self = server.getPlayerList().getPlayers().get(0);
            float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
            float pitch = (float) -Math.toDegrees(Math.atan2(ty - y, Math.hypot(tx - x, tz - z)));
            self.teleportTo(server.overworld(), cx + x, floor + y - self.getEyeHeight(), cz + z, yaw, pitch);
        });
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        int k = CrabLairBuilder.KING_X, kz = CrabLairBuilder.KING_Z, s = CrabLairBuilder.SHAFT_Z;
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                });
            }
            default -> { }
        }
        // The lair is placed by the game itself (CrabLairPlacement: after the sanctuary, under a Jiangnan Waterlands).
        if (ready == 0 && ticks > 20 && ticks % 20 == 0) server.execute(() -> {
            if (ready != 0) return;
            var centre = com.murimblock.world.structure.CrabLairPlacement.get(server.overworld()).centre();
            if (centre == null) {
                if (ticks > 20 * 60 * 6 && !finished) { check(false, "no lair placed after 6 minutes"); finish(mc); }
                return;
            }
            cx = centre.getX(); cz = centre.getZ(); floor = centre.getY();
            ServerLevel level = server.overworld();
            for (int x = (cx - 48) >> 4; x <= (cx + 48) >> 4; x++) for (int z = (cz - 48) >> 4; z <= (cz + 48) >> 4; z++) level.getChunk(x, z);
            surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx + CrabLairBuilder.SHAFT + 6, cz + CrabLairBuilder.SHAFT_Z);
            var biome = level.getBiome(new net.minecraft.core.BlockPos(cx, surface, cz + 30));
            check(true, "the lair was placed at " + centre + " (ticks " + ticks + "), surface " + surface);
            check(biome.is(com.murimblock.world.MurimBiomes.JIANGNAN_WATERLANDS), "it lies under the Jiangnan Waterlands (" + biome.unwrapKey().map(key -> key.location().toString()).orElse("?") + ")");
            ready = ticks;
        });
        if (ready == 0) return;
        switch (ticks - ready) {
            // The swamp above the shaft, then down into it.
            case 20 -> look(server, 16, surface - floor + 12, s + 18, 0, surface - floor - 6, s);
            case 100 -> capture(mc, "01-shaft-from-the-swamp.png");
            case 102 -> look(server, 0, 34, s - 1, 4, 0, s + 14);
            case 130 -> capture(mc, "02-down-the-shaft.png");
            // High in the cavern, the whole floor, the junk and the shaft's light.
            case 132 -> look(server, 30, 24, 30, -12, 6, -6);
            case 160 -> capture(mc, "03-the-cavern.png");
            case 162 -> look(server, -2, 22, 34, -10, 10, -14);
            case 190 -> capture(mc, "04-the-junk.png");
            // On the floor, and beside the king.
            case 192 -> look(server, 22, 2.6, 26, -14, 12, -10);
            case 220 -> capture(mc, "05-the-floor.png");
            case 222 -> look(server, k + 9, 5, kz + 9, k, 3, kz);
            case 240 -> server.execute(() -> check(server.overworld().getEntitiesOfClass(CrabKingEntity.class,
                    new AABB(cx - 50, floor - 2, cz - 50, cx + 50, floor + 50, cz + 50)).size() == 1, "the Crab King waits in his lair"));
            case 250 -> capture(mc, "06-the-king.png");
            // His mist fills the whole arena (CrabKingEntity.breatheMist): seen by the far wall, on the junk, high up.
            case 252 -> server.execute(() -> server.overworld().getEntitiesOfClass(CrabKingEntity.class,
                    new AABB(cx - 50, floor - 2, cz - 50, cx + 50, floor + 50, cz + 50)).forEach(king -> king.breatheMist(server.overworld())));
            case 254 -> look(server, -34, 2.6, -14, 0, 3, 0);
            case 300 -> server.execute(() -> check(server.overworld().getEntitiesOfClass(com.murimblock.mob.CrabMistEntity.class,
                    new AABB(cx - 4, floor - 2, cz - 4, cx + 4, floor + 4, cz + 4)).stream().anyMatch(m -> m.radius() > 40), "his mist fills the cavern"));
            case 310 -> capture(mc, "07-mist-by-the-far-wall.png");
            case 312 -> look(server, -2, 22, 34, -10, 10, -14);
            case 340 -> capture(mc, "08-mist-over-the-junk.png");
            case 342 -> look(server, 22, 2.6, 26, -14, 12, -10);
            case 370 -> capture(mc, "09-mist-on-the-floor.png");
            case 376 -> finish(mc);
            default -> { }
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Crab lair tour: {} {}", ok ? "ok" : "FAILED", what);
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
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("crab-lair-tour-report.txt"), String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the crab lair tour report", e);
        }
        mc.execute(mc::stop);
    }
}
