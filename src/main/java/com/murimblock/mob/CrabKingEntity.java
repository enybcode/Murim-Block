package com.murimblock.mob;

import com.murimblock.integration.epicfight.CrabKingAnimations;
import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The Crab King (蟹王), the boss of the crab art (docs/mobs/crab-king.md): a martial artist the crab men's sickness took
 * whole, drawn 1.5 times a man's size (client/CrabKingRenderer). Not spawned in the world: he waits in a structure (a
 * spawn egg, /summon, until it is built), and never despawns. Tough (260 health, armour 14, his shell barely moved by
 * blows) and slow, his pincers his weapons: a sweep, a lunging snap and a slam whose landing sends a shockwave
 * (CrabKingPatch), every one parryable. Under half his health he roars and calls two armoured crabs out of the ground,
 * once. He breathes under water and moves faster in it. His health shows in a boss bar to the players near him. The crab
 * art's manual will be his drop when the art is made.
 */
public class CrabKingEntity extends Monster {
    /** The slam's shockwave: its reach around the landing point, and its share of his attack. */
    public static final double SHOCKWAVE_RADIUS = 3.6;
    public static final float SHOCKWAVE_DAMAGE = 0.6F;
    /** Ticks before the shockwave in which the slam's pincers may have struck or been parried. */
    private static final int SLAM_WINDOW = 12;
    /** How many armoured crabs he calls, under half his health. */
    public static final int CRABS_CALLED = 2;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean shockwaveSent;
    /** Whether he has roared and called his crabs (saved: once in his life). */
    private boolean enraged;

    public CrabKingEntity(EntityType<? extends CrabKingEntity> type, Level level) {
        super(type, level);
        xpReward = 60;
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createCrabKingAttributes() {
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260).add(Attributes.ARMOR, 14).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.ATTACK_DAMAGE, 11).add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.33))
                .add(EpicFightAttributes.STUN_ARMOR, 14.0).add(EpicFightAttributes.IMPACT, 2.8);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(ArmoredCrabEntity.class, CrabManEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Epic Fight installs his attack and chase goals for his empty hands (CrabKingPatch).
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ---- a crab: he breathes under water (the entity type tag minecraft:can_breathe_under_water), and is quicker in it

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && isInWater() && tickCount % 20 == 0)
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
    }

    // ---- the boss bar

    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    /** Whether this player sees his boss bar. */
    public boolean showsBossBarTo(ServerPlayer player) {
        return bossBar.getPlayers().contains(player);
    }

    public boolean isEnraged() {
        return enraged;
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        setAggressive(getTarget() != null);
        if (!enraged && getHealth() < getMaxHealth() / 2 && level() instanceof ServerLevel server) enrage(server);
    }

    /** Under half his health, once: he roars and two armoured crabs come out of the ground around him. */
    public void enrage(ServerLevel server) {
        enraged = true;
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(CrabKingAnimations.ROAR, 0.0F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 0.6F);
        for (int i = 0; i < CRABS_CALLED; i++) {
            double a = getYRot() * Math.PI / 180 + (i == 0 ? 1.6 : -1.6);
            Vec3 at = position().add(-Math.sin(a) * 2.5, 0, Math.cos(a) * 2.5);
            ArmoredCrabEntity crab = MurimEntities.ARMORED_CRAB.get().create(server);
            if (crab == null) continue;
            crab.moveTo(at.x, getY(), at.z, getYRot(), 0);
            crab.finalizeSpawn(server, server.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            if (getTarget() != null) crab.setTarget(getTarget());
            server.addFreshEntity(crab);
            BlockState ground = server.getBlockState(BlockPos.containing(at.x, getY() - 0.5, at.z));
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, getY() + 0.1, at.z, 30, 0.5, 0.1, 0.5, 0.1);
        }
    }

    // ---- the slam's shockwave

    @Override public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) shockwaveTick(server);
    }

    private void shockwaveTick(ServerLevel server) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        boolean slamming = main != null && main.getRealAnimation() == CrabKingAnimations.SLAM && !main.isEnd();
        if (!slamming) {
            shockwaveSent = false;
            return;
        }
        if (shockwaveSent || main.getElapsedTime() < CrabKingAnimations.SLAM_IMPACT) return;
        shockwaveSent = true;
        shockwave(server);
    }

    /**
     * The pincers meet the ground: everyone near the landing point (not his crabs) takes a share of his attack and is
     * thrown back and up. A guard raised in time parries it; who the pincers just struck or who just parried them is
     * spared: the slam is one blow.
     */
    public void shockwave(ServerLevel server) {
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 at = position().add(look.scale(2.2));
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * SHOCKWAVE_DAMAGE;
        AABB area = new AABB(at, at).inflate(SHOCKWAVE_RADIUS, 1.5, SHOCKWAVE_RADIUS);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive()
                && !(e instanceof ArmoredCrabEntity) && !(e instanceof CrabManEntity) && e.distanceToSqr(at) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS)) {
            boolean struck = victim.getLastHurtByMob() == this && victim.tickCount - victim.getLastHurtByMobTimestamp() <= SLAM_WINDOW;
            if (struck || MurimGuardSkill.parriedWithin(victim, SLAM_WINDOW)) continue;
            if (victim.hurt(damageSources().mobAttack(this), damage)) {
                Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
                Vec3 push = away.lengthSqr() < 1e-4 ? look : away.normalize();
                victim.push(push.x * 0.8, 0.45, push.z * 0.8);
                victim.hurtMarked = true;
            }
        }
        BlockState ground = server.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 70, 1.6, 0.1, 1.6, 0.15);
        server.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.3, at.z, 40, 1.4, 0.2, 1.4, 0.2);
        server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.1, at.z, 20, 1.8, 0.05, 1.8, 0.04);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.7F, 1.2F);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.TURTLE_EGG_CRACK, SoundSource.HOSTILE, 1.4F, 0.5F);
    }

    // ---- saved

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", enraged);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enraged = tag.getBoolean("Enraged");
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }

    // A shell creaking, a deep clicking voice.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.TURTLE_AMBIENT_LAND; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.TURTLE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.TURTLE_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.5F; }
}
