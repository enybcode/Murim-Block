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
 * sword low. Struck, he answers with the Azure Cloud Sword Art's own cuts, the M1 strikes a player of the art uses:
 * <ul>
 *   <li>Drifting Cut then Returning Cut, the level cut and its backhand (within 2.5 blocks);</li>
 *   <li>Falling Rain, the cut from above, rarer (a 5 s cooldown).</li>
 * </ul>
 */
public class AzureSenseiPatch extends HumanoidMobPatch<AzureSenseiEntity> {
    public AzureSenseiPatch(AzureSenseiEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    /** His cuts with the Azure Cloud in hand (built once the animations exist). */
    private static CombatBehaviors.Builder<HumanoidMobPatch<?>> azureCuts() {
        return CombatBehaviors.<HumanoidMobPatch<?>>builder()
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(10.0F).canBeInterrupted(true).looping(false)
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.DRIFTING_CUT).withinEyeHeight().withinDistance(0.0D, 2.5D))
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.RETURNING_CUT).withinEyeHeight().withinDistance(0.0D, 2.8D)))
                .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(5.0F).cooldown(100).canBeInterrupted(true).looping(false)
                        .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                .animationBehavior(MurimAnimations.FALLING_RAIN).withinEyeHeight().withinDistance(0.0D, 2.5D)));
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
