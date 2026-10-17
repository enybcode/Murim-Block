package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the players near a Majin Fist impact (MajinFist.flash): a flash of sunlight there, a Photon light lasting
 * {@code ticks} (client/CruelSunRenderer.flash); nothing where Photon's lights are off.
 */
public record SunFlashPayload(double x, double y, double z, float range, float intensity, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SunFlashPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "sun_flash"));
    public static final StreamCodec<ByteBuf, SunFlashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SunFlashPayload::x,
            ByteBufCodecs.DOUBLE, SunFlashPayload::y,
            ByteBufCodecs.DOUBLE, SunFlashPayload::z,
            ByteBufCodecs.FLOAT, SunFlashPayload::range,
            ByteBufCodecs.FLOAT, SunFlashPayload::intensity,
            ByteBufCodecs.VAR_INT, SunFlashPayload::ticks,
            SunFlashPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
