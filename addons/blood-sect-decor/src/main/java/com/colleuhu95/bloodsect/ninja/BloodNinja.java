package com.colleuhu95.bloodsect.ninja;

import com.colleuhu95.bloodsect.BloodSectDecor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BloodNinja extends Monster {
    private static final EntityDataAccessor<Long> DAO_START = SynchedEntityData.defineId(BloodNinja.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DAO_YAW = SynchedEntityData.defineId(BloodNinja.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DAO_KIND = SynchedEntityData.defineId(BloodNinja.class, EntityDataSerializers.INT);
    private BlockPos guardPost;
    private final BloodStepAttack bloodStep = new BloodStepAttack(this);
    private final BloodDaoM1Attack daoM1 = new BloodDaoM1Attack(this);
    private long lastDrainTime = -10000;

    public BloodNinja(EntityType<? extends BloodNinja> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BloodSectDecor.BLOOD_DAO.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DAO_START, -1L);
        builder.define(DAO_YAW, 0F);
        builder.define(DAO_KIND, DaoAttackMotion.CLOUD_STEP);
    }

    public float getDaoAttackAge(float partial) {
        long start = this.entityData.get(DAO_START);
        return start < 0 ? DaoAttackMotion.DURATION : Math.max(0, this.level().getGameTime() - start + partial);
    }

    public int getDaoAttackVariant() { return entityData.get(DAO_KIND); }
    public float getDaoYaw() { return entityData.get(DAO_YAW); }
    public boolean isDaoAttacking() { return getDaoAttackAge(0) < DaoAttackMotion.duration(getDaoAttackVariant()); }
    public float getDrainAge(float partial) { return level().getGameTime()-lastDrainTime+partial; }

    @Override public net.minecraft.world.entity.HumanoidArm getMainArm() { return net.minecraft.world.entity.HumanoidArm.RIGHT; }

    void beginDaoAnimation(float yaw) {
        beginDaoAnimation(yaw, DaoAttackMotion.CLOUD_STEP);
    }
    void beginDaoAnimation(float yaw, int kind) {
        entityData.set(DAO_KIND, kind);
        entityData.set(DAO_YAW, yaw);
        entityData.set(DAO_START, level().getGameTime());
        lockDaoFacing();
    }
    void endDaoAnimation() { entityData.set(DAO_START, -1L); }
    private void lockDaoFacing() {
        float yaw=getDaoYaw();
        setYRot(yaw); setYHeadRot(yaw); yBodyRot=yaw;
        if(level().isClientSide) {yBodyRotO=yaw; yHeadRotO=yaw;}
    }
    @Override public void handleEntityEvent(byte event) {
        if(event==78) lastDrainTime=level().getGameTime();
        else super.handleEntityEvent(event);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24)
            .add(Attributes.MOVEMENT_SPEED, .30).add(Attributes.ATTACK_DAMAGE, 4)
            .add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() {
                // An idle scan must not start vanilla's 20-tick path-check cooldown.
                // A newly acquired target can then engage on the next selector update.
                return BloodNinja.this.getTarget() != null && super.canUse();
            }
            @Override public void tick() {
                if (BloodNinja.this.isDaoAttacking()) {
                    BloodNinja.this.getNavigation().stop();
                    // Committed direction: a side-step can evade the visible windup.
                } else super.tick();
            }
            @Override protected void checkAndPerformAttack(LivingEntity target) {
                if (BloodNinja.this.bloodStep.start(target) || BloodNinja.this.daoM1.start(target)) resetAttackCooldown();
            }
            @Override public boolean canContinueToUse() {
                var target = BloodNinja.this.getTarget();
                // Vanilla uses the 10-block wandering restriction here. Combat
                // instead follows the independently bounded 28-block guard zone.
                return target != null && target.isAlive() && BloodNinja.this.canAttack(target);
            }
        });
        this.goalSelector.addGoal(2, new ReturnToPostGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, .65, 40));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 3, true, false, this::canAttack));
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            if (this.guardPost == null) this.setGuardPost(this.blockPosition());
            if (this.getTarget() != null && !this.canAttack(this.getTarget())) {
                this.setTarget(null);
                this.getNavigation().stop();
            }
        }
        if (isDaoAttacking()) {
            getNavigation().stop();
            setDeltaMovement(0,getDeltaMovement().y,0);
        }
        super.tick();
        if (!this.level().isClientSide) { bloodStep.tick(); daoM1.tick(); }
        if (isDaoAttacking()) lockDaoFacing();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (!super.canAttack(target)) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable)) return false;
        var center = this.getGuardPost().getBottomCenter();
        return this.position().distanceToSqr(center) <= 28 * 28 && target.position().distanceToSqr(center) <= 28 * 28;
    }

    public BlockPos getGuardPost() {
        return this.guardPost == null ? this.blockPosition() : this.guardPost;
    }

    public void setGuardPost(BlockPos post) {
        this.guardPost = post.immutable();
        this.restrictTo(this.guardPost, 10);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("GuardPost", this.getGuardPost().asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BloodSectDecor.BLOOD_DAO.get()));
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        this.setDropChance(EquipmentSlot.MAINHAND, 0);
        this.bloodStep.reset();
        this.daoM1.reset();
        if (tag.contains("GuardPost", Tag.TAG_LONG)) this.setGuardPost(BlockPos.of(tag.getLong("GuardPost")));
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { this.playSound(SoundEvents.WOOL_STEP, .12F, 1.15F); }
}
