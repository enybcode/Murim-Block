package com.murimblock.combat.attack;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.murimblock.combat.attack.AttackContactResolver.*;
import static org.junit.jupiter.api.Assertions.*;

class BladeContactDetectorTest {
    private static final UUID PLAYER = new UUID(0, 1);
    private static final UUID MOB = new UUID(0, 2);
    private static final AttackId A = new AttackId(PLAYER, 1);
    private static final AttackId B = new AttackId(MOB, 1);
    private static final BladePose HORIZONTAL = new BladePose(new Vec3(-1, 1, 0), new Vec3(1, 1, 0));
    private static final BladePose VERTICAL = new BladePose(new Vec3(0, 0, 0), new Vec3(0, 2, 0));

    @Test
    void twoAttackingBladesClashAndConsumeBothStrikesWithoutEitherActorGuarding() {
        var first = attacking(A, HORIZONTAL, 100, 102.5, Vec3.ZERO);
        var second = attacking(B, VERTICAL, 100, 102.5, Vec3.ZERO);
        var contact = BladeContactDetector.detect(first, second).orElseThrow();
        assertEquals(new BladeClash(102.5, A, B), contact.event());
        assertEquals(new Vec3(0, 1, 0), contact.point());
        var result = resolve(List.of(new BodyHit(102.75, A, MOB), new BodyHit(102.75, B, PLAYER), contact.event()), Set.of());
        assertEquals(List.of(contact.event()), result.accepted());
        assertEquals(Set.of(A, B), result.consumed());
    }

    @Test
    void anAttackingBladeTouchingAGuardProducesABlockInsteadOfBodyDamage() {
        var attack = attacking(A, HORIZONTAL, 100, 102.5, Vec3.ZERO);
        var guard = BladeCollider.guarding(MOB, 102.5, VERTICAL, 0.05);
        var contact = BladeContactDetector.detect(attack, guard).orElseThrow();
        assertEquals(new GuardBlock(102.5, A, MOB), contact.event());
        assertEquals(contact, BladeContactDetector.detect(guard, attack).orElseThrow());
        var result = resolve(List.of(new BodyHit(102.75, A, MOB), contact.event()), Set.of());
        assertEquals(List.of(contact.event()), result.accepted());
        assertEquals(Set.of(A), result.consumed());
    }

    @Test
    void actorSortOrderCannotSwapTheAttackingAndGuardingRoles() {
        var guard = BladeCollider.guarding(PLAYER, 102.5, HORIZONTAL, 0.05);
        var attack = attacking(B, VERTICAL, 100, 102.5, Vec3.ZERO);
        var contact = BladeContactDetector.detect(attack, guard).orElseThrow();
        assertEquals(new GuardBlock(102.5, B, PLAYER), contact.event());
        assertEquals(contact, BladeContactDetector.detect(guard, attack).orElseThrow());
    }

    @Test
    void simultaneousAttacksAtDifferentBladePositionsDoNotClash() {
        var first = attacking(A, HORIZONTAL, 100, 102.5, Vec3.ZERO);
        var second = attacking(B, VERTICAL, 100, 102.5, new Vec3(0, 0, 2));
        assertTrue(BladeContactDetector.detect(first, second).isEmpty());
    }

    @Test
    void windupRecoveryFinishedAndIdleBladesDoNotAutomaticallyBlockAnAttack() {
        var incoming = attacking(B, VERTICAL, 100, 102.5, Vec3.ZERO);
        for (double startedAt : new double[]{103, 101, 98.5, 95}) {
            var inactive = attacking(A, HORIZONTAL, startedAt, 102.5, Vec3.ZERO);
            assertTrue(BladeContactDetector.detect(inactive, incoming).isEmpty());
        }
        assertTrue(BladeContactDetector.detect(BladeCollider.inactive(PLAYER, 102.5, HORIZONTAL, 0.05), incoming).isEmpty());
    }

