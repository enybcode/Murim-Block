package com.murimblock.shenfa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * One body's animations: a base layer (the living motion: idle, walk...), composite layers by priority that take
 * over the joints of their clip's mask (a guard on the arms, a strike on the whole body), and additive layers laid on
 * top (breathing). Nothing ever pops: a layer that changes clip blends from the pose it showed into the new clip over
 * the new clip's transition time, a layer that starts fades in over the pose under it, and a layer that stops fades
 * out the same way.
 * <p>
 * Each tick also gathers the events its clips passed ({@link #events()}) and, for clips with {@link RootMotion},
 * how far their root travelled ({@link #rootMotion()}): that travel is taken out of the drawn pose, for the body
 * itself to move.
 */
public final class Animator {
    public enum Priority {
        LOWEST, LOW, MIDDLE, HIGH, HIGHEST
    }

    private final Armature armature;
    private final Consumer<Pose> finish;
    private final Layer base;
    private final Map<Priority, Layer> layers = new EnumMap<>(Priority.class);
    private final Map<String, Layer> additives = new LinkedHashMap<>();
    private String motion = LivingMotion.IDLE.key();
    private final List<ClipEvent> events = new ArrayList<>();
    private final Vector3f rootMotion = new Vector3f();
    private boolean noGravity;

    /** @param finish run on every finished pose (the biped settles its helper joints there); may be null */
    public Animator(Armature armature, Consumer<Pose> finish) {
        this.armature = armature;
        this.finish = finish == null ? pose -> {
        } : finish;
        base = new Layer();
        base.weight = base.previousWeight = base.targetWeight = 1;
        for (Priority priority : Priority.values()) {
            layers.put(priority, new Layer());
        }
    }

    /** The living motion's key, which picks each layer's mask ({@link Animation#mask(String)}). */
    public void motion(String key) {
        motion = key;
    }

    public String motion() {
        return motion;
    }

    /** The base layer's clip: blends into it unless it already plays; then only its speed changes. */
    public void playBase(Animation animation, float speed) {
        if (base.player.animation() == animation) {
            base.player.speed(speed);
            return;
        }
        base.start(animation, speed, false);
    }

    public Animation baseAnimation() {
        return base.player.animation();
    }

    /** Plays {@code animation} on a composite layer; {@code mirrored} swaps left and right. */
    public void play(Priority priority, Animation animation, float speed, boolean mirrored) {
        Layer layer = layers.get(priority);
        layer.start(animation, speed, mirrored);
        layer.targetWeight = 1;
    }

    /** Fades a composite layer out over its clip's transition time. */
    public void stop(Priority priority) {
        layers.get(priority).targetWeight = 0;
    }

    /** The clip on a layer, or null when the layer is off or fading out. */
    public Animation playing(Priority priority) {
        Layer layer = layers.get(priority);
        return layer.targetWeight > 0 ? layer.player.animation() : null;
    }

    /** Seconds into the clip of a layer, or -1 when the layer has none on. */
    public float time(Priority priority) {
        Layer layer = layers.get(priority);
        return layer.targetWeight > 0 && layer.player.animation() != null ? layer.player.time() : -1;
    }

    /** Whether any composite layer has a clip on (not fading out). */
    public boolean anyLayer() {
        for (Priority priority : Priority.values()) {
            if (playing(priority) != null) {
                return true;
            }
        }
        return false;
    }

    /** Whether a composite layer plays a clip with root motion (the clip, not the ground, then places the feet). */
    public boolean rootMotionPlaying() {
        for (Priority priority : Priority.values()) {
            Animation animation = playing(priority);
            if (animation != null && animation.rootMotion() != null) {
                return true;
            }
        }
        return false;
    }

    /** Starts an additive clip (it keeps its place if it already plays). */
    public void playAdditive(Animation animation) {
        Layer layer = additives.computeIfAbsent(animation.id(), id -> new Layer());
        if (layer.player.animation() != animation) {
            layer.player.play(animation, 1, null);
        }
        layer.targetWeight = 1;
    }

    public void stopAdditive(String id) {
        Layer layer = additives.get(id);
        if (layer != null) {
            layer.targetWeight = 0;
        }
    }

    /** One game tick, times {@code scale} (slow motion). */
    public void tick(float scale) {
        events.clear();
        rootMotion.zero();
        noGravity = false;
        base.tick(scale);
        layers.values().forEach(layer -> layer.tick(scale));
        additives.values().forEach(layer -> layer.tick(scale));
        additives.values().removeIf(layer -> layer.weight <= 0 && layer.targetWeight <= 0);
    }

    /** The events the clips passed in the last tick (mirrored with their clip), in the order they came. */
    public List<ClipEvent> events() {
        return events;
    }

    /** How far root motion moved the body in the last tick, in model space (pixels; the model faces +Z). */
    public Vector3fc rootMotion() {
        return rootMotion;
    }

    /** Whether a clip with root motion holds the body up (no gravity) at the last tick. */
    public boolean noGravity() {
        return noGravity;
    }

    /** Where the root of {@code animation} stands at {@code time} seconds. */
    private Vector3f root(Animation animation, float time) {
        return new Vector3f(animation.clip().sample(time, armature.bindPose()).get(0).translation());
    }

    /** The pose between the last two ticks. */
    public Pose pose(float partialTick) {
        return pose(partialTick, null);
    }

    /**
     * The pose between the last two ticks, over {@code under} instead of the base layer's clip when it is given: a
     * mob's own vanilla pose, with Shenfa's layers laid on it. A composite layer playing an additive clip adds its
     * change (a flinch, a lunge laid on the walk) rather than taking the joints over.
     */
    public Pose pose(float partialTick, Pose under) {
        Pose result = under != null ? under.copy() : base.sample(partialTick);
        for (Layer layer : layers.values()) {
            float weight = layer.weight(partialTick);
            Animation animation = layer.player.animation();
            if (weight > 0 && animation != null) {
                result = animation.additive() ? result.add(delta(layer, partialTick, weight), animation.mask(motion))
                        : layer.overlay.blend(result, layer.sample(partialTick), layer.jointWeights(partialTick, weight));
            }
        }
        for (Layer layer : additives.values()) {
            float weight = layer.weight(partialTick);
            Animation animation = layer.player.animation();
            if (weight > 0 && animation != null) {
                result = result.add(delta(layer, partialTick, weight), animation.mask(motion));
            }
        }
        finish.accept(result);
        return result;
    }

    /** An additive clip's change from its first frame, scaled by {@code weight}. */
    private Pose delta(Layer layer, float partialTick, float weight) {
        Pose first = layer.player.animation().clip().sample(0, armature.bindPose());
        Pose now = layer.player.pose(partialTick, armature.bindPose());
        Pose delta = new Pose(now.size());
        for (int i = 0; i < now.size(); i++) {
            JointTransform a = first.get(i), b = now.get(i);
            Vector3f move = new Vector3f(b.translation()).sub(a.translation()).mul(weight);
            Quaternionf turn = new Quaternionf(a.rotation()).conjugate().mul(b.rotation());
            new Quaternionf().slerp(turn, weight, turn);
            delta.set(i, JointTransform.of(move, turn, new Vector3f(1, 1, 1)));
        }
        RootMotion root = layer.player.animation().rootMotion();
        if (root != null) {
            // The body itself carries the root's travel.
            JointTransform hips = delta.get(0);
            Vector3f kept = new Vector3f(0, root.vertical() ? 0 : hips.translation().y(), 0);
            delta.set(0, JointTransform.of(kept, hips.rotation(), hips.scale()));
        }
        return layer.mirrored ? delta.mirrored(armature) : delta;
    }

    /**
     * Blends poses joint by joint, each joint keeping the way it turns from one frame to the next. Two rotations
     * almost half a turn apart (an arm raised overhead blending into one swinging back) have two short ways round;
     * as the clips move, the shorter one can switch sides mid-blend and the limb would flip in one frame.
     */
    public static final class SteadyBlend {
        /** Below this |dot| (rotations more than about 150° apart) a joint keeps the side it was going round. */
        private static final float NEAR_HALF_TURN = 0.25f;
        private int[] signs;
        /** Each side's rotation last frame: q and -q are the same turn, so a clip may hand either; this keeps one. */
        private Quaternionf[] lastFrom, lastTo;

        /** Forgets the ways round taken so far: the next blend starts from the shorter way for each joint. */
        void reset() {
            signs = null;
        }

        Pose blend(Pose a, Pose b, float weight) {
            float[] weights = new float[a.size()];
            Arrays.fill(weights, weight);
            return blend(a, b, weights);
        }

        Pose blend(Pose a, Pose b, float[] weights) {
            Pose result = a.copy();
            for (int i = 0; i < a.size(); i++) {
                float w = weights[i];
                if (w <= 0) {
                    continue;
                }
                JointTransform from = a.get(i), to = b.get(i);
                Quaternionf turn = rotation(i, a.size(), from.rotation(), to.rotation(), w);
                result.set(i, w >= 1 ? to : JointTransform.of(new Vector3f(from.translation()).lerp(to.translation(), w),
                        turn, new Vector3f(from.scale()).lerp(to.scale(), w)));
            }
            return result;
        }

        /**
         * One joint's rotation {@code weight} of the way from {@code from} to {@code to}, keeping the way round it
         * took last frame.
         *
         * @param joints how many joints this blend tracks (the first call sizes it)
         */
        public Quaternionf rotation(int joint, int joints, Quaternionfc from, Quaternionfc to, float weight) {
            if (signs == null) {
                signs = new int[joints];
                lastFrom = new Quaternionf[joints];
                lastTo = new Quaternionf[joints];
            }
            Quaternionf p = steady(new Quaternionf(from), lastFrom, joint);
            Quaternionf q = steady(new Quaternionf(to), lastTo, joint);
            float dot = p.dot(q);
            if (signs[joint] == 0 || Math.abs(dot) > NEAR_HALF_TURN) {
                signs[joint] = dot < 0 ? -1 : 1;
            }
            if (weight >= 1) {
                return q;
            }
            return slerp(p, q.mul(signs[joint]), weight);
        }

        private static Quaternionf steady(Quaternionf q, Quaternionf[] last, int i) {
            if (last[i] != null && last[i].dot(q) < 0) {
                q.mul(-1);
            }
            last[i] = new Quaternionf(q);
            return q;
        }

        /** Slerp along the arc from {@code a} to {@code b} as given, never flipping {@code b} to the shorter side. */
        private static Quaternionf slerp(Quaternionf a, Quaternionf b, float t) {
            float dot = Math.max(-1, Math.min(1, a.dot(b)));
            float angle = (float) Math.acos(dot);
            float sin = (float) Math.sin(angle);
            float wa, wb;
            if (sin < 1e-4f) {
                wa = 1 - t;
                wb = t;
            } else {
                wa = (float) Math.sin((1 - t) * angle) / sin;
                wb = (float) Math.sin(t * angle) / sin;
            }
            return new Quaternionf(a.x * wa + b.x * wb, a.y * wa + b.y * wb, a.z * wa + b.z * wb, a.w * wa + b.w * wb)
                    .normalize();
        }
    }

    private static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** A clip player with a blend from the last pose shown, and a weight that fades in and out. */
    private final class Layer {
        final AnimationPlayer player = new AnimationPlayer();
        boolean mirrored;
        /** The pose shown when the clip changed, blended into the new clip; null when there is nothing to blend. */
        Pose from;
        float blendDuration;
        float blend, previousBlend;
        float weight, previousWeight, targetWeight;
        /** How much the layer holds each joint: follows its mask, which can change with the motion, without a pop. */
        float[] joints = new float[armature.size()], previousJoints = new float[armature.size()];
        Pose last;
        final SteadyBlend change = new SteadyBlend(), overlay = new SteadyBlend();

        Layer() {
            // A layer fading out has been let go: what its clip still passes no longer happens.
            player.listener(event -> {
                if (this == base || targetWeight > 0) {
                    events.add(mirrored ? event.mirrored() : event);
                }
            });
        }

        void start(Animation animation, float speed, boolean mirrored) {
            boolean shown = last != null && weight > 0;
            from = shown ? last : null;
            blendDuration = animation.transition();
            blend = previousBlend = 0;
            // A new blend starts with no memory of the last one's way round (it may be long past and wrong now).
            change.reset();
            this.mirrored = mirrored;
            if (!shown) {
                // Fading in from nothing: the layer weight blends, so the joints can start on the mask at once.
                JointMask mask = animation.mask(motion);
                for (int i = 0; i < joints.length; i++) {
                    joints[i] = previousJoints[i] = mask.contains(i) ? 1 : 0;
                }
            }
            player.play(animation, speed, () -> {
                if (this != base && !animation.loop()) {
                    targetWeight = 0;
                }
            });
        }

        void tick(float scale) {
            boolean held = this == base || targetWeight > 0;
            player.tick(scale);
            Animation playing = player.animation();
            if (held && playing != null && playing.rootMotion() != null) {
                travel(playing);
            }
            previousBlend = blend;
            blend += AnimationPlayer.TICK * scale;
            previousWeight = weight;
            Animation animation = player.animation();
            float fade = animation == null ? 0 : animation.transition();
            float step = fade > 0 ? AnimationPlayer.TICK * scale / fade : 1;
            weight = targetWeight > weight ? Math.min(targetWeight, weight + step) : Math.max(targetWeight, weight - step);
            System.arraycopy(joints, 0, previousJoints, 0, joints.length);
            if (animation != null) {
                JointMask mask = animation.mask(motion);
                for (int i = 0; i < joints.length; i++) {
                    joints[i] = mask.contains(i) ? Math.min(1, joints[i] + step) : Math.max(0, joints[i] - step);
                }
            }
            if (weight <= 0 && targetWeight <= 0 && this != base) {
                player.stop();
                from = last = null;
            }
        }

        /** Adds the root's travel over the last tick to the body's (a loop going round is not travel). */
        private void travel(Animation playing) {
            RootMotion root = playing.rootMotion();
            float before = player.previousTime(), now = player.time();
            if (Math.signum(now - before) == Math.signum(player.speed())) {
                Vector3f move = root(playing, now).sub(root(playing, before));
                if (!root.vertical()) {
                    move.y = 0;
                }
                if (mirrored) {
                    move.x = -move.x;
                }
                rootMotion.add(move);
            }
            noGravity |= root.noGravityAt(now);
        }

        float[] jointWeights(float partialTick, float weight) {
            float[] result = new float[joints.length];
            for (int i = 0; i < joints.length; i++) {
                result[i] = weight * ease(previousJoints[i] + (joints[i] - previousJoints[i]) * partialTick);
            }
            return result;
        }

        float weight(float partialTick) {
            return ease(previousWeight + (weight - previousWeight) * partialTick);
        }

        Pose sample(float partialTick) {
            Pose pose = player.pose(partialTick, armature.bindPose());
            Animation playing = player.animation();
            if (playing != null && playing.rootMotion() != null) {
                // The root stays where the clip starts it: the body itself carries the travel.
                JointTransform root = pose.get(0);
                Vector3f start = root(playing, 0), at = new Vector3f(root.translation());
                at.set(start.x, playing.rootMotion().vertical() ? start.y : at.y, start.z);
                pose.set(0, JointTransform.of(at, root.rotation(), root.scale()));
            }
            if (mirrored) {
                pose = pose.mirrored(armature);
            }
            if (from != null && blendDuration > 0) {
                float t = (previousBlend + (blend - previousBlend) * partialTick) / blendDuration;
                if (t < 1) {
                    pose = change.blend(from, pose, ease(t));
                } else {
                    from = null;
                }
            }
            last = pose;
            return pose;
        }
    }
}
