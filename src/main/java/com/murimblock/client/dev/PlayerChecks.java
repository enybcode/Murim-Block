package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.LivingMotion;
import com.murimblock.shenfa.Pose;
import com.murimblock.shenfa.client.FirstPerson;
import com.murimblock.shenfa.client.FootPlanting;
import com.murimblock.shenfa.client.PlayerAnimators;
import com.murimblock.shenfa.client.SkeletalPlayerRenderer;
import com.murimblock.shenfa.client.SkeletalPlayers;
import com.murimblock.client.dev.EngineTest.Step;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The real player, played through the real keys like a person would, filmed by a camera kept at a fixed angle from
 * it: standing, walking, running, sneaking, jumping, falling, a ladder, swimming, a boat, a bed, an elytra flight, a
 * riptide spin, a bow, a crossbow, a shield, sword swings, a trident, a spyglass, eating, and the boxing stance with
 * the guard and a jab. For each part it checks the motion read from the body, measures the largest turn of a joint
 * from one drawn frame to the next (a pop shows as a spike), and saves a frame every two ticks
 * ({@code screenshots/player/<part>/}).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class PlayerChecks {
    /** Far from the other checks' lineup, so the floor, pool and ladder built here never get in their way. */
    private static final int AREA = 300;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static final BlockPos BED = BlockPos.containing(AREA - 4, Y, 8);
    private static final ResourceLocation GUARD = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "fists/guard");
    private static final ResourceLocation JAB = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "fists/jab");
    /** Camera angles from the player's facing (degrees): its front from the left, its back from the left, its side. */
    private static final float FRONT = 140, BACK = 35, SIDE = 90;

    /**
     * One part of the tour.
     *
     * @param view     where the camera looks from, relative to the player's facing at the start
     * @param start    runs when the part starts (server work arrives a tick or two later)
     * @param tick     runs every tick with the tick number in the part
     * @param expected the motion the body should mostly show in the second half, or null to only record it
     */
    private record Part(String name, int ticks, float view, Consumer<Minecraft> start, BiConsumer<Minecraft, Integer> tick,
                        LivingMotion expected) {
    }

    private static final List<Part> PARTS = new ArrayList<>();
    private static int part = -1;
    private static int ticks;
    private static float facing;
    private static final Map<LivingMotion, Integer> SEEN = new EnumMap<>(LivingMotion.class);
    private static final List<String> REPORT = new ArrayList<>();
    private static Pose lastPose;
    /** Game time (ticks) of the last probed frame, and whether a shot was saved since (saving stalls a few frames). */
    private static float lastTime;
    private static boolean shotSince;
    private static float largestTurn;
    private static String largestWhere = "";
    /** Frames where a joint turned faster than {@link #SPIKE} (at 60 fps), with what the body was doing. */
    private static final List<String> spikes = new ArrayList<>();
    private static final float SPIKE = 40;
    /** The foot on the ground: its last place in the world, which foot, and how far it slid while planted. */
    private static Vector3f plantedAt;
    private static int plantedFoot = -1;
    private static float slide, slideTime, slideMost;
    /** How far the foot on the ground stands off the block surface under it (pixels). */
    private static float groundOff, groundMost;
    private static int groundSamples;
    private static int frames;
    private static long lastShotTick = -1;
    private static int shot;

    private PlayerChecks() {
    }

    static List<Step> steps() {
        buildParts();
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("player setup", PlayerChecks::setup));
        steps.add(new Step("player tour", mc -> {
            part = -1;
            next(mc);
            EngineTest.busy = () -> part >= 0 && part < PARTS.size();
        }));
        steps.add(new Step("player report", mc -> {
            EngineTest.busy = () -> false;
            REPORT.forEach(line -> Murimblock.LOGGER.info("Engine test player: {}", line));
            keys(mc, false, false, false, false, false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.fov().set(70);
            mc.options.hideGui = true;
            SkeletalPlayerRenderer.poseProbe = null;
            PlayerAnimators.combat(false);
            SkeletalPlayers.enable(false);
        }));
        return steps;
    }

    private static void buildParts() {
        PARTS.clear();
        PARTS.add(new Part("idle", 40, FRONT, mc -> at(mc, X, Y, Z, 0), none(), LivingMotion.IDLE));
        PARTS.add(new Part("walk", 40, FRONT, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, true, false, false, false, false), LivingMotion.WALK));
        PARTS.add(new Part("walk_side", 40, SIDE, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, true, false, false, false, false), LivingMotion.WALK));
        PARTS.add(new Part("run", 40, FRONT, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, true, true, false, false, false), LivingMotion.RUN));
        PARTS.add(new Part("run_side", 40, SIDE, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, true, true, false, false, false), LivingMotion.RUN));
        PARTS.add(new Part("walk_back", 40, SIDE, mc -> at(mc, X, Y, Z + 10, 0),
                (mc, t) -> keys(mc, false, false, false, false, false, true, false), LivingMotion.WALK));
        PARTS.add(new Part("strafe", 40, FRONT, mc -> at(mc, X - 4, Y, Z + 20, 0),
                (mc, t) -> keys(mc, false, false, false, false, false, false, true), LivingMotion.WALK));
        PARTS.add(new Part("stairs_up", 40, SIDE, mc -> at(mc, X + 5, Y, Z, 0),
                (mc, t) -> keys(mc, true, false, false, false, false), LivingMotion.WALK));
        PARTS.add(new Part("stairs_down", 40, SIDE, mc -> at(mc, X + 5, Y + 3, Z + 10, 180),
                (mc, t) -> keys(mc, true, false, false, false, false), LivingMotion.WALK));
        PARTS.add(new Part("slabs", 40, SIDE, mc -> at(mc, X - 5, Y, Z, 0),
                (mc, t) -> keys(mc, true, false, false, false, false), LivingMotion.WALK));
        // On a slab's edge: the right foot on the slab, the left one half a block lower.
        PARTS.add(new Part("slab_edge", 30, FRONT, mc -> at(mc, AREA - 2.95, Y + 0.5, Z + 2.5, 0), none(),
                LivingMotion.IDLE));
        PARTS.add(new Part("turn", 60, FRONT, mc -> at(mc, X, Y, Z, 0), (mc, t) -> {
            keys(mc, false, false, false, false, false);
            mc.player.setYRot(mc.player.getYRot() + 3);
        }, LivingMotion.IDLE));
        PARTS.add(new Part("walk_stop_walk", 60, SIDE, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, t < 20 || t >= 35, t >= 45, false, false, false), null));
        PARTS.add(new Part("sneak", 30, FRONT, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, false, false, true, false, false), LivingMotion.SNEAK));
        PARTS.add(new Part("sneak_walk", 40, SIDE, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, true, false, true, false, false), LivingMotion.SNEAK_WALK));
        PARTS.add(new Part("jump", 40, SIDE, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, false, false, false, t % 20 < 2, false), null));
        PARTS.add(new Part("run_jump", 40, SIDE, mc -> at(mc, X, Y, Z, 0),
                (mc, t) -> keys(mc, true, true, false, true, false), null));
        PARTS.add(new Part("fall", 30, SIDE, mc -> at(mc, X, Y + 40, Z, 0), none(), LivingMotion.FALL));
        PARTS.add(new Part("land", 40, SIDE, mc -> {
        }, none(), null));
        PARTS.add(new Part("ladder", 40, BACK, mc -> at(mc, X, Y, -1.3, 180),
                (mc, t) -> keys(mc, true, false, false, false, false), LivingMotion.CLIMB));
        PARTS.add(new Part("swim", 40, SIDE, mc -> at(mc, X + 11, Y - 3.5, 2, 0),
                (mc, t) -> keys(mc, true, true, false, false, false), LivingMotion.SWIM));
        PARTS.add(new Part("boat", 30, FRONT, mc -> {
            at(mc, X, Y, Z + 6, 0);
            EngineTest.server(mc, player -> {
                Boat boat = new Boat(EntityType.BOAT, player.serverLevel());
                boat.moveTo(X, Y, Z + 6, 0, 0);
                player.serverLevel().addFreshEntity(boat);
                player.startRiding(boat, true);
            });
        }, none(), LivingMotion.RIDE));
        PARTS.add(new Part("bed", 30, FRONT, mc -> EngineTest.server(mc, player -> {
            if (player.getVehicle() != null) {
                var boat = player.getVehicle();
                player.stopRiding();
                boat.discard();
            }
            // Night: by day the server wakes a sleeper at once.
            player.serverLevel().setDayTime(18000);
            player.startSleeping(BED);
        }), none(), LivingMotion.SLEEP));
        PARTS.add(new Part("elytra", 50, SIDE, mc -> {
            EngineTest.server(mc, player -> {
                player.stopSleeping();
                player.serverLevel().setDayTime(6000);
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            });
            at(mc, X, Y + 50, Z, 0);
        }, (mc, t) -> keys(mc, false, false, false, t == 6, false), LivingMotion.GLIDE));
        PARTS.add(new Part("riptide", 25, SIDE, mc -> {
            EngineTest.server(mc, player -> player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY));
            hold(mc, new ItemStack(Items.TRIDENT), ItemStack.EMPTY);
            at(mc, X, Y + 5, Z, 0);
        }, (mc, t) -> {
            if (t == 4) {
                // The spin flag is set by the server; the client starts its own spin for the motion.
                EngineTest.server(mc, player -> player.startAutoSpinAttack(20, 8, new ItemStack(Items.TRIDENT)));
                mc.player.startAutoSpinAttack(20, 8, new ItemStack(Items.TRIDENT));
                mc.player.setDeltaMovement(0, 0.6, 1.2);
            }
        }, null));
        PARTS.add(new Part("bow", 40, FRONT, mc -> {
            hold(mc, new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 64));
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> use(mc, t, 4, 34, InteractionHand.MAIN_HAND), null));
        PARTS.add(new Part("crossbow", 50, FRONT, mc -> {
            hold(mc, new ItemStack(Items.CROSSBOW), new ItemStack(Items.ARROW, 64));
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> use(mc, t, 4, 34, InteractionHand.MAIN_HAND), null));
        PARTS.add(new Part("shield", 40, FRONT, mc -> {
            hold(mc, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD));
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> use(mc, t, 4, 34, InteractionHand.OFF_HAND), null));
        PARTS.add(new Part("swing_walk", 40, FRONT, mc -> {
            hold(mc, new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY);
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> {
            keys(mc, true, false, false, false, false);
            if (t % 8 == 2) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }, LivingMotion.WALK));
        PARTS.add(new Part("trident", 30, FRONT, mc -> {
            hold(mc, new ItemStack(Items.TRIDENT), ItemStack.EMPTY);
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> use(mc, t, 4, 100, InteractionHand.MAIN_HAND), null));
        PARTS.add(new Part("spyglass", 30, FRONT, mc -> {
            hold(mc, new ItemStack(Items.SPYGLASS), ItemStack.EMPTY);
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> use(mc, t, 4, 100, InteractionHand.MAIN_HAND), null));
        PARTS.add(new Part("eat", 40, FRONT, mc -> {
            hold(mc, new ItemStack(Items.GOLDEN_APPLE, 16), ItemStack.EMPTY);
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> use(mc, t, 4, 100, InteractionHand.MAIN_HAND), null));
        PARTS.add(new Part("stance", 40, FRONT, mc -> {
            hold(mc, ItemStack.EMPTY, ItemStack.EMPTY);
            at(mc, X, Y, Z, 0);
            PlayerAnimators.combat(true);
        }, none(), LivingMotion.IDLE));
        PARTS.add(new Part("guard_walk", 40, FRONT, mc -> {
            at(mc, X, Y, Z, 0);
            SkeletalPlayers.play(GUARD, Animator.Priority.MIDDLE, 1, false);
        }, (mc, t) -> keys(mc, true, false, false, false, false), LivingMotion.WALK));
        PARTS.add(new Part("jabs", 40, FRONT, mc -> {
            SkeletalPlayers.stop(Animator.Priority.MIDDLE);
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> {
            if (t % 12 == 4) {
                SkeletalPlayers.play(JAB, Animator.Priority.HIGH, 1, t % 24 == 16);
            }
        }, LivingMotion.IDLE));
        // First person (parts named fp_): the body seen from its eyes, vanilla's hands or ours when a layer plays.
        PARTS.add(new Part("fp_look_down", 40, 0, mc -> {
            PlayerAnimators.combat(false);
            hold(mc, ItemStack.EMPTY, ItemStack.EMPTY);
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> look(mc, 70), LivingMotion.IDLE));
        PARTS.add(new Part("fp_walk_down", 40, 0, mc -> at(mc, X, Y, Z, 0), (mc, t) -> {
            look(mc, 55);
            keys(mc, true, false, false, false, false);
        }, LivingMotion.WALK));
        PARTS.add(new Part("fp_sword_walk", 40, 0, mc -> {
            hold(mc, new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY);
            at(mc, X, Y, Z, 0);
        }, (mc, t) -> {
            look(mc, 10);
            keys(mc, true, false, false, false, false);
            if (t % 10 == 5) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }, LivingMotion.WALK));
        PARTS.add(new Part("fp_guard_walk", 40, 0, mc -> {
            hold(mc, ItemStack.EMPTY, ItemStack.EMPTY);
            PlayerAnimators.combat(true);
            at(mc, X, Y, Z, 0);
            SkeletalPlayers.play(GUARD, Animator.Priority.MIDDLE, 1, false);
        }, (mc, t) -> {
            look(mc, 15);
            keys(mc, true, false, false, false, false);
        }, LivingMotion.WALK));
        // The combat stance's raised arms up close, looking down on the bent elbows (their sides once z-fought,
        // and the slim arms' hinge sampled two texel rows in a hatch).
        for (int pitch : new int[] {35, 60}) {
            PARTS.add(new Part("fp_stance_down" + pitch, 30, 0, mc -> {
                SkeletalPlayers.stop(Animator.Priority.MIDDLE);
                at(mc, X, Y, Z, 0);
            }, (mc, t) -> {
                look(mc, pitch);
                keys(mc, false, false, false, false, false);
            }, LivingMotion.IDLE));
        }
        for (int fov : new int[] {70, 30, 110}) {
            PARTS.add(new Part("fp_jabs_fov" + fov, 40, 0, mc -> {
                SkeletalPlayers.stop(Animator.Priority.MIDDLE);
                mc.options.fov().set(fov);
                at(mc, X, Y, Z, 0);
            }, (mc, t) -> {
                look(mc, 0);
                if (t % 12 == 4) {
                    SkeletalPlayers.play(JAB, Animator.Priority.HIGH, 1, t % 24 == 16);
                }
            }, LivingMotion.IDLE));
        }
    }

    private static void look(Minecraft mc, float pitch) {
        mc.player.setXRot(pitch);
        mc.player.xRotO = pitch;
    }

    private static BiConsumer<Minecraft, Integer> none() {
        return (mc, t) -> keys(mc, false, false, false, false, false);
    }

    /** Holds the use key from tick {@code from} to {@code to}, starting the use on the first tick. */
    private static void use(Minecraft mc, int t, int from, int to, InteractionHand hand) {
        keys(mc, false, false, false, false, t >= from && t < to);
        if (t == from) {
            mc.gameMode.useItem(mc.player, hand);
        }
    }

    private static void keys(Minecraft mc, boolean forward, boolean sprint, boolean sneak, boolean jump, boolean use) {
        keys(mc, forward, sprint, sneak, jump, use, false, false);
    }

    private static void keys(Minecraft mc, boolean forward, boolean sprint, boolean sneak, boolean jump, boolean use,
                             boolean back, boolean left) {
        mc.options.keyDown.setDown(back);
        mc.options.keyLeft.setDown(left);
        mc.options.keyUp.setDown(forward);
        mc.options.keySprint.setDown(sprint);
        mc.options.keyShift.setDown(sneak);
        mc.options.keyJump.setDown(jump);
        mc.options.keyUse.setDown(use);
        if (!sprint && mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    private static void hold(Minecraft mc, ItemStack main, ItemStack off) {
        EngineTest.server(mc, player -> {
            player.setItemSlot(EquipmentSlot.MAINHAND, main);
            player.setItemSlot(EquipmentSlot.OFFHAND, off);
        });
    }

    private static void at(Minecraft mc, double x, double y, double z, float yaw) {
        facing = yaw;
        EngineTest.server(mc, player -> player.connection.teleport(x, y, z, yaw, 0));
        mc.player.setYRot(yaw);
        mc.player.setXRot(0);
        mc.player.setDeltaMovement(0, 0, 0);
    }

    /** A floor, a pool, a ladder on a wall and a bed, high in the sky; the player on foot (not flying). */
    private static void setup(Minecraft mc) {
        // The tour plays without anyone at the keyboard: losing the focus must not pause it.
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = true;
        mc.options.cloudStatus().set(CloudStatus.OFF);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        SkeletalPlayers.pose(SkeletalPlayers.ANIMATED);
        SkeletalPlayers.enable(true);
        PlayerAnimators.force(null);
        EngineTest.server(mc, player -> {
            ServerLevel level = player.serverLevel();
            level.setDayTime(6000);
            fill(level, -8, -6, 8, 40, (int) Y - 1, (int) Y - 1, Blocks.SMOOTH_STONE.defaultBlockState());
            fill(level, 9, -1, 15, 13, (int) Y - 6, (int) Y - 1, Blocks.SMOOTH_STONE.defaultBlockState());
            fill(level, 10, 0, 14, 12, (int) Y - 5, (int) Y - 1, Blocks.WATER.defaultBlockState());
            fill(level, -2, -3, 2, -3, (int) Y, (int) Y + 7, Blocks.SMOOTH_STONE.defaultBlockState());
            fill(level, 0, -2, 0, -2, (int) Y, (int) Y + 7,
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
            BlockState head = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH)
                    .setValue(BedBlock.PART, BedPart.HEAD);
            level.setBlockAndUpdate(BED, head);
            level.setBlockAndUpdate(BED.south(), head.setValue(BedBlock.PART, BedPart.FOOT));
            // A staircase going up south (x 4 to 6), three steps to a landing; bottom slabs and their edge (x -6 to -4).
            BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            for (int step = 0; step < 3; step++) {
                if (step > 0) {
                    fill(level, 4, 2 + step, 6, 2 + step, (int) Y, (int) Y + step - 1,
                            Blocks.SMOOTH_STONE.defaultBlockState());
                }
                fill(level, 4, 2 + step, 6, 2 + step, (int) Y + step, (int) Y + step, stairs);
            }
            fill(level, 4, 5, 6, 12, (int) Y, (int) Y + 2, Blocks.SMOOTH_STONE.defaultBlockState());
            BlockState slab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
            fill(level, -6, 2, -4, 3, (int) Y, (int) Y, slab);
            fill(level, -6, 6, -4, 7, (int) Y, (int) Y, slab);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            // Effects left from an earlier run (the golden apple) would cloud the shots with particles.
            player.removeAllEffects();
        });
        mc.player.getAbilities().flying = false;
        SkeletalPlayerRenderer.poseProbe = PlayerChecks::probe;
    }

    /** Fills a box, x relative to the area. */
    private static void fill(ServerLevel level, int x0, int z0, int x1, int z1, int y0, int y1, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(AREA + x0, y0, z0, AREA + x1, y1, z1)) {
            level.setBlockAndUpdate(pos, state);
        }
    }

    private static void next(Minecraft mc) {
        if (part >= 0) {
            finish();
        }
        part++;
        ticks = 0;
        SEEN.clear();
        lastPose = null;
        largestTurn = 0;
        largestWhere = "";
        spikes.clear();
        plantedAt = null;
        plantedFoot = -1;
        slide = slideTime = slideMost = 0;
        groundOff = groundMost = 0;
        groundSamples = 0;
        frames = 0;
        SkeletalPlayers.enable(true);
        shot = 0;
        if (part < PARTS.size()) {
            keys(mc, false, false, false, false, false);
            boolean firstPerson = PARTS.get(part).name().startsWith("fp_");
            mc.options.setCameraType(firstPerson ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
            // In first person the hands show only with the interface (as in play).
            mc.options.hideGui = !firstPerson;
            if (!firstPerson) {
                mc.options.fov().set(70);
            }
            FirstPerson.resetNearest();
            Murimblock.LOGGER.info("Engine test player: part {}", PARTS.get(part).name());
            PARTS.get(part).start().accept(mc);
        }
    }

    private static void finish() {
        Part done = PARTS.get(part);
        LivingMotion most = SEEN.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                .orElse(null);
        float nearest = FirstPerson.resetNearest();
        String line = done.name() + ": motions " + SEEN + ", largest joint turn per frame at 60 fps "
                + String.format("%.1f°", largestTurn) + " (" + largestWhere + ") over " + frames + " frames"
                + (slideTime > 0 ? String.format(", planted foot slides %.2f blocks/s on average (%.2f at most)",
                slide / slideTime, slideMost) : "")
                + (groundSamples > 0 ? String.format(", foot off its ground %.2f px on average (%.2f at most)",
                groundOff / groundSamples, groundMost) : "")
                + (nearest < Float.MAX_VALUE ? String.format(", nearest hand %.2f blocks from the eye", nearest) : "");
        REPORT.add(line);
        spikes.forEach(spike -> REPORT.add("  spike " + spike));
        if (done.expected() != null) {
            EngineTest.check("the real player " + done.name() + " plays " + done.expected().key() + " (" + SEEN + ")",
                    most == done.expected());
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (part < 0 || part >= PARTS.size() || mc.player == null || mc.isPaused()) {
            return;
        }
        Part current = PARTS.get(part);
        current.tick().accept(mc, ticks);
        if (ticks >= current.ticks() / 2) {
            SEEN.merge(PlayerAnimators.motion(mc.player), 1, Integer::sum);
        }
        if (++ticks >= current.ticks()) {
            next(mc);
        }
    }

    /** The camera: at a fixed angle from where the player faced when the part started, 5 blocks away, looking down. */
    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (part >= 0 && part < PARTS.size() && !PARTS.get(part).name().startsWith("fp_")) {
            event.setYaw(facing + PARTS.get(part).view());
            event.setPitch(18);
        }
    }

    @SubscribeEvent
    public static void distance(CalculateDetachedCameraDistanceEvent event) {
        if (part >= 0 && part < PARTS.size() && !PARTS.get(part).name().startsWith("fp_")) {
            event.setDistance(5);
        }
    }

    private static void probe(AbstractClientPlayer player, Pose pose) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || part < 0 || part >= PARTS.size()) {
            return;
        }
        float now = mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        float elapsed = now - lastTime;
        if (lastPose != null && ticks > 3 && !shotSince && elapsed > 0) {
            for (int i = 0; i < pose.size(); i++) {
                float dot = Math.abs(new Quaternionf(pose.get(i).rotation()).dot(new Quaternionf(lastPose.get(i).rotation())));
                // As if frames came at 60 per second: a tick is three of them.
                float turn = (float) Math.toDegrees(2 * Math.acos(Math.min(1, dot))) / (elapsed * 3);
                if (turn > SPIKE && spikes.size() < 12) {
                    spikes.add(String.format("t%d+%.2f %s %.0f° (raw %.0f° in %.3f ticks) %s y%.3f %s", ticks,
                            mc.getTimer().getGameTimeDeltaPartialTick(false),
                            com.murimblock.shenfa.Biped.ARMATURE.joint(i).name(), turn,
                            turn * elapsed * 3, elapsed, PlayerAnimators.motion(player).key(), player.getY(),
                            FootPlanting.debug(player)));
                }
                if (turn > largestTurn) {
                    largestTurn = turn;
                    var base = PlayerAnimators.baseAnimation(player);
                    largestWhere = com.murimblock.shenfa.Biped.ARMATURE.joint(i).name() + " at tick " + ticks + ", "
                            + PlayerAnimators.motion(player).key() + ", " + (base == null ? "-" : base.id())
                            + (player.isUsingItem() ? ", using " + player.getUseItem().getItem() : "")
                            + (player.swinging ? ", swinging" : "") + ", feet " + FootPlanting.debug(player)
                            + (player.onGround() ? "" : ", in the air");
                }
            }
            frames++;
        }
        footSlide(player, pose, mc.getTimer().getGameTimeDeltaPartialTick(false), elapsed);
        lastPose = pose.copy();
        lastTime = now;
        shotSince = false;
    }

    /**
     * How the feet keep to the ground, while walking, running or standing: the foot nearest its ground (within half a
     * pixel) should stay where it is in the world while the body moves over it (its slide, in blocks a
     * second), and stand on the block surface under it (its height off that surface, in pixels).
     */
    private static void footSlide(AbstractClientPlayer player, Pose pose, float partialTick, float elapsed) {
        LivingMotion motion = PlayerAnimators.motion(player);
        if (!EnumSet.of(LivingMotion.WALK, LivingMotion.RUN, LivingMotion.SNEAK_WALK, LivingMotion.IDLE,
                LivingMotion.SNEAK).contains(motion) || !player.onGround()) {
            plantedAt = null;
            return;
        }
        float toBlocks = 0.9375f / 16;
        var joints = pose.toModelMatrices(Biped.ARMATURE);
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        Vector3f at = new Vector3f((float) Mth.lerp(partialTick, player.xo, player.getX()),
                (float) Mth.lerp(partialTick, player.yo, player.getY()),
                (float) Mth.lerp(partialTick, player.zo, player.getZ()));
        int chosen = -1;
        Vector3f chosenPoint = null;
        float chosenOff = Float.MAX_VALUE;
        for (String leg : new String[] {"Leg_L", "Leg_R"}) {
            int index = Biped.ARMATURE.get(leg).index();
            Vector3f foot = joints[index].transformPosition(new Vector3f(0, -6, 0)).mul(toBlocks)
                    .rotateY((float) Math.toRadians(-yaw)).add(at);
            float ground = FootPlanting.ground(player.level(), foot.x, foot.z, at.y);
            float off = Float.isNaN(ground) ? Float.MAX_VALUE : (foot.y - ground) / toBlocks;
            if (Math.abs(off) < Math.abs(chosenOff)) {
                chosen = index;
                chosenPoint = foot;
                chosenOff = off;
            }
        }
        if (chosenPoint == null || chosenOff > 0.5f) {
            plantedAt = null;
            return;
        }
        if (!shotSince && ticks > 5) {
            groundOff += Math.abs(chosenOff);
            groundSamples++;
            groundMost = Math.max(groundMost, Math.abs(chosenOff));
        }
        Vector3f world = new Vector3f(chosenPoint.x, 0, chosenPoint.z);
        if (plantedAt != null && plantedFoot == chosen && elapsed > 0 && !shotSince && ticks > 5) {
            float seconds = elapsed / 20;
            float speed = world.distance(plantedAt) / seconds;
            if (speed > 20) {
                // A teleport, not a step.
                plantedAt = null;
                return;
            }
            slide += speed * seconds;
            slideTime += seconds;
            slideMost = Math.max(slideMost, speed);
        }
        plantedAt = world;
        plantedFoot = chosen;
    }

    /** One shot every two ticks of the part. */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (part < 0 || part >= PARTS.size() || mc.level == null) {
            return;
        }
        long tick = mc.level.getGameTime();
        if (tick == lastShotTick || tick % 2 != 0 || ticks < 2) {
            return;
        }
        lastShotTick = tick;
        shotSince = true;
        if (PARTS.get(part).name().equals("elytra")) {
            // Every other shot is vanilla's, to compare the flight a tenth of a second apart.
            SkeletalPlayers.enable(!SkeletalPlayers.enabled());
        }
        SkeletalRenderChecks.saveTo(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()),
                "player/" + PARTS.get(part).name() + "/frame_" + String.format("%03d", shot++));
    }
}
