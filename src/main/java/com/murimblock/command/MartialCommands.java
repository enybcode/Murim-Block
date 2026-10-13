package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.murimblock.Murimblock;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * /martial: the martial arts by hand, to test them (operators only, like /qi set).
 * <pre>
 * /martial info [player]                          arts, techniques, bar, mastery
 * /martial learn &lt;art&gt; [players]                 the art and its first technique, like reading the manual
 * /martial unlock &lt;technique|all&gt; [players]      a technique (its art too), or every technique of every art
 * /martial forget &lt;technique|art|all&gt; [players]  forgotten, off the bar, mastery lost; an art's manual can be received again
 * /martial mastery set|add &lt;technique|all&gt; &lt;value&gt; [players]   mastery, 0 to 100
 * /martial bar set &lt;slot 1-10&gt; &lt;technique&gt; [players] / bar clear &lt;slot 1-10&gt; [players]
 * /martial cooldown reset [players]               every technique ready again
 * </pre>
 * Arts and techniques are named by their path (cloud_step_slash) or full id (murimblock:cloud_step_slash).
 */
public final class MartialCommands {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(name -> Component.literal("Unknown martial art or technique: " + name));

    private static final SuggestionProvider<CommandSourceStack> ARTS = (c, b) -> SharedSuggestionProvider.suggest(artNames(), b);
    private static final SuggestionProvider<CommandSourceStack> TECHNIQUES = (c, b) -> SharedSuggestionProvider.suggest(techniqueNames(false), b);
    private static final SuggestionProvider<CommandSourceStack> TECHNIQUES_OR_ALL = (c, b) -> SharedSuggestionProvider.suggest(techniqueNames(true), b);
    private static final SuggestionProvider<CommandSourceStack> ANYTHING = (c, b) -> {
        List<String> all = new ArrayList<>(techniqueNames(true));
        all.addAll(artNames());
        return SharedSuggestionProvider.suggest(all, b);
    };

