package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.murimblock.mob.BanditChiefEntity;
import com.mojang.datafixers.util.Pair;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

import java.util.Set;

/**
 * Epic Fight side of the Bandit Chief: a humanoid on the biped skeleton holding his great axe in both hands (a
 * greatsword for Epic Fight, so he stands, walks and runs as with one). His attacks are his own (chief-axe.cjs):
 * <ul>
 *   <li>the cleave: the axe up behind the head and down to the ground ahead (within 3.4 blocks), the most frequent;</li>
 *   <li>the sweep: wound far back on the right, swept level across the front (within 3.6 blocks);</li>
 *   <li>the leaping slam: from 3 to 7 blocks away, he leaps forward and drives the axe into the ground; the landing
 *   sends a shockwave (BanditChiefEntity). Rarer, with a 9 s cooldown.</li>
 * </ul>
 * Every one is slow to come and can be parried. Standing, he plays his own idle: the axe on his shoulder, a roll of the neck.
 */
public class BanditChiefPatch extends HumanoidMobPatch<BanditChiefEntity> {
    public BanditChiefPatch(BanditChiefEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> axeAttacks() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(10.0F).cooldown(30).canBeInterrupted(true).looping(false)
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.CHIEF_CLEAVE).withinEyeHeight().withinDistance(0.0D, 3.4D)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(7.0F).cooldown(40).canBeInterrupted(true).looping(false)
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.CHIEF_SWEEP).withinEyeHeight().withinDistance(0.0D, 3.6D)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(6.0F).cooldown(180).canBeInterrupted(true).looping(false)
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.CHIEF_SLAM).withinEyeHeight().withinDistance(3.0D, 7.0D)));
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.GREATSWORD, ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, axeAttacks()));
        // His own idle, the axe on his shoulder (it comes after the greatsword's own stance, so it wins); he walks and
        // chases as the engine's two-handed holders do.
        weaponLivingMotions.put(CapabilityItem.WeaponCategories.GREATSWORD, ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, Set.of(
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.IDLE, MurimAnimations.CHIEF_IDLE),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.WALK, Animations.BIPED_WALK_TWOHAND),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.CHASE, Animations.BIPED_WALK_TWOHAND))));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, MurimAnimations.CHIEF_IDLE);
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
