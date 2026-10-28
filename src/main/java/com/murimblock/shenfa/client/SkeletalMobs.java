package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimClientConfig;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.BipedPoses;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.LivingMotion;
import com.murimblock.shenfa.MotionSet;
import com.murimblock.shenfa.Pose;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Quaternionf;

/**
 * Vanilla mobs drawn with a look of ours on the biped's skeleton ({@code docs/mobs/}): the same mob (id, spawns,
 * drops, behaviour), a new model, texture and motion. The zombie is the Disciple déchu.
 * <p>
 * A look with clips of its own ({@code skeletal/animations/<set>.gltf}, motions in {@code skeletal/motions/<set>.json})
 * moves with them: its living motions, its combat stance while it is aggressive, a strike when it swings (every third
 * one its double blow), a flinch when it is hurt, its death. A look without clips moves with the biped's motions and
 * reaches and swings like vanilla's zombie. {@code shenfaMobs = false} in the client config brings vanilla's models back.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalMobs {
    /** A mob's look: its model (skeletal/), texture, glowing parts (or null) and clip set (or null). */
    public record Look(ResourceLocation model, ResourceLocation texture, ResourceLocation glow, String clips) {
        ResourceLocation clip(String name) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, clips + "/" + name);
        }

        ResourceLocation motions() {
            return clips == null ? PlayerAnimators.MOTIONS : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, clips);
        }
    }

    public static final Look DISCIPLE = new Look(id("mobs/disciple"), id("textures/entity/disciple.png"),
            id("textures/entity/disciple_glow.png"), "disciple");
    private static final Map<EntityType<?>, Look> LOOKS = Map.of(EntityType.ZOMBIE, DISCIPLE);
    private static final float MOVING = 0.6f, RUNNING = 4.5f;
    /** Every this many strikes, the double blow. */
    private static final int DOUBLE_EVERY = 3;
    private static final Map<Mob, State> STATES = new WeakHashMap<>();
    private static Boolean enabled;
    /** Dev: told of each move a mob starts (strike, double, flinch, death). */
    public static java.util.function.BiConsumer<Mob, String> moveProbe;

    private SkeletalMobs() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    private static final class State {
        final Animator animator = new Animator(Biped.ARMATURE, Biped::settleHelpers);
        LivingMotion motion = LivingMotion.IDLE;
        float speed;
        int airTicks;
        /** How far the arms are raised toward the target (0 to 1), now and a tick ago (looks without clips). */
        float raise, previousRaise;
        boolean swinging, dying;
        int hurtTime, strikes;
    }

    /** Whether mobs are drawn with our looks (the client config, unless a dev command overrides it). */
    public static boolean enabled() {
        return enabled != null ? enabled : MurimClientConfig.shenfaMobs();
    }

    public static void enable(Boolean on) {
        enabled = on;
    }

    /** The look {@code entity} wears, or null to draw it as vanilla does. */
    public static Look look(Entity entity) {
        return enabled() ? LOOKS.get(entity.getType()) : null;
    }

    /** For checks: the clip {@code mob} plays at {@code priority}, or null. */
    public static Animation playing(Mob mob, Animator.Priority priority) {
        State state = STATES.get(mob);
        return state == null ? null : state.animator.playing(priority);
    }

    /** For checks: the clip of {@code mob}'s living motion, or null. */
    public static Animation baseAnimation(Mob mob) {
        State state = STATES.get(mob);
        return state == null ? null : state.animator.baseAnimation();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.ZOMBIE, context -> new SkeletalMobRenderer<>(context,
                new net.minecraft.client.renderer.entity.ZombieRenderer(context)));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || !enabled()) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Mob mob && LOOKS.containsKey(mob.getType())) {
                Look look = LOOKS.get(mob.getType());
                State state = STATES.computeIfAbsent(mob, key -> new State());
                update(mob, look, state);
                if (look.clips() != null) {
                    moves(mob, look, state);
                }
                state.animator.tick(1);
                state.previousRaise = state.raise;
                state.raise += ((mob.isAggressive() ? 1 : 0) - state.raise) * 0.25f;
            }
        }
    }

    /** Strikes, flinches and the death, as the mob swings, is hurt and dies. */
    private static void moves(Mob mob, Look look, State state) {
        if (mob.deathTime > 0) {
            if (!state.dying) {
                state.dying = true;
                state.animator.stop(Animator.Priority.MIDDLE);
                state.animator.stop(Animator.Priority.LOW);
                play(mob, state, look, Animator.Priority.HIGHEST, "death");
            }
            return;
        }
        if (mob.swinging && !state.swinging) {
            state.strikes++;
            play(mob, state, look, Animator.Priority.MIDDLE, state.strikes % DOUBLE_EVERY == 0 ? "double" : "strike");
        }
        state.swinging = mob.swinging;
        if (mob.hurtTime > state.hurtTime) {
            play(mob, state, look, Animator.Priority.LOW, "flinch");
        }
        state.hurtTime = mob.hurtTime;
    }

    private static void play(Mob mob, State state, Look look, Animator.Priority priority, String clip) {
        if (moveProbe != null) {
            moveProbe.accept(mob, clip);
        }
        Animation animation = SkeletalClips.get(look.clip(clip));
        if (animation != null) {
            state.animator.play(priority, animation, 1, false);
        }
    }

    /** Whether {@code mob}'s look falls by a clip of its own when it dies (vanilla's fall to the side is left out). */
    static boolean diesByClip(Mob mob) {
        Look look = look(mob);
        return look != null && look.clips() != null && SkeletalClips.get(look.clip("death")) != null;
    }

    /** {@code mob}'s pose this frame: its motion and moves, its head's look. */
    public static Pose pose(Mob mob, float partialTick, float netHeadYaw, float headPitch) {
        Look look = LOOKS.get(mob.getType());
        State state = STATES.computeIfAbsent(mob, key -> new State());
        if (state.animator.baseAnimation() == null && look != null) {
            update(mob, look, state);
        }
        Pose pose = state.animator.pose(partialTick);
        float raise = Mth.lerp(partialTick, state.previousRaise, state.raise);
        if (look != null && look.clips() == null && raise > 0.01f) {
            // Vanilla's zombie arms (AnimationUtils.animateZombieArms): reaching forward, a swing down and in.
            float attack = mob.getAttackAnim(partialTick);
            float swing = Mth.sin(attack * Mth.PI);
            float lift = Mth.sin((1 - (1 - attack) * (1 - attack)) * Mth.PI);
            // Straight toward the target (vanilla lifts them higher, -120°), then the swing.
            float reach = -Mth.HALF_PI + swing * 1.2f - lift * 0.4f;
            float in = 0.1f - swing * 0.6f;
            arm(pose, "Arm_R", "Shoulder_R", reach, -in, raise);
            arm(pose, "Arm_L", "Shoulder_L", reach, in, raise);
        }
        if (!state.dying) {
            BipedPoses.look(pose, netHeadYaw, headPitch);
        }
        return pose;
    }

    /**
     * Turns an arm toward vanilla's turn ({@code x}, {@code y}, radians, relative to the body) by {@code weight}: the
     * turn is the arm's in model space, so it is taken back into the shoulder's space first.
     */
    private static void arm(Pose pose, String joint, String shoulder, float x, float y, float weight) {
        int index = Biped.ARMATURE.get(joint).index();
        Quaternionf parent = pose.toModelMatrices(Biped.ARMATURE)[Biped.ARMATURE.get(shoulder).index()]
                .getUnnormalizedRotation(new Quaternionf()).normalize();
        Quaternionf target = parent.conjugate().mul(BipedPoses.fromVanilla(x, y, 0));
        JointTransform current = pose.get(index);
        pose.set(index, JointTransform.of(current.translation(),
                new Quaternionf(current.rotation()).slerp(target, weight), current.scale()));
    }

    private static void update(Mob mob, Look look, State state) {
        double dx = mob.getX() - mob.xo, dz = mob.getZ() - mob.zo, dy = mob.getY() - mob.yo;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz) * 20;
        // A client does not always hear that a mob stands on the ground: it is in the air while it moves up or down.
        boolean grounded = mob.onGround() || mob.isInWater() || Math.abs(dy) < 1e-3;
        state.airTicks = grounded ? 0 : state.airTicks + 1;
        state.motion = state.airTicks > 2 ? (dy > 0 ? LivingMotion.JUMP : LivingMotion.FALL)
                : horizontal < MOVING ? LivingMotion.IDLE : horizontal > RUNNING ? LivingMotion.RUN : LivingMotion.WALK;
        state.speed += (horizontal - state.speed) * 0.5f;
        MotionSet set = SkeletalClips.motions(look.motions());
        String id = set == null ? null : set.clip(state.motion, look.clips() != null && mob.isAggressive());
        Animation animation = id == null ? null : SkeletalClips.get(ResourceLocation.parse(id));
        if (animation == null) {
            return;
        }
        float speed = animation.moveSpeed() > 0 ? Mth.clamp(state.speed / animation.moveSpeed(), 0.4f, 2.5f) : 1;
        state.animator.motion(state.motion.key());
        state.animator.playBase(animation, speed);
        if (look.clips() != null) {
            for (String additive : set.additive()) {
                Animation clip = SkeletalClips.get(ResourceLocation.parse(additive));
                if (clip != null) {
                    state.animator.playAdditive(clip);
                }
            }
        }
    }
}
