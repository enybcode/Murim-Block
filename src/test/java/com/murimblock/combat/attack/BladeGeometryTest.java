package com.murimblock.combat.attack;

import java.util.Random;
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
    void nearlyParallelTipContactIsNotLostWhenEndpointsAreReversed() {
        var tilted = new BladePose(new Vec3(-1, 0.100000008, 0), new Vec3(1, 0.099999992, 0));
        var horizontalReversed = new BladePose(HORIZONTAL.tip(), HORIZONTAL.hilt());
        var tiltedReversed = new BladePose(tilted.tip(), tilted.hilt());
        for (var first : new BladePose[]{HORIZONTAL, horizontalReversed}) {
            for (var second : new BladePose[]{tilted, tiltedReversed}) {
                assertTrue(BladeGeometry.intersect(first, 0.05, second, 0.05).isPresent());
                assertTrue(BladeGeometry.intersect(second, 0.05, first, 0.05).isPresent());
            }
        }
    }

    @Test
    void shortSegmentsRetainTheirTipContactsInsteadOfBecomingTheirHiltPoints() {
        var tiny = new BladePose(Vec3.ZERO, new Vec3(0.0000625, 0, 0));
        var atTip = new BladePose(tiny.tip(), new Vec3(0.0000625, 0.0000625, 0));
        assertTrue(BladeGeometry.intersect(tiny, 0, atTip, 0).isPresent());
        assertTrue(BladeGeometry.intersect(atTip, 0, tiny, 0).isPresent());
    }

    @Test
    void shortCrossingSegmentsRetainInteriorContacts() {
        var horizontal = new BladePose(new Vec3(-0.00003125, 0, 0), new Vec3(0.00003125, 0, 0));
        var vertical = new BladePose(new Vec3(0, -0.00003125, 0), new Vec3(0, 0.00003125, 0));
        var contact = BladeGeometry.intersect(horizontal, 0, vertical, 0).orElseThrow();
        assertEquals(Vec3.ZERO, contact.midpoint());
    }

    @Test
    void tinyGapRemainsAMissAfterShortSegmentNormalization() {
        var horizontal = new BladePose(Vec3.ZERO, new Vec3(0.0000625, 0, 0));
        var parallel = horizontal.toWorld(new Vec3(0, 0.00001, 0), 0);
        assertTrue(BladeGeometry.intersect(horizontal, 0.000001, parallel, 0.000001).isEmpty());
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

    @Test
    void knownEndpointContactsSurviveScaleYawOffsetAndActorPermutation() {
        var random = new Random(4567);
        for (int i = 0; i < 500; i++) {
            double size = new double[]{0.0000625, 0.125, 1, 8}[i % 4];
            var origin = i % 2 == 0 ? Vec3.ZERO : new Vec3(29_000_000, 100, -29_000_000);
            double yaw = random.nextDouble() * 360;
            var first = new BladePose(new Vec3(-size, 0, 0), new Vec3(size, 0, 0)).toWorld(origin, yaw);
            var second = new BladePose(new Vec3(0, size * 0.04, 0),
                    new Vec3(random.nextDouble(-2, 2) * size, random.nextDouble(-2, 2) * size,
                            random.nextDouble(-2, 2) * size)).toWorld(origin, yaw);
            assertInAnyOrder(first, second, 0.025 * size, true, i);
        }
    }

    @Test
    void separatedBladesRemainMissesAcrossScaleYawOffsetAndActorPermutation() {
        var random = new Random(8910);
        for (int i = 0; i < 500; i++) {
            double size = new double[]{0.0000625, 0.125, 1, 8}[i % 4];
            var origin = i % 2 == 0 ? Vec3.ZERO : new Vec3(-29_000_000, 100, 29_000_000);
            double yaw = random.nextDouble() * 360;
            var first = new BladePose(new Vec3(-size, 0, 0), new Vec3(size, 0, 0)).toWorld(origin, yaw);
            var second = new BladePose(new Vec3(-size, 0, 5 * size),
                    new Vec3(random.nextDouble(-1, 1) * size, random.nextDouble(-1, 1) * size,
                            random.nextDouble(4, 6) * size)).toWorld(origin, yaw);
            assertInAnyOrder(first, second, 0.025 * size, false, i);
        }
    }

    private static void assertInAnyOrder(BladePose first, BladePose second, double radius,
                                         boolean expected, int scenario) {
        for (var a : new BladePose[]{first, new BladePose(first.tip(), first.hilt())}) {
            for (var b : new BladePose[]{second, new BladePose(second.tip(), second.hilt())}) {
                assertEquals(expected, BladeGeometry.intersect(a, radius, b, radius).isPresent(), "Scenario " + scenario);
                assertEquals(expected, BladeGeometry.intersect(b, radius, a, radius).isPresent(), "Swapped scenario " + scenario);
            }
        }
    }
}
