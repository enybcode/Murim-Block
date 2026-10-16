package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.murimblock.mob.BanditWarlordEntity;
import java.util.Set;
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

/**
 * Epic Fight side of the Bandit Warlord: a humanoid on the biped skeleton, his fists his weapons (a fist for Epic Fight),
 * with his own idle, walk and blows (BanditWarlordAnimations):
 * <ul>
 *   <li>the Mountain-Crushing Punch within 4 blocks, the most frequent;</li>
 *   <li>the Earth Breaker within 5 blocks, with an 8 s cooldown: its shockwave reaches all round him;</li>
 *   <li>the Bull Charge from 4 to 10 blocks, with a 10 s cooldown: to catch whoever backs away;</li>
 *   <li>the Sky Breaker within 3.5 blocks, with a 12 s cooldown: whoever it strikes is thrown up;</li>
 *   <li>the Mountain Collapse from 5 to 14 blocks, with an 11 s cooldown: a fissure that breaks the ground toward his foe;</li>
 *   <li>the Thousand-Jin Drop from 6 to 18 blocks, with a 15 s cooldown: a leap onto his foe, a crater where he lands.</li>
 * </ul>
 * Nothing while he falls or lands (BanditWarlordEntity holds his AI then).
 */
public class BanditWarlordPatch extends HumanoidMobPatch<BanditWarlordEntity> {
    public BanditWarlordPatch(BanditWarlordEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    private static CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> series(float weight, int cooldown) {
        return CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(weight).cooldown(cooldown).canBeInterrupted(false).looping(false);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(AnimationAccessor<? extends StaticAnimation> animation, double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> fists() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 30).nextBehavior(move(BanditWarlordAnimations.CRUSHER, 0.0D, 4.0D)))
                .newBehaviorSeries(series(7.0F, 160).nextBehavior(move(BanditWarlordAnimations.EARTH_BREAKER, 0.0D, 5.0D)))
                .newBehaviorSeries(series(8.0F, 200).nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                        .animationBehavior(BanditWarlordAnimations.BULL_CHARGE).withinDistance(4.0D, 10.0D)))
                .newBehaviorSeries(series(6.0F, 240).nextBehavior(move(BanditWarlordAnimations.SKY_BREAKER, 0.0D, 3.5D)))
                .newBehaviorSeries(series(7.0F, 220).nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                        .animationBehavior(BanditWarlordAnimations.COLLAPSE).withinDistance(5.0D, 14.0D)))
                .newBehaviorSeries(series(6.0F, 300).nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                        .animationBehavior(BanditWarlordAnimations.LEAP).withinDistance(6.0D, 18.0D)));
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, fists()));
        weaponLivingMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, Set.of(
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.IDLE, BanditWarlordAnimations.IDLE),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.WALK, BanditWarlordAnimations.WALK),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.CHASE, BanditWarlordAnimations.WALK))));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, BanditWarlordAnimations.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, BanditWarlordAnimations.WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, BanditWarlordAnimations.WALK);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    /** Whether his fall is now the meteor's (the dive) rather than Epic Fight's. */
    private boolean meteorPose;

    /** Falling from the sky, he dives from the first tick, on every side (the flag is synced); any other fall is Epic Fight's. */
    @Override
    public void updateMotion(boolean considerInaction) {
        boolean meteor = getOriginal().isMeteor();
        if (meteor != meteorPose) {
            meteorPose = meteor;
            getAnimator().addLivingAnimation(LivingMotions.FALL, meteor ? BanditWarlordAnimations.DIVE : Animations.BIPED_FALL);
        }
        if (meteor) {
            currentLivingMotion = LivingMotions.FALL;
            return;
        }
        commonAggressiveMobUpdateMotion(considerInaction);
    }
}
