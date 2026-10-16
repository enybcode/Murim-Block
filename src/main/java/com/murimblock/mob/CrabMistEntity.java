package com.murimblock.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Crab King's mist (Enzo, 16/10: "an attack where he makes mist and becomes invisible", docs/mobs/crab-king.md): a
 * thick cloud {@link #RADIUS} blocks round where he breathed it, {@link #LIFETIME} ticks, thickening at first and
 * thinning at the end. Nothing of its own but the cloud: each client draws its drifting puffs and, for a player inside,
 * shuts the view down to a few blocks (client/CrabMistClient). The king is unseen in it (CrabKingEntity). Never saved.
 */
public final class CrabMistEntity extends Entity {
    public static final double RADIUS = 11;
    public static final int LIFETIME = 240;
    /** Ticks to thicken at first and to thin at the end. */
    public static final int FADE = 30;

    public CrabMistEntity(EntityType<? extends CrabMistEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** How thick the mist is now, 0 to 1. */
    public float density(float partial) {
        float t = tickCount + partial;
        return Mth.clamp(Math.min(t / FADE, (LIFETIME - t) / FADE), 0, 1);
    }

    /** How thick it is at a point (0 outside, thickest at its heart, thinning over its last three blocks). */
    public float densityAt(Vec3 at, float partial) {
        double d = Math.sqrt(Math.pow(at.x - getX(), 2) + Math.pow(at.z - getZ(), 2));
        if (d > RADIUS || at.y < getY() - 3 || at.y > getY() + 9) return 0;
        return density(partial) * (float) Mth.clamp((RADIUS - d) / 3.0, 0, 1);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            // Its drifting puffs, as many as it is thick.
            float k = density(0);
            for (int i = 0; i < 12 * k; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * RADIUS;
                level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * r, getY() + random.nextDouble() * 3.5, getZ() + Math.sin(a) * r,
                        (random.nextDouble() - 0.5) * 0.02, 0.003, (random.nextDouble() - 0.5) * 0.02);
            }
            return;
        }
        if (tickCount >= LIFETIME) discard();
    }

    @Override public boolean isPickable() {
        return false;
    }

    @Override public boolean shouldBeSaved() {
        return false;
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
