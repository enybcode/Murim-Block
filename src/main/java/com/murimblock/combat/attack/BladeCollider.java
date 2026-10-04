package com.murimblock.combat.attack;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/** One server-owned blade snapshot in world-space blocks at a common sample time. */
public record BladeCollider(UUID owner, double sampledAt, BladePose pose, double radius,
                            State state, Optional<AttackContactResolver.AttackId> attack) {
    public enum State { INACTIVE, ATTACKING, GUARDING }

    public BladeCollider {
        Objects.requireNonNull(owner);
        sampledAt = requireTime(sampledAt);
        Objects.requireNonNull(pose);
        Objects.requireNonNull(state);
        Objects.requireNonNull(attack);
        if (!Double.isFinite(radius) || radius <= 0 || !Double.isFinite(radius * radius)) {
            throw new IllegalArgumentException("Blade collider radius must be finite and positive");
        }
        if ((state == State.ATTACKING) != attack.isPresent()) {
            throw new IllegalArgumentException("Only an attacking blade must carry an attack identity");
        }
        if (attack.isPresent() && !attack.orElseThrow().attacker().equals(owner)) {
            throw new IllegalArgumentException("Blade and attack must belong to the same actor");
        }
    }

    public static BladeCollider fromAttack(AttackContactResolver.AttackId attack, AttackDefinition definition,
                                            double startedAt, double sampledAt, Vec3 origin, double yawDegrees) {
        Objects.requireNonNull(attack);
        Objects.requireNonNull(definition);
        startedAt = requireTime(startedAt);
        sampledAt = requireTime(sampledAt);
        double age = sampledAt - startedAt;
        boolean active = definition.timeline().phaseAt(age) == AttackTimeline.Phase.CONTACT;
        BladePose pose = definition.blade().sample(age).toWorld(origin, yawDegrees);
        return new BladeCollider(attack.attacker(), sampledAt, pose, definition.blade().radius(),
                active ? State.ATTACKING : State.INACTIVE, active ? Optional.of(attack) : Optional.empty());
    }

    public static BladeCollider guarding(UUID owner, double sampledAt, BladePose worldPose, double radius) {
        return new BladeCollider(owner, sampledAt, worldPose, radius, State.GUARDING, Optional.empty());
    }

    public static BladeCollider inactive(UUID owner, double sampledAt, BladePose worldPose, double radius) {
        return new BladeCollider(owner, sampledAt, worldPose, radius, State.INACTIVE, Optional.empty());
    }

    private static double requireTime(double time) {
        if (!Double.isFinite(time) || time < 0) throw new IllegalArgumentException("Server sample time must be finite and nonnegative");
        return time == 0 ? 0.0 : time;
    }
}
