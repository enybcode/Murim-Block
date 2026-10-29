package com.murimblock.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * The overworld's biome source on the Murim map ({@code murimblock:murim_map}): each column gets the biome of its
 * zone from {@link MurimMap}. The {@code fallback} source (vanilla's overworld) still gives the caves and the sea.
 */
public final class MurimBiomeSource extends BiomeSource {
    public static final MapCodec<MurimBiomeSource> CODEC = RecordCodecBuilder.<MurimBiomeSource>mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Biome.CODEC).fieldOf("biomes").forGetter(s -> s.byKey),
            BiomeSource.CODEC.fieldOf("fallback").forGetter(s -> s.fallback)).apply(i, MurimBiomeSource::new))
            .validate(MurimBiomeSource::complete);

    private final Map<String, Holder<Biome>> byKey;
    private final BiomeSource fallback;
    private final Holder<Biome>[] biomes;
    /** The last column asked for, per thread: a column is asked once for every 4 blocks of height. */
    private final ThreadLocal<long[]> last = ThreadLocal.withInitial(() -> new long[] {Long.MIN_VALUE, 0});

    @SuppressWarnings("unchecked")
    private MurimBiomeSource(Map<String, Holder<Biome>> byKey, BiomeSource fallback) {
        this.byKey = byKey;
        this.fallback = fallback;
        this.biomes = new Holder[MurimBiome.values().length];
        for (MurimBiome biome : MurimBiome.values()) {
            biomes[biome.ordinal()] = byKey.get(biome.key);
        }
    }

    private static DataResult<MurimBiomeSource> complete(MurimBiomeSource source) {
        for (MurimBiome biome : MurimBiome.values()) {
            if (source.biomes[biome.ordinal()] == null) {
                return DataResult.error(() -> "murim_map is missing the biome '" + biome.key + "'");
            }
        }
        return DataResult.success(source);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(byKey.values().stream(), fallback.possibleBiomes().stream()).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        MurimBiome biome = column(x, z);
        Holder<Biome> vanilla = fallback.getNoiseBiome(x, y, z, sampler);
        if (biome == MurimBiome.OCEAN) {
            return vanilla.is(BiomeTags.IS_OCEAN) ? vanilla : biomes[MurimBiome.OCEAN.ordinal()];
        }
        if (vanilla.is(Biomes.LUSH_CAVES) || vanilla.is(Biomes.DRIPSTONE_CAVES) || vanilla.is(Biomes.DEEP_DARK)) {
            return vanilla;
        }
        return biomes[biome.ordinal()];
    }

    private MurimBiome column(int quartX, int quartZ) {
        long key = (long) quartX << 32 | quartZ & 0xffffffffL;
        long[] cache = last.get();
        if (cache[0] != key) {
            cache[0] = key;
            cache[1] = MurimMap.biome(QuartPos.toBlock(quartX) + 2, QuartPos.toBlock(quartZ) + 2).ordinal();
        }
        return MurimBiome.values()[(int) cache[1]];
    }
}
