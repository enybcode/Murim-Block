package com.murimblock.martial;

/**
 * Blood Crystal's charge, in numbers. Held, the crystal forms in the hand; past the minimum charge, every few ticks it
 * draws one point of the blood gauge into itself, growing and gaining power. The whole gauge (20 points) makes the
 * great crystal, 3 blocks across, which explodes where it lands. A crystal thrown without points is the small one.
 */
public final class BloodCrystalCharge {
    /** Ticks before the crystal can be thrown (0.6 s), and between two points drawn from the gauge (0.1 s: the whole gauge in 2 s). */
    public static final int MIN_TICKS = 12, POINT_TICKS = 2;
    /** Points a crystal can hold: the whole gauge. */
    public static final int MAX_POINTS = BloodCrystalState.MAX_GAUGE;
    /** A held crystal leaves on its own after 3 s. */
    public static final int AUTO_RELEASE_TICKS = 60;
    /** Damage shares: the small crystal, and what each point adds (a full gauge adds 1.8); the great crystal's explosion and its reach. */
    public static final double BASE_SHARE = 1.2, POINT_SHARE = 1.8 / MAX_POINTS, EXPLOSION_SHARE = 2.0, EXPLOSION_RADIUS = 3.5;
    /** Scale of the model (0.75 block at 1): forming 0.2 to 0.6, then a full gauge adds 3.4; the great crystal is 4 (3 blocks). */
    public static final float FORMED_SCALE = 0.6F, POINT_SCALE = 3.4F / MAX_POINTS;

    private BloodCrystalCharge() {
    }

    /** Whether, `held` ticks into the charge, the crystal draws a point (it already holds `points`). */
    public static boolean drawsPoint(long held, int points) {
        return held >= MIN_TICKS && points < MAX_POINTS && (held - MIN_TICKS) % POINT_TICKS == 0;
    }

    /** The crystal's model scale after `held` ticks with `points` drawn. */
    public static float scale(long held, int points) {
        float formed = 0.2F + (FORMED_SCALE - 0.2F) * Math.clamp(held / (float) MIN_TICKS, 0F, 1F);
        return formed + POINT_SCALE * Math.clamp(points, 0, MAX_POINTS);
    }

    /** Share of the thrower's attack the crystal deals to what it hits. */
    public static double share(int points) {
        return BASE_SHARE + POINT_SHARE * Math.clamp(points, 0, MAX_POINTS);
    }

    /** Whether a crystal holding `points` is the great one, which explodes. */
    public static boolean great(int points) {
        return points >= MAX_POINTS;
    }

    /** Flight: launch speed (blocks/tick) and gravity (blocks/tick²); the great crystal is slower and heavier. */
    public static double speed(int points) {
        return great(points) ? 0.65 : 1.0;
    }

    public static double gravity(int points) {
        return great(points) ? 0.035 : 0.025;
    }

    /** Share of the explosion at `distance` blocks from its centre: whole at the centre, half at its edge, none beyond. */
    public static double explosionShare(double distance) {
        if (distance > EXPLOSION_RADIUS) return 0;
        return EXPLOSION_SHARE * (1 - 0.5 * distance / EXPLOSION_RADIUS);
    }
}
