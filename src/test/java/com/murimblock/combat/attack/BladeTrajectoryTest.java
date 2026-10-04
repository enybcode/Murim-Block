package com.murimblock.combat.attack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BladeTrajectoryTest {
    private static BladePose pose(double x) {
        return new BladePose(new Vec3(x, 1, 0), new Vec3(x, 1, 2));
    }

    private static BladeTrajectory trajectory() {
        return new BladeTrajectory(0.05, List.of(
                new BladeTrajectory.Keyframe(0, pose(0)),
                new BladeTrajectory.Keyframe(2, pose(4)),
                new BladeTrajectory.Keyframe(5, pose(10))));
    }

    @Test
    void interpolatesEndpointsAcrossNonuniformKeysAndClampsAtEnds() {
        var path = trajectory();
        assertEquals(pose(0), path.sample(-1));
        assertEquals(pose(0), path.sample(0));
        assertEquals(pose(2), path.sample(1));
        assertEquals(pose(4), path.sample(2));
        assertEquals(pose(7), path.sample(3.5));
        assertEquals(pose(10), path.sample(5));
        assertEquals(pose(10), path.sample(100));
        assertEquals(5, path.durationTicks());
    }

    @Test
    void inputKeysAreCopiedAndExposedKeysCannotBeChanged() {
        var keys = new ArrayList<>(trajectory().keyframes());
        var path = new BladeTrajectory(0.05, keys);
        keys.clear();
        assertEquals(3, path.keyframes().size());
        assertThrows(UnsupportedOperationException.class, () -> path.keyframes().clear());
    }

    @Test
    void keysMustStartAtZeroAndStrictlyIncrease() {
        assertThrows(IllegalArgumentException.class, () -> new BladeTrajectory(0.05, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new BladeTrajectory(0.05,
                List.of(new BladeTrajectory.Keyframe(0, pose(0)))));
        assertThrows(IllegalArgumentException.class, () -> new BladeTrajectory(0.05,
                List.of(new BladeTrajectory.Keyframe(1, pose(0)), new BladeTrajectory.Keyframe(2, pose(2)))));
        assertThrows(IllegalArgumentException.class, () -> new BladeTrajectory(0.05,
                List.of(new BladeTrajectory.Keyframe(0, pose(0)), new BladeTrajectory.Keyframe(0, pose(2)))));
        assertThrows(IllegalArgumentException.class, () -> new BladeTrajectory(0.05,
                List.of(new BladeTrajectory.Keyframe(0, pose(0)), new BladeTrajectory.Keyframe(2, pose(2)),
                        new BladeTrajectory.Keyframe(1, pose(1)))));
    }

    @Test
    void invalidRadiiTimesAndMissingPosesAreRejected() {
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new BladeTrajectory(invalid, trajectory().keyframes()));
        }
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new BladeTrajectory.Keyframe(invalid, pose(0)));
        }
        assertThrows(IllegalArgumentException.class, () -> trajectory().sample(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> trajectory().sample(Double.POSITIVE_INFINITY));
        assertThrows(NullPointerException.class, () -> new BladeTrajectory.Keyframe(0, null));
    }

    @Test
    void minecraftYawRotatesForwardBladeThenTranslatesItsOrigin() {
        var local = new BladePose(Vec3.ZERO, new Vec3(0, 1, 2));
        var origin = new Vec3(10, 20, 30);
        assertVector(origin, local.toWorld(origin, 0).hilt());
        assertVector(new Vec3(10, 21, 32), local.toWorld(origin, 0).tip());
        assertVector(new Vec3(8, 21, 30), local.toWorld(origin, 90).tip());
        assertVector(new Vec3(12, 21, 30), local.toWorld(origin, -90).tip());
        assertVector(new Vec3(10, 21, 28), local.toWorld(origin, 180).tip());
    }

    @Test
    void poseValidationRejectsInvalidCoordinatesAndInterpolation() {
        assertThrows(NullPointerException.class, () -> new BladePose(null, Vec3.ZERO));
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new BladePose(new Vec3(invalid, 0, 0), Vec3.ZERO));
            assertThrows(IllegalArgumentException.class, () -> new BladePose(Vec3.ZERO, new Vec3(0, invalid, 0)));
            assertThrows(IllegalArgumentException.class, () -> pose(0).toWorld(Vec3.ZERO, invalid));
            assertThrows(IllegalArgumentException.class, () -> pose(0).interpolate(pose(1), invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> pose(0).interpolate(pose(1), -0.1));
        assertThrows(IllegalArgumentException.class, () -> pose(0).interpolate(pose(1), 1.1));
        assertThrows(IllegalArgumentException.class, () -> pose(0).toWorld(new Vec3(0, 0, Double.NaN), 0));
    }

    @Test
    void attackDefinitionRequiresMotionCoveringEveryPhase() {
        var id = ResourceLocation.fromNamespaceAndPath("murimblock", "sword_cut_a");
        var animation = ResourceLocation.fromNamespaceAndPath("murimblock", "combat/sword_cut_a");
        var definition = new AttackDefinition(id, animation, new AttackTimeline(1, 2, 2), trajectory());
        assertEquals(id, definition.id());
        assertEquals(animation, definition.animation());
        assertThrows(IllegalArgumentException.class,
                () -> new AttackDefinition(id, animation, new AttackTimeline(1, 2, 3), trajectory()));
        assertThrows(NullPointerException.class,
                () -> new AttackDefinition(id, null, new AttackTimeline(1, 2, 2), trajectory()));
    }

    private static void assertVector(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1.0e-10);
        assertEquals(expected.y, actual.y, 1.0e-10);
        assertEquals(expected.z, actual.z, 1.0e-10);
    }
}
