package com.murimblock.client;

import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Idles out of step. Bandits come in bands of two to four that spawn together: each would start its idle at the same
 * moment and breathe, glance and lift its blade in unison, like copies. So the first time a mob is drawn standing in its
 * idle, it starts at its own point of the loop (from its id). Only then, before anyone has seen it: once it has moved,
 * it comes back to the idle from the start, as anyone does, already out of step with the others.
 */
public final class IdlePhase {
    /** The mobs already drawn once (render thread only). */
    private static final Map<LivingEntity, Boolean> SEEN = new WeakHashMap<>();

    private IdlePhase() { }

    /** Where in its idle (0 to 1) a mob with this id starts. */
    public static float phase(int id) {
        return (MobFaces.mix(id, 0, 9) % 1000) / 1000.0F;
    }

    /** Called each time the mob is drawn, before it is posed. */
    public static void desync(LivingEntity mob, LivingEntityPatch<?> patch, AssetAccessor<? extends StaticAnimation> idle) {
        if (SEEN.put(mob, Boolean.TRUE) != null || idle == null) return;
        AnimationPlayer player = patch.getAnimator().getPlayerFor(null);
        if (player == null || player.getAnimation() != idle || player.getRealAnimation() != idle) return;
        float at = phase(mob.getId()) * idle.get().getTotalTime();
        player.setElapsedTime(at, at);
    }
}
