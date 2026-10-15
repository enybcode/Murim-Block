package com.murimblock.integration.epicfight;

import com.murimblock.mob.TigerEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
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
 * Epic Fight side of the tiger: up close it swipes and bites (now and then a double swipe, rarely the slow maul); from
 * four to seven blocks, with its prey in sight, it pounces.
 */
public final class TigerPatch extends MobPatch<TigerEntity> {
    /** Pounce range, in blocks, and how often at most (ticks). */
    static final double POUNCE_MIN = 3.5D, POUNCE_MAX = 7.0D;
    static final int POUNCE_COOLDOWN = 160;

    public TigerPatch(TigerEntity original) {
        super(original, Factions.NEUTRAL);
    }

    private static BehaviorSeries.Builder<TigerPatch> series(float weight, int cooldown) {
        return BehaviorSeries.<TigerPatch>builder().weight(weight).canBeInterrupted(true).looping(false).cooldown(cooldown);
    }

    private static Behavior.Builder<TigerPatch> attack(AnimationAccessor<AttackAnimation> animation, double min, double max) {
        return Behavior.<TigerPatch>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    /** Built lazily: the animation accessors only exist after Epic Fight's registry event. */
    private static final class Behaviors {
        static final CombatBehaviors.Builder<TigerPatch> TIGER = CombatBehaviors.<TigerPatch>builder()
                .newBehaviorSeries(series(40.0F, 15).nextBehavior(attack(TigerEpicFight.SWIPE, 0.0D, 2.8D)))
                .newBehaviorSeries(series(30.0F, 20).nextBehavior(attack(TigerEpicFight.BITE, 0.0D, 2.6D)))
                .newBehaviorSeries(series(15.0F, 60).nextBehavior(attack(TigerEpicFight.DOUBLE_SWIPE, 0.0D, 2.8D)))
                .newBehaviorSeries(series(8.0F, 140).nextBehavior(attack(TigerEpicFight.MAUL, 0.0D, 3.0D)))
                .newBehaviorSeries(series(30.0F, POUNCE_COOLDOWN).nextBehavior(Behavior.<TigerPatch>builder()
                        .animationBehavior(TigerEpicFight.POUNCE_ATTACK).withinDistance(POUNCE_MIN, POUNCE_MAX)
                        .custom(patch -> patch.getTarget() != null && patch.getOriginal().hasLineOfSight(patch.getTarget()))));
    }

    @Override protected void initAI() {
        super.initAI();
        original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, Behaviors.TIGER.build(this)));
        original.goalSelector.addGoal(1, new TargetChasingGoal(this, original, 1.25D, false));
    }

    @Override public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, TigerEpicFight.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, TigerEpicFight.WALK);
        animator.addLivingAnimation(LivingMotions.DEATH, TigerEpicFight.DEATH);
    }

    @Override public void updateMotion(boolean considerInaction) {
        super.commonMobUpdateMotion(considerInaction);
    }

    @Override public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        return switch (stunType) {
            case SHORT, LONG, HOLD, KNOCKDOWN, NEUTRALIZE -> TigerEpicFight.HURT;
            default -> null;
        };
    }
}
