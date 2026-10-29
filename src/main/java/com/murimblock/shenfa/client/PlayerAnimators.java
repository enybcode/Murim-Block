package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.BipedPoses;
import com.murimblock.shenfa.LivingMotion;
import com.murimblock.shenfa.MotionSet;
import com.murimblock.shenfa.Pose;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Each player's {@link Animator}: every tick it reads what the body does (standing, walking, running, sneaking,
 * jumping, falling, swimming, climbing, riding, sleeping, gliding, spinning) and plays that motion's clip from the
 * biped motion set on the base layer, at the speed the body really moves so the feet do not slide. The renderer
 * lays and turns the body like vanilla (in bed, along an elytra flight, in a riptide spin).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class PlayerAnimators {
    public static final ResourceLocation MOTIONS = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "biped");
    /** Below this many blocks per second the body stands. */
    private static final float MOVING = 0.6f;
    /** Falling further than this (blocks) turns a jump into a fall. */
    private static final float FALLING = 1.2f;
    private static final float SHORT_DROP = 0.6f;
    private static final Set<LivingMotion> LEG_TURNING = EnumSet.of(LivingMotion.WALK, LivingMotion.RUN,
            LivingMotion.SNEAK_WALK);
    private static final float LEG_TURN_MOST = (float) Math.toRadians(80);
    private static final float BACKWARD_FROM = (float) Math.toRadians(110), BACKWARD_UNTIL = (float) Math.toRadians(70);
    /** The share of the way the legs turn toward their target each tick. */
    private static final float LEG_TURN_RATE = 0.35f;

    private static final Map<AbstractClientPlayer, State> STATES = new WeakHashMap<>();
    private static boolean combat;
    private static LivingMotion forced;
    /** Whether the local player's root motion is applied elsewhere (by the fight, which moves the body itself). */
    public static java.util.function.BooleanSupplier rootMotionElsewhere = () -> false;

    private PlayerAnimators() {
    }

    /** One player's animator and what it remembers between ticks. */
    private static final class State {
        final Animator animator = new Animator(Biped.ARMATURE, Biped::settleHelpers);
        LivingMotion motion = LivingMotion.IDLE;
        float speed;
        int airTicks;
        /** The legs' turn toward where the body goes (radians, smoothed), and whether it goes backward. */
        float legTurn, previousLegTurn, legTarget;
        boolean backward;
        /** Combat mode as the server said (the stance clips), and ticks the layers stay frozen (a hit stop). */
        boolean combatMode;
        int frozen;
    }

    /** Dev: every player in combat mode, whatever the server says (the motion set's combat clips come first). */
    public static void combat(boolean on) {
        combat = on;
    }

    /** Whether the local player is in combat mode. */
    public static boolean inCombat() {
        Minecraft mc = Minecraft.getInstance();
        return combat || mc.player != null && inCombat(mc.player);
    }

    public static boolean inCombat(AbstractClientPlayer player) {
        State state = STATES.get(player);
        return combat || state != null && state.combatMode;
    }

    /** Combat mode of {@code player}, as the server says it (or the local player predicts it). */
    public static void mode(AbstractClientPlayer player, boolean on) {
        STATES.computeIfAbsent(player, key -> new State()).combatMode = on;
    }

    /**
     * Whether a clip holds the whole body of a player (seated in meditation): its feet are not planted, vanilla's arm
     * work and held items are left out, and in first person the eye may go as low as the head does.
     */
    public static java.util.function.Predicate<AbstractClientPlayer> bodyHeld = player -> false;

    /** Holds an additive clip of {@code player} at {@code seconds} (see {@link Animator#holdAdditive}). */
    public static void holdAdditive(AbstractClientPlayer player, Animation animation, float seconds) {
        STATES.computeIfAbsent(player, key -> new State()).animator.holdAdditive(animation, seconds);
    }

    public static void stopAdditive(AbstractClientPlayer player, String id) {
        State state = STATES.get(player);
        if (state != null) {
            state.animator.stopAdditive(id);
        }
    }

    /** Plays {@code animation} on a layer of {@code player} alone. */
    public static void play(AbstractClientPlayer player, Animator.Priority priority, Animation animation, float speed,
                            boolean mirrored) {
        STATES.computeIfAbsent(player, key -> new State()).animator.play(priority, animation, speed, mirrored);
    }

    public static void stop(AbstractClientPlayer player, Animator.Priority priority) {
        State state = STATES.get(player);
        if (state != null) {
            state.animator.stop(priority);
        }
    }

    /** The clip on a layer of {@code player}, or null. */
    public static Animation playing(AbstractClientPlayer player, Animator.Priority priority) {
        State state = STATES.get(player);
        return state == null ? null : state.animator.playing(priority);
    }

    /** Seconds into the clip on a layer of {@code player}, or -1. */
    public static float time(AbstractClientPlayer player, Animator.Priority priority) {
        State state = STATES.get(player);
        return state == null ? -1 : state.animator.time(priority);
    }

    /** Freezes {@code player}'s layers for {@code ticks} (a hit stop). */
    public static void freeze(AbstractClientPlayer player, int ticks) {
        State state = STATES.computeIfAbsent(player, key -> new State());
        state.frozen = Math.max(state.frozen, ticks);
    }

    /** Dev: every player plays this motion whatever its body does; null goes back to reading the body. */
    public static void force(LivingMotion motion) {
        forced = motion;
    }

    /** Runs {@code action} on the animator of every player in the world (dev commands and tests). */
    public static void each(Consumer<Animator> action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.players().forEach(player -> {
                State state = STATES.computeIfAbsent(player, key -> new State());
                update(player, state);
                action.accept(state.animator);
            });
        }
    }

    /** Forgets every animator: after a reload their clips are stale. */
    public static void clear() {
        STATES.clear();
    }

    /** Whether a composite layer (a guard, a strike) plays on {@code player}. */
    public static boolean layerPlaying(AbstractClientPlayer player) {
        State state = STATES.get(player);
        return state != null && state.animator.anyLayer();
    }

    /** Whether a clip with root motion plays on {@code player}: its feet follow the clip, not the ground. */
    public static boolean rootMotionPlaying(AbstractClientPlayer player) {
        State state = STATES.get(player);
        return state != null && state.animator.rootMotionPlaying();
    }

    /** The base layer's clip of {@code player}, or null. */
    public static Animation baseAnimation(AbstractClientPlayer player) {
        State state = STATES.get(player);
        return state == null ? null : state.animator.baseAnimation();
    }

    public static LivingMotion motion(AbstractClientPlayer player) {
        State state = STATES.get(player);
        return state == null ? LivingMotion.IDLE : state.motion;
    }

    public static Pose pose(AbstractClientPlayer player, float partialTick) {
        State state = STATES.computeIfAbsent(player, key -> new State());
        if (state.animator.baseAnimation() == null) {
            update(player, state);
        }
        Pose pose = state.animator.pose(partialTick);
        BipedPoses.turnLegs(pose, Mth.lerp(partialTick, state.previousLegTurn, state.legTurn));
        return pose;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || !SkeletalPlayers.enabled()) {
            return;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            State state = STATES.computeIfAbsent(player, key -> new State());
            update(player, state);
            state.animator.tick(state.frozen > 0 ? 0 : SkeletalPlayers.slowMotion());
            if (state.frozen > 0) {
                state.frozen--;
            }
            ClipEvents.run(player, state.animator.events());
            if (player == mc.player && !rootMotionElsewhere.getAsBoolean()) {
                rootMotion(player, state.animator);
            }
            state.previousLegTurn = state.legTurn;
            state.legTurn += Mth.wrapDegrees((float) Math.toDegrees(state.legTarget - state.legTurn)) * Mth.DEG_TO_RAD
                    * LEG_TURN_RATE;
        }
    }

    /**
     * Moves the local player by its clips' root motion, through {@code Entity.move} so walls and the ground stop it;
     * the server hears of it with the player's next position, as of any movement (until the server plays clips).
     * Others' bodies move with the positions the server sends.
     */
    private static void rootMotion(AbstractClientPlayer player, Animator animator) {
        Vector3fc travel = animator.rootMotion();
        if (travel.lengthSquared() > 0) {
            Vector3f move = new Vector3f(travel).mul(SkeletalPlayerRenderer.PLAYER_SCALE / 16 * player.getScale())
                    .rotateY((float) Math.toRadians(-player.yBodyRot));
            player.move(MoverType.SELF, new Vec3(move.x, move.y, move.z));
        }
        if (animator.noGravity()) {
            // Gravity pulls after the move of each tick: holding the fall at zero here keeps the body up.
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, Math.max(0, velocity.y), velocity.z);
            player.resetFallDistance();
        }
    }

    private static void update(AbstractClientPlayer player, State state) {
        double dx = player.getX() - player.xo, dz = player.getZ() - player.zo, dy = player.getY() - player.yo;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz) * 20;
        state.airTicks = player.onGround() ? 0 : state.airTicks + 1;
        state.motion = forced != null ? forced : read(player, state, horizontal, (float) dy * 20);
        float moving = state.motion == LivingMotion.CLIMB ? Math.abs((float) dy * 20) : horizontal;
        // Smoothed, so a speed that wobbles from tick to tick does not make the clip stutter.
        state.speed += (moving - state.speed) * 0.5f;
        legs(player, state, dx, dz);

        MotionSet set = SkeletalClips.motions(MOTIONS);
        if (set == null) {
            return;
        }
        String id = set.clip(state.motion, combat || state.combatMode);
        Animation animation = id == null ? null : SkeletalClips.get(ResourceLocation.parse(id));
        if (animation == null) {
            return;
        }
        float speed = 1;
        if (animation.moveSpeed() > 0 && forced == null) {
            float min = state.motion == LivingMotion.CLIMB ? 0 : 0.4f;
            speed = Mth.clamp(state.speed / animation.moveSpeed(), min, 2.5f);
            if (state.backward) {
                speed = -speed;
            }
        }
        state.animator.motion(state.motion.key());
        state.animator.playBase(animation, speed);
        for (String additive : set.additive()) {
            Animation clip = SkeletalClips.get(ResourceLocation.parse(additive));
            if (clip != null) {
                state.animator.playAdditive(clip);
            }
        }
    }

    /**
     * Where the legs point: walking sideways or back, the hips and legs turn toward where the body goes (at most
     * {@link #LEG_TURN_MOST}) while the chest faces ahead; going back, the step cycle also plays backward. Vanilla turns
     * the body toward the motion too, but only partly, and not at all going straight back.
     */
    private static void legs(AbstractClientPlayer player, State state, double dx, double dz) {
        Vector3f going = new Vector3f((float) dx, 0, (float) dz).rotateY((float) Math.toRadians(player.yBodyRot));
        if (going.lengthSquared() < 1e-4f || !LEG_TURNING.contains(state.motion)) {
            state.backward = false;
            state.legTarget = 0;
            return;
        }
        // The angle from the body's front (+Z) toward its left (+X); with some hysteresis between forward and back.
        float angle = (float) Math.atan2(going.x, going.z);
        float side = Math.abs(angle);
        if (state.backward ? side < BACKWARD_UNTIL : side > BACKWARD_FROM) {
            state.backward = !state.backward;
        }
        float turn = state.backward ? Mth.wrapDegrees((float) Math.toDegrees(angle) - 180) * Mth.DEG_TO_RAD : angle;
        state.legTarget = Mth.clamp(turn, -LEG_TURN_MOST, LEG_TURN_MOST);
    }

    private static LivingMotion read(AbstractClientPlayer player, State state, float horizontal, float vertical) {
        if (player.isSleeping()) {
            return LivingMotion.SLEEP;
        }
        if (player.isFallFlying()) {
            return LivingMotion.GLIDE;
        }
        if (player.isAutoSpinAttack()) {
            return LivingMotion.SPIN;
        }
        if (player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit()) {
            return LivingMotion.RIDE;
        }
        if (player.isVisuallySwimming()) {
            return LivingMotion.SWIM;
        }
        if (player.onClimbable() && !player.onGround()) {
            return LivingMotion.CLIMB;
        }
        // Not a jump: one tick off the ground (Minecraft lifts a body up a stair or slab in a tick, as fast as a jump
        // starts), nor a short drop off a step (under {@link #SHORT_DROP} blocks).
        boolean shortDrop = vertical <= 0 && player.fallDistance < SHORT_DROP;
        if (!player.onGround() && state.airTicks > 1 && !shortDrop) {
            if (vertical > 0) {
                return LivingMotion.JUMP;
            }
            return player.fallDistance > FALLING || state.motion == LivingMotion.FALL ? LivingMotion.FALL
                    : LivingMotion.JUMP;
        }
        boolean moving = horizontal > MOVING;
        if (player.isCrouching()) {
            return moving ? LivingMotion.SNEAK_WALK : LivingMotion.SNEAK;
        }
        if (!moving) {
            return LivingMotion.IDLE;
        }
        return player.isSprinting() ? LivingMotion.RUN : LivingMotion.WALK;
    }
}
