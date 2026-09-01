package com.murimblock.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MurimblockCommandsTest {
    @Test
    void qiMaxDevelopmentCommandsAreVisibleWithoutPermissions() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        MurimblockCommands.registerDevelopmentCommands(dispatcher);

        assertVisibleChildren("qimax", dispatcher, Set.of("check", "set", "add", "remove", "reset"));
    }

    @Test
    void qiDevelopmentCommandsAreVisibleWithoutPermissions() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        MurimblockCommands.registerDevelopmentCommands(dispatcher);

        assertVisibleChildren("qi", dispatcher, Set.of("check", "set", "add", "remove", "refill"));
    }

    private static void assertVisibleChildren(
            String commandName,
            CommandDispatcher<CommandSourceStack> dispatcher,
            Set<String> expected
    ) {
        CommandNode<CommandSourceStack> command = dispatcher.getRoot().getChild(commandName);

        assertNotNull(command);
        assertEquals(expected, command.getChildren().stream()
                .filter(child -> child.canUse(null))
                .map(CommandNode::getName)
                .collect(Collectors.toSet()));
    }
}
