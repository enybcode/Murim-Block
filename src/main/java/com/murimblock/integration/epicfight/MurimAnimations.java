package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DashAttackAnimation;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;

/**
 * Murim's own player animations for the Epic Fight biped (assets/murimblock/animmodels/animations/biped/murim, written
 * by scripts/animation/knockdown-animations.cjs), and the living motions that select them.
 */
public final class MurimAnimations {
    public static AnimationAccessor<StaticAnimation> DOWNED_IDLE;
    public static AnimationAccessor<MovementAnimation> DOWNED_CRAWL;
    public static AnimationAccessor<StaticAnimation> CARRY_IDLE;
    public static AnimationAccessor<MovementAnimation> CARRY_WALK;
    public static AnimationAccessor<StaticAnimation> CARRIED;
    /**
     * Azure Cloud Sword Art: Cloud Step Slash, a 5-block dash slashing everything on its path (Epic Fight's dash attack,
     * its own root motion, the long dash collider on the root joint).
     */
    public static AnimationAccessor<DashAttackAnimation> CLOUD_STEP_SLASH;
    /**
     * The path Cloud Step Slash cuts: a box 1.6 wide, 1.6 high and 5.4 long, from the player to the end of the dash
     * (Epic Fight's joint space: -Z is forward). The server does not move a player during a dash (the client does, and
     * reports the position at the end), so the whole path is covered from the start of the cut.
     */
    private static final OBBCollider CLOUD_STEP_PATH = new OBBCollider(0.8D, 0.8D, 2.7D, 0.0D, 1.0D, -2.7D);
    /** Played by whoever executes a knocked-down player, until each martial art brings its own finish. */
    public static AnimationAccessor<ActionAnimation> FINISH_GENERIC;

    private MurimAnimations() {
    }

    /** Part of Murimblock's single animation builder (see {@link ArmoredCrabEpicFight#register}). */
    static void buildAnimations(AnimationBuilder builder) {
        DOWNED_IDLE = builder.nextAccessor("biped/murim/downed_idle", accessor -> new StaticAnimation(true, accessor, Armatures.BIPED));
        DOWNED_CRAWL = builder.nextAccessor("biped/murim/downed_crawl", accessor -> new MovementAnimation(true, accessor, Armatures.BIPED));
        CARRY_IDLE = builder.nextAccessor("biped/murim/carry_idle", accessor -> new StaticAnimation(true, accessor, Armatures.BIPED));
        CARRY_WALK = builder.nextAccessor("biped/murim/carry_walk", accessor -> new MovementAnimation(true, accessor, Armatures.BIPED));
        CARRIED = builder.nextAccessor("biped/murim/carried", accessor -> new StaticAnimation(true, accessor, Armatures.BIPED));
        FINISH_GENERIC = builder.nextAccessor("biped/murim/finish_generic", accessor -> new ActionAnimation(0.1F, accessor, Armatures.BIPED));
        // transition, antic, preDelay, contact, recovery: the blade cuts from the start of the dash to the end of the sweep.
        CLOUD_STEP_SLASH = builder.nextAccessor("biped/murim/cloud_step_slash", accessor ->
                new DashAttackAnimation(0.08F, 0.12F, 0.16F, 0.46F, 0.66F, CLOUD_STEP_PATH, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                        .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.4F))
                        .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3))
                        .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                        .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                        .addProperty(AttackAnimationProperty.FIXED_MOVE_DISTANCE, true));
    }

    /** Murim's living motions: what a knocked-down, carrying or carried player is doing. */
    public enum Motions implements LivingMotion {
        DOWNED, CRAWL, CARRY, CARRY_WALK, CARRIED;

        final int id;

        Motions() {
            this.id = LivingMotion.ENUM_MANAGER.assign(this);
        }

        @Override
        public int universalOrdinal() {
            return this.id;
        }
    }

    /** Called from the mod constructor, like Epic Fight registers its own motions. */
    public static void registerMotions() {
        LivingMotion.ENUM_MANAGER.registerEnumCls(Murimblock.MOD_ID, Motions.class);
    }
}
