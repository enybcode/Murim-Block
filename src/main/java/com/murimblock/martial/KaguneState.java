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

    /** How long the transformation lasts (s): the animation's own length (art/blockbench/kagune_forms.bbmodel). */
    public static final float TRANSFORM_SECONDS = 2.6F;
    /** On all fours (Enzo, 18/10/2026: "change the hitbox!"): the body lies level, lower and longer than a man standing. */
    public static final net.minecraft.world.entity.EntityDimensions BEAST_SIZE =
            net.minecraft.world.entity.EntityDimensions.scalable(0.9F, 1.15F).withEyeHeight(0.95F);
    /** On all fours he goes faster than standing (Enzo: "it must go faster on all fours than standing"): +40%. */
    static final double BEAST_SPEED = 0.4;
    private static final ResourceLocation BEAST_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "demon_beast_speed");

    /** Whether the player is in the demon form now, on all fours: past the transformation. The same on both sides. */
    public static boolean beast(Player player) {
        var state = of(player);
        if (state.form() == Form.DEMON) return true;
        return state.form() == Form.TRANSFORM && (player.level().getGameTime() - state.since()) / 20F >= TRANSFORM_SECONDS;
    }

    /** The beast's body and speed, kept in step with the form on each side (the hitbox on both, the speed on the server). */
    @EventBusSubscriber(modid = Murimblock.MOD_ID)
    static final class Beast {
        private static final java.util.Set<java.util.UUID> SERVER = new java.util.HashSet<>(), CLIENT = new java.util.HashSet<>();

        private Beast() { }

        @SubscribeEvent
        static void size(net.neoforged.neoforge.event.entity.EntityEvent.Size event) {
            if (!(event.getEntity() instanceof Player player) || !player.hasData(TYPE)) return;
            var pose = event.getPose();
            if (pose == net.minecraft.world.entity.Pose.SLEEPING || pose == net.minecraft.world.entity.Pose.DYING) return;
            if (beast(player)) event.setNewSize(BEAST_SIZE);
        }

        @SubscribeEvent
        static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            var seen = player.level().isClientSide ? CLIENT : SERVER;
            boolean now = beast(player);
            if (now == seen.contains(player.getUUID())) return;
            if (now) seen.add(player.getUUID()); else seen.remove(player.getUUID());
            player.refreshDimensions();
            if (player.level().isClientSide) return;
            var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (speed == null) return;
            speed.removeModifier(BEAST_SPEED_ID);
            if (now) speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BEAST_SPEED_ID, BEAST_SPEED,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
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
