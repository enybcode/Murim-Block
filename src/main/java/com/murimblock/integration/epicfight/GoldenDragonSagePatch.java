package com.murimblock.integration.epicfight;

import com.murimblock.mob.GoldenDragonSageEntity;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

/**
 * Epic Fight side of the Golden Dragon Sage: a humanoid on the biped skeleton, his own model (client/GoldenDragonSageRenderer),
 * seated in the lotus at rest (the players' meditation, MurimAnimations.MEDITATE). He never fights for now.
 */
public class GoldenDragonSagePatch extends HumanoidMobPatch<GoldenDragonSageEntity> {
    public GoldenDragonSagePatch(GoldenDragonSageEntity entity) {
        super(entity, Factions.NEUTRAL);
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, MurimAnimations.MEDITATE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        commonMobUpdateMotion(considerInaction);
    }
}
