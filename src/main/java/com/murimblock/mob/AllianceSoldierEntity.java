package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A soldier of the Orthodox Alliance (docs/mobs/alliance-soldiers.md), one of five kinds, each its own entity type: all
 * in cloth (scripts/animation/alliance-soldier-3d.cjs), each with the Alliance's weapon of his kind in hand:
 * <ul>
 *   <li>Spear Guard: the Azure Peak Spear (Epic Fight's spear moves);</li>
 *   <li>Sword Disciple: the Snow Plum Jian (the Azure Cloud's basic cuts, AllianceSoldierPatch);</li>
 *   <li>Archer: a bow, shooting from up to 15 blocks;</li>
 *   <li>Warrior Monk: his fists (the Long Fist's punches and kicks);</li>
 *   <li>Sergeant: the Snow Plum Jian and the Rattan Shield.</li>
 * </ul>
 * Neutral toward peaceful people and ordinary animals; bandits and tigers are threats fought on sight.
 * Struck, a soldier turns on whoever struck him and calls
 * every soldier near him (and again every few seconds while he sees his foe); they stay angry for 20 to 39 seconds
 * after the last blow, then forget. A monster that strikes one of them is fought the same way. They stay about where
 * they were placed and never despawn.
 */
public class AllianceSoldierEntity extends PathfinderMob implements RangedAttackMob, NeutralMob {
    /** The five kinds: the path of their entity type, their weapons, health, armour and own strength. */
    public enum Kind {
        SPEAR_GUARD("alliance_spear_guard", "spear", () -> new ItemStack(MurimItems.ALLIANCE_SPEAR.get()), () -> ItemStack.EMPTY, 30, 4, 2),
        SWORD_DISCIPLE("alliance_sword_disciple", "sword", () -> new ItemStack(MurimItems.SNOW_PLUM_JIAN.get()), () -> ItemStack.EMPTY, 30, 4, 2),
        ARCHER("alliance_archer", "archer", () -> new ItemStack(Items.BOW), () -> ItemStack.EMPTY, 24, 2, 2),
        WARRIOR_MONK("alliance_warrior_monk", "monk", () -> ItemStack.EMPTY, () -> ItemStack.EMPTY, 36, 6, 5),
        SERGEANT("alliance_sergeant", "sergeant", () -> new ItemStack(MurimItems.SNOW_PLUM_JIAN.get()),
                () -> new ItemStack(MurimItems.RATTAN_SHIELD.get()), 40, 6, 3);

        public final String id, model;
        final Supplier<ItemStack> mainHand, offHand;
        final double health, armour, attack;

        Kind(String id, String model, Supplier<ItemStack> mainHand, Supplier<ItemStack> offHand, double health, double armour, double attack) {
            this.id = id;
            this.model = model;
            this.mainHand = mainHand;
            this.offHand = offHand;
            this.health = health;
            this.armour = armour;
            this.attack = attack;
        }

        public AttributeSupplier.Builder attributes() {
            return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                    .add(Attributes.MAX_HEALTH, health).add(Attributes.ARMOR, armour).add(Attributes.ATTACK_DAMAGE, attack)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.STEP_HEIGHT, 1.0));
        }
    }

    /** How far from their post they keep (blocks). */
    public static final int HOME_RADIUS = 10;
    /** The archer's reach (blocks), and how long he draws (ticks) and waits between two arrows. */
    public static final int BOW_RANGE = 15, BOW_DRAW = 20, BOW_PAUSE = 20;
    /** The chance he leaves his weapon (or shield), like the bandit's dao. */
    static final float WEAPON_DROP = 0.01F;

    /** How long they stay angry after the last blow, and how often an angry soldier calls the others (as the piglins). */
    private static final UniformInt ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39), ALERT_INTERVAL = TimeUtil.rangeOfSeconds(4, 6);

    private final Kind kind;
    private int angerTime, ticksUntilNextAlert;
    /** On patrol (AllianceWanderers): the leader picks far points to walk to, the others follow him. */
    private boolean patrolLeader;
    @Nullable private UUID patrolLeaderId;
    @Nullable private BlockPos patrolTarget;
    @Nullable private UUID angerTarget;

    public AllianceSoldierEntity(EntityType<? extends AllianceSoldierEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = 5;
        setCanPickUpLoot(false);
    }

    public Kind kind() {
        return kind;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // The archer shoots (Epic Fight leaves a bow to the mob's own goals); the others fight with Epic Fight's moves.
        goalSelector.addGoal(2, new ShootGoal(this));
        goalSelector.addGoal(4, new PatrolGoal(this));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9));
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.6);
        stroll.setInterval(200);
        goalSelector.addGoal(6, stroll);
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Retaliation takes priority; tigers are threats even before they strike a camp's defenders.
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, TigerEntity.class, 5, true, false, null));
        targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    /** With a target, he is aggressive: the clients see it (his brows knot, MobFaces; the archer aims). */
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        updatePersistentAnger((ServerLevel) level(), true);
        if (getTarget() != null) maybeAlertOthers();
        setAggressive(getTarget() != null);
    }

    // ---- anger, as the piglins

    /** Every few seconds while he sees his foe, the soldiers about him who are not fighting join in. */
    private void maybeAlertOthers() {
        if (ticksUntilNextAlert > 0) {
            ticksUntilNextAlert--;
            return;
        }
        LivingEntity foe = getTarget();
        if (foe != null && getSensing().hasLineOfSight(foe)) {
            double range = getAttributeValue(Attributes.FOLLOW_RANGE);
            level().getEntitiesOfClass(AllianceSoldierEntity.class, AABB.unitCubeFromLowerCorner(position()).inflate(range, 10.0, range), EntitySelector.NO_SPECTATORS)
                    .stream().filter(other -> other != this && other.getTarget() == null && !other.isAlliedTo(foe))
                    .forEach(other -> other.setTarget(foe));
        }
        ticksUntilNextAlert = ALERT_INTERVAL.sample(getRandom());
    }

    @Override public void setTarget(@Nullable LivingEntity target) {
        if (getTarget() == null && target != null) ticksUntilNextAlert = ALERT_INTERVAL.sample(getRandom());
        if (target instanceof Player player) setLastHurtByPlayer(player);
        super.setTarget(target);
    }

    @Override public int getRemainingPersistentAngerTime() { return angerTime; }
    @Override public void setRemainingPersistentAngerTime(int time) { angerTime = time; }
    @Override @Nullable public UUID getPersistentAngerTarget() { return angerTarget; }
    @Override public void setPersistentAngerTarget(@Nullable UUID target) { angerTarget = target; }
    @Override public void startPersistentAngerTimer() { setRemainingPersistentAngerTime(ANGER_TIME.sample(getRandom())); }

    // ---- the archer's bow

    @Override public void performRangedAttack(LivingEntity target, float power) {
        ItemStack bow = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), power, bow);
        double dx = target.getX() - getX(), dy = target.getY(1.0 / 3.0) - arrow.getY(), dz = target.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.2, dz, 1.6F, 14 - level().getDifficulty().getId() * 4);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    /** The archer: closes in until he sees his target within reach, draws for a second, looses, waits a second. */
    static final class ShootGoal extends Goal {
        private final AllianceSoldierEntity archer;
        private int pause;

        ShootGoal(AllianceSoldierEntity archer) {
            this.archer = archer;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity target = archer.getTarget();
            return target != null && target.isAlive() && archer.getMainHandItem().getItem() instanceof BowItem;
        }

        @Override public void stop() {
            archer.stopUsingItem();
        }

        @Override public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override public void tick() {
            LivingEntity target = archer.getTarget();
            if (target == null) return;
            boolean sees = archer.getSensing().hasLineOfSight(target);
            boolean inReach = archer.distanceToSqr(target) <= BOW_RANGE * BOW_RANGE;
            if (sees && inReach) archer.getNavigation().stop();
            else archer.getNavigation().moveTo(target, 1.0);
            archer.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (pause > 0) pause--;
            if (!sees || !inReach) {
                if (archer.isUsingItem()) archer.stopUsingItem();
                return;
            }
            if (!archer.isUsingItem()) {
                if (pause == 0) archer.startUsingItem(InteractionHand.MAIN_HAND);
            } else if (archer.getTicksUsingItem() >= BOW_DRAW) {
                archer.stopUsingItem();
                archer.performRangedAttack(target, BowItem.getPowerForTime(BOW_DRAW));
                pause = BOW_PAUSE;
            }
        }
    }

    // ---- on patrol

    /** How far a patrol's leader walks before choosing again (blocks), and how close the others keep to him. */
    public static final int PATROL_STEP_MIN = 48, PATROL_STEP_MAX = 96, PATROL_KEEP = 5;

    /** Makes him the leader of a patrol. */
    public void leadPatrol() {
        patrolLeader = true;
        patrolLeaderId = null;
    }

    /** Puts him in the patrol of this leader. */
    public void followPatrol(AllianceSoldierEntity leader) {
        patrolLeader = false;
        patrolLeaderId = leader.getUUID();
    }

    public boolean isPatrolLeader() { return patrolLeader; }
    @Nullable public UUID patrolLeaderId() { return patrolLeaderId; }
    @Nullable public BlockPos patrolTarget() { return patrolTarget; }
    public boolean onPatrol() { return patrolLeader || patrolLeaderId != null; }

    /**
     * A patrol on the move, when nobody is fought: the leader walks at a steady pace to a point 48 to 96 blocks off, then
     * chooses another (and another if the way is blocked); the others keep within 5 blocks of him. A patrol whose leader
     * is gone takes the first of them for its new leader.
     */
    static final class PatrolGoal extends Goal {
        private final AllianceSoldierEntity soldier;
        private int stuck;

        PatrolGoal(AllianceSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            return soldier.onPatrol() && soldier.getTarget() == null && !soldier.isVehicle();
        }

        @Override public void tick() {
            if (!(soldier.level() instanceof ServerLevel level)) return;
            if (soldier.patrolLeader) lead(level);
            else follow(level);
        }

        private void lead(ServerLevel level) {
            BlockPos target = soldier.patrolTarget;
            boolean arrived = target != null && soldier.blockPosition().closerThan(target, 4);
            if (target == null || arrived || ++stuck > 400) {
                stuck = 0;
                double angle = soldier.getRandom().nextDouble() * Math.PI * 2;
                int step = PATROL_STEP_MIN + soldier.getRandom().nextInt(PATROL_STEP_MAX - PATROL_STEP_MIN + 1);
                int x = soldier.getBlockX() + (int) Math.round(Math.cos(angle) * step), z = soldier.getBlockZ() + (int) Math.round(Math.sin(angle) * step);
                soldier.patrolTarget = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                target = soldier.patrolTarget;
            }
            if (soldier.getNavigation().isDone()) soldier.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.6);
        }

        private void follow(ServerLevel level) {
            if (!(level.getEntity(soldier.patrolLeaderId) instanceof AllianceSoldierEntity leader) || !leader.isAlive()) {
                // The leader is gone: the patrol goes on under the first who notices.
                UUID lost = soldier.patrolLeaderId;
                soldier.leadPatrol();
                for (AllianceSoldierEntity other : level.getEntitiesOfClass(AllianceSoldierEntity.class, soldier.getBoundingBox().inflate(16),
                        o -> o != soldier && lost.equals(o.patrolLeaderId)))
                    other.followPatrol(soldier);
                return;
            }
            if (soldier.distanceToSqr(leader) > PATROL_KEEP * PATROL_KEEP) soldier.getNavigation().moveTo(leader, 0.75);
            else soldier.getNavigation().stop();
        }
    }

    // ---- dropped from the sky (the Peak Captain's reinforcements)

    /** Summoned by the captain: he leaves neither loot nor experience (no farming the captain's waves). */
    private boolean summoned;
    /** Falling from the sky: no fall damage, the fall's pose; he lands in a crouch (sky-drop.cjs). Seen by the clients. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> SKY_FALLING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(AllianceSoldierEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    /** Ticks left of the landing, during which no fall can hurt him either. */
    private int landing;

    /**
     * Sends him down from the sky, summoned: he falls in the fall's pose, takes no fall damage, lands in a crouch with a
     * thud and a ring of dust, then fights whoever he was set on. He keeps no post (he goes wherever his foe is).
     */
    public void dropFromSky() {
        summoned = true;
        entityData.set(SKY_FALLING, true);
        clearRestriction();
        setPersistenceRequired();
    }

    public boolean summoned() { return summoned; }
    public boolean skyFalling() { return entityData.get(SKY_FALLING); }

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKY_FALLING, false);
    }

    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        if (skyFalling() || landing > 0) return false;
        return super.causeFallDamage(distance, multiplier, source);
    }

    @Override public void aiStep() {
        super.aiStep();
        if (landing > 0) landing--;
        if (skyFalling() && onGround() && level() instanceof ServerLevel server) land(server);
    }

    private void land(ServerLevel server) {
        entityData.set(SKY_FALLING, false);
        landing = 30;
        fallDistance = 0;
        var patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(this, yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(com.murimblock.integration.epicfight.MurimAnimations.ALLIANCE_SKY_LANDING, 0);
        var ground = server.getBlockState(blockPosition().below());
        if (!ground.isAir()) server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, ground),
                getX(), getY() + 0.1, getZ(), 40, 0.9, 0.05, 0.9, 0.15);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 16, 0.8, 0.05, 0.8, 0.03);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_BIG_FALL, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.7F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    @Override protected boolean shouldDropLoot() {
        return !summoned && super.shouldDropLoot();
    }

    @Override protected int getBaseExperienceReward() {
        return summoned ? 0 : super.getBaseExperienceReward();
    }

    // ---- his post

    /** Keeps him about his post, around this block. */
    public void setHome(BlockPos home) {
        restrictTo(home, HOME_RADIUS);
    }

    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, kind.mainHand.get());
        setItemSlot(EquipmentSlot.OFFHAND, kind.offHand.get());
        setDropChance(EquipmentSlot.MAINHAND, WEAPON_DROP);
        setDropChance(EquipmentSlot.OFFHAND, WEAPON_DROP);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        // Placed (an egg, a command, a structure), he keeps his post for good; out walking (a patrol, a monk on the
        // road, AllianceWanderers), he goes where he likes and is gone once no one is near.
        boolean wandering = type == MobSpawnType.NATURAL || type == MobSpawnType.PATROL;
        if (!wandering) {
            if (!hasRestriction()) setHome(blockPosition());
            setPersistenceRequired();
        }
        return result;
    }

    /** Only those out walking can go (Mob.checkDespawn leaves alone those whose persistence is required). */
    @Override public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) tag.put("Home", NbtUtils.writeBlockPos(getRestrictCenter()));
        addPersistentAngerSaveData(tag);
        if (patrolLeader) tag.putBoolean("PatrolLeader", true);
        if (patrolLeaderId != null) tag.putUUID("PatrolLeaderId", patrolLeaderId);
        if (patrolTarget != null) tag.put("PatrolTarget", NbtUtils.writeBlockPos(patrolTarget));
        if (summoned) tag.putBoolean("Summoned", true);
        if (skyFalling()) tag.putBoolean("SkyFalling", true);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
        readPersistentAngerSaveData(level(), tag);
        patrolLeader = tag.getBoolean("PatrolLeader");
        patrolLeaderId = tag.hasUUID("PatrolLeaderId") ? tag.getUUID("PatrolLeaderId") : null;
        patrolTarget = NbtUtils.readBlockPos(tag, "PatrolTarget").orElse(null);
        summoned = tag.getBoolean("Summoned");
        entityData.set(SKY_FALLING, tag.getBoolean("SkyFalling"));
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
}
