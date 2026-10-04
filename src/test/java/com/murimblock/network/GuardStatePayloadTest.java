package com.murimblock.network;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardStatePayloadTest {
    @Test
    void bothInputStatesRoundTrip() {
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                net.minecraft.core.RegistryAccess.EMPTY);
        try {
            for (boolean holding : new boolean[]{true, false}) {
                var expected = new GuardStatePayload(holding);
                GuardStatePayload.STREAM_CODEC.encode(buffer, expected);
                assertEquals(expected, GuardStatePayload.STREAM_CODEC.decode(buffer));
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void packetContainsOnlyInputIntentAndNoClientChosenDamageTimingOrTarget() {
        assertEquals(java.util.List.of("holding"), Arrays.stream(GuardStatePayload.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList());
        assertTrue(new GuardStatePayload(true).holding());
        assertFalse(new GuardStatePayload(false).holding());
    }

    @Test
    void packetHasItsOwnStableChannel() {
        assertEquals("murimblock", GuardStatePayload.TYPE.id().getNamespace());
        assertEquals("guard_state", GuardStatePayload.TYPE.id().getPath());
    }
}
