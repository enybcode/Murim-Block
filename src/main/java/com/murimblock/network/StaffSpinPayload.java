package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the player and everyone tracking them: they started (or stopped) the Spinning Rampart, so their clients show
 * its looping spin as long as it lasts (client/StaffClient).
 */
public record StaffSpinPayload(int entityId, boolean spinning) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StaffSpinPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "staff_spin"));
    public static final StreamCodec<ByteBuf, StaffSpinPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StaffSpinPayload::entityId,
            ByteBufCodecs.BOOL, StaffSpinPayload::spinning,
            StaffSpinPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
