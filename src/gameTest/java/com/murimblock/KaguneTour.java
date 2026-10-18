package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.martial.KaguneState;
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
 * Development-only real-client look at the Devouring Demon Art's kagune on a player (KaguneState, client/KaguneOverlay):
 * in a new flat world, the camera's own player in front view, then from behind, with the four tentacles, through the
 * transformation, then with the two centipedes and the mask. Run with the weapon tour's client:
 * MURIM_KAGUNE=1 gradlew -PgameTests runWeaponTour. Shots go to build/weapon-tour-run/screenshots/kagune.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KaguneTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static final List<String> report = new ArrayList<>();

    private KaguneTour() { }

    public static boolean wanted() {
        return "1".equals(System.getenv("MURIM_KAGUNE"));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.weaponTour") || !wanted()) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    String name = "murim-kagune-" + System.currentTimeMillis();
                    LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(7L, false, false),
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
            LOGGER.error("Kagune tour crashed", e);
            finish(mc);
        }
    }

    private static void set(MinecraftServer server, KaguneState.Form form) {
        server.execute(() -> {
            ServerPlayer self = server.getPlayerList().getPlayers().get(0);
            KaguneState.set(self, form);
        });
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        // MURIM_KAGUNE_FILM=1: a film instead, a frame every other tick, from the front: the tentacles, the
        // transformation at 100, then the centipedes.
        // MURIM_KAGUNE_FILM=beast: the demon form on all fours, from the front then from behind: still, walking, running.
        if ("beast".equals(System.getenv("MURIM_KAGUNE_FILM")) && ticks > 20) {
            var up = mc.options.keyUp.getKey();
            var sprint = mc.options.keySprint.getKey();
            if (ticks == 40) { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); set(server, KaguneState.Form.DEMON); }
            if (ticks == 100) net.minecraft.client.KeyMapping.set(up, true);
            if (ticks == 160) net.minecraft.client.KeyMapping.set(sprint, true);
            if (ticks == 200) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (ticks == 240) { net.minecraft.client.KeyMapping.set(sprint, false); net.minecraft.client.KeyMapping.set(up, false); }
            // Then it turns on the spot, left then right: the kagune swings like a tail.
            if (ticks > 244 && ticks <= 300) { mc.player.setYRot(mc.player.getYRot() + (ticks <= 272 ? 9 : -9)); mc.player.setYBodyRot(mc.player.getYRot()); }
            if (ticks >= 60 && ticks <= 320 && ticks % 2 == 0) capture(mc, String.format("beast/%03d.png", (ticks - 60) / 2));
            if (ticks == 150) check(com.murimblock.client.DemonBeastClient.motion(mc.player) == com.murimblock.integration.epicfight.MurimAnimations.Motions.DEMON_BEAST_WALK,
                    "walking in the demon form is the beast's walk: " + com.murimblock.client.DemonBeastClient.motion(mc.player));
            if (ticks == 322) finish(mc);
            return;
        }
        if ("1".equals(System.getenv("MURIM_KAGUNE_FILM")) && ticks > 20) {
            if (ticks == 40) { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); set(server, KaguneState.Form.HUNGER); }
            if (ticks == 108) set(server, KaguneState.Form.TRANSFORM);
            if (ticks >= 60 && ticks <= 260 && ticks % 2 == 0) capture(mc, String.format("film/%03d.png", (ticks - 60) / 2));
            if (ticks == 262) { check(true, "filmed"); finish(mc); }
            return;
        }
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                server.execute(() -> {
                    var level = server.overworld();
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = server.getPlayerList().getPlayers().get(0);
                    self.getAbilities().flying = false;
                    self.onUpdateAbilities();
                    self.teleportTo(level, 0.5, -60, 0.5, 0, 0);
                });
            }
            case 40 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); set(server, KaguneState.Form.HUNGER); }
            case 70 -> capture(mc, "01-hunger-front-a.png");
            case 80 -> capture(mc, "02-hunger-front-b.png");
            case 82 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 100 -> capture(mc, "03-hunger-back.png");
            case 102 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 110 -> set(server, KaguneState.Form.TRANSFORM);
            case 116, 122, 128, 134, 140, 146, 152, 158, 164 -> capture(mc, String.format("04-transform-%03d.png", ticks - 110));
            case 190 -> capture(mc, "05-demon-front.png");
            case 192 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 210 -> capture(mc, "06-demon-back.png");
            case 212 -> {
                check(KaguneState.of(mc.player).form() == KaguneState.Form.TRANSFORM, "the client sees the kagune's form: " + KaguneState.of(mc.player).form());
                finish(mc);
            }
            default -> { }
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        if (!ok) failed = true;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve("kagune").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("weapon-tour-report.txt"), String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Kagune tour: could not write its report", e);
        }
        mc.stop();
    }
}
