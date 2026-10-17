package com.murimblock.integration.epicfight;

import com.google.common.collect.ImmutableMap;
import com.murimblock.mob.ScarletGuardianEntity;
import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

public final class ScarletGuardianPatch extends HumanoidMobPatch<ScarletGuardianEntity> {
    public ScarletGuardianPatch(ScarletGuardianEntity entity) { super(entity,Factions.NEUTRAL); }
    @Override public boolean isTargetInvulnerable(Entity target) {
        // Cultists have no Epic Fight faction patch or scoreboard team; filter every collider contact.
        return original.isAlliedTo(target)||super.isTargetInvulnerable(target);
    }
    @Override protected void setWeaponMotions() {
        super.setWeaponMotions();
        var moves=CombatBehaviors.<HumanoidMobPatch<?>>builder()
            .newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(10).cooldown(26).canBeInterrupted(true).looping(false)
                .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(ScarletGuardianAnimations.STRAIGHT)
                    .withinDistance(0,3.3).custom(p -> ((ScarletGuardianEntity)p.getOriginal()).canMelee()))
                .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(ScarletGuardianAnimations.CROSS)
                    .withinDistance(0,3.3).custom(p -> ((ScarletGuardianEntity)p.getOriginal()).canMelee())));
        weaponAttackMotions.put(CapabilityItem.WeaponCategories.FIST,ImmutableMap.of(CapabilityItem.Styles.COMMON,moves,CapabilityItem.Styles.ONE_HAND,moves));
    }
    @Override public void initAnimator(Animator a) {
        super.initAnimator(a);
        a.addLivingAnimation(LivingMotions.IDLE,ScarletGuardianAnimations.IDLE);
        a.addLivingAnimation(LivingMotions.WALK,ScarletGuardianAnimations.WALK);
        a.addLivingAnimation(LivingMotions.CHASE,ScarletGuardianAnimations.WALK);
        a.addLivingAnimation(LivingMotions.DEATH,Animations.BIPED_DEATH);
        a.addLivingAnimation(LivingMotions.FALL,Animations.BIPED_FALL);
    }
    @Override public void updateMotion(boolean considerInaction) { commonAggressiveMobUpdateMotion(considerInaction); }
}
