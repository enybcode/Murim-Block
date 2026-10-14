package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.dialogue.DialogueService;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Conversations with characters (DialogueService): a page shown to the player, and the answer they chose. */
public final class DialoguePayloads {
    private DialoguePayloads() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** One answer the player may give: its id (sent back) and its text. */
    public record Option(String id, Component label) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Option> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Option::id,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Option::label,
                Option::new);
    }

    /** Server to a player: what the speaker says, and the answers offered (none: the last page, closed by the player). */
    public record Page(int speakerId, Component speaker, Component line, List<Option> options) implements CustomPacketPayload {
        public static final Type<Page> TYPE = new Type<>(id("dialogue_page"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Page::speakerId,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Page::speaker,
                ComponentSerialization.TRUSTED_STREAM_CODEC, Page::line,
                Option.STREAM_CODEC.apply(ByteBufCodecs.list(8)), Page::options,
                Page::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client: the answer chosen on the page of this speaker. */
    public record Choose(int speakerId, String option) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(id("dialogue_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Choose::speakerId,
                ByteBufCodecs.stringUtf8(64), Choose::option,
                Choose::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(Choose payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) DialogueService.choose(player, payload.speakerId, payload.option);
        }
    }
}
