package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MurimblockCommandsTest {
    @Test
    void qiMaxChangesRequireOperatorPermission() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        MurimblockCommands.registerDevelopmentCommands(dispatcher);

        assertVisibleChildren("qimax", dispatcher, Set.of("check"));
        assertVisibleChildren("qimax", dispatcher, Set.of("check", "set", "add", "remove", "reset"), 2);
    }

    @Test
    void qiChangesRequireOperatorPermission() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        MurimblockCommands.registerDevelopmentCommands(dispatcher);

        assertVisibleChildren("qi", dispatcher, Set.of("check", "reward"));
        assertVisibleChildren("qi", dispatcher, Set.of("check", "set", "add", "remove", "refill", "reward"), 2);
    }

    @Test
    void combatDevelopmentCommandsAreVisibleWithoutPermissions() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        CombatCommands.register(dispatcher);

        assertVisibleChildren("combat", dispatcher, Set.of("check", "on", "off", "toggle"));
    }

    @Test
    void combatCommandTreeContainsOnlyTheRetainedModeControls() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CombatCommands.register(dispatcher);
        assertEquals(Set.of("check", "on", "off", "toggle"), dispatcher.getRoot()
                .getChild("combat").getChildren().stream().map(CommandNode::getName).collect(Collectors.toSet()));
    }

    private static void assertVisibleChildren(
            String commandName,
            CommandDispatcher<CommandSourceStack> dispatcher,
            Set<String> expected
    ) {
        assertVisibleChildren(commandName, dispatcher, expected, 0);
    }

    private static void assertVisibleChildren(String commandName, CommandDispatcher<CommandSourceStack> dispatcher,
                                             Set<String> expected, int permission) {
        CommandNode<CommandSourceStack> command = dispatcher.getRoot().getChild(commandName);

        assertNotNull(command);
        assertEquals(expected, command.getChildren().stream()
                .filter(child -> child.canUse(source(permission)))
                .map(CommandNode::getName)
                .collect(Collectors.toSet()));
    }

    private static CommandSourceStack source(int permission) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, permission,
                "test", Component.literal("test"), null, null);
    }
}
