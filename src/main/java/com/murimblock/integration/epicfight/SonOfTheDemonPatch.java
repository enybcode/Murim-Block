package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.murimblock.mob.SonOfTheDemonEntity;
import java.util.Set;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * Epic Fight side of the Son of the Demon: a humanoid on his own armature (ColossusAnimations.ARMATURE), his fists his
 * weapons (a fist for Epic Fight), his own idle and walk. His blows are the Majin Fist's (mob/majin/MajinFist.MOVES), each
 * within its range, its cooldown and its share of his life. Nothing while the transformation plays (he cannot target).
 */
public class SonOfTheDemonPatch extends HumanoidMobPatch<SonOfTheDemonEntity> {
    public SonOfTheDemonPatch(SonOfTheDemonEntity entity) {
        super(entity, Factions.NEUTRAL);
    }


    @Override
    protected void setWeaponMotions() {
        super.setWeaponMotions();
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, com.murimblock.mob.majin.MajinFist.behaviors()));
        weaponLivingMotions.put(CapabilityItem.WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, Set.of(
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.IDLE, ColossusAnimations.IDLE),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.WALK, ColossusAnimations.WALK),
                Pair.<LivingMotion, AnimationAccessor<? extends StaticAnimation>>of(LivingMotions.CHASE, ColossusAnimations.WALK))));
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE, ColossusAnimations.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, ColossusAnimations.WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, ColossusAnimations.WALK);
        animator.addLivingAnimation(LivingMotions.FALL, ColossusAnimations.IDLE);
        animator.addLivingAnimation(LivingMotions.DEATH, yesman.epicfight.gameasset.Animations.BIPED_DEATH);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        commonAggressiveMobUpdateMotion(considerInaction);
    }
}
