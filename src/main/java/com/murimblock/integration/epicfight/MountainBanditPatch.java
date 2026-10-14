package com.murimblock.integration.epicfight;

import com.murimblock.mob.MountainBanditEntity;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;

/**
 * Epic Fight side of the Mountain Bandit: a humanoid on the biped skeleton, like the engine's own vindicator. The
 * engine holds the weapon at the hand and picks the attacks from what is held: with a Bandit Dao (a sword), its
 * one-handed sword behaviours (a two-hit combo and a lunging cut). Walks and runs like a person, not like a zombie.
 */
public class MountainBanditPatch extends HumanoidMobPatch<MountainBanditEntity> {
    public MountainBanditPatch(MountainBanditEntity entity) {
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
