package com.murimblock.combat;

/**
 * Where a move can hit, around the striker's eyes. Distances in blocks, angles in degrees.
 *
 * <ul>
 * <li>{@code box}: a box along the view (it follows where the eyes look, up and down too), from {@code start} to
 * {@code start + length} ahead, {@code width} across and {@code height} tall. Straight punches, thrusts, spears.</li>
 * <li>{@code arc}: a slice of a cylinder of {@code radius} around the eyes, {@code height} tall, swept from
 * {@code from} to {@code to} degrees of the view (negative to the left) over the active ticks. Hooks, sword cuts.</li>
 * </ul>
 */
public record HitShape(String type, double start, double length, double width, double height, double radius,
                       double from, double to) {
    public static final String BOX = "box";
    public static final String ARC = "arc";

    public static HitShape box(double start, double length, double width, double height) {
        return new HitShape(BOX, start, length, width, height, 0, 0, 0);
    }

    public static HitShape arc(double radius, double height, double from, double to) {
        return new HitShape(ARC, 0, 0, 0, height, radius, from, to);
    }

    public boolean isArc() {
        return ARC.equals(type);
    }

    /** How far from the eyes it reaches, for the first rough search. */
    public double reach() {
        return isArc() ? radius : start + length + Math.max(width, height);
    }
}
