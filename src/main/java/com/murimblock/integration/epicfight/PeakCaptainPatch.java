package com.murimblock.integration.epicfight;

import com.murimblock.mob.PeakCaptainEntity;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

/**
 * Epic Fight side of the Captain of the Peak Guard: a humanoid on the biped skeleton, his own model
 * (client/PeakCaptainRenderer), the Crimson Dragon Spear in his hands: for now Epic Fight's own spear moves, every one
 * parryable. The Celestial Peak Spear's techniques (docs/combat/styles/celestial-peak-spear.md) take their place once
 * that art is built.
 */
public class PeakCaptainPatch extends HumanoidMobPatch<PeakCaptainEntity> {
    public PeakCaptainPatch(PeakCaptainEntity entity) {
        super(entity, Factions.NEUTRAL);
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
