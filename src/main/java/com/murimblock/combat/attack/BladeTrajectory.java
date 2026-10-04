package com.murimblock.combat.attack;

import java.util.List;
import java.util.Objects;

/** Piecewise-linear baked endpoint motion, not a skeletal or continuous collision solver. */
public record BladeTrajectory(double radius, List<Keyframe> keyframes) {
    public record Keyframe(double ageTicks, BladePose pose) {
        public Keyframe {
            if (!Double.isFinite(ageTicks) || ageTicks < 0) {
                throw new IllegalArgumentException("Keyframe time must be finite and nonnegative");
            }
            Objects.requireNonNull(pose);
        }
    }

    public BladeTrajectory {
        if (!Double.isFinite(radius) || radius <= 0 || !Double.isFinite(radius * radius)) {
            throw new IllegalArgumentException("Blade radius must be finite and positive");
        }
        keyframes = List.copyOf(keyframes);
        if (keyframes.size() < 2 || keyframes.getFirst().ageTicks != 0) {
            throw new IllegalArgumentException("A trajectory needs at least two keys, starting at tick zero");
        }
        double previous = -1;
        for (Keyframe key : keyframes) {
            if (key.ageTicks <= previous) {
                throw new IllegalArgumentException("Keyframe times must be strictly increasing");
            }
            previous = key.ageTicks;
        }
    }

    public double durationTicks() {
        return keyframes.getLast().ageTicks;
    }

    public BladePose sample(double ageTicks) {
        if (!Double.isFinite(ageTicks)) throw new IllegalArgumentException("Sample time must be finite");
        if (ageTicks <= 0) return keyframes.getFirst().pose;
        if (ageTicks >= durationTicks()) return keyframes.getLast().pose;
        int low = 0;
        int high = keyframes.size() - 1;
        while (high - low > 1) {
            int middle = low + (high - low) / 2;
            if (keyframes.get(middle).ageTicks <= ageTicks) low = middle;
            else high = middle;
        }
        Keyframe from = keyframes.get(low);
        Keyframe to = keyframes.get(high);
        return from.pose.interpolate(to.pose, (ageTicks - from.ageTicks) / (to.ageTicks - from.ageTicks));
    }
}
