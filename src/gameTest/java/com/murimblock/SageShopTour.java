package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.GuideBookClient;
import com.murimblock.client.ScrollShopScreen;
import com.murimblock.dialogue.DialogueService;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.GoldenDragonSageEntity;
import com.murimblock.mob.GoldenDragonShop;
import com.murimblock.mob.MurimEntities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
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
 * Development-only real-client check of the Golden Dragon's scroll of wares (gradlew -PgameTests runSageShopTour): the
 * player asks him for his wares, the scroll unrolls, a guide is bought through its seal, the wares are scrolled, then
 * the guide is read. Report in sage-shop-tour-report.txt, pictures in build/sage-shop-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SageShopTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static GoldenDragonSageEntity sage;
    private static final List<String> report = new ArrayList<>();

    private SageShopTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.sageShopTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-sage-shop", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-sage-shop", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Sage shop tour crashed", e);
            finish(mc);
        }
    }

    private static int index(net.minecraft.world.item.Item item) {
        for (int i = 0; i < GoldenDragonShop.wares().size(); i++) if (GoldenDragonShop.wares().get(i).item().get().is(item)) return i;
        return -1;
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                ServerPlayer self = self(server);
                self.getInventory().clearContent();
                self.getInventory().add(new ItemStack(MurimItems.GOLD_YUANBAO.get(), 41));
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 10);
                sage = MurimEntities.GOLDEN_DRAGON_SAGE.get().spawn(level, BlockPos.containing(self.getX(), self.getY(), self.getZ() + 3), MobSpawnType.COMMAND);
            });
            // He is asked for his wares: the conversation, then the scroll.
            case 40 -> server.execute(() -> sage.interact(self(server), InteractionHand.MAIN_HAND));
            case 50 -> server.execute(() -> DialogueService.choose(self(server), sage.getId(), "wares"));
            case 55 -> capture(mc, "01-unrolling.png");
            case 70 -> {
                check(ScrollShopScreen.current() != null && ScrollShopScreen.current().wares().size() == GoldenDragonShop.wares().size(),
                        "the scroll unrolls with every ware (" + (ScrollShopScreen.current() == null ? "no scroll" : ScrollShopScreen.current().wares().size() + " wares") + ")");
                capture(mc, "02-scroll.png");
            }
            case 72 -> { if (mc.screen instanceof ScrollShopScreen s) s.buyForTests(index(MurimItems.LONG_FIST_GUIDE.get())); }
            case 82 -> {
                check(mc.player.getInventory().countItem(MurimItems.LONG_FIST_GUIDE.get()) == 1
                        && mc.player.getInventory().countItem(MurimItems.GOLD_YUANBAO.get()) == 38, "the Long Fist guide is bought for 3 yuanbao");
                capture(mc, "03-bought.png");
            }
            case 84 -> { if (mc.screen instanceof ScrollShopScreen s) s.mouseScrolled(0, 0, 0, -1); }
            case 86 -> { if (mc.screen instanceof ScrollShopScreen s) s.mouseScrolled(0, 0, 0, -1); }
            case 88 -> { if (mc.screen instanceof ScrollShopScreen s) s.mouseScrolled(0, 0, 0, -1); }
            case 92 -> capture(mc, "04-scrolled.png");
            // At a larger GUI scale too.
            case 94 -> { mc.options.guiScale().set(2); mc.resizeDisplay(); }
            case 100 -> capture(mc, "05-scroll-gui-2.png");
            case 104 -> {
                mc.options.guiScale().set(0);
                mc.resizeDisplay();
                mc.setScreen(null);
                GuideBookClient.open(((com.murimblock.item.MartialGuideItem) MurimItems.LONG_FIST_GUIDE.get()).pages());
            }
            case 112 -> capture(mc, "06-guide.png");
            case 114 -> { if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen) mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN, 0, 0); }
            case 118 -> capture(mc, "07-guide-page-2.png");
            case 130 -> {
                server.execute(() -> sage.discard());
                finish(mc);
            }
            default -> { }
        }
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
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
        LOGGER.info("Sage shop tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("sage-shop-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Sage shop tour report", e);
        }
        LOGGER.info("Sage shop tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
