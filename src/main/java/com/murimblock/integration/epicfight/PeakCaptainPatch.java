package com.murimblock.integration.epicfight;

import com.murimblock.mob.PeakCaptainEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

/**
 * Epic Fight side of the Captain of the Peak Guard: a humanoid on the biped skeleton, his own model
 * (client/PeakCaptainRenderer), the Crimson Dragon Spear in his hands. By his phase (PeakCaptainEntity.Phase):
 * <ul>
 *   <li>SPEAR: Epic Fight's own spear moves;</li>
 *   <li>RETREAT: no attack and no chase (his men fight);</li>
 *   <li>ART: the Celestial Peak Spear (docs/combat/styles/celestial-peak-spear.md), by range: Piercing Dragon onto a
 *   foe 4 to 8 blocks off, Plum Blossom Thrusts at the spear's reach, Sweep of a Thousand Soldiers on whoever presses
 *   him close, the two-handed spear's thrusts between. Slower to come back than the player's.</li>
 * </ul>
 * Every move can be parried.
 */
public class PeakCaptainPatch extends HumanoidMobPatch<PeakCaptainEntity> {
    public PeakCaptainPatch(PeakCaptainEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    private static CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> series(float weight, int cooldown) {
        return CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(weight).cooldown(cooldown).canBeInterrupted(true).looping(false);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(AnimationAccessor<? extends AttackAnimation> animation, double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    /** The Celestial Peak Spear in his hands (cooldowns in ticks, longer than the player's). */
    static CombatBehaviors.Builder<HumanoidMobPatch<?>> celestialPeakSpear() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 0).nextBehavior(move(Animations.BIPED_MOB_SPEAR_TWOHAND1, 0.0D, 3.0D))
                        .nextBehavior(move(Animations.BIPED_MOB_SPEAR_TWOHAND2, 0.0D, 3.0D))
                        .nextBehavior(move(Animations.BIPED_MOB_SPEAR_TWOHAND3, 0.0D, 3.2D)))
                .newBehaviorSeries(series(8.0F, 120).nextBehavior(move(MurimAnimations.CELESTIAL_PLUM_BLOSSOM, 2.0D, 4.0D)))
                .newBehaviorSeries(series(9.0F, 140).nextBehavior(move(MurimAnimations.CELESTIAL_PIERCING_DRAGON, 4.0D, 8.0D)))
                .newBehaviorSeries(series(7.0F, 160).nextBehavior(move(MurimAnimations.CELESTIAL_THOUSAND_SWEEP, 0.0D, 2.6D)));
    }

    @Override
    protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
        if (getOriginal().phase() == PeakCaptainEntity.Phase.ART
                && EpicFightCapabilities.getItemStackCapability(getOriginal().getMainHandItem()).getWeaponCategory() == CapabilityItem.WeaponCategories.SPEAR)
            return celestialPeakSpear();
        return super.getHoldingItemWeaponMotionBuilder();
    }

    /** In the retreat (also when loaded in it), he neither attacks nor chases. */
    @Override
    public void setAIAsInfantry(boolean holdingRangedWeapon) {
        super.setAIAsInfantry(holdingRangedWeapon);
        if (getOriginal().phase() == PeakCaptainEntity.Phase.RETREAT) holdBack();
    }

    /** No attack and no chase: his men fight. */
    public void holdBack() {
        getOriginal().goalSelector.removeAllGoals(goal -> goal instanceof AnimatedAttackGoal || goal instanceof TargetChasingGoal);
    }

    /** Back in the fight with the Celestial Peak Spear (the entity is already in its ART phase). */
    public void takeUpTheArt() {
        holdBack();
        setAIAsInfantry(false);
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
