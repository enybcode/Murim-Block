package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the two players of a finish: start (or, with {@code start} false, break off) the cinematic camera. Sent
 * only to the executor and the victim; everyone else watches the finish as usual.
 */
public record FinishCinematicPayload(boolean start, int executorId, int victimId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FinishCinematicPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "finish_cinematic"));
    public static final StreamCodec<ByteBuf, FinishCinematicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FinishCinematicPayload::start,
            ByteBufCodecs.VAR_INT, FinishCinematicPayload::executorId,
            ByteBufCodecs.VAR_INT, FinishCinematicPayload::victimId,
            FinishCinematicPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
