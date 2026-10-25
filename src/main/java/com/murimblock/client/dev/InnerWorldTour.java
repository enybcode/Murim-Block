package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.StageBreakthrough;
import com.murimblock.client.innerworld.TrialEntry;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.Meditation;
import com.murimblock.innerworld.InnerIsland;
import com.murimblock.innerworld.InnerIslands;
import com.murimblock.innerworld.InnerWorld;
import com.murimblock.client.innerworld.OldMasterScreen;
import com.murimblock.innerworld.MeditatingBody;
import com.murimblock.innerworld.MortalTrial;
import com.murimblock.innerworld.OldMaster;
import com.murimblock.innerworld.TrialZombie;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import com.murimblock.client.gui.SystemPage;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.page.CultivationPage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Dev tool, off unless {@code murimblock.innerWorldTour} is set (run {@code gradlew runInnerWorldTour}): checks the
 * inner world in a real client (locked as a mortal, entry, island, nothing breakable or placeable, falling, the seated
 * body and a blow on it, the Old Master), a stage breakthrough, then the mortal trial (lost once, then won); logs
 * PASS or FAIL for each, saves screenshots
 * to run/screenshots, then quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldTour {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.innerWorldTour");
    private static final String WORLD = "murim-inner-world-tour";
    private static final int WORLD_FRAMES = 120;
    private static final int STEP_FRAMES = 60;

    private static boolean started;
    private static int frames = -1;
    private static int step;
    private static int failures;
    private static BlockPos sat;
    private static volatile boolean trialDone;
    /** Next film frame of the trial entry, or -1 when not filming. */
    private static int filmFrame = -1;
    private static long filmTick;
    /** Pending breakthrough shots: effect age in ticks, file name. */
    private static final List<Map.Entry<Integer, String>> SHOTS = new ArrayList<>();
    /** Pending shots taken as soon as their condition holds. */
    private static final List<Map.Entry<BooleanSupplier, String>> TIMED = new ArrayList<>();

    private InnerWorldTour() {
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            // A fresh game folder opens on the accessibility screen first.
            if (mc.options.onboardAccessibility) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            } else if (mc.screen instanceof TitleScreen) {
                started = true;
                frames = -1;
                openWorld(mc);
            } else if (++frames % 300 == 0) {
                Murimblock.LOGGER.info("Inner world tour: waiting for the title screen, on {}", mc.screen);
            }
            return;
        }
        if (mc.player == null || mc.screen != null && !(mc.screen instanceof SystemScreen || mc.screen instanceof OldMasterScreen)) {
            return;
        }
        // The trial entry is filmed one shot per game tick, from BEGIN TRIAL to the title (made into a GIF).
        if (filmFrame >= 0 && mc.level.getGameTime() != filmTick) {
            filmTick = mc.level.getGameTime();
            shoot(mc, String.format("film/murim_trial_entry_%03d.png", filmFrame++));
            if (TrialEntry.arrival() >= 49) {
                filmFrame = -1;
            }
        }
        // Breakthrough shots are timed on the effect's own ticks, not on frames.
        int age = StageBreakthrough.age(mc.player.getId());
        if (!SHOTS.isEmpty() && age >= SHOTS.get(0).getKey()) {
            shoot(mc, SHOTS.remove(0).getValue());
        }
        if (!TIMED.isEmpty() && TIMED.get(0).getKey().getAsBoolean()) {
            shoot(mc, TIMED.remove(0).getValue());
        }
        if (++frames < WORLD_FRAMES || (frames - WORLD_FRAMES) % STEP_FRAMES != 0) {
            return;
        }
        run(mc, step++);
    }

    private static void run(Minecraft mc, int index) {
        switch (index) {
            case 0 -> server(mc, player -> {
                // A mortal, meditating on a platform in the sky: the inner world stays shut.
                sat = BlockPos.containing(player.getX(), 200, player.getZ());
                for (BlockPos pos : BlockPos.betweenClosed(sat.offset(-3, -1, -3), sat.offset(3, -1, 3))) {
                    player.serverLevel().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.teleportTo(sat.getX() + 0.5, sat.getY(), sat.getZ() + 0.5);
                Cultivation c = Meditation.of(player);
                c.set(0, 9, 0, c.maxQi(), c.maxQi(), 3);
            });
            case 1 -> server(mc, player -> {
                Meditation.start(player);
                InnerWorld.enter(player);
                check("mortal stays out", !InnerWorld.state(player).inside() && !InnerWorld.isInnerWorld(player.level()));
                Cultivation c = Meditation.of(player);
                c.set(1, 1, 0, c.maxQi(), c.maxQi(), 3);
                Meditation.sync(player);
                InnerWorld.enter(player);
                check("third-rate enters", InnerWorld.state(player).inside() && InnerWorld.isInnerWorld(player.level()));
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                check("arrival on own island", center != null && Math.abs(player.getX() - center.getX() - 0.5) < 1
                        && Math.abs(player.getZ() - center.getZ() - InnerIsland.ARRIVAL_Z - 0.5) < 1);
                check("body left seated", player.server.overworld().getEntity(InnerWorld.state(player).body())
                        instanceof MeditatingBody);
            });
            case 2 -> {
                shoot(mc, "murim_inner_arrival.png");
                mc.setScreen(new SystemScreen(SystemPage.CULTIVATION, new CultivationPage(false)));
            }
            case 3 -> {
                // The Cultivation page inside: the button now reads RETURN TO BODY.
                shoot(mc, "murim_inner_page.png");
                mc.setScreen(null);
            }
            case 4 -> server(mc, player -> {
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                BlockPos floor = center.offset(3, 0, 3);
                BlockState before = player.level().getBlockState(floor);
                player.gameMode.destroyBlock(floor);
                check("arena block cannot be broken", player.level().getBlockState(floor) == before && !before.isAir());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
                player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                check("no block placed", player.level().getBlockState(floor.above()).isAir());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                // Hover above the south shore, looking down on the whole island.
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.connection.teleport(center.getX() + 0.5, InnerIsland.SURFACE_Y + 30, center.getZ() + 58.5, 180, 30);
            });
            case 5 -> shoot(mc, "murim_inner_aerial.png");
            case 6 -> view(mc, 6, 6, 22, 234, 15);
            case 7 -> shoot(mc, "murim_inner_pond.png");
            case 8 -> view(mc, 46, -14, 10, 90, -12);
            case 9 -> shoot(mc, "murim_inner_underside.png");
            case 10 -> view(mc, 0, 24, 0, 180, 90);
            case 11 -> shoot(mc, "murim_inner_arena.png");
            case 12 -> server(mc, player -> {
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                List<OldMaster> masters = player.serverLevel().getEntitiesOfClass(OldMaster.class, new AABB(center).inflate(InnerIsland.RADIUS));
                check("old master seated in the NPC zone", masters.size() == 1
                        && Math.abs(masters.get(0).getX() - center.getX()) < 1
                        && Math.abs(masters.get(0).getZ() - center.getZ() - InnerIsland.NPC_Z - InnerIsland.NPC_SIZE / 2.0) < 1);
                // Standing on the path south of him, a little to the side: he turns his head to look.
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.connection.teleport(center.getX() + 1.5, InnerIsland.SURFACE_Y + 1, center.getZ() - 19.5, 170, 8);
            });
            case 13 -> {
                mc.options.hideGui = true;
                return;
            }
            case 14 -> {
                shoot(mc, "murim_inner_master.png");
                mc.options.hideGui = false;
                master(mc).ifPresent(master -> mc.setScreen(new OldMasterScreen(master)));
            }
            case 15 -> mc.screen.keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0);
            case 16 -> {
                shoot(mc, "murim_inner_master_greet.png");
                mc.screen.keyPressed(GLFW.GLFW_KEY_1, 0, 0);
            }
            case 17 -> mc.screen.keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0);
            case 18 -> {
                check("old master answers", mc.screen instanceof OldMasterScreen);
                shoot(mc, "murim_inner_master_who.png");
            }
            case 19 -> mc.setScreen(null);
            case 20 -> server(mc, player -> {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                player.teleportTo(center.getX() + 0.5, InnerIsland.SURFACE_Y - 50, center.getZ() + 0.5);
            });
            case 21 -> server(mc, player -> {
                check("fall returns to body", !InnerWorld.state(player).inside() && !InnerWorld.isInnerWorld(player.level())
                        && player.blockPosition().distManhattan(sat) <= 2);
                BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                check("old master leaves with the player", player.server.getLevel(InnerWorld.LEVEL)
                        .getEntitiesOfClass(OldMaster.class, new AABB(center).inflate(InnerIsland.RADIUS)).isEmpty());
                // The body seen from the front: seat one, then step back to look at it.
                Meditation.start(player);
                MeditatingBody body = MeditatingBody.seat(player);
                body.setYRot(180);
                body.setYHeadRot(180);
                body.setYBodyRot(180);
                InnerWorld.state(player).enter(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot(), body.getUUID());
                Meditation.stop(player, null);
                player.connection.teleport(sat.getX() + 0.5, sat.getY(), sat.getZ() - 2.5, 0, 25);
            });
            case 22 -> {
                mc.options.hideGui = true;
                return;
            }
            case 23 -> {
                shoot(mc, "murim_inner_body.png");
                mc.options.hideGui = false;
            }
            case 24 -> server(mc, player -> {
                UUID bodyId = InnerWorld.state(player).body();
                if (player.serverLevel().getEntity(bodyId) instanceof MeditatingBody body) {
                    body.hurt(player.damageSources().playerAttack(player), 1);
                }
                check("a blow on the body ends the trip", !InnerWorld.state(player).inside()
                        && player.serverLevel().getEntity(bodyId) == null);
            });
            case 25 -> server(mc, player -> {
                // A stage breakthrough on the sky platform: Third-rate stage 3, gauge full, meditating.
                player.connection.teleport(sat.getX() + 0.5, sat.getY(), sat.getZ() + 0.5, 30, 10);
                Cultivation c = Meditation.of(player);
                c.set(1, 3, CultivationRules.gauge(1, 3), c.maxQi(), c.maxQi(), 4);
                Meditation.start(player);
                Meditation.sync(player);
            });
            case 26 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server(mc, Meditation::advance);
                SHOTS.add(Map.entry(20, "murim_breakthrough_spiral.png"));
                SHOTS.add(Map.entry(39, "murim_breakthrough_burst.png"));
                SHOTS.add(Map.entry(55, "murim_breakthrough_window.png"));
            }
            case 27 -> server(mc, player -> check("breakthrough rises one stage", Meditation.of(player).stage() == 4));
            case 28 -> {
                if (StageBreakthrough.age(mc.player.getId()) >= 0 || !SHOTS.isEmpty()) {
                    step--; // Still playing: ask again at the next step.
                    return;
                }
                check("camera back in first person", mc.options.getCameraType() == CameraType.FIRST_PERSON);
            }
            case 29 -> server(mc, player -> {
                // The mortal trial: Body Tempering 9, full gauge, 4 meridians, meditating.
                Cultivation c = Meditation.of(player);
                c.set(0, 9, CultivationRules.gauge(0, 9), c.maxQi(), c.maxQi(), 4);
                Meditation.start(player);
                Meditation.sync(player);
            });
            case 30 -> mc.setScreen(new SystemScreen(SystemPage.CULTIVATION, new CultivationPage(false)));
            case 31 -> {
                shoot(mc, "murim_trial_page.png");
                mc.setScreen(null);
                server(mc, MortalTrial::begin);
                File film = new File(mc.gameDirectory, "screenshots/film");
                film.mkdirs();
                File[] old = film.listFiles();
                if (old != null) {
                    for (File f : old) {
                        f.delete();
                    }
                }
                filmFrame = 0;
                TIMED.add(Map.entry(() -> TrialEntry.ownDive() >= 36, "murim_trial_dive.png"));
                TIMED.add(Map.entry(() -> TrialEntry.arrival() >= 12, "murim_trial_rise_1.png"));
                TIMED.add(Map.entry(() -> TrialEntry.arrival() >= 26, "murim_trial_rise_2.png"));
                TIMED.add(Map.entry(() -> TrialEntry.arrival() >= 48, "murim_trial_title.png"));
            }
            case 32 -> {
                if (!TIMED.isEmpty()) {
                    step--; // The dive, the arrival and the title play first.
                    return;
                }
                server(mc, player -> check("after the dive, the trial begins on the graveyard island",
                        InnerWorld.isTrial(player.level()) && InnerWorld.state(player).inside()));
            }
            case 33 -> {
                if (trialZombies(mc) == 0) {
                    step--; // The first wave comes after a few seconds of calm.
                    return;
                }
                server(mc, player -> {
                    check("wave 1 climbs out of the graves", countZombies(player) == MortalTrial.WAVES[0]);
                    BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.connection.teleport(center.getX() + 0.5, InnerIsland.SURFACE_Y + 16, center.getZ() + 40.5, 180, 28);
                });
            }
            case 34 -> shoot(mc, "murim_trial_island.png");
            case 35 -> server(mc, player -> {
                InnerWorld.leave(player, "message.murimblock.inner_world.leave");
                Cultivation c = Meditation.of(player);
                check("leaving loses the trial and scatters 30 % of the gauge", !InnerWorld.state(player).inside()
                        && c.realm() == 0 && Math.abs(c.stageQi() - CultivationRules.gauge(0, 9) * 0.7) < 1e-6);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                c.set(0, 9, CultivationRules.gauge(0, 9), c.maxQi(), c.maxQi(), 4);
                Meditation.sync(player);
            });
            case 36 -> server(mc, player -> {
                // Back on the platform for a tick: now on the ground, it can sit down again.
                check("the zombies of a lost trial are gone", countZombies(player) == 0);
                Meditation.start(player);
            });
            case 37 -> server(mc, MortalTrial::begin);
            case 38 -> server(mc, player -> {
                if (InnerWorld.isTrial(player.level())) {
                    player.serverLevel().getEntitiesOfClass(TrialZombie.class, player.getBoundingBox().inflate(80))
                            .forEach(z -> z.kill());
                }
                trialDone = !InnerWorld.state(player).inside() && !MortalTrial.diving(player);
            });
            case 39 -> {
                if (!trialDone) {
                    step -= 2; // Clear the next wave.
                    return;
                }
                server(mc, player -> {
                    Cultivation c = Meditation.of(player);
                    check("five waves cleared: Third-Rate Warrior", c.realm() == 1 && c.stage() == 1 && c.stageQi() == 0);
                });
            }
            case 40 -> shoot(mc, "murim_trial_won.png");
            default -> {
                Murimblock.LOGGER.info("Inner world tour: done, {} failure(s)", failures);
                mc.stop();
            }
        }
    }

    /** Trial zombies the client sees near the player. */
    private static int trialZombies(Minecraft mc) {
        return mc.level.getEntitiesOfClass(TrialZombie.class, mc.player.getBoundingBox().inflate(80)).size();
    }

    private static int countZombies(ServerPlayer player) {
        ServerLevel trial = player.server.getLevel(InnerWorld.TRIAL_LEVEL);
        BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
        return trial.getEntitiesOfClass(TrialZombie.class, new AABB(center).inflate(InnerIsland.RADIUS * 2)).size();
    }

    private static Optional<OldMaster> master(Minecraft mc) {
        return mc.level.getEntitiesOfClass(OldMaster.class, mc.player.getBoundingBox().inflate(16)).stream().findFirst();
    }

    /** Puts the (flying) camera at an offset from the island center, above the surface, looking this way. */
    private static void view(Minecraft mc, int x, int y, int z, float yaw, float pitch) {
        server(mc, player -> {
            BlockPos center = InnerIslands.get(player.server).existingCenter(player.getUUID());
            player.connection.teleport(center.getX() + x + 0.5, InnerIsland.SURFACE_Y + y, center.getZ() + z + 0.5, yaw, pitch);
        });
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        Murimblock.LOGGER.info("Inner world tour: {} {}", ok ? "PASS" : "FAIL", what);
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void shoot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> Murimblock.LOGGER.info("Inner world tour shot: {}", message.getString()));
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> Murimblock.LOGGER.error("Inner world tour: could not open {}", WORLD));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(23102026L, false, false),
                WorldPresets::createNormalWorldDimensions, mc.screen);
    }
}
