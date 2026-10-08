package com.murimblock.integration.epicfight;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpicFightContentPolicyTest {
    @Test
    void onlyExactEngineNamespaceIsBlocked() {
        assertTrue(EpicFightContentPolicy.isNative(ResourceLocation.parse("epicfight:skillbook")));
        assertFalse(EpicFightContentPolicy.isNative(ResourceLocation.parse("murimblock:skillbook")));
        assertFalse(EpicFightContentPolicy.isNative(ResourceLocation.parse("addon:epicfight_book")));
        assertFalse(EpicFightContentPolicy.isNative(null));
    }

    @Test
    void nativeRecipesAndForeignRecipesWithNativeOutputsAreRetired() {
        assertTrue(blocked("epicfight:any", "{}"));
        assertTrue(blocked("addon:book", "{\"result\":{\"id\":\"epicfight:skillbook\",\"count\":1}}"));
        assertTrue(blocked("addon:book", "{\"result\":{\"item\":\"epicfight:skillbook\"}}"));
        assertTrue(blocked("addon:book", "{\"result\":\"epicfight:skillbook\"}"));
    }

    @Test
    void unrelatedOrCustomRecipeFormatsAreNotDestroyed() {
        for (String json : new String[]{"{}", "[]", "null", "{\"result\":5}", "{\"result\":[]}",
                "{\"result\":{\"id\":\"minecraft:iron_sword\"}}", "{\"result\":\"murimblock:book\"}",
                "{\"result\":\"Invalid ID\"}"}) assertFalse(blocked("addon:recipe", json));
    }

    private static boolean blocked(String id, String json) {
        return EpicFightContentPolicy.blockedRecipe(ResourceLocation.parse(id), JsonParser.parseString(json));
    }
}
