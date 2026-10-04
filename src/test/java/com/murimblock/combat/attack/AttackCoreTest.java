package com.murimblock.combat.attack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.murimblock.combat.attack.AttackContactResolver.*;
import static org.junit.jupiter.api.Assertions.*;

/** Core composition only: no Minecraft runtime, network or damage integration. */
class AttackCoreTest {
    private static final AttackId PLAYER = new AttackId(new UUID(0, 1), 1);
    private static final AttackId MOB = new AttackId(new UUID(0, 2), 1);
    private static final AttackTimeline TIMELINE = new AttackTimeline(2, 2, 3);

    @Test
    void actualContactDuringTheActivePhaseProducesAClashInsteadOfBodyHits() {
        var horizontal = new BladePose(new Vec3(-1, 1, 0), new Vec3(1, 1, 0));
        var vertical = new BladePose(new Vec3(0, 0, 0), new Vec3(0, 2, 0));
        assertEquals(List.of(new BladeClash(102.5, PLAYER, MOB)),
                exchange(stationary(horizontal), stationary(vertical), 2.5).accepted());
    }

    @Test
    void simultaneousAttacksDoNotClashIfTheirBladesAreSeparated() {
        var horizontal = new BladePose(new Vec3(-1, 1, 0), new Vec3(1, 1, 0));
        var distant = horizontal.toWorld(new Vec3(0, 0, 2), 0);
        assertEquals(List.of(new BodyHit(102.75, PLAYER, MOB.attacker()),
                new BodyHit(102.75, MOB, PLAYER.attacker())),
                exchange(stationary(horizontal), stationary(distant), 2.5).accepted());
    }

    @Test
    void intersectingBladesOutsideTheContactPhaseDoNotGenerateACandidate() {
        var blade = new BladePose(Vec3.ZERO, new Vec3(0, 1, 0));
        assertTrue(exchange(stationary(blade), stationary(blade), 1).accepted().isEmpty());
        assertTrue(exchange(stationary(blade), stationary(blade), 4).accepted().isEmpty());
    }

    private static Resolution exchange(BladeTrajectory first, BladeTrajectory second, double age) {
        var candidates = new ArrayList<Contact>();
        if (TIMELINE.phaseAt(age) == AttackTimeline.Phase.CONTACT) {
            BladeGeometry.intersect(first.sample(age), first.radius(), second.sample(age), second.radius())
                    .ifPresent(contact -> candidates.add(new BladeClash(100 + age, PLAYER, MOB)));
            // Body contacts are supplied fixtures, not body collision detection.
            candidates.add(new BodyHit(100 + age + 0.25, PLAYER, MOB.attacker()));
            candidates.add(new BodyHit(100 + age + 0.25, MOB, PLAYER.attacker()));
        }
        return resolve(candidates, Set.of());
    }

    private static BladeTrajectory stationary(BladePose pose) {
        return new BladeTrajectory(0.05, List.of(new BladeTrajectory.Keyframe(0, pose),
                new BladeTrajectory.Keyframe(TIMELINE.durationTicks(), pose)));
    }
}
