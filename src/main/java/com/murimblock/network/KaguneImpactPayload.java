package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the players near a kagune's blow landing (martial/DevouringDemon): who struck, who was struck, where, and
 * which strike (KaguneState.STRIKE_*). The clients show it at the impact only (client/KaguneImpactFx): a light, a spray
 * of blood, the view shaken for the two of them, and the strike held a moment (the hit-stop).
 */
public record KaguneImpactPayload(int attacker, int victim, double x, double y, double z, int strike) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KaguneImpactPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "kagune_impact"));
    public static final StreamCodec<ByteBuf, KaguneImpactPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KaguneImpactPayload::attacker,
            ByteBufCodecs.VAR_INT, KaguneImpactPayload::victim,
            ByteBufCodecs.DOUBLE, KaguneImpactPayload::x,
            ByteBufCodecs.DOUBLE, KaguneImpactPayload::y,
            ByteBufCodecs.DOUBLE, KaguneImpactPayload::z,
            ByteBufCodecs.VAR_INT, KaguneImpactPayload::strike,
            KaguneImpactPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
