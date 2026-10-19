package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.RavenMasterEntity;
import com.murimblock.mob.RavenMasterSightings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only real-client look at the Raven Master (gradlew -PgameTests runRavenMasterTour), in a flat world with
 * a lone tall tree, a rock and a few bushes: (1) shown to a player in Qi Condensation by the sightings, perched far off
 * on the tree top, his ravens circling (over the player's shoulder, then zoomed, then from the side); (2) the player
 * looks him in the eyes: the cloud of ravens on the tree top, and he comes out beside the player; (3) a few frames of
 * the fight, his raven swarm and a shadow door among them, and one with the boss bar; (4) his portrait, close. Report in
 * raven-master-tour-report.txt, pictures in build/raven-master-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenMasterTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static Marker camera;
    private static Vec3 start = Vec3.ZERO, perch = Vec3.ZERO;
    private static RavenMasterEntity master, sitter;
    private static volatile Vec3 face = Vec3.ZERO;
    /** A tag on what the tour spawns itself (the clearing of the scene leaves it). */
    private static final String KEEP = "murimblock.tour_keep";
    private static final List<String> report = new ArrayList<>();

    private RavenMasterTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.ravenMasterTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-raven-master", GameType.SURVIVAL, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-raven-master", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Raven Master tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (camera != null && mc.getCameraEntity() == camera && mc.getConnection() != null) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround()));
        }
        // ---- the scene, and his sighting
        if (ticks == 20) server.execute(() -> {
            ServerLevel level = server.overworld();
            level.setDayTime(5000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            ServerPlayer self = self(server);
            CultivationService.setCultivation(self, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
            self.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100000, 4, false, false));
            self.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100000, 2, false, false));
            self.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100000, 0, false, false));
            self.teleportTo(level, Math.floor(self.getX()) + 0.5, self.getY(), Math.floor(self.getZ()) + 0.5, 0, 0);
            start = self.position();
            scene(level, BlockPos.containing(start));
        });
        if (ticks == 30) mc.options.hideGui = true;
        // Epic Fight's red outline on the one the player targets: not in the film.
        yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
        yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
        // The scene to himself: the world's own wanderers and beasts cleared away.
        if (ticks >= 20 && ticks % 10 == 0) server.execute(() -> server.overworld().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.Mob.class),
                e -> !(e instanceof RavenMasterEntity) && !e.getTags().contains(KEEP)).forEach(net.minecraft.world.entity.Entity::discard));
        if (ticks == 50) server.execute(() -> {
            ServerPlayer self = self(server);
            for (int i = 0; i < 30 && master == null; i++) master = RavenMasterSightings.appear(server.overworld(), self);
            check(master != null, "the sightings found a perch for him before the player");
            if (master == null) return;
            perch = master.position();
            double distance = Math.hypot(perch.x - start.x, perch.z - start.z);
            double off = Math.abs(net.minecraft.util.Mth.wrapDegrees(Math.toDegrees(Math.atan2(-(perch.x - start.x), perch.z - start.z)) - self.getYRot()));
            check(distance >= RavenMasterSightings.NEAR && distance <= RavenMasterSightings.FAR, "he perches " + Math.round(distance) + " blocks off");
            check(off <= RavenMasterSightings.VIEW_HALF_ANGLE, "within the player's view (" + Math.round(off) + " degrees off their look)");
            check(perch.y - start.y >= RavenMasterSightings.MIN_RISE, "high up: " + Math.round(perch.y - start.y) + " blocks over the ground (" + perch + ")");
            check(master.fleeting() && !master.hunting(), "he only watches");
        });
        if (master == null && ticks > 50) {
            if (ticks > 60) finish(mc);
            return;
        }
        Vec3 eye = start.add(0, 1.62, 0), his = perch.add(0, 1.6, 0);
        // (1) Watching: over the player's shoulder, zoomed, from the side.
        if (ticks == 60) look(mc, eye.add(-0.9, 0.5, -2.6), his);
        if (ticks == 75) capture(mc, "1-far/over-the-shoulder.png");
        if (ticks == 80) {
            mc.options.fov().set(30);
            look(mc, eye.add(0, 0.35, 0.6), his);
        }
        if (ticks >= 95 && ticks <= 135 && ticks % 10 == 5) capture(mc, String.format("1-far/zoom-%d.png", (ticks - 95) / 10));
        if (ticks == 140) {
            mc.options.fov().set(70);
            Vec3 side = his.add(new Vec3(perch.x - start.x, 0, perch.z - start.z).normalize().yRot((float) Math.toRadians(70)).scale(11)).add(0, 0.8, 0);
            look(mc, side, his.add(0, 0.4, 0));
        }
        if (ticks == 150 || ticks == 165) capture(mc, "1-far/perch-side-" + (ticks == 150 ? 0 : 1) + ".png");
        if (ticks == 168) server.execute(() -> check(!master.isRemoved() && !master.hunting() && master.getTarget() == null, "still watching after 6 s, attacking no one"));
        // (2) The player meets his eyes (from the side of his perch: the cloud of ravens), and he comes out by the player.
        if (ticks == 172) {
            mc.options.fov().set(70);
            Vec3 side = his.add(new Vec3(perch.x - start.x, 0, perch.z - start.z).normalize().yRot((float) Math.toRadians(70)).scale(11)).add(0, 0.8, 0);
            look(mc, side, his.add(0, 0.2, 0));
        }
        if (ticks == 176) mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, his.add(0, 0.02, 0));
        if (ticks >= 178 && ticks <= 190 && ticks % 2 == 0) capture(mc, String.format("2-stare/perch-burst-%d.png", ticks));
        if (ticks >= 191 && ticks <= 206 && fightCamera(mc, 5.0, 1.3) && ticks % 4 == 0) capture(mc, String.format("2-stare/arrival-%d.png", ticks));
        if (ticks == 207) server.execute(() -> {
            ServerPlayer self = self(server);
            check(master.hunting() && master.getTarget() == self, "met in the eyes, he fights the player (" + master.mood() + ")");
            check(master.distanceTo(self) < 8, "he came out beside the player (" + Math.round(master.distanceTo(self) * 10) / 10.0 + " blocks)");
        });
        // (3) The fight: a raven swarm, a shadow door; one frame with the boss bar.
        if (ticks == 230) server.execute(() -> master.startSwarm());
        if (ticks == 290) server.execute(() -> master.startShadowStep());
        if (ticks >= 209 && ticks <= 330 && fightCamera(mc, 4.0, 0.9) && ticks % 5 == 0) capture(mc, String.format("3-fight/frame-%02d.png", (ticks - 210) / 5));
        if (ticks >= 334 && ticks <= 340) fightCamera(mc, 5.5, 1.6);
        if (ticks == 334) mc.options.hideGui = false;
        if (ticks == 340) capture(mc, "3-fight/boss-bar.png");
        if (ticks == 342) {
            mc.options.hideGui = true;
            server.execute(() -> {
                check(self(server).isAlive(), "the player lived through the fight");
                check(master.pecksLanded() > 0, "his raven swarm pecked (" + master.pecksLanded() + " pecks)");
            });
        }
        // (4) His portrait: one placed by an egg (he stays, watching), the player behind the camera looking at his feet.
        if (ticks == 345) server.execute(() -> {
            master.discard();
            ServerLevel level = server.overworld();
            ServerPlayer self = self(server);
            BlockPos at = BlockPos.containing(start.add(-10, 0, 20));
            sitter = MurimEntities.RAVEN_MASTER.get().spawn(level, at, MobSpawnType.SPAWN_EGG);
            if (sitter != null) {
                sitter.setYRot(180);
                sitter.setYHeadRot(180);
                sitter.yBodyRot = 180;
                face = sitter.position().add(0, 1.62, 0);
            }
            self.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5 - 6, 0, 80);
        });
        if (ticks == 348) mc.player.setXRot(80);
        if (ticks == 360) look(mc, face.add(0, 0.05, -1.5), face);
        if (ticks == 372) capture(mc, "4-portrait/front.png");
        if (ticks == 374) look(mc, face.add(-1.2, 0.2, -1.1), face.add(0, -0.1, 0));
        if (ticks == 382) capture(mc, "4-portrait/three-quarters.png");
        if (ticks == 384) look(mc, face.add(-1.8, -0.1, 0.05), face.add(0, -0.1, 0));
        if (ticks == 392) capture(mc, "4-portrait/side.png");
        if (ticks == 394) look(mc, face.add(1.6, -0.2, 2.0), face.add(0, -0.4, 0));
        if (ticks == 402) capture(mc, "4-portrait/back.png");
        if (ticks == 403) look(mc, face.add(0.3, -0.5, -2.4), face.add(0, -0.7, 0));
        if (ticks == 408) capture(mc, "4-portrait/fan.png");
        if (ticks == 409) server.execute(() -> {
            check(sitter != null && !sitter.hunting(), "placed by an egg, he watches and does not fight");
            report.add("       start " + start + ", perch " + perch + ", sitter " + (sitter == null ? null : sitter.position()) + ", player " + self(server).position());
        });
        if (ticks == 412) finish(mc);
    }

    /** The scene round the player: a lone tall tree ahead, a rock off to its left, a few bushes and grass. */
    private static void scene(ServerLevel level, BlockPos feet) {
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState(), leaves = Blocks.SPRUCE_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        BlockPos tree = feet.offset(6, 0, 58);
        for (int y = 0; y < 11; y++) level.setBlock(tree.above(y), log, 3);
        for (int y = 4; y <= 11; y++) {
            int r = y >= 10 ? 0 : y >= 8 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) == r && Math.abs(dz) == r && r > 0 && (y % 2 == 0)) continue;
                BlockPos at = tree.offset(dx, y, dz);
                if (level.getBlockState(at).isAir()) level.setBlock(at, leaves, 3);
            }
        }
        // The rock: stone, three steps narrowing, moss on top.
        BlockPos rock = feet.offset(-16, 0, 50);
        for (int y = 0; y < 6; y++) {
            int r = y < 2 ? 2 : y < 4 ? 1 : 0;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                level.setBlock(rock.offset(dx, y, dz), (y == 5 ? Blocks.MOSSY_COBBLESTONE : (dx + dz + y) % 3 == 0 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState(), 3);
        }
        var random = level.getRandom();
        for (int i = 0; i < 70; i++) {
            BlockPos at = feet.offset(random.nextInt(60) - 30, 0, random.nextInt(70) - 4);
            if (!level.getBlockState(at).isAir()) continue;
            if (i % 9 == 0) level.setBlock(at, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
            else level.setBlock(at, (i % 3 == 0 ? Blocks.FERN : Blocks.SHORT_GRASS).defaultBlockState(), 3);
        }
    }

    /**
     * The camera beside the fight: `side` blocks off the line between the player and him, at `up` blocks over their
     * middle, looking at it (client positions). False if he is not there.
     */
    private static boolean fightCamera(Minecraft mc, double side, double up) {
        var him = master == null ? null : mc.level.getEntity(master.getId());
        if (him == null) return false;
        Vec3 a = mc.player.position(), b = him.position(), mid = a.add(b).scale(0.5);
        Vec3 across = new Vec3(b.z - a.z, 0, a.x - b.x);
        across = across.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : across.normalize();
        look(mc, mid.add(across.scale(side)).add(0, up + 0.6, 0), mid.add(0, 1.0, 0));
        return true;
    }

    /**
     * The camera at `eye`, looking at `at`: a marker 4 blocks ahead of it, the view from behind it (Minecraft's third
     * person sets the camera 4 blocks behind what it follows).
     */
    private static void look(Minecraft mc, Vec3 eye, Vec3 at) {
        Vec3 d = at.subtract(eye);
        eye = eye.add(d.normalize().scale(4.0));
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
        if (Boolean.getBoolean("murimblock.ravenMasterTour") && camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
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
        LOGGER.info("Raven Master tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        mc.options.fov().set(70);
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("raven-master-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Raven Master tour report", e);
        }
        LOGGER.info("Raven Master tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
