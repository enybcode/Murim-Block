package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.gui.SystemPage;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.page.CultivationPage;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
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
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Dev tool, off unless the {@code murimblock.guiShots} system property is set (run {@code gradlew runGuiShots}):
 * opens (or creates) a test world, shows each System page at GUI scale 3, saves a screenshot, then quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GuiScreenshots {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.guiShots");
    private static final String WORLD = "murim-gui-shots";
    private static final long SEED = 23102026L;
    /** Frames to wait in the world before the first page, so chunks are drawn behind the window. */
    private static final int WORLD_FRAMES = 120;
    /** Frames to wait on a page before the shot, so layout and blur have settled. */
    private static final int SETTLE_FRAMES = 20;
    private static final int GUI_SCALE = 3;

    private enum Step { TITLE, LOADING, PAGES, DONE }

    private record Shot(String name, SystemPage page, Supplier<SystemPageView> view) {
    }

    private static final List<Shot> SHOTS = List.of(
            new Shot("profile", SystemPage.PROFILE, SystemPage.PROFILE::createView),
            new Shot("techniques", SystemPage.TECHNIQUES, SystemPage.TECHNIQUES::createView),
            new Shot("cultivation", SystemPage.CULTIVATION, () -> new CultivationPage(false)),
            new Shot("cultivation_meridians", SystemPage.CULTIVATION, () -> new CultivationPage(true)),
            new Shot("info", SystemPage.INFO, SystemPage.INFO::createView));

    private static Step step = Step.TITLE;
    private static int page;
    private static int frames;

    private GuiScreenshots() {
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        switch (step) {
            case TITLE -> {
                if (mc.screen instanceof TitleScreen) {
                    mc.options.guiScale().set(GUI_SCALE);
                    mc.resizeDisplay();
                    openWorld(mc);
                    step = Step.LOADING;
                    frames = 0;
                }
            }
            case LOADING -> {
                if (mc.player == null || mc.screen != null) {
                    frames = 0;
                } else if (++frames >= WORLD_FRAMES) {
                    open(mc, 0);
                    step = Step.PAGES;
                }
            }
            case PAGES -> {
                if (++frames < SETTLE_FRAMES) {
                    return;
                }
                String name = "murim_" + SHOTS.get(page).name() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                        message -> Murimblock.LOGGER.info("GUI shot: {}", message.getString()));
                if (page + 1 < SHOTS.size()) {
                    open(mc, page + 1);
                } else {
                    // The game draws a few more frames while stopping: no second shot of the last page.
                    step = Step.DONE;
                    mc.stop();
                }
            }
            case DONE -> {
            }
        }
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> Murimblock.LOGGER.error("GUI shots: could not open {}", WORLD));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(SEED, false, false),
                WorldPresets::createNormalWorldDimensions, mc.screen);
    }

    private static void open(Minecraft mc, int index) {
        page = index;
        frames = 0;
        Shot shot = SHOTS.get(index);
        mc.setScreen(new SystemScreen(shot.page(), shot.view().get()));
    }
}
