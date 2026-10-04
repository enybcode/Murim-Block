package com.murimblock.combat.attack;

import java.util.Optional;

/** All ages are server ticks, including fractional ticks; phase ends are exclusive. */
public record AttackTimeline(int windupTicks, int contactTicks, int recoveryTicks) {
    public enum Phase { NOT_STARTED, WINDUP, CONTACT, RECOVERY, FINISHED }

    public record ContactWindow(double fromInclusive, double toExclusive) {
        public ContactWindow {
            if (!Double.isFinite(fromInclusive) || !Double.isFinite(toExclusive)
                    || fromInclusive < 0 || fromInclusive >= toExclusive) {
                throw new IllegalArgumentException("Invalid contact window");
            }
        }
    }

    public AttackTimeline {
        if (windupTicks < 0 || contactTicks <= 0 || recoveryTicks < 0
                || (long) windupTicks + contactTicks + recoveryTicks > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid attack phase durations");
        }
    }

    public int contactEndsAt() {
        return windupTicks + contactTicks;
    }

    public int durationTicks() {
        return contactEndsAt() + recoveryTicks;
    }

    public Phase phaseAt(double ageTicks) {
        requireFinite(ageTicks);
        if (ageTicks < 0) return Phase.NOT_STARTED;
        if (ageTicks < windupTicks) return Phase.WINDUP;
        if (ageTicks < contactEndsAt()) return Phase.CONTACT;
        if (ageTicks < durationTicks()) return Phase.RECOVERY;
        return Phase.FINISHED;
    }

    /** Clips an update interval to contact time, even when an update skips a whole phase. */
    public Optional<ContactWindow> contactWindow(double fromInclusive, double toExclusive) {
        requireFinite(fromInclusive);
        requireFinite(toExclusive);
        if (fromInclusive > toExclusive) {
            throw new IllegalArgumentException("Attack time must not run backwards");
        }
        double from = Math.max(fromInclusive, windupTicks);
        double to = Math.min(toExclusive, contactEndsAt());
        return from < to ? Optional.of(new ContactWindow(from, to)) : Optional.empty();
    }

    private static void requireFinite(double time) {
        if (!Double.isFinite(time)) throw new IllegalArgumentException("Attack time must be finite");
    }
}
