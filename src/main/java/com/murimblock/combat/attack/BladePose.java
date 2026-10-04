package com.murimblock.combat.attack;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/** Blade endpoints in blocks. Local +X matches world +X at zero yaw; +Y up, +Z forward. */
public record BladePose(Vec3 hilt, Vec3 tip) {
    public BladePose {
        requireFinite(hilt);
        requireFinite(tip);
    }

    public BladePose interpolate(BladePose other, double fraction) {
        Objects.requireNonNull(other);
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Interpolation fraction must be within [0, 1]");
        }
        return new BladePose(hilt.lerp(other.hilt, fraction), tip.lerp(other.tip, fraction));
    }

    /** Uses Minecraft yaw: zero faces +Z, positive 90 degrees faces -X. */
    public BladePose toWorld(Vec3 origin, double yawDegrees) {
        requireFinite(origin);
        if (!Double.isFinite(yawDegrees)) throw new IllegalArgumentException("Yaw must be finite");
        double radians = Math.toRadians(yawDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new BladePose(rotate(hilt, cos, sin).add(origin), rotate(tip, cos, sin).add(origin));
    }

    private static Vec3 rotate(Vec3 point, double cos, double sin) {
        return new Vec3(point.x * cos - point.z * sin, point.y, point.x * sin + point.z * cos);
    }

    static void requireFinite(Vec3 point) {
        Objects.requireNonNull(point);
        if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)) {
            throw new IllegalArgumentException("Blade coordinates must be finite");
        }
    }
}
