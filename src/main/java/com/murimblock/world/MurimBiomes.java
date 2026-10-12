package com.murimblock.world;

import com.murimblock.Murimblock;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * The seven Murim biomes, inspired by the landscapes of feudal China (docs/biomes). Three large ones (karst, loess,
 * frontier desert) and four medium ones. Their JSON lives in data/murimblock/worldgen/biome.
 */
public final class MurimBiomes {
    /** Guilin–Yangshuo tower karst along the Li River. Large. */
    public static final ResourceKey<Biome> JADE_KARST_FOREST = key("jade_karst_forest");
    /** Loess Plateau of the Yellow River: yellow tablelands cut by gullies. Large. */
    public static final ResourceKey<Biome> LOESS_PLATEAU = key("loess_plateau");
    /** Hexi Corridor and Gobi: varnished gravel plains, yardangs, poplar oases. Large. */
    public static final ResourceKey<Biome> HEXI_FRONTIER_DESERT = key("hexi_frontier_desert");
    /** Huangshan: pale granite spires above a sea of clouds, cliff pines. Medium. */
    public static final ResourceKey<Biome> CLOUD_SEA_PEAKS = key("cloud_sea_peaks");
    /** Sichuan bamboo sea and panda mountains. Medium. */
    public static final ResourceKey<Biome> SHU_BAMBOO_SEA = key("shu_bamboo_sea");
    /** Jiangnan water country: ponds, lotus, willows. Medium. */
    public static final ResourceKey<Biome> JIANGNAN_WATERLANDS = key("jiangnan_waterlands");
    /** Peach Blossom Spring hidden among Wulingyuan sandstone pillars. Medium. */
    public static final ResourceKey<Biome> PEACH_BLOSSOM_VALLEY = key("peach_blossom_valley");

    public static final List<ResourceKey<Biome>> ALL = List.of(JADE_KARST_FOREST, LOESS_PLATEAU, HEXI_FRONTIER_DESERT,
            CLOUD_SEA_PEAKS, SHU_BAMBOO_SEA, JIANGNAN_WATERLANDS, PEACH_BLOSSOM_VALLEY);

    private MurimBiomes() { }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }
}
