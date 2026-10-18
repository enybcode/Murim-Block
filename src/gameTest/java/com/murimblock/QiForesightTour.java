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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Opt-in real client look at the mock-up of Qi vision's foresight (QiForesight): a player at Qi Crystallization, shielded,
 * Qi Sense open, faces a mountain bandit who attacks them; each of his blows is foreseen, his Qi gathering where it will
 * land, then bursting at the contact. Filmed from the side. Never included in the player JAR.
 * Run: gradlew -PgameTests runQiForesightTour; frames in build/qi-foresight-tour-run/screenshots/foresight/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiForesightTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;
    private static Mob bandit;
    private static net.minecraft.world.entity.Marker camera;

    private QiForesightTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.qiForesightTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("qi-foresight", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("qi-foresight-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
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
                p.teleportTo(level, 0.5, -60, 0.5, 0, 10);
                CultivationService.setCultivation(p, CultivationRealm.QI_CRYSTALLIZATION, CultivationStage.EARLY);
                QiService.setQiMax(p, 3000);
                QiService.refillQi(p);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 120, 4, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 120, 4, false, false));
                bandit = com.murimblock.mob.MurimEntities.MOUNTAIN_BANDIT.get().create(level);
                bandit.moveTo(0.5, -60, 3.0, 180, 0);
                bandit.setPersistenceRequired();
                level.addFreshEntity(bandit);
                bandit.setTarget(p);
            });
            if (ticks == 40) server.execute(() -> { QiSense.setActive(server.getPlayerList().getPlayers().get(0), true); server.tickRateManager().setTickRate(4); });
            if (ticks == 41) {
                camera = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityType.MARKER, mc.level);
                camera.moveTo(5.0, -58.8, 1.8, 90, 8);
                camera.xo = camera.xOld = 5.0; camera.yo = camera.yOld = -58.8; camera.zo = camera.zOld = 1.8;
                camera.yRotO = 90; camera.xRotO = 8;
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            if (ticks >= 60 && ticks < 160) capture(mc, String.format("foresight/frame-%03d.png", ticks - 60));
            if (ticks == 170) {
                Files.writeString(mc.gameDirectory.toPath().resolve("qi-foresight-tour-report.txt"), "PASSED; sense open=" + com.murimblock.client.cultivation.QiSenseClient.open() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Qi foresight tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }
}
