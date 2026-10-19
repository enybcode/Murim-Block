package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.cultivation.QiSense;
import com.murimblock.qi.QiService;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
 * Opt-in real client look at running on water (WaterWalking): a player at Qi Liquidation sprints from the bank across a
 * pool, the real keys held, seen from behind; then slows to a walk and sinks. Never included in the player JAR.
 * Run: gradlew -PgameTests runWaterWalkTour; frames in build/water-walk-tour-run/screenshots/water/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class WaterWalkTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;
    private static String report = "";

    private WaterWalkTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.waterWalkTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("water-walk", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("water-walk-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
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
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                // A pool two deep, 28 long, run across along +x.
                for (int x = 0; x <= 28; x++) for (int z = -5; z <= 5; z++) for (int y = -62; y <= -61; y++)
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, -4.5, -60, 0.5, -90, 15);
                CultivationService.setCultivation(p, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
                QiService.setQiMax(p, 1500);
                QiService.refillQi(p);
            });
            if (ticks == 40) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            // Sprinting from the bank across the water, then slowing to a walk: it sinks.
            if (ticks >= 50 && ticks < 120) {
                mc.player.setYRot(-90);
                mc.options.keyUp.setDown(true);
                mc.options.keySprint.setDown(true);
                mc.player.setSprinting(true);
            }
            if (ticks == 120) {
                mc.options.keySprint.setDown(false);
                mc.player.setSprinting(false);
            }
            if (ticks == 135) mc.options.keyUp.setDown(false);
            if (ticks >= 50 && ticks < 150 && ticks % 2 == 0) capture(mc, String.format("water/frame-%03d.png", (ticks - 50) / 2));
            if (ticks == 100) report = "x=" + String.format("%.1f", mc.player.getX()) + " y=" + String.format("%.2f", mc.player.getY()) + " inWater=" + mc.player.isInWater();
            if (ticks == 155) {
                Files.writeString(mc.gameDirectory.toPath().resolve("water-walk-tour-report.txt"), "PASSED; at 100: " + report
                        + "; at end: y=" + String.format("%.2f", mc.player.getY()) + " inWater=" + mc.player.isInWater() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Water walk tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x, double z) {
        Mob mob = type.create(level);
        mob.moveTo(x, -60, z, 180, 0);
        mob.setNoAi(true);
        mob.setYBodyRot(180);
        mob.setYHeadRot(180);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }
}
