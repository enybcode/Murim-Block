package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.murimblock.mob.AzureSenseiEntity;
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
 * Epic Fight side of Sensei Yun: a humanoid on the biped skeleton, the Azure Cloud held at the hand as a player's sword.
 * At rest and walking he plays his own motions (scripts/animation/sensei-motions.cjs): the left hand behind his back, the
 * sword low. Struck, he fights with the Azure Cloud Sword Art itself, as a master of it would (every move can be parried):
 * <ul>
 *   <li>the M1 strikes in a row: Drifting Cut, Returning Cut, Piercing Mist (which goes through armour);</li>
 *   <li>Rising Cloud then Falling Rain, the lift and the cut that knocks down;</li>
 *   <li>Swallow Returns, his spin, and Thousand Drizzle, the flurry of thrusts;</li>
 *   <li>Wind Step and Cloud Step Slash: dashes onto someone keeping their distance (2.5 to 7.5 blocks);</li>
 *   <li>in his second phase only (below half his health): Heaven Splitting Crescent and the Storm of Needles (Thousand
 *   Drizzle evolved).</li>
 * </ul>
 * Not the Cloud Step Dive: it is Cloud Step Slash from the air, its body plunging 2.5 blocks; played from the ground it
 * sank him into it. Every move of his keeps his body above the ground (AzureSenseiGameTests).
 * Played by a mob, the techniques are their own strikes: the lift, the carry and the delayed cuts of a player's are not
 * there (MartialCombat acts for players only).
 */
public class AzureSenseiPatch extends HumanoidMobPatch<AzureSenseiEntity> {
    public AzureSenseiPatch(AzureSenseiEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    /** His moves with the Azure Cloud in hand (built once the animations exist). */
    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> azureCuts() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 0).nextBehavior(move(MurimAnimations.DRIFTING_CUT, 0.0D, 2.5D))
                        .nextBehavior(move(MurimAnimations.RETURNING_CUT, 0.0D, 2.8D))
                        .nextBehavior(move(MurimAnimations.PIERCING_MIST, 0.0D, 3.0D)))
                .newBehaviorSeries(series(6.0F, 80).nextBehavior(move(MurimAnimations.RISING_CLOUD, 0.0D, 2.5D))
                        .nextBehavior(move(MurimAnimations.FALLING_RAIN, 0.0D, 2.8D)))
                .newBehaviorSeries(series(5.0F, 120).nextBehavior(move(MurimAnimations.SWALLOW_RETURNS, 0.0D, 2.8D)))
                .newBehaviorSeries(series(4.0F, 160).nextBehavior(move(MurimAnimations.THOUSAND_DRIZZLE, 0.0D, 3.2D)))
                .newBehaviorSeries(series(6.0F, 60).nextBehavior(move(MurimAnimations.WIND_STEP, 2.5D, 5.0D)))
                .newBehaviorSeries(series(8.0F, 100).nextBehavior(move(MurimAnimations.CLOUD_STEP_SLASH, 3.5D, 7.5D)))
                .newBehaviorSeries(series(7.0F, 160).nextBehavior(move(MurimAnimations.HEAVEN_SPLITTING_CRESCENT, 0.0D, 3.5D).custom(AzureSenseiPatch::inEarnest)))
                .newBehaviorSeries(series(5.0F, 200).nextBehavior(move(MurimAnimations.THOUSAND_DRIZZLE_STORM, 0.0D, 3.2D).custom(AzureSenseiPatch::inEarnest)));
    }

    private static CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> series(float weight, int cooldown) {
        return CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(weight).cooldown(cooldown).canBeInterrupted(true).looping(false);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(AnimationAccessor<? extends StaticAnimation> animation, double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    /** Whether he fights in earnest (his second phase). */
    private static boolean inEarnest(HumanoidMobPatch<?> patch) {
        return patch.getOriginal() instanceof AzureSenseiEntity sensei && sensei.secondPhase();
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.SWORD, ImmutableMap.of(CapabilityItem.Styles.COMMON, azureCuts()));
        // His own idle and walk with the sword in hand (they come after the sword's own stance, so they win).
        weaponLivingMotions.put(CapabilityItem.WeaponCategories.SWORD, ImmutableMap.of(CapabilityItem.Styles.COMMON, Set.of(
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.IDLE, MurimAnimations.SENSEI_IDLE),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.WALK, MurimAnimations.SENSEI_WALK))));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, MurimAnimations.SENSEI_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, MurimAnimations.SENSEI_WALK);
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
