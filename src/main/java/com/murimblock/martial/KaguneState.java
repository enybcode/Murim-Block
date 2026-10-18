package com.murimblock.martial;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The kagune of the Devouring Demon Art on a player (docs/combat/styles/devouring-demon-art.md): which form is out and
 * since when (game time), seen by every client (client/KaguneOverlay draws it on the skin). First slice: the two forms
 * and the transformation, set by the operator command {@code /kagune off|hunger|transform|demon}; the Fury bar and the
 * techniques come next. Not saved: a reloaded player has his kagune put away.
 */
public record KaguneState(Form form, long since) {
    public enum Form {
        /** Put away: nothing drawn. */
        NONE,
        /** 0 to 99 Fury: the four tentacles. */
        HUNGER,
        /** Full Fury: the tentacles draw back, the centipedes burst out (then DEMON). */
        TRANSFORM,
        /** The demon form: the two centipedes and the mask. */
        DEMON
    }

    public static final KaguneState NONE = new KaguneState(Form.NONE, 0);
    public static final StreamCodec<ByteBuf, KaguneState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> Form.values()[i], Form::ordinal), KaguneState::form,
            ByteBufCodecs.VAR_LONG, KaguneState::since,
            KaguneState::new);
    public static final AttachmentType<KaguneState> TYPE = AttachmentType.builder(() -> NONE)
            .sync((holder, recipient) -> true, STREAM_CODEC).build();

    public static KaguneState of(Player player) {
        return player.getData(TYPE);
    }

    public static void set(Player player, Form form) {
        player.setData(TYPE, new KaguneState(form, player.level().getGameTime()));
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID)
    static final class Registration {
        private Registration() { }

        @SubscribeEvent
        static void register(RegisterEvent event) {
            event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper ->
                    helper.register(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "kagune_state"), TYPE));
        }
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID)
    static final class Command {
        private Command() { }

        @SubscribeEvent
        static void register(RegisterCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("kagune").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("form", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"off", "hunger", "transform", "demon"}, builder))
                            .executes(context -> {
                                var player = context.getSource().getPlayerOrException();
                                String name = StringArgumentType.getString(context, "form");
                                Form form = switch (name) {
                                    case "hunger" -> Form.HUNGER;
                                    case "transform" -> Form.TRANSFORM;
                                    case "demon" -> Form.DEMON;
                                    default -> Form.NONE;
                                };
                                set(player, form);
                                context.getSource().sendSuccess(() -> Component.literal("Kagune: " + form.name().toLowerCase()), false);
                                return 1;
                            })));
        }
    }
}
