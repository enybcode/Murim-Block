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
        Vector3d pointA = new Vector3d();
        Vector3d pointB = new Vector3d();
        Intersectiond.findClosestPointsLineSegments(
                first.hilt().x, first.hilt().y, first.hilt().z, first.tip().x, first.tip().y, first.tip().z,
                second.hilt().x, second.hilt().y, second.hilt().z, second.tip().x, second.tip().y, second.tip().z,
                pointA, pointB);
        // JOML 1.10.5 returns a dot product for two degenerate segments; use its closest points.
        double distanceSquared = pointA.distanceSquared(pointB);
        if (!Double.isFinite(distanceSquared)) throw new IllegalArgumentException("Non-finite blade separation");
        if (distanceSquared > radiusSquared) return Optional.empty();
        return Optional.of(new Contact(new Vec3(pointA.x, pointA.y, pointA.z),
                new Vec3(pointB.x, pointB.y, pointB.z)));
    }

    private static void requireRadius(double radius) {
        if (!Double.isFinite(radius) || radius < 0) {
            throw new IllegalArgumentException("Contact radius must be finite and nonnegative");
        }
    }
}
