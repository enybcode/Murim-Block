package com.murimblock;

import com.murimblock.client.MurimMapScreen;
import com.murimblock.client.gui.MurimProfileScreen;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
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

/**
 * Opt-in graphical check of the Murim map in a new Murim world (seed 42): the Info page and its Murim Map button, then
 * the map itself at full screen. Never included in the player JAR.
 * Run: gradlew -PgameTests runMurimMapTour; pictures in build/murim-map-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MurimMapTour {
    private static boolean creating, finished;
    private static int ticks;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.murimMapTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Murim Map", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-map-" + System.currentTimeMillis(), settings, new WorldOptions(42L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            switch (ticks) {
                case 60 -> mc.setScreen(MurimProfileScreen.info());
                case 70 -> capture(mc, "01-info.png");
                case 75 -> MurimMapScreen.open();
                case 100 -> capture(mc, "02-map.png");
                case 110 -> {
                    String report = MurimMapScreen.ready() ? "PASSED" : "FAILED: no map came";
                    Files.writeString(mc.gameDirectory.toPath().resolve("murim-map-tour-report.txt"), report + "\n");
                    finished = true;
                    mc.stop();
                }
                default -> { }
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Murim map tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(path.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> { try (image) { image.writeToFile(path); } catch (Exception e) { throw new RuntimeException(e); } });
    }
}
