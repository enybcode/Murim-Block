package com.murimblock.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Joint;
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
 * The Crab King's moves (scripts/animation/crab-king-moves.cjs, docs/mobs/crab-king.md): his idle, his three attacks with
 * the pincers and his roar. He is drawn 1.5 times a man's size, so his hit boxes reach as far (they sit before his root,
 * forward along -z). Every attack is slow to come and can be parried.
 * <ul>
 *   <li>the sweep: the great right pincer swept level across the front, the widest;</li>
 *   <li>the snap: a lunge, both pincers driven forward and snapped shut, the farthest;</li>
 *   <li>the slam: both pincers slammed to the ground ahead; its landing sends a shockwave (CrabKingEntity).</li>
 * </ul>
 */
public final class CrabKingAnimations {
    public static AnimationAccessor<StaticAnimation> IDLE;
    public static AnimationAccessor<AttackAnimation> SWEEP, SNAP, SLAM;
    public static AnimationAccessor<ActionAnimation> ROAR;

    /** When the slam's pincers meet the ground (seconds into it): the shockwave then. */
    public static final float SLAM_IMPACT = 0.74F;

    private static final OBBCollider SWEEP_BOX = new OBBCollider(2.8D, 1.0D, 1.8D, 0.0D, 1.3D, -2.2D);
    private static final OBBCollider SNAP_BOX = new OBBCollider(1.4D, 1.0D, 1.9D, 0.0D, 1.4D, -2.7D);
    private static final OBBCollider SLAM_BOX = new OBBCollider(1.7D, 1.2D, 1.8D, 0.0D, 0.9D, -2.4D);

    private CrabKingAnimations() { }

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
    }
}
