package com.murimblock.shenfa;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A sheet of cloth (Epic Fight simulates its capes this way): a grid of points moved by their own momentum and
 * gravity (Verlet), kept at their distance from their neighbours, the top row pinned to the body, and kept out of the
 * body by planes. Positions are in the world (blocks), so the cloth trails when the body runs or turns. A cape, and
 * later robe tails and sashes.
 */
public final class Cloth {
    /** One fixed step of the simulation (seconds); a frame runs as many as its time holds. */
    public static final float STEP = 1 / 60f;
    private static final int ITERATIONS = 6;
    private final int columns, rows;
    private final float spacingX, spacingY;
    private final Vector3f[] position, previous;
    private final boolean[] pinned;
    private float gravity = 18, damping = 0.985f;
    private float pending;
    private boolean started;

    /**
     * @param width  the sheet's width (blocks), across {@code columns} points
     * @param height its height (blocks), down {@code rows} points
     */
    public Cloth(int columns, int rows, float width, float height) {
        this.columns = columns;
        this.rows = rows;
        spacingX = width / (columns - 1);
        spacingY = height / (rows - 1);
        position = new Vector3f[columns * rows];
        previous = new Vector3f[columns * rows];
        pinned = new boolean[columns * rows];
        for (int i = 0; i < position.length; i++) {
            position[i] = new Vector3f();
            previous[i] = new Vector3f();
        }
        for (int c = 0; c < columns; c++) {
            pinned[c] = true;
        }
    }

    /** A plane the cloth stays on one side of: points closer than {@code distance} behind it are pushed out. */
    public record Plane(Vector3fc point, Vector3fc normal, float distance) {
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public Vector3fc point(int column, int row) {
        return position[row * columns + column];
    }

    public Cloth gravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    public Cloth damping(float damping) {
        this.damping = damping;
        return this;
    }

    /**
     * Moves the top row to {@code top} (one point per column, left to right) and runs the simulation for {@code seconds};
     * the first call lays the sheet straight down from there. Planes push points out of the body.
     */
    public void update(Vector3fc[] top, Vector3fc down, float seconds, Plane... planes) {
        if (!started) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    Vector3f at = new Vector3f(down).normalize().mul(r * spacingY).add(top[c]);
                    position[r * columns + c].set(at);
                    previous[r * columns + c].set(at);
                }
            }
            started = true;
        }
        pending = Math.min(pending + seconds, 0.1f);
        while (pending >= STEP) {
            pending -= STEP;
            step(top, planes);
        }
    }

    /** Forgets the motion: the next update lays the sheet down again (a teleport). */
    public void reset() {
        started = false;
        pending = 0;
    }

    private void step(Vector3fc[] top, Plane[] planes) {
        float drop = gravity * STEP * STEP;
        Vector3f velocity = new Vector3f();
        for (int i = 0; i < position.length; i++) {
            if (pinned[i]) {
                previous[i].set(position[i]);
                position[i].set(top[i % columns]);
                continue;
            }
            Vector3f at = position[i];
            at.sub(previous[i], velocity).mul(damping);
            previous[i].set(at);
            at.add(velocity).sub(0, drop, 0);
        }
        Vector3f gap = new Vector3f();
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    int i = r * columns + c;
                    if (c + 1 < columns) {
                        keep(i, i + 1, spacingX, gap);
                    }
                    if (r + 1 < rows) {
                        keep(i, i + columns, spacingY, gap);
                    }
                    if (c + 1 < columns && r + 1 < rows) {
                        float diagonal = (float) Math.hypot(spacingX, spacingY);
                        keep(i, i + columns + 1, diagonal, gap);
                        keep(i + 1, i + columns, diagonal, gap);
                    }
                }
            }
            for (Plane plane : planes) {
                for (int i = 0; i < position.length; i++) {
                    if (pinned[i]) {
                        continue;
                    }
                    float depth = gap.set(position[i]).sub(plane.point()).dot(plane.normal()) - plane.distance();
                    if (depth < 0) {
                        position[i].fma(-depth, plane.normal());
                    }
                }
            }
        }
    }

    /** Pulls two points toward their rest distance (a pinned point does not move). */
    private void keep(int a, int b, float rest, Vector3f gap) {
        gap.set(position[b]).sub(position[a]);
        float length = gap.length();
        if (length < 1e-6f) {
            return;
        }
        float push = (length - rest) / length;
        boolean fixedA = pinned[a], fixedB = pinned[b];
        if (fixedA && fixedB) {
            return;
        }
        float shareA = fixedA ? 0 : fixedB ? 1 : 0.5f, shareB = fixedB ? 0 : fixedA ? 1 : 0.5f;
        position[a].fma(push * shareA, gap);
        position[b].fma(-push * shareB, gap);
    }
}
