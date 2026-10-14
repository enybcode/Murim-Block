package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murimblock.world.MurimWorldgen;
import com.murimblock.world.structure.site.SitePlan;
import com.murimblock.world.structure.site.SiteStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Keeps a placed feature (trees, bamboo, rock peaks...) off the Murim sites: no tree grows through a roof and no
 * karst tower rises through a courtyard. Rejects positions within {@code margin} blocks of a site's reach, using the
 * structure references of the chunks around (a site's origin is the middle of its start chunk).
 */
public final class AwayFromSitesFilter extends PlacementFilter {
    public static final MapCodec<AwayFromSitesFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 48).fieldOf("margin").forGetter(f -> f.margin)
    ).apply(i, AwayFromSitesFilter::new));

    private final int margin;

    public AwayFromSitesFilter(int margin) {
        this.margin = margin;
    }

    @Override protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !nearSite(context.getLevel(), pos.getX(), pos.getZ(), margin);
    }

    /**
     * True if (x, z) is within {@code margin} blocks of the reach of a Murim site. Reads the structure starts of the
     * chunks around (world generation keeps them available 8 chunks out while features are placed).
     */
    public static boolean nearSite(WorldGenLevel level, int x, int z, int margin) {
        int cx = x >> 4, cz = z >> 4;
        int around = Math.min(8, (SitePlan.MAX_REACH + margin) / 16 + 1);
        for (int dx = -around; dx <= around; dx++) for (int dz = -around; dz <= around; dz++) {
            if (!level.hasChunk(cx + dx, cz + dz)) continue;
            ChunkAccess chunk = level.getChunk(cx + dx, cz + dz, ChunkStatus.STRUCTURE_STARTS, false);
            if (chunk == null) continue;
            for (var entry : chunk.getAllStarts().entrySet()) {
                if (!(entry.getKey() instanceof SiteStructure site) || !entry.getValue().isValid()) continue;
                int reach = site.plan().site().reach() + margin;
                ChunkPos start = entry.getValue().getChunkPos();
                if (Math.abs(x - start.getMiddleBlockX()) <= reach && Math.abs(z - start.getMiddleBlockZ()) <= reach) return true;
            }
        }
        return false;
    }

    @Override public PlacementModifierType<?> type() {
        return MurimWorldgen.AWAY_FROM_SITES.get();
    }
}
