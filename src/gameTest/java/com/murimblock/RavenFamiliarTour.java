package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.CombatService;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.world.raven.RavenBlocks;
import com.murimblock.world.raven.RavenHalls;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only real-client look at the Raven Messenger Art's familiar and its keeper (gradlew -PgameTests
 * runRavenFamiliarTour): the Shadow Raven flying at the shoulder of a student walking, then landed beside them standing
 * still; the Raven Tower built a little way off, and the Crow Master in his room. Report in
 * raven-familiar-tour-report.txt, pictures in build/raven-familiar-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenFamiliarTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static Vec3 tower = Vec3.ZERO;
    private static final List<String> report = new ArrayList<>();

    private RavenFamiliarTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.ravenFamiliarTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-raven-familiar", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-raven-familiar", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Raven familiar tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        switch (ticks) {
            case 20 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                CultivationService.setCultivation(self, MartialArts.RAVEN_REALM, CultivationStage.EARLY);
                MartialService.setForTests(self, MartialService.data(self).learn(MartialArts.art(MartialArts.RAVEN_ART)));
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.IRON_FAN.get()));
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
                start = self.position();
            });
            case 30 -> mc.options.hideGui = true;
            default -> { }
        }
        // Walking south a block every 4 ticks: the raven flies at the shoulder.
        if (ticks >= 40 && ticks < 100 && ticks % 2 == 0) server.execute(() -> {
            ServerPlayer self = self(server);
            self.teleportTo(self.getX(), self.getY(), self.getZ() + 0.25);
        });
        if (ticks == 60) look(mc, start.add(2.4, 2.6, 8.6), start.add(0, 2.0, 7.0));
        if (ticks >= 60 && ticks <= 96 && ticks % 6 == 0) capture(mc, String.format("1-flying/frame-%03d.png", (ticks - 60) / 6));
        if (ticks == 100) server.execute(() -> check(server.overworld().getEntitiesOfClass(com.murimblock.martial.RavenFamiliarEntity.class, self(server).getBoundingBox().inflate(8)).size() == 1, "one raven keeps by the student"));
        // Standing still: the raven lands beside.
        if (ticks == 170) look(mc, start.add(-2.2, 1.2, 17.5), start.add(-1.0, 0.4, 15.4));
        if (ticks >= 170 && ticks <= 200 && ticks % 10 == 0) capture(mc, String.format("2-perched/frame-%03d.png", (ticks - 170) / 10));
        // The Raven Tower, built a little way off, and its master.
        if (ticks == 210) server.execute(() -> {
            ServerPlayer self = self(server);
            int gx = (int) Math.floor(start.x) + 40, gz = (int) Math.floor(start.z);
            com.murimblock.world.structure.site.SitePiece.buildWhole(server.overworld(), com.murimblock.world.structure.site.SitePlan.RAVEN_TOWER, gx, gz,
                    net.minecraft.world.level.block.Rotation.NONE, 7L, new int[]{(int) Math.floor(self.getY()) - 1});
            tower = new Vec3(gx + 0.5, self.getY(), gz + 0.5);
        });
        if (ticks == 260) look(mc, tower.add(18, 9, 22), tower.add(0, 9, 0));
        if (ticks == 280) capture(mc, "3-tower.png");
        if (ticks == 290) server.execute(() -> {
            var masters = server.overworld().getEntitiesOfClass(com.murimblock.mob.CrowMasterEntity.class, new net.minecraft.world.phys.AABB(tower, tower).inflate(16));
            check(masters.size() == 1, "the Raven Tower holds its Crow Master (" + masters.size() + ")");
            if (!masters.isEmpty()) { var m = masters.getFirst(); m.setNoAi(true); m.teleportTo(tower.x + 1, m.getY(), tower.z + 1); m.setYRot(0); m.setYHeadRot(0); m.yBodyRot = 0; }
        });
        if (ticks == 300) look(mc, tower.add(1.5, 4.6, 3.6), tower.add(1.5, 4.0, 1.5));
        if (ticks == 330) capture(mc, "4-master.png");
        if (ticks == 335) look(mc, tower.add(-1.6, 4.4, 3.4), tower.add(1.5, 4.0, 1.5));
        if (ticks == 350) capture(mc, "5-master-side.png");
        if (ticks == 360) finish(mc);
    }

    /** The camera at `eye`, looking at `at`. */
    private static void look(Minecraft mc, Vec3 eye, Vec3 at) {
        Vec3 d = at.subtract(eye);
        camera = new Marker(EntityType.MARKER, mc.level);
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        camera.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        mc.setCameraEntity(camera);
    }

    @SubscribeEvent
    public static void onRenderHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (Boolean.getBoolean("murimblock.ravenFamiliarTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        LOGGER.info("Raven familiar tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("raven-familiar-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Raven tour report", e);
        }
        LOGGER.info("Raven familiar tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
