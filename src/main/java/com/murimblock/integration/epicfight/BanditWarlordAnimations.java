package com.murimblock.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Bandit Warlord's moves (scripts/animation/bandit-warlord-3d.cjs, docs/mobs/bandit-warlord.md), the Mountain-Shattering
 * Fist's: his idle and heavy walk, his roar, his four blows with the fists, and his fall from the sky. He is drawn twice a
 * man's size, so his hit boxes reach as far (before his root, forward along -z). Every blow is slow to come and can be
 * parried.
 * <ul>
 *   <li>Mountain-Crushing Punch: a straight right, a step and the fist driven through; it throws back;</li>
 *   <li>Earth Breaker: both fists brought down on the ground ahead; the impact sends a shockwave all round him that
 *   throws everyone up (BanditWarlordEntity), {@link #EARTH_IMPACT};</li>
 *   <li>Bull Charge: the shoulder lowered, a rush of some four blocks through everything before him; it knocks down;</li>
 *   <li>Sky Breaker: crouched low, an uppercut; whoever it strikes is thrown high in the air (BanditWarlordEntity),
 *   {@link #SKY_IMPACT}.</li>
 *   <li>Mountain Collapse: the right fist raised high and driven into the ground ahead from one knee: a fissure runs out
 *   from it, breaking the ground and throwing up whoever stands on it (BanditWarlordEntity), {@link #COLLAPSE_IMPACT};</li>
 *   <li>Thousand-Jin Drop: a crouch and a leap high onto his foe ({@link #LEAP}, the entity itself leaps at
 *   {@link #LEAP_JUMP}); where he lands, fist first ({@link #LEAP_LAND}), a crater and a shockwave.</li>
 * </ul>
 * His fall: {@link #DIVE} held while he falls, then {@link #LAND} from the impact (the landing, up, the roar).
 */
public final class BanditWarlordAnimations {
    public static AnimationAccessor<StaticAnimation> IDLE, WALK, DIVE;
    public static AnimationAccessor<AttackAnimation> CRUSHER, EARTH_BREAKER, BULL_CHARGE, SKY_BREAKER;
    public static AnimationAccessor<ActionAnimation> ROAR, LAND, COLLAPSE, LEAP_LAND;
    /** In the air: nothing else may start, but he is not held still (he flies). */
    public static AnimationAccessor<StaticAnimation> LEAP;
    /** When the Collapse's fist meets the ground, and when the Drop leaves it (seconds into them). */
    public static final float COLLAPSE_IMPACT = 0.7F, LEAP_JUMP = 0.3F;

    /** The leap: nothing else may start meanwhile, he cannot turn; he moves (the entity flies), unlike an action. */
    private static final class Airborne extends StaticAnimation {
        Airborne(AnimationAccessor<? extends StaticAnimation> accessor) {
            super(0.1F, false, accessor, Armatures.BIPED);
            this.stateSpectrumBlueprint.clear().newTimePair(0.0F, Float.MAX_VALUE)
                    .addState(yesman.epicfight.api.animation.types.EntityState.TURNING_LOCKED, true)
                    .addState(yesman.epicfight.api.animation.types.EntityState.INACTION, true)
                    .addState(yesman.epicfight.api.animation.types.EntityState.UPDATE_LIVING_MOTION, false)
                    .addState(yesman.epicfight.api.animation.types.EntityState.COMBO_ATTACKS_DOABLE, false);
        }
    }

    /** When the Earth Breaker's fists meet the ground, and the Sky Breaker's fist comes up through (seconds into them). */
    public static final float EARTH_IMPACT = 0.78F, SKY_IMPACT = 0.5F;
    /** How long the landing lasts, up to the end of his roar (seconds): he cannot be hurt meanwhile. */
    public static final float LAND_LENGTH = 2.78F;

    private static final OBBCollider CRUSHER_BOX = new OBBCollider(1.4D, 1.3D, 2.2D, 0.0D, 2.1D, -3.0D);
    private static final OBBCollider EARTH_BOX = new OBBCollider(2.2D, 1.4D, 2.2D, 0.0D, 1.0D, -2.6D);
    private static final OBBCollider CHARGE_BOX = new OBBCollider(1.6D, 1.8D, 1.4D, 0.0D, 1.8D, -1.6D);
    private static final OBBCollider SKY_BOX = new OBBCollider(1.4D, 2.0D, 1.5D, 0.0D, 2.2D, -2.0D);

    private BanditWarlordAnimations() { }

    private static Joint root() {
        return Armatures.BIPED.get().rootJoint;
    }

    /** A blow of his: as hard as {@code damage} times his attack, a heavy hit's sounds, played at its animated speed. */
    private static <A extends AttackAnimation> A blow(A animation, float damage, StunType stun) {
        animation.addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage));
        animation.addProperty(AttackPhaseProperty.STUN_TYPE, stun);
        animation.addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT_HARD.get());
        animation.addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get());
        animation.addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F);
        return animation;
    }

    public static void build(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("biped/murim/warlord_idle", a -> new StaticAnimation(0.2F, true, a, Armatures.BIPED));
        WALK = builder.nextAccessor("biped/murim/warlord_walk", a -> new StaticAnimation(0.2F, true, a, Armatures.BIPED));
        DIVE = builder.nextAccessor("biped/murim/warlord_dive", a -> new StaticAnimation(0.1F, true, a, Armatures.BIPED));
        ROAR = builder.nextAccessor("biped/murim/warlord_roar", a -> new ActionAnimation(0.15F, a, Armatures.BIPED));
        LAND = builder.nextAccessor("biped/murim/warlord_land", a -> new ActionAnimation(0.02F, a, Armatures.BIPED));
        COLLAPSE = builder.nextAccessor("biped/murim/warlord_collapse", a -> new ActionAnimation(0.15F, a, Armatures.BIPED));
        LEAP = builder.nextAccessor("biped/murim/warlord_leap", Airborne::new);
        LEAP_LAND = builder.nextAccessor("biped/murim/warlord_leap_land", a -> new ActionAnimation(0.02F, a, Armatures.BIPED));
        // transition, antic, preDelay, contact, recovery: the hit lands between preDelay and contact.
        CRUSHER = builder.nextAccessor("biped/murim/warlord_crusher", a -> blow(new AttackAnimation(0.15F, 0.3F, 0.4F, 0.56F, 0.9F,
                CRUSHER_BOX, root(), a, Armatures.BIPED), 1.0F, StunType.LONG));
        EARTH_BREAKER = builder.nextAccessor("biped/murim/warlord_earth_breaker", a -> blow(new AttackAnimation(0.15F, 0.6F, 0.72F, 0.86F, 1.4F,
                EARTH_BOX, root(), a, Armatures.BIPED), 1.3F, StunType.KNOCKDOWN)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3)));
        BULL_CHARGE = builder.nextAccessor("biped/murim/warlord_bull_charge", a -> blow(new AttackAnimation(0.15F, 0.35F, 0.45F, 1.05F, 1.3F,
                CHARGE_BOX, root(), a, Armatures.BIPED), 1.1F, StunType.KNOCKDOWN)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(4)));
        SKY_BREAKER = builder.nextAccessor("biped/murim/warlord_sky_breaker", a -> blow(new AttackAnimation(0.15F, 0.35F, 0.42F, 0.6F, 0.95F,
                SKY_BOX, root(), a, Armatures.BIPED), 1.4F, StunType.LONG));
    }
}
