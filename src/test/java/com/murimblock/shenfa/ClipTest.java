package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ClipTest {
    private static final int HEAD = Biped.ARMATURE.get(Biped.HEAD).index();
    private static final int ROOT = Biped.ARMATURE.get(Biped.ROOT).index();

    private static float[] quaternions(Quaternionf... rotations) {
        float[] values = new float[rotations.length * 4];
        for (int i = 0; i < rotations.length; i++) {
            values[i * 4] = rotations[i].x;
            values[i * 4 + 1] = rotations[i].y;
            values[i * 4 + 2] = rotations[i].z;
            values[i * 4 + 3] = rotations[i].w;
        }
        return values;
    }

    private static Clip translations(Clip.Interpolation interpolation, float[] times, float... values) {
        return new Clip("t", List.of(new Clip.Channel(ROOT, Clip.Path.TRANSLATION, interpolation, times, values)));
    }

    private static Vector3f rootAt(Clip clip, float time) {
        return new Vector3f(clip.sample(time, Biped.ARMATURE.bindPose()).get(ROOT).translation());
    }

    @Test
    void linearHitsKeysAndBlendsBetween() {
        Clip clip = translations(Clip.Interpolation.LINEAR, new float[] {0, 1, 3}, 0, 10, 0, 0, 20, 0, 4, 20, 0);
        assertEquals(3, clip.duration(), 1e-6f);
        assertEquals(new Vector3f(0, 10, 0), rootAt(clip, 0));
        assertEquals(new Vector3f(0, 15, 0), rootAt(clip, 0.5f));
        assertEquals(new Vector3f(0, 20, 0), rootAt(clip, 1));
        assertEquals(new Vector3f(2, 20, 0), rootAt(clip, 2));
        // Outside the keys the ends hold.
        assertEquals(new Vector3f(0, 10, 0), rootAt(clip, -1));
        assertEquals(new Vector3f(4, 20, 0), rootAt(clip, 9));
    }

    @Test
    void stepHoldsUntilTheNextKey() {
        Clip clip = translations(Clip.Interpolation.STEP, new float[] {0, 1}, 0, 0, 0, 5, 5, 5);
        assertEquals(new Vector3f(), rootAt(clip, 0.99f));
        assertEquals(new Vector3f(5, 5, 5), rootAt(clip, 1));
    }

    @Test
    void cubicSplineFollowsTheTangents() {
        // Keys at 0 and 2 with flat tangents: an ease in and out, half way at the middle but slow at the ends.
        float[] flat = {
            0, 0, 0, /* value */ 0, 0, 0, 0, 0, 0,
            0, 0, 0, /* value */ 8, 0, 0, 0, 0, 0,
        };
        Clip eased = translations(Clip.Interpolation.CUBICSPLINE, new float[] {0, 2}, flat);
        assertEquals(4, rootAt(eased, 1).x, 1e-5f);
        assertEquals(8 * (3 * 0.0625f - 2 * 0.015625f), rootAt(eased, 0.5f).x, 1e-5f);
        // Tangents of 4 per second over a 2 second span with values 0 and 8: a straight line.
        float[] straight = {
            0, 0, 0, /* value */ 0, 0, 0, 4, 0, 0,
            4, 0, 0, /* value */ 8, 0, 0, 0, 0, 0,
        };
        Clip line = translations(Clip.Interpolation.CUBICSPLINE, new float[] {0, 2}, straight);
        assertEquals(2, rootAt(line, 0.5f).x, 1e-5f);
        assertEquals(8, rootAt(line, 2).x, 1e-5f);
    }

    @Test
    void rotationsSlerpAndOtherPartsStay() {
        Quaternionf quarter = new Quaternionf().rotateX((float) Math.toRadians(90));
        Clip clip = new Clip("r", List.of(new Clip.Channel(HEAD, Clip.Path.ROTATION, Clip.Interpolation.LINEAR,
                new float[] {0, 1}, quaternions(new Quaternionf(), quarter))));
        Pose pose = clip.sample(0.5f, Biped.ARMATURE.bindPose());
        Quaternionf expected = new Quaternionf().rotateX((float) Math.toRadians(45));
        assertTrue(new Quaternionf(pose.get(HEAD).rotation()).equals(expected, 1e-5f));
        // Only the rotation is keyed: the head keeps its rest offset, other joints their rest.
        assertEquals(Biped.ARMATURE.joint(HEAD).bind().translation(), pose.get(HEAD).translation());
        assertEquals(Biped.ARMATURE.joint(ROOT).bind().translation(), pose.get(ROOT).translation());
    }

    @Test
    void badChannelsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> translations(Clip.Interpolation.LINEAR, new float[] {0, 1}, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> translations(Clip.Interpolation.LINEAR, new float[] {1, 0}, 0, 0, 0, 0, 0, 0));
    }
}
