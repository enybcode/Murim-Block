package com.murimblock.combat.attack;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

import static com.murimblock.combat.attack.AttackContactResolver.*;
import static com.murimblock.combat.attack.BladeCollider.State.*;

/** Generates instantaneous candidate exchanges; the server runtime still authorizes their outcomes. */
public final class BladeContactDetector {
    public record WeaponContact(Contact event, Vec3 point) {
        public WeaponContact {
            Objects.requireNonNull(event);
            BladePose.requireFinite(point);
            if (!(event instanceof BladeClash || event instanceof GuardBlock)) {
                throw new IllegalArgumentException("A weapon contact must be a clash or guard block");
            }
        }
    }

    private BladeContactDetector() { }

    public static Optional<WeaponContact> detect(BladeCollider first, BladeCollider second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        if (first.sampledAt() != second.sampledAt()) {
            throw new IllegalArgumentException("Blades must be sampled at the same server time");
        }
        if (first.owner().equals(second.owner()) || first.state() == INACTIVE || second.state() == INACTIVE
                || (first.state() != ATTACKING && second.state() != ATTACKING)) {
            return Optional.empty();
        }
        // Stable actor order also makes the authoritative effect point independent of caller order.
        if (first.owner().compareTo(second.owner()) > 0) {
            BladeCollider swap = first;
            first = second;
            second = swap;
        }
        var geometry = BladeGeometry.intersect(first.pose(), first.radius(), second.pose(), second.radius());
        if (geometry.isEmpty()) return Optional.empty();
        Contact event;
        if (first.state() == ATTACKING && second.state() == ATTACKING) {
            event = new BladeClash(first.sampledAt(), first.attack().orElseThrow(), second.attack().orElseThrow());
        } else {
            BladeCollider attacking = first.state() == ATTACKING ? first : second;
            BladeCollider guarding = first.state() == GUARDING ? first : second;
            event = new GuardBlock(first.sampledAt(), attacking.attack().orElseThrow(), guarding.owner());
        }
        return Optional.of(new WeaponContact(event, geometry.orElseThrow().midpoint()));
    }
}
