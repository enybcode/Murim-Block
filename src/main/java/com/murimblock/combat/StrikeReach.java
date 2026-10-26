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
 * Who a blow reaches: living entities whose box meets the move's hit shape this tick, not behind a wall. The shape
 * follows the view from the previous tick to the current one, so a turn between two ticks misses nothing; an arc
 * also sweeps its own part of the swing on each active tick.
 */
public final class StrikeReach {
    private static final int SWEEP_SAMPLES = 4;

    private StrikeReach() {
    }

    /** Hit shape this tick: for a box, the view at each sample; for an arc, the slice swept, in absolute yaws. */
    public static boolean meets(LivingEntity attacker, CombatMove move, float fromYaw, float fromPitch, int activeIndex,
                                int activeCount, AABB box) {
        HitShape shape = move.hitbox();
        double[] eye = eye(attacker);
        double[] min = {box.minX, box.minY, box.minZ}, max = {box.maxX, box.maxY, box.maxZ};
        float yaw = attacker.getYRot();
        if (shape.isArc()) {
            double[] slice = HitGeometry.arcSlice(shape, activeIndex, activeCount);
            double turn = Mth.wrapDegrees(yaw - fromYaw);
            double a = Math.min(fromYaw + slice[0], fromYaw + turn + slice[0]);
            double b = Math.max(fromYaw + slice[1], fromYaw + turn + slice[1]);
            return HitGeometry.arcHits(eye, a, b, shape, min, max);
        }
        for (int i = 0; i <= SWEEP_SAMPLES; i++) {
            float t = (float) i / SWEEP_SAMPLES;
            float sampleYaw = fromYaw + Mth.wrapDegrees(yaw - fromYaw) * t;
            float samplePitch = Mth.lerp(t, fromPitch, attacker.getXRot());
            if (HitGeometry.boxHits(eye, sampleYaw, samplePitch, shape, min, max)) {
                return true;
            }
        }
        return false;
    }

    public static List<LivingEntity> targets(LivingEntity attacker, CombatMove move, float fromYaw, float fromPitch,
                                             int activeIndex, int activeCount) {
        Vec3 eye = attacker.getEyePosition();
        AABB area = new AABB(eye, eye).inflate(move.hitbox().reach() + 1);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity target : attacker.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != attacker && e.isAlive() && e.isPickable() && !e.isSpectator() && !attacker.isPassengerOfSameVehicle(e))) {
            AABB box = target.getBoundingBox();
            if (meets(attacker, move, fromYaw, fromPitch, activeIndex, activeCount, box)
                    && !blocked(attacker, eye, closestPoint(box, eye))) {
                found.add(target);
            }
        }
        return found;
    }

    public static double[] eye(Entity entity) {
        Vec3 eye = entity.getEyePosition();
        return new double[] {eye.x, eye.y, eye.z};
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
