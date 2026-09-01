package com.murimblock.qi;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Persistent per-player record of bosses that already granted their first victory reward.
 */
public record QiBossProgress(Set<ResourceLocation> defeatedBosses) {
    public static final Codec<QiBossProgress> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(locations -> new QiBossProgress(new HashSet<>(locations)),
                    progress -> List.copyOf(progress.defeatedBosses));

    public QiBossProgress {
        defeatedBosses = Set.copyOf(defeatedBosses);
    }

    public static QiBossProgress initial() {
        return new QiBossProgress(Set.of());
    }

    public boolean hasDefeated(ResourceLocation bossId) {
        return defeatedBosses.contains(bossId);
    }

    public QiBossProgress markDefeated(ResourceLocation bossId) {
        if (hasDefeated(bossId)) {
            return this;
        }

        Set<ResourceLocation> updated = new HashSet<>(defeatedBosses);
        updated.add(bossId);
        return new QiBossProgress(updated);
    }
}
