package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.world.map.MurimBiomeSource;
import com.murimblock.world.map.MurimMapView;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Murim map (MurimMapView): a player asks for it, the server sends it (empty in a world without Murim lands). */
public final class MurimMapPayloads {
    private MurimMapPayloads() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** Client: the map, please. */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("murim_map_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = StreamCodec.unit(new Request());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Request payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean murim = player.server.overworld().getChunkSource().getGenerator().getBiomeSource() instanceof MurimBiomeSource;
            PacketDistributor.sendToPlayer(player, murim ? new View(MurimMapView.cells(), MurimMapView.hearts()) : new View(new byte[0], new int[0]));
        }
    }

    /** Server to a player: the grid of regions and their hearts (both empty: no Murim lands in this world). */
    public record View(byte[] cells, int[] hearts) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(id("murim_map_view"));
        public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY, View::cells,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)).map(l -> l.stream().mapToInt(Integer::intValue).toArray(),
                        a -> java.util.Arrays.stream(a).boxed().toList()), View::hearts,
                View::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
