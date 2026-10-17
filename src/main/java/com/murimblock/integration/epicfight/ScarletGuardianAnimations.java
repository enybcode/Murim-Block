package com.murimblock.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;

/** Existing Sanguine motions, with contact volumes and readable timings for a four-block guardian. */
public final class ScarletGuardianAnimations {
    public static AnimationAccessor<StaticAnimation> IDLE, WALK;
    public static AnimationAccessor<AttackAnimation> STRAIGHT, CROSS;
    public static void build(AnimationBuilder b) {
        IDLE=b.nextAccessor("biped/murim/guardian_idle",a -> new StaticAnimation(.15F,true,a,Armatures.BIPED));
        WALK=b.nextAccessor("biped/murim/guardian_walk",a -> new StaticAnimation(.15F,true,a,Armatures.BIPED));
        // Root-space collision deliberately spans the player's torso as well as the guardian's fist height.
        var box=new OBBCollider(.95,1.35,1.45,0,1.5,-1.7);
        STRAIGHT=b.nextAccessor("biped/murim/guardian_straight",a -> new AttackAnimation(.12F,.09F,.14F,.23F,.65F,box,
                Armatures.BIPED.get().rootJoint,a,Armatures.BIPED).addProperty(AttackPhaseProperty.DAMAGE_MODIFIER,ValueModifier.multiplier(1F)));
        CROSS=b.nextAccessor("biped/murim/guardian_cross",a -> new AttackAnimation(.12F,.08F,.12F,.24F,.7F,box,
                Armatures.BIPED.get().rootJoint,a,Armatures.BIPED).addProperty(AttackPhaseProperty.DAMAGE_MODIFIER,ValueModifier.multiplier(1.1F)));
    }
    private ScarletGuardianAnimations() { }
}
