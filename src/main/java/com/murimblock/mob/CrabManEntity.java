package com.murimblock.mob;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Infected crab man: a tougher hostile human with a crab claw and a shell (zombie health, armour).
 * Always arrives with two armored crabs. Does not burn in daylight, gets Speed I in water.
 * Spawns naturally near water like the crab, alone and about seven times rarer (weight 3 against 20, see
 * crab_man_spawns.json); structures will come later. Claw attacks and knockback come from its Epic Fight patch.
 */
public final class CrabManEntity extends Monster {
    static final int ESCORT_SIZE = 2;
    /** Close around it (a crab is 1.1 blocks wide); tried in order until two crabs fit. */
    private static final double[][] ESCORT_OFFSETS = {{1.5, 0}, {-1.5, 0}, {0, 1.5}, {0, -1.5}, {1.5, 1.5}, {-1.5, -1.5}, {1.5, -1.5}, {-1.5, 1.5}};
    /**
     * Water ball volley (12/10/2026, Enzo): one big ball and four small bubbles that follow it. Per small bubble:
     * scale, launch speed (slower than the big one, so they trail it), yaw spread in degrees, delay in ticks.
     */
    static final float[][] VOLLEY = {{0.42F, 0.76F, -4, 1}, {0.34F, 0.73F, 5, 2}, {0.38F, 0.70F, -2, 3}, {0.30F, 0.67F, 3, 4}};
    private final List<WaterBallEntity> chargingBalls = new ArrayList<>();

    public CrabManEntity(EntityType<? extends CrabManEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createCrabManAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ARMOR_TOUGHNESS, 2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 24)
                // Vanilla ignores walking speed while swimming unless this is above zero (Depth Strider I level),
                // so the Speed I it gets in water actually makes it faster there.
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.33);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // The Epic Fight patch adds the animated claw attacks and the chase goal when it joins the world.
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isInWater() && tickCount % 10 == 0) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
        }
    }

    /** Start of the water ball cast: the big ball and its small bubbles appear in the claw. */
    public void chargeWaterBalls() {
        chargingBalls.removeIf(ball -> !ball.isAlive());
        if (!chargingBalls.isEmpty()) return;
        chargingBalls.add(WaterBallEntity.charging(this, 1.0F, 0));
        for (int i = 0; i < VOLLEY.length; i++) chargingBalls.add(WaterBallEntity.charging(this, VOLLEY[i][0], i + 1));
        chargingBalls.forEach(level()::addFreshEntity);
    }

    /** The throw: the big ball leaves first, the small ones one tick apart behind it. */
    public void releaseWaterBalls(@Nullable LivingEntity target) {
        for (WaterBallEntity ball : chargingBalls) {
            if (!ball.isAlive() || ball.state() != WaterBallEntity.CHARGING) continue;
            if (ball.isBig()) ball.launch(target, WaterBallEntity.SPEED, 0, 0);
            else {
                float[] v = VOLLEY[ball.slot() - 1];
                ball.launch(target, v[1], v[2], (int) v[3]);
            }
        }
        chargingBalls.clear();
    }

    /** Neutral to light like the crabs, so it can spawn and roam at any hour. */
    @Override public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    /** One crab man per natural spawn; its escort comes from finalizeSpawn. */
    @Override public int getMaxSpawnClusterSize() {
        return 1;
    }

    /** Same rule as the armored crab: not in Peaceful, solid ground, water within reach, any hour. */
    public static boolean checkCrabManSpawnRules(EntityType<CrabManEntity> type, ServerLevelAccessor level,
                                                 MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (!checkMobSpawnRules(type, level, reason, pos, random)) return false;
        return reason == MobSpawnType.SPAWNER || reason == MobSpawnType.STRUCTURE || ArmoredCrabEntity.isNearWater(level, pos);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        // Placed by a structure, its crabs stay with it, as it does (Canvas.spawn keeps it from despawning).
        spawnEscort(level, difficulty, reason == MobSpawnType.STRUCTURE);
        return result;
    }

    /**
     * Always two armored crabs: first on free spots around it; if none is free (cramped cave, walls), on its
     * own position, where entity pushing separates them. Free means no block collision; a crab may drop a step.
     */
    public int spawnEscort(ServerLevelAccessor level, DifficultyInstance difficulty) {
        return spawnEscort(level, difficulty, false);
    }

    /** The same; {@code persistent}: the crabs never despawn (a structure's). */
    public int spawnEscort(ServerLevelAccessor level, DifficultyInstance difficulty, boolean persistent) {
        int spawned = 0;
        for (double[] offset : ESCORT_OFFSETS) {
            if (spawned == ESCORT_SIZE) break;
            if (placeCrab(level, difficulty, getX() + offset[0], getZ() + offset[1], true, persistent)) spawned++;
        }
        while (spawned < ESCORT_SIZE && placeCrab(level, difficulty, getX(), getZ(), false, persistent)) spawned++;
        return spawned;
    }

    private boolean placeCrab(ServerLevelAccessor level, DifficultyInstance difficulty, double x, double z, boolean needsRoom, boolean persistent) {
        ArmoredCrabEntity crab = MurimEntities.ARMORED_CRAB.get().create(level.getLevel());
        if (crab == null) return false;
        crab.moveTo(x, getY(), z, random.nextFloat() * 360, 0);
        if (needsRoom && !level.noBlockCollision(crab, crab.getBoundingBox())) return false;
        crab.finalizeSpawn(level, difficulty, MobSpawnType.MOB_SUMMONED, null);
        if (persistent) crab.setPersistenceRequired();
        level.addFreshEntityWithPassengers(crab);
        return true;
    }
}
