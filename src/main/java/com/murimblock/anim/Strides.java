package com.murimblock.anim;

import org.joml.Vector3f;

/**
 * How fast a step cycle carries the body: the foot on the ground goes back under the body at the speed the body
 * goes forward, so a clip's ground speed at speed 1 is the average backward speed of its lower sole while that sole is
 * on the ground. Playing the clip at (body speed / ground speed) keeps the feet from sliding.
 */
public final class Strides {
    /** Blocks per model pixel for a player (drawn at 15/16). */
    private static final float TO_BLOCKS = 0.9375f / 16;
    /** A sole this close to the ground (pixels) is on it. */
    private static final float ON_GROUND = 1;
    private static final int SAMPLES_PER_SECOND = 120;

    private Strides() {
    }

    /**
     * The ground speed (blocks a second) of {@code clip} played at speed 1 on {@code armature}'s legs
     * ({@code Leg_L}, {@code Leg_R}, soles 6 px below them), or 0 when it has no such legs or never touches the ground.
     */
    public static float speed(Armature armature, Clip clip) {
        Joint left = armature.find("Leg_L"), right = armature.find("Leg_R");
        if (left == null || right == null || clip.duration() <= 0) {
            return 0;
        }
        int[] shins = {left.index(), right.index()};
        float step = 1f / SAMPLES_PER_SECOND, distance = 0, time = 0;
        Vector3f last = null;
        int lastShin = -1;
        for (float t = 0; t <= clip.duration() + 1e-4f; t += step) {
            Pose pose = clip.sample(t, armature.bindPose());
            if (armature == Biped.ARMATURE) {
                Biped.settleHelpers(pose);
            }
            Vector3f lowest = null;
            int shin = -1;
            for (int index : shins) {
                Vector3f sole = LegIk.sole(armature, pose, index, FootPlanter.SOLE);
                if (lowest == null || sole.y < lowest.y) {
                    lowest = sole;
                    shin = index;
                }
            }
            if (lowest.y > ON_GROUND) {
                last = null;
                continue;
            }
            if (last != null && shin == lastShin) {
                // The body faces +Z: the planted foot goes toward -Z.
                distance += last.z - lowest.z;
                time += step;
            }
            last = lowest;
            lastShin = shin;
        }
        return time > 0 ? Math.max(0, distance / time) * TO_BLOCKS : 0;
    }
}
