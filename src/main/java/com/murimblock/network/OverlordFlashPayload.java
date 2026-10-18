package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the players near an Overlord Spear impact (martial/OverlordFx.flash): a flash of blood red and gold there, a
 * Photon light {@code range} blocks wide lasting {@code ticks} (client/OverlordFxClient); nothing where Photon's lights
 * are off.
 */
public record OverlordFlashPayload(double x, double y, double z, float range, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OverlordFlashPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "overlord_flash"));
    public static final StreamCodec<ByteBuf, OverlordFlashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, OverlordFlashPayload::x,
            ByteBufCodecs.DOUBLE, OverlordFlashPayload::y,
            ByteBufCodecs.DOUBLE, OverlordFlashPayload::z,
            ByteBufCodecs.FLOAT, OverlordFlashPayload::range,
            ByteBufCodecs.VAR_INT, OverlordFlashPayload::ticks,
            OverlordFlashPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
