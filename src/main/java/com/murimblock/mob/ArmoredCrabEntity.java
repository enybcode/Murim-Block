package com.murimblock.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Small hostile shore crab: easy to kill, spawns day and night near water in groups of three to five.
 * Melee attacks are animated and applied by its Epic Fight patch; no vanilla melee goal here.
 */
public final class ArmoredCrabEntity extends Monster {
    /** Horizontal and vertical reach, in blocks, of the "near water" spawn rule. */
    public static final int WATER_SEARCH_RADIUS = 4;

    public ArmoredCrabEntity(EntityType<? extends ArmoredCrabEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createCrabAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // The Epic Fight patch adds the animated attack and chase goals when the crab joins the world.
    }

    /** Same experience as a chicken: 1 to 3 points. */
    @Override protected int getBaseExperienceReward() {
        return 1 + random.nextInt(3);
    }

    @Override public int getMaxSpawnClusterSize() {
        return 5;
    }

    /**
     * Monsters normally prefer darkness, which also blocks their natural spawn in daylight.
     * The crab is neutral to light so it can spawn and roam at any hour.
     */
    @Override public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    public static boolean checkCrabSpawnRules(EntityType<ArmoredCrabEntity> type, ServerLevelAccessor level,
                                              MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        return reason == MobSpawnType.SPAWNER || reason == MobSpawnType.STRUCTURE || isNearWater(level, pos);
    }

    static boolean isNearWater(LevelReader level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-WATER_SEARCH_RADIUS, -2, -WATER_SEARCH_RADIUS),
                pos.offset(WATER_SEARCH_RADIUS, 1, WATER_SEARCH_RADIUS))) {
            if (level.getFluidState(p).is(FluidTags.WATER)) return true;
        }
        return false;
    }
}
