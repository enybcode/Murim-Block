package com.murimblock.client;

/**
 * A strip of cloth hung by its top edge, as a chain of equal links down its middle (Verlet, as RopeChain): a cape, a
 * sash's tail, a tassel. Gravity pulls it down; a gentle spring turns each link back toward its rest slant (the
 * first toward the slant the body gives it, the others toward the one above), so it drapes as it was modelled when
 * still and flies back behind a running man; it never passes through a plane (the back or the belly it hangs against).
 * Pure arithmetic, in world blocks; the body's motion reaches it through the anchor, the slant and the plane, each
 * frame.
 */
public final class ClothStrand {
    /** Gravity, in blocks per second squared. */
    static final double GRAVITY = 14.0;
    /** How fast a swing dies down, per second. */
    static final double DAMPING = 3.0;
    /** The spring turning each link back toward its rest slant, per second squared: cloth, so soft (it sways at about 1.2 Hz). */
    static final double STIFFNESS = 60.0;
    static final double MAX_STEP = 1.0 / 240.0;
    /** Rounds of keeping the links at their length, per step. */
    static final int ITERATIONS = 4;
    /** A gap longer than this (seconds) with no drawing: the strip is laid out again instead of jumping. */
    static final double STALE = 0.25;

    private final int links;
    /** Joints 0 (the anchor, the middle of the top edge) to links, x y z each; and where each was the step before. */
    private final double[] at, before;
    private long lastNanos;
    private boolean started;

    public ClothStrand(int links) {
        this.links = Math.max(1, links);
        this.at = new double[(this.links + 1) * 3];
        this.before = new double[(this.links + 1) * 3];
    }

    public int links() {
        return links;
    }

    /**
     * Moves the anchor to (x, y, z) at {@code nanos} and runs the strip up to then: each link {@code length} long,
     * (sx, sy, sz) the rest slant (a unit vector), and the strip kept on the side of the plane through (px, py, pz)
     * that its unit normal (nx, ny, nz) points to.
     */
    public void follow(double x, double y, double z, double sx, double sy, double sz, double length,
                       double px, double py, double pz, double nx, double ny, double nz, long nanos) {
        double dt = (nanos - lastNanos) / 1.0e9;
        if (!started || dt > STALE || dt < 0 || length <= 0) {
            layOut(x, y, z, sx, sy, sz, length);
            lastNanos = nanos;
            started = true;
            return;
        }
        if (dt == 0) return;
        lastNanos = nanos;
        int steps = (int) Math.ceil(dt / MAX_STEP);
        double h = dt / steps;
        double fromX = at[0], fromY = at[1], fromZ = at[2];
        double g = GRAVITY * h * h, keep = Math.exp(-DAMPING * h), stiff = STIFFNESS * h * h;
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
                at[k + 1] -= g;
            }
            // Each link turns toward its rest slant: the first the body's, the others the one above's.
            double dx = sx, dy = sy, dz = sz;
            for (int i = 1; i <= links; i++) {
                int k = i * 3, p = k - 3;
                at[k] += (at[p] + dx * length - at[k]) * stiff;
                at[k + 1] += (at[p + 1] + dy * length - at[k + 1]) * stiff;
                at[k + 2] += (at[p + 2] + dz * length - at[k + 2]) * stiff;
                double ex = at[k] - at[p], ey = at[k + 1] - at[p + 1], ez = at[k + 2] - at[p + 2], d = Math.sqrt(ex * ex + ey * ey + ez * ez);
                if (d > 1.0e-9) { dx = ex / d; dy = ey / d; dz = ez / d; }
            }
            for (int n = 0; n < ITERATIONS; n++) {
                // The links hold their length (the anchor does not move).
                for (int i = 1; i <= links; i++) {
                    int k = i * 3, p = k - 3;
                    double ex = at[k] - at[p], ey = at[k + 1] - at[p + 1], ez = at[k + 2] - at[p + 2], d = Math.sqrt(ex * ex + ey * ey + ez * ez);
                    if (d < 1.0e-9) { ex = sx; ey = sy; ez = sz; d = 1; }
                    double off = (d - length) / d;
                    if (i == 1) {
                        at[k] -= ex * off; at[k + 1] -= ey * off; at[k + 2] -= ez * off;
                    } else {
                        at[p] += ex * off / 2; at[p + 1] += ey * off / 2; at[p + 2] += ez * off / 2;
                        at[k] -= ex * off / 2; at[k + 1] -= ey * off / 2; at[k + 2] -= ez * off / 2;
                    }
                }
                // Nothing sinks behind the plane: it is pushed back onto it.
                for (int i = 1; i <= links; i++) {
                    int k = i * 3;
                    double under = (at[k] - px) * nx + (at[k + 1] - py) * ny + (at[k + 2] - pz) * nz;
                    if (under < 0) { at[k] -= nx * under; at[k + 1] -= ny * under; at[k + 2] -= nz * under; }
                }
            }
        }
    }

    /** The strip laid along its rest slant, still. */
    private void layOut(double x, double y, double z, double sx, double sy, double sz, double length) {
        for (int i = 0; i <= links; i++) {
            int k = i * 3;
            at[k] = before[k] = x + sx * length * i;
            at[k + 1] = before[k + 1] = y + sy * length * i;
            at[k + 2] = before[k + 2] = z + sz * length * i;
        }
    }

    /** Joint i (0 = the anchor, links = the bottom): {x, y, z}. */
    public double[] joint(int i) {
        return new double[] {at[i * 3], at[i * 3 + 1], at[i * 3 + 2]};
    }
}
