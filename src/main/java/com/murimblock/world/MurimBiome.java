package com.murimblock.world;

/** The biomes of the Murim map, by zone (docs/world/map.md). The last two are vanilla. */
public enum MurimBiome {
    FROZEN_TUNDRA("frozen_tundra"),
    COLD_DESERT("cold_desert"),
    FROZEN_STEPPE("frozen_steppe"),
    QINGHAI_PLATEAU("qinghai_plateau"),
    WESTERN_SNOW_PEAKS("western_snow_peaks"),
    LOESS_PLATEAU("loess_plateau"),
    CENTRAL_PLAINS("central_plains"),
    SHANDONG_HILLS("shandong_hills"),
    SACRED_MOUNTAIN("sacred_mountain"),
    RED_BASIN("red_basin"),
    BAMBOO_MOUNTAINS("bamboo_mountains"),
    HUBEI_WATERLANDS("hubei_waterlands"),
    RIVER_DELTA("river_delta"),
    STONE_FOREST("stone_forest"),
    WESTERN_GORGES("western_gorges"),
    UNNAM_JUNGLE("unnam_jungle"),
    STONE_PEAKS("stone_peaks"),
    RICE_TERRACES("rice_terraces"),
    SUBTROPICAL_FOREST("subtropical_forest"),
    SOUTHEAST_COAST("southeast_coast"),
    BEACH("beach"),
    OCEAN("ocean");

    /** Key in the biome source's {@code biomes} map; for our own biomes also the biome id path. */
    public final String key;

    MurimBiome(String key) {
        this.key = key;
    }

    /** True for the biomes this mod defines, false for the vanilla beach and ocean. */
    public boolean ours() {
        return ordinal() < BEACH.ordinal();
    }
}
