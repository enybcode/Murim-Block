package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the players near a kagune going under the ground (martial/DevouringDemon: Impaling Thorns, Burrow): who cast
 * it, which strike (KaguneState.STRIKE_*) and where it will burst out (x, y, z of each point, in turn). The clients draw
 * it coming up there on time (client/KaguneEruptions).
 */
public record KaguneEruptPayload(int attacker, int strike, List<Double> points) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KaguneEruptPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "kagune_erupt"));
    public static final StreamCodec<ByteBuf, KaguneEruptPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KaguneEruptPayload::attacker,
            ByteBufCodecs.VAR_INT, KaguneEruptPayload::strike,
            ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list(30)), KaguneEruptPayload::points,
            KaguneEruptPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
