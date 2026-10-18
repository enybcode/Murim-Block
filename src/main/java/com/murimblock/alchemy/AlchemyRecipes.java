package com.murimblock.alchemy;

import com.murimblock.Murimblock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The pill furnace's recipes (docs/cultivation/alchemy.md): the ingredients in their order (principal, assistant,
 * harmonizer; the spring water always first), Murim herbs and Minecraft's own things alike; the heat bands of extraction,
 * stabilization and refining with how long each is held (ticks), the Qi the refining draws each tick, what comes out and
 * how many, and where it is learned: the Alchemy Manual (the common ones), the Treatise on Elixirs (the intermediate
 * ones) or a rare recipe scroll. Their order is their index (the screen, the known recipes' mask): append only. No
 * recipe's ingredients begin another's (the furnace starts as soon as a recipe is complete).
 */
public final class AlchemyRecipes {
    private AlchemyRecipes() {
    }

    public enum Role { PRINCIPAL, ASSISTANT, HARMONIZER }

    /** Where a recipe is learned. */
    public enum Source { MANUAL, TREATISE, SCROLL }

    /** An ingredient: a Murimblock item by its path, or a Minecraft one by its full id ("minecraft:feather"). */
    public record Ingredient(String item, Role role) {
        public ResourceLocation id() {
            return item.contains(":") ? ResourceLocation.parse(item) : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, item);
        }
        public Item get() {
            return BuiltInRegistries.ITEM.get(id());
        }
    }

    public record Band(int low, int high, int hold) {
        public boolean holds(int heat) { return heat >= low && heat <= high; }
    }

    public record Recipe(int index, String id, String result, int count, List<Ingredient> herbs, Band extract, Band stabilize, Band refine,
                         double qiPerTick, Source source) {
        public Item resultItem() {
            return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, result));
        }
        public Band band(int phase) {
            return phase == PillFurnaceBlockEntity.EXTRACT ? extract : phase == PillFurnaceBlockEntity.STABILIZE ? stabilize : refine;
        }
        public String nameKey() { return "alchemy.murimblock.recipe." + id; }
        public boolean inManual() { return source == Source.MANUAL; }
    }

    private static Ingredient p(String item) { return new Ingredient(item, Role.PRINCIPAL); }
    private static Ingredient a(String item) { return new Ingredient(item, Role.ASSISTANT); }
    private static Ingredient h(String item) { return new Ingredient(item, Role.HARMONIZER); }
    private static Band b(int low, int high, int hold) { return new Band(low, high, hold); }

    private static final Source M = Source.MANUAL, T = Source.TREATISE, S = Source.SCROLL;

    public static final List<Recipe> ALL = List.of(
        new Recipe(0, "qi_gathering_pill", "qi_gathering_pill", 2, List.of(p("lotus_seed_pod"), a("sea_buckthorn_berries"), h("chrysanthemum_flower")),
                b(600, 700, 120), b(400, 480, 80), b(520, 590, 100), 0.3, M),
        new Recipe(1, "realm_breaking_pill", "realm_breaking_pill", 1, List.of(p("cistanche_stem"), a("rhododendron_blossom"), h("lotus_seed_pod")),
                b(680, 760, 140), b(440, 500, 100), b(600, 650, 120), 0.6, M),
        new Recipe(2, "healing_decoction", "healing_decoction", 2, List.of(p("sea_buckthorn_berries"), a("astragalus_root")),
                b(450, 560, 80), b(300, 400, 60), b(380, 460, 60), 0.15, M),
        new Recipe(3, "qi_decoction", "qi_decoction", 2, List.of(p("goji_berries"), a("fresh_bamboo_shoot")),
                b(480, 580, 90), b(320, 420, 60), b(420, 500, 70), 0.2, M),
        new Recipe(4, "purifying_brew", "purifying_brew", 1, List.of(p("fern_frond"), h("chrysanthemum_flower")),
                b(420, 520, 90), b(300, 380, 70), b(360, 430, 80), 0.25, M),
        new Recipe(5, "martial_elixir", "martial_elixir", 1, List.of(p("ginseng_root"), a("kunlun_sedge_bundle"), h("goji_berries")),
                b(700, 790, 140), b(460, 520, 100), b(620, 670, 120), 0.8, S),
        new Recipe(6, "soul_soothing_pill", "soul_soothing_pill", 1, List.of(p("lingzhi_cap"), a("snow_lotus_flower"), h("chrysanthemum_flower")),
                b(720, 800, 160), b(480, 540, 120), b(640, 690, 140), 1.0, S),

        // The Alchemy Manual's elixirs: a passive for a few minutes, wide bands, little Qi.
        new Recipe(7, "swift_wind_pill", "swift_wind_pill", 2, List.of(p("fresh_bamboo_shoot"), a("minecraft:sugar"), h("minecraft:feather")),
                b(500, 610, 80), b(340, 440, 60), b(420, 520, 60), 0.15, M),
        new Recipe(8, "night_eye_pill", "night_eye_pill", 2, List.of(p("minecraft:golden_carrot"), a("goji_berries")),
                b(480, 590, 80), b(320, 420, 60), b(400, 500, 60), 0.15, M),
        new Recipe(9, "fish_breath_pill", "fish_breath_pill", 2, List.of(p("minecraft:pufferfish"), a("minecraft:kelp"), h("fern_frond")),
                b(460, 570, 80), b(300, 400, 60), b(380, 480, 60), 0.15, M),
        new Recipe(10, "fire_ward_pill", "fire_ward_pill", 2, List.of(p("minecraft:magma_cream"), a("rhododendron_blossom")),
                b(620, 720, 90), b(400, 490, 60), b(500, 590, 70), 0.2, M),
        new Recipe(11, "feather_fall_pill", "feather_fall_pill", 2, List.of(p("minecraft:phantom_membrane"), a("minecraft:feather")),
                b(440, 550, 80), b(300, 390, 60), b(370, 460, 60), 0.15, M),
        new Recipe(12, "iron_skin_pill", "iron_skin_pill", 2, List.of(p("minecraft:iron_ingot"), a("astragalus_root"), h("chrysanthemum_flower")),
                b(640, 740, 100), b(420, 500, 70), b(520, 600, 80), 0.25, M),
        new Recipe(13, "qi_flow_pill", "qi_flow_pill", 2, List.of(p("lotus_seed_pod"), a("minecraft:glowstone_dust")),
                b(560, 660, 100), b(380, 460, 70), b(480, 560, 80), 0.3, M),
        new Recipe(14, "clear_mind_decoction", "clear_mind_decoction", 1, List.of(p("minecraft:honey_bottle"), h("chrysanthemum_flower")),
                b(400, 500, 80), b(280, 370, 60), b(340, 420, 60), 0.2, M),
        new Recipe(15, "leaping_toad_pill", "leaping_toad_pill", 2, List.of(p("minecraft:rabbit_foot"), a("fresh_bamboo_shoot")),
                b(500, 600, 80), b(330, 430, 60), b(410, 500, 60), 0.15, M),
        new Recipe(16, "stoneheart_pill", "stoneheart_pill", 2, List.of(p("minecraft:turtle_scute"), a("astragalus_root")),
                b(620, 720, 100), b(420, 500, 70), b(520, 600, 80), 0.25, M),

        // The Treatise on Elixirs: stronger passives, narrower bands, more Qi.
        new Recipe(17, "tiger_might_pill", "tiger_might_pill", 1, List.of(p("tiger_claw"), a("ginseng_root"), h("minecraft:blaze_powder")),
                b(660, 750, 120), b(440, 510, 90), b(560, 630, 100), 0.45, T),
        new Recipe(18, "thorn_body_pill", "thorn_body_pill", 1, List.of(p("minecraft:cactus"), a("minecraft:prismarine_shard"), h("rhododendron_blossom")),
                b(600, 690, 120), b(400, 470, 90), b(520, 590, 100), 0.4, T),
        new Recipe(19, "blood_vigor_pill", "blood_vigor_pill", 1, List.of(p("minecraft:glistering_melon_slice"), a("goji_berries"), h("minecraft:redstone")),
                b(620, 710, 120), b(420, 490, 90), b(540, 610, 100), 0.45, T),
        new Recipe(20, "keen_senses_pill", "keen_senses_pill", 1, List.of(p("minecraft:amethyst_shard"), a("minecraft:spider_eye"), h("chrysanthemum_flower")),
                b(560, 650, 110), b(380, 450, 80), b(480, 550, 90), 0.35, T),
        new Recipe(21, "golden_body_pill", "golden_body_pill", 1, List.of(p("minecraft:golden_apple"), a("ginseng_root"), h("lotus_seed_pod")),
                b(680, 770, 130), b(450, 520, 100), b(580, 650, 110), 0.5, T),
        new Recipe(22, "deep_sea_elixir", "deep_sea_elixir", 1, List.of(p("minecraft:nautilus_shell"), a("minecraft:prismarine_crystals"), h("lotus_seed_pod")),
                b(540, 630, 120), b(360, 430, 90), b(460, 530, 100), 0.4, T),
        new Recipe(23, "miners_vigor_pill", "miners_vigor_pill", 2, List.of(p("minecraft:lapis_lazuli"), a("minecraft:redstone"), h("fresh_bamboo_shoot")),
                b(580, 670, 110), b(390, 460, 80), b(500, 570, 90), 0.35, T),
        new Recipe(24, "great_qi_flow_pill", "great_qi_flow_pill", 1, List.of(p("lingzhi_cap"), a("minecraft:glowstone_dust"), h("lotus_seed_pod")),
                b(680, 760, 130), b(450, 515, 100), b(590, 650, 110), 0.6, T),
        new Recipe(25, "fortune_pill", "fortune_pill", 1, List.of(p("minecraft:emerald"), a("minecraft:gold_nugget"), h("goji_berries")),
                b(600, 690, 120), b(400, 470, 90), b(520, 590, 100), 0.4, T),
        new Recipe(26, "shadow_step_pill", "shadow_step_pill", 1, List.of(p("minecraft:ender_pearl"), a("minecraft:fermented_spider_eye"), h("fern_frond")),
                b(640, 720, 120), b(420, 485, 90), b(550, 615, 100), 0.5, T),

        // The rare scrolls: the strongest passives, narrow bands, much Qi, much toxicity.
        new Recipe(27, "diamond_body_pill", "diamond_body_pill", 1, List.of(p("minecraft:diamond"), a("minecraft:obsidian"), h("kunlun_sedge_bundle")),
                b(740, 810, 160), b(490, 545, 120), b(650, 700, 140), 0.9, S),
        new Recipe(28, "phoenix_rebirth_pill", "phoenix_rebirth_pill", 1, List.of(p("snow_lotus_flower"), a("minecraft:blaze_rod"), h("minecraft:glistering_melon_slice")),
                b(750, 820, 160), b(500, 555, 120), b(660, 710, 140), 1.0, S),
        new Recipe(29, "heavenly_tiger_pill", "heavenly_tiger_pill", 1, List.of(p("ginseng_root"), a("minecraft:blaze_rod"), h("minecraft:gunpowder")),
                b(740, 810, 160), b(490, 545, 120), b(650, 700, 140), 0.9, S),
        new Recipe(30, "dragon_breath_elixir", "dragon_breath_elixir", 1, List.of(p("minecraft:dragon_breath"), a("snow_lotus_flower"), h("lingzhi_cap")),
                b(750, 820, 170), b(500, 560, 130), b(670, 720, 150), 1.1, S)
    );

    private static Set<ResourceLocation> ingredients;

    public static Recipe byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : null;
    }

    public static Recipe byId(String id) {
        for (Recipe r : ALL) if (r.id().equals(id)) return r;
        return null;
    }

    /** The recipes learned from this source. */
    public static List<String> from(Source source) {
        return ALL.stream().filter(r -> r.source() == source).map(Recipe::id).toList();
    }

    /** Whether some recipe uses this item (what the furnace's ingredient slots take). */
    public static boolean isIngredient(Item item) {
        if (ingredients == null) {
            Set<ResourceLocation> all = new HashSet<>();
            for (Recipe r : ALL) for (Ingredient i : r.herbs()) all.add(i.id());
            ingredients = all;
        }
        return ingredients.contains(BuiltInRegistries.ITEM.getKey(item));
    }

    /** The recipes whose ingredients begin with these, in order. */
    public static List<Recipe> startingWith(List<Item> herbs) {
        return ALL.stream().filter(r -> {
            if (herbs.size() > r.herbs().size()) return false;
            for (int i = 0; i < herbs.size(); i++) if (r.herbs().get(i).get() != herbs.get(i)) return false;
            return true;
        }).toList();
    }

    /** The recipe these ingredients make exactly, or null. */
    public static Recipe made(List<Item> herbs) {
        for (Recipe r : startingWith(herbs)) if (r.herbs().size() == herbs.size()) return r;
        return null;
    }

    /** Ids for the heat bands' and herbs' item names, for tests. */
    public static Supplier<Item> item(String id) {
        return () -> BuiltInRegistries.ITEM.get(id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, id));
    }
}
