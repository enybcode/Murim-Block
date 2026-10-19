package com.murimblock.world.plant;

import java.util.List;

/**
 * The Murim's plants (docs/cultivation/plants.md). Written by scripts/plants/plants-data.cjs: do not edit by hand.
 * The order is saved in worlds (a crop's species is its index): new species go at the end, none is ever moved.
 */
public final class PlantCatalog {
    private PlantCatalog() {
    }

    public enum Kind { HERB, FLOWER }
    public enum Rarity { COMMON, UNCOMMON, RARE, VERY_RARE }
    /** What it roots in: as the wild plants (MurimPlantBlock.Soil), and WATER: floating on still water. */
    public enum Soil { GRASS, SAND, WET, ALPINE, WATER }
    /** The warmth it wants: COLD (the snows), HOT (the dry lands), MILD (the rest), ANY. */
    public enum Climate { COLD, MILD, HOT, ANY }
    /** Harvested, it stays (flowers, berries, leaves: it grows them back) or it is pulled up whole (roots, shoots). */
    public enum Harvest { KEEP, UPROOT }

    /**
     * A species: its index (saved), its models' name, its wild block (an existing block or one MurimFlora makes), its herb
     * item (herbs only) and what it grows from, its kind, rarity, soil, climate and harvest, its growth stages modelled (4,
     * or 1 grown by scale), and its colours.
     */
    public record Species(int index, String id, String wild, boolean existingWild, String herb, String seed, Kind kind, Rarity rarity,
                          Soil soil, Climate climate, Harvest harvest, int stagesModelled, List<String> colours) {
        public boolean isHerb() { return kind == Kind.HERB; }
    }

