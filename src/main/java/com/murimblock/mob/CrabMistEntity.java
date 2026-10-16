package com.murimblock.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Crab King's mist (Enzo, 16/10: "an attack where he makes mist and becomes invisible", docs/mobs/crab-king.md): a
 * thick cloud round where he breathed it, {@link #LIFETIME} ticks, thickening at first and thinning at the end. In the
 * open it is {@link #RADIUS} blocks round him; in his lair it fills the whole cavern (Enzo, 16/10: "it must be in the
 * whole arena"; CrabKingEntity sizes it). Nothing of its own but the cloud: each client draws its drifting puffs and,
 * for a player inside, shuts the view down to a few blocks (client/CrabMistClient). The king is unseen in it
 * (CrabKingEntity). Never saved.
 */
public final class CrabMistEntity extends Entity {
    /** Its size in the open: radius, and height over where it was breathed. */
    public static final double RADIUS = 11, HEIGHT = 9;
    /** The largest it can be (his lair's cavern), for clients looking for the mist round them. */
    public static final double MAX_RADIUS = 50;
    public static final int LIFETIME = 240;
    /** Ticks to thicken at first and to thin at the end. */
    public static final int FADE = 30;
    /** Round a player, puffs are drawn this far at most (a mist as wide as the cavern is drawn round who is in it). */
    private static final double PUFFS_ROUND = 16;
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(CrabMistEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TALL = SynchedEntityData.defineId(CrabMistEntity.class, EntityDataSerializers.FLOAT);

    public CrabMistEntity(EntityType<? extends CrabMistEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Its radius and its height over where it stands (blocks). */
    public void setSize(double radius, double height) {
        entityData.set(SIZE, (float) Math.min(radius, MAX_RADIUS));
        entityData.set(TALL, (float) height);
    }

    public double radius() {
        return entityData.get(SIZE);
    }

    public double height() {
        return entityData.get(TALL);
    }

    /** How thick the mist is now, 0 to 1. */
    public float density(float partial) {
        float t = tickCount + partial;
        return Mth.clamp(Math.min(t / FADE, (LIFETIME - t) / FADE), 0, 1);
    }

    /** How thick it is at a point (0 outside, thickest at its heart, thinning over its last three blocks). */
    public float densityAt(Vec3 at, float partial) {
        double r = radius(), d = Math.sqrt(Math.pow(at.x - getX(), 2) + Math.pow(at.z - getZ(), 2));
        if (d > r || at.y < getY() - 3 || at.y > getY() + height()) return 0;
        return density(partial) * (float) Mth.clamp((r - d) / 3.0, 0, 1);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, (float) RADIUS);
        builder.define(TALL, (float) HEIGHT);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            float k = density(0);
            double r = radius();
            if (r <= PUFFS_ROUND) puffs(getX(), getY(), getZ(), r, 12 * k);
            // Wide as the cavern: drawn round those in it, so it is as thick everywhere.
            else for (Player player : level().players())
                if (densityAt(player.position(), 0) > 0) puffs(player.getX(), player.getY() - 1, player.getZ(), PUFFS_ROUND, 12 * k);
            return;
        }
        if (tickCount >= LIFETIME) discard();
    }

    /** Its drifting puffs, so many round (x, z) from y up, within its own bounds. */
    private void puffs(double x, double y, double z, double round, float count) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * round;
            Vec3 at = new Vec3(x + Math.cos(a) * r, y + random.nextDouble() * 3.5, z + Math.sin(a) * r);
            if (densityAt(at, 0) <= 0) continue;
            level().addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, (random.nextDouble() - 0.5) * 0.02, 0.003, (random.nextDouble() - 0.5) * 0.02);
        }
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
