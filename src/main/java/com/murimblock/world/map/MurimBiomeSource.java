package com.murimblock.world.map;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murimblock.world.MurimBiomes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * The overworld's biomes on the Murim map (MurimMap; set in data/minecraft/worldgen/world_preset/normal.json):
 * vanilla's own choice everywhere ({@code vanilla}), except on the land of the bubble, where each block takes a Murim
 * biome of its region, picked by the same climate noise vanilla reads, so the patches follow the relief (the
 * mountainous biomes where the erosion is low). Caves, beaches and the sea stay vanilla's.
 * <ul>
 *   <li>centre: cloud-sea peaks on the mountains, the Wulingyuan stone forest where it is humid, the peach blossom valley
 *   elsewhere;</li>
 *   <li>north: the Kunlun glaciers on the immense ranges raised there (MurimMap.uplift) and the other mountains, the
 *   Tianshan snowfields elsewhere;</li>
 *   <li>west: the frontier desert where it is dry, the loess plateau elsewhere;</li>
 *   <li>south: the karst where it is humid, the camphor hill country on the gentler land, the bamboo sea on the hills;</li>
 *   <li>east: cloud-sea peaks on the highest mountains, the camphor hill country on its rolling hills (MurimMap.hills),
 *   the Jiangnan water country in the hollows between them.</li>
 * </ul>
 */
public final class MurimBiomeSource extends BiomeSource {
    public static final MapCodec<MurimBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("vanilla").forGetter(s -> s.vanilla),
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(s -> s.biomes)
    ).apply(i, MurimBiomeSource::new));

    /** Erosion below which the land is mountainous (vanilla's first two erosion slices), and the highest of it. */
    static final float MOUNTAINS = -0.375F, HIGHEST = -0.78F;
    /** Raised past its plateau by a third of the ranges' height or more, the north is the Kunlun's ranges. */
    static final double KUNLUN_UPLIFT = MurimMap.UPLIFT * (MurimMap.PLATEAU + 0.33 * (1 - MurimMap.PLATEAU));
    /** Above this share of their height, the east's hills are camphor country. */
    static final double EAST_HILLS = 0.32;
    /** Above this humidity, the centre's land below the mountains is the Wulingyuan stone forest. */
    static final float WULINGYUAN_HUMIDITY = 0.1F;

    private final BiomeSource vanilla;
    private final HolderSet<Biome> biomes;
    private final Map<ResourceKey<Biome>, Holder<Biome>> byKey = new HashMap<>();

    public MurimBiomeSource(BiomeSource vanilla, HolderSet<Biome> biomes) {
        this.vanilla = vanilla;
        this.biomes = biomes;
        biomes.forEach(h -> h.unwrapKey().ifPresent(k -> byKey.put(k, h)));
    }

    public BiomeSource vanilla() {
        return vanilla;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(vanilla.possibleBiomes().stream(), biomes.stream()).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> own = vanilla.getNoiseBiome(x, y, z, sampler);
        int bx = QuartPos.toBlock(x), bz = QuartPos.toBlock(z);
        if (!MurimMap.inBubble(bx, bz) || keepsVanilla(own)) return own;
        Climate.TargetPoint p = sampler.sample(x, y, z);
        MurimMap.Region region = MurimMap.region(bx, bz);
        // The north's raised ranges (MurimMap.uplift) are the Kunlun's glaciers, whatever vanilla's erosion says.
        ResourceKey<Biome> key = region == MurimMap.Region.NORTH && MurimMap.uplift(bx, bz) > KUNLUN_UPLIFT ? MurimBiomes.KUNLUN_GLACIERS
                // The east's hills are camphor country; the Jiangnan's water country lies in the hollows between them.
                : region == MurimMap.Region.EAST && MurimMap.hills(bx, bz) > EAST_HILLS
                        && Climate.unquantizeCoord(p.erosion()) >= HIGHEST ? MurimBiomes.CAMPHOR_HILLS
                : pick(region, Climate.unquantizeCoord(p.erosion()), Climate.unquantizeCoord(p.humidity()));
        Holder<Biome> murim = byKey.get(key);
        return murim != null ? murim : own;
    }

    /** What vanilla keeps inside the bubble: the caves underground, and the shore and sea at its edge. */
    private static boolean keepsVanilla(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || biome.is(Biomes.STONY_SHORE)
                || biome.is(Biomes.DRIPSTONE_CAVES) || biome.is(Biomes.LUSH_CAVES) || biome.is(Biomes.DEEP_DARK);
    }

    /** The Murim biome of a region for a given erosion (low: mountains) and humidity. */
    public static ResourceKey<Biome> pick(MurimMap.Region region, float erosion, float humidity) {
        return switch (region) {
            case CENTER -> erosion < MOUNTAINS ? MurimBiomes.CLOUD_SEA_PEAKS
                    : humidity > WULINGYUAN_HUMIDITY ? MurimBiomes.WULINGYUAN_PILLARS : MurimBiomes.PEACH_BLOSSOM_VALLEY;
            case NORTH -> erosion < MOUNTAINS ? MurimBiomes.KUNLUN_GLACIERS : MurimBiomes.TIANSHAN_SNOWFIELDS;
            case WEST -> humidity < -0.1F ? MurimBiomes.HEXI_FRONTIER_DESERT : MurimBiomes.LOESS_PLATEAU;
            // The south: the karst where it is humid, the plains of the camphor country on the gentler land, the bamboo
            // sea on the steeper hills.
            case SOUTH -> humidity > 0.15F ? MurimBiomes.JADE_KARST_FOREST
                    : erosion > -0.2F ? MurimBiomes.CAMPHOR_HILLS : MurimBiomes.SHU_BAMBOO_SEA;
            case EAST -> erosion < HIGHEST ? MurimBiomes.CLOUD_SEA_PEAKS : MurimBiomes.JIANGNAN_WATERLANDS;
        };
    }
}
