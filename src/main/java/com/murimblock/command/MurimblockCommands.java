package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.murimblock.qi.QiConstants;
import com.murimblock.qi.QiService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MurimblockCommands {
    private static final int ADMIN_PERMISSION_LEVEL = 2;

    private MurimblockCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CultivationCommands.onRegisterCommands(event);
        registerQiMax(event.getDispatcher());
        registerQi(event.getDispatcher());
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
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .executes(context -> reset(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reset(context, EntityArgument.getPlayer(context, "player"))))));
    }

    private static void registerQi(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("qi")
                .then(Commands.literal("refill")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .executes(context -> refill(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> refill(context, EntityArgument.getPlayer(context, "player"))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> valueCommand(
            String name,
            BiFunction<ServerPlayer, Double, Boolean> operation
    ) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> applyValue(
                                context,
                                context.getSource().getPlayerOrException(),
                                DoubleArgumentType.getDouble(context, "value"),
                                operation
                        )))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                .executes(context -> applyValue(
                                        context,
                                        EntityArgument.getPlayer(context, "player"),
                                        DoubleArgumentType.getDouble(context, "value"),
                                        operation
                                ))));
    }

    private static int check(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean namedTarget) {
        String value = format(QiService.getQiMax(target));
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
            BiFunction<ServerPlayer, Double, Boolean> operation
    ) {
        operation.apply(target, value);
        sendQiMaxChanged(context.getSource(), target);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        QiService.setQiMax(target, QiConstants.INITIAL_QI_MAX);
        sendQiMaxChanged(context.getSource(), target);
        return 1;
    }

    private static int refill(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        QiService.refillQi(target);
        context.getSource().sendSuccess(
                () -> Component.literal("Qi de " + target.getGameProfile().getName() + " rempli à "
                        + format(QiService.getQi(target)) + " / " + format(QiService.getQiMax(target))),
                true
        );
        return 1;
    }

    private static void sendQiMaxChanged(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(
                () -> Component.literal("Qi Max de " + target.getGameProfile().getName() + " : "
                        + format(QiService.getQiMax(target)) + " (Qi : " + format(QiService.getQi(target)) + ")"),
                true
        );
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
