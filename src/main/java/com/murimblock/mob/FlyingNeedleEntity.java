package com.murimblock.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A needle thrown by the flying needles (MurimItems.FLYING_NEEDLES, SectWeaponAnimations): flies straight and fast from
 * the thrower's chest toward where they look, and stops in the first body or block it meets. Each needle hurts for a
 * share of the thrower's attack, as a projectile (a guard parries it like an arrow); the needles of one throw each
 * count, so a fan of three into one body hurts three times. Lives less than a second and is never saved.
 */
public final class FlyingNeedleEntity extends Projectile {
    /** Blocks per tick, and how far it flies (blocks). */
    public static final double SPEED = 1.8, RANGE = 16;
    /** Each needle's share of its thrower's attack. */
    public static final float DAMAGE_SHARE = 0.4F;

    private double travelled;
    private float damage;

    public FlyingNeedleEntity(EntityType<? extends FlyingNeedleEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /**
     * Throws `count` needles from `thrower`, spread evenly over `spread` degrees about where it looks, `pitch` degrees
     * further down (an air throw at the ground below).
     */
    public static void throwFan(LivingEntity thrower, int count, float spread, float pitch) {
        if (thrower.level().isClientSide) return;
        float damage = (float) thrower.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_SHARE;
        float xRot = Math.min(90, thrower.getXRot() + pitch);
        for (int i = 0; i < count; i++) {
            float yaw = thrower.getYRot() + (count == 1 ? 0 : spread * ((float) i / (count - 1) - 0.5F));
            FlyingNeedleEntity needle = new FlyingNeedleEntity(MurimEntities.FLYING_NEEDLE.get(), thrower.level());
            needle.setOwner(thrower);
            needle.damage = damage;
            Vec3 dir = Vec3.directionFromRotation(xRot, yaw);
            Vec3 from = thrower.getEyePosition().add(0, -0.3, 0).add(dir.scale(0.5));
            needle.moveTo(from.x, from.y, from.z, yaw, xRot);
            needle.setDeltaMovement(dir.scale(SPEED));
            thrower.level().addFreshEntity(needle);
        }
    }

    /** Where it flies, from its heading. */
    public Vec3 heading() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }

    @Override public void tick() {
        super.tick();
        Vec3 from = position(), motion = heading().scale(SPEED), to = from.add(motion);
        if (level().isClientSide) {
            // Each client flies it itself along its heading; the server decides what it hits and when it ends.
            setPos(to);
            return;
        }
        HitResult wall = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() == HitResult.Type.BLOCK) to = wall.getLocation();
        // The nearest body on its path this tick.
        LivingEntity hit = null;
        double nearest = Double.MAX_VALUE;
        for (Entity e : level().getEntities(this, new AABB(from, to).inflate(0.3), this::canHitEntity)) {
            if (!(e instanceof LivingEntity target)) continue;
            var through = target.getBoundingBox().inflate(0.15).clip(from, to);
            if (through.isEmpty()) continue;
            double d = through.get().distanceToSqr(from);
            if (d < nearest) { nearest = d; hit = target; }
        }
        if (hit != null) {
            strike(hit);
            discard();
            return;
        }
        if (wall.getType() == HitResult.Type.BLOCK) {
            discard();
            return;
        }
        setPos(to);
        travelled += SPEED;
        if (travelled >= RANGE || tickCount > 20) discard();
    }

    /** The client flies it on its own (see tick): the server's position updates are not followed. */
    @Override public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (position().distanceToSqr(x, y, z) > 9) setPos(x, y, z);
    }

    @Override protected boolean canHitEntity(Entity target) {
        return target.isAlive() && target != getOwner() && target.isPickable() && !target.isSpectator()
                && !(getOwner() != null && target.isPassengerOfSameVehicle(getOwner()));
    }

    private void strike(LivingEntity target) {
        Entity owner = getOwner();
        // Each needle of a fan counts: the previous one's moment of invulnerability does not stop it.
        target.invulnerableTime = 0;
        target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), damage);
    }

    @Override public boolean shouldBeSaved() {
        return false;
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
}
