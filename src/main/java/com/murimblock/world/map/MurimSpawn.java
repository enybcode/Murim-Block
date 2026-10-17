package com.murimblock.world.map;

import com.murimblock.world.MurimBiomes;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Where a new Murim world is entered (decision of 14/10/2026: at random for now; later by one's family): a point drawn
 * from the world's seed anywhere on the land of the bubble, on a Murim biome, neither in water nor on a peak. Worlds of
 * other kinds (flat, the GameTest server's) keep vanilla's spawn.
 */
public final class MurimSpawn {
    /** How far from the centre the spawn may be drawn, and the highest ground it may stand on. */
    public static final int REACH = MurimMap.RADIUS * 3 / 5, HIGHEST = 140;
    private static final int TRIES = 200;

    private MurimSpawn() {
    }

    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        BlockPos spawn = find(level);
        if (spawn == null) return;
        level.setDefaultSpawnPos(spawn, 0.0F);
        event.setCanceled(true);
    }

    /** A spawn on the bubble's land, or null if this world has no Murim map (or none was found). */
    public static BlockPos find(ServerLevel level) {
        var source = level.getChunkSource().getGenerator();
        if (!(source.getBiomeSource() instanceof MurimBiomeSource biomes)) return null;
        var random = new Random(level.getSeed() ^ 0x5350415750L);
        var randomState = level.getChunkSource().randomState();
        for (int i = 0; i < TRIES; i++) {
            int x = random.nextInt(2 * REACH) - REACH, z = random.nextInt(2 * REACH) - REACH;
            int y = source.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
            int surface = source.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
            if (surface != y || y <= level.getSeaLevel() + 1 || y > HIGHEST) continue; // water over it, or too high
            var biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), randomState.sampler());
            if (biome.is(MurimBiomes.MURIM_TAG)) return new BlockPos(x, y, z);
        }
        return null;
    }
}
