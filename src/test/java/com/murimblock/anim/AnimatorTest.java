package com.murimblock.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** The animator with hand-made clips, then the shipped motion and fist clips switched every way (no pop). */
class AnimatorTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/murimblock/skeletal");
    private static final int HEAD = Biped.ARMATURE.get(Biped.HEAD).index();
    private static final int ARM_L = Biped.ARMATURE.get("Arm_L").index();
    private static final int ARM_R = Biped.ARMATURE.get("Arm_R").index();
    private static final int LEG_L = Biped.ARMATURE.get("Leg_L").index();
    /** Samples per tick: about 60 frames per second. */
    private static final int FRAMES = 3;
    /**
     * A pop is a frame that moves more than the clips themselves plus a smooth blend of the gap between them: the
     * eased blend (smoothstep) moves at most 1.5 times its average speed. A switch with no blend would cross the
     * whole gap in one frame.
     */
    private static final float EASE_PEAK = 1.5f;

    /** A clip holding {@code joint} turned {@code degrees} about X. */
    private static Animation hold(String id, int joint, float degrees, float transition, JointMask mask) {
        Quaternionf q = new Quaternionf().rotateX((float) Math.toRadians(degrees));
        Clip clip = new Clip(id, List.of(new Clip.Channel(joint, Clip.Path.ROTATION, Clip.Interpolation.LINEAR,
                new float[] {0, 1}, new float[] {q.x, q.y, q.z, q.w, q.x, q.y, q.z, q.w})));
        return new Animation(id, clip, true, transition, mask);
    }

    private static float angleX(Pose pose, int joint) {
        Quaternionf q = new Quaternionf(pose.get(joint).rotation());
        return (float) Math.toDegrees(2 * Math.atan2(q.x, q.w));
    }

    @Test
    void theBaseBlendsFromWhatWasShown() {
        Animator animator = new Animator(Biped.ARMATURE, null);
        animator.playBase(hold("a", HEAD, 0, 0.1f, JointMask.all(Biped.ARMATURE)), 1);
        animator.pose(0);
        animator.playBase(hold("b", HEAD, 40, 0.1f, JointMask.all(Biped.ARMATURE)), 1);
        // Right after the switch the old pose shows; one tick into a 0.1 s blend it is half way (the ease is symmetric).
        // pose(1) is the pose at the last tick, pose(0) the one a tick before.
        assertEquals(0, angleX(animator.pose(0), HEAD), 1e-3f);
        animator.tick(1);
        assertEquals(20, angleX(animator.pose(1), HEAD), 1e-2f);
        animator.tick(1);
        assertEquals(40, angleX(animator.pose(1), HEAD), 1e-3f);
    }

    @Test
    void layersFadeInAndOutOverTheirMask() {
        Animator animator = new Animator(Biped.ARMATURE, null);
        animator.playBase(hold("base", LEG_L, 0, 0.1f, JointMask.all(Biped.ARMATURE)), 1);
        Animation arm = hold("arm", ARM_L, -60, 0.1f, JointMask.of(Biped.ARMATURE, "Arm_L"));
        animator.play(Animator.Priority.MIDDLE, arm, 1, false);
        assertEquals(0, angleX(animator.pose(0), ARM_L), 1e-3f);
        animator.tick(1);
        assertEquals(-30, angleX(animator.pose(1), ARM_L), 0.5f);
        animator.tick(1);
        assertEquals(-60, angleX(animator.pose(1), ARM_L), 1e-3f);
        // Mirrored, the same clip moves the other arm.
        animator.play(Animator.Priority.HIGH, hold("arm2", ARM_L, -30, 0.05f, JointMask.all(Biped.ARMATURE)), 1, true);
        animator.tick(1);
        assertEquals(-30, angleX(animator.pose(1), ARM_R), 1e-3f);
        animator.stop(Animator.Priority.MIDDLE);
        assertNotNull(animator.pose(0));
        assertNull(animator.playing(Animator.Priority.MIDDLE));
        animator.tick(1);
        animator.tick(1);
        assertEquals(0, angleX(animator.pose(1), ARM_L), 1e-3f);
    }

    @Test
    void aMaskThatChangesWithTheMotionBlends() {
        Animator animator = new Animator(Biped.ARMATURE, null);
        animator.playBase(hold("base", LEG_L, 0, 0.1f, JointMask.all(Biped.ARMATURE)), 1);
        Animation guard = new Animation("guard", hold("guard", LEG_L, 30, 0.1f, null).clip(), true, 0.1f,
                JointMask.all(Biped.ARMATURE), Map.of("walk", JointMask.of(Biped.ARMATURE, "Arm_L")), false, 0);
        animator.play(Animator.Priority.MIDDLE, guard, 1, false);
        animator.tick(1);
        animator.tick(1);
        assertEquals(30, angleX(animator.pose(1), LEG_L), 1e-3f);
        animator.motion("walk");
        animator.tick(1);
        assertEquals(15, angleX(animator.pose(1), LEG_L), 0.5f);
        animator.tick(1);
        assertEquals(0, angleX(animator.pose(1), LEG_L), 1e-3f);
    }

    @Test
    void additiveClipsAddTheirChange() {
        Animator animator = new Animator(Biped.ARMATURE, null);
        animator.playBase(hold("base", HEAD, 10, 0.1f, JointMask.all(Biped.ARMATURE)), 1);
        Quaternionf nod = new Quaternionf().rotateX((float) Math.toRadians(20));
        Clip clip = new Clip("nod", List.of(new Clip.Channel(HEAD, Clip.Path.ROTATION, Clip.Interpolation.LINEAR,
                new float[] {0, 1}, new float[] {0, 0, 0, 1, nod.x, nod.y, nod.z, nod.w})));
        animator.playAdditive(new Animation("nod", clip, true, 0.05f, JointMask.all(Biped.ARMATURE), Map.of(), true, 0));
        for (int i = 0; i < 10; i++) {
            animator.tick(1);
        }
        // Half a second into the nod: 10° added to the base's 10°.
        assertEquals(20, angleX(animator.pose(1), HEAD), 0.1f);
    }

    // The shipped clips.

    private static Map<String, Animation> load(String set) {
        try {
            Path dir = ASSETS.resolve("animations");
            return ClipLoader.load(set, Files.readString(dir.resolve(set + ".gltf")), name -> {
                try {
                    return Files.readAllBytes(dir.resolve(name));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Biped.ARMATURE, Files.readString(dir.resolve(set + ".json")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void theMotionSetNamesShippedClips() throws IOException {
        MotionSet set = MotionSet.parse(Files.readString(ASSETS.resolve("motions/biped.json")));
        Map<String, Animation> clips = new LinkedHashMap<>();
        load("biped").values().forEach(a -> clips.put("murimblock:" + a.id(), a));
        load("fists").values().forEach(a -> clips.put("murimblock:" + a.id(), a));
        for (LivingMotion motion : LivingMotion.values()) {
            for (boolean combat : new boolean[] {false, true}) {
                String clip = set.clip(motion, combat);
                assertTrue(clips.containsKey(clip), motion + " " + combat + ": " + clip);
            }
        }
        assertEquals("murimblock:fists/stance", set.clip(LivingMotion.IDLE, true));
        // No combat run: a combat run runs like any run.
        assertEquals("murimblock:biped/run", set.clip(LivingMotion.RUN, true));
        set.additive().forEach(id -> assertTrue(clips.get(id).additive(), id));
    }

    /** The largest change of any joint between two samples: rotation in degrees, translation in pixels. */
    private record Step(float degrees, float pixels) {
        Step max(Step other) {
            return new Step(Math.max(degrees, other.degrees), Math.max(pixels, other.pixels));
        }

        Step plus(Step other) {
            return new Step(degrees + other.degrees, pixels + other.pixels);
        }

        /** The fastest frame of an eased blend across this difference over {@code seconds}. */
        Step blendedOver(float seconds) {
            float frames = seconds * 20 * FRAMES;
            return new Step(EASE_PEAK * degrees / frames, EASE_PEAK * pixels / frames);
        }

        boolean exceeds(Step allowed) {
            return degrees > allowed.degrees * 1.1f + 1 || pixels > allowed.pixels * 1.1f + 0.05f;
        }
    }

    private static Step change(Pose a, Pose b) {
        float degrees = 0, pixels = 0;
        for (int i = 0; i < a.size(); i++) {
            float dot = Math.abs(new Quaternionf(a.get(i).rotation()).dot(new Quaternionf(b.get(i).rotation())));
            degrees = Math.max(degrees, (float) Math.toDegrees(2 * Math.acos(Math.min(1, dot))));
            pixels = Math.max(pixels, new Vector3f(a.get(i).translation()).distance(b.get(i).translation()));
        }
        return new Step(degrees, pixels);
    }

    /** Runs {@code ticks} ticks, sampling {@link #FRAMES} times per tick, and returns the largest change. */
    private static Step run(Animator animator, int ticks, Pose[] last) {
        Step most = new Step(0, 0);
        for (int t = 0; t < ticks; t++) {
            for (int f = 0; f < FRAMES; f++) {
                Pose pose = animator.pose(f / (float) FRAMES);
                if (last[0] != null) {
                    most = most.max(change(last[0], pose));
                }
                last[0] = pose;
            }
            animator.tick(1);
        }
        return most;
    }

    private static Pose sample(Animation animation, float time) {
        Pose pose = animation.clip().sample(time, Biped.ARMATURE.bindPose());
        Biped.settleHelpers(pose);
        return pose;
    }

    /** How fast the clip itself moves: its largest change between two frames at 60 fps. */
    private static Step own(Animation animation) {
        Step most = new Step(0, 0);
        float frame = AnimationPlayer.TICK / FRAMES;
        for (float t = frame; t <= animation.clip().duration() + 1e-4f; t += frame) {
            most = most.max(change(sample(animation, t - frame), sample(animation, t)));
        }
        return most;
    }

    @Test
    void noPopBetweenAnyTwoMotionsOrStrikes() {
        Map<String, Animation> biped = load("biped");
        Map<String, Animation> fists = load("fists");
        List<Animation> bases = new ArrayList<>();
        biped.values().stream().filter(a -> !a.additive()).forEach(bases::add);
        bases.add(fists.get("stance"));
        bases.add(fists.get("stance_walk"));
        List<String> failures = new ArrayList<>();
        Step worst = new Step(0, 0);
        for (Animation from : bases) {
            for (Animation to : bases) {
                if (from == to) {
                    continue;
                }
                // Switch at several points of the first clip's cycle.
                for (int at : new int[] {3, 8, 13}) {
                    Animator animator = new Animator(Biped.ARMATURE, Biped::settleHelpers);
                    Pose[] last = new Pose[1];
                    animator.playBase(from, 1);
                    run(animator, at, last);
                    Step gap = change(last[0], sample(to, 0));
                    animator.playBase(to, 1);
                    Step seen = run(animator, 20, last);
                    worst = worst.max(seen);
                    Step allowed = own(from).plus(own(to)).plus(gap.blendedOver(to.transition()));
                    if (seen.exceeds(allowed)) {
                        failures.add(from.id() + " -> " + to.id() + " at tick " + at + ": " + seen + ", allowed "
                                + allowed);
                    }
                }
            }
        }
        // Strikes and the guard over each motion: in, a new motion under it (the guard's mask changes), and out.
        // Several blends overlap here, so each may cross the widest gap (half a turn, a block's width).
        Animation guard = fists.get("guard"), jab = fists.get("jab");
        Animation walk = biped.get("walk");
        Step widest = new Step(180, 16);
        for (Animation base : bases) {
            for (Animation layer : List.of(guard, jab)) {
                Animator animator = new Animator(Biped.ARMATURE, Biped::settleHelpers);
                Pose[] last = new Pose[1];
                animator.playBase(base, 1);
                run(animator, 7, last);
                animator.play(Animator.Priority.MIDDLE, layer, 1, false);
                Step seen = run(animator, 4, last);
                animator.playBase(walk, 1);
                animator.motion(LivingMotion.WALK.key());
                seen = seen.max(run(animator, 6, last));
                animator.stop(Animator.Priority.MIDDLE);
                seen = seen.max(run(animator, 10, last));
                worst = worst.max(seen);
                Step allowed = own(base).plus(own(layer)).plus(own(walk)).plus(widest.blendedOver(layer.transition()))
                        .plus(widest.blendedOver(walk.transition()));
                if (seen.exceeds(allowed)) {
                    failures.add(layer.id() + " over " + base.id() + ": " + seen + ", allowed " + allowed);
                }
            }
        }
        System.out.println("Largest change between two frames at 60 fps: " + worst);
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void theTestCatchesACut() {
        // The same switch with the blend taken out must count as a pop.
        Map<String, Animation> biped = load("biped");
        Animation idle = biped.get("idle"), climb = biped.get("climb");
        Animation cut = new Animation(climb.id(), climb.clip(), true, 0, climb.mask());
        Animator animator = new Animator(Biped.ARMATURE, Biped::settleHelpers);
        Pose[] last = new Pose[1];
        animator.playBase(idle, 1);
        run(animator, 5, last);
        Step gap = change(last[0], sample(climb, 0));
        animator.playBase(cut, 1);
        Step seen = run(animator, 10, last);
        assertTrue(seen.exceeds(own(idle).plus(own(climb)).plus(gap.blendedOver(climb.transition()))), seen.toString());
    }
}
