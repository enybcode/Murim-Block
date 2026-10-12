package com.murimblock.integration.epicfight;

import com.murimblock.mob.CrabManEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors.Behavior;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors.BehaviorSeries;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

/**
 * Epic Fight side of the crab man. Same faction as its armored crabs (NEUTRAL), so its claw does not
 * hurt the escort. Up close the overhead claw swing and the lunging jab (knockback); at 5 to 16 blocks the water ball volley.
 */
public final class CrabManPatch extends MobPatch<CrabManEntity> {
    public static final double WATER_BALL_MIN_RANGE = 5.0D, WATER_BALL_MAX_RANGE = 16.0D;
    public static final int WATER_BALL_COOLDOWN = 140;

    public CrabManPatch(CrabManEntity original) {
        super(original, Factions.NEUTRAL);
    }

    /** Built lazily: the animation accessors only exist after Epic Fight's registry event. */
    private static final class Behaviors {
        static final CombatBehaviors.Builder<CrabManPatch> CRAB_MAN = CombatBehaviors.<CrabManPatch>builder()
                .newBehaviorSeries(BehaviorSeries.<CrabManPatch>builder().weight(50.0F).canBeInterrupted(false).looping(false).cooldown(30)
                        .nextBehavior(Behavior.<CrabManPatch>builder().animationBehavior(CrabManEpicFight.ATTACK_CLAW)
                                .withinEyeHeight().withinDistance(0.0D, 1.8D)))
                .newBehaviorSeries(BehaviorSeries.<CrabManPatch>builder().weight(50.0F).canBeInterrupted(false).looping(false).cooldown(40)
                        .nextBehavior(Behavior.<CrabManPatch>builder().animationBehavior(CrabManEpicFight.CLAW_SNAP)
                                .withinEyeHeight().withinDistance(0.8D, 2.2D)))
                // At range: the water ball volley, every 7 s at most, only with a clear line of sight.
                .newBehaviorSeries(BehaviorSeries.<CrabManPatch>builder().weight(50.0F).canBeInterrupted(false).looping(false).cooldown(WATER_BALL_COOLDOWN)
                        .nextBehavior(Behavior.<CrabManPatch>builder().animationBehavior(CrabManEpicFight.WATER_BALL)
                                .withinDistance(WATER_BALL_MIN_RANGE, WATER_BALL_MAX_RANGE)
                                .custom(patch -> patch.getTarget() != null && patch.getOriginal().hasLineOfSight(patch.getTarget()))));
    }

    @Override protected void initAI() {
        super.initAI();
        original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, Behaviors.CRAB_MAN.build(this)));
        original.goalSelector.addGoal(1, new TargetChasingGoal(this, original, 1.0D, false));
    }

    @Override public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, CrabManEpicFight.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, CrabManEpicFight.WALK);
        animator.addLivingAnimation(LivingMotions.DEATH, CrabManEpicFight.DEATH);
    }

    @Override public void updateMotion(boolean considerInaction) {
        super.commonMobUpdateMotion(considerInaction);
    }

    @Override public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        return switch (stunType) {
            case SHORT, LONG, HOLD, KNOCKDOWN, NEUTRALIZE -> CrabManEpicFight.HURT;
            default -> null;
        };
    }
}
