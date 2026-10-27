package com.murimblock.shenfa.client;

import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.FootPlanter;
import com.murimblock.shenfa.LivingMotion;
import com.murimblock.shenfa.Pose;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Keeps each player's feet on the ground ({@link FootPlanter}, one per player): planted while walking, running or
 * sneak-walking, standing put while still, each on the block surface under it (stairs, slabs, paths, snow).
 * Minecraft moves a walking body at 4.3 blocks a second, more than a clip's stride covers at a natural pace, so a
 * clip alone lets the planted foot slide.
 */
public final class FootPlanting {
    private static final Set<LivingMotion> STEPPING = EnumSet.of(LivingMotion.WALK, LivingMotion.RUN,
            LivingMotion.SNEAK_WALK);
    private static final Set<LivingMotion> STANDING = EnumSet.of(LivingMotion.IDLE, LivingMotion.SNEAK);
    /** The ground is looked for this far (blocks) above and below the body's feet: a stair or slab step. */
    private static final double REACH_UP = 0.55, REACH_DOWN = 0.6;

    private static final Map<AbstractClientPlayer, State> STATES = new WeakHashMap<>();

    private FootPlanting() {
    }

    private static final class State {
        final FootPlanter planter = new FootPlanter();
        float lastTime = Float.NaN;
        /** The feet's mode and the game tick when last drawn: steps are ours only while drawn and planting. */
        FootPlanter.Mode mode = FootPlanter.Mode.OFF;
        long drawn = Long.MIN_VALUE / 2;
    }

    /**
     * Places the feet of {@code pose} (changed in place).
     *
     * @param bodyYaw the body's turn as drawn (degrees)
     * @param scale   the player's scale (attribute)
     */
    static void apply(AbstractClientPlayer player, Pose pose, float partialTick, float bodyYaw, float scale) {
        State state = STATES.computeIfAbsent(player, key -> new State());
        float now = player.level().getGameTime() + partialTick;
        float seconds = Float.isNaN(state.lastTime) ? 0 : Math.max(0, Math.min(0.25f, (now - state.lastTime) / 20));
        state.lastTime = now;
        LivingMotion motion = PlayerAnimators.motion(player);
        // Stepping down a stair drops the body for a tick or two: still on its feet.
        boolean grounded = player.onGround() || player.fallDistance < 0.7f && player.getDeltaMovement().y <= 0;
        boolean free = SkeletalPlayers.animated() && grounded && !player.isPassenger() && !player.isVisuallySwimming()
                && !player.isFallFlying() && !player.isSleeping() && !player.isAutoSpinAttack()
                && !PlayerAnimators.rootMotionPlaying(player)
                // A reaction (a stagger, a knockdown) throws the legs about: the clip places them.
                && PlayerAnimators.playing(player, Animator.Priority.HIGHEST) == null;
        FootPlanter.Mode mode = !free ? FootPlanter.Mode.OFF : STEPPING.contains(motion) ? FootPlanter.Mode.STEP
                : STANDING.contains(motion) ? FootPlanter.Mode.STAND : FootPlanter.Mode.OFF;
        Vector3f at = new Vector3f((float) Mth.lerp(partialTick, player.xo, player.getX()),
                (float) Mth.lerp(partialTick, player.yo, player.getY()),
                (float) Mth.lerp(partialTick, player.zo, player.getZ()));
        // Where the body goes, in model space (the model faces +Z; the world is the model turned by the body's yaw).
        Vector3f heading = new Vector3f((float) (player.getX() - player.xo), 0, (float) (player.getZ() - player.zo))
                .rotateY((float) Math.toRadians(bodyYaw));
        heading = heading.lengthSquared() < 1e-8f ? new Vector3f(0, 0, 1) : heading.normalize();
        Level level = player.level();
        state.planter.apply(pose, at, (float) Math.toRadians(-bodyYaw), SkeletalPlayerRenderer.PLAYER_SCALE * scale / 16,
                seconds, mode, heading, (x, z, near) -> ground(level, x, z, near));
        state.mode = mode;
        state.drawn = level.getGameTime();
        for (int side = 0; side < 2; side++) {
            if (state.planter.landed(side) && steps(player, mode)) {
                Vector3fc sole = state.planter.planted(side);
                // A shuffle in place is softer than a stride.
                Footsteps.step(player, new Vec3(sole.x(), sole.y(), sole.z()), mode == FootPlanter.Mode.STAND ? 0.5f : 1);
            }
        }
    }

    /**
     * Whether Shenfa plays {@code player}'s step sounds (as its feet land) in place of vanilla's: it was drawn in the
     * last ticks with its feet planting. Sneaking makes no step sound in vanilla, nor here.
     */
    public static boolean ownsSteps(AbstractClientPlayer player) {
        State state = STATES.get(player);
        return state != null && player.level().getGameTime() - state.drawn <= 2 && steps(player, state.mode);
    }

    private static boolean steps(AbstractClientPlayer player, FootPlanter.Mode mode) {
        return mode != FootPlanter.Mode.OFF && !player.isDiscrete() && !player.isInWater();
    }

    /** For checks: the state of {@code player}'s feet, or "-". */
    public static String debug(AbstractClientPlayer player) {
        State state = STATES.get(player);
        return state == null ? "-" : state.planter.debug();
    }

    /** The top of the highest collision surface under {@code x, z} within a step of {@code near}; NaN when none. */
    public static float ground(Level level, float x, float z, float near) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = Mth.floor(near + REACH_UP), bottom = Mth.floor(near - REACH_DOWN);
        for (int y = top; y >= bottom; y--) {
            pos.set(Mth.floor(x), y, Mth.floor(z));
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            double localX = x - pos.getX(), localZ = z - pos.getZ(), highest = Double.NaN;
            for (AABB box : shape.toAabbs()) {
                if (localX >= box.minX && localX <= box.maxX && localZ >= box.minZ && localZ <= box.maxZ
                        && (Double.isNaN(highest) || box.maxY > highest)) {
                    highest = box.maxY;
                }
            }
            if (!Double.isNaN(highest)) {
                // The first surface found from above is the ground, if it is within a step; one higher is a wall or a
                // step too tall, and whatever lies under it is buried, not ground.
                float surface = (float) (y + highest);
                return surface <= near + REACH_UP && surface >= near - REACH_DOWN ? surface : Float.NaN;
            }
        }
        return Float.NaN;
    }
}
