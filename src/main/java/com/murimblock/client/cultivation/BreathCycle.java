package com.murimblock.client.cultivation;

/**
 * The breathing ring of meditation: over a 4 s cycle it grows from 0.4 to 1.5 times the target circle (inhale) and
 * shrinks back (exhale), crossing the circle twice. A press is good when the ring is close enough to the circle.
 */
public final class BreathCycle {
    public static final float CYCLE_SECONDS = 4;
    public static final float MIN_SCALE = 0.4f;
    public static final float MAX_SCALE = 1.5f;
    /** How far from the circle (in circle sizes) a press still counts as good: about a third of a second. */
    public static final float GOOD_WINDOW = 0.15f;

    private BreathCycle() {
    }

    /** Ring size relative to the target circle, eased in and out, at this many seconds into meditation. */
    public static float scale(float seconds) {
        double phase = (seconds % CYCLE_SECONDS) / CYCLE_SECONDS;
        return (float) (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - Math.cos(2 * Math.PI * phase)) / 2);
    }

    public static boolean inhaling(float seconds) {
        return seconds % CYCLE_SECONDS < CYCLE_SECONDS / 2;
    }

    public static boolean good(float seconds) {
        return Math.abs(scale(seconds) - 1) <= GOOD_WINDOW;
    }
}