    @Test
    void twoGuardsCannotProduceAnOffensiveExchangeAndGuardReleaseStopsBlocking() {
        var first = BladeCollider.guarding(PLAYER, 102.5, HORIZONTAL, 0.05);
        var second = BladeCollider.guarding(MOB, 102.5, VERTICAL, 0.05);
        assertTrue(BladeContactDetector.detect(first, second).isEmpty());
        var incoming = attacking(A, HORIZONTAL, 100, 102.5, Vec3.ZERO);
        assertTrue(BladeContactDetector.detect(incoming, second).isPresent());
        assertTrue(BladeContactDetector.detect(incoming, BladeCollider.inactive(MOB, 102.5, VERTICAL, 0.05)).isEmpty());
    }

    @Test
    void anActorCannotBlockItsOwnBlade() {
        assertTrue(BladeContactDetector.detect(attacking(A, HORIZONTAL, 100, 102.5, Vec3.ZERO),
                BladeCollider.guarding(PLAYER, 102.5, VERTICAL, 0.05)).isEmpty());
    }

    @Test
    void mixingSnapshotsFromDifferentServerTimesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BladeContactDetector.detect(
                attacking(A, HORIZONTAL, 100, 102.5, Vec3.ZERO),
                attacking(B, VERTICAL, 100, 102.6, Vec3.ZERO)));
    }

    @Test
    void impactEffectIsLocatedAtTheWeaponContactInWorldSpace() {
        var origin = new Vec3(10, 50, 30);
        var contact = BladeContactDetector.detect(attacking(A, HORIZONTAL, 100, 102.5, origin),
                BladeCollider.guarding(MOB, 102.5, VERTICAL.toWorld(origin, 0), 0.05)).orElseThrow();
        assertEquals(new Vec3(10, 51, 30), contact.point());
        assertNotEquals(origin, contact.point());
    }

    @Test
    void overlappingLargeWeaponBoundsDoNotReplaceActualThinBladeContact() {
        var diagonal = new BladePose(new Vec3(-1, 1, -1), new Vec3(1, 1, 1));
        var offset = new BladePose(new Vec3(-1, 1, 1), new Vec3(0, 1, 2));
        assertTrue(BladeContactDetector.detect(attacking(A, diagonal, 100, 102.5, Vec3.ZERO),
                BladeCollider.guarding(MOB, 102.5, offset, 0.05)).isEmpty());
    }

    @Test
    void heldGuardCanInterceptANewStrikeWithoutBeingConsumedByTheFirst() {
        var guard = BladeCollider.guarding(MOB, 102.5, VERTICAL, 0.05);
        var first = BladeContactDetector.detect(attacking(A, HORIZONTAL, 100, 102.5, Vec3.ZERO), guard).orElseThrow();
        var consumed = resolve(List.of(first.event()), Set.of()).consumed();
        var nextAttack = new AttackId(PLAYER, 2);
        var next = BladeContactDetector.detect(attacking(nextAttack, HORIZONTAL, 110, 112.5, Vec3.ZERO),
                BladeCollider.guarding(MOB, 112.5, VERTICAL, 0.05)).orElseThrow();
        assertEquals(List.of(next.event()), resolve(List.of(next.event()), consumed).accepted());
    }

    @Test
    void effectRecordCannotMislabelABodyHitAsWeaponContact() {
        assertThrows(IllegalArgumentException.class, () -> new BladeContactDetector.WeaponContact(
                new BodyHit(102.5, A, MOB), Vec3.ZERO));
    }

    private static BladeCollider attacking(AttackId attack, BladePose localPose, double startedAt,
                                             double sampledAt, Vec3 origin) {
        var id = ResourceLocation.fromNamespaceAndPath("murimblock", "blade_test");
        var path = new BladeTrajectory(0.05, List.of(new BladeTrajectory.Keyframe(0, localPose),
                new BladeTrajectory.Keyframe(7, localPose)));
        var definition = new AttackDefinition(id, id, new AttackTimeline(2, 2, 3), path);
        return BladeCollider.fromAttack(attack, definition, startedAt, sampledAt, origin, 0);
    }
}
