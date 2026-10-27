package com.murimblock.combat;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Shenfa's and combat's messages between client and server. */
public final class CombatPayloads {
    private CombatPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }

    /**
     * Server to everyone tracking a body (and the body itself): play a clip on a layer, or stop the layer when
     * {@code clip} is empty. Clip ids go as text: clients may have more clips than the server (resource packs).
     */
    public record Anim(int entity, int layer, String clip, float speed, boolean mirrored) implements CustomPacketPayload {
        public static final Type<Anim> TYPE = payloadType("shenfa_anim");
        public static final StreamCodec<ByteBuf, Anim> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Anim::entity, ByteBufCodecs.VAR_INT, Anim::layer, ByteBufCodecs.STRING_UTF8,
                Anim::clip, ByteBufCodecs.FLOAT, Anim::speed, ByteBufCodecs.BOOL, Anim::mirrored, Anim::new);

        @Override
        public Type<Anim> type() {
            return TYPE;
        }
    }

    /** What a blow did. */
    public enum Result {
        HIT, GUARD, PARRY, GUARD_BREAK, DODGED
    }

    /**
     * Server to everyone near: a blow landed (or was guarded, parried, dodged), for the feel: sparks, sound, camera
     * shake, the hit stop on both bodies, the target's reaction.
     */
    public record Hit(int attacker, int target, Result result, int stun, float impact, boolean cut, int hitStop,
                      Vec3 at) implements CustomPacketPayload {
        public static final Type<Hit> TYPE = payloadType("combat_hit");
        public static final StreamCodec<FriendlyByteBuf, Hit> STREAM_CODEC = StreamCodec.of((buf, hit) -> {
            buf.writeVarInt(hit.attacker);
            buf.writeVarInt(hit.target);
            buf.writeEnum(hit.result);
            buf.writeVarInt(hit.stun);
            buf.writeFloat(hit.impact);
            buf.writeBoolean(hit.cut);
            buf.writeVarInt(hit.hitStop);
            buf.writeVec3(hit.at);
        }, buf -> new Hit(buf.readVarInt(), buf.readVarInt(), buf.readEnum(Result.class), buf.readVarInt(),
                buf.readFloat(), buf.readBoolean(), buf.readVarInt(), buf.readVec3()));

        @Override
        public Type<Hit> type() {
            return TYPE;
        }
    }

    /** Server to everyone tracking a player (and the player): combat mode is on or off (the stance, the strikes). */
    public record Mode(int entity, boolean on) implements CustomPacketPayload {
        public static final Type<Mode> TYPE = payloadType("combat_mode");
        public static final StreamCodec<ByteBuf, Mode> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Mode::entity, ByteBufCodecs.BOOL, Mode::on, Mode::new);

        @Override
        public Type<Mode> type() {
            return TYPE;
        }
    }

    /**
     * Server to a player: its own fight as the server sees it, when it differs from what the client predicted: the
     * move it plays (empty for none) and how far in, and a stagger.
     */
    public record Self(String clip, float time, int stunTicks) implements CustomPacketPayload {
        public static final Type<Self> TYPE = payloadType("combat_self");
        public static final StreamCodec<ByteBuf, Self> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Self::clip, ByteBufCodecs.FLOAT, Self::time, ByteBufCodecs.VAR_INT,
                Self::stunTicks, Self::new);

        @Override
        public Type<Self> type() {
            return TYPE;
        }
    }

    /** What a player asks for. */
    public enum Action {
        MODE_ON, MODE_OFF, ATTACK, TECHNIQUE, DODGE, GUARD_UP, GUARD_DOWN
    }

    /** Client to server: a key of the fight was pressed; {@code direction} for a dodge (forward, back, left, right). */
    public record Input(Action action, int direction) implements CustomPacketPayload {
        public static final Type<Input> TYPE = payloadType("combat_input");
        public static final StreamCodec<FriendlyByteBuf, Input> STREAM_CODEC = StreamCodec.of((buf, input) -> {
            buf.writeEnum(input.action);
            buf.writeVarInt(input.direction);
        }, buf -> new Input(buf.readEnum(Action.class), buf.readVarInt()));

        @Override
        public Type<Input> type() {
            return TYPE;
        }
    }
}
