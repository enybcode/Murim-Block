package com.murimblock.combat;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The combat messages between client and server. */
public final class CombatPayloads {
    private CombatPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }

    /** Server to client: every move, on join and after {@code /reload}. */
    public record Moves(Map<ResourceLocation, CombatMove> moves) implements CustomPacketPayload {
        public static final Type<Moves> TYPE = CombatPayloads.payloadType("combat_moves");
        public static final StreamCodec<ByteBuf, Moves> STREAM_CODEC = ByteBufCodecs.<ByteBuf, ResourceLocation,
                CombatMove, Map<ResourceLocation, CombatMove>>map(HashMap::new, ResourceLocation.STREAM_CODEC,
                CombatMove.STREAM_CODEC).map(Moves::new, Moves::moves);

        @Override
        public Type<Moves> type() {
            return TYPE;
        }
    }

    /** Server to client: every fighting style, on join and after {@code /reload}. */
    public record Movesets(Map<ResourceLocation, CombatMoveset> movesets) implements CustomPacketPayload {
        public static final Type<Movesets> TYPE = CombatPayloads.payloadType("combat_movesets");
        public static final StreamCodec<ByteBuf, Movesets> STREAM_CODEC = ByteBufCodecs.<ByteBuf, ResourceLocation,
                CombatMoveset, Map<ResourceLocation, CombatMoveset>>map(HashMap::new, ResourceLocation.STREAM_CODEC,
                CombatMoveset.STREAM_CODEC).map(Movesets::new, Movesets::movesets);

        @Override
        public Type<Movesets> type() {
            return TYPE;
        }
    }

    /** Client to server: switch combat mode on or off. */
    public record ToggleCombatMode() implements CustomPacketPayload {
        public static final Type<ToggleCombatMode> TYPE = CombatPayloads.payloadType("toggle_combat_mode");
        public static final StreamCodec<ByteBuf, ToggleCombatMode> STREAM_CODEC =
                StreamCodec.unit(new ToggleCombatMode());

        @Override
        public Type<ToggleCombatMode> type() {
            return TYPE;
        }
    }

    /** Server to the player: combat mode is now on or off, and why (a lang key, empty for the player's own toggle). */
    public record CombatMode(boolean on, String reason) implements CustomPacketPayload {
        public static final Type<CombatMode> TYPE = CombatPayloads.payloadType("combat_mode");
        public static final StreamCodec<ByteBuf, CombatMode> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, CombatMode::on, ByteBufCodecs.STRING_UTF8, CombatMode::reason, CombatMode::new);

        @Override
        public Type<CombatMode> type() {
            return TYPE;
        }
    }

    /** Client to server: the player started this strike (its animation already plays on their screen). */
    public record StrikeRequest(ResourceLocation move) implements CustomPacketPayload {
        public static final Type<StrikeRequest> TYPE = CombatPayloads.payloadType("strike_request");
        public static final StreamCodec<ByteBuf, StrikeRequest> STREAM_CODEC =
                ResourceLocation.STREAM_CODEC.map(StrikeRequest::new, StrikeRequest::move);

        @Override
        public Type<StrikeRequest> type() {
            return TYPE;
        }
    }

    /** Server to the striker: the strike was refused, stop it. */
    public record StrikeRefused() implements CustomPacketPayload {
        public static final Type<StrikeRefused> TYPE = CombatPayloads.payloadType("strike_refused");
        public static final StreamCodec<ByteBuf, StrikeRefused> STREAM_CODEC = StreamCodec.unit(new StrikeRefused());

        @Override
        public Type<StrikeRefused> type() {
            return TYPE;
        }
    }

    /** Server to those watching a fighter (not the fighter): they started this strike. */
    public record Strike(int entityId, ResourceLocation move) implements CustomPacketPayload {
        public static final Type<Strike> TYPE = CombatPayloads.payloadType("strike");
        public static final StreamCodec<ByteBuf, Strike> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Strike::entityId, ResourceLocation.STREAM_CODEC, Strike::move, Strike::new);

        @Override
        public Type<Strike> type() {
            return TYPE;
        }
    }

    /** Server to those watching the attacker (not the attacker, who predicted it): a blow landed. */
    public record Impact(int attackerId, int targetId, int hitStop) implements CustomPacketPayload {
        public static final Type<Impact> TYPE = CombatPayloads.payloadType("strike_impact");
        public static final StreamCodec<ByteBuf, Impact> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Impact::attackerId, ByteBufCodecs.VAR_INT, Impact::targetId,
                ByteBufCodecs.VAR_INT, Impact::hitStop, Impact::new);

        @Override
        public Type<Impact> type() {
            return TYPE;
        }
    }
}
