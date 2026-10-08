package com.murimblock.client.hud;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpicFightHudTest {
    @Test
    void cancelsOnlyTheFourPinnedEpicFightLayers() {
        for (String name : new String[]{"stamina_bar", "skills", "weapon_innate", "charging_bar"}) {
            assertTrue(EpicFightHud.replaces(ResourceLocation.fromNamespaceAndPath("epicfight", name)));
            assertFalse(EpicFightHud.replaces(ResourceLocation.fromNamespaceAndPath("minecraft", name)));
        }
        assertFalse(EpicFightHud.replaces(ResourceLocation.parse("minecraft:hotbar")));
        assertFalse(EpicFightHud.replaces(ResourceLocation.parse("addon:skills")));
    }

    @Test
    void metersClampFiniteRatiosAndRejectInvalidValues() {
        assertEquals(30, EpicFightHud.fillWidth(50, 100, 60));
        assertEquals(60, EpicFightHud.fillWidth(200, 100, 60));
        assertEquals(0, EpicFightHud.fillWidth(-10, 100, 60));
        assertEquals(0, EpicFightHud.fillWidth(Double.NaN, 100, 60));
        assertEquals(0, EpicFightHud.fillWidth(10, 0, 60));
        assertEquals(0, EpicFightHud.fillWidth(10, 100, -1));
    }
}
