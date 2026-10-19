package com.murimblock.martial;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The kagune of the Devouring Demon Art on a player (docs/combat/styles/devouring-demon-art.md): which form is out and
 * since when (game time), the Fury (0 to 100) and the strike the kagune is making (since when), seen by every client
 * (client/KaguneOverlay draws it on the skin, client/KaguneStrikes points it at the foe, the combat bar shows the Fury).
 * The server drives it (DevouringDemon); the operator command {@code /kagune off|hunger|transform|demon} and
 * {@code /kagune fury <0-100>} set it by hand. Not saved: a reloaded player has his kagune put away.
 */
public record KaguneState(Form form, long since, int fury, int strike, long strikeSince, Form from) {
    public enum Form {
        /** Put away: nothing drawn. */
        NONE,
        /** 0 to 99 Fury: the four tentacles. */
        HUNGER,
        /** Full Fury: the centipedes (or the tentacles) burst out beside the others, the mask grows (then DEMON). */
        TRANSFORM,
        /** The demon form: the four tentacles and the two centipedes at once, the mask, on all fours. */
        DEMON,
        /** 0 to 99 Fury, the kagune switched (Kagune Shift, Enzo 19/10/2026): the two centipedes, standing. */
        CENTIPEDE
    }

    public static final int MAX_FURY = 100;
    /**
     * The kagune's strikes, for the clients (client/KaguneStrikes): none, Lash, Centipede Bite, Impaling Thorns, Burrow,
     * Grapple, Constrict. In the demon form the tentacles and the centipedes strike at once: {@link #strike} holds the
     * tentacles' in its low byte, the centipedes' in the next ({@link #strikes}, {@link #has}).
     */
    public static final int STRIKE_NONE = 0, STRIKE_LASH = 1, STRIKE_BITE = 2, STRIKE_THORNS = 3, STRIKE_BURROW = 4, STRIKE_GRAPPLE = 5, STRIKE_CONSTRICT = 6;

    /** The strike field for a tentacles' strike and a centipedes' one at once (either may be none). */
    public static int strikes(int tentacles, int centipedes) {
        return (tentacles & 0xFF) | (centipedes & 0xFF) << 8;
    }

    /** Whether this strike is under way in the strike field (the tentacles' or the centipedes'). */
    public static boolean has(int strike, int code) {
        return code != STRIKE_NONE && ((strike & 0xFF) == code || (strike >> 8 & 0xFF) == code);
    }

    public static final KaguneState NONE = new KaguneState(Form.NONE, 0, 0, STRIKE_NONE, 0, Form.NONE);
    public static final StreamCodec<ByteBuf, KaguneState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> Form.values()[i], Form::ordinal), KaguneState::form,
            ByteBufCodecs.VAR_LONG, KaguneState::since,
            ByteBufCodecs.VAR_INT, KaguneState::fury,
            ByteBufCodecs.VAR_INT, KaguneState::strike,
            ByteBufCodecs.VAR_LONG, KaguneState::strikeSince,
            ByteBufCodecs.VAR_INT.map(i -> Form.values()[i], Form::ordinal), KaguneState::from,
            KaguneState::new);
    public static final AttachmentType<KaguneState> TYPE = AttachmentType.builder(() -> NONE)
            .sync((holder, recipient) -> true, STREAM_CODEC).build();

    public static KaguneState of(Player player) {
        return player.getData(TYPE);
    }

    /** Takes out (or changes) the form, from now, the one it was kept (for the change's look); the Fury and the strike stay. */
    public static void set(Player player, Form form) {
        var s = of(player);
        put(player, new KaguneState(form, player.level().getGameTime(), form == Form.NONE ? 0 : s.fury(), s.strike(), s.strikeSince(), s.form()));
    }

    /** This state in another form, from {@code now}, the one it was kept. */
    public KaguneState into(Form next, long now) {
        return new KaguneState(next, now, fury, strike, strikeSince, form);
    }

    /** Whether the tentacles are out in this form (or coming), and the centipedes. */
    public static boolean tentacles(Form form) {
        return form == Form.HUNGER || form == Form.TRANSFORM || form == Form.DEMON;
    }

    public static boolean centipedes(Form form) {
        return form == Form.CENTIPEDE || form == Form.TRANSFORM || form == Form.DEMON;
    }

    static void put(Player player, KaguneState state) {
        if (!of(player).equals(state)) player.setData(TYPE, state);
    }

    public KaguneState withFury(int value) {
        return new KaguneState(form, since, Math.clamp(value, 0, MAX_FURY), strike, strikeSince, from);
    }

    public KaguneState withStrike(int code, long now) {
        return new KaguneState(form, since, fury, code, now, from);
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

    /** Whether the player is in the demon form or becoming it (the techniques change from the transformation on). */
    public static boolean demon(Player player) {
        var form = of(player).form();
        return form == Form.TRANSFORM || form == Form.DEMON;
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
                    .then(Commands.literal("fury").then(Commands.argument("value", IntegerArgumentType.integer(0, MAX_FURY))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                int value = IntegerArgumentType.getInteger(context, "value");
                                DevouringDemon.setFury(player, value);
                                context.getSource().sendSuccess(() -> Component.literal("Fury: " + value), false);
                                return 1;
                            })))
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
