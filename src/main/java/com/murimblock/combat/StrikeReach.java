package com.murimblock.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Who a blow reaches: living entities within reach, inside the cone in front of the eyes, not behind a wall. The
 * cone sweeps from the view of the previous tick to the current one, so a turn between two ticks misses nothing.
 */
public final class StrikeReach {
    private static final int SWEEP_SAMPLES = 4;

    private StrikeReach() {
    }

    public static List<LivingEntity> targets(LivingEntity attacker, CombatMove move, float fromYaw, float fromPitch) {
        Vec3 eye = attacker.getEyePosition();
        double reach = move.reach();
        List<Vec3> looks = new ArrayList<>(SWEEP_SAMPLES + 1);
        for (int i = 0; i <= SWEEP_SAMPLES; i++) {
            float t = (float) i / SWEEP_SAMPLES;
            float yaw = fromYaw + Mth.wrapDegrees(attacker.getYRot() - fromYaw) * t;
            float pitch = Mth.lerp(t, fromPitch, attacker.getXRot());
            looks.add(Vec3.directionFromRotation(pitch, yaw));
        }
        List<LivingEntity> found = new ArrayList<>();
        AABB area = new AABB(eye, eye).inflate(reach + 1);
        for (LivingEntity target : attacker.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != attacker && e.isAlive() && e.isPickable() && !e.isSpectator() && !attacker.isPassengerOfSameVehicle(e))) {
            AABB box = target.getBoundingBox();
            Vec3 nearest = closestPoint(box, eye);
            if (nearest.distanceTo(eye) > reach || !inAnyCone(looks, eye, nearest, box.getCenter(), move.angle() / 2)
                    || blocked(attacker, eye, nearest)) {
                continue;
            }
            found.add(target);
        }
        return found;
    }

    private static boolean inAnyCone(List<Vec3> looks, Vec3 eye, Vec3 nearest, Vec3 center, double halfAngle) {
        for (Vec3 look : looks) {
            if (withinCone(look, nearest.subtract(eye), halfAngle) || withinCone(look, center.subtract(eye), halfAngle)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code toTarget} lies within {@code halfAngle} degrees of {@code look}; a target at the eyes counts. */
    public static boolean withinCone(Vec3 look, Vec3 toTarget, double halfAngle) {
        double length = toTarget.length();
        if (length < 1.0E-4) {
            return true;
        }
        double cos = look.normalize().dot(toTarget.scale(1 / length));
        return cos >= Math.cos(Math.toRadians(halfAngle));
    }

    public static Vec3 closestPoint(AABB box, Vec3 point) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    private static boolean blocked(Entity attacker, Vec3 eye, Vec3 point) {
        return attacker.level().clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                attacker)).getType() != HitResult.Type.MISS;
    }
}
