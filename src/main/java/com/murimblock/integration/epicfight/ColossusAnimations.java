package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures.ArmatureAccessor;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Son of the Demon, the Bandit Warlord's second phase (docs/mobs/bandit-warlord.md): his own armature (the biped's
 * joints at his proportions, scripts/animation/colossus-epicfight.cjs) and his moves, written on it
 * (animmodels/animations/colossus): his idle and heavy walk, his awakening (the roar, arms flung wide), the gorilla's chest
 * beat, a roar; and a stand-in blow, a heavy straight right, until the Majin Fist's (given by Enzo one by one).
 */
public final class ColossusAnimations {
    /** His armature: a humanoid's, so Epic Fight drives him as any, its joints where his body bends. */
    public static final ArmatureAccessor<HumanoidArmature> ARMATURE =
            ArmatureAccessor.create(Murimblock.MOD_ID, "entity/son_of_the_demon", HumanoidArmature::new);

    public static AnimationAccessor<StaticAnimation> IDLE, WALK;
    public static AnimationAccessor<ActionAnimation> AWAKEN, CHEST_BEAT, ROAR;
    /** The film's end: he grabs the viewer, leaps, flies (a loop), throws him, lands (WarlordTransformation). */
    public static AnimationAccessor<ActionAnimation> SMASH, GRAB, LEAP, THROW, LAND;
    public static AnimationAccessor<StaticAnimation> FLY, DIVE;
    public static AnimationAccessor<AttackAnimation> PUNCH;
    /** The chest beat's eight blows (seconds into it). */
    public static final float[] BEATS = {0.75F, 1.0F, 1.25F, 1.5F, 1.75F, 2.0F, 2.25F, 2.5F};

    /** His fist, drawn twice a man's size: far ahead of him. */
    private static final OBBCollider PUNCH_BOX = new OBBCollider(1.6D, 1.6D, 2.6D, 0.0D, 3.2D, -3.6D);

    private ColossusAnimations() { }

    public static void build(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("colossus/idle", a -> new StaticAnimation(0.2F, true, a, ARMATURE));
        WALK = builder.nextAccessor("colossus/walk", a -> new StaticAnimation(0.2F, true, a, ARMATURE));
        AWAKEN = builder.nextAccessor("colossus/awaken", a -> new ActionAnimation(0.05F, a, ARMATURE));
        CHEST_BEAT = builder.nextAccessor("colossus/chest_beat", a -> new ActionAnimation(0.1F, a, ARMATURE));
        ROAR = builder.nextAccessor("colossus/roar", a -> new ActionAnimation(0.15F, a, ARMATURE));
        SMASH = builder.nextAccessor("colossus/smash", a -> new ActionAnimation(0.08F, a, ARMATURE));
        GRAB = builder.nextAccessor("colossus/grab", a -> new ActionAnimation(0.1F, a, ARMATURE));
        LEAP = builder.nextAccessor("colossus/leap", a -> new ActionAnimation(0.1F, a, ARMATURE));
        FLY = builder.nextAccessor("colossus/fly", a -> new StaticAnimation(0.15F, true, a, ARMATURE));
        DIVE = builder.nextAccessor("colossus/dive", a -> new StaticAnimation(0.1F, true, a, ARMATURE));
        THROW = builder.nextAccessor("colossus/throw", a -> new ActionAnimation(0.1F, a, ARMATURE));
        LAND = builder.nextAccessor("colossus/land", a -> new ActionAnimation(0.02F, a, ARMATURE));
        PUNCH = builder.nextAccessor("colossus/punch", a -> {
            AttackAnimation punch = new AttackAnimation(0.15F, 0.35F, 0.45F, 0.62F, 1.0F, PUNCH_BOX, ARMATURE.get().rootJoint, a, ARMATURE);
            punch.addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.2F));
            punch.addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG);
            punch.addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT_HARD.get());
            punch.addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get());
            punch.addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F);
            return punch;
        });
    }
}
