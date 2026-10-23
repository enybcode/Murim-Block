package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The cultivation messages between client and server. */
public final class CultivationPayloads {
    private CultivationPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }

    /** Server to owner: the whole cultivation. */
    public record Sync(Cultivation.Snapshot snapshot) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = CultivationPayloads.payloadType("cultivation_sync");
        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC =
                CultivationCodecs.SNAPSHOT.map(Sync::new, Sync::snapshot);

        @Override
        public Type<Sync> type() {
            return TYPE;
        }
    }

    /** Server to everyone tracking a player (and the player): sits down in lotus or stands up, with the Qi held. */
    public record Pose(int entityId, boolean meditating, int qi) implements CustomPacketPayload {
        public static final Type<Pose> TYPE = CultivationPayloads.payloadType("meditation_pose");
        public static final StreamCodec<ByteBuf, Pose> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Pose::entityId, ByteBufCodecs.BOOL, Pose::meditating, ByteBufCodecs.VAR_INT,
                Pose::qi, Pose::new);

        @Override
        public Type<Pose> type() {
            return TYPE;
        }
    }

    /** Server to everyone tracking a meditating player, every second: the Qi held, which sizes the Qi smoke. */
    public record MeditatorQi(int entityId, int qi) implements CustomPacketPayload {
        public static final Type<MeditatorQi> TYPE = CultivationPayloads.payloadType("meditator_qi");
        public static final StreamCodec<ByteBuf, MeditatorQi> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MeditatorQi::entityId, ByteBufCodecs.VAR_INT, MeditatorQi::qi, MeditatorQi::new);

        @Override
        public Type<MeditatorQi> type() {
            return TYPE;
        }
    }

    /** Client to server: start or stop meditating. */
    public record ToggleMeditation() implements CustomPacketPayload {
        public static final Type<ToggleMeditation> TYPE = CultivationPayloads.payloadType("toggle_meditation");
        public static final StreamCodec<ByteBuf, ToggleMeditation> STREAM_CODEC = StreamCodec.unit(new ToggleMeditation());

        @Override
        public Type<ToggleMeditation> type() {
            return TYPE;
        }
    }

    /** Client to server: rise one stage. */
    public record Advance() implements CustomPacketPayload {
        public static final Type<Advance> TYPE = CultivationPayloads.payloadType("advance_stage");
        public static final StreamCodec<ByteBuf, Advance> STREAM_CODEC = StreamCodec.unit(new Advance());

        @Override
        public Type<Advance> type() {
            return TYPE;
        }
    }

    /** Client to server: one beat of the breathing QTE, perfect, good or missed. */
    public record Breath(BreathResult result) implements CustomPacketPayload {
        public static final Type<Breath> TYPE = CultivationPayloads.payloadType("meditation_breath");
        public static final StreamCodec<ByteBuf, Breath> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
                id -> new Breath(BreathResult.byId(id)), b -> b.result().ordinal());

        @Override
        public Type<Breath> type() {
            return TYPE;
        }
    }
}
