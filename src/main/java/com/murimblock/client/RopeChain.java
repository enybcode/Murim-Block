package com.murimblock.client;

/**
 * A chain of equal links hung from a point the hand carries, as TasselPendulum is for one cord: each joint has its
 * weight (Verlet), the links keep their length, and a little stiffness keeps each link in line with the one before it
 * (the first with the handle), so a swung chain flies out straight and a still one droops. Pure arithmetic, in any
 * space where "down" is given; gravity counted in link lengths, so it swings at the same pace whatever its scale.
 */
public final class RopeChain {
    /** Gravity, in link lengths per second squared (about the world's, for a link of a seventh of a block). */
    static final double GRAVITY = 70.0;
    /** How fast a swing dies down, per second. */
    static final double DAMPING = 2.5;
    /** How fast each link turns back in line with the one before it, per second. */
    static final double STIFFNESS = 9.0;
    static final double MAX_STEP = 1.0 / 240.0;
    /** Rounds of keeping the links at their length, per step. */
    static final int ITERATIONS = 4;
    /** A gap longer than this (seconds) with no drawing: the chain is laid out again instead of jumping. */
    static final double STALE = 0.25;

    private final int links;
    /** Joints 0 (the anchor) to links, x y z each; and where each was the step before. */
    private final double[] at, before;
    private long lastNanos;
    private boolean started;

    public RopeChain(int links) {
        this.links = links;
        this.at = new double[(links + 1) * 3];
        this.before = new double[(links + 1) * 3];
    }

    public int links() {
        return links;
    }

    /**
     * Moves the anchor to (x, y, z) at {@code nanos}, the handle pointing along (ax, ay, az), each link `length` long, and
     * runs the chain up to then; (downX, downY, downZ) is gravity's direction.
     */
    public void follow(double x, double y, double z, double ax, double ay, double az, double length,
                       double downX, double downY, double downZ, long nanos) {
        double dt = (nanos - lastNanos) / 1.0e9;
        if (!started || dt > STALE || dt < 0 || length <= 0) {
            layOut(x, y, z, ax, ay, az, length);
            lastNanos = nanos;
            started = true;
            return;
        }
        if (dt == 0) return;
        lastNanos = nanos;
        int steps = (int) Math.ceil(dt / MAX_STEP);
        double h = dt / steps;
        double fromX = at[0], fromY = at[1], fromZ = at[2];
        double g = GRAVITY * length * h * h, keep = Math.exp(-DAMPING * h), stiff = 1 - Math.exp(-STIFFNESS * h);
        for (int s = 1; s <= steps; s++) {
            double t = (double) s / steps;
            at[0] = fromX + (x - fromX) * t; at[1] = fromY + (y - fromY) * t; at[2] = fromZ + (z - fromZ) * t;
            for (int i = 1; i <= links; i++) {
                int k = i * 3;
                for (int c = 0; c < 3; c++) {
                    double now = at[k + c];
                    at[k + c] = now + (now - before[k + c]) * keep;
                    before[k + c] = now;
                }
                at[k] += downX * g; at[k + 1] += downY * g; at[k + 2] += downZ * g;
            }
            // Each link turns toward the line of the one before it.
            double dx = ax, dy = ay, dz = az;
            for (int i = 1; i <= links; i++) {
                int k = i * 3, p = k - 3;
                at[k] += (at[p] + dx * length - at[k]) * stiff;
                at[k + 1] += (at[p + 1] + dy * length - at[k + 1]) * stiff;
                at[k + 2] += (at[p + 2] + dz * length - at[k + 2]) * stiff;
                double ex = at[k] - at[p], ey = at[k + 1] - at[p + 1], ez = at[k + 2] - at[p + 2], d = Math.sqrt(ex * ex + ey * ey + ez * ez);
                if (d > 1.0e-9) { dx = ex / d; dy = ey / d; dz = ez / d; }
            }
            // The links hold their length (the anchor does not move).
            for (int n = 0; n < ITERATIONS; n++) {
                for (int i = 1; i <= links; i++) {
                    int k = i * 3, p = k - 3;
                    double ex = at[k] - at[p], ey = at[k + 1] - at[p + 1], ez = at[k + 2] - at[p + 2], d = Math.sqrt(ex * ex + ey * ey + ez * ez);
                    if (d < 1.0e-9) { ex = downX; ey = downY; ez = downZ; d = 1; }
                    double off = (d - length) / d;
                    if (i == 1) {
                        at[k] -= ex * off; at[k + 1] -= ey * off; at[k + 2] -= ez * off;
                    } else {
                        at[p] += ex * off / 2; at[p + 1] += ey * off / 2; at[p + 2] += ez * off / 2;
                        at[k] -= ex * off / 2; at[k + 1] -= ey * off / 2; at[k + 2] -= ez * off / 2;
                    }
                }
            }
        }
    }

    /** The chain laid straight along the handle, still. */
    private void layOut(double x, double y, double z, double ax, double ay, double az, double length) {
        for (int i = 0; i <= links; i++) {
            int k = i * 3;
            at[k] = before[k] = x + ax * length * i;
            at[k + 1] = before[k + 1] = y + ay * length * i;
            at[k + 2] = before[k + 2] = z + az * length * i;
        }
    }

    /** Joint i (0 = the anchor, links = the end): {x, y, z}. */
    public double[] joint(int i) {
        return new double[] {at[i * 3], at[i * 3 + 1], at[i * 3 + 2]};
    }
}
