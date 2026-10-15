package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
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
 * They keep the roads: they hunt bandits and every monster but the creeper, and leave players alone unless struck; a
 * soldier struck calls the others near him. They stay about where they were placed and never despawn.
 */
public class AllianceSoldierEntity extends PathfinderMob implements RangedAttackMob {
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

    private final Kind kind;

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
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.9));
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.6);
        stroll.setInterval(200);
        goalSelector.addGoal(6, stroll);
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                target -> target instanceof Enemy && !(target instanceof Creeper)));
    }

    /** With a target, he is aggressive: the clients see it (his brows knot, MobFaces; the archer aims). */
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        setAggressive(getTarget() != null);
    }

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
        if (!hasRestriction()) setHome(blockPosition());
        setPersistenceRequired();
        return result;
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) tag.put("Home", NbtUtils.writeBlockPos(getRestrictCenter()));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
    }

    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
}
