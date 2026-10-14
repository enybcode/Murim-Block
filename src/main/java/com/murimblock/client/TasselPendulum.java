package com.murimblock.client;

/**
 * A tassel on its cord: a weight hanging at a fixed length from a pivot that the hand carries. It lags when the hand
 * moves, swings back past the rest and settles hanging down. Pure arithmetic, in any space where "down" is given: the
 * world's blocks for an item in the world or in first person, the screen's pixels for an entity drawn in a menu.
 * Gravity is counted in lengths of cord, so a tassel swings at the same pace whatever its size or space.
 */
public final class TasselPendulum {
    /** Gravity, in cord lengths per second squared: a period near 0.75 s, a small tassel's quick swing. */
    static final double GRAVITY = 70.0;
    /** How fast a swing dies down, per second: settled in about a second and a half. */
    static final double DAMPING = 3.0;
    /** The longest step of the simulation: fast cuts are split, so the cord never stretches or explodes. */
    static final double MAX_STEP = 1.0 / 240.0;
    /** The fastest the weight may move, in cord lengths per second. */
    static final double MAX_SPEED = 120.0;
    /** A gap longer than this (seconds) with no drawing: the tassel is laid back at rest instead of jumping. */
    static final double STALE = 0.25;

    private double pivotX, pivotY, pivotZ;
    private double tipX, tipY, tipZ;
    private double velX, velY, velZ;
    private long lastNanos;
    private boolean started;

    /**
     * Moves the pivot to (x, y, z) at the time {@code nanos} and runs the swing up to then.
     * (downX, downY, downZ): the unit vector of gravity in that space; length: the cord's length in that space.
     */
    public void follow(double x, double y, double z, double length, double downX, double downY, double downZ, long nanos) {
        double dt = (nanos - lastNanos) / 1.0e9;
        if (!started || dt > STALE || dt < 0 || length <= 0) {
            rest(x, y, z, length, downX, downY, downZ);
            lastNanos = nanos;
            started = true;
            return;
        }
        if (dt == 0) return;
        lastNanos = nanos;
        int steps = (int) Math.ceil(dt / MAX_STEP);
        double h = dt / steps;
        double fromX = pivotX, fromY = pivotY, fromZ = pivotZ;
        double gravity = GRAVITY * length, keep = Math.exp(-DAMPING * h), maxSpeed = MAX_SPEED * length;
        for (int i = 1; i <= steps; i++) {
            // The hand moves evenly over the frame.
            double t = (double) i / steps;
            double px = fromX + (x - fromX) * t, py = fromY + (y - fromY) * t, pz = fromZ + (z - fromZ) * t;
            velX = (velX + downX * gravity * h) * keep;
            velY = (velY + downY * gravity * h) * keep;
            velZ = (velZ + downZ * gravity * h) * keep;
            double nx = tipX + velX * h, ny = tipY + velY * h, nz = tipZ + velZ * h;
            // The cord holds: the weight stays at its length from the pivot.
            double dx = nx - px, dy = ny - py, dz = nz - pz, d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 1.0e-9) { dx = downX; dy = downY; dz = downZ; d = 1; }
            nx = px + dx / d * length; ny = py + dy / d * length; nz = pz + dz / d * length;
            velX = (nx - tipX) / h; velY = (ny - tipY) / h; velZ = (nz - tipZ) / h;
            double speed = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
            if (speed > maxSpeed) { velX *= maxSpeed / speed; velY *= maxSpeed / speed; velZ *= maxSpeed / speed; }
            tipX = nx; tipY = ny; tipZ = nz;
        }
        pivotX = x; pivotY = y; pivotZ = z;
    }

    private void rest(double x, double y, double z, double length, double downX, double downY, double downZ) {
        pivotX = x; pivotY = y; pivotZ = z;
        tipX = x + downX * length; tipY = y + downY * length; tipZ = z + downZ * length;
        velX = velY = velZ = 0;
    }

    /** The direction from the pivot to the weight, a unit vector: {x, y, z}. */
    public double[] direction() {
        double dx = tipX - pivotX, dy = tipY - pivotY, dz = tipZ - pivotZ, d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return d < 1.0e-9 ? new double[] {0, -1, 0} : new double[] {dx / d, dy / d, dz / d};
    }
}
