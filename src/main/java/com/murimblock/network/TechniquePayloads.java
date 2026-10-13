package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The combat bar's messages: cast a slot, place a technique in a slot, and the server's cooldown notice. */
public final class TechniquePayloads {
    private TechniquePayloads() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** Client asks to use the technique in a slot of its combat bar (keys 1 to 0 in combat mode). */
    public record Cast(int slot) implements CustomPacketPayload {
        public static final Type<Cast> TYPE = new Type<>(id("technique_cast"));
        public static final StreamCodec<ByteBuf, Cast> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Cast::new, Cast::slot);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Cast payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ResourceLocation id = MartialService.data(player).slot(payload.slot).orElse(null);
            MartialArts.Technique technique = id == null ? null : MartialArts.technique(id);
            CastResult result = MartialService.castSlot(player, payload.slot);
            if (technique == null) return;
            Component name = Component.translatable(technique.translationKey());
            switch (result) {
                case CAST -> PacketDistributor.sendToPlayer(player, new Cooldown(technique.id(), MartialService.cooldownLeft(player, technique.id())));
                case COOLDOWN -> player.displayClientMessage(Component.translatable("technique.murimblock.cast.cooldown", name,
                        (MartialService.cooldownLeft(player, technique.id()) + 19) / 20), true);
                case NO_QI -> player.displayClientMessage(Component.translatable("technique.murimblock.cast.no_qi", name, (int) technique.qiCost()), true);
                case NOT_IN_COMBAT -> player.displayClientMessage(Component.translatable("technique.murimblock.cast.not_in_combat"), true);
                case NO_WEAPON -> player.displayClientMessage(Component.translatable("technique.murimblock.cast.no_weapon", name), true);
                default -> { }
            }
        }
    }

    /**
     * Client: the key of a slot went down (its release sends {@link Cast}). For a technique held to aim (Cloud Step
     * Slash), the server puts the player in its draw stance; for the others, nothing.
     */
    public record Aim(int slot) implements CustomPacketPayload {
        public static final Type<Aim> TYPE = new Type<>(id("technique_aim"));
        public static final StreamCodec<ByteBuf, Aim> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Aim::new, Aim::slot);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Aim payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) MartialService.aim(player, payload.slot);
        }
    }

    /** Client places an unlocked technique in a slot of its bar (or clears it). */
    public record Assign(int slot, Optional<ResourceLocation> technique) implements CustomPacketPayload {
        public static final Type<Assign> TYPE = new Type<>(id("technique_assign"));
        public static final StreamCodec<ByteBuf, Assign> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Assign::slot,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Assign::technique,
                Assign::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Assign payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) MartialService.assign(player, payload.slot, payload.technique.orElse(null));
        }
    }

    /** Client places a strike of its main style's passive in one of the four M1 slots (Techniques tab). */
    public record SetStrike(int slot, ResourceLocation strike) implements CustomPacketPayload {
        public static final Type<SetStrike> TYPE = new Type<>(id("strike_set"));
        public static final StreamCodec<ByteBuf, SetStrike> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetStrike::slot,
                ResourceLocation.STREAM_CODEC, SetStrike::strike,
                SetStrike::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(SetStrike payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) MartialService.setStrike(player, payload.slot, payload.strike);
        }
    }

    /** Server tells the client a perfect parry opened a riposte window of that many ticks (the bar lights up). */
    public record Riposte(int ticks) implements CustomPacketPayload {
        public static final Type<Riposte> TYPE = new Type<>(id("technique_riposte"));
        public static final StreamCodec<ByteBuf, Riposte> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Riposte::new, Riposte::ticks);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server tells the client a technique was used and how long until it is ready (for the bar's cooldown display). */
    public record Cooldown(ResourceLocation technique, int ticks) implements CustomPacketPayload {
        public static final Type<Cooldown> TYPE = new Type<>(id("technique_cooldown"));
        public static final StreamCodec<ByteBuf, Cooldown> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Cooldown::technique,
                ByteBufCodecs.VAR_INT, Cooldown::ticks,
                Cooldown::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
