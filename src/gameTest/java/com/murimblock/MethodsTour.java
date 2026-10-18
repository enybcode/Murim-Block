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
 * Opt-in real client look at the cultivation methods (CultivationMethod, the Cultivation tab): a player at Qi
 * Condensation who knows Frost Lotus Heart and Blazing Sun Scripture (not Azure Sky Breathing); the method's pill, the
 * methods' view on each, then Blazing Sun practised (the Qi out of tune). Never included in the player JAR.
 * Run: gradlew -PgameTests runMethodsTour; screenshots in build/methods-tour-run/screenshots/methods/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MethodsTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;

    private MethodsTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.methodsTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("methods", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("methods-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            var server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            if (ticks == 20) server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, 0.5, -60, 0.5, 0, 0);
                CultivationService.setCultivation(p, CultivationRealm.QI_CONDENSATION, CultivationStage.MIDDLE);
                QiService.setQiMax(p, 600);
                QiService.refillQi(p);
                com.murimblock.cultivation.MethodService.learn(p, com.murimblock.cultivation.CultivationMethod.FROST_LOTUS_HEART);
                com.murimblock.cultivation.MethodService.learn(p, com.murimblock.cultivation.CultivationMethod.BLAZING_SUN_SCRIPTURE);
            });
            if (ticks == 40) mc.setScreen(com.murimblock.client.gui.MurimProfileScreen.cultivation());
            if (ticks == 50) capture(mc, "methods/01-cultivation-tab.png");
            if (ticks == 52) mc.setScreen(com.murimblock.client.gui.MurimProfileScreen.methods(com.murimblock.cultivation.CultivationMethod.ORDINARY_BREATH));
            if (ticks == 60) capture(mc, "methods/02-ordinary-breath.png");
            if (ticks == 62) mc.setScreen(com.murimblock.client.gui.MurimProfileScreen.methods(com.murimblock.cultivation.CultivationMethod.BLAZING_SUN_SCRIPTURE));
            if (ticks == 70) capture(mc, "methods/03-blazing-sun.png");
            if (ticks == 72) mc.setScreen(com.murimblock.client.gui.MurimProfileScreen.methods(com.murimblock.cultivation.CultivationMethod.AZURE_SKY_BREATHING));
            if (ticks == 80) capture(mc, "methods/04-azure-sky-unknown.png");
            if (ticks == 82) server.execute(() -> com.murimblock.cultivation.MethodService.practise(server.getPlayerList().getPlayers().get(0), com.murimblock.cultivation.CultivationMethod.BLAZING_SUN_SCRIPTURE));
            if (ticks == 90) mc.setScreen(com.murimblock.client.gui.MurimProfileScreen.methods(com.murimblock.cultivation.CultivationMethod.BLAZING_SUN_SCRIPTURE));
            if (ticks == 100) capture(mc, "methods/05-blazing-sun-practised.png");
            if (ticks == 102) mc.setScreen(com.murimblock.client.gui.MurimProfileScreen.cultivation());
            if (ticks == 110) capture(mc, "methods/06-cultivation-tab-after.png");
            if (ticks == 115) {
                Files.writeString(mc.gameDirectory.toPath().resolve("methods-tour-report.txt"), "PASSED; method="
                        + com.murimblock.cultivation.MethodService.method(mc.player).id() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Methods tour failed", e);
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
