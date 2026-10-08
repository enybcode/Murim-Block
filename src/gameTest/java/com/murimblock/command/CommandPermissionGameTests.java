package com.murimblock.command;

import com.murimblock.Murimblock;
import com.murimblock.qi.QiService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.player;
import static com.murimblock.testing.GameTestPlayers.cleanup;

@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CommandPermissionGameTests {
    @GameTest(template = "foundation_arena", batch = "command_permissions")
    public static void qiMutationCommandsRejectNonOperatorsButAllowOperators(GameTestHelper helper) {
        var player = player(helper);
        try {
            QiService.setQi(player, 20);
            var commands = helper.getLevel().getServer().getCommands();
            var unprivileged = player.createCommandSourceStack().withPermission(0);
            commands.performPrefixedCommand(unprivileged, "qi set 90");
            commands.performPrefixedCommand(unprivileged, "qi refill");
            commands.performPrefixedCommand(unprivileged, "qimax set 999");
            commands.performPrefixedCommand(unprivileged, "qimax reset");
            helper.assertTrue(QiService.getQi(player) == 20 && QiService.getQiMax(player) == 100,
                    "Non-operator command changed permanent Qi state");
            commands.performPrefixedCommand(unprivileged.withPermission(2), "qimax set 150");
            commands.performPrefixedCommand(unprivileged.withPermission(2), "qi set 90");
            helper.assertTrue(QiService.getQi(player) == 90 && QiService.getQiMax(player) == 150,
                    "Operator debug commands stopped working");
            helper.succeed();
        } finally { cleanup(player); }
    }
}
