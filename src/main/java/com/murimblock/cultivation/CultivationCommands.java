package com.murimblock.cultivation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /murim cultivation get|fill|set <player>}, for operators and tests. */
final class CultivationCommands {
    private CultivationCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("murim").requires(s -> s.hasPermission(2))
                .then(Commands.literal("cultivation")
                        .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                                .executes(CultivationCommands::get)))
                        .then(Commands.literal("fill").then(Commands.argument("player", EntityArgument.player())
                                .executes(CultivationCommands::fill)))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("realm", IntegerArgumentType.integer(0, CultivationRealms.LADDER.size() - 1))
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, CultivationRules.STAGES))
                                .then(Commands.argument("meridians", IntegerArgumentType.integer(0, CultivationRules.MERIDIANS))
                                        .executes(CultivationCommands::set))))))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Cultivation c = Meditation.of(player);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "%s: realm %d stage %d, stage Qi %.1f / %d, Qi %.1f / %d, meridians %d, meditating %s, harmony %.2f",
                player.getScoreboardName(), c.realm(), c.stage(), c.stageQi(), c.gauge(), c.qi(), c.maxQi(),
                c.meridians(), c.meditating(), c.harmony())), false);
        return 1;
    }

    /** Fills the stage gauge and Qi. */
    private static int fill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Cultivation c = Meditation.of(player);
        c.set(c.realm(), c.stage(), c.gauge(), c.maxQi(), c.maxQi(), c.meridians());
        Meditation.sync(player);
        return get(ctx);
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Cultivation c = Meditation.of(player);
        c.set(IntegerArgumentType.getInteger(ctx, "realm"), IntegerArgumentType.getInteger(ctx, "stage"), 0,
                c.qi(), c.maxQi(), IntegerArgumentType.getInteger(ctx, "meridians"));
        Meditation.sync(player);
        return get(ctx);
    }
}
