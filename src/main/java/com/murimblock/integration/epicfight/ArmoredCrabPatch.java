package com.murimblock.integration.epicfight;

import com.murimblock.mob.ArmoredCrabEntity;
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

/** Epic Fight side of the crab, modelled on the engine's SpiderPatch: animated claws, no vanilla melee. */
public final class ArmoredCrabPatch extends MobPatch<ArmoredCrabEntity> {
    public ArmoredCrabPatch(ArmoredCrabEntity original) {
        super(original, Factions.NEUTRAL);
    }

    /** Built lazily: the animation accessors only exist after Epic Fight's registry event. */
    private static final class Behaviors {
        static final CombatBehaviors.Builder<ArmoredCrabPatch> CRAB = CombatBehaviors.<ArmoredCrabPatch>builder()
                .newBehaviorSeries(BehaviorSeries.<ArmoredCrabPatch>builder().weight(60.0F).canBeInterrupted(true).looping(false).cooldown(20)
                        .nextBehavior(Behavior.<ArmoredCrabPatch>builder().animationBehavior(ArmoredCrabEpicFight.ATTACK_RIGHT)
                                .withinEyeHeight().withinDistance(0.0D, 1.8D)))
                .newBehaviorSeries(BehaviorSeries.<ArmoredCrabPatch>builder().weight(40.0F).canBeInterrupted(true).looping(false).cooldown(15)
                        .nextBehavior(Behavior.<ArmoredCrabPatch>builder().animationBehavior(ArmoredCrabEpicFight.ATTACK_LEFT)
                                .withinEyeHeight().withinDistance(0.0D, 1.8D)));
    }

    @Override protected void initAI() {
        super.initAI();
        original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, Behaviors.CRAB.build(this)));
        original.goalSelector.addGoal(1, new TargetChasingGoal(this, original, 1.0D, false));
    }

    @Override public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, ArmoredCrabEpicFight.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, ArmoredCrabEpicFight.WALK);
        animator.addLivingAnimation(LivingMotions.DEATH, ArmoredCrabEpicFight.DEATH);
    }

    @Override public void updateMotion(boolean considerInaction) {
        super.commonMobUpdateMotion(considerInaction);
    }

    @Override public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        return switch (stunType) {
            case SHORT, LONG, HOLD, KNOCKDOWN, NEUTRALIZE -> ArmoredCrabEpicFight.HURT;
            default -> null;
        };
    }
}
