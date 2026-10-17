package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.murimblock.mob.GoldenBellMonkEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/**
 * Epic Fight side of the Golden Bell monk: a humanoid on the biped skeleton, his own model
 * (client/GoldenBellMonkRenderer). Neutral; struck, he fights bare-handed with Epic Fight's own fists: the three punches
 * in a row close in, the dashing punch onto someone a few blocks off. Every blow can be parried.
 */
public class GoldenBellMonkPatch extends HumanoidMobPatch<GoldenBellMonkEntity> {
    public GoldenBellMonkPatch(GoldenBellMonkEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    private static CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> series(float weight, int cooldown) {
        return CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(weight).cooldown(cooldown).canBeInterrupted(true).looping(false);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(AnimationAccessor<? extends StaticAnimation> animation, double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    /** His fists (built once the animations exist). */
    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> fists() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 0).nextBehavior(move(Animations.FIST_AUTO1, 0.0D, 1.8D))
                        .nextBehavior(move(Animations.FIST_AUTO2, 0.0D, 1.8D))
                        .nextBehavior(move(Animations.FIST_AUTO3, 0.0D, 2.0D)))
                .newBehaviorSeries(series(5.0F, 100).nextBehavior(move(Animations.FIST_DASH, 2.5D, 5.0D)));
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        var fists = fists();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, fists, CapabilityItem.Styles.ONE_HAND, fists));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        commonAggressiveMobUpdateMotion(considerInaction);
    }
}
