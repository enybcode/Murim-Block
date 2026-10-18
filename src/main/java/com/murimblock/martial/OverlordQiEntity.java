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
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A shape of the Overlord Spear's Qi (OverlordSpear), shown only: Tyrant's Thrust's beam (a lance of red and gold light
 * stretched along its line, geo/overlord_beam) or the ultimate's giant spear (the Spear of Ruin twenty times its size,
 * geo/overlord_giant_spear). It touches nothing and is never saved; the server moves it and takes it away when its time
 * is over. Drawn by client/OverlordQiRenderer, turned by {@link #turn()}, its length grown over a few ticks, fading out
 * over its last ticks.
 */
public class OverlordQiEntity extends Entity implements GeoEntity {
    public static final byte BEAM = 0, SPEAR = 1;

    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(OverlordQiEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(OverlordQiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH_FROM = SynchedEntityData.defineId(OverlordQiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH_TO = SynchedEntityData.defineId(OverlordQiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GROW_TICKS = SynchedEntityData.defineId(OverlordQiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(OverlordQiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Quaternionf> TURN = SynchedEntityData.defineId(OverlordQiEntity.class, EntityDataSerializers.QUATERNION);
    /** How many of its last ticks it fades over. */
    public static final int FADE_TICKS = 6;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OverlordQiEntity(EntityType<? extends OverlordQiEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, BEAM).define(WIDTH, 1.0F).define(LENGTH_FROM, 1.0F).define(LENGTH_TO, 1.0F).define(GROW_TICKS, 0)
                .define(LIFE, 20).define(TURN, new Quaternionf());
    }

    /** Sets what it is, its width, its length grown from `from` to `to` over `grow` ticks, its turn (from +Y) and its life. */
    public OverlordQiEntity shape(byte kind, float width, float from, float to, int grow, Quaternionf turn, int life) {
        entityData.set(KIND, kind);
        entityData.set(WIDTH, width);
        entityData.set(LENGTH_FROM, from);
        entityData.set(LENGTH_TO, to);
        entityData.set(GROW_TICKS, Math.max(0, grow));
        entityData.set(TURN, new Quaternionf(turn));
        entityData.set(LIFE, life);
        return this;
    }

    /** Lives this many more ticks from now. */
    public void liveFor(int ticks) {
        entityData.set(LIFE, tickCount + ticks);
    }

    public byte kind() { return entityData.get(KIND); }
    public float width() { return entityData.get(WIDTH); }
    public Quaternionf turn() { return new Quaternionf(entityData.get(TURN)); }
    public int life() { return entityData.get(LIFE); }

    /** Its length now (blocks), between two ticks, eased out. */
    public float length(float partialTick) {
        int ticks = entityData.get(GROW_TICKS);
        float from = entityData.get(LENGTH_FROM), to = entityData.get(LENGTH_TO);
        if (ticks <= 0) return to;
        float k = Math.min(1.0F, Math.max(0.0F, (tickCount + partialTick) / ticks));
        return from + (to - from) * (1 - (1 - k) * (1 - k));
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

    /** Drawn whenever any of its length may be in sight (its own box is a tiny point at one end). */
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        double l = Math.max(length(0), entityData.get(LENGTH_TO)) + 1;
        return getBoundingBox().inflate(l);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160 * 160; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
