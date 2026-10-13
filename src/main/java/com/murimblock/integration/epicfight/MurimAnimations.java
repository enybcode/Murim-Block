package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AirSlashAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.ComboAttackAnimation;
import yesman.epicfight.api.animation.types.DashAttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;

/**
 * Murim's own player animations for the Epic Fight biped (assets/murimblock/animmodels/animations/biped/murim, written
 * by scripts/animation/knockdown-animations.cjs and azure-cloud-animations.cjs), and the living motions that select them.
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
     * Cloud Step Slash held: the draw stance (the blade sheathed back along the left hip) while the key is down, looping;
     * the player cannot walk but turns with the camera to aim. Let go, the dash. Both from Blockbench
     * (art/blockbench/azure_cloud_step.bbmodel).
     */
    public static AnimationAccessor<StaticAnimation> CLOUD_STEP_AIM;
    /**
     * The path Cloud Step Slash cuts: a box 1.6 wide, 1.6 high and 5.4 long, from the player to the end of the dash
     * (Epic Fight's joint space: -Z is forward). The server does not move a player during a dash (the client does, and
     * reports the position at the end), so the whole path is covered from the start of the cut.
     */
    private static final OBBCollider CLOUD_STEP_PATH = new OBBCollider(0.8D, 0.8D, 2.7D, 0.0D, 1.0D, -2.7D);
    /** Played by whoever executes a knocked-down player, until each martial art brings its own finish. */
    public static AnimationAccessor<ActionAnimation> FINISH_GENERIC;

    // ---------------------------------------------------------------- Azure Cloud Sword Art (scripts/animation/azure-cloud-animations.cjs)
    // Every hit below comes from a box on the root joint (Epic Fight's joint space: Y up, -Z forward), not from the blade,
    // so a strike lands where it is drawn whatever the sword. The server does not move a player during an animation (the
    // client does), so a box covers the whole way a step or a dash goes.

    /** Flowing Cloud Stance, the art's passive: its six strikes (M1), the air attack and the sprinting attack. */
    public static AnimationAccessor<ComboAttackAnimation> DRIFTING_CUT, RETURNING_CUT, PIERCING_MIST, RISING_CLOUD, FALLING_RAIN;
    public static AnimationAccessor<AirSlashAnimation> FALLING_RAIN_AIR;
    public static AnimationAccessor<DashAttackAnimation> WIND_STEP;
    /** Cloud Step Slash from the air (plunges, knocks down) and after a perfect parry (short, cuts twice). */
    public static AnimationAccessor<DashAttackAnimation> CLOUD_STEP_DIVE;
    public static AnimationAccessor<AttackAnimation> CLOUD_STEP_RIPOSTE;
    /** The secret: the spinning cut from behind the attacker. */
    public static AnimationAccessor<AttackAnimation> SWALLOW_RETURNS;
    /** Thousand Drizzle: six thrusts that hold, a last one that pushes; Storm of Needles, its evolution, is wider. */
    public static AnimationAccessor<AttackAnimation> THOUSAND_DRIZZLE, THOUSAND_DRIZZLE_STORM;
    /** Thousand Drizzle at mid-distance: one thrust; the wave of Qi does the hitting (QiBladeEntity). */
    public static AnimationAccessor<ActionAnimation> DRIZZLE_WAVE;
    /** Heaven-Splitting Crescent on the ground (lifts) and its air version, Cloud-Splitting Descent (slams, knocks down). */
    public static AnimationAccessor<AttackAnimation> HEAVEN_SPLITTING_CRESCENT, CLOUD_SPLITTING_DESCENT;
    /** Last Rain Over the Clouds, the art's finish: the blade to the sky, a leap and one vertical cut (lands at 1.5 s). */
    public static AnimationAccessor<ActionAnimation> FINISH_AZURE;

    /** A wide horizontal arc in front: 2.6 across, from 0.1 to 2.3 ahead. */
    private static final OBBCollider ARC = new OBBCollider(1.3D, 0.9D, 1.1D, 0.0D, 1.0D, -1.2D);
    /** A long narrow thrust, up to 3.6 ahead. */
    private static final OBBCollider PIERCE = new OBBCollider(0.5D, 0.6D, 1.7D, 0.0D, 1.1D, -1.9D);
    /** A cut from low to high, tall. */
    private static final OBBCollider RISE = new OBBCollider(1.0D, 1.2D, 1.0D, 0.0D, 1.1D, -1.2D);
    /** A cut from high to low. */
    private static final OBBCollider FALL = new OBBCollider(0.9D, 1.2D, 1.1D, 0.0D, 0.9D, -1.3D);
    /** Wind Step's slide: two blocks forward, so a box from the player to 3.2 ahead. */
    private static final OBBCollider WIND_STEP_PATH = new OBBCollider(1.0D, 0.9D, 1.6D, 0.0D, 1.0D, -1.6D);
    /** The dive: 4 ahead and 2.5 down. */
    private static final OBBCollider DIVE_PATH = new OBBCollider(0.9D, 1.8D, 2.3D, 0.0D, -0.4D, -2.2D);
    /** The riposte dash: 2.5 ahead. */
    private static final OBBCollider RIPOSTE_PATH = new OBBCollider(0.9D, 0.9D, 1.7D, 0.0D, 1.0D, -1.5D);
    /** The spin of Swallow Returns: all around, mostly in front. */
    private static final OBBCollider SPIN = new OBBCollider(1.5D, 1.0D, 1.5D, 0.0D, 1.0D, -0.7D);
    private static final OBBCollider DRIZZLE = new OBBCollider(0.7D, 0.8D, 1.4D, 0.0D, 1.1D, -1.6D);
    private static final OBBCollider STORM = new OBBCollider(1.4D, 1.0D, 1.7D, 0.0D, 1.1D, -1.8D);
    private static final OBBCollider CRESCENT = new OBBCollider(1.2D, 1.5D, 1.2D, 0.0D, 1.3D, -1.2D);
    /** The shock of the descent: three blocks all around. */
    private static final OBBCollider SHOCKWAVE = new OBBCollider(3.0D, 1.4D, 3.0D, 0.0D, 0.4D, 0.0D);

    /** Thousand Drizzle's six quick thrusts (seconds), as drawn by the animation script. */
    public static final float[] DRIZZLE_THRUSTS = {0.2F, 0.32F, 0.44F, 0.56F, 0.68F, 0.8F};
    /** The last, pushing thrust of Thousand Drizzle starts there. */
    public static final float DRIZZLE_FINAL = 0.98F;

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
        CLOUD_STEP_AIM = builder.nextAccessor("biped/murim/cloud_step_aim", accessor -> new HeldStance(0.12F, accessor));
        // transition, antic, preDelay, contact, recovery: out of the draw stance, the blade cuts from the launch to the end
        // of the draw (the 5 blocks take 0.2 s); then the pose past the target and the flick.
        CLOUD_STEP_SLASH = builder.nextAccessor("biped/murim/cloud_step_slash", accessor ->
                new DashAttackAnimation(0.04F, 0.02F, 0.04F, 0.22F, 0.55F, CLOUD_STEP_PATH, Armatures.BIPED.get().rootJoint, accessor, Armatures.BIPED)
                        .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.4F))
                        .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3))
                        .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                        .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                        .addProperty(AttackAnimationProperty.FIXED_MOVE_DISTANCE, true));
        buildAzureCloudSword(builder);
    }

    /**
     * A stance held in a loop: the feet still, the body free to turn (to aim), no attack, skill or item meanwhile, and
     * Epic Fight's walking and idling kept from replacing it. Its states hold over the whole loop.
     */
    private static final class HeldStance extends StaticAnimation {
        HeldStance(float transition, AnimationAccessor<? extends StaticAnimation> accessor) {
            super(transition, true, accessor, Armatures.BIPED);
            this.stateSpectrumBlueprint.clear().newTimePair(0.0F, Float.MAX_VALUE)
                    .addState(EntityState.MOVEMENT_LOCKED, true)
                    .addState(EntityState.TURNING_LOCKED, false)
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

    /**
     * The Azure Cloud Sword Art. The strikes (M1) follow the weapon's speed like Epic Fight's own (authored for a sword,
     * 1.6 attacks a second); the techniques play at their own pace whatever the weapon.
     */
    private static void buildAzureCloudSword(AnimationBuilder builder) {
        // transition, antic, preDelay, contact, recovery: the hit lands between preDelay and contact. The four default
        // strikes come from Blockbench (art/blockbench/azure_cloud_m1.bbmodel): each window covers its swing through the front.
        DRIFTING_CUT = builder.nextAccessor("biped/murim/drifting_cut", a -> strike(new ComboAttackAnimation(0.08F, 0.08F, 0.13F, 0.28F, 0.42F, ARC, root(), a, Armatures.BIPED), 1.0F));
        RETURNING_CUT = builder.nextAccessor("biped/murim/returning_cut", a -> strike(new ComboAttackAnimation(0.08F, 0.07F, 0.12F, 0.27F, 0.4F, ARC, root(), a, Armatures.BIPED), 1.0F));
        PIERCING_MIST = builder.nextAccessor("biped/murim/piercing_mist", a -> strike(new ComboAttackAnimation(0.08F, 0.12F, 0.17F, 0.32F, 0.45F, PIERCE, root(), a, Armatures.BIPED), 1.1F)
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(20.0F))
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get()));
        RISING_CLOUD = builder.nextAccessor("biped/murim/rising_cloud", a -> strike(new ComboAttackAnimation(0.08F, 0.1F, 0.16F, 0.28F, 0.45F, RISE, root(), a, Armatures.BIPED), 1.0F)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG));
        FALLING_RAIN = builder.nextAccessor("biped/murim/falling_rain", a -> strike(new ComboAttackAnimation(0.08F, 0.15F, 0.2F, 0.31F, 0.52F, FALL, root(), a, Armatures.BIPED), 1.25F)
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN));
        // Epic Fight's air attack: plays when the player attacks while rising or at the top of a jump.
        FALLING_RAIN_AIR = builder.nextAccessor("biped/murim/falling_rain_air", a -> new AirSlashAnimation(0.08F, 0.1F, 0.3F, 0.55F, FALL, root(), a, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.25F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F));
        WIND_STEP = builder.nextAccessor("biped/murim/wind_step", a -> new DashAttackAnimation(0.08F, 0.08F, 0.12F, 0.36F, 0.5F, WIND_STEP_PATH, root(), a, Armatures.BIPED)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.1F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2))
                .addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F)
                .addProperty(AttackAnimationProperty.FIXED_MOVE_DISTANCE, true));

        CLOUD_STEP_DIVE = builder.nextAccessor("biped/murim/cloud_step_dive", a -> technique(new DashAttackAnimation(0.06F, 0.12F, 0.16F, 0.46F, 0.66F, DIVE_PATH, root(), a, Armatures.BIPED), 1.4F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(3))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                .addProperty(AttackAnimationProperty.FIXED_MOVE_DISTANCE, true)
                .addProperty(ActionAnimationProperty.MOVE_VERTICAL, true));
        CLOUD_STEP_RIPOSTE = builder.nextAccessor("biped/murim/cloud_step_riposte", a -> technique(new AttackAnimation(0.06F, a, Armatures.BIPED,
                        new Phase(0.0F, 0.1F, 0.12F, 0.22F, 0.24F, 0.24F, root(), RIPOSTE_PATH),
                        new Phase(0.24F, 0.27F, 0.28F, 0.38F, 0.5F, Float.MAX_VALUE, root(), RIPOSTE_PATH)), 1.0F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2))
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                .addProperty(AttackAnimationProperty.FIXED_MOVE_DISTANCE, true));
        SWALLOW_RETURNS = builder.nextAccessor("biped/murim/swallow_returns", a -> technique(new AttackAnimation(0.02F, 0.0F, 0.08F, 0.32F, 0.45F, SPIN, root(), a, Armatures.BIPED), 1.3F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get()));

        THOUSAND_DRIZZLE = builder.nextAccessor("biped/murim/thousand_drizzle", a -> drizzle(a, DRIZZLE));
        THOUSAND_DRIZZLE_STORM = builder.nextAccessor("biped/murim/thousand_drizzle_storm", a -> drizzle(a, STORM));
        DRIZZLE_WAVE = builder.nextAccessor("biped/murim/drizzle_wave", a -> new ActionAnimation(0.06F, a, Armatures.BIPED));

        HEAVEN_SPLITTING_CRESCENT = builder.nextAccessor("biped/murim/heaven_splitting_crescent", a -> technique(new AttackAnimation(0.06F, 0.14F, 0.2F, 0.34F, 0.6F, CRESCENT, root(), a, Armatures.BIPED), 1.2F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get()));
        CLOUD_SPLITTING_DESCENT = builder.nextAccessor("biped/murim/cloud_splitting_descent", a -> technique(new AttackAnimation(0.04F, 0.14F, 0.2F, 0.34F, 0.6F, SHOCKWAVE, root(), a, Armatures.BIPED), 1.3F)
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(5))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_BIG.get()));

        FINISH_AZURE = builder.nextAccessor("biped/murim/finish_azure", a -> new ActionAnimation(0.1F, a, Armatures.BIPED));
    }

    /** A strike of the M1 chain: its damage, at the weapon's speed. */
    private static <A extends AttackAnimation> A strike(A animation, float damage) {
        animation.addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage));
        animation.addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.6F);
        return animation;
    }

    /** A technique: its damage, at its own pace whatever the weapon's speed. */
    private static <A extends AttackAnimation> A technique(A animation, float damage) {
        animation.addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage));
        animation.addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F);
        return animation;
    }

    /**
     * Thousand Drizzle: one phase per quick thrust (a light hit that holds the target in place), then the last thrust
     * (heavier, pushes). Each phase strikes anew, so one target takes all seven.
     */
    private static AttackAnimation drizzle(AnimationAccessor<? extends AttackAnimation> accessor, OBBCollider box) {
        Phase[] phases = new Phase[DRIZZLE_THRUSTS.length + 1];
        float start = 0.0F;
        for (int i = 0; i < DRIZZLE_THRUSTS.length; i++) {
            float t = DRIZZLE_THRUSTS[i];
            phases[i] = new Phase(start, t - 0.05F, t - 0.03F, t + 0.02F, t + 0.04F, t + 0.06F, root(), box)
                    .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.35F))
                    .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
                    .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get());
            start = t + 0.06F;
        }
        phases[DRIZZLE_THRUSTS.length] = new Phase(start, DRIZZLE_FINAL, DRIZZLE_FINAL + 0.03F, DRIZZLE_FINAL + 0.12F, 1.3F, Float.MAX_VALUE, root(), box)
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.9F))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(3.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH.get());
        return new AttackAnimation(0.06F, accessor, Armatures.BIPED, phases)
                .addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F);
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
