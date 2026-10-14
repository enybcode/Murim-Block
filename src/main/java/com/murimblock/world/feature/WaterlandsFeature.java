package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The Jiangnan water country (lake Tai, the canal towns): the biome's flat land lies just under sea level, so left
 * alone it is one open lake. This turns it into what the region looks like: low strips and fields of land one block
 * above the water, divided by winding canals, with ponds of every size between them, shallow and muddy-bottomed;
 * only here and there the open lake. Runs once per chunk, column by column, where the biome is.
 */
public class WaterlandsFeature extends Feature<WaterlandsFeature.Config> {
    public WaterlandsFeature() {
        super(Config.CODEC);
    }

    public record Config(ResourceKey<Biome> biome, float land) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(Config::biome),
                Codec.floatRange(-1, 1).fieldOf("land").forGetter(Config::land)
        ).apply(i, Config::new));
    }

    private static long cachedSeed;
    private static SimplexNoise fields, detail, canals, lakes;

    private static synchronized void noises(long seed) {
        if (fields != null && cachedSeed == seed) return;
        cachedSeed = seed;
        fields = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed ^ 0x51A7E5L)));
        detail = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed ^ 0x2B1D0CL)));
        canals = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed ^ 0x7C3A11L)));
        lakes = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed ^ 0x19F0A3L)));
    }

    @Override public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config c = context.config();
        noises(level.getSeed());
        int minX = context.origin().getX() & ~15, minZ = context.origin().getZ() & ~15;
        int waterTop = level.getSeaLevel() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1; // top solid block
                if (floor >= waterTop) continue; // dry land: left as it is
                if (!level.getBlockState(pos.set(x, waterTop, z)).is(Blocks.WATER)) continue;
                if (!level.getBiome(pos.set(x, waterTop, z)).is(c.biome())) continue;
                int depth = waterTop - floor;
                // Open lake where this broad noise is low, or where the water is really deep.
                boolean lake = lakes.getValue(x / 160.0, z / 160.0) < -0.62 || depth > 12;
                // Fields and strips of land, broken by ponds; canals wind through as thin lines.
                double field = fields.getValue(x / 46.0, z / 46.0) * 0.7 + detail.getValue(x / 13.0, z / 13.0) * 0.3;
                boolean canal = Math.abs(canals.getValue(x / 75.0, z / 75.0)) < 0.045;
                boolean land = !lake && !canal && field > -c.land();
                if (land) {
                    for (int y = floor + 1; y < waterTop + 1; y++) level.setBlock(pos.set(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                    level.setBlock(pos.set(x, waterTop + 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    changed = true;
                } else if (!lake && depth > 2) {
                    // Ponds and canals stay shallow: a muddy bottom two blocks down, three in their middle.
                    int bottom = waterTop - (field < -c.land() - 0.25 ? 3 : 2);
                    for (int y = floor + 1; y <= bottom; y++) level.setBlock(pos.set(x, y, z), Blocks.MUD.defaultBlockState(), 2);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
