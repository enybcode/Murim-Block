package com.murimblock.combat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Operator commands for tuning combat: {@code /murim debug hitboxes [on|off]}. */
public final class CombatCommands {
    private CombatCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("murim").requires(s -> s.hasPermission(2))
                .then(Commands.literal("debug").then(Commands.literal("hitboxes")
                        .then(Commands.literal("on").executes(ctx -> hitboxes(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> hitboxes(ctx, false))))));
    }

    private static int hitboxes(CommandContext<CommandSourceStack> ctx, boolean on) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PacketDistributor.sendToPlayer(player, new CombatPayloads.DebugHitboxes(on));
        ctx.getSource().sendSuccess(() -> Component.literal("Hitboxes " + (on ? "shown" : "hidden")), false);
        return 1;
    }
}
