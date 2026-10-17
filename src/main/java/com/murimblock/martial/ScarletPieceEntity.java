package com.murimblock.martial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A piece of Scarlet Bloom's show (ScarletBloom): a crystal spike, a beam of the cage, a shard, the bloom, a stone
 * torn from the ground, a piece of the burst body (a limb, in its owner's skin), a gout of blood or a pool of it. Shown only: it touches nothing, is never saved, and is moved and taken away by the finish. It
 * is drawn with its own models (client/ScarletPieceRenderer: geo/scarlet_*.geo.json, the thrown crystal's model for a
 * shard, the ground's own block for a stone), turned by {@link #turn()}, its length grown from one value to another
 * over a few ticks.
 */
public class ScarletPieceEntity extends Entity implements GeoEntity {
    /** What it is. */
    public static final byte SPIKE = 0, ROD = 1, SHARD = 2, BLOOM = 3, STONE = 4, LIMB = 5, BLOOD = 6, PUDDLE = 7;
    /** A limb's part of the body. */
    public static final byte HEAD = 0, TORSO = 1, RIGHT_ARM = 2, LEFT_ARM = 3, RIGHT_LEG = 4, LEFT_LEG = 5;

    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH_FROM = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH_TO = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GROW_START = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GROW_TICKS = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Quaternionf> TURN = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Integer> BLOCK = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<java.util.Optional<java.util.UUID>> OWNER = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> PART = SynchedEntityData.defineId(ScarletPieceEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ScarletPieceEntity(EntityType<? extends ScarletPieceEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, SPIKE).define(WIDTH, 0.25F).define(LENGTH_FROM, 1.0F).define(LENGTH_TO, 1.0F).define(GROW_START, 0)
                .define(GROW_TICKS, 0).define(TURN, new Quaternionf()).define(BLOCK, Block.getId(Blocks.DIRT.defaultBlockState()))
                .define(OWNER, java.util.Optional.empty()).define(PART, HEAD);
    }

    /** Sets what it is, its width and length (blocks) and its turn (from +Y). */
    public ScarletPieceEntity shape(byte kind, float width, float length, Quaternionf turn) {
        entityData.set(KIND, kind);
        entityData.set(WIDTH, width);
        entityData.set(LENGTH_FROM, length);
        entityData.set(LENGTH_TO, length);
        entityData.set(TURN, new Quaternionf(turn));
        return this;
    }

    /** A stone of this block. */
    public ScarletPieceEntity block(BlockState state) {
        entityData.set(BLOCK, Block.getId(state));
        return this;
    }

    /** A limb: this part of this player's body, drawn in their skin. */
    public ScarletPieceEntity limb(java.util.UUID owner, byte part) {
        entityData.set(OWNER, java.util.Optional.of(owner));
        entityData.set(PART, part);
        return this;
    }

    public java.util.Optional<java.util.UUID> owner() { return entityData.get(OWNER); }
    public byte part() { return entityData.get(PART); }

    /** Grows (or shrinks) its length from what it is now to this, over these ticks. */
    public void grow(float to, int ticks) {
        entityData.set(LENGTH_FROM, length(0));
        entityData.set(LENGTH_TO, to);
        entityData.set(GROW_START, tickCount);
        entityData.set(GROW_TICKS, Math.max(0, ticks));
    }

    /** Turns it. */
    public void turn(Quaternionf turn) {
        entityData.set(TURN, new Quaternionf(turn));
    }

    public byte kind() { return entityData.get(KIND); }
    public float width() { return entityData.get(WIDTH); }
    public Quaternionf turn() { return new Quaternionf(entityData.get(TURN)); }
    public BlockState blockState() { return Block.stateById(entityData.get(BLOCK)); }

    /** Its length now (blocks), between two ticks. */
    public float length(float partialTick) {
        int ticks = entityData.get(GROW_TICKS);
        float from = entityData.get(LENGTH_FROM), to = entityData.get(LENGTH_TO);
        if (ticks <= 0) return to;
        float k = Math.min(1.0F, Math.max(0.0F, (tickCount + partialTick - entityData.get(GROW_START)) / ticks));
        float ease = 1 - (1 - k) * (1 - k);
        return from + (to - from) * ease;
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 96 * 96; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
