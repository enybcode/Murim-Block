package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Gives a biome its own ground: replaces the stone (and buried dirt) of the upper layers with the landscape's rock,
 * in horizontal bands, so cliffs, ravines and river banks show it: thick loess with red fossil soil bands on the
 * plateau, limestone in the karst, pale granite on Huangshan. Runs once per chunk, column by column, only where the
 * surface belongs to the biome; the top block (grass, sand, snow) stays as the surface rules made it.
 */
public class GroundBodyFeature extends Feature<GroundBodyFeature.Config> {
    public GroundBodyFeature() {
        super(Config.CODEC);
    }

    public record Config(ResourceKey<Biome> biome, BlockState body, BlockState band, int depth, int bandSpacing)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(Config::biome),
                BlockState.CODEC.fieldOf("body").forGetter(Config::body),
                BlockState.CODEC.fieldOf("band").forGetter(Config::band),
                Codec.intRange(1, 128).fieldOf("depth").forGetter(Config::depth),
                Codec.intRange(0, 64).fieldOf("band_spacing").forGetter(Config::bandSpacing)
        ).apply(i, Config::new));
    }

    @Override public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config c = context.config();
        int minX = context.origin().getX() & ~15, minZ = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (!level.getBiome(pos.set(x, surface, z)).is(c.biome())) continue;
                for (int y = surface - 1; y > surface - c.depth() && y > level.getMinBuildHeight(); y--) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.is(BlockTags.BASE_STONE_OVERWORLD) && !state.is(BlockTags.DIRT)) continue;
                    boolean band = c.bandSpacing() > 0 && Math.floorMod(y, c.bandSpacing()) == 0;
                    level.setBlock(pos, band ? c.band() : c.body(), 2);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
