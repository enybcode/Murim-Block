package com.murimblock.combat.attack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.murimblock.combat.attack.BladeCollider.State.*;
import static org.junit.jupiter.api.Assertions.*;

class BladeColliderTest {
    private static final UUID OWNER = new UUID(0, 1);
    private static final AttackContactResolver.AttackId ATTACK = new AttackContactResolver.AttackId(OWNER, 1);
    private static final BladePose POSE = new BladePose(Vec3.ZERO, new Vec3(0, 0, 1));

    @Test
    void sampleLocatesMovingBladeInWorldSpaceAndMarksTheActiveAttack() {
        var collider = BladeCollider.fromAttack(ATTACK, definition(), 100, 102.5, new Vec3(10, 20, 30), 90);
        assertEquals(ATTACKING, collider.state());
        assertEquals(Optional.of(ATTACK), collider.attack());
        assertEquals(0.05, collider.radius());
        assertEquals(102.5, collider.sampledAt());
        assertTrue(collider.pose().hilt().distanceTo(new Vec3(10, 21, 32.5)) < 1.0e-10);
        assertTrue(collider.pose().tip().distanceTo(new Vec3(9, 21, 32.5)) < 1.0e-10);
    }

    @Test
    void notStartedWindupRecoveryAndFinishedBladesCannotClash() {
        for (double age : new double[]{-1, 0, 1.99, 4, 7, 10}) {
            var collider = BladeCollider.fromAttack(ATTACK, definition(), 100, 100 + age, Vec3.ZERO, 0);
            assertEquals(INACTIVE, collider.state(), "Age " + age);
            assertTrue(collider.attack().isEmpty());
        }
    }

    @Test
    void contactStartIsInclusiveAndContactEndIsExclusive() {
        for (double age : new double[]{2, 3.99}) {
            assertEquals(ATTACKING, BladeCollider.fromAttack(ATTACK, definition(), 100, 100 + age, Vec3.ZERO, 0).state());
        }
        assertEquals(INACTIVE, BladeCollider.fromAttack(ATTACK, definition(), 100, 104, Vec3.ZERO, 0).state());
    }

    @Test
    void guardAndInactiveSnapshotsDoNotCarryAnOffensiveAttack() {
        var guard = BladeCollider.guarding(OWNER, 100, POSE, 0.05);
        var idle = BladeCollider.inactive(OWNER, 100, POSE, 0.05);
        assertEquals(GUARDING, guard.state());
        assertEquals(INACTIVE, idle.state());
        assertTrue(guard.attack().isEmpty());
        assertTrue(idle.attack().isEmpty());
        assertEquals(POSE, guard.pose());
    }

    @Test
    void actorAndStateCannotDisagreeWithTheAttackIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new BladeCollider(OWNER, 100, POSE, 0.05, ATTACKING, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new BladeCollider(OWNER, 100, POSE, 0.05, GUARDING, Optional.of(ATTACK)));
        assertThrows(IllegalArgumentException.class,
                () -> new BladeCollider(new UUID(0, 2), 100, POSE, 0.05, ATTACKING, Optional.of(ATTACK)));
    }

    @Test
    void invalidClockGeometryAndRadiusInputsAreRejected() {
        for (double radius : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> BladeCollider.guarding(OWNER, 100, POSE, radius));
        }
        for (double time : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> BladeCollider.guarding(OWNER, time, POSE, 0.05));
            assertThrows(IllegalArgumentException.class,
                    () -> BladeCollider.fromAttack(ATTACK, definition(), time, 100, Vec3.ZERO, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> BladeCollider.fromAttack(ATTACK, definition(), 100, time, Vec3.ZERO, 0));
        }
        assertThrows(NullPointerException.class, () -> BladeCollider.guarding(OWNER, 100, null, 0.05));
        assertThrows(IllegalArgumentException.class,
                () -> BladeCollider.fromAttack(ATTACK, definition(), 100, 102.5, Vec3.ZERO, Double.NaN));
    }

    @Test
    void signedZeroSampleTimesHaveTheSameIdentity() {
        assertEquals(BladeCollider.guarding(OWNER, 0.0, POSE, 0.05),
                BladeCollider.guarding(OWNER, -0.0, POSE, 0.05));
    }

    private static AttackDefinition definition() {
        var id = ResourceLocation.fromNamespaceAndPath("murimblock", "sword_cut_test");
        var path = new BladeTrajectory(0.05, List.of(
                new BladeTrajectory.Keyframe(0, new BladePose(new Vec3(0, 1, 0), new Vec3(0, 1, 1))),
                new BladeTrajectory.Keyframe(7, new BladePose(new Vec3(7, 1, 0), new Vec3(7, 1, 1)))));
        return new AttackDefinition(id, id, new AttackTimeline(2, 2, 3), path);
    }
}
