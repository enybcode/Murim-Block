package com.murimblock.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Crab King's moves (scripts/animation/crab-king-moves.cjs, docs/mobs/crab-king.md): his idle and heavy walk, his
 * three attacks with the pincers, his grab and his roar. He is drawn twice a man's size, so his hit boxes reach as far
 * (they sit before his root, forward along -z). Every attack is slow to come and can be parried.
 * <ul>
 *   <li>the sweep: the great right pincer swept level across the front, the widest;</li>
 *   <li>the snap: a lunge, both pincers driven forward and snapped shut, the farthest;</li>
 *   <li>the slam: both pincers slammed to the ground ahead; its landing sends a shockwave (CrabKingEntity);</li>
 *   <li>the grab: the great right pincer snapped shut at {@link #GRAB_CATCH}; what it catches (CrabKingEntity) is held up
 *   high, squeezed, and slammed to the ground at {@link #GRAB_RELEASE} of the hold. No hit box of Epic Fight's: the catch
 *   is the entity's, through a guard, but a perfect parry at the catch makes it miss.</li>
 * </ul>
 */
public final class CrabKingAnimations {
    public static AnimationAccessor<StaticAnimation> IDLE, WALK;
    public static AnimationAccessor<AttackAnimation> SWEEP, SNAP, SLAM;
    public static AnimationAccessor<ActionAnimation> ROAR, GRAB, GRAB_HOLD;
    /** Pincers from the deep: both pincers plunged into the ground at {@link #BURROW_PLUNGE}, giant ones bursting ahead. */
    public static AnimationAccessor<ActionAnimation> BURROW;
    public static final float BURROW_PLUNGE = 0.62F;
    /** The mist: it bursts out of him at {@link #MIST_RELEASE}; he is gone into it at {@link #MIST_GONE} (seconds). */
    public static AnimationAccessor<ActionAnimation> MIST;
    public static final float MIST_RELEASE = 0.75F, MIST_GONE = 1.2F;
    /** Played on whoever the pincer holds: pinned, kicking; nothing else can be done meanwhile. */
    public static AnimationAccessor<StaticAnimation> HELD;
    /** On the victim: the hold on its clock (struggling, frozen, crushed, whipped back), then flailing as it flies. */
    public static AnimationAccessor<StaticAnimation> VICTIM, THROWN;

    /** When the slam's pincers meet the ground (seconds into it): the shockwave then. */
    public static final float SLAM_IMPACT = 0.74F;
    /** When the grab's pincer snaps shut (seconds into it), and when the hold's slam lets go (seconds into the hold). */
    public static final float GRAB_CATCH = 0.55F, GRAB_RELEASE = 2.45F;

    private static final OBBCollider SWEEP_BOX = new OBBCollider(3.7D, 1.3D, 2.4D, 0.0D, 1.7D, -2.9D);
    private static final OBBCollider SNAP_BOX = new OBBCollider(1.9D, 1.3D, 2.5D, 0.0D, 1.9D, -3.6D);
    private static final OBBCollider SLAM_BOX = new OBBCollider(2.3D, 1.6D, 2.4D, 0.0D, 1.2D, -3.2D);

    private CrabKingAnimations() { }

    /**
     * Held in the pincer, looped: no moving or turning, no attack, guard, roll or skill, no item used or switched, Epic
     * Fight's walking and idling kept from replacing it. Its states hold over the whole loop; it ends when the Crab King
     * lets go (his slam's knock-down, or a short stun if he is broken off).
     */
    private static final class Pinned extends StaticAnimation {
        Pinned(AnimationAccessor<? extends StaticAnimation> accessor, boolean loop) {
            super(0.08F, loop, accessor, Armatures.BIPED);
            this.stateSpectrumBlueprint.clear().newTimePair(0.0F, Float.MAX_VALUE)
                    .addState(EntityState.MOVEMENT_LOCKED, true)
                    .addState(EntityState.TURNING_LOCKED, true)
                    .addState(EntityState.INACTION, true)
                    .addState(EntityState.UPDATE_LIVING_MOTION, false)
                    .addState(EntityState.COMBO_ATTACKS_DOABLE, false)
                    .addState(EntityState.SKILL_EXECUTABLE, false)
                    .addState(EntityState.CAN_USE_ITEM, false)
                    .addState(EntityState.CAN_SWITCH_HAND_ITEM, false);
        }
    }

    private static Joint root() {
        return Armatures.BIPED.get().rootJoint;
    }

    private static <A extends AttackAnimation> A strike(A animation, float damage) {
        animation.addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage));
        animation.addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLUNT_HIT_HARD.get());
        animation.addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get());
        return animation;
    }

    public static void build(AnimationBuilder builder) {
        IDLE = builder.nextAccessor("biped/murim/crab_king_idle", a -> new StaticAnimation(0.2F, true, a, Armatures.BIPED));
        // transition, antic, preDelay, contact, recovery: the hit lands between preDelay and contact.
        SWEEP = builder.nextAccessor("biped/murim/crab_king_sweep", a -> strike(new AttackAnimation(0.15F, 0.3F, 0.42F, 0.62F, 0.95F,
                SWEEP_BOX, root(), a, Armatures.BIPED), 1.0F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(4)));
        SNAP = builder.nextAccessor("biped/murim/crab_king_snap", a -> strike(new AttackAnimation(0.15F, 0.38F, 0.5F, 0.66F, 1.2F,
                SNAP_BOX, root(), a, Armatures.BIPED), 1.3F)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG));
        SLAM = builder.nextAccessor("biped/murim/crab_king_slam", a -> strike(new AttackAnimation(0.15F, 0.5F, 0.66F, 0.8F, 1.4F,
                SLAM_BOX, root(), a, Armatures.BIPED), 1.5F)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3)));
        ROAR = builder.nextAccessor("biped/murim/crab_king_roar", a -> new ActionAnimation(0.15F, a, Armatures.BIPED));
        WALK = builder.nextAccessor("biped/murim/crab_king_walk", a -> new StaticAnimation(0.2F, true, a, Armatures.BIPED));
        GRAB = builder.nextAccessor("biped/murim/crab_king_grab", a -> new ActionAnimation(0.12F, a, Armatures.BIPED));
        GRAB_HOLD = builder.nextAccessor("biped/murim/crab_king_grab_hold", a -> new ActionAnimation(0.05F, a, Armatures.BIPED));
        MIST = builder.nextAccessor("biped/murim/crab_king_mist", a -> new ActionAnimation(0.15F, a, Armatures.BIPED));
        BURROW = builder.nextAccessor("biped/murim/crab_king_burrow", a -> new ActionAnimation(0.15F, a, Armatures.BIPED));
        HELD = builder.nextAccessor("biped/murim/crab_king_held", a -> new Pinned(a, true));
        VICTIM = builder.nextAccessor("biped/murim/crab_king_victim", a -> new Pinned(a, false));
        THROWN = builder.nextAccessor("biped/murim/crab_king_thrown", a -> new Pinned(a, true));
    }
}
