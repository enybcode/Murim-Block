package com.murimblock.combat.attack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Resolves already validated contacts; it does not find targets or authorize damage. */
public final class AttackContactResolver {
    public record AttackId(UUID attacker, long sequence) implements Comparable<AttackId> {
        public AttackId {
            Objects.requireNonNull(attacker);
            if (sequence < 0) throw new IllegalArgumentException("Attack sequence must be nonnegative");
        }

        @Override
        public int compareTo(AttackId other) {
            int actorOrder = attacker.compareTo(other.attacker);
            return actorOrder != 0 ? actorOrder : Long.compare(sequence, other.sequence);
        }
    }

    public sealed interface Contact permits BodyHit, BladeClash, Obstruction {
        double time();
        AttackId attack();
    }

    public record BodyHit(double time, AttackId attack, UUID target) implements Contact {
        public BodyHit {
            requireTime(time);
            Objects.requireNonNull(attack);
            Objects.requireNonNull(target);
            if (attack.attacker.equals(target)) throw new IllegalArgumentException("An attack cannot hit its owner");
        }
    }

    public record BladeClash(double time, AttackId attack, AttackId other) implements Contact {
        public BladeClash {
            requireTime(time);
            Objects.requireNonNull(attack);
            Objects.requireNonNull(other);
            if (attack.attacker.equals(other.attacker)) throw new IllegalArgumentException("An actor cannot clash with itself");
            if (attack.compareTo(other) > 0) {
                AttackId swap = attack;
                attack = other;
                other = swap;
            }
        }
    }

    public record Obstruction(double time, AttackId attack, BlockPos block) implements Contact {
        public Obstruction {
            requireTime(time);
            Objects.requireNonNull(attack);
            block = Objects.requireNonNull(block).immutable();
        }
    }

    public record Resolution(List<Contact> accepted, Set<AttackId> consumed) {
        public Resolution {
            accepted = List.copyOf(accepted);
            consumed = Set.copyOf(consumed);
        }
    }

    private static final Comparator<BlockPos> BLOCK_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

    private static final Comparator<Contact> CONTACT_ORDER = Comparator.comparingDouble(Contact::time)
            .thenComparingInt(AttackContactResolver::priority)
            .thenComparing(Contact::attack)
            .thenComparing(AttackContactResolver::compareDestination);

    private AttackContactResolver() { }

    /** First impact ends the strike; callers retain consumption until that action is retired. */
    public static Resolution resolve(List<? extends Contact> candidates, Set<AttackId> alreadyConsumed) {
        List<Contact> ordered = new ArrayList<>(List.copyOf(candidates));
        ordered.sort(CONTACT_ORDER);
        Set<AttackId> consumed = new HashSet<>(Set.copyOf(alreadyConsumed));
        List<Contact> accepted = new ArrayList<>();
        for (Contact contact : ordered) {
            if (consumed.contains(contact.attack())) continue;
            if (contact instanceof BladeClash clash) {
                if (consumed.contains(clash.other)) continue;
                consumed.add(clash.other);
            }
            consumed.add(contact.attack());
            accepted.add(contact);
        }
        return new Resolution(accepted, consumed);
    }

    // Exact-time ties prefer an obstruction, then a clash, then a body hit.
    private static int priority(Contact contact) {
        return switch (contact) {
            case Obstruction ignored -> 0;
            case BladeClash ignored -> 1;
            case BodyHit ignored -> 2;
        };
    }

    private static int compareDestination(Contact first, Contact second) {
        return switch (first) {
            case Obstruction obstruction -> BLOCK_ORDER.compare(obstruction.block, ((Obstruction) second).block);
            case BladeClash clash -> clash.other.compareTo(((BladeClash) second).other);
            case BodyHit hit -> hit.target.compareTo(((BodyHit) second).target);
        };
    }

    private static void requireTime(double time) {
        if (!Double.isFinite(time) || time < 0) throw new IllegalArgumentException("Contact time must be finite and nonnegative");
    }
}