    private MartialCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("martial")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> info(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("learn")
                        .then(withTargets(Commands.argument("art", ResourceLocationArgument.id()).suggests(ARTS), c -> {
                            MartialArts.Art art = art(c);
                            return (p, s) -> s.add(MartialService.learn(p, art) ? "learned " + name(art) : "already knew " + name(art));
                        })))
                .then(Commands.literal("unlock")
                        .then(withTargets(Commands.argument("technique", ResourceLocationArgument.id()).suggests(TECHNIQUES_OR_ALL), c -> {
                            List<MartialArts.Technique> techniques = techniques(c);
                            return (p, s) -> {
                                techniques.forEach(t -> MartialService.unlock(p, t));
                                s.add("unlocked " + names(techniques));
                            };
                        })))
                .then(Commands.literal("forget")
                        .then(withTargets(Commands.argument("what", ResourceLocationArgument.id()).suggests(ANYTHING), c -> {
                            String what = arg(c, "what");
                            MartialArts.Art art = what.equals("all") ? null : MartialArts.art(id(what));
                            if (what.equals("all")) return (p, s) -> {
                                MartialArts.arts().forEach(a -> MartialService.forgetArt(p, a));
                                s.add("forgot every art");
                            };
                            if (art != null) return (p, s) -> {
                                MartialService.forgetArt(p, art);
                                s.add("forgot " + name(art));
                            };
                            MartialArts.Technique technique = technique(what);
                            return (p, s) -> {
                                MartialService.forgetTechnique(p, technique.id());
                                s.add("forgot " + name(technique));
                            };
                        })))
                .then(Commands.literal("mastery")
                        .then(mastery("set", false))
                        .then(mastery("add", true)))
                .then(Commands.literal("bar")
                        .then(Commands.literal("set")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, MartialData.BAR_SLOTS))
                                        .then(withTargets(Commands.argument("technique", ResourceLocationArgument.id()).suggests(TECHNIQUES), c -> {
                                            int slot = IntegerArgumentType.getInteger(c, "slot");
                                            MartialArts.Technique technique = technique(arg(c, "technique"));
                                            return (p, s) -> {
                                                // Placing a technique teaches it, so that the command never silently does nothing.
                                                MartialService.unlock(p, technique);
                                                MartialService.assign(p, slot - 1, technique.id());
                                                s.add(name(technique) + " in slot " + slot);
                                            };
                                        }))))
                        .then(Commands.literal("clear")
                                .then(withTargets(Commands.argument("slot", IntegerArgumentType.integer(1, MartialData.BAR_SLOTS)), c -> {
                                    int slot = IntegerArgumentType.getInteger(c, "slot");
                                    return (p, s) -> {
                                        MartialService.assign(p, slot - 1, null);
                                        s.add("slot " + slot + " emptied");
                                    };
                                }))))
                .then(Commands.literal("cooldown")
                        .then(withTargets(Commands.literal("reset"), c -> (p, s) -> {
                            MartialService.resetCooldowns(p);
                            s.add("every technique ready");
                        }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> mastery(String verb, boolean add) {
        return Commands.literal(verb)
                .then(Commands.argument("technique", ResourceLocationArgument.id()).suggests(TECHNIQUES_OR_ALL)
                        .then(withTargets(Commands.argument("value", IntegerArgumentType.integer(add ? -100 : 0, 100)), c -> {
                            List<MartialArts.Technique> techniques = techniques(c);
                            int value = IntegerArgumentType.getInteger(c, "value");
                            return (p, s) -> {
                                for (MartialArts.Technique t : techniques)
                                    MartialService.setMastery(p, t.id(), add ? MartialService.data(p).mastery(t.id()) + value : value);
                                s.add(techniques.size() == 1
                                        ? "mastery of " + name(techniques.getFirst()) + " = " + MartialService.data(p).mastery(techniques.getFirst().id())
                                        : "mastery " + (add ? (value >= 0 ? "+" : "") + value : "= " + value) + " on " + names(techniques));
                            };
                        })));
    }

    /** Reads the command's arguments, then what it does to each player (adding what it did to the feedback). */
    private interface Factory {
        BiConsumer<ServerPlayer, List<String>> make(CommandContext<CommandSourceStack> c) throws CommandSyntaxException;
    }

    /** The node runs on the command's own player, or on the players named after it. */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withTargets(T node, Factory factory) {
        node.executes(c -> run(c, List.of(c.getSource().getPlayerOrException()), factory));
        node.then(Commands.argument("players", EntityArgument.players())
                .executes(c -> run(c, EntityArgument.getPlayers(c, "players"), factory)));
        return node;
    }

    private static int run(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, Factory factory) throws CommandSyntaxException {
        BiConsumer<ServerPlayer, List<String>> action = factory.make(c);
        for (ServerPlayer player : players) {
            List<String> done = new ArrayList<>();
            action.accept(player, done);
            String line = player.getGameProfile().getName() + ": " + String.join(", ", done);
            c.getSource().sendSuccess(() -> Component.literal(line), true);
        }
        return players.size();
    }

    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        MartialData data = MartialService.data(player);
        List<String> lines = new ArrayList<>();
        lines.add("Martial arts of " + player.getGameProfile().getName() + ":");
        for (MartialArts.Art art : MartialArts.arts()) {
            lines.add(" " + name(art) + (data.knowsArt(art.id()) ? "" : " (not known)")
                    + (data.manualsReceived().contains(art.id()) ? ", manual received" : ""));
            for (MartialArts.Technique t : art.techniques()) {
                int cooldown = MartialService.cooldownLeft(player, t.id());
                lines.add("  " + (data.hasTechnique(t.id()) ? "+ " : "- ") + name(t)
                        + (data.hasTechnique(t.id()) ? "  mastery " + data.mastery(t.id()) + "/100" : "  locked")
                        + (cooldown > 0 ? String.format("  cooldown %.1f s", cooldown / 20.0) : ""));
            }
        }
        StringBuilder bar = new StringBuilder(" Bar:");
        for (int i = 0; i < MartialData.BAR_SLOTS; i++) {
            Optional<ResourceLocation> slot = data.slot(i);
            bar.append(" [").append(i + 1).append(' ').append(slot.map(id -> {
                MartialArts.Technique t = MartialArts.technique(id);
                return t == null ? id.toString() : name(t);
            }).orElse("-")).append(']');
        }
        lines.add(bar.toString());
        for (String line : lines) c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    /** An art, technique or "all" as typed: a bare name (read by Minecraft as minecraft:name) stays bare. */
    private static String arg(CommandContext<CommandSourceStack> c, String argument) {
        ResourceLocation id = ResourceLocationArgument.getId(c, argument);
        return id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) ? id.getPath() : id.toString();
    }

    private static ResourceLocation id(String name) {
        return name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name);
    }

    private static MartialArts.Art art(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String name = arg(c, "art");
        ResourceLocation id = id(name);
        MartialArts.Art art = id == null ? null : MartialArts.art(id);
        if (art == null) throw UNKNOWN.create(name);
        return art;
    }

    private static MartialArts.Technique technique(String name) throws CommandSyntaxException {
        ResourceLocation id = id(name);
        MartialArts.Technique technique = id == null ? null : MartialArts.technique(id);
        if (technique == null) throw UNKNOWN.create(name);
        return technique;
    }

    /** One technique, or every technique of every art for "all". */
    private static List<MartialArts.Technique> techniques(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String name = arg(c, "technique");
        if (!name.equals("all")) return List.of(technique(name));
        List<MartialArts.Technique> all = new ArrayList<>();
        for (MartialArts.Art art : MartialArts.arts()) all.addAll(art.techniques());
        return all;
    }

    private static List<String> artNames() {
        List<String> names = new ArrayList<>();
        for (MartialArts.Art art : MartialArts.arts()) names.add(art.id().getPath());
        return names;
    }

    private static List<String> techniqueNames(boolean withAll) {
        List<String> names = new ArrayList<>();
        if (withAll) names.add("all");
        for (MartialArts.Art art : MartialArts.arts()) for (MartialArts.Technique t : art.techniques()) names.add(t.id().getPath());
        return names;
    }

    private static String name(MartialArts.Art art) {
        return Component.translatable(art.translationKey()).getString();
    }

    private static String name(MartialArts.Technique technique) {
        return Component.translatable(technique.translationKey()).getString();
    }

    private static String names(List<MartialArts.Technique> techniques) {
        return techniques.size() == 1 ? name(techniques.getFirst()) : techniques.size() + " techniques";
    }
}
