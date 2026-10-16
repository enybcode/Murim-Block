package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.murimblock.world.SpawnProtection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Operator controls for WorldEdit imports and player-made buildings; coordinates never load or edit chunks. */
public final class SpawnProtectionCommands {
    private SpawnProtectionCommands() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("murimspawn").requires(s -> s.hasPermission(2))
                .then(Commands.literal("protect").then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos()).executes(c -> protect(c.getSource(),
                                        StringArgumentType.getString(c, "name"), BlockPosArgument.getBlockPos(c, "from"), BlockPosArgument.getBlockPos(c, "to")))))))
                .then(Commands.literal("protecthere").then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512)).executes(c -> {
                            var source = c.getSource(); var level = source.getLevel();
                            BlockPos centre = BlockPos.containing(source.getPosition()); int r = IntegerArgumentType.getInteger(c, "radius");
                            return protect(source, StringArgumentType.getString(c, "name"),
                                    new BlockPos(centre.getX() - r, level.getMinBuildHeight(), centre.getZ() - r),
                                    new BlockPos(centre.getX() + r, level.getMaxBuildHeight() - 1, centre.getZ() + r));
                        }))))
                .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SpawnProtection.get(c.getSource().getLevel()).regions().keySet(), b))
                        .executes(c -> {
                            String name = StringArgumentType.getString(c, "name");
                            if (!SpawnProtection.get(c.getSource().getLevel()).remove(name)) {
                                c.getSource().sendFailure(Component.translatable("command.murimblock.spawn.missing", name)); return 0;
                            }
                            c.getSource().sendSuccess(() -> Component.translatable("command.murimblock.spawn.removed", name), true); return 1;
                        })))
                .then(Commands.literal("list").executes(c -> {
                    var regions = SpawnProtection.get(c.getSource().getLevel()).regions();
                    c.getSource().sendSuccess(() -> Component.translatable("command.murimblock.spawn.list", regions.size()), false);
                    regions.forEach((name, b) -> c.getSource().sendSuccess(() -> Component.translatable("command.murimblock.spawn.region",
                            name, b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()), false));
                    return regions.size();
                })));
    }
    private static int protect(CommandSourceStack source, String name, BlockPos first, BlockPos second) {
        SpawnProtection.get(source.getLevel()).protect(name, first, second);
        source.sendSuccess(() -> Component.translatable("command.murimblock.spawn.protected", name), true); return 1;
    }
}
