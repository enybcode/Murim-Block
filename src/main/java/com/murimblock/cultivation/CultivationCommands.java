package com.murimblock.cultivation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/** {@code /murim cultivation get|fill|set|qi <player>} and {@code /murim prepare}, for operators and tests. */
final class CultivationCommands {
    private static final int PREPARED_QI = 10_000;

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
                                        .executes(CultivationCommands::set))))))
                        .then(Commands.literal("qi").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(CultivationCommands::qi)))))
                .then(Commands.literal("prepare").executes(CultivationCommands::prepare)));
    }

    /**
     * Test setup in one go for the caller: the first realm that holds every meridian, last stage, all meridians open,
     * a full gauge and 10 000 Qi; clear day, and no hostile mob near enough to stop meditation.
     */
    private static int prepare(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int realm = 0;
        while (CultivationRules.meridianCap(realm) < CultivationRules.MERIDIANS
                && realm < CultivationRealms.LADDER.size() - 1) {
            realm++;
        }
        Cultivation c = Meditation.of(player);
        c.set(realm, CultivationRules.STAGES, 0, PREPARED_QI, PREPARED_QI, CultivationRules.MERIDIANS);
        c.set(realm, CultivationRules.STAGES, c.gauge(), PREPARED_QI, PREPARED_QI, CultivationRules.MERIDIANS);
        Meditation.sync(player);
        ServerLevel level = player.serverLevel();
        level.setDayTime(1000);
        level.setWeatherParameters(6000, 0, false, false);
        level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m instanceof Enemy)
                .forEach(Entity::discard);
        ctx.getSource().sendSuccess(() -> Component.literal("Prepared: all meridians, full gauge, "
                + PREPARED_QI + " Qi, clear day, no hostile mob within 32 blocks. Press C to meditate."), false);
        return 1;
    }

    /** Sets the Qi held and its maximum, to see the Qi smoke at any size. */
    private static int qi(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Cultivation c = Meditation.of(player);
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        c.set(c.realm(), c.stage(), c.stageQi(), amount, amount, c.meridians());
        Meditation.sync(player);
        return get(ctx);
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Cultivation c = Meditation.of(player);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "%s: realm %d stage %d, max Qi %.1f (stage peak %d, +%.1f / %d), Qi %.1f, meridians %d, meditating %s, calm %.2f",
                player.getScoreboardName(), c.realm(), c.stage(), c.maxQiExact(), c.peak(), c.stageQi(), c.gauge(), c.qi(),
                c.meridians(), c.meditating(), c.calm())), false);
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
                c.qi(), 0, IntegerArgumentType.getInteger(ctx, "meridians"));
        Meditation.sync(player);
        return get(ctx);
    }
}
