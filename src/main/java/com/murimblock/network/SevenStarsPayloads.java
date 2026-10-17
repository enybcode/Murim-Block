package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import org.joml.Vector3f;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to the players around: the Seven Stars Sword's stars (martial/SevenStarsSword), drawn by client/SevenStarsFx. */
public final class SevenStarsPayloads {
    private SevenStarsPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path));
    }

    /** The stars a target carries now (0: none), and whose. */
    public record Marks(int entityId, UUID owner, int count) implements CustomPacketPayload {
        public static final Type<Marks> TYPE = SevenStarsPayloads.type("star_marks");
        public static final StreamCodec<ByteBuf, Marks> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Marks::entityId,
                UUIDUtil.STREAM_CODEC, Marks::owner,
                ByteBufCodecs.VAR_INT, Marks::count,
                Marks::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** A full constellation burst on this target. */
    public record Burst(int entityId) implements CustomPacketPayload {
        public static final Type<Burst> TYPE = SevenStarsPayloads.type("star_burst");
        public static final StreamCodec<ByteBuf, Burst> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Burst::entityId, Burst::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Shooting Star: the player went from a to b as a streak of light, cutting these bodies on the way. */
    public record Streak(int playerId, Vector3f from, Vector3f to, List<Integer> hits) implements CustomPacketPayload {
        public static final Type<Streak> TYPE = SevenStarsPayloads.type("shooting_star");
        public static final StreamCodec<ByteBuf, Streak> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Streak::playerId,
                ByteBufCodecs.VECTOR3F, Streak::from,
                ByteBufCodecs.VECTOR3F, Streak::to,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Streak::hits,
                Streak::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * One of the techniques' effects, for client/SevenStarsFx: its kind, the player, a target (or -1), places, entity ids,
     * a number (what each means is said at each kind).
     */
    public record Fx(int kind, int playerId, int target, List<Vector3f> points, List<Integer> ids, int value) implements CustomPacketPayload {
        /** Dipper Steps: the target, the bounds' places, the ticks between them. */
        public static final int STEPS = 0;
        /** Orbiting Stars: stars round the player (value), for ids[0] ticks. */
        public static final int ORBIT = 1;
        /** A blow stopped by an orbiting star: from points[0], struck back at target, value stars left. */
        public static final int ORBIT_BLOCK = 2;
        /** The orbiting stars left fly off: one to each of ids. */
        public static final int ORBIT_RELEASE = 3;
        /** Constellation Link: the marked enemies ids, linked; they burst value ticks later. */
        public static final int LINK = 4;
        /** Falling Star Rain: its circle at points[0], radius value tenths of a block, for ids[0] ticks. */
        public static final int RAIN_CIRCLE = 5;
        /** A falling star from points[0] to points[1], landing value ticks later. */
        public static final int RAIN_STAR = 6;
        /** Seven Stars Sword Array round target: the swords' places, ids = rise, first blow, gap, final blow (ticks). */
        public static final int ARRAY = 7;
        /** Polaris Riposte: the pole star over the player, the target's constellation completed. */
        public static final int POLARIS = 8;
        /** Extinguished Stars: star number value lit over the target. */
        public static final int FINISH_STAR = 9;
        /** Extinguished Stars: all put out. */
        public static final int FINISH_OUT = 10;

        public static final Type<Fx> TYPE = SevenStarsPayloads.type("seven_stars_fx");
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
