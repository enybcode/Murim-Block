package com.murimblock.world.feature;

import com.murimblock.world.MurimBiome;
import com.murimblock.world.MurimMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The Honam's rice terraces ({@code murimblock:rice_terraces}), in the manner of Yuanyang and Longji: the hills cut
 * into flat steps that follow their contour lines, each step a paddy of shallow water on mud with young rice in it,
 * held by a retaining wall of packed mud or mossy stone with a beaten path along its rim. The hilltops stay wooded
 * above the paddies.
 * <p>
 * A column's step is its ground rounded down to a multiple of its step height, which grows with the slope (1 block on
 * gentle ground, up to 4 on steep ground) and is read from the map's relief, the same in every chunk. A column only
 * loses its top. Rounding a stepped column gives the same step (its water counts as ground), so a neighbour reads the
 * same whether it was cut yet or not. A column whose four neighbours stand on the same step or higher holds water;
 * the others are the walls.
 */
public class TerraceFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState RICE = Blocks.SEAGRASS.defaultBlockState();
    private static final BlockState MUD = Blocks.MUD.defaultBlockState();
    private static final BlockState PATH = Blocks.DIRT_PATH.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState PACKED_MUD = Blocks.PACKED_MUD.defaultBlockState();
    private static final BlockState MOSSY_STONE = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final ResourceKey<ConfiguredFeature<?, ?>> CROWN_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.withDefaultNamespace("jungle_tree_no_vine"));
    /** One tree per this many crown columns, roughly. */
    private static final int CROWN_SPACING = 23;

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
                // Low ground by the rivers stays as it is; the hilltops stay wooded.
                if (ground <= 64) {
                    continue;
                }
                if (MurimMap.riceCrown(x, z)) {
                    if (Math.floorMod(x * 31 + z * 17 + (x ^ z) * 7, CROWN_SPACING) == 0) {
                        crownTree(context, level, pos.set(x, ground, z));
                    }
                    continue;
                }
                int step = step(x, z, ground);
                for (int y = step; y < ground; y++) {
                    level.setBlock(pos.set(x, y, z), AIR, 2);
                }
                int lowest = Math.min(Math.min(step(level, x + 1, z), step(level, x - 1, z)),
                        Math.min(step(level, x, z + 1), step(level, x, z - 1)));
                boolean paddy = lowest >= step
                        // Now and then a dry field among the paddies.
                        && noise.getValue(x * 0.03, z * 0.03) > -0.45;
                if (paddy) {
                    // Young rice in most of the water.
                    boolean rice = noise.getValue(x * 0.4 + 17, z * 0.4) > -0.25;
                    level.setBlock(pos.set(x, step - 1, z), rice ? RICE : WATER, 2);
                    level.setBlock(pos.set(x, step - 2, z), MUD, 2);
                    level.setBlock(pos.set(x, step - 3, z), MUD, 2);
                } else if (lowest < step) {
                    // A retaining wall down to the next step, a beaten path along its rim.
                    BlockState wall = noise.getValue(x * 0.08, z * 0.08 + 40) > 0.2 ? MOSSY_STONE : PACKED_MUD;
                    level.setBlock(pos.set(x, step - 1, z), PATH, 2);
                    for (int y = step - 2; y >= lowest - 1; y--) {
                        level.setBlock(pos.set(x, y, z), wall, 2);
                    }
                } else {
                    level.setBlock(pos.set(x, step - 1, z), GRASS, 2);
                }
                any = true;
            }
        }
        return any;
    }

    private static void crownTree(FeaturePlaceContext<NoneFeatureConfiguration> context, WorldGenLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
            return;
        }
        level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(CROWN_TREE)
                .ifPresent(tree -> tree.value().place(level, context.chunkGenerator(), context.random(), pos.immutable()));
    }

    /** The step height here: taller where the hill is steeper. */
    private static int stepHeight(int x, int z) {
        double h = MurimMap.relief(x, z).height();
        double slope = Math.max(Math.abs(MurimMap.relief(x + 6, z).height() - h), Math.abs(MurimMap.relief(x, z + 6).height() - h)) / 6;
        return slope < .15 ? 1 : slope < .35 ? 2 : slope < .6 ? 3 : 4;
    }

    private static int step(int x, int z, int ground) {
        int height = stepHeight(x, z);
        return Math.floorDiv(ground, height) * height;
    }

    private static int step(WorldGenLevel level, int x, int z) {
        return step(x, z, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
    }
}
