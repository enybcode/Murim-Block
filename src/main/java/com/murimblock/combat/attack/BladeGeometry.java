package com.murimblock.combat.attack;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import org.joml.Intersectiond;
import org.joml.Vector3d;

/** Instantaneous capsule contacts. The runtime must separately sweep motion between updates. */
public final class BladeGeometry {
    public record Contact(Vec3 pointOnFirst, Vec3 pointOnSecond) {
        public Contact {
            BladePose.requireFinite(pointOnFirst);
            BladePose.requireFinite(pointOnSecond);
        }

        public Vec3 midpoint() {
            return pointOnFirst.scale(0.5).add(pointOnSecond.scale(0.5));
        }
    }

    private BladeGeometry() { }

    public static Optional<Contact> intersect(BladePose first, double firstRadius,
                                             BladePose second, double secondRadius) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        requireRadius(firstRadius);
        requireRadius(secondRadius);
        double radius = firstRadius + secondRadius;
        double radiusSquared = radius * radius;
        if (!Double.isFinite(radiusSquared)) throw new IllegalArgumentException("Combined radius is too large");
        double lengthA = first.hilt().distanceTo(first.tip());
        double lengthB = second.hilt().distanceTo(second.tip());
        double shortest = lengthA == 0 ? lengthB : lengthB == 0 ? lengthA : Math.min(lengthA, lengthB);
        double scale = shortest > 0 ? Math.max(1, 1 / shortest) : 1;
        if (!Double.isFinite(scale)) throw new IllegalArgumentException("Blade length is too small for collision precision");

        // Rebase world coordinates and keep nonzero segments above JOML's fixed degeneracy threshold.
        Vec3 origin = first.hilt();
        Vector3d a0 = new Vector3d();
        Vector3d a1 = relative(first.tip(), origin, scale);
        Vector3d b0 = relative(second.hilt(), origin, scale);
        Vector3d b1 = relative(second.tip(), origin, scale);
        Vector3d pointA = new Vector3d();
        Vector3d pointB = new Vector3d();
        Intersectiond.findClosestPointsLineSegments(
                a0.x, a0.y, a0.z, a1.x, a1.y, a1.z,
                b0.x, b0.y, b0.z, b1.x, b1.y, b1.z,
                pointA, pointB);

        // Near-parallel cancellation can select the wrong end; retain the best endpoint projections too.
        includeEndpoint(a0, b0, b1, pointA, pointB);
        includeEndpoint(a1, b0, b1, pointA, pointB);
        includeEndpoint(b0, a0, a1, pointB, pointA);
        includeEndpoint(b1, a0, a1, pointB, pointA);
        // JOML 1.10.5 returns a dot product for two degenerate segments; use its closest points.
        double distance = pointA.distance(pointB) / scale;
        if (!Double.isFinite(distance)) throw new IllegalArgumentException("Non-finite blade separation");
        if (distance > radius) return Optional.empty();
        return Optional.of(new Contact(world(pointA, origin, scale), world(pointB, origin, scale)));
    }

    private static void includeEndpoint(Vector3d endpoint, Vector3d start, Vector3d end,
                                        Vector3d closestEndpoint, Vector3d closestSegment) {
        Vector3d candidate = new Vector3d();
        if (start.equals(end)) candidate.set(start);
        else Intersectiond.findClosestPointOnLineSegment(start.x, start.y, start.z, end.x, end.y, end.z,
                endpoint.x, endpoint.y, endpoint.z, candidate);
        if (endpoint.distanceSquared(candidate) < closestEndpoint.distanceSquared(closestSegment)) {
            closestEndpoint.set(endpoint);
            closestSegment.set(candidate);
        }
    }

    private static Vector3d relative(Vec3 point, Vec3 origin, double scale) {
        return new Vector3d((point.x - origin.x) * scale, (point.y - origin.y) * scale, (point.z - origin.z) * scale);
    }

    private static Vec3 world(Vector3d point, Vec3 origin, double scale) {
        return new Vec3(point.x / scale, point.y / scale, point.z / scale).add(origin);
    }

    private static void requireRadius(double radius) {
        if (!Double.isFinite(radius) || radius < 0) {
            throw new IllegalArgumentException("Contact radius must be finite and nonnegative");
        }
    }
}
