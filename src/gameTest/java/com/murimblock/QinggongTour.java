package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.cultivation.QinggongClient;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
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
 * Opt-in real client look at Qinggong's leap in the air (cultivation/Qinggong): a player at Qi Guiding runs along a stone
 * ledge toward a gap seven blocks wide over a deep pit, jumps at its edge, leaps again in the air off their Qi and lands on
 * the far side; filmed from the side, frame by frame. Never included in the player JAR.
 * Run: gradlew -PgameTests runQinggongTour; frames in build/qinggong-tour-run/screenshots/leap/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QinggongTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks, jumpedAt = -1;
    private static boolean landed;
    private static Marker camera;
    private static final List<String> report = new ArrayList<>();
    /** The ledge's edge (x) and the far side (x); the pit between, the run along +x at z 0. */
    static final int EDGE = 0, FAR = 8, Y = -40;

    private QinggongTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.qinggongTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("qinggong", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("qinggong-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
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
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                // A ledge, a pit seven blocks wide and twenty deep, the far side; stone pillars to measure by.
                for (int x = -16; x <= FAR + 6; x++) for (int z = -2; z <= 2; z++) for (int y = Y - 20; y <= Y; y++) {
                    boolean pit = x > EDGE && x < FAR;
                    level.setBlockAndUpdate(new BlockPos(x, y, z), pit ? Blocks.AIR.defaultBlockState() : y == Y ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState());
                }
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, -12.5, Y + 1, 0.5, -90, 0);
                CultivationService.setCultivation(p, CultivationRealm.QI_GUIDING, CultivationStage.EARLY);
                QiService.setQiMax(p, 300);
                QiService.refillQi(p);
            });
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                place(-4, Y + 3.5, -11.5, 0, 6);
                mc.options.fov().set(70);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            // The run: forward held, as a player would, through the jump and the leap; sprinting; at the edge, the jump;
            // five ticks into the air, the leap.
            if (ticks >= 60) {
                mc.player.setYRot(-90);
                // The camera is a marker, so the player reads no keys: forward is set on it directly.
                mc.player.zza = landed ? 0 : 1;
                if (!landed) mc.player.setSprinting(true);
            }
            if (ticks >= 60 && jumpedAt < 0 && mc.player.getX() >= EDGE + 0.6) {
                mc.player.jumpFromGround();
                jumpedAt = ticks;
            }
            if (jumpedAt > 0 && ticks > jumpedAt + 8 && mc.player.onGround()) landed = true;
            if (jumpedAt > 0 && ticks == jumpedAt + 5) {
                report.add("canLeap=" + com.murimblock.cultivation.Qinggong.canLeap(mc.player));
                QinggongClient.leap(mc.player);
            }
            if (ticks >= 56 && ticks < 56 + 80) capture(mc, String.format("leap/frame-%03d.png", ticks - 56));
            if (ticks == 150) {
                var p = mc.player;
                boolean across = p.getX() >= FAR && p.getY() >= Y + 0.9;
                report.add(String.format("landed at x %.1f y %.1f", p.getX(), p.getY()));
                Files.writeString(mc.gameDirectory.toPath().resolve("qinggong-tour-report.txt"), (across ? "PASSED" : "FAILED") + "; " + report + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Qinggong tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void place(double x, double y, double z, float yaw, float pitch) {
        camera.setPos(x, y, z);
        camera.xo = camera.xOld = x;
        camera.yo = camera.yOld = y;
        camera.zo = camera.zOld = z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }
}
