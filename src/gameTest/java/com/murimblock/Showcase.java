package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.MurimMistClient;
import com.murimblock.client.gui.MurimProfileScreen;
import com.murimblock.integration.epicfight.CrabManEpicFight;
import com.murimblock.integration.epicfight.CrabManPatch;
import com.murimblock.mob.ArmoredCrabEntity;
import com.murimblock.mob.CrabManEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.world.MurimBiomes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Development-only filming of the presentation video (gradlew -PgameTests runShowcase): a fresh normal world, a list
 * of shots (biome fly-overs, the crab man and his water balls by day and at night, the Murim menu), one frame per
 * client tick written to build/showcase-run/frames, and shots.json with each shot's frames and captions for the
 * editing script (scripts/showcase/make-video.cjs). Vertical 720 x 1280.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class Showcase {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 20261012L;

    /** Camera at frame f of n. */
    interface Camera { Vec3[] at(int f, int n); }

    static final class Shot {
        final String name, title, subtitle;
        final int prep, frames;
        Consumer<ServerLevel> setup = l -> { };
        BiConsumer<ServerLevel, Integer> onFrame = (l, f) -> { };
        Camera camera;
        boolean gui;
        Shot(String name, String title, String subtitle, int prep, int frames) {
            this.name = name; this.title = title; this.subtitle = subtitle; this.prep = prep; this.frames = frames;
        }
    }

    private static boolean creating, finished;
    private static int ticks, shotIndex = -1, phaseTicks, frame, firstFrame;
    private static List<Shot> shots;
    private static final List<String> timeline = new ArrayList<>();
    /** Set on the server thread once a shot's setup (finding places, spawning) is done. */
    private static volatile boolean ready;
    private static volatile BlockPos place;
    private static volatile int ground;
    private static volatile double startAngle;
    private static volatile CrabManEntity actor;

    private Showcase() { }

    // ---------------------------------------------------------------- the shots

    private static List<Shot> plan() {
        List<Shot> list = new ArrayList<>();
        list.add(biomeShot("title", "MURIMBLOCK", "Un mod wuxia pour Minecraft", MurimBiomes.JADE_KARST_FOREST, 70, 64, 48, 28));
        list.add(biomeShot("karst", "Forêt des pics de jade", "Karst de Guilin, rivière Li", MurimBiomes.JADE_KARST_FOREST, 100, 44, 18, 20));
        list.add(biomeShot("loess", "Plateau de lœss", "Fleuve Jaune, steppe et ravins", MurimBiomes.LOESS_PLATEAU, 100, 46, 30, 4));
        list.add(biomeShot("desert", "Désert du Hexi", "Gobi, yardangs, peupliers d'or", MurimBiomes.HEXI_FRONTIER_DESERT, 100, 40, 20, 6));
        list.add(biomeShot("peaks", "Pics de la mer de nuages", "Huangshan, granite et pins", MurimBiomes.CLOUD_SEA_PEAKS, 100, 46, 22, 22));
        list.add(biomeShot("bamboo", "Mer de bambous de Shu", "Bambous géants du Sichuan", MurimBiomes.SHU_BAMBOO_SEA, 100, 30, 16, 14));
        list.add(biomeShot("jiangnan", "Pays des eaux du Jiangnan", "Lotus, saules et étangs", MurimBiomes.JIANGNAN_WATERLANDS, 100, 34, 14, 2));
        list.add(biomeShot("peach", "Vallée des fleurs de pêcher", "La source cachée de Tao Yuanming", MurimBiomes.PEACH_BLOSSOM_VALLEY, 100, 46, 30, 16));
        list.add(crabShot(false));
        list.add(crabShot(true));
        Shot gui = new Shot("menu", "Voie du Murim", "Qi, cultivation et techniques", 30, 70);
        gui.setup = level -> { level.setDayTime(6000); ready = true; };
        gui.camera = (f, n) -> list.get(1).camera.at(n / 2, n);
        gui.gui = true;
        list.add(gui);
        list.add(biomeShot("end", "MURIMBLOCK", "Bientôt : structures, sectes et techniques", MurimBiomes.CLOUD_SEA_PEAKS, 80, 60, 40, 30));
        return list;
    }

    /** A slow push-in towards the heart of a biome, arcing 25 degrees, from radius r0 at height h above the ground. */
    private static Shot biomeShot(String name, String title, String subtitle, ResourceKey<Biome> biome, int frames, double r0, double h, double look) {
        Shot shot = new Shot(name, title, subtitle, 160, frames);
        shot.setup = level -> {
            level.setDayTime(name.equals("end") ? 12600 : 4500);
            BlockPos edge = BiomeTour.largest(level, biome);
            if (edge == null) { var r = level.findClosestBiome3d(hb -> hb.is(biome), BlockPos.ZERO.atY(80), 9600, 32, 64); edge = r == null ? BlockPos.ZERO : r.getFirst(); }
            place = BiomeTour.heart(level, biome, edge);
            teleport(level, place.getX() + 0.5, 220, place.getZ() + 0.5, 0, 60);
        };
        // After the chunks are in: ground height and a clear camera line (try angles until the path is in the air).
        shot.onFrame = (level, f) -> { };
        shot.camera = (f, n) -> {
            double t = smooth((double) f / Math.max(1, n - 1));
            double a = startAngle + Math.toRadians(25) * t;
            double r = r0 * (1 - 0.32 * t);
            double y = ground + h * (1 - 0.25 * t);
            Vec3 eye = new Vec3(place.getX() + Math.cos(a) * r, y, place.getZ() + Math.sin(a) * r);
            Vec3 target = new Vec3(place.getX(), ground + look, place.getZ());
            return new Vec3[]{eye, target};
        };
        return shot;
    }

    /** Crab man on open ground: jab, claw swing, water ball volley; at night the volley again for the Photon flash. */
    private static Shot crabShot(boolean night) {
        Shot shot = night ? new Shot("night", "La nuit, l'eau s'illumine", "Reflets GeckoLib, éclat Photon", 120, 120)
                : new Shot("crabman", "Crab Man", "Pince, jab et salve de boules d'eau", 160, 230);
        shot.setup = level -> {
            level.setDayTime(night ? 18000 : 5000);
            if (actor != null) actor.discard();
            BlockPos edge = BiomeTour.largest(level, MurimBiomes.LOESS_PLATEAU);
            place = BiomeTour.heart(level, MurimBiomes.LOESS_PLATEAU, edge);
            teleport(level, place.getX() + 0.5, 200, place.getZ() + 0.5, 0, 60);
        };
        shot.onFrame = (level, f) -> {
            if (actor == null || !actor.isAlive()) return;
            var patch = EpicFightCapabilities.getEntityPatch(actor, CrabManPatch.class);
            if (night) {
                if (f == 10) patch.playAnimationSynchronized(CrabManEpicFight.WATER_BALL, 0);
            } else {
                if (f == 20) patch.playAnimationSynchronized(CrabManEpicFight.CLAW_SNAP, 0);
                if (f == 70) patch.playAnimationSynchronized(CrabManEpicFight.ATTACK_CLAW, 0);
                if (f == 120) patch.playAnimationSynchronized(CrabManEpicFight.WATER_BALL, 0);
            }
        };
        shot.camera = (f, n) -> {
            double t = (double) f / Math.max(1, n - 1);
            Vec3 man = new Vec3(place.getX() + 0.5, ground, place.getZ() + 0.5);
            // He faces +X; the balls land 6 to 9 blocks ahead of him.
            Vec3 landing = man.add(7, 0, 0);
            if (night) {
                return new Vec3[]{man.add(2.5, 2.4, 6.5), man.add(3.5, 1.2, 0)};
            }
            double a = Math.toRadians(70 + 70 * smooth(t));
            double r = 4.2 + 2.0 * smooth(Math.max(0, (t - 0.5) / 0.5));
            Vec3 eye = man.add(Math.cos(a) * r, 1.9 + 0.8 * t, Math.sin(a) * r);
            Vec3 target = man.add(0, 1.3, 0).lerp(landing.add(0, 1, 0), smooth(Math.max(0, (t - 0.55) / 0.45)) * 0.6);
            return new Vec3[]{eye, target};
        };
        return shot;
    }

    // ---------------------------------------------------------------- driver

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.showcase") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        // Filming runs unattended: never let the window pause the game when it loses focus.
        mc.options.pauseOnLostFocus = false;
        // No Epic Fight target outline around the mob that targets the camera player.
        ClientConfig.showTargetIndicator = false;
        ClientConfig.enableTargetEntityGuide = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    mc.createWorldOpenFlows().createFreshLevel("murim-showcase",
                            new LevelSettings("murim-showcase", GameType.SPECTATOR, false, Difficulty.EASY, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(SEED, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            if (ticks == 20) {
                mc.options.hideGui = true;
                mc.options.renderDistance().set(14);
                mc.options.fov().set(75);
                mc.options.gamma().set(1.0);
                mc.options.pauseOnLostFocus = false;
                shots = plan();
                // -Dmurimblock.showcaseOnly=crabman,night films only those shots (for quick retakes).
                String only = System.getProperty("murimblock.showcaseOnly", "");
                if (!only.isBlank()) shots.removeIf(s -> !Arrays.asList(only.split(",")).contains(s.name));
                mc.getSingleplayerServer().execute(() -> {
                    ServerLevel level = mc.getSingleplayerServer().overworld();
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
                    level.setWeatherParameters(1000000, 0, false, false);
                });
            }
            if (ticks < 60) return;
            if (shotIndex < 0) startShot(mc, 0);
            Shot shot = shots.get(shotIndex);
            phaseTicks++;
            if (!ready) { phaseTicks = 0; return; }
            if (phaseTicks == shot.prep - 20) prepare(mc, shot);
            if (phaseTicks < shot.prep) return;
            int f = phaseTicks - shot.prep;
            if (f == 0) {
                firstFrame = frame;
                if (shot.gui) mc.setScreen(new MurimProfileScreen());
                MurimMistClient.settleNow();
            }
            if (f < shot.frames) {
                Vec3[] cam = shot.camera.at(f, shot.frames);
                place(mc, cam[0], cam[1]);
                int ff = f;
                mc.getSingleplayerServer().execute(() -> shot.onFrame.accept(mc.getSingleplayerServer().overworld(), ff));
                // The frame on screen now was rendered from the previous tick's camera: keep it (one tick behind).
                if (f > 0) capture(mc);
                return;
            }
            capture(mc);
            timeline.add(String.format("{\"name\":\"%s\",\"title\":\"%s\",\"subtitle\":\"%s\",\"start\":%d,\"end\":%d}",
                    shot.name, shot.title, shot.subtitle, firstFrame, frame - 1));
            if (shot.gui) mc.setScreen(null);
            if (shotIndex + 1 < shots.size()) startShot(mc, shotIndex + 1);
            else finish(mc);
        } catch (Exception e) {
            LOGGER.error("Showcase failed", e);
            finish(mc);
        }
    }

    private static void startShot(Minecraft mc, int index) {
        shotIndex = index;
        phaseTicks = 0;
        ready = false;
        Shot shot = shots.get(index);
        LOGGER.info("Showcase shot {}: {}", index, shot.name);
        mc.getSingleplayerServer().execute(() -> {
            ServerLevel level = mc.getSingleplayerServer().overworld();
            if (shot.gui) { shot.setup.accept(level); return; }
            shot.setup.accept(level);
            ready = true;
        });
    }

    /** Chunks are loaded by now: measure the ground, pick a clear camera arc, spawn the actors. */
    private static void prepare(Minecraft mc, Shot shot) {
        mc.getSingleplayerServer().execute(() -> {
            ServerLevel level = mc.getSingleplayerServer().overworld();
            if (place == null) return;
            ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, place.getX(), place.getZ());
            if (shot.name.equals("crabman") || shot.name.equals("night")) {
                // A clear stage: no tree or bush between the camera and the crab man.
                for (int dx = -14; dx <= 14; dx++) for (int dz = -14; dz <= 14; dz++) for (int dy = 0; dy <= 16; dy++) {
                    BlockPos p = new BlockPos(place.getX() + dx, ground + dy, place.getZ() + dz);
                    var state = level.getBlockState(p);
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.canBeReplaced())
                        if (!state.isAir() && state.getFluidState().isEmpty()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                CrabManEntity man = MurimEntities.CRAB_MAN.get().create(level);
                man.moveTo(place.getX() + 0.5, ground, place.getZ() + 0.5, -90, 0);
                man.yBodyRot = -90; man.yHeadRot = -90;
                level.addFreshEntity(man);
                man.setNoAi(true);
                // Persistent: the camera player hovers far above at first, which would despawn them at once.
                man.setPersistenceRequired();
                if (!shot.name.equals("night")) man.spawnEscort(level, level.getCurrentDifficultyAt(man.blockPosition()));
                level.getEntitiesOfClass(ArmoredCrabEntity.class, man.getBoundingBox().inflate(6)).forEach(Mob::setPersistenceRequired);
                actor = man;
                return;
            }
            // Turn the arc until neither end of it is inside rock or a tree.
            for (int k = 0; k < 12; k++) {
                startAngle = k * Math.PI / 6;
                boolean clear = true;
                for (int s = 0; s <= 8 && clear; s++) {
                    Vec3 eye = shot.camera.at(s * (shot.frames - 1) / 8, shot.frames)[0];
                    for (int dy = -1; dy <= 1; dy++) if (!level.getBlockState(BlockPos.containing(eye.x, eye.y + dy, eye.z)).isAir()) clear = false;
                }
                if (clear) break;
            }
        });
    }

    private static void place(Minecraft mc, Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        var p = mc.player;
        if (!p.getAbilities().flying) { p.getAbilities().flying = true; }
        double y = eye.y - p.getEyeHeight();
        p.absMoveTo(eye.x, y, eye.z, yaw, pitch);
        p.yRotO = yaw; p.xRotO = pitch; p.setYHeadRot(yaw); p.yHeadRotO = yaw; p.setYBodyRot(yaw); p.yBodyRotO = yaw;
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void teleport(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayers().get(0);
        // Creative flight, not spectator: spectators see entities with a highlight outline.
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();
        player.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static void capture(Minecraft mc) {
        Path file = mc.gameDirectory.toPath().resolve("frames").resolve(String.format("%05d.png", frame++));
        try {
            Files.createDirectories(file.getParent());
            var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            Util.ioPool().execute(() -> {
                try (image) { image.writeToFile(file); } catch (Exception e) { LOGGER.error("Frame write failed", e); }
            });
        } catch (Exception e) {
            LOGGER.error("Frame capture failed", e);
        }
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static void finish(Minecraft mc) {
        finished = true;
        try {
            Thread.sleep(1500); // let the last frames reach the disk
            Files.writeString(mc.gameDirectory.toPath().resolve("shots.json"), "[\n" + String.join(",\n", timeline) + "\n]\n");
        } catch (Exception e) {
            LOGGER.error("Could not write shots.json", e);
        }
        LOGGER.info("Showcase finished: {} frames", frame);
        mc.stop();
    }
}
