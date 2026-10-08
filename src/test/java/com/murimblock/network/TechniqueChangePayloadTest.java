package com.murimblock.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TechniqueChangePayloadTest {
    @Test
    void requestRoundTripsWithoutPlayerIdentityOrClientAuthoredSkillData() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var request = new TechniqueChangePayload(21, "epicfight:guard", 40);
            TechniqueChangePayload.STREAM_CODEC.encode(buffer, request);
            assertEquals(request, TechniqueChangePayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test
    void boundedCodecRejectsOversizedSkillIdentifiers() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            assertThrows(RuntimeException.class, () -> TechniqueChangePayload.STREAM_CODEC.encode(buffer,
                    new TechniqueChangePayload(0, "a".repeat(129), -1)));
        } finally { buffer.release(); }
    }
}
