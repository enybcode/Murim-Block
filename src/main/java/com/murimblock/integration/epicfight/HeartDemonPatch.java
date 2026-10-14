package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.murimblock.cultivation.inner.HeartDemonEntity;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/**
 * Epic Fight side of the Heart Demon: a humanoid on the biped skeleton that fights with its owner's moves. With a
 * sword (the Azure Cloud Sword's demon), the art's cuts and Cloud Step Slash from a distance; bare-handed (the Long
 * Fist's, or a demon without art), Epic Fight's punches, Rapid Fists close and Tiger Step Punch from a distance.
 */
public class HeartDemonPatch extends HumanoidMobPatch<HeartDemonEntity> {
    public HeartDemonPatch(HeartDemonEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> animation,
                                                                             double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> swordMoves() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(10.0F).canBeInterrupted(true).looping(false)
                        .nextBehavior(move(MurimAnimations.DRIFTING_CUT, 0, 2.5))
                        .nextBehavior(move(MurimAnimations.RETURNING_CUT, 0, 2.8))
                        .nextBehavior(move(MurimAnimations.PIERCING_MIST, 0, 3.2)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(5.0F).cooldown(100).canBeInterrupted(true).looping(false)
                        .nextBehavior(move(MurimAnimations.FALLING_RAIN, 0, 2.5)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(6.0F).cooldown(160).canBeInterrupted(false).looping(false)
                        .nextBehavior(move(MurimAnimations.CLOUD_STEP_SLASH, 3.0, 6.0)));
    }

    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> fistMoves() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(10.0F).canBeInterrupted(true).looping(false)
                        .nextBehavior(move(Animations.FIST_AUTO1, 0, 1.8))
                        .nextBehavior(move(Animations.FIST_AUTO2, 0, 1.8))
                        .nextBehavior(move(Animations.FIST_AUTO3, 0, 2.0)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(5.0F).cooldown(200).canBeInterrupted(false).looping(false)
                        .nextBehavior(move(MurimAnimations.RAPID_FISTS, 0, 2.0)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(6.0F).cooldown(140).canBeInterrupted(false).looping(false)
                        .nextBehavior(move(MurimAnimations.TIGER_STEP_PUNCH, 2.6, 4.5)));
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        var sword = swordMoves();
        var fists = fistMoves();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.SWORD, ImmutableMap.of(CapabilityItem.Styles.COMMON, sword, CapabilityItem.Styles.ONE_HAND, sword));
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, fists, CapabilityItem.Styles.ONE_HAND, fists));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        commonAggressiveMobUpdateMotion(considerInaction);
    }
}
