package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.murimblock.mob.AllianceSoldierEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

/**
 * Epic Fight side of the Alliance's soldiers: humanoids on the biped skeleton, their own model
 * (client/AllianceSoldierRenderer). By the weapon in hand:
 * <ul>
 *   <li>the jian (Sword Disciple, Sergeant, with or without the shield): the Azure Cloud's basic cuts, Drifting Cut,
 *   Returning Cut, Piercing Mist, and now and then Rising Cloud then Falling Rain;</li>
 *   <li>the spear (Spear Guard): Epic Fight's own spear moves;</li>
 *   <li>the fists (Warrior Monk): the Long Fist's Rapid Fists, Whirlwind Kick and Tiger Step Punch onto someone a few
 *   blocks off;</li>
 *   <li>the bow (Archer): no melee moves; he shoots with his own goal (AllianceSoldierEntity.ShootGoal), aiming as Epic
 *   Fight's archers do.</li>
 * </ul>
 * Every move can be parried.
 */
public class AllianceSoldierPatch extends HumanoidMobPatch<AllianceSoldierEntity> {
    public AllianceSoldierPatch(AllianceSoldierEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    private static CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> series(float weight, int cooldown) {
        return CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(weight).cooldown(cooldown).canBeInterrupted(true).looping(false);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(AnimationAccessor<? extends StaticAnimation> animation, double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    /** The jian: the Azure Cloud Sword Art's basic cuts (built once the animations exist). */
    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> jianCuts() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 0).nextBehavior(move(MurimAnimations.DRIFTING_CUT, 0.0D, 2.5D))
                        .nextBehavior(move(MurimAnimations.RETURNING_CUT, 0.0D, 2.8D))
                        .nextBehavior(move(MurimAnimations.PIERCING_MIST, 0.0D, 3.0D)))
                .newBehaviorSeries(series(4.0F, 120).nextBehavior(move(MurimAnimations.RISING_CLOUD, 0.0D, 2.5D))
                        .nextBehavior(move(MurimAnimations.FALLING_RAIN, 0.0D, 2.8D)));
    }

    /** The fists: the Long Fist's punches and kicks. */
    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> longFist() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 40).nextBehavior(move(MurimAnimations.RAPID_FISTS, 0.0D, 2.2D)))
                .newBehaviorSeries(series(6.0F, 80).nextBehavior(move(MurimAnimations.WHIRLWIND_KICK, 0.0D, 2.5D)))
                .newBehaviorSeries(series(5.0F, 120).nextBehavior(move(MurimAnimations.TIGER_STEP_PUNCH, 2.5D, 5.0D)));
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        // With the shield in the other hand the jian is held one-handed: the same cuts.
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.SWORD,
                ImmutableMap.of(CapabilityItem.Styles.COMMON, jianCuts(), CapabilityItem.Styles.ONE_HAND, jianCuts()));
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, longFist()));
    }

    /** The archer keeps to his bow: Epic Fight's melee and chase goals (it gives a bow the fists' moves) are taken away. */
    @Override
    public void setAIAsInfantry(boolean holdingRangedWeapon) {
        super.setAIAsInfantry(holdingRangedWeapon);
        if (getOriginal().kind() == AllianceSoldierEntity.Kind.ARCHER)
            getOriginal().goalSelector.removeAllGoals(goal -> goal instanceof AnimatedAttackGoal || goal instanceof TargetChasingGoal);
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

    /** Whether his fall is now the sky drop's (its own pose) rather than Epic Fight's. */
    private boolean skyPose;

    /**
     * Dropped from the sky by the captain, he falls in his own pose (sky-drop.cjs) from the first tick, on every side
     * (the flag is synced); any other fall is Epic Fight's.
     */
    @Override
    public void updateMotion(boolean considerInaction) {
        boolean sky = getOriginal().skyFalling();
        if (sky != skyPose) {
            skyPose = sky;
            getAnimator().addLivingAnimation(LivingMotions.FALL, sky ? MurimAnimations.ALLIANCE_SKY_FALL : Animations.BIPED_FALL);
        }
        if (sky) {
            currentLivingMotion = LivingMotions.FALL;
            return;
        }
        if (getOriginal().kind() == AllianceSoldierEntity.Kind.ARCHER) commonAggressiveRangedMobUpdateMotion(considerInaction);
        else commonAggressiveMobUpdateMotion(considerInaction);
    }
}
