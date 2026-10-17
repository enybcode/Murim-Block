package com.murimblock.mob.majin;

import com.murimblock.mob.SonOfTheDemonEntity;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A sun of the Majin Fist (MajinFist): the Cruel Sun he conjures over his palm and hurls, and the great sun of The One.
 * Held, it follows a point over him and grows; thrown, it flies an arc to where it was aimed, then bursts
 * ({@link MajinFist#sunBurst}). Drawn as layered light (client/CruelSunRenderer) with a Photon light of its own. Its
 * size (blocks across) is seen by the clients. Not saved: a sun alive when the server stops is gone.
 */
public class CruelSunEntity extends Entity {
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(CruelSunEntity.class, EntityDataSerializers.FLOAT);
    /** How much of its light is left (1: whole): a burst fades as it swells. */
    private static final EntityDataAccessor<Float> FADE = SynchedEntityData.defineId(CruelSunEntity.class, EntityDataSerializers.FLOAT);
    /** A burst: a ball of fire swelling to {@link #burstTo} over {@link #burstTicks} and fading out (-1: not one). */
    private int burstTicks = -1, bursting;
    private float burstTo;

    @Nullable private SonOfTheDemonEntity owner;
    /** Held over him: how high, how far behind; the size it grows to, and in how many ticks. */
    private double heldUp, heldBack;
    private float growTo, growFrom;
    private int growTicks, held;
    /** Thrown: from where, to where, how high its arc, in how many ticks, how far along. */
    @Nullable private Vec3 from, to;
    private double arc;
    private int flight = -1, flown;
    /** Its burst: how far it reaches and how hard (a share of his attack). */
    private double burstRadius;
    private float burstShare;

    public CruelSunEntity(EntityType<? extends CruelSunEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 0.3F);
        builder.define(FADE, 1F);
    }

    public float size() {
        return entityData.get(SIZE);
    }

    public void setSize(float size) {
        entityData.set(SIZE, size);
    }

    public float fade() {
        return entityData.get(FADE);
    }

    /** A ball of fire where a sun bursts: swelling to {@code size} across in {@code ticks}, fading as it goes. */
    public void burst(float size, int ticks) {
        burstTo = size;
        burstTicks = Math.max(1, ticks);
        bursting = 0;
        flight = -1;
        owner = null;
    }

    /** Held over {@code owner}: {@code up} blocks over his feet, {@code back} behind him, growing to {@code size} in {@code ticks}. */
    public void hold(SonOfTheDemonEntity owner, double up, double back, float size, int ticks) {
        this.owner = owner;
        heldUp = up;
        heldBack = back;
        growFrom = size();
        growTo = size;
        growTicks = Math.max(1, ticks);
        held = 0;
        flight = -1;
        follow();
    }

    /** Thrown at {@code target}: an arc {@code arc} blocks high, {@code ticks} long; its burst {@code radius} wide, {@code share} of his attack. */
    public void hurl(Vec3 target, double arc, int ticks, double radius, float share) {
        from = position();
        to = target;
        this.arc = arc;
        flight = Math.max(1, ticks);
        flown = 0;
        burstRadius = radius;
        burstShare = share;
    }

    public boolean thrown() {
        return flight > 0;
    }

    @Nullable public Vec3 target() {
        return to;
    }

    private void follow() {
        if (owner == null) return;
        Vec3 back = Vec3.directionFromRotation(0, owner.getYRot()).multiply(1, 0, 1).normalize().scale(-heldBack);
        setPos(owner.getX() + back.x, owner.getY() + heldUp, owner.getZ() + back.z);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            // Flames licking off it, sparks falling.
            float s = size();
            for (int i = 0; i < Math.max(1, (int) (s * 2)); i++) {
                double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI, r = s * 0.5;
                level().addParticle(ParticleTypes.FLAME, getX() + Math.cos(a) * Math.sin(b) * r, getY() + Math.cos(b) * r, getZ() + Math.sin(a) * Math.sin(b) * r, 0, 0.02, 0);
            }
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (burstTicks > 0) {
            bursting++;
            float k = Math.min(1, bursting / (float) burstTicks);
            setSize(burstTo * (1 - (1 - k) * (1 - k)));
            entityData.set(FADE, 1 - k * k);
            if (k >= 1) discard();
            return;
        }
        if (flight < 0) {
            if (owner == null || !owner.isAlive()) { discard(); return; }
            held++;
            setSize(growFrom + (growTo - growFrom) * Math.min(1, held / (float) growTicks));
            follow();
            if (tickCount % 10 == 0) level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 2.0F, 0.5F);
            if (tickCount > 1200) discard();
            return;
        }
        flown++;
        double k = Math.min(1, flown / (double) flight);
        Vec3 at = from.lerp(to, k).add(0, arc * 4 * k * (1 - k), 0);
        setPos(at.x, at.y, at.z);
        if (tickCount % 4 == 0) level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.6F);
        if (k >= 1) {
            if (owner != null) MajinFist.sunBurst(owner, level, to, burstRadius, burstShare, size());
            discard();
        }
    }

    @Override public boolean isPickable() {
        return false;
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public boolean shouldBeSaved() {
        return false;
    }
}
