package com.murimblock.family;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.murimblock.Murimblock;
import com.murimblock.client.family.ClientFamily;
import com.murimblock.registry.MurimAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The family draw on the server (docs/world/families.md): on first joining, a player is drawn a house and the
 * Destiny window opens on their screen; ACCEPT keeps it, REROLL (once, free) draws again and that draw is final. No
 * inheritance is granted yet: the house is only shown (Profile, the System's message). Commands for tests:
 * {@code /family draw}, {@code /family set <house>}, {@code /family reset}.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class FamilyEvents {
    private static final String PROTOCOL = "1";

    private FamilyEvents() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }

    /** Server to owner: their house ("" for none), whether the free reroll is left, and whether to open Destiny. */
    public record Sync(String family, boolean freeReroll, boolean open) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = payloadType("family_sync");
        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Sync::family, ByteBufCodecs.BOOL, Sync::freeReroll, ByteBufCodecs.BOOL, Sync::open,
                Sync::new);

        @Override
        public Type<Sync> type() {
            return TYPE;
        }
    }

    /** Owner to server: keep the house ({@code reroll} false) or spend the free reroll. */
    public record Choice(boolean reroll) implements CustomPacketPayload {
        public static final Type<Choice> TYPE = payloadType("family_choice");
        public static final StreamCodec<ByteBuf, Choice> STREAM_CODEC = ByteBufCodecs.BOOL.map(Choice::new, Choice::reroll);

        @Override
        public Type<Choice> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(Sync.TYPE, Sync.STREAM_CODEC,
                (payload, context) -> ClientFamily.receive(payload.family(), payload.freeReroll(), payload.open()));
        registrar.playToServer(Choice.TYPE, Choice.STREAM_CODEC,
                (payload, context) -> choose((ServerPlayer) context.player(), payload.reroll()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FamilyState state = state(player);
            if (state.family() == null) {
                draw(player, state);
                sync(player, true);
            } else {
                // A player who left before choosing finds the window again; a settled house's bonuses are put back.
                if (!state.freeReroll()) {
                    InheritanceGrants.apply(player, state);
                }
                sync(player, state.freeReroll());
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !state(player).freeReroll()) {
            InheritanceGrants.apply(player, state(player));
        }
    }

    public static FamilyState state(ServerPlayer player) {
        return player.getData(MurimAttachments.FAMILY);
    }

    private static void draw(ServerPlayer player, FamilyState state) {
        Family house = FamilyDraw.draw(new java.util.Random(player.getRandom().nextLong()), state.sinceEpic());
        state.drawn(house);
    }

    public static void sync(ServerPlayer player, boolean open) {
        FamilyState state = state(player);
        Family house = state.family();
        PacketDistributor.sendToPlayer(player, new Sync(house == null ? "" : house.id(), state.freeReroll(), open));
    }

    private static void choose(ServerPlayer player, boolean reroll) {
        FamilyState state = state(player);
        if (state.family() == null || !state.freeReroll()) {
            sync(player, false);
            return;
        }
        state.spendFreeReroll();
        if (reroll) {
            draw(player, state);
        }
        announce(player, state.family());
        sync(player, reroll);
    }

    /** A Destiny Talisman burnt: a final new draw, granted and shown in the Destiny window. */
    public static void redraw(ServerPlayer player) {
        FamilyState state = state(player);
        draw(player, state);
        announce(player, state.family());
        sync(player, true);
    }

    /** The house is settled: the System's message, and its inheritance granted (the old house's bonuses taken back). */
    private static void announce(ServerPlayer player, Family house) {
        InheritanceGrants.apply(player, state(player));
        player.sendSystemMessage(Component.literal("[!] ").withStyle(ChatFormatting.AQUA)
                .append(Component.translatable("family.murimblock.born", Component.translatable(house.nameKey()).copy()
                        .withStyle(s -> s.withColor(house.rarity.text)))));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("family").requires(s -> s.hasPermission(2))
                .then(Commands.literal("draw").executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    FamilyState state = state(player);
                    draw(player, state);
                    announce(player, state.family());
                    sync(player, false);
                    return 1;
                }))
                .then(Commands.literal("set").then(Commands.argument("house", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.stream(Family.values()).map(Family::id), b))
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            Family house = Family.byId(StringArgumentType.getString(c, "house"));
                            if (house == null) {
                                c.getSource().sendFailure(Component.translatable("family.murimblock.unknown"));
                                return 0;
                            }
                            state(player).set(house);
                            state(player).spendFreeReroll();
                            announce(player, house);
                            sync(player, false);
                            return 1;
                        })))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    FamilyState state = state(player);
                    InheritanceGrants.removeStats(player, state);
                    state.reset();
                    draw(player, state);
                    sync(player, true);
                    return 1;
                })));
    }
}
