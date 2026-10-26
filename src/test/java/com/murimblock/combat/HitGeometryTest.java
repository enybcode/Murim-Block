package com.murimblock.combat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitGeometryTest {
    /** Eyes at the origin, 1.62 above the feet; a husk-sized target is 0.6 wide and 1.95 tall. */
    private static final double[] EYE = {0, 1.62, 0};
    private static final HitShape JAB = HitShape.box(0, 3.0, 1.2, 1.2);
    private static final HitShape HOOK = HitShape.arc(2.6, 1.4, -70, 20);

    private static double[][] target(double x, double z) {
        return new double[][] {{x - 0.3, 0, z - 0.3}, {x + 0.3, 1.95, z + 0.3}};
    }

    private static boolean jab(double yaw, double pitch, double x, double z) {
        double[][] t = target(x, z);
        return HitGeometry.boxHits(EYE, yaw, pitch, JAB, t[0], t[1]);
    }

    private static boolean hook(double x, double z) {
        double[][] t = target(x, z);
        return HitGeometry.arcHits(EYE, HOOK.from(), HOOK.to(), HOOK, t[0], t[1]);
    }

    @Test
    void lookFollowsMinecraftAngles() {
        assertArrayEquals(new double[] {0, 0, 1}, HitGeometry.look(0, 0), 1.0E-9);
        assertArrayEquals(new double[] {-1, 0, 0}, HitGeometry.look(90, 0), 1.0E-9);
        assertArrayEquals(new double[] {0, -1, 0}, HitGeometry.look(0, 90), 1.0E-9);
    }

    @Test
    void boxReachesAheadAndNoFurther() {
        assertTrue(jab(0, 0, 0, 2.2));
        assertTrue(jab(0, 0, 0, 3.2), "the target's near face at 2.9 is inside");
        assertFalse(jab(0, 0, 0, 3.4), "the near face at 3.1 is past the reach");
        assertFalse(jab(0, 0, 0, -2), "never behind");
    }

    @Test
    void boxIsNarrow() {
        assertTrue(jab(0, 0, 0.8, 2), "the target's edge at 0.5 is inside half a width of 0.6");
        assertFalse(jab(0, 0, 1.5, 1.5), "a target well to the side is missed");
    }

    @Test
    void boxFollowsTheView() {
        assertTrue(jab(90, 0, -2.2, 0), "yaw 90 looks to -x");
        assertFalse(jab(90, 0, 0, 2.2));
        assertFalse(jab(0, -60, 0, 2.6), "looking up passes over a target ahead");
    }

    @Test
    void arcSweepsTheLeftSide() {
        // Facing +z, the left is +x: the hook starts 70 degrees to the left and ends 20 to the right.
        assertTrue(hook(1.3, 1.3), "45 degrees left");
        assertTrue(hook(0, 2.0), "straight ahead");
        assertFalse(hook(-1.8, 1.0), "60 degrees right is past its end");
        assertFalse(hook(0, 3.3), "out of its radius");
        assertFalse(hook(0, -2.0), "behind");
    }

    @Test
    void arcHasAHeight() {
        double[] min = {-0.3, 3.5, 1.7}, max = {0.3, 4.5, 2.3};
        assertFalse(HitGeometry.arcHits(EYE, -70, 20, HOOK, min, max), "far above the band");
    }

    @Test
    void arcSlicesShareTheSweep() {
        double[] first = HitGeometry.arcSlice(HOOK, 0, 2), second = HitGeometry.arcSlice(HOOK, 1, 2);
        assertArrayEquals(new double[] {-70, -25}, first, 1.0E-9);
        assertArrayEquals(new double[] {-25, 20}, second, 1.0E-9);
    }
}
