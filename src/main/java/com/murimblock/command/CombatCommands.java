package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.murimblock.combat.CombatService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CombatCommands {
    private CombatCommands() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combat")
                .then(Commands.literal("check")
                        .executes(context -> check(context, context.getSource().getPlayerOrException())))
                .then(Commands.literal("on")
                        .executes(context -> set(context, context.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("off")
                        .executes(context -> set(context, context.getSource().getPlayerOrException(), false)))
                .then(Commands.literal("toggle")
                        .executes(context -> toggle(context, context.getSource().getPlayerOrException()))));
    }

    private static int check(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        sendState(context, player);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context, ServerPlayer player, boolean enabled) {
        CombatService.setCombatMode(player, enabled);
        sendState(context, player);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CombatService.toggleCombatMode(player);
        sendState(context, player);
        return 1;
    }

    private static void sendState(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        Component message = Component.literal("Combat Mode : " + (CombatService.isInCombatMode(player) ? "ON" : "OFF"));
        context.getSource().sendSuccess(() -> message, false);
    }
}