    public static final List<Species> ALL = List.of(
        new Species(0, "chrysanthemum", "chrysanthemum", true, "chrysanthemum_flower", "chrysanthemum_seeds", Kind.HERB, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 4, List.of("gold", "white", "purple")),
        new Species(1, "bamboo_shoot", "bamboo_shoot", true, "fresh_bamboo_shoot", "bamboo_shoot_rhizome", Kind.HERB, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.UPROOT, 1, List.of("default")),
        new Species(2, "astragalus_root", "milk_vetch", true, "astragalus_root", "milk_vetch_seeds", Kind.HERB, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.UPROOT, 1, List.of("default")),
        new Species(3, "kunlun_sedge", "kunlun_sedge", true, "kunlun_sedge_bundle", "kunlun_sedge_seeds", Kind.HERB, Rarity.COMMON, Soil.ALPINE, Climate.COLD, Harvest.KEEP, 1, List.of("default")),
        new Species(4, "fern_frond", "karst_fern", true, "fern_frond", "karst_fern_spores", Kind.HERB, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(5, "sea_buckthorn_berries", "sea_buckthorn", true, "sea_buckthorn_berries", "sea_buckthorn_cutting", Kind.HERB, Rarity.UNCOMMON, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(6, "lotus_seed_pod", "lotus_blossom", true, "lotus_seed_pod", "lotus_blossom_seeds", Kind.HERB, Rarity.UNCOMMON, Soil.WATER, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(7, "cistanche", "cistanche", true, "cistanche_stem", "cistanche_seeds", Kind.HERB, Rarity.RARE, Soil.SAND, Climate.HOT, Harvest.UPROOT, 1, List.of("default")),
        new Species(8, "rhododendron_blossom", "rhododendron", true, "rhododendron_blossom", "rhododendron_cutting", Kind.HERB, Rarity.RARE, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(9, "snow_lotus", "snow_lotus", true, "snow_lotus_flower", "snow_lotus_seeds", Kind.HERB, Rarity.RARE, Soil.ALPINE, Climate.COLD, Harvest.UPROOT, 1, List.of("default")),
        new Species(10, "ginseng_root", "ginseng", false, "ginseng_root", "ginseng_seeds", Kind.HERB, Rarity.RARE, Soil.GRASS, Climate.MILD, Harvest.UPROOT, 1, List.of("default")),
        new Species(11, "lingzhi", "lingzhi", false, "lingzhi_cap", "lingzhi_spores", Kind.HERB, Rarity.VERY_RARE, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(12, "goji_berries", "goji_bush", false, "goji_berries", "goji_bush_seeds", Kind.HERB, Rarity.COMMON, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(13, "peony", "peony", false, null, "peony_seeds", Kind.FLOWER, Rarity.UNCOMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(14, "cymbidium_orchid", "cymbidium_orchid", false, null, "cymbidium_orchid_bulb", Kind.FLOWER, Rarity.RARE, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(15, "camellia", "camellia", false, null, "camellia_cutting", Kind.FLOWER, Rarity.UNCOMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(16, "balloon_flower", "balloon_flower", false, null, "balloon_flower_seeds", Kind.FLOWER, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(17, "red_spider_lily", "red_spider_lily", false, null, "red_spider_lily_bulb", Kind.FLOWER, Rarity.UNCOMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(18, "white_lily", "white_lily", false, null, "white_lily_bulb", Kind.FLOWER, Rarity.UNCOMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(19, "blue_poppy", "blue_poppy", false, null, "blue_poppy_seeds", Kind.FLOWER, Rarity.RARE, Soil.ALPINE, Climate.COLD, Harvest.KEEP, 1, List.of("default")),
        new Species(20, "edelweiss", "edelweiss", false, null, "edelweiss_seeds", Kind.FLOWER, Rarity.UNCOMMON, Soil.ALPINE, Climate.COLD, Harvest.KEEP, 1, List.of("default")),
        new Species(21, "gentian", "gentian", false, null, "gentian_seeds", Kind.FLOWER, Rarity.COMMON, Soil.ALPINE, Climate.COLD, Harvest.KEEP, 1, List.of("default")),
        new Species(22, "alpine_forget_me_not", "alpine_forget_me_not", false, null, "alpine_forget_me_not_seeds", Kind.FLOWER, Rarity.COMMON, Soil.ALPINE, Climate.COLD, Harvest.KEEP, 1, List.of("default")),
        new Species(23, "snow_saxifrage", "snow_saxifrage", false, null, "snow_saxifrage_seeds", Kind.FLOWER, Rarity.COMMON, Soil.ALPINE, Climate.COLD, Harvest.KEEP, 1, List.of("default")),
        new Species(24, "tianshan_tulip", "tianshan_tulip", false, null, "tianshan_tulip_bulb", Kind.FLOWER, Rarity.UNCOMMON, Soil.ALPINE, Climate.COLD, Harvest.KEEP, 1, List.of("default")),
        new Species(25, "desert_candle", "desert_candle", false, null, "desert_candle_rhizome", Kind.FLOWER, Rarity.RARE, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(26, "desert_iris", "desert_iris", false, null, "desert_iris_rhizome", Kind.FLOWER, Rarity.UNCOMMON, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(27, "chinese_pink", "chinese_pink", false, null, "chinese_pink_seeds", Kind.FLOWER, Rarity.COMMON, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(28, "field_poppy", "field_poppy", false, null, "field_poppy_seeds", Kind.FLOWER, Rarity.COMMON, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(29, "caper_flower", "caper_flower", false, null, "caper_flower_seeds", Kind.FLOWER, Rarity.UNCOMMON, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(30, "harmal", "harmal", false, null, "harmal_seeds", Kind.FLOWER, Rarity.COMMON, Soil.SAND, Climate.HOT, Harvest.KEEP, 1, List.of("default")),
        new Species(31, "hydrangea", "hydrangea", false, null, "hydrangea_cutting", Kind.FLOWER, Rarity.UNCOMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(32, "gardenia", "gardenia", false, null, "gardenia_cutting", Kind.FLOWER, Rarity.UNCOMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(33, "hibiscus", "hibiscus", false, null, "hibiscus_cutting", Kind.FLOWER, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(34, "garden_balsam", "garden_balsam", false, null, "garden_balsam_seeds", Kind.FLOWER, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(35, "ginger_lily", "ginger_lily", false, null, "ginger_lily_rhizome", Kind.FLOWER, Rarity.UNCOMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(36, "blue_flag_iris", "blue_flag_iris", false, null, "blue_flag_iris_rhizome", Kind.FLOWER, Rarity.COMMON, Soil.GRASS, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(37, "water_lily", "water_lily", false, null, "water_lily_rhizome", Kind.FLOWER, Rarity.UNCOMMON, Soil.WATER, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(38, "morning_glory", "morning_glory", false, null, "morning_glory_seeds", Kind.FLOWER, Rarity.COMMON, Soil.WET, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(39, "daylily", "daylily", false, null, "daylily_rhizome", Kind.FLOWER, Rarity.COMMON, Soil.WET, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(40, "jasmine", "jasmine", false, null, "jasmine_cutting", Kind.FLOWER, Rarity.COMMON, Soil.WET, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(41, "ladybells", "ladybells", false, null, "ladybells_seeds", Kind.FLOWER, Rarity.UNCOMMON, Soil.WET, Climate.MILD, Harvest.KEEP, 1, List.of("default")),
        new Species(42, "udumbara", "udumbara", false, null, "udumbara_seeds", Kind.FLOWER, Rarity.VERY_RARE, Soil.GRASS, Climate.ANY, Harvest.KEEP, 1, List.of("default"))
    );

    public static Species byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : ALL.get(0);
    }

    public static Species byId(String id) {
        for (Species s : ALL) if (s.id().equals(id)) return s;
        return null;
    }
}
