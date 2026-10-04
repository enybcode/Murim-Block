package com.murimblock.combat.attack;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Shared action contract; registering an animation or applying damage is an adapter's job. */
public record AttackDefinition(ResourceLocation id, ResourceLocation animation,
                               AttackTimeline timeline, BladeTrajectory blade) {
    public AttackDefinition {
        Objects.requireNonNull(id);
        Objects.requireNonNull(animation);
        Objects.requireNonNull(timeline);
        Objects.requireNonNull(blade);
        if (blade.durationTicks() < timeline.durationTicks()) {
            throw new IllegalArgumentException("Blade motion must cover the entire attack timeline");
        }
    }
}
