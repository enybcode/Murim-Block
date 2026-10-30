package com.murimblock.world.feature;

import com.murimblock.world.MurimBiome;
import com.murimblock.world.MurimMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

/**
 * The Honam's rice terraces ({@code murimblock:rice_terraces}): the hills cut into flat steps that follow their
 * contour lines, each step a paddy of shallow water on mud, held by a grass bank along its lower edge.
 * <p>
 * Each column's step is its ground rounded down to a multiple of {@link #STEP}; a column only loses its top. A column
 * whose four neighbours stand on the same step or higher holds water; the others are the banks. Rounding a stepped
 * column gives the same step (its water counts as ground), so a neighbour reads the same whether it was cut yet or not.
 */
public class TerraceFeature extends Feature<NoneFeatureConfiguration> {
    /** The height of one step (blocks). */
    static final int STEP = 3;

    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState MUD = Blocks.MUD.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public TerraceFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        SimplexNoise noise = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(level.getSeed() ^ 0x7E44ACE5L)));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                if (MurimMap.biome(x, z) != MurimBiome.RICE_TERRACES) {
                    continue;
                }
                int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                // Low ground by the rivers stays as it is.
                if (ground <= 64) {
                    continue;
                }
                int step = step(ground);
                for (int y = step; y < ground; y++) {
                    level.setBlock(pos.set(x, y, z), AIR, 2);
                }
                boolean paddy = step(level, x + 1, z) >= step && step(level, x - 1, z) >= step
                        && step(level, x, z + 1) >= step && step(level, x, z - 1) >= step
                        // Now and then a dry field among the paddies.
                        && noise.getValue(x * 0.03, z * 0.03) > -0.45;
                if (paddy) {
                    level.setBlock(pos.set(x, step - 1, z), WATER, 2);
                    level.setBlock(pos.set(x, step - 2, z), MUD, 2);
                    level.setBlock(pos.set(x, step - 3, z), MUD, 2);
                } else {
                    level.setBlock(pos.set(x, step - 1, z), GRASS, 2);
                    for (int y = step - 2; y >= step - 1 - STEP; y--) {
                        if (level.getBlockState(pos.set(x, y, z)).is(Blocks.STONE)) {
                            level.setBlock(pos, DIRT, 2);
                        }
                    }
                }
                any = true;
            }
        }
        return any;
    }

    private static int step(int ground) {
        return Math.floorDiv(ground, STEP) * STEP;
    }

    private static int step(WorldGenLevel level, int x, int z) {
        return step(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
    }
}
