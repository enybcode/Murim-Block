package com.murimblock.combat;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry of hit shapes, in plain doubles so it can be unit tested. Points are {@code {x, y, z}} in world
 * blocks; yaw and pitch follow Minecraft (yaw 0 looks to +z, 90 to -x; pitch 90 looks down). Boxes are tested with
 * the separating axis theorem; arcs are cut into convex slices and tested the same way in the horizontal plane.
 */
public final class HitGeometry {
    /** Arcs are cut into slices of at most this many degrees, each drawn with a few points. */
    private static final double SLICE = 30;
    private static final int SLICE_POINTS = 4;

    private HitGeometry() {
    }

    /** The view direction for this yaw and pitch. */
    public static double[] look(double yaw, double pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new double[] {-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    /** The box's center and its three half axes (ahead, across, up), each scaled to its half size. */
    private static double[][] boxFrame(double[] eye, double yaw, double pitch, HitShape s) {
        double[] f = look(yaw, pitch);
        double y = Math.toRadians(yaw);
        double[] r = {-Math.cos(y), 0, -Math.sin(y)};
        double[] u = cross(r, f);
        double ahead = s.start() + s.length() / 2;
        double[] c = {eye[0] + f[0] * ahead, eye[1] + f[1] * ahead, eye[2] + f[2] * ahead};
        return new double[][] {c, scale(f, s.length() / 2), scale(r, s.width() / 2), scale(u, s.height() / 2)};
    }

    /** The 8 corners of a box shape, for drawing: bottom face then top face, each in order around. */
    public static double[][] boxCorners(double[] eye, double yaw, double pitch, HitShape s) {
        double[][] fr = boxFrame(eye, yaw, pitch, s);
        double[][] out = new double[8][];
        int[][] signs = {{-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
        for (int i = 0; i < 8; i++) {
            double[] p = fr[0].clone();
            for (int a = 0; a < 3; a++) {
                for (int k = 0; k < 3; k++) {
                    p[k] += fr[a + 1][k] * signs[i][a];
                }
            }
            out[i] = p;
        }
        return out;
    }

    /** Whether a box shape and an axis-aligned box ({@code min}, {@code max}) overlap. */
    public static boolean boxHits(double[] eye, double yaw, double pitch, HitShape s, double[] min, double[] max) {
        double[][] fr = boxFrame(eye, yaw, pitch, s);
        double[] c = fr[0];
        double[][] half = {fr[1], fr[2], fr[3]};
        double[] bc = {(min[0] + max[0]) / 2, (min[1] + max[1]) / 2, (min[2] + max[2]) / 2};
        double[] be = {(max[0] - min[0]) / 2, (max[1] - min[1]) / 2, (max[2] - min[2]) / 2};
        double[][] world = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        List<double[]> axes = new ArrayList<>();
        for (double[] w : world) {
            axes.add(w);
        }
        for (double[] h : half) {
            axes.add(h);
            for (double[] w : world) {
                axes.add(cross(h, w));
            }
        }
        double[] d = {bc[0] - c[0], bc[1] - c[1], bc[2] - c[2]};
        for (double[] axis : axes) {
            double len = Math.sqrt(dot(axis, axis));
            if (len < 1.0E-9) {
                continue;
            }
            double ra = 0;
            for (double[] h : half) {
                ra += Math.abs(dot(h, axis));
            }
            double rb = be[0] * Math.abs(axis[0]) + be[1] * Math.abs(axis[1]) + be[2] * Math.abs(axis[2]);
            if (Math.abs(dot(d, axis)) > ra + rb) {
                return false;
            }
        }
        return true;
    }

    /** The outline of an arc between two absolute yaws, in the horizontal plane at the eyes: the center, then the curve. */
    public static List<double[]> arcOutline(double[] eye, double yawFrom, double yawTo, double radius) {
        List<double[]> points = new ArrayList<>();
        points.add(new double[] {eye[0], eye[2]});
        int steps = Math.max(2, (int) Math.ceil(Math.abs(yawTo - yawFrom) / SLICE * SLICE_POINTS));
        for (int i = 0; i <= steps; i++) {
            double yaw = Math.toRadians(yawFrom + (yawTo - yawFrom) * i / steps);
            points.add(new double[] {eye[0] - Math.sin(yaw) * radius, eye[2] + Math.cos(yaw) * radius});
        }
        return points;
    }

    /** Whether an arc between two absolute yaws (any order) overlaps an axis-aligned box. */
    public static boolean arcHits(double[] eye, double yawFrom, double yawTo, HitShape s, double[] min, double[] max) {
        double bottom = eye[1] - s.height() / 2, top = eye[1] + s.height() / 2;
        if (max[1] < bottom || min[1] > top) {
            return false;
        }
        double a = Math.min(yawFrom, yawTo), b = Math.max(yawFrom, yawTo);
        // Convex slices: the center and a short stretch of curve.
        for (double from = a; from < b || from == a; from += SLICE) {
            double to = Math.min(b, from + SLICE);
            if (sliceHitsRect(arcOutline(eye, from, to, s.radius()), min[0], max[0], min[2], max[2])) {
                return true;
            }
            if (to >= b) {
                break;
            }
        }
        return false;
    }

    /** Separating axis test between a convex polygon (x, z points) and a rectangle. */
    private static boolean sliceHitsRect(List<double[]> poly, double minX, double maxX, double minZ, double maxZ) {
        double[][] rect = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
        List<double[]> axes = new ArrayList<>(List.of(new double[] {1, 0}, new double[] {0, 1}));
        for (int i = 0; i < poly.size(); i++) {
            double[] p = poly.get(i), q = poly.get((i + 1) % poly.size());
            axes.add(new double[] {-(q[1] - p[1]), q[0] - p[0]});
        }
        for (double[] axis : axes) {
            if (Math.abs(axis[0]) + Math.abs(axis[1]) < 1.0E-12) {
                continue;
            }
            double pMin = Double.MAX_VALUE, pMax = -Double.MAX_VALUE, rMin = Double.MAX_VALUE, rMax = -Double.MAX_VALUE;
            for (double[] p : poly) {
                double v = p[0] * axis[0] + p[1] * axis[1];
                pMin = Math.min(pMin, v);
                pMax = Math.max(pMax, v);
            }
            for (double[] p : rect) {
                double v = p[0] * axis[0] + p[1] * axis[1];
                rMin = Math.min(rMin, v);
                rMax = Math.max(rMax, v);
            }
            if (pMax < rMin || rMax < pMin) {
                return false;
            }
        }
        return true;
    }

    /** The part of an arc swept during active tick {@code index} of {@code count}, as degrees of the view. */
    public static double[] arcSlice(HitShape s, int index, int count) {
        double step = (s.to() - s.from()) / Math.max(1, count);
        return new double[] {s.from() + step * index, s.from() + step * (index + 1)};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }
}
