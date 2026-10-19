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
 * Development-only real-client look at the Raven Halls (gradlew -PgameTests runRavenTour): a raven perch set down in
 * the world, then home to one's own hall (the crouching Closed Room), seen from inside both ways and from outside, a
 * door to the perch, and back. Report in raven-tour-report.txt, pictures in build/raven-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start;
    private static BlockPos perch;
    private static final List<String> report = new ArrayList<>();

    private RavenTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.ravenTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-raven", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-raven", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Raven tour crashed", e);
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
                ServerPlayer self = self(server);
                CultivationService.setCultivation(self, MartialArts.RAVEN_REALM, CultivationStage.EARLY);
                MartialService.setForTests(self, MartialService.data(self).learn(MartialArts.art(MartialArts.RAVEN_ART)));
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.IRON_FAN.get()));
                CombatService.setCombatMode(self, true);
                self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
                start = self.position();
                // A perch set down three blocks ahead, as a student would.
                perch = self.blockPosition().offset(0, 0, 3);
                ItemStack item = new ItemStack(RavenBlocks.RAVEN_PERCH_ITEM.get());
                level.setBlock(perch, RavenBlocks.RAVEN_PERCH.get().defaultBlockState(), 3);
                RavenBlocks.RAVEN_PERCH.get().setPlacedBy(level, perch, level.getBlockState(perch), self, item);
            });
            case 34 -> {
                mc.options.hideGui = true;
                look(mc, Vec3.atBottomCenterOf(perch).add(2.2, 1.4, -1.6), Vec3.atBottomCenterOf(perch).add(0, 0.8, 0));
            }
            case 40 -> capture(mc, "01-perch.png");
            case 44 -> {
                mc.setCameraEntity(mc.player);
                server.execute(() -> check(RavenHalls.home(self(server)), "home to one's own hall"));
            }
            case 90 -> server.execute(() -> check(RavenHalls.isHalls(self(server).level()), "in the Raven Halls"));
            case 92 -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            case 100 -> capture(mc, "02-hall-inside.png");
            // Out on the terrace's edge, looking out over the rail into the castle.
            case 102 -> server.execute(() -> {
                ServerPlayer self = self(server);
                BlockPos hall = RavenHalls.hallOf(self);
                self.teleportTo(self.serverLevel(), hall.getX() + 0.5, hall.getY(), hall.getZ() + 18.5, 0, -4);
            });
            case 150 -> capture(mc, "03-terrace-edge.png");
            case 152 -> {
                BlockPos hall = RavenHalls.hallOf(self(server));
                Vec3 c = Vec3.atBottomCenterOf(hall);
                look(mc, c.add(34, 16, 38), c.add(0, 2, 0));
            }
            case 175 -> capture(mc, "04-hall-in-the-castle.png");
            // Deep in the castle, away from the hall: ahead, up, down.
            case 177 -> {
                mc.setCameraEntity(mc.player);
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    self.setGameMode(GameType.SPECTATOR);
                    BlockPos hall = RavenHalls.hallOf(self);
                    self.teleportTo(self.serverLevel(), hall.getX() + 150.5, hall.getY() + 5, hall.getZ() + 150.5, 135, 0);
                });
            }
            case 260 -> capture(mc, "05-castle-ahead.png");
            case 262 -> server.execute(() -> { ServerPlayer self = self(server); self.teleportTo(self.serverLevel(), self.getX(), self.getY(), self.getZ(), 45, -55); });
            case 275 -> capture(mc, "06-castle-up.png");
            case 277 -> server.execute(() -> { ServerPlayer self = self(server); self.teleportTo(self.serverLevel(), self.getX(), self.getY(), self.getZ(), 225, 60); });
            case 290 -> capture(mc, "07-castle-down.png");
            case 292 -> server.execute(() -> {
                ServerPlayer self = self(server);
                self.setGameMode(GameType.SURVIVAL);
                // Through the west door (the first perch's).
                BlockPos hall = RavenHalls.hallOf(self);
                self.teleportTo(self.serverLevel(), hall.getX() - 10.5, hall.getY(), hall.getZ() + 0.5, 90, 0);
            });
            case 330 -> server.execute(() -> {
                ServerPlayer self = self(server);
                check(self.level() == server.overworld() && self.blockPosition().closerThan(perch, 3), "the west door leads to the perch (at " + self.blockPosition() + ")");
            });
            case 340 -> finish(mc);
            default -> { }
        }
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
        if (Boolean.getBoolean("murimblock.ravenTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        LOGGER.info("Raven tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("raven-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Raven tour report", e);
        }
        LOGGER.info("Raven tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
