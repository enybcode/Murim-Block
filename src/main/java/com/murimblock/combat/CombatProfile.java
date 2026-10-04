package com.murimblock.combat;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** A martial art's rules for one weapon category, independent of its material. */
public record CombatProfile(ResourceLocation martialArt, WeaponCategory category, Guard guard) {
    public CombatProfile {
        Objects.requireNonNull(martialArt);
        Objects.requireNonNull(category);
        Objects.requireNonNull(guard);
    }

    public record Guard(double coneDegrees, double qiPerDamage, double minimumQi,
                        double movementScale, int impactTicks, int breakTicks, int attackRecoveryTicks) {
        public Guard {
            if (!Double.isFinite(coneDegrees) || coneDegrees <= 0 || coneDegrees > 180
                    || !Double.isFinite(qiPerDamage) || qiPerDamage <= 0
                    || !Double.isFinite(minimumQi) || minimumQi <= 0
                    || !Double.isFinite(movementScale) || movementScale <= 0 || movementScale > 1
                    || impactTicks <= 0 || breakTicks <= 0 || attackRecoveryTicks <= 0) {
                throw new IllegalArgumentException("Invalid guard profile");
            }
        }

        public boolean covers(Vec3 facing, Vec3 incomingDirection) {
            Objects.requireNonNull(facing);
            Objects.requireNonNull(incomingDirection);
            double a = facing.horizontalDistance();
            double b = incomingDirection.horizontalDistance();
            if (!Double.isFinite(a) || !Double.isFinite(b) || a < 1.0E-6 || b < 1.0E-6) return false;
            double dot = (facing.x / a) * (incomingDirection.x / b)
                    + (facing.z / a) * (incomingDirection.z / b);
            return dot >= Math.cos(Math.toRadians(coneDegrees / 2));
        }

        public double cost(float damage) {
            if (!Float.isFinite(damage) || damage <= 0) return 0;
            double cost = Math.max(minimumQi, damage * qiPerDamage);
            return Double.isFinite(cost) ? cost : Double.POSITIVE_INFINITY;
        }

        public boolean canPay(double qi, float damage) {
            double cost = cost(damage);
            return cost > 0 && Double.isFinite(cost) && Double.isFinite(qi) && qi >= cost;
        }
    }
}
