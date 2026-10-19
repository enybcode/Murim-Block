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
 * Opt-in real client look at flying on the sword (SwordFlight): a player at Core Formation, a diamond sword in hand,
 * jumps, presses jump again and holds it three seconds (the Qi gathering under the feet), flies on along the look a
 * little up then down, and gets off with two quick jumps; seen from behind, the real keys held. Never in the player JAR.
 * Run: gradlew -PgameTests runSwordFlightTour; frames in build/sword-flight-tour-run/screenshots/flight/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SwordFlightTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;
    private static net.minecraft.world.entity.Marker camera;
    private static String report = "";

    private SwordFlightTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.swordFlightTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("sword-flight", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("sword-flight-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
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
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, 0.5, -60, 0.5, -90, 0);
                CultivationService.setCultivation(p, CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
                QiService.setQiMax(p, 9000);
                QiService.refillQi(p);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.murimblock.item.MurimItems.AZURE_CLOUD_SWORD.get()));
            });
            if (ticks == 40) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            // Jump, then the second press held three seconds: onto the sword.
            if (ticks >= 45) mc.player.setYRot(-90);
            if (ticks == 50) mc.options.keyJump.setDown(true);
            if (ticks == 52) mc.options.keyJump.setDown(false);
            if (ticks == 56) mc.options.keyJump.setDown(true);
            if (ticks == 56 + 64) mc.options.keyJump.setDown(false);
            // On the sword, up a little (jump held).
            if (ticks == 124) mc.options.keyJump.setDown(true);
            if (ticks == 146) mc.options.keyJump.setDown(false);
            if (ticks == 150) report = "mounted=" + com.murimblock.client.cultivation.SwordFlightClient.flying(mc.player);
            // The rider's pose, from the side.
            if (ticks == 154) {
                camera = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityType.MARKER, mc.level);
                double x = mc.player.getX() + 0.3, y = mc.player.getY() + 1.6, z = mc.player.getZ() + 5.0;
                camera.moveTo(x, y, z, 180, 14);
                camera.xo = camera.xOld = x; camera.yo = camera.yOld = y; camera.zo = camera.zOld = z;
                camera.yRotO = 180; camera.xRotO = 14;
                mc.setCameraEntity(camera);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            if (ticks == 170) capture(mc, "pose/skate-side.png");
            if (ticks == 176) {
                mc.setCameraEntity(mc.player);
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            }
            // Flying on, then very fast (sprint held), then slowing; off with two quick jumps.
            if (ticks == 180) mc.player.setXRot(0);
            if (ticks == 180) mc.options.keyUp.setDown(true);
            if (ticks == 220) mc.options.keySprint.setDown(true);
            if (ticks == 300) mc.options.keySprint.setDown(false);
            if (ticks == 280) report += " fast at 280: " + String.format("%.2f", mc.player.getDeltaMovement().length()) + " b/t";
            if (ticks == 320) mc.options.keyUp.setDown(false);
            if (ticks == 330 || ticks == 334) mc.options.keyJump.setDown(true);
            if (ticks == 331 || ticks == 335) mc.options.keyJump.setDown(false);
            if (ticks >= 48 && ticks < 370 && ticks % 2 == 0 && (ticks < 154 || ticks > 178)) capture(mc, String.format("flight/frame-%03d.png", (ticks - 48) / 2));
            if (ticks == 375) {
                Files.writeString(mc.gameDirectory.toPath().resolve("sword-flight-tour-report.txt"), "PASSED; " + report
                        + "; at end flying=" + com.murimblock.client.cultivation.SwordFlightClient.flying(mc.player) + " y=" + String.format("%.1f", mc.player.getY()) + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Sword flight tour failed", e);
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
