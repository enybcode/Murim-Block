package com.murimblock.mob;

import com.murimblock.world.map.MurimMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;

/**
 * The tiger (docs/mobs/tiger.md): a wolf's statistics doubled (16 health, 8 attack, the wolf's speed, a 32-block
 * pursuit), aggressive, living only in the centre of the Murim map, by day and by night. It fights with five animated
 * attacks from its Epic Fight patch (TigerPatch): the swipe, the bite, the double swipe, the pounce and the maul.
 * It leaves its pelt and its claws, one time in five each (loot_table/entities/tiger.json).
 */
public final class TigerEntity extends Monster {
    public static final double MAX_HEALTH = 16, ATTACK_DAMAGE = 8, SPEED = 0.3, FOLLOW_RANGE = 32;
    /** How hard the pounce throws it forward and up (blocks per tick). */
    static final double LEAP_FORWARD = 1.05, LEAP_UP = 0.42;

    public TigerEntity(EntityType<? extends TigerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createTigerAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // The Epic Fight patch adds the animated attacks and the chase when the tiger joins the world.
    }

    /** The pounce's spring (its animation, at the end of the crouch): thrown at its target, or straight ahead. */
    public void leapAt(LivingEntity target) {
        Vec3 ahead = target != null ? target.position().subtract(position()) : Vec3.directionFromRotation(0, getYRot());
        Vec3 flat = new Vec3(ahead.x, 0, ahead.z);
        if (flat.lengthSqr() < 1.0E-4) return;
        flat = flat.normalize();
        setDeltaMovement(flat.x * LEAP_FORWARD, LEAP_UP, flat.z * LEAP_FORWARD);
        hasImpulse = true;
        hurtMarked = true;
    }

    @Override protected int getBaseExperienceReward() {
        return 5;
    }

    @Override public int getMaxSpawnClusterSize() {
        return 2;
    }

    /** It hunts by day as well as by night: no preference for darkness. */
    @Override public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    /**
     * Only in the centre of the Murim map, on open ground, at any hour, never on Peaceful; and rare (Enzo, 19/10/2026:
     * "il y en a beaucoup trop, ça doit être rare"): a light weight among the monsters, alone, and one try in
     * {@link #RARITY} only.
     */
    public static boolean checkTigerSpawnRules(EntityType<TigerEntity> type, ServerLevelAccessor level,
                                               MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        if (reason == MobSpawnType.SPAWNER || reason == MobSpawnType.STRUCTURE) return true;
        return inItsLands(pos.getX(), pos.getZ()) && level.canSeeSky(pos) && random.nextInt(RARITY) == 0;
    }

    /** One natural try in this many becomes a tiger. */
    public static final int RARITY = 3;

    /** The centre of the Murim map. */
    public static boolean inItsLands(int x, int z) {
        return MurimMap.inBubble(x, z) && MurimMap.region(x, z) == MurimMap.Region.CENTER;
    }
}
