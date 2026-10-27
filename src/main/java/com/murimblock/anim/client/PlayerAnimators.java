package com.murimblock.anim.client;

import com.murimblock.Murimblock;
import com.murimblock.anim.Animation;
import com.murimblock.anim.Animator;
import com.murimblock.anim.Biped;
import com.murimblock.anim.LivingMotion;
import com.murimblock.anim.MotionSet;
import com.murimblock.anim.Pose;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

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

    private static final Map<AbstractClientPlayer, State> STATES = new WeakHashMap<>();
    private static boolean combat;
    private static LivingMotion forced;

    private PlayerAnimators() {
    }

    /** One player's animator and what it remembers between ticks. */
    private static final class State {
        final Animator animator = new Animator(Biped.ARMATURE, Biped::settleHelpers);
        LivingMotion motion = LivingMotion.IDLE;
        float speed;
        int airTicks;
    }

    /** Dev switch until combat mode exists (step 9): the motion set's combat clips come first. */
    public static void combat(boolean on) {
        combat = on;
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
        return state.animator.pose(partialTick);
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
            state.animator.tick(SkeletalPlayers.slowMotion());
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

        MotionSet set = SkeletalClips.motions(MOTIONS);
        if (set == null) {
            return;
        }
        String id = set.clip(state.motion, combat);
        Animation animation = id == null ? null : SkeletalClips.get(ResourceLocation.parse(id));
        if (animation == null) {
            return;
        }
        float speed = 1;
        if (animation.moveSpeed() > 0 && forced == null) {
            float min = state.motion == LivingMotion.CLIMB ? 0 : 0.4f;
            speed = Mth.clamp(state.speed / animation.moveSpeed(), min, 2.5f);
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
        // One tick off the ground (a step down, a slab) is not a jump.
        if (!player.onGround() && (state.airTicks > 1 || vertical > 2)) {
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
