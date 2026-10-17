package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Server to the players around: the Purple Heaven Thunder Saber (martial/ThunderSaber), drawn by client/ThunderSaberFx. */
public final class ThunderSaberPayloads {
    private ThunderSaberPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path));
    }

    /** A player's thunder drums now: beats, three to a drum (0 to 9). */
    public record Drums(int entityId, int beats) implements CustomPacketPayload {
        public static final Type<Drums> TYPE = ThunderSaberPayloads.type("thunder_drums");
        public static final StreamCodec<ByteBuf, Drums> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Drums::entityId,
                ByteBufCodecs.VAR_INT, Drums::beats,
                Drums::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A body electrified for this many ticks (0: no longer). */
    public record Electrified(int entityId, int ticks) implements CustomPacketPayload {
        public static final Type<Electrified> TYPE = ThunderSaberPayloads.type("thunder_electrified");
        public static final StreamCodec<ByteBuf, Electrified> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Electrified::entityId,
                ByteBufCodecs.VAR_INT, Electrified::ticks,
                Electrified::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * One of the art's effects, for client/ThunderSaberFx: its kind, the player, a target (or -1), places, entity ids, a
     * number (what each means is said at each kind).
     */
    public record Fx(int kind, int playerId, int target, List<Vector3f> points, List<Integer> ids, int value) implements CustomPacketPayload {
        /** A bolt of lightning from points[0] down to points[1], value its width in tenths. */
        public static final int BOLT = 0;
        /** A shockwave on the ground at points[0], value its reach in tenths; ids[0] 1 if the thunder's (violet). */
        public static final int SHOCKWAVE = 1;
        /** Arcs of lightning from point to point along points (a chain). */
        public static final int ARCS = 2;
        /** Thunder Descends: the leap from points[0] to points[1], over value ticks. */
        public static final int LEAP = 3;
        /** Rolling Thunder: the spin of the player, value ticks; ids[0] 1 if the thunder's. */
        public static final int SPIN = 4;
        /** Heaven's Drum planted: its cage of lightning round the player for at most value ticks. */
        public static final int DRUM = 5;
        /** A blow stopped by Heaven's Drum, from points[0]; value the blows stopped so far. */
        public static final int DRUM_BLOCK = 6;
        /** Heaven's Drum beaten: the cage ends, the discharge out to value tenths; ids[0] 1 if the thunder's. */
        public static final int DRUM_END = 7;
        /** Severing Flash charging (value ticks at most); value 0 when it stops. */
        public static final int CHARGE = 8;
        /** Severing Flash's wave from points[0] to points[1] over value ticks; ids[0] 1 if the thunder's; ids[1..] cut. */
        public static final int WAVE = 9;
        /** Nine Heavens' Wrath: its circle and cloud at points[0], radius value tenths, for ids[0] ticks. */
        public static final int WRATH = 10;
        /** Nine Heavens' Wrath: the column of lightning at points[0], value its radius in tenths. */
        public static final int COLUMN = 11;
        /** Thunder Sovereign's Descent: the player wrapped in lightning for value ticks. */
        public static final int SOVEREIGN = 12;
        /** Heaven Answers: the sky opens over target. */
        public static final int ANSWERS = 13;

        public static final Type<Fx> TYPE = ThunderSaberPayloads.type("thunder_saber_fx");
        public static final StreamCodec<ByteBuf, Fx> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Fx::kind,
                ByteBufCodecs.VAR_INT, Fx::playerId,
                ByteBufCodecs.INT, Fx::target,
                ByteBufCodecs.VECTOR3F.apply(ByteBufCodecs.list()), Fx::points,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Fx::ids,
                ByteBufCodecs.VAR_INT, Fx::value,
                Fx::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
