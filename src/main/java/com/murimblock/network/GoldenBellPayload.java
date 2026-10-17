package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the player and everyone round them: the Golden Bell (martial/GoldenBell). `skin` is what their body is made of
 * now (GoldenBell.SKIN_*: their own, gold, iron, stone), or an effect: -1 the bell rings, -2 a blow sent back. `form`:
 * whether they hold the golden form (its light). Drawn by client/GoldenBellSkins.
 */
public record GoldenBellPayload(int entityId, int skin, boolean form) implements CustomPacketPayload {
    public static final Type<GoldenBellPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "golden_bell"));
    public static final StreamCodec<ByteBuf, GoldenBellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GoldenBellPayload::entityId,
            ByteBufCodecs.INT, GoldenBellPayload::skin,
            ByteBufCodecs.BOOL, GoldenBellPayload::form,
            GoldenBellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
