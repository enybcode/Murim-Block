package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.item.MurimItems;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.ItemStack;
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
 * The Bandit Chief (山大王), the mountain bandits' leader and the first mini-boss: a huge brute with an immense two-handed
 * axe (docs/mobs/bandit-chief.md). Not spawned in the world yet (spawn egg, /summon). Tough (120 health, armour 8, barely
 * moved by blows, not thrown off balance by every hit) but slow, and every one of his attacks can be parried
 * (BanditChiefPatch: the cleave, the sweep and the leaping slam, whose landing sends a shockwave around him). Hit, he
 * calls the bandits around him. His health shows in a boss bar to the players near him.
 */
public class BanditChiefEntity extends Monster {
    /** The great axe drops one time in ten. */
    private static final float AXE_DROP_CHANCE = 0.1F;
    /** The slam's shockwave: its reach around the landing point, and its share of his attack. */
    public static final double SHOCKWAVE_RADIUS = 3.2;
    public static final float SHOCKWAVE_DAMAGE = 0.6F;
    /** Ticks before the shockwave in which the slam's axe may have struck or been parried. */
    private static final int SLAM_WINDOW = 12;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    /** Whether the slam now playing has already sent its shockwave. */
    private boolean shockwaveSent;

    public BanditChiefEntity(EntityType<? extends BanditChiefEntity> type, Level level) {
        super(type, level);
        xpReward = 40;
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createChiefAttributes() {
        // No attack of his own: the axe gives its 8.
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120).add(Attributes.ARMOR, 8).add(Attributes.ARMOR_TOUGHNESS, 2)
                .add(Attributes.ATTACK_DAMAGE, 0).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.STEP_HEIGHT, 1.0))
                .add(EpicFightAttributes.STUN_ARMOR, 10.0).add(EpicFightAttributes.IMPACT, 2.5);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(MountainBanditEntity.class, BanditChiefEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Epic Fight installs his attack and chase goals for the axe in hand (BanditChiefPatch).
    }

    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.CHIEF_GREAT_AXE.get()));
        setDropChance(EquipmentSlot.MAINHAND, AXE_DROP_CHANCE);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return result;
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

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
    }

    // ---- the slam's shockwave

    @Override public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) shockwaveTick(server);
    }

    private void shockwaveTick(ServerLevel server) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        boolean slamming = main != null && main.getRealAnimation() == MurimAnimations.CHIEF_SLAM && !main.isEnd();
        if (!slamming) {
            shockwaveSent = false;
            return;
        }
        if (shockwaveSent || main.getElapsedTime() < MurimAnimations.CHIEF_SLAM_IMPACT) return;
        shockwaveSent = true;
        shockwave(server);
    }

    /**
     * The axe meets the ground: everyone near the landing point (not the bandits) takes a share of his attack and is
     * thrown back and up. It is a blow from him: a guard raised in time toward him parries it. Who the axe itself just
     * struck, or who just parried it, is spared: the slam is one blow, parried once.
     */
    public void shockwave(ServerLevel server) {
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 at = position().add(look.scale(1.6));
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * SHOCKWAVE_DAMAGE;
        AABB area = new AABB(at, at).inflate(SHOCKWAVE_RADIUS, 1.5, SHOCKWAVE_RADIUS);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive()
                && !(e instanceof MountainBanditEntity) && !(e instanceof BanditChiefEntity) && e.distanceToSqr(at) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS)) {
            boolean struck = victim.getLastHurtByMob() == this && victim.tickCount - victim.getLastHurtByMobTimestamp() <= SLAM_WINDOW;
            if (struck || MurimGuardSkill.parriedWithin(victim, SLAM_WINDOW)) continue;
            if (victim.hurt(damageSources().mobAttack(this), damage)) {
                Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
                Vec3 push = away.lengthSqr() < 1e-4 ? look : away.normalize();
                victim.push(push.x * 0.7, 0.45, push.z * 0.7);
                victim.hurtMarked = true;
            }
        }
        BlockState ground = server.getBlockState(net.minecraft.core.BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 60, 1.4, 0.1, 1.4, 0.15);
        server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 2, 0.6, 0.1, 0.6, 0.0);
        server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.1, at.z, 24, 1.8, 0.05, 1.8, 0.04);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.7F, 1.3F);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 0.6F);
    }

    // A big man's voice: the vindicator's, deeper.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VINDICATOR_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VINDICATOR_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VINDICATOR_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.7F; }
}
