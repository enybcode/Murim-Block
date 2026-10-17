package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * At least {@link #GAP} blocks between any two Murim structures, edge to edge (Enzo, 16/10/2026: so none overlap).
 * <p>
 * Chunks are generated one by one, so a site cannot see the others: it works them out. Every structure set of the mod
 * is a random spread, whose grid gives, from the seed alone, the one chunk of each cell where a site may start. A
 * site refuses a place when another site may start near enough (in a biome it accepts, where it is allowed) that the
 * two would come within the gap. It does not ask the other site's terrain, so it errs on the side of stepping aside.
 * <p>
 * Each pair is weighed by one side only, so the two never both give way: the more frequent site (the smaller spacing)
 * steps aside for the rarer one; between two sets as frequent, the set whose name sorts first wins; two starts of the
 * same set, the cell that sorts first. The unique structures built at runtime (the Cloud Cliff Sanctuary, the Crab
 * King's lair) step aside for every site.
 */
public final class SiteSpacing {
    /** The least distance between two structures' edges (blocks). */
    public static final int GAP = 100;
    /** How far a structure that is not a Murim site is taken to reach from its centre (blocks). */
    static final int OTHER_REACH = 32;

    private SiteSpacing() {
    }

    /**
     * Whether a structure whose centre is the middle of {@code chunk}, reaching {@code reach} blocks out, would come within
     * the gap of a site that may start round it. {@code self}: its own structure set, which decides who gives way; null
     * for a structure built at runtime, which gives way to all.
     */
    public static boolean crowded(RegistryAccess access, long seed, BiomeSource biomes, Climate.Sampler sampler, int seaLevel,
                                  ChunkPos chunk, int reach, @Nullable ResourceKey<StructureSet> self) {
        var sets = access.registryOrThrow(Registries.STRUCTURE_SET);
        int ownSpacing = Integer.MAX_VALUE;
        if (self != null && sets.get(self) != null && sets.get(self).placement() instanceof RandomSpreadStructurePlacement own) ownSpacing = own.spacing();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        for (var entry : sets.entrySet()) {
            ResourceKey<StructureSet> key = entry.getKey();
            if (!key.location().getNamespace().equals(Murimblock.MOD_ID)) continue;
            StructureSet set = entry.getValue();
            if (!(set.placement() instanceof RandomSpreadStructurePlacement spread)) continue;
            boolean same = key.equals(self);
            // Who gives way: the more frequent set, or (as frequent) the one whose name sorts last.
            if (self != null && !same) {
                int spacing = spread.spacing();
                if (spacing < ownSpacing) continue;
                if (spacing == ownSpacing && key.location().compareTo(self.location()) > 0) continue;
            }
            int spacing = spread.spacing();
            int ownCellX = Math.floorDiv(chunk.x, spacing), ownCellZ = Math.floorDiv(chunk.z, spacing);
            for (var choice : set.structures()) {
                Structure other = choice.structure().value();
                int otherReach = other instanceof SiteStructure s ? s.plan().site().reach() : OTHER_REACH;
                double limit = reach + otherReach + GAP;
                int cells = (int) Math.ceil(limit / 16.0);
                for (int rx = Math.floorDiv(chunk.x - cells, spacing); rx <= Math.floorDiv(chunk.x + cells, spacing); rx++)
                    for (int rz = Math.floorDiv(chunk.z - cells, spacing); rz <= Math.floorDiv(chunk.z + cells, spacing); rz++) {
                        // Two starts of the same set: the cell that sorts first keeps its place.
                        if (same && (rx > ownCellX || rx == ownCellX && rz >= ownCellZ)) continue;
                        ChunkPos there = spread.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                        int ox = there.getMiddleBlockX(), oz = there.getMiddleBlockZ();
                        if (Math.hypot(ox - x, oz - z) >= limit) continue;
                        if (other instanceof SiteStructure s && !s.plan().site().allowedAt(ox, oz)) continue;
                        var biome = biomes.getNoiseBiome(QuartPos.fromBlock(ox), QuartPos.fromBlock(seaLevel), QuartPos.fromBlock(oz), sampler);
                        if (other.biomes().contains(biome)) return true;
                    }
            }
        }
        return false;
    }

    /** For a structure built at runtime in this level, centred on this block: whether a site may start too near. */
    public static boolean crowded(ServerLevel level, int x, int z, int reach) {
        var generator = level.getChunkSource().getGenerator();
        return crowded(level.registryAccess(), level.getSeed(), generator.getBiomeSource(), level.getChunkSource().randomState().sampler(),
                generator.getSeaLevel(), new ChunkPos(x >> 4, z >> 4), reach, null);
    }

    /** The structure set a structure belongs to (null if none of the mod's). */
    @Nullable static ResourceKey<StructureSet> setOf(RegistryAccess access, Structure structure) {
        for (var entry : access.registryOrThrow(Registries.STRUCTURE_SET).entrySet())
            for (var choice : entry.getValue().structures())
                if (choice.structure().value() == structure) return entry.getKey();
        return null;
    }
}
