package com.murimblock.combat.attack;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BladeGeometryTest {
    private static final BladePose HORIZONTAL = new BladePose(new Vec3(-1, 0, 0), new Vec3(1, 0, 0));

    @Test
    void crossingSegmentsContactAtTheirSharedPoint() {
        var vertical = new BladePose(new Vec3(0, -1, 0), new Vec3(0, 1, 0));
        var contact = BladeGeometry.intersect(HORIZONTAL, 0.05, vertical, 0.05).orElseThrow();
        assertEquals(Vec3.ZERO, contact.pointOnFirst());
        assertEquals(Vec3.ZERO, contact.pointOnSecond());
        assertEquals(Vec3.ZERO, contact.midpoint());
    }

    @Test
    void parallelCapsulesIncludeAnExactGrazeButNotANearMiss() {
        var graze = new BladePose(new Vec3(-1, 0.125, 0), new Vec3(1, 0.125, 0));
        assertTrue(BladeGeometry.intersect(HORIZONTAL, 0.0625, graze, 0.0625).isPresent());
        var miss = new BladePose(new Vec3(-1, 0.126, 0), new Vec3(1, 0.126, 0));
        assertTrue(BladeGeometry.intersect(HORIZONTAL, 0.0625, miss, 0.0625).isEmpty());
    }

    @Test
    void crossingInProjectionDoesNotClashWhenAtDifferentHeights() {
        var raised = new BladePose(new Vec3(0, 0.5, -1), new Vec3(0, 0.5, 1));
        assertTrue(BladeGeometry.intersect(HORIZONTAL, 0.05, raised, 0.05).isEmpty());
    }

    @Test
    void finiteEndpointsPreventContactsOnInfiniteLineExtensions() {
        var beyondTip = new BladePose(new Vec3(2, -1, 0), new Vec3(2, 1, 0));
        assertTrue(BladeGeometry.intersect(HORIZONTAL, 0.05, beyondTip, 0.05).isEmpty());
        var touchingTip = new BladePose(new Vec3(1, -1, 0), new Vec3(1, 1, 0));
        assertTrue(BladeGeometry.intersect(HORIZONTAL, 0, touchingTip, 0).isPresent());
    }

    @Test
    void reversingEndpointsAndSwappingActorsPreserveContactGeometry() {
        var other = new BladePose(new Vec3(0, -1, 0.05), new Vec3(0, 1, 0.05));
        var original = BladeGeometry.intersect(HORIZONTAL, 0.05, other, 0.05).orElseThrow();
        var swapped = BladeGeometry.intersect(other, 0.05, HORIZONTAL, 0.05).orElseThrow();
        var reversed = BladeGeometry.intersect(new BladePose(HORIZONTAL.tip(), HORIZONTAL.hilt()),
                0.05, other, 0.05).orElseThrow();
        assertEquals(original.pointOnFirst(), swapped.pointOnSecond());
        assertEquals(original.pointOnSecond(), swapped.pointOnFirst());
        assertEquals(original.midpoint(), reversed.midpoint());
    }

    @Test
    void degenerateSegmentsBehaveAsSpheresRatherThanInvalidDirections() {
        var point = new BladePose(Vec3.ZERO, Vec3.ZERO);
        var nearby = new BladePose(new Vec3(0, 0.1, 0), new Vec3(0, 0.1, 0));
        assertTrue(BladeGeometry.intersect(point, 0.05, nearby, 0.05).isPresent());
        assertTrue(BladeGeometry.intersect(point, 0.01, nearby, 0.01).isEmpty());
        assertTrue(BladeGeometry.intersect(point, 0, HORIZONTAL, 0).isPresent());
        assertTrue(BladeGeometry.intersect(HORIZONTAL, 0, point, 0).isPresent());
    }

    @Test
    void largeWorldOffsetsDoNotChangeALocalContact() {
        var origin = new Vec3(29_000_000, 100, -29_000_000);
        var other = new BladePose(new Vec3(0, -1, 0), new Vec3(0, 1, 0));
        var contact = BladeGeometry.intersect(HORIZONTAL.toWorld(origin, 0), 0.05,
                other.toWorld(origin, 0), 0.05).orElseThrow();
        assertEquals(origin, contact.midpoint());
    }

    @Test
    void pointContactsUseSeparationRatherThanAnOriginDependentDotProduct() {
        var origin = new Vec3(29_000_000, 100, -29_000_000);
        var point = new BladePose(origin, origin);
        var nearPosition = origin.add(0, 0.0625, 0);
        var near = new BladePose(nearPosition, nearPosition);
        assertTrue(BladeGeometry.intersect(point, 0.0625, near, 0.0625).isPresent());
        var farPosition = origin.add(0, 1, 0);
        var far = new BladePose(farPosition, farPosition);
        assertTrue(BladeGeometry.intersect(point, 0.0625, far, 0.0625).isEmpty());
    }

    @Test
    void invalidContactRadiiAreRejected() {
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,
                    () -> BladeGeometry.intersect(HORIZONTAL, invalid, HORIZONTAL, 0.05));
            assertThrows(IllegalArgumentException.class,
                    () -> BladeGeometry.intersect(HORIZONTAL, 0.05, HORIZONTAL, invalid));
        }
    }
}
