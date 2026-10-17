package com.murimblock.world;

import com.murimblock.Murimblock;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * The Murim biomes, inspired by the landscapes of feudal China (docs/biomes): seven of the first slice, and the two
 * snows of the north (14/10/2026). Where each grows is the Murim map's (world/map). Their JSON lives in
 * data/murimblock/worldgen/biome.
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
    /** Tianshan: snowfields and frozen meadows under the Heavenly Mountains, dark pines. North, lowlands. */
    public static final ResourceKey<Biome> TIANSHAN_SNOWFIELDS = key("tianshan_snowfields");
    /** Kunlun: glaciers and ice-blue rock, the roof of the world. North, mountains. */
    public static final ResourceKey<Biome> KUNLUN_GLACIERS = key("kunlun_glaciers");
    /** Camphor hill country (Jiangxi, Hunan, Huizhou): rolling hills, camphor woods with clean edges, milk vetch. South and east. */
    public static final ResourceKey<Biome> CAMPHOR_HILLS = key("camphor_hills");
    /** Wulingyuan (Zhangjiajie): a forest of quartz-sandstone pillars 60 to 110 blocks tall, green-topped, misty gorges. Centre. */
    public static final ResourceKey<Biome> WULINGYUAN_PILLARS = key("wulingyuan_pillars");

    public static final List<ResourceKey<Biome>> ALL = List.of(JADE_KARST_FOREST, LOESS_PLATEAU, HEXI_FRONTIER_DESERT,
            CLOUD_SEA_PEAKS, SHU_BAMBOO_SEA, JIANGNAN_WATERLANDS, PEACH_BLOSSOM_VALLEY, TIANSHAN_SNOWFIELDS, KUNLUN_GLACIERS, CAMPHOR_HILLS,
            WULINGYUAN_PILLARS);

    /** The Murim biomes as a tag (data/murimblock/tags/worldgen/biome/murim_biomes.json). */
    public static final net.minecraft.tags.TagKey<Biome> MURIM_TAG = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.murimblock.Murimblock.MOD_ID, "murim_biomes"));

    private MurimBiomes() { }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }
}
