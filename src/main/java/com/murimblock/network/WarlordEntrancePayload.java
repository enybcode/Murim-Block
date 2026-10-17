package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the players in a bandit camp whose garrison has just died: play the Bandit Warlord's entrance as a film
 * (client/WarlordCinematic), on the server's timing (WarlordEntrance): the plaza he falls on, the war drum that beats.
 */
public record WarlordEntrancePayload(BlockPos plaza, BlockPos drum) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WarlordEntrancePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "warlord_entrance"));
    public static final StreamCodec<ByteBuf, WarlordEntrancePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarlordEntrancePayload::plaza,
            BlockPos.STREAM_CODEC, WarlordEntrancePayload::drum,
            WarlordEntrancePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
