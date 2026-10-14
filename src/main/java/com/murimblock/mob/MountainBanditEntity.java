package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import org.jetbrains.annotations.Nullable;

/**
 * The Mountain Bandit (山賊), the first common human enemy: a brigand of the mountain passes with a Bandit Dao, in bands of
 * two to four, as frequent as the zombie (docs/mobs/mountain-bandit). One mob in five outfits, like the axolotl's
 * colours: the outfit ({@link #getVariant()}) only changes the skin. It fights with its dao through Epic Fight
 * (MountainBanditPatch: Murim's own sword cuts, edge first), calls the bandits near it when it is hit, and
 * despawns far from players like any monster.
 */
public class MountainBanditEntity extends Monster {
    /** The outfits, in the order of their saved id: Ash Wanderer, Crimson Scarf, Wolf Pelt, Dune Raider, Gilded Mask. */
    public static final String[] OUTFITS = {"ash", "crimson", "wolf", "dune", "gilded"};
    /** How often each outfit spawns, in percent (the gilded mask is the rare one). */
    private static final int[] OUTFIT_WEIGHTS = {30, 25, 20, 24, 1};
    /** The dao drops one time in a hundred, worn (Looting raises it like any held equipment). */
    private static final float DAO_DROP_CHANCE = 0.01F;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MountainBanditEntity.class, EntityDataSerializers.INT);

    public MountainBanditEntity(EntityType<? extends MountainBanditEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createBanditAttributes() {
        // No attack of its own: the dao gives its 4 damage, so the bandit hits as hard as its blade.
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20).add(Attributes.ARMOR, 2).add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 24));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    /** The outfit, 0 to 4 (see {@link #OUTFITS}). */
    public int getVariant() {
        return Math.floorMod(entityData.get(VARIANT), OUTFITS.length);
    }

    public void setVariant(int variant) {
        entityData.set(VARIANT, Math.floorMod(variant, OUTFITS.length));
    }

    static int randomOutfit(RandomSource random) {
        int roll = random.nextInt(100);
        for (int i = 0; i < OUTFIT_WEIGHTS.length; i++) {
            if (roll < OUTFIT_WEIGHTS[i]) return i;
            roll -= OUTFIT_WEIGHTS[i];
        }
        return 0;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) setVariant(tag.getInt("Variant"));
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Hit, it calls the bandits around it onto whoever hit it.
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(MountainBanditEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Epic Fight installs its attack and chase goals for the dao in hand (MountainBanditPatch). No vanilla melee goal.
    }

    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.BANDIT_DAO.get()));
        setDropChance(EquipmentSlot.MAINHAND, DAO_DROP_CHANCE);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        setVariant(randomOutfit(level.getRandom()));
        return result;
    }

    // Human voices: the illagers' (bandits are men of the passes, not the undead).
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VINDICATOR_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VINDICATOR_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VINDICATOR_DEATH; }
}
