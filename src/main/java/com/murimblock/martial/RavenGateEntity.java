package com.murimblock.martial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A door between the worlds (the Raven Messenger Art, RavenFx), shown only: a disc of shadow ringed with amber light and
 * fringed with feathers (geo/raven_gate), lying on the ground under what it swallows ({@link #PIT}), facing down where it
 * lets something fall ({@link #SKY}), or standing up like a door ({@link #DOOR}, turned to its yaw). It opens over a few
 * ticks, turns slowly, and closes over its last ticks; on the client it lights its place (Photon) and sheds feathers. It
 * touches nothing and is never saved.
 */
public class RavenGateEntity extends Entity implements GeoEntity {
    public static final byte PIT = 0, SKY = 1, DOOR = 2;
    /** Ticks it takes to open, and to close. */
    public static final int OPEN_TICKS = 5, CLOSE_TICKS = 6;

    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(RavenGateEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(RavenGateEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FACING = SynchedEntityData.defineId(RavenGateEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(RavenGateEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RavenGateEntity(EntityType<? extends RavenGateEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, PIT).define(SIZE, 1.0F).define(FACING, 0.0F).define(LIFE, 20);
    }

    /** What it is, its radius (blocks), the way it faces (degrees, a door's) and its life (ticks). */
    public RavenGateEntity shape(byte kind, float radius, float facing, int life) {
        entityData.set(KIND, kind);
        entityData.set(SIZE, radius);
        entityData.set(FACING, facing);
        entityData.set(LIFE, life);
        return this;
    }

    /** Lives this many more ticks from now. */
    public void liveFor(int ticks) {
        entityData.set(LIFE, tickCount + ticks);
    }

    public byte kind() { return entityData.get(KIND); }
    public float size() { return entityData.get(SIZE); }
    public float facing() { return entityData.get(FACING); }
    public int life() { return entityData.get(LIFE); }

    /** How open it is now (0 shut, 1 open): eased open over its first ticks, shut over its last. */
    public float openness(float partialTick) {
        float t = tickCount + partialTick, left = life() - t;
        float open = Math.min(1.0F, t / OPEN_TICKS), close = Math.min(1.0F, Math.max(0.0F, left / CLOSE_TICKS));
        float k = Math.min(open, close);
        return 1 - (1 - k) * (1 - k);
    }

    /** Its turn about its own face (degrees), slow, the swirl winding in. */
    public float spin(float partialTick) {
        return (tickCount + partialTick) * 6.0F;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) com.murimblock.client.RavenFxClient.gateTick(this);
        else if (tickCount >= life()) discard();
    }

    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(size() * 1.6 + 1);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
