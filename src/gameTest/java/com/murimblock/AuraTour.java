package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.qi.QiService;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
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
import org.slf4j.Logger;

/**
 * Opt-in real client look at the meditation's aura (the volume of flames, AuraClient, chosen by Enzo): a
 * player sits down at nightfall once per scene, each with another art (its colour and its touch) and another amount of
 * Qi (its size); each time the frames of the sitting's turning view are kept, for a GIF per scene. Never included in the player JAR.
 * Run: gradlew -PgameTests runAuraTour; frames in build/aura-tour-run/screenshots/<scene>/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class AuraTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Each sitting: the art its aura shows, and the Qi held (the aura's size). */
    private record Scene(String name, net.minecraft.resources.ResourceLocation art, int qi) { }
    private static final Scene[] SCENES = {
        new Scene("sanguine-200qi", com.murimblock.martial.MartialArts.SANGUINE_ART, 200),
        new Scene("sanguine-20000qi", com.murimblock.martial.MartialArts.SANGUINE_ART, 20000),
        new Scene("azure-2500qi", com.murimblock.martial.MartialArts.AZURE_CLOUD_SWORD, 2500),
        new Scene("thunder-5000qi", com.murimblock.martial.MartialArts.THUNDER_SABER, 5000),
        new Scene("seven-stars-5000qi", com.murimblock.martial.MartialArts.SEVEN_STARS_SWORD, 5000),
        new Scene("mountain-5000qi", com.murimblock.martial.MartialArts.MOUNTAIN_SHATTERING_FIST, 5000),
        new Scene("golden-bell-5000qi", com.murimblock.martial.MartialArts.GOLDEN_BELL, 5000),
        new Scene("majin-5000qi", com.murimblock.martial.MartialArts.MAJIN_FIST, 5000)};
    /** Ticks per style: sit, film the turning view, get up, wait. */
    private static final int EACH = 100, FIRST = 60;
    private static boolean creating, finished;
    private static int ticks;
    private static final List<String> report = new ArrayList<>();

    private AuraTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.auraTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("aura", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("aura-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            var server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.hideGui = true;
            mc.getToasts().clear();
            if (ticks == 20) server.execute(() -> {
                var level = server.overworld();
                level.setDayTime(13200);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++)
                    level.setBlockAndUpdate(new BlockPos(x, -61, z), (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
                ServerPlayer p = player(server);
                p.teleportTo(level, 0.5, -60, 0.5, 180, 0);

            });
            if (ticks < FIRST) return;
            int i = (ticks - FIRST) / EACH, t = (ticks - FIRST) % EACH;
            if (i >= SCENES.length) {
                Files.writeString(mc.gameDirectory.toPath().resolve("aura-tour-report.txt"), "PASSED; " + report + "\n");
                finished = true;
                mc.stop();
                return;
            }
            Scene scene = SCENES[i];
            if (t == 0) {
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    p.setOnGround(true);
                    // Learnt now, as main style: at equal mastery (none yet), the main style shows.
                    com.murimblock.martial.MartialService.learn(p, com.murimblock.martial.MartialArts.art(scene.art));
                    com.murimblock.martial.MartialService.setMainArt(p, scene.art);
                    QiService.setQiMax(p, scene.qi);
                    QiService.refillQi(p);
                    report.add(scene.name + " seated=" + MeditationService.start(p));
                });
            }
            if (t >= 8 && t < 56) capture(mc, scene.name + String.format("/frame-%03d.png", t - 8));
            if (t == 64) server.execute(() -> MeditationService.stop(player(server), false));
        } catch (Exception e) {
            LOGGER.error("Aura tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }
}
