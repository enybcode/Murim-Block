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
 * Opt-in real client look at Qi vision's tiring eyes (QiSenseClient.fatigue): held open from 28 s, the sight wavers,
 * blurs and darkens from the edges until it closes by itself at 40 s. Never included in the player JAR.
 * Run: gradlew -PgameTests runQiFatigueTour; frames in build/qi-fatigue-tour-run/screenshots/fatigue/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiFatigueTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;

    private QiFatigueTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.qiFatigueTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("qi-fatigue", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("qi-fatigue-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
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
                for (int x = -8; x <= 8; x++) for (int z = -3; z <= 12; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                for (int x = -4; x <= 4; x++) for (int y = -60; y <= -56; y++) level.setBlockAndUpdate(new BlockPos(x, y, 4), Blocks.STONE_BRICKS.defaultBlockState());
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, 0.5, -60, -1.5, -14, 2);
                CultivationService.setCultivation(p, CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
                QiService.setQiMax(p, 9000);
                QiService.refillQi(p);
                spawn(level, com.murimblock.mob.MurimEntities.MOUNTAIN_BANDIT.get(), 1.5, 7.5);
                spawn(level, EntityType.ZOMBIE, -2.0, 9.5);
                spawn(level, EntityType.COW, 3.0, 11.0);
                spawn(level, com.murimblock.mob.MurimEntities.MOUNTAIN_BANDIT.get(), 6.5, 6.0);
            });
            if (ticks == 62) server.execute(() -> QiSense.setActive(server.getPlayerList().getPlayers().get(0), true));
            if (ticks >= 62 + 28 * 20 && ticks < 62 + 42 * 20 && ticks % 4 == 0) capture(mc, String.format("fatigue/frame-%03d.png", (ticks - 62 - 28 * 20) / 4));
            if (ticks == 62 + 43 * 20) {
                Files.writeString(mc.gameDirectory.toPath().resolve("qi-fatigue-tour-report.txt"), "PASSED; sense open="
                        + com.murimblock.client.cultivation.QiSenseClient.open() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Qi fatigue tour failed", e);
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
