package com.murimblock.martial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A shape of the Crescent Moon Halberd's moonlight (CrescentMoonHalberd), shown only: a flying crescent
 * (geo/moon_crescent), Moonlit Circle's ring on the ground (geo/moon_ring, turning), Mirror Lake's mirror (geo/moon_mirror,
 * rippling), the ultimate's full moon (geo/moon_full, turning) or one of its beams (geo/moon_beam, stretched down). It
 * touches nothing and is never saved; the server moves it and takes it away when its time is over. Drawn by
 * client/MoonQiRenderer, turned by {@link #turn()}, scaled by {@link #size()} (a beam by its length too), grown over its
 * first ticks and faded over its last.
 */
public class MoonQiEntity extends Entity implements GeoEntity {
    public static final byte CRESCENT = 0, RING = 1, MIRROR = 2, MOON = 3, BEAM = 4;

    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(MoonQiEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MoonQiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(MoonQiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GROW_TICKS = SynchedEntityData.defineId(MoonQiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(MoonQiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Quaternionf> TURN = SynchedEntityData.defineId(MoonQiEntity.class, EntityDataSerializers.QUATERNION);
    /** How many of its last ticks it fades over. */
    public static final int FADE_TICKS = 6;

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin"), RIPPLE = RawAnimation.begin().thenLoop("ripple"),
            TURNING = RawAnimation.begin().thenLoop("turn");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MoonQiEntity(EntityType<? extends MoonQiEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, CRESCENT).define(SIZE, 1.0F).define(LENGTH, 1.0F).define(GROW_TICKS, 0).define(LIFE, 20).define(TURN, new Quaternionf());
    }

    /** Sets what it is, its size, its length (a beam's), the ticks it grows over, its turn and its life. */
    public MoonQiEntity shape(byte kind, float size, float length, int grow, Quaternionf turn, int life) {
        entityData.set(KIND, kind);
        entityData.set(SIZE, size);
        entityData.set(LENGTH, length);
        entityData.set(GROW_TICKS, Math.max(0, grow));
        entityData.set(TURN, new Quaternionf(turn));
        entityData.set(LIFE, life);
        return this;
    }

    public void turnTo(Quaternionf turn) { entityData.set(TURN, new Quaternionf(turn)); }

    /** Lives this many more ticks from now. */
    public void liveFor(int ticks) { entityData.set(LIFE, tickCount + ticks); }

    public byte kind() { return entityData.get(KIND); }
    public Quaternionf turn() { return new Quaternionf(entityData.get(TURN)); }
    public float length() { return entityData.get(LENGTH); }
    public int life() { return entityData.get(LIFE); }

    /** Its size now, between two ticks: grown from nothing over its first ticks, eased out. */
    public float size(float partialTick) {
        int ticks = entityData.get(GROW_TICKS);
        float to = entityData.get(SIZE);
        if (ticks <= 0) return to;
        float k = Math.min(1.0F, Math.max(0.0F, (tickCount + partialTick) / ticks));
        return to * (1 - (1 - k) * (1 - k));
    }

    /** How much of it shows (1: whole), fading over its last ticks. */
    public float alpha(float partialTick) {
        float left = life() - tickCount - partialTick;
        return Math.max(0.0F, Math.min(1.0F, left / FADE_TICKS));
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= life()) discard();
    }

    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        double reach = Math.max(entityData.get(SIZE) * 3, entityData.get(LENGTH)) + 2;
        return getBoundingBox().inflate(reach);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 200 * 200; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "moon", 0, state -> switch (kind()) {
            case RING -> state.setAndContinue(SPIN);
            case MIRROR -> state.setAndContinue(RIPPLE);
            case MOON -> state.setAndContinue(TURNING);
            default -> PlayState.STOP;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
