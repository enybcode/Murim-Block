package com.murimblock.integration.epicfight;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;

/**
 * The Devouring Demon Art's animations (docs/combat/styles/devouring-demon-art.md). The beast gait of the demon form
 * (scripts/animation/demon-beast-gait.cjs): on all fours, the body level, its hands and feet on the ground (checked at
 * every key), crouched and breathing, walking diagonally, and bounding when it runs. The techniques' bodies
 * (scripts/animation/demon-art-techniques.cjs): the kagune strikes, the body drives it.
 */
public final class DemonArtAnimations {
    public static AnimationAccessor<StaticAnimation> BEAST_IDLE;
    public static AnimationAccessor<MovementAnimation> BEAST_WALK, BEAST_RUN;
    public static AnimationAccessor<ActionAnimation> LASH, BITE, THORNS, BURROW;

    private DemonArtAnimations() { }

    static void build(AnimationBuilder builder) {
        BEAST_IDLE = builder.nextAccessor("biped/murim/demon_beast_idle", a -> new StaticAnimation(true, a, Armatures.BIPED));
        BEAST_WALK = builder.nextAccessor("biped/murim/demon_beast_walk", a -> new MovementAnimation(true, a, Armatures.BIPED));
        BEAST_RUN = builder.nextAccessor("biped/murim/demon_beast_run", a -> new MovementAnimation(true, a, Armatures.BIPED));
        LASH = builder.nextAccessor("biped/murim/demon_lash", a -> new ActionAnimation(0.05F, a, Armatures.BIPED));
        BITE = builder.nextAccessor("biped/murim/demon_bite", a -> new ActionAnimation(0.05F, a, Armatures.BIPED));
        THORNS = builder.nextAccessor("biped/murim/demon_thorns", a -> new ActionAnimation(0.05F, a, Armatures.BIPED));
        BURROW = builder.nextAccessor("biped/murim/demon_burrow", a -> new ActionAnimation(0.05F, a, Armatures.BIPED));
    }
}
