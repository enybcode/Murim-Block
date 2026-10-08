package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Summon-only integration fixture, not a finished Murim character or a farmable enemy. */
public final class TrainingOpponentEntity extends Zombie {
    public TrainingOpponentEntity(EntityType<? extends TrainingOpponentEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setCanPickUpLoot(false);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0);
    }

    public static AttributeSupplier.Builder createTrainingAttributes() {
        return EpicFightMobSupport.humanoidAttributes(Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0));
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Epic Fight installs AnimatedAttackGoal and TargetChasingGoal on join. No vanilla melee goal.
    }

    @Override protected boolean isSunSensitive() { return false; }
    @Override protected boolean convertsInWater() { return false; }
    @Override public void setBaby(boolean baby) { super.setBaby(false); }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Keep the fixture reward-free even if commands or saved NBT replace its equipment.
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType type, @Nullable SpawnGroupData data) {
        // Avoid zombie random equipment, baby/jockey selection and reinforcement bonuses in this fixture.
        return data;
    }
}
