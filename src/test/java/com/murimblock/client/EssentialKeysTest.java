package com.murimblock.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EssentialKeysTest {
    @Test
    void essentialsKeysAreKnownByNameOrCategory() {
        assertTrue(EssentialKeys.essentials("essential.keybind.zoom", "key.categories.misc"));
        assertTrue(EssentialKeys.essentials("Zoom", "Essential"));
        assertFalse(EssentialKeys.essentials("key.murimblock.charge_qi", "key.categories.murimblock"));
        assertFalse(EssentialKeys.essentials("key.hotbar.1", "key.categories.inventory"));
    }
}
