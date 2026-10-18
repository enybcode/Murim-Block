package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.cultivation.QiSense;
import com.murimblock.cultivation.VitalPoints;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
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
 * Opt-in real client look at Qi vision's vital points (VitalPoints): a player at Foundation Establishment, Qi Sense open,
 * has just parried a mountain bandit; its points light up, then the player strikes two of them, which burst. Seen in
 * first person, slowed down. Never included in the player JAR.
 * Run: gradlew -PgameTests runVitalPointsTour; frames in build/vital-points-tour-run/screenshots/vital/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class VitalPointsTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks, struck;
    private static Mob bandit;

    private VitalPointsTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.vitalPointsTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("vital-points", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("vital-points-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
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
                for (int x = -6; x <= 6; x++) for (int z = -3; z <= 8; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, 0.5, -60, 0.5, 0, 12);
                CultivationService.setCultivation(p, CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationStage.EARLY);
                QiService.setQiMax(p, 6000);
                QiService.refillQi(p);
                bandit = com.murimblock.mob.MurimEntities.MOUNTAIN_BANDIT.get().create(level);
                bandit.moveTo(0.5, -60, 3.0, 180, 0);
                bandit.setNoAi(true);
                bandit.setYBodyRot(180);
                bandit.setYHeadRot(180);
                bandit.setPersistenceRequired();
                level.addFreshEntity(bandit);
            });
            if (ticks == 40) server.execute(() -> {
                QiSense.setActive(server.getPlayerList().getPlayers().get(0), true);
                server.tickRateManager().setTickRate(5);
            });
            // The perfect parry just made.
            if (ticks == 50) server.execute(() -> VitalPoints.onPerfectParry(server.getPlayerList().getPlayers().get(0), bandit));
            if (ticks == 70 || ticks == 88) server.execute(() -> strike(server.getPlayerList().getPlayers().get(0)));
            if (ticks >= 52 && ticks < 150) capture(mc, String.format("vital/frame-%03d.png", ticks - 52));
            if (ticks == 160) {
                Files.writeString(mc.gameDirectory.toPath().resolve("vital-points-tour-report.txt"), "PASSED; sense open=" + com.murimblock.client.cultivation.QiSenseClient.open()
                        + "; struck=" + struck + "; still lit=" + Integer.bitCount(com.murimblock.client.cultivation.VitalPointsClient.lit()) + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Vital points tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    /** The player looks at a lit point and strikes it. */
    private static void strike(ServerPlayer p) {
        int mask = VitalPoints.open(p, bandit);
        if (mask == 0) return;
        int point = Integer.numberOfTrailingZeros(mask);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, VitalPoints.position(bandit, point));
        p.swing(InteractionHand.MAIN_HAND, true);
        bandit.invulnerableTime = 0;
        bandit.hurt(p.damageSources().playerAttack(p), 1);
        if ((VitalPoints.open(p, bandit) & 1 << point) == 0) struck++;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }
}
