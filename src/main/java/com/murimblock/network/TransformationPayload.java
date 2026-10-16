package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the players near the Bandit Warlord when his life falls to zero: play his transformation into the Son of the
 * Demon as a film (client/TransformationCinematic), on the server's timing (WarlordTransformation): the warlord, where
 * he kneels, the middle of the arena in the sky, whether there is an arena (else the film stays on the ground), and the
 * world's time at its tick zero: the film keeps to the world's clock, which the server keeps in step, so a slow client
 * never drifts from the server's beats (it skips frames, not time).
 */
public record TransformationPayload(int warlordId, BlockPos ground, BlockPos arena, boolean rises, long start) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TransformationPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "warlord_transformation"));
    public static final StreamCodec<ByteBuf, TransformationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TransformationPayload::warlordId,
            BlockPos.STREAM_CODEC, TransformationPayload::ground,
            BlockPos.STREAM_CODEC, TransformationPayload::arena,
            ByteBufCodecs.BOOL, TransformationPayload::rises,
            ByteBufCodecs.VAR_LONG, TransformationPayload::start,
            TransformationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
