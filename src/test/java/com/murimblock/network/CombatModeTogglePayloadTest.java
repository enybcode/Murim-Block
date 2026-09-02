package com.murimblock.network;

import com.murimblock.Murimblock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatModeTogglePayloadTest {
    @Test
    void payloadHasStableIdentifier() {
        assertEquals(Murimblock.MOD_ID, CombatModeTogglePayload.TYPE.id().getNamespace());
        assertEquals("combat_mode_toggle", CombatModeTogglePayload.TYPE.id().getPath());
    }

    @Test
    void payloadCarriesNoClientChosenState() {
        assertEquals(0, CombatModeTogglePayload.class.getRecordComponents().length);
    }
}
