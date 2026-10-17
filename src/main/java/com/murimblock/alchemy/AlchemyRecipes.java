package com.murimblock.alchemy;

import com.murimblock.Murimblock;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The pill furnace's recipes (docs/cultivation/alchemy.md): the herbs in their order (principal, assistant, harmonizer;
 * the spring water always first), the heat bands of extraction, stabilization and refining with how long each is held
 * (ticks), the Qi the refining draws each tick, what comes out and how many, and whether the Alchemy Manual teaches it
 * (the others are scrolls to find). Their order is their index on the furnace's screen: append only.
 */
public final class AlchemyRecipes {
    private AlchemyRecipes() {
    }

    public enum Role { PRINCIPAL, ASSISTANT, HARMONIZER }

    public record Ingredient(String item, Role role) {
        public Item get() {
            return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, item));
        }
    }

    public record Band(int low, int high, int hold) {
        public boolean holds(int heat) { return heat >= low && heat <= high; }
    }

    public record Recipe(int index, String id, String result, int count, List<Ingredient> herbs, Band extract, Band stabilize, Band refine,
                         double qiPerTick, boolean inManual) {
        public Item resultItem() {
            return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, result));
        }
        public Band band(int phase) {
            return phase == PillFurnaceBlockEntity.EXTRACT ? extract : phase == PillFurnaceBlockEntity.STABILIZE ? stabilize : refine;
        }
        public String nameKey() { return "alchemy.murimblock.recipe." + id; }
    }

    private static Ingredient p(String item) { return new Ingredient(item, Role.PRINCIPAL); }
    private static Ingredient a(String item) { return new Ingredient(item, Role.ASSISTANT); }
    private static Ingredient h(String item) { return new Ingredient(item, Role.HARMONIZER); }

    public static final List<Recipe> ALL = List.of(
        new Recipe(0, "qi_gathering_pill", "qi_gathering_pill", 2, List.of(p("lotus_seed_pod"), a("sea_buckthorn_berries"), h("chrysanthemum_flower")),
                new Band(600, 700, 120), new Band(400, 480, 80), new Band(520, 590, 100), 0.3, true),
        new Recipe(1, "realm_breaking_pill", "realm_breaking_pill", 1, List.of(p("cistanche_stem"), a("rhododendron_blossom"), h("lotus_seed_pod")),
                new Band(680, 760, 140), new Band(440, 500, 100), new Band(600, 650, 120), 0.6, true),
        new Recipe(2, "healing_decoction", "healing_decoction", 2, List.of(p("sea_buckthorn_berries"), a("astragalus_root")),
                new Band(450, 560, 80), new Band(300, 400, 60), new Band(380, 460, 60), 0.15, true),
        new Recipe(3, "qi_decoction", "qi_decoction", 2, List.of(p("goji_berries"), a("fresh_bamboo_shoot")),
                new Band(480, 580, 90), new Band(320, 420, 60), new Band(420, 500, 70), 0.2, true),
        new Recipe(4, "purifying_brew", "purifying_brew", 1, List.of(p("fern_frond"), h("chrysanthemum_flower")),
                new Band(420, 520, 90), new Band(300, 380, 70), new Band(360, 430, 80), 0.25, true),
        new Recipe(5, "martial_elixir", "martial_elixir", 1, List.of(p("ginseng_root"), a("kunlun_sedge_bundle"), h("goji_berries")),
                new Band(700, 790, 140), new Band(460, 520, 100), new Band(620, 670, 120), 0.8, false),
        new Recipe(6, "soul_soothing_pill", "soul_soothing_pill", 1, List.of(p("lingzhi_cap"), a("snow_lotus_flower"), h("chrysanthemum_flower")),
                new Band(720, 800, 160), new Band(480, 540, 120), new Band(640, 690, 140), 1.0, false)
    );

    public static Recipe byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : null;
    }

    public static Recipe byId(String id) {
        for (Recipe r : ALL) if (r.id().equals(id)) return r;
        return null;
    }

    /** The recipes whose herbs begin with these, in order. */
    public static List<Recipe> startingWith(List<Item> herbs) {
        return ALL.stream().filter(r -> {
            if (herbs.size() > r.herbs().size()) return false;
            for (int i = 0; i < herbs.size(); i++) if (r.herbs().get(i).get() != herbs.get(i)) return false;
            return true;
        }).toList();
    }

    /** The recipe these herbs make exactly, or null. */
    public static Recipe made(List<Item> herbs) {
        for (Recipe r : startingWith(herbs)) if (r.herbs().size() == herbs.size()) return r;
        return null;
    }

    /** Ids for the heat bands' and herbs' item names, for tests. */
    public static Supplier<Item> item(String id) {
        return () -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, id));
    }
}
