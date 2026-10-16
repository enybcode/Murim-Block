package com.murimblock.bloodsect.ninja;

import com.murimblock.item.MurimItems;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The Blood Sect's soldier, its middle rank (Enzo, 17/10/2026): a Blood Ninja a grade above the cultists, in richer cloth
 * (three outfits, scripts/animation/blood-soldier-3d.cjs) and the whole Blood Lacquer set, which it really wears (its armour
 * counts) and may drop, a piece at a time. The cultists' dao, blood step and M1, harder and with more life.
 */
public final class BloodSoldier extends BloodNinja {
    /** Blade, reaver, warden. */
    public static final int SOLDIER_OUTFITS = 3;
    /** The chance of each piece of its armour to drop when a player kills it. */
    public static final float ARMOUR_DROP_CHANCE = 0.12F;

    public BloodSoldier(EntityType<? extends BloodSoldier> type, Level level) {
        super(type, level);
        this.xpReward = 12;
        wearTheLacquer();
    }

    @Override protected int outfitCount() { return SOLDIER_OUTFITS; }
    @Override protected double baseAttack() { return 6; }

    /** The four pieces of the Blood Lacquer, each with its chance to drop. */
    public void wearTheLacquer() {
        wear(EquipmentSlot.HEAD, new ItemStack(MurimItems.BLOOD_LACQUER_MASK.get()));
        wear(EquipmentSlot.CHEST, new ItemStack(MurimItems.BLOOD_LACQUER_CUIRASS.get()));
        wear(EquipmentSlot.LEGS, new ItemStack(MurimItems.BLOOD_LACQUER_TASSETS.get()));
        wear(EquipmentSlot.FEET, new ItemStack(MurimItems.BLOOD_LACQUER_GREAVES.get()));
    }

    private void wear(EquipmentSlot slot, ItemStack stack) {
        setItemSlot(slot, stack);
        setDropChance(slot, ARMOUR_DROP_CHANCE);
    }

    /** A new soldier draws one of its rank's outfits; the cultists' light armours are not for it. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setLook(this.random.nextInt(SOLDIER_OUTFITS), 0);
        return out;
    }

    /** No random armour or weapons from Minecraft's own spawning: it has its own. */
    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40)
            .add(Attributes.MOVEMENT_SPEED, .30).add(Attributes.ATTACK_DAMAGE, 6)
            .add(Attributes.FOLLOW_RANGE, 24).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }
}
