package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.murimblock.qi.QiConstants;
import com.murimblock.qi.QiFormat;
import com.murimblock.qi.QiRewardManager;
import com.murimblock.qi.QiService;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MurimblockCommands {
    private MurimblockCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CultivationCommands.onRegisterCommands(event);
        registerDevelopmentCommands(event.getDispatcher());
    }

    static void registerDevelopmentCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerQiMax(dispatcher);
        registerQi(dispatcher);
    }

    private static void registerQiMax(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("qimax")
                .then(Commands.literal("check")
                        .executes(context -> check(context, context.getSource().getPlayerOrException(), false))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> check(context, EntityArgument.getPlayer(context, "player"), true))))
                .then(valueCommand("set", QiService::setQiMax))
                .then(valueCommand("add", QiService::addQiMax))
                .then(valueCommand("remove", QiService::removeQiMax))
                .then(Commands.literal("reset")
                        .executes(context -> reset(context, context.getSource().getPlayerOrException(), false))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reset(context, EntityArgument.getPlayer(context, "player"), true)))));
    }

    private static void registerQi(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("qi")
                .then(Commands.literal("check")
                        .executes(context -> checkQi(context, context.getSource().getPlayerOrException(), false))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> checkQi(context, EntityArgument.getPlayer(context, "player"), true))))
                .then(qiValueCommand("set", QiService::setQi))
                .then(qiValueCommand("add", QiService::addQi))
                .then(qiValueCommand("remove", QiService::removeQi))
                .then(Commands.literal("refill")
                        .executes(context -> refill(context, context.getSource().getPlayerOrException(), false))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> refill(context, EntityArgument.getPlayer(context, "player"), true))))
                .then(Commands.literal("reward")
                        .then(Commands.literal("check")
                                .executes(context -> checkReward(context, context.getSource().getPlayerOrException())))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> resetRewardData(context, context.getSource().getPlayerOrException())))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> valueCommand(
            String name,
            BiFunction<ServerPlayer, Double, Boolean> operation
    ) {
        return Commands.literal(name)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> applyValue(
                                context,
                                context.getSource().getPlayerOrException(),
                                DoubleArgumentType.getDouble(context, "value"),
                                operation,
                                false
                        )))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                .executes(context -> applyValue(
                                        context,
                                        EntityArgument.getPlayer(context, "player"),
                                        DoubleArgumentType.getDouble(context, "value"),
                                        operation,
                                        true
                                ))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> qiValueCommand(
            String name,
            BiFunction<ServerPlayer, Double, Boolean> operation
    ) {
        return Commands.literal(name)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> applyQiValue(
                                context,
                                context.getSource().getPlayerOrException(),
                                DoubleArgumentType.getDouble(context, "value"),
                                operation,
                                false
                        )))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                .executes(context -> applyQiValue(
                                        context,
                                        EntityArgument.getPlayer(context, "player"),
                                        DoubleArgumentType.getDouble(context, "value"),
                                        operation,
                                        true
                                ))));
    }

    private static int check(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean namedTarget) {
        String value = QiFormat.format(QiService.getQiMax(target));
        Component message = namedTarget
                ? Component.literal("Qi Max de " + target.getGameProfile().getName() + " : " + value)
                : Component.literal("Qi Max : " + value);
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    private static int applyValue(
            CommandContext<CommandSourceStack> context,
            ServerPlayer target,
            double value,
            BiFunction<ServerPlayer, Double, Boolean> operation,
            boolean namedTarget
    ) {
        operation.apply(target, value);
        sendQiMaxChanged(context.getSource(), target, namedTarget);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean namedTarget) {
        QiService.setQiMax(target, QiConstants.INITIAL_QI_MAX);
        sendQiMaxChanged(context.getSource(), target, namedTarget);
        return 1;
    }

    private static int checkQi(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean namedTarget) {
        sendQiState(context.getSource(), target, namedTarget);
        return 1;
    }

    private static int applyQiValue(
            CommandContext<CommandSourceStack> context,
            ServerPlayer target,
            double value,
            BiFunction<ServerPlayer, Double, Boolean> operation,
            boolean namedTarget
    ) {
        operation.apply(target, value);
        sendQiState(context.getSource(), target, namedTarget);
        return 1;
    }

    private static int refill(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean namedTarget) {
        QiService.refillQi(target);
        sendQiState(context.getSource(), target, namedTarget);
        return 1;
    }

    private static int checkReward(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Component message = Component.literal(QiRewardManager.describeAntiFarm(player, EntityType.ZOMBIE));
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    private static int resetRewardData(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        QiRewardManager.resetDevelopmentData(player);
        context.getSource().sendSuccess(() -> Component.literal("Donnees de recompense Qi reinitialisees."), true);
        return 1;
    }

    private static void sendQiMaxChanged(CommandSourceStack source, ServerPlayer target, boolean namedTarget) {
        String value = QiFormat.format(QiService.getQiMax(target));
        Component message = namedTarget
                ? Component.literal("Qi Max de " + target.getGameProfile().getName() + " = " + value
                        + " (Qi : " + QiFormat.format(QiService.getQi(target)) + ")")
                : Component.literal("Qi Max = " + value);
        source.sendSuccess(() -> message, true);
    }

    private static void sendQiState(CommandSourceStack source, ServerPlayer target, boolean namedTarget) {
        String value = QiFormat.format(QiService.getQi(target)) + " / " + QiFormat.format(QiService.getQiMax(target));
        Component message = namedTarget
                ? Component.literal("Qi de " + target.getGameProfile().getName() + " : " + value)
                : Component.literal("Qi : " + value);
        source.sendSuccess(
                () -> message,
                true
        );
    }
}
