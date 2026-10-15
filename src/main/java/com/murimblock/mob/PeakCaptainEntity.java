package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.registry.entries.EpicFightAttributes;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The Captain of the Peak Guard (docs/mobs/peak-captain.md), the Orthodox Alliance's spear boss: in cloth
 * (scripts/animation/peak-captain-3d.cjs: a long azure coat, a white cape that swings, client/ClothLayer), wearing the
 * Peak Guard's armour and holding the Crimson Dragon Spear, both items of their own. Never spawned naturally (an egg,
 * /summon); in the world only on the leitai of the Alliance's arena (world/structure/site/AllianceArena), where he stands
 * guard at his post and goes back to it after a fight.
 * <p>
 * Neutral, as the Alliance's soldiers: he attacks no one first. Struck, he turns on whoever struck him and calls the
 * soldiers near him; he stays angry 30 to 50 seconds after the last blow. Strong (180 health, his armour's 20 points,
 * hard to stagger), Epic Fight's spear moves (PeakCaptainPatch); every one can be parried. His health shows in a boss
 * bar. He never loses his gear by the usual drop: his loot table leaves one piece of the armour every time and the
 * spear now and then.
 */
public class PeakCaptainEntity extends PathfinderMob implements NeutralMob {
    /** How long he stays angry after the last blow, and how often, angry, he calls the soldiers about him. */
    private static final UniformInt ANGER_TIME = TimeUtil.rangeOfSeconds(30, 50), ALERT_INTERVAL = TimeUtil.rangeOfSeconds(4, 6);

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int angerTime, ticksUntilNextAlert;
    @Nullable private UUID angerTarget;
    /** Where he stands guard (his spawn point): he goes back there when he has no one to fight. */
    @Nullable private BlockPos post;

    public PeakCaptainEntity(EntityType<? extends PeakCaptainEntity> type, Level level) {
        super(type, level);
        xpReward = 50;
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createCaptainAttributes() {
        // No armour or attack of his own: the Peak Guard's armour gives 20 (toughness 8), the spear its 6.
        return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.0))
                .add(EpicFightAttributes.STUN_ARMOR, 8.0).add(EpicFightAttributes.IMPACT, 1.8);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // He does not wander: he stands at his post, and goes back to it after a fight.
        goalSelector.addGoal(5, new ReturnToPostGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Only whoever strikes him (and whoever he is still angry at); Epic Fight installs his spear's attack and chase.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    /** His whole gear: the Peak Guard's armour and the Crimson Dragon Spear, none of it dropped the usual way. */
    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(MurimItems.PEAK_GUARD_HELM.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(MurimItems.PEAK_GUARD_LAMELLAR.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(MurimItems.PEAK_GUARD_TASSETS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(MurimItems.PEAK_GUARD_GREAVES.get()));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.CRIMSON_DRAGON_SPEAR.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        post = blockPosition();
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
        updatePersistentAnger((ServerLevel) level(), true);
        if (getTarget() != null) maybeAlertSoldiers();
        // With a target, he is aggressive: the clients see it (his face frowns, MobFaces).
        setAggressive(getTarget() != null);
    }

    // ---- anger, as the Alliance's soldiers

    /** Every few seconds while he sees his foe, the soldiers about him who are not fighting join in. */
    private void maybeAlertSoldiers() {
        if (ticksUntilNextAlert > 0) {
            ticksUntilNextAlert--;
            return;
        }
        LivingEntity foe = getTarget();
        if (foe != null && getSensing().hasLineOfSight(foe)) {
            double range = getAttributeValue(Attributes.FOLLOW_RANGE);
            level().getEntitiesOfClass(AllianceSoldierEntity.class, AABB.unitCubeFromLowerCorner(position()).inflate(range, 10.0, range), EntitySelector.NO_SPECTATORS)
                    .stream().filter(soldier -> soldier.getTarget() == null && !soldier.isAlliedTo(foe))
                    .forEach(soldier -> soldier.setTarget(foe));
        }
        ticksUntilNextAlert = ALERT_INTERVAL.sample(getRandom());
    }

    @Override public void setTarget(@Nullable LivingEntity target) {
        if (getTarget() == null && target != null) ticksUntilNextAlert = 0;
        if (target instanceof Player player) setLastHurtByPlayer(player);
        super.setTarget(target);
    }

    @Override public int getRemainingPersistentAngerTime() { return angerTime; }
    @Override public void setRemainingPersistentAngerTime(int time) { angerTime = time; }
    @Override @Nullable public UUID getPersistentAngerTarget() { return angerTarget; }
    @Override public void setPersistentAngerTarget(@Nullable UUID target) { angerTarget = target; }
    @Override public void startPersistentAngerTimer() { setRemainingPersistentAngerTime(ANGER_TIME.sample(getRandom())); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        addPersistentAngerSaveData(tag);
        if (post != null) tag.put("Post", NbtUtils.writeBlockPos(post));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readPersistentAngerSaveData(level(), tag);
        post = NbtUtils.readBlockPos(tag, "Post").orElse(null);
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }

    /** Where he stands guard, or null (never placed). */
    @Nullable public BlockPos post() {
        return post;
    }

    /** With no one to fight, he walks back to his post and faces the way he faced there. */
    static final class ReturnToPostGoal extends Goal {
        private final PeakCaptainEntity captain;

        ReturnToPostGoal(PeakCaptainEntity captain) {
            this.captain = captain;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            return captain.post != null && captain.getTarget() == null && captain.post.distToCenterSqr(captain.position()) > 2.5 * 2.5;
        }

        @Override public boolean canContinueToUse() {
            return canUse() && !captain.getNavigation().isDone();
        }

        @Override public void start() {
            captain.getNavigation().moveTo(captain.post.getX() + 0.5, captain.post.getY(), captain.post.getZ() + 0.5, 0.8);
        }
    }

    // A commander's voice: the vindicator's, a little deeper.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VINDICATOR_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VINDICATOR_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VINDICATOR_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.85F; }
}
