package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimClientConfig;
import com.murimblock.client.gui.SystemButton;
import com.murimblock.client.gui.SystemPage;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.page.CultivationPage;
import com.murimblock.client.gui.page.InfoPage;
import com.murimblock.client.cultivation.BreathingHud;
import com.murimblock.client.cultivation.QiAura;
import com.murimblock.cultivation.BreathResult;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.Meditation;
import com.murimblock.registry.MurimItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
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
 * opens (or creates) a test world, shows each System page at GUI scale Auto, saves a screenshot, then quits.
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
    /** Minecraft's own "Auto" GUI scale (4 at 1080p): the System must look the same whatever it is. */
    private static final int GUI_SCALE = 0;

    private enum Step { TITLE, LOADING, MEDITATION, PAGES, DONE }

    /** Frames to let the lotus pose and the synced cultivation settle before the meditation shot. */
    private static final int MEDITATION_FRAMES = 90;
    /** Frames to land on the platform before sitting down. */
    private static final int PLATFORM_FRAMES = 30;
    /** Frames to let the full aura build up before its shot. */
    private static final int STRONG_AURA_FRAMES = 150;

    /** A page to shoot; {@code click}, if set, is the translation key of a button pressed before the shot. */
    private record Shot(String name, SystemPage page, Supplier<SystemPageView> view, String click) {
        Shot(String name, SystemPage page, Supplier<SystemPageView> view) {
            this(name, page, view, null);
        }
    }

    private static final List<Shot> SHOTS = List.of(
            new Shot("profile", SystemPage.PROFILE, SystemPage.PROFILE::createView),
            new Shot("techniques", SystemPage.TECHNIQUES, SystemPage.TECHNIQUES::createView),
            new Shot("cultivation", SystemPage.CULTIVATION, () -> new CultivationPage(false)),
            new Shot("cultivation_meridians", SystemPage.CULTIVATION, () -> new CultivationPage(true)),
            new Shot("info", SystemPage.INFO, () -> new InfoPage(InfoPage.QUESTS)),
            new Shot("info_log", SystemPage.INFO, () -> new InfoPage(InfoPage.LOG)),
            new Shot("info_guide", SystemPage.INFO, () -> guide(true)),
            new Shot("info_guide_hidden", SystemPage.INFO, () -> guide(true), "gui.murimblock.info.guide.hide"),
            new Shot("info_guide_back", SystemPage.INFO, () -> guide(false), "gui.murimblock.info.guide.show"));

    private static Step step = Step.TITLE;
    /** The player's Guide option, put back after the shots. */
    private static boolean showGuide = true;
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
                    standOnPlatform(mc);
                    step = Step.MEDITATION;
                    frames = 0;
                }
            }
            case MEDITATION -> {
                if (++frames == PLATFORM_FRAMES) {
                    startMeditation(mc);
                }
                // The breathing HUD in its four mockup states: neutral, good beat, miss, gauge full.
                if (frames == MEDITATION_FRAMES) {
                    shoot(mc, "murim_meditation.png");
                    BreathingHud.showFeedback(true);
                    // A good breath's Qi ring, given a few frames to fade in before the good-beat shot.
                    QiAura.burst(mc.player);
                } else if (frames == MEDITATION_FRAMES + 12) {
                    shoot(mc, "murim_breath_good.png");
                    BreathingHud.showFeedback(false);
                } else if (frames == MEDITATION_FRAMES + 16) {
                    shoot(mc, "murim_breath_miss.png");
                    fillGauge(mc);
                }
                if (frames < MEDITATION_FRAMES + 40) {
                    return;
                }
                if (frames < MEDITATION_FRAMES + 41) {
                    // A three-quarter view of the pose: turn the body, not the camera.
                    mc.player.setYBodyRot(mc.player.getYRot() + 90);
                    mc.player.yBodyRotO = mc.player.yBodyRot;
                    return;
                }
                if (frames == MEDITATION_FRAMES + 41) {
                    shoot(mc, "murim_breath_full.png");
                    mc.options.hideGui = true;
                    return;
                }
                if (frames < MEDITATION_FRAMES + 44) {
                    return;
                }
                if (frames == MEDITATION_FRAMES + 44) {
                    // The pose alone, without the HUD, with a beginner's Qi; then the aura of a great reserve.
                    shoot(mc, "murim_meditation_pose.png");
                    QiAura.previewQi = 10_000;
                    return;
                }
                if (frames < MEDITATION_FRAMES + 44 + STRONG_AURA_FRAMES) {
                    return;
                }
                shoot(mc, "murim_meditation_pose_strong.png");
                QiAura.previewQi = -1;
                mc.options.hideGui = false;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                showGuide = MurimClientConfig.showGuide();
                open(mc, 0);
                step = Step.PAGES;
            }
            case PAGES -> {
                if (++frames == SETTLE_FRAMES / 2 && SHOTS.get(page).click() != null) {
                    press(mc, SHOTS.get(page).click());
                }
                if (frames < SETTLE_FRAMES) {
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
                    MurimClientConfig.setShowGuide(showGuide);
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

    /**
     * Seats the player in lotus seen from the front, at Body Tempering 3 with 4 meridians and a full gauge, and has
     * them swallow one Meridian-Opening Pill, so the shots show the real synced cultivation.
     */
    /** A small stone platform high in the sky, so the lotus pose stands out against it. */
    private static void standOnPlatform(Minecraft mc) {
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            BlockPos center = BlockPos.containing(player.getX(), 200, player.getZ());
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 0, 2))) {
                player.serverLevel().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
            player.setYRot(180);
            player.setXRot(10);
        });
    }

    private static void shoot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> Murimblock.LOGGER.info("GUI shot: {}", message.getString()));
    }

    private static void fillGauge(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Cultivation c = Meditation.of(player);
            c.set(c.realm(), c.stage(), c.gauge(), c.qi(), c.maxQi(), c.meridians());
            Meditation.sync(player);
        });
    }

    private static void startMeditation(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Cultivation c = Meditation.of(player);
            c.set(0, 3, 0, c.maxQi(), c.maxQi(), 3);
            c.set(0, 3, c.gauge() * 0.62, c.maxQi(), c.maxQi(), 3);
            Meditation.start(player);
            for (int i = 0; i < 6; i++) {
                c.breathe(BreathResult.PERFECT, player.tickCount);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.MERIDIAN_PILL.get()));
            player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Murimblock.LOGGER.info("GUI shots: meditating {}, meridians {}", c.meditating(), c.meridians());
        });
    }

    /** The Info page on its Guide tab, with the Guide option on or off. */
    private static SystemPageView guide(boolean show) {
        MurimClientConfig.setShowGuide(show);
        return new InfoPage(InfoPage.GUIDE);
    }

    /** Presses the System button labelled with this key, as a click would, and logs whether it was found. */
    private static void press(Minecraft mc, String key) {
        String label = Component.translatable(key).getString();
        for (GuiEventListener child : List.copyOf(mc.screen.children())) {
            if (child instanceof SystemButton button && button.getMessage().getString().equals(label)) {
                button.onPress();
                Murimblock.LOGGER.info("GUI shots: pressed {}, showGuide = {}", label, MurimClientConfig.showGuide());
                return;
            }
        }
        Murimblock.LOGGER.error("GUI shots: no button {}", label);
    }

    private static void open(Minecraft mc, int index) {
        page = index;
        frames = 0;
        Shot shot = SHOTS.get(index);
        mc.setScreen(new SystemScreen(shot.page(), shot.view().get()));
    }
}
