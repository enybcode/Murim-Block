package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Yardang of the Dunhuang desert: a long ridge of hardened lake mud carved by the wind, steep and blunt at the
 * windward (east) end, tapering downwind (west), layered. All yardangs share the prevailing wind axis (X).
 */
public class YardangFeature extends Feature<YardangFeature.Config> {
    public YardangFeature() {
        super(Config.CODEC);
    }

    public record Config(BlockStateProvider body, BlockStateProvider band, IntProvider length, IntProvider height,
                         IntProvider width) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockStateProvider.CODEC.fieldOf("body").forGetter(Config::body),
                BlockStateProvider.CODEC.fieldOf("band").forGetter(Config::band),
                IntProvider.codec(4, 48).fieldOf("length").forGetter(Config::length),
                IntProvider.codec(2, 24).fieldOf("height").forGetter(Config::height),
                IntProvider.codec(1, 12).fieldOf("width").forGetter(Config::width)
        ).apply(i, Config::new));
    }

    @Override public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Config c = context.config();
        BlockPos origin = context.origin();
        int length = c.length().sample(random), height = c.height().sample(random), width = c.width().sample(random);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bandOffset = random.nextInt(3);
        for (int i = 0; i < length; i++) {
            double u = (double) i / length; // 0 windward head, 1 downwind tail
            // Blunt head, long tail: height falls off along the length, and the ridge narrows.
            double h = height * (u < 0.15 ? 0.85 + u : Math.pow(1 - (u - 0.15) / 0.85, 1.3));
            double w = width * (1 - 0.6 * u);
            int x = origin.getX() - i;
            for (int dz = -(int) Math.ceil(w); dz <= (int) Math.ceil(w); dz++) {
                double across = Math.abs(dz) / Math.max(1, w);
                if (across > 1) continue;
                int z = origin.getZ() + dz;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                // Rounded crest, steep flanks.
                int top = ground + (int) Math.round(h * Math.sqrt(1 - across * across));
                for (int y = ground - 2; y < top; y++) {
                    pos.set(x, y, z);
                    boolean band = Math.floorMod(y + bandOffset, 3) == 0;
                    level.setBlock(pos, (band ? c.band() : c.body()).getState(random, pos), 2);
                }
            }
        }
        return true;
    }
}
