package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.QiSense;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The meditation key, Qi Sense and what clients are shown of breakthroughs (docs/cultivation/qi-cultivation.md). */
public final class CultivationPayloads {
    private CultivationPayloads() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** Client: the meditation key. A tap sits down or gets up; held while seated, it opens (or leaves) the inner world. */
    public record Meditate(int action) implements CustomPacketPayload {
        public static final int STOP = 0, START = 1, HOLD = 2;
        public static final Type<Meditate> TYPE = new Type<>(id("meditate"));
        public static final StreamCodec<ByteBuf, Meditate> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Meditate::new, Meditate::action);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Meditate payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case START -> MeditationService.start(player);
                case STOP -> MeditationService.stop(player, true);
                case HOLD -> MeditationService.held(player);
                default -> { }
            }
        }
    }

    /** Client: the Qi Sense key held down (true) or let go. */
    public record Sense(boolean active) implements CustomPacketPayload {
        public static final Type<Sense> TYPE = new Type<>(id("qi_sense"));
        public static final StreamCodec<ByteBuf, Sense> STREAM_CODEC = ByteBufCodecs.BOOL.map(Sense::new, Sense::active);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Sense payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) QiSense.setActive(player, payload.active);
        }
    }

    /** One body read by Qi Sense: what the server lets this reader see of it (realm and stage -1 when unreadable). */
    public record Reading(int entityId, int realm, int stage, int kind) {
        public static final StreamCodec<ByteBuf, Reading> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Reading::entityId,
                ByteBufCodecs.VAR_INT, Reading::realm,
                ByteBufCodecs.VAR_INT, Reading::stage,
                ByteBufCodecs.VAR_INT, Reading::kind,
                Reading::new);
    }

    /** Server to a reader: the auras within reach, as they may read them (an empty list: the sense closed or unopened). */
    public record SenseReadings(boolean open, List<Reading> readings) implements CustomPacketPayload {
        public static final Type<SenseReadings> TYPE = new Type<>(id("qi_sense_readings"));
        public static final StreamCodec<ByteBuf, SenseReadings> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, SenseReadings::open,
                Reading.STREAM_CODEC.apply(ByteBufCodecs.list(64)), SenseReadings::readings,
                SenseReadings::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to the clients near a body: a breakthrough (or its failure) happens to it now, to be shown. */
    public record Breakthrough(int entityId, int kind) implements CustomPacketPayload {
        public static final int MINOR = 0, MAJOR = 1, DEVIATION = 2, ENTER = 3, RETURN = 4;
        public static final Type<Breakthrough> TYPE = new Type<>(id("breakthrough"));
        public static final StreamCodec<ByteBuf, Breakthrough> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Breakthrough::entityId,
                ByteBufCodecs.VAR_INT, Breakthrough::kind,
                Breakthrough::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
