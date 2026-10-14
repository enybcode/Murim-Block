package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.murimblock.mob.MountainBanditEntity;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/**
 * Epic Fight side of the Mountain Bandit: a humanoid on the biped skeleton, like the engine's own vindicator. The
 * engine holds the dao at the hand as it holds a player's sword. Its cuts are Murim's own sword cuts, drawn so the edge
 * leads (the M1 strikes validated for the Azure Cloud Sword Art), not the engine's generic mob swings:
 * <ul>
 *   <li>the double cut: a level cut from right to left, then the backhand from left to right (within 2.5 blocks);</li>
 *   <li>the falling cut: raised over the head and brought straight down, rarer (a 6 s cooldown).</li>
 * </ul>
 */
public class MountainBanditPatch extends HumanoidMobPatch<MountainBanditEntity> {
    public MountainBanditPatch(MountainBanditEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    /** The bandit's cuts with a sword in hand (built once the animations exist). */
    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> swordCuts() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(10.0F).canBeInterrupted(true).looping(false)
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.DRIFTING_CUT).withinEyeHeight().withinDistance(0.0D, 2.5D))
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.RETURNING_CUT).withinEyeHeight().withinDistance(0.0D, 2.8D)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(4.0F).cooldown(120).canBeInterrupted(true).looping(false)
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.FALLING_RAIN).withinEyeHeight().withinDistance(0.0D, 2.5D)));
    }

    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.SWORD, ImmutableMap.of(CapabilityItem.Styles.COMMON, swordCuts()));
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
