package com.murimblock.network;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QiChargeStatePayloadTest {
    @Test
    void payloadOnlyCarriesRequestedChargingState() {
        assertTrue(new QiChargeStatePayload(true).charging());
        assertFalse(new QiChargeStatePayload(false).charging());
    }

    @Test
    void payloadUsesMurimblockChannelId() {
        assertEquals(
                ResourceLocation.fromNamespaceAndPath("murimblock", "qi_charge_state"),
                QiChargeStatePayload.TYPE.id()
        );
    }
}
