package com.murimblock;

import com.murimblock.world.plant.MurimFlora;
import com.murimblock.world.plant.PlantCatalog;
import com.murimblock.world.plant.PlantCropBlock;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opt-in graphical check of the Murim's plants (docs/cultivation/plants.md) in a real client: the 43 wild plants, still
 * 3D blocks, in rows on grass (the water lily and the lotus on water); then a garden of planted herbs at their four
 * stages (the chrysanthemum's own models, the ginseng's grown by scale), moving; then at night, the spirit ones glowing.
 * Never included in the player JAR. Run: gradlew -PgameTests runGardenTour; pictures in build/garden-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GardenTour {
    private static boolean creating, finished;
    private static int ticks;
    private static Marker camera;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.gardenTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Garden", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("garden-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.hideGui = true;
            mc.getToasts().clear();
            MinecraftServer server = mc.getSingleplayerServer();
            if (ticks == 15) server.execute(() -> build(server.overworld()));
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                mc.options.fov().set(60);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            // The wild plants: from above the rows, then closer on each third.
            if (ticks == 42) place(12, -50.5, -9, 0, 38);
            if (ticks == 70) capture(mc, "01-wild-all.png");
            for (int part = 0; part < 3; part++) {
                if (ticks == 74 + part * 14) place(2 + part * 9.5, -55.5, -4.5, 0, 30);
                if (ticks == 84 + part * 14) capture(mc, String.format("02-wild-%d.png", part));
            }
            // The garden: four stages of the chrysanthemum and of the ginseng, and a spirit lingzhi.
            if (ticks == 125) place(4.5, -56.8, 16.5, 0, 32);
            if (ticks == 140) capture(mc, "03-garden.png");
            if (ticks == 146) capture(mc, "03-garden-b.png");
            if (ticks == 150) place(1.5, -58.6, 19.0, 0, 18);
            if (ticks == 165) capture(mc, "04-garden-close.png");
            if (ticks == 170) server.execute(() -> server.overworld().setDayTime(18000));
            if (ticks == 190) place(4.5, -56.8, 16.5, 0, 32);
            if (ticks == 205) capture(mc, "05-garden-night.png");
            if (ticks == 215) {
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Garden tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void build(ServerLevel level) {
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        var player = level.getServer().getPlayerList().getPlayers().get(0);
        player.teleportTo(level, 0.5, -60, -30, 0, 0);
        // The wild plants: 43 in rows of 11, a block apart, on grass (water under the floating ones).
        int i = 0;
        for (PlantCatalog.Species s : PlantCatalog.ALL) {
            BlockPos pos = new BlockPos(1 + (i % 11) * 2, -60, -2 + (i / 11) * 2);
            boolean water = s.soil() == PlantCatalog.Soil.WATER;
            level.setBlock(pos.below(), water ? Blocks.WATER.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            level.setBlock(pos, MurimFlora.wildBlock(s).defaultBlockState(), 3);
            i++;
        }
        // The garden, a row a species: chrysanthemum, ginseng, then lingzhi and gentian grown.
        String[] rows = {"chrysanthemum", "ginseng_root", "lingzhi", "peony"};
        for (int r = 0; r < rows.length; r++) {
            PlantCatalog.Species s = PlantCatalog.byId(rows[r]);
            for (int age = 0; age < 4; age++) {
                BlockPos pos = new BlockPos(1 + age * 2, -60, 22 + r * 2);
                level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
                level.setBlock(pos, MurimFlora.CROP.get().planted(s).setValue(PlantCropBlock.AGE, age), 3);
            }
        }
    }

    private static void place(double x, double y, double z, float yaw, float pitch) {
        camera.setPos(x, y, z);
        camera.xo = camera.xOld = x;
        camera.yo = camera.yOld = y;
        camera.zo = camera.zOld = z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(path.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> { try (image) { image.writeToFile(path); } catch (Exception e) { throw new RuntimeException(e); } });
    }
}
