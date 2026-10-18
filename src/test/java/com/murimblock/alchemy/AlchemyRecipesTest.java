package com.murimblock.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The furnace's recipes hold together: indices in order, no recipe the beginning of another, every elixir brewed and named. */
class AlchemyRecipesTest {
    private static final String RES = "/assets/murimblock/";

    private boolean exists(String path) {
        return getClass().getResource(RES + path) != null;
    }

    private static List<String> ingredients(AlchemyRecipes.Recipe r) {
        return r.herbs().stream().map(i -> i.item()).toList();
    }

    @Test
    void indicesFollowTheListAndIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < AlchemyRecipes.ALL.size(); i++) {
            AlchemyRecipes.Recipe r = AlchemyRecipes.ALL.get(i);
            assertEquals(i, r.index(), r.id());
            assertTrue(ids.add(r.id()), "twice: " + r.id());
        }
        assertTrue(AlchemyRecipes.ALL.size() >= 31, "recipes: " + AlchemyRecipes.ALL.size());
    }

    @Test
    void noRecipeBeginsAnother() {
        for (AlchemyRecipes.Recipe a : AlchemyRecipes.ALL)
            for (AlchemyRecipes.Recipe b : AlchemyRecipes.ALL) {
                if (a == b) continue;
                List<String> x = ingredients(a), y = ingredients(b);
                assertFalse(x.size() <= y.size() && y.subList(0, x.size()).equals(x), a.id() + " begins " + b.id());
            }
    }

    @Test
    void theBandsAreInOrderAndStabilizingIsCooler() {
        for (AlchemyRecipes.Recipe r : AlchemyRecipes.ALL) {
            for (AlchemyRecipes.Band b : List.of(r.extract(), r.stabilize(), r.refine())) assertTrue(b.low() < b.high() && b.hold() > 0, r.id());
            assertTrue(r.stabilize().high() < r.extract().low() && r.extract().high() < PillFurnaceBlockEntity.BLAST - PillFurnaceBlockEntity.SCORCH_MARGIN, r.id());
        }
    }

    @Test
    void everyElixirIsBrewedNamedAndDrawn() throws Exception {
        JsonObject lang;
        try (var in = new InputStreamReader(getClass().getResourceAsStream(RES + "lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(in).getAsJsonObject();
        }
        for (AlchemyElixirs.Elixir e : AlchemyElixirs.ALL) {
            assertNotNull(AlchemyRecipes.byId(e.id()), "no recipe for " + e.id());
            assertTrue(lang.has("item.murimblock." + e.id()) && lang.has("alchemy.murimblock.passive." + e.id()), "unnamed " + e.id());
            assertTrue(exists("textures/item/alchemy/" + e.id() + ".png"), "undrawn " + e.id());
            for (AlchemyElixirs.Effect x : e.effects())
                if (x.id().startsWith("murimblock:")) {
                    String id = x.id().substring("murimblock:".length());
                    assertTrue(lang.has("effect.murimblock." + id) && exists("textures/mob_effect/" + id + ".png"), "effect " + id);
                }
        }
        for (AlchemyRecipes.Recipe r : AlchemyRecipes.ALL) assertTrue(lang.has(r.nameKey()), "unnamed recipe " + r.id());
    }
}
