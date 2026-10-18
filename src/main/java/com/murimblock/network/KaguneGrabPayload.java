package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the players near a kagune going for something (martial/DevouringDemon: Grapple, Constrict): who cast it,
 * which strike (KaguneState.STRIKE_*), the foe it goes for (-1: none) and the point (the foe's feet, or where Grapple
 * hooks the wall). The clients aim the kagune at it and, for Constrict, coil it round the foe (client/KaguneGrabs).
 */
public record KaguneGrabPayload(int attacker, int strike, int target, double x, double y, double z) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KaguneGrabPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "kagune_grab"));
    public static final StreamCodec<ByteBuf, KaguneGrabPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KaguneGrabPayload::attacker,
            ByteBufCodecs.VAR_INT, KaguneGrabPayload::strike,
            ByteBufCodecs.VAR_INT, KaguneGrabPayload::target,
            ByteBufCodecs.DOUBLE, KaguneGrabPayload::x,
            ByteBufCodecs.DOUBLE, KaguneGrabPayload::y,
            ByteBufCodecs.DOUBLE, KaguneGrabPayload::z,
            KaguneGrabPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
