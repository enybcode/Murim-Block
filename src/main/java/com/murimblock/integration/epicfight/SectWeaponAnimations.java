package com.murimblock.integration.epicfight;

import com.murimblock.mob.FlyingNeedleEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationEvent.InTimeEvent;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.AirSlashAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.ComboAttackAnimation;
import yesman.epicfight.api.animation.types.DashAttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The moves of the three sect weapons Epic Fight has no type for (MurimWeaponCategories), from
 * scripts/animation/sect-weapons.cjs (art/blockbench/sect_weapons.bbmodel): for each a stance, four M1 strikes, a running
 * attack and an air attack, put together into movesets by MurimWeapons. The strikes follow the weapon's speed, as Epic
 * Fight's own (authored for 1.6 attacks a second).
 * <ul>
 *   <li>iron fan: quick flicks of the open fan, a circle over the head, a lunge; a short reach;</li>
 *   <li>chain whip: lashes reaching about five blocks (the chain's length), a sweep at the legs that hits all round;</li>
 *   <li>flying needles: no blow of the hand; each throw sends needles ({@link FlyingNeedleEntity}) at the time the hand
 *   lets go.</li>
 * </ul>
 */
public final class SectWeaponAnimations {
    public static AnimationAccessor<StaticAnimation> FAN_IDLE, WHIP_IDLE, NEEDLES_IDLE;
    public static AnimationAccessor<ComboAttackAnimation> FAN_AUTO1, FAN_AUTO2, FAN_AUTO3, FAN_AUTO4;
    public static AnimationAccessor<ComboAttackAnimation> WHIP_AUTO1, WHIP_AUTO2, WHIP_AUTO3, WHIP_AUTO4;
    public static AnimationAccessor<ComboAttackAnimation> NEEDLES_AUTO1, NEEDLES_AUTO2, NEEDLES_AUTO3, NEEDLES_AUTO4;
    public static AnimationAccessor<DashAttackAnimation> FAN_DASH, WHIP_DASH, NEEDLES_DASH;
    public static AnimationAccessor<AirSlashAnimation> FAN_AIR, WHIP_AIR, NEEDLES_AIR;

    // The hit boxes, on the root joint (Epic Fight's joint space: -Z is forward).
    /** The open fan's flick: a short arc ahead. */
    private static final OBBCollider FAN_ARC = new OBBCollider(1.1D, 0.8D, 1.0D, 0.0D, 1.0D, -1.1D);
    /** The circle over the head, coming down across: all round, a little ahead. */
    private static final OBBCollider FAN_CIRCLE = new OBBCollider(1.4D, 1.0D, 1.4D, 0.0D, 1.0D, -0.6D);
    /** The lunge with the fan's edge. */
    private static final OBBCollider FAN_THRUST = new OBBCollider(0.5D, 0.6D, 1.5D, 0.0D, 1.1D, -1.7D);
    /** A lash: a narrow strip as long as the chain, about five blocks ahead. */
    private static final OBBCollider WHIP_LASH = new OBBCollider(0.6D, 0.8D, 2.4D, 0.0D, 1.0D, -2.6D);
    /** The sidearm lash: a wide arc ahead, as far as the chain reaches. */
    private static final OBBCollider WHIP_ARC = new OBBCollider(2.6D, 0.9D, 1.8D, 0.0D, 1.0D, -1.8D);
    /** The low sweep at the legs. */
    private static final OBBCollider WHIP_LOW = new OBBCollider(2.6D, 0.5D, 1.8D, 0.0D, 0.4D, -1.8D);
    /** The needles' throws strike nothing by hand: a box out of reach under the feet; the needles do the hurting. */
    private static final OBBCollider NO_BLOW = new OBBCollider(0.01D, 0.01D, 0.01D, 0.0D, -64.0D, 0.0D);

    private SectWeaponAnimations() { }

    private static Joint root() {
        return Armatures.BIPED.get().rootJoint;
    }

    /** An M1 strike: its damage, at the weapon's speed. */
    private static <A extends AttackAnimation> A strike(A animation, float damage) {
        animation.addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage));
        animation.addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F);
        return animation;
    }

    /** A throw: no blow of the hand; `count` needles leave the hand at `release` s, spread over `spread` degrees. */
    private static <A extends AttackAnimation> A needleThrow(A animation, float release, int count, float spread, float pitch) {
        animation.addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F);
        animation.addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SMALL.get());
        animation.addEvents(InTimeEvent.create(release, (AnimationEvent.E0) (patch, self, params) ->
                FlyingNeedleEntity.throwFan(patch.getOriginal(), count, spread, pitch), AnimationEvent.Side.SERVER));
        return animation;
    }

    public static void build(AnimationBuilder builder) {
        // transition, antic, preDelay, contact, recovery: the hit lands between preDelay and contact.
        FAN_IDLE = builder.nextAccessor("biped/murim/fan_idle", a -> new StaticAnimation(0.15F, true, a, Armatures.BIPED));
        FAN_AUTO1 = builder.nextAccessor("biped/murim/fan_auto1", a -> strike(new ComboAttackAnimation(0.08F, 0.06F, 0.12F, 0.26F, 0.42F, FAN_ARC, root(), a, Armatures.BIPED), 0.9F)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SMALL.get()));
        FAN_AUTO2 = builder.nextAccessor("biped/murim/fan_auto2", a -> strike(new ComboAttackAnimation(0.08F, 0.07F, 0.13F, 0.27F, 0.44F, FAN_ARC, root(), a, Armatures.BIPED), 0.9F)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SMALL.get()));
        FAN_AUTO3 = builder.nextAccessor("biped/murim/fan_auto3", a -> strike(new ComboAttackAnimation(0.08F, 0.28F, 0.34F, 0.52F, 0.6F, FAN_CIRCLE, root(), a, Armatures.BIPED), 1.1F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2)));
        FAN_AUTO4 = builder.nextAccessor("biped/murim/fan_auto4", a -> strike(new ComboAttackAnimation(0.08F, 0.14F, 0.18F, 0.36F, 0.54F, FAN_THRUST, root(), a, Armatures.BIPED), 1.3F)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG));
        FAN_DASH = builder.nextAccessor("biped/murim/fan_dash", a -> strike(new DashAttackAnimation(0.06F, 0.06F, 0.1F, 0.3F, 0.45F, FAN_THRUST, root(), a, Armatures.BIPED), 1.2F));
        FAN_AIR = builder.nextAccessor("biped/murim/fan_air", a -> strike(new AirSlashAnimation(0.08F, 0.1F, 0.26F, 0.4F, FAN_ARC, root(), a, Armatures.BIPED), 1.1F));

        WHIP_IDLE = builder.nextAccessor("biped/murim/whip_idle", a -> new StaticAnimation(0.15F, true, a, Armatures.BIPED));
        WHIP_AUTO1 = builder.nextAccessor("biped/murim/whip_auto1", a -> strike(new ComboAttackAnimation(0.08F, 0.18F, 0.22F, 0.34F, 0.58F, WHIP_LASH, root(), a, Armatures.BIPED), 1.0F)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get()));
        WHIP_AUTO2 = builder.nextAccessor("biped/murim/whip_auto2", a -> strike(new ComboAttackAnimation(0.08F, 0.14F, 0.18F, 0.34F, 0.56F, WHIP_ARC, root(), a, Armatures.BIPED), 0.85F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2))
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get()));
        WHIP_AUTO3 = builder.nextAccessor("biped/murim/whip_auto3", a -> strike(new ComboAttackAnimation(0.08F, 0.44F, 0.48F, 0.6F, 0.68F, WHIP_LASH, root(), a, Armatures.BIPED), 1.2F)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get()));
        WHIP_AUTO4 = builder.nextAccessor("biped/murim/whip_auto4", a -> strike(new ComboAttackAnimation(0.08F, 0.18F, 0.22F, 0.42F, 0.7F, WHIP_LOW, root(), a, Armatures.BIPED), 1.1F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN));
        WHIP_DASH = builder.nextAccessor("biped/murim/whip_dash", a -> strike(new DashAttackAnimation(0.06F, 0.12F, 0.16F, 0.3F, 0.4F, WHIP_LASH, root(), a, Armatures.BIPED), 1.1F));
        WHIP_AIR = builder.nextAccessor("biped/murim/whip_air", a -> strike(new AirSlashAnimation(0.08F, 0.12F, 0.26F, 0.4F, WHIP_LASH, root(), a, Armatures.BIPED), 1.1F));

        NEEDLES_IDLE = builder.nextAccessor("biped/murim/needles_idle", a -> new StaticAnimation(0.15F, true, a, Armatures.BIPED));
        NEEDLES_AUTO1 = builder.nextAccessor("biped/murim/needles_auto1", a -> needleThrow(new ComboAttackAnimation(0.06F, 0.1F, 0.12F, 0.16F, 0.4F, NO_BLOW, root(), a, Armatures.BIPED), 0.13F, 3, 12F, 0F));
        NEEDLES_AUTO2 = builder.nextAccessor("biped/murim/needles_auto2", a -> needleThrow(new ComboAttackAnimation(0.06F, 0.12F, 0.14F, 0.18F, 0.42F, NO_BLOW, root(), a, Armatures.BIPED), 0.15F, 3, 14F, 0F));
        NEEDLES_AUTO3 = builder.nextAccessor("biped/murim/needles_auto3", a -> needleThrow(new ComboAttackAnimation(0.06F, 0.14F, 0.16F, 0.2F, 0.46F, NO_BLOW, root(), a, Armatures.BIPED), 0.17F, 3, 8F, 0F));
        NEEDLES_AUTO4 = builder.nextAccessor("biped/murim/needles_auto4", a -> needleThrow(new ComboAttackAnimation(0.06F, 0.16F, 0.18F, 0.22F, 0.54F, NO_BLOW, root(), a, Armatures.BIPED), 0.19F, 7, 60F, 0F));
        NEEDLES_DASH = builder.nextAccessor("biped/murim/needles_dash", a -> needleThrow(new DashAttackAnimation(0.06F, 0.18F, 0.2F, 0.24F, 0.46F, NO_BLOW, root(), a, Armatures.BIPED), 0.21F, 3, 10F, 0F));
        NEEDLES_AIR = builder.nextAccessor("biped/murim/needles_air", a -> needleThrow(new AirSlashAnimation(0.06F, 0.14F, 0.2F, 0.36F, NO_BLOW, root(), a, Armatures.BIPED), 0.17F, 5, 30F, 35F));
    }
}
