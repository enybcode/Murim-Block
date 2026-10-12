package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import com.murimblock.mob.CrabManEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.InTimeEvent;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.HitAnimation;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.event.types.registry.EntityPatchRegistryEvent;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.Armatures.ArmatureAccessor;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.world.damagesource.StunType;

/**
 * Epic Fight side of the crab man: armature, the seven clips exported from
 * C:\Users\enzob\crabe\human-crab-animated-v2 (V6 model, already at the 87.5 % in-game scale) and the claw collider.
 * Registered through ArmoredCrabEpicFight, which owns Murimblock's single animation builder.
 */
public final class CrabManEpicFight {
    public static final ArmatureAccessor<CrabManArmature> ARMATURE =
            ArmatureAccessor.create(Murimblock.MOD_ID, "entity/crab_man", CrabManArmature::new);

    // Half extents then centre, in blocks, in the claw joint's own axes. Measured in a GameTest probe: the joint
    // is the wrist and keeps the model axes, the claw itself reaching +X and -Y from it (about 0.65 block long
    // at the 87.5 % scale). At the jab contact the wrist is ~0.7 block in front of the crab man.
    public static final Collider CLAW = ColliderPreset.registerCollider(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "crab_man_claw"),
            new OBBCollider(0.45D, 0.45D, 0.3D, 0.3D, -0.27D, 0D));

    public static AnimationAccessor<StaticAnimation> IDLE;
    public static AnimationAccessor<MovementAnimation> WALK;
    public static AnimationAccessor<AttackAnimation> ATTACK_CLAW;
    public static AnimationAccessor<AttackAnimation> CLAW_SNAP;
    public static AnimationAccessor<ActionAnimation> WATER_BALL;
    public static AnimationAccessor<HitAnimation> HURT;
    public static AnimationAccessor<LongHitAnimation> DEATH;

    private CrabManEpicFight() { }

    static void buildAnimations(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("crab_man/idle", a -> new StaticAnimation(true, a, ARMATURE));
        WALK = builder.nextAccessor("crab_man/walk", a -> new MovementAnimation(true, a, ARMATURE));
        // start, antic, preDelay, contact, recovery, end (clip times from the animated V2 README).
        ATTACK_CLAW = builder.nextAccessor("crab_man/attack_claw", a -> designedSpeed(new AttackAnimation(0.15F, a, ARMATURE,
                new Phase(0.0F, 0.62F, 0.66F, 0.76F, 0.95F, 1.625F, ARMATURE.get().claw, CLAW))));
        // The jab: lunge step, then a hard pinch that throws the target back (long stun, high impact).
        CLAW_SNAP = builder.nextAccessor("crab_man/claw_snap", a -> designedSpeed(new AttackAnimation(0.15F, a, ARMATURE,
                new Phase(0.0F, 0.72F, 0.74F, 0.82F, 1.3F, 41F / 24F, ARMATURE.get().claw, CLAW)
                        .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                        .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(2.5F)))));
        // Water ball volley: the balls form in the claw from 0.3 s (charge until 0.78 s) and leave at 0.82 s.
        WATER_BALL = builder.nextAccessor("crab_man/water_ball", a -> new ActionAnimation(0.15F, a, ARMATURE)
                .addEvents(
                        InTimeEvent.create(0.3F, (AnimationEvent.E0) (patch, animation, params) -> {
                            if (patch.getOriginal() instanceof CrabManEntity crabMan) crabMan.chargeWaterBalls();
                        }, AnimationEvent.Side.SERVER),
                        InTimeEvent.create(0.82F, (AnimationEvent.E0) (patch, animation, params) -> {
                            if (patch.getOriginal() instanceof CrabManEntity crabMan) crabMan.releaseWaterBalls(patch.getTarget());
                        }, AnimationEvent.Side.SERVER)));
        HURT = builder.nextAccessor("crab_man/hurt", a -> new HitAnimation(0.08F, a, ARMATURE));
        DEATH = builder.nextAccessor("crab_man/death", a -> new LongHitAnimation(0.16F, a, ARMATURE));
    }

    /**
     * Without this Epic Fight scales an attack's speed by its length times the attacker's attack speed (a 1.6 s
     * clip ran 1.6x faster). Factor 0 plays the clip at the timing it was animated with.
     */
    static AttackAnimation designedSpeed(AttackAnimation animation) {
        animation.addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F);
        return animation;
    }

    static void registerPatch(EntityPatchRegistryEvent event) {
        event.registerEntityPatch(MurimEntities.CRAB_MAN.get(), CrabManPatch::new);
        Armatures.registerEntityTypeArmature(MurimEntities.CRAB_MAN.get(), ARMATURE);
    }
}
