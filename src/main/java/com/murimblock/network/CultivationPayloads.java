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

    /** Client: its cultivator's gaze rests on a mote of Qi round their meditation (MeditationOrbs): draw it in. */
    public record Absorb(int entityId) implements CustomPacketPayload {
        public static final Type<Absorb> TYPE = new Type<>(id("absorb_mote"));
        public static final StreamCodec<ByteBuf, Absorb> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Absorb::new, Absorb::entityId);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Absorb payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) com.murimblock.cultivation.MeditationOrbs.draw(player, payload.entityId);
        }
    }

    /** Client: it leapt once more in the air (Qinggong): the server checks, takes the Qi, shows the gust. */
    public record Leap() implements CustomPacketPayload {
        public static final Type<Leap> TYPE = new Type<>(id("qinggong_leap"));
        public static final StreamCodec<ByteBuf, Leap> STREAM_CODEC = StreamCodec.unit(new Leap());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Leap payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) com.murimblock.cultivation.Qinggong.leap(player);
        }
    }

    /** Client: practise this known cultivation method (from the Cultivation tab). */
    public record PractiseMethod(String method) implements CustomPacketPayload {
        public static final Type<PractiseMethod> TYPE = new Type<>(id("practise_method"));
        public static final StreamCodec<ByteBuf, PractiseMethod> STREAM_CODEC = ByteBufCodecs.stringUtf8(64).map(PractiseMethod::new, PractiseMethod::method);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(PractiseMethod payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player)
                com.murimblock.cultivation.CultivationMethod.byId(payload.method).ifPresent(m -> com.murimblock.cultivation.MethodService.practise(player, m));
        }
    }

    /** Client: flying on the sword: the second jump held (CHARGE), let go too soon (CANCEL), held long enough (MOUNT), off it (LAND). */
    public record SwordFlight(int action) implements CustomPacketPayload {
        public static final int CHARGE = 0, CANCEL = 1, MOUNT = 2, LAND = 3;
        public static final Type<SwordFlight> TYPE = new Type<>(id("sword_flight"));
        public static final StreamCodec<ByteBuf, SwordFlight> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(SwordFlight::new, SwordFlight::action);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(SwordFlight payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case CHARGE -> com.murimblock.cultivation.SwordFlight.charge(player);
                case CANCEL -> com.murimblock.cultivation.SwordFlight.cancel(player);
                case MOUNT -> com.murimblock.cultivation.SwordFlight.mount(player);
                case LAND -> com.murimblock.cultivation.SwordFlight.land(player, false);
                default -> { }
            }
        }
    }

    /** Server to those who see a player (and to them): they fly on their sword now, or not. */
    public record SwordFlightState(int entityId, boolean flying) implements CustomPacketPayload {
        public static final Type<SwordFlightState> TYPE = new Type<>(id("sword_flight_state"));
        public static final StreamCodec<ByteBuf, SwordFlightState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SwordFlightState::entityId,
                ByteBufCodecs.BOOL, SwordFlightState::flying,
                SwordFlightState::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client: the aura's key pressed: release it, or hold it in (AuraPressure). */
    public record TogglePressure() implements CustomPacketPayload {
        public static final Type<TogglePressure> TYPE = new Type<>(id("toggle_pressure"));
        public static final StreamCodec<ByteBuf, TogglePressure> STREAM_CODEC = StreamCodec.unit(new TogglePressure());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(TogglePressure payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) com.murimblock.cultivation.AuraPressure.toggle(player);
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

    /**
     * Server to a player: the vital points lit on a body (a bit each of VitalPoints.POINTS, 0: none left), the one just
     * struck (-1: none) and for how many ticks more they stay lit.
     */
    public record Vital(int entityId, int mask, int struck, int ticks) implements CustomPacketPayload {
        public static final Type<Vital> TYPE = new Type<>(id("vital_points"));
        public static final StreamCodec<ByteBuf, Vital> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Vital::entityId,
                ByteBufCodecs.VAR_INT, Vital::mask,
                ByteBufCodecs.VAR_INT, Vital::struck,
                ByteBufCodecs.VAR_INT, Vital::ticks,
                Vital::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A point of a trail of the powerful (QiTraces): where, which way, how old (ticks), the walker's art ("" a creature, "none" no art), which walker. */
    public record Trace(float x, float y, float z, float yaw, int age, String art, int walker) {
        public static final StreamCodec<ByteBuf, Trace> STREAM_CODEC = new StreamCodec<>() {
            @Override public Trace decode(ByteBuf buf) {
                return new Trace(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.stringUtf8(128).decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
            }

            @Override public void encode(ByteBuf buf, Trace t) {
                buf.writeFloat(t.x).writeFloat(t.y).writeFloat(t.z).writeFloat(t.yaw);
                ByteBufCodecs.VAR_INT.encode(buf, t.age);
                ByteBufCodecs.stringUtf8(128).encode(buf, t.art);
                ByteBufCodecs.VAR_INT.encode(buf, t.walker);
            }
        };
    }

    /** Server to a reader at Nascent Soul, Qi Sense open, each second: the trails of the powerful around, the hidden caches (where their Qi comes out). */
    public record Traces(List<Trace> traces, List<net.minecraft.core.BlockPos> caches) implements CustomPacketPayload {
        public static final Type<Traces> TYPE = new Type<>(id("qi_traces"));
        public static final StreamCodec<ByteBuf, Traces> STREAM_CODEC = StreamCodec.composite(
                Trace.STREAM_CODEC.apply(ByteBufCodecs.list(256)), Traces::traces,
                net.minecraft.core.BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(32)), Traces::caches,
                Traces::new);

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
