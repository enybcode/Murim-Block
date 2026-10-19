package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.murimblock.mob.RavenMasterEntity;
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
 * Epic Fight side of the Raven Master: a humanoid on the biped skeleton, the iron fan held at the hand as a player's.
 * At rest and walking he plays his own motions (scripts/animation/raven-master-motions.cjs): the fan open before his
 * chest, the left hand behind his back, the head cocking as a bird's. He fights with the fan's own strikes (every one
 * can be parried):
 * <ul>
 *   <li>the four M1 in a row: the flick, the backhand, the circle over the head, the lunge (SectWeaponAnimations);</li>
 *   <li>the flick then the lunge, a short series;</li>
 *   <li>the running lunge onto someone keeping their distance (3 to 6 blocks).</li>
 * </ul>
 * His shadow doors and his raven swarm are his own (RavenMasterEntity), started between these.
 */
public class RavenMasterPatch extends HumanoidMobPatch<RavenMasterEntity> {
    public RavenMasterPatch(RavenMasterEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    /** His moves with the fan in hand (built once the animations exist). */
    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> fanStrikes() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(series(10.0F, 0).nextBehavior(move(SectWeaponAnimations.FAN_AUTO1, 0.0D, 2.4D))
                        .nextBehavior(move(SectWeaponAnimations.FAN_AUTO2, 0.0D, 2.6D))
                        .nextBehavior(move(SectWeaponAnimations.FAN_AUTO3, 0.0D, 2.8D))
                        .nextBehavior(move(SectWeaponAnimations.FAN_AUTO4, 0.0D, 3.0D)))
                .newBehaviorSeries(series(6.0F, 60).nextBehavior(move(SectWeaponAnimations.FAN_AUTO1, 0.0D, 2.4D))
                        .nextBehavior(move(SectWeaponAnimations.FAN_AUTO4, 0.0D, 3.0D)))
                .newBehaviorSeries(series(7.0F, 80).nextBehavior(move(SectWeaponAnimations.FAN_DASH, 3.0D, 6.0D)));
    }

    private static CombatBehaviors.BehaviorSeries.Builder<HumanoidMobPatch<?>> series(float weight, int cooldown) {
        return CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(weight).cooldown(cooldown).canBeInterrupted(true).looping(false);
    }

    private static CombatBehaviors.Behavior.Builder<HumanoidMobPatch<?>> move(AnimationAccessor<? extends StaticAnimation> animation, double min, double max) {
        return CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(animation).withinEyeHeight().withinDistance(min, max);
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        var strikes = fanStrikes();
        weaponAttackMotions.put(MurimWeaponCategories.IRON_FAN, ImmutableMap.of(CapabilityItem.Styles.COMMON, strikes, CapabilityItem.Styles.ONE_HAND, strikes));
        // His own idle and walk with the fan in hand (they come after the fan's own stance, so they win).
        Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> motions = Set.of(
                Pair.of(LivingMotions.IDLE, MurimAnimations.RAVEN_MASTER_IDLE),
                Pair.of(LivingMotions.WALK, MurimAnimations.RAVEN_MASTER_WALK));
        weaponLivingMotions.put(MurimWeaponCategories.IRON_FAN, ImmutableMap.of(CapabilityItem.Styles.COMMON, motions, CapabilityItem.Styles.ONE_HAND, motions));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, MurimAnimations.RAVEN_MASTER_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, MurimAnimations.RAVEN_MASTER_WALK);
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
