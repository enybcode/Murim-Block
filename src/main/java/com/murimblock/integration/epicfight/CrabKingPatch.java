package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.murimblock.mob.CrabKingEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import java.util.Set;

/**
 * Epic Fight side of the Crab King: a humanoid on the biped skeleton, his hands empty (his pincers are his own: a fist
 * for Epic Fight), with his own idle and attacks (CrabKingAnimations):
 * <ul>
 *   <li>the sweep within 5 blocks, the most frequent;</li>
 *   <li>the snap from 2 to 5.8 blocks: he lunges;</li>
 *   <li>the slam within 4.8 blocks, rarer, with an 8 s cooldown: its landing sends a shockwave;</li>
 *   <li>the grab from 1.5 to 5 blocks, rarer still, with an 11 s cooldown: what the great pincer catches is lifted,
 *   squeezed and thrown (CrabKingEntity);</li>
 *   <li>the pincer from the deep, from 4 to 14 blocks (his reach at a distance), with a 15 s cooldown: a giant pincer out
 *   of the ground sweeps, strikes to its side and pinches toward his target (CrabPincerEntity); he holds his own
 *   plunged meanwhile.</li>
 * </ul>
 * He walks and chases with his own heavy walk.
 * The crab art's techniques will join them when the art is made (docs/mobs/crab-king.md).
 */
public class CrabKingPatch extends HumanoidMobPatch<CrabKingEntity> {
    public CrabKingPatch(CrabKingEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    private static CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> series(float weight, int cooldown) {
        return CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(weight).cooldown(cooldown).canBeInterrupted(false).looping(false);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(AnimationAccessor<? extends StaticAnimation> animation, double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> pincers() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 30).nextBehavior(move(CrabKingAnimations.SWEEP, 0.0D, 5.0D)))
                .newBehaviorSeries(series(7.0F, 50).nextBehavior(move(CrabKingAnimations.SNAP, 2.0D, 5.8D)))
                .newBehaviorSeries(series(6.0F, 160).nextBehavior(move(CrabKingAnimations.SLAM, 0.0D, 4.8D)))
                .newBehaviorSeries(series(6.0F, 220).nextBehavior(move(CrabKingAnimations.GRAB, 1.5D, 5.0D)))
                .newBehaviorSeries(series(8.0F, 300).nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                        .animationBehavior(CrabKingAnimations.BURROW).withinDistance(4.0D, 14.0D)));
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, pincers()));
        weaponLivingMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, Set.of(
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.IDLE, CrabKingAnimations.IDLE),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.WALK, CrabKingAnimations.WALK),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.CHASE, CrabKingAnimations.WALK))));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, CrabKingAnimations.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, CrabKingAnimations.WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, CrabKingAnimations.WALK);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        commonAggressiveMobUpdateMotion(considerInaction);
    }
}
