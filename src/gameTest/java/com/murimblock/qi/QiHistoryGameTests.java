package com.murimblock.qi;

import com.murimblock.Murimblock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.player;
import static com.murimblock.testing.GameTestPlayers.cleanup;

@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QiHistoryGameTests {
    @GameTest(template = "foundation_arena", batch = "qi_history")
    public static void serverLifecycleClearsOnlyTransientRewards(GameTestHelper helper) {
        var player = player(helper);
        try {
            QiRewardManager.clearTransientHistory();
            QiService.setQi(player, 73);
            var zombie = QiRewardManager.rewardTarget(EntityType.ZOMBIE);
            for (int i = 0; i < 21; i++) QiRewardManager.calculateQiReward(player.getUUID(), QiBossProgress.initial(), zombie, i);
            QiEvents.onServerStopped(new ServerStoppedEvent(helper.getLevel().getServer()));
            helper.assertTrue(QiRewardManager.calculateQiReward(player.getUUID(), QiBossProgress.initial(), zombie, 0)
                    .recentKillCount() == 1, "Stopped server leaked anti-farm state into a new session");
            QiEvents.onServerStarted(new ServerStartedEvent(helper.getLevel().getServer()));
            helper.assertTrue(QiRewardManager.calculateQiReward(player.getUUID(), QiBossProgress.initial(), zombie, 0)
                    .recentKillCount() == 1, "Started server inherited anti-farm state");
            helper.assertTrue(QiService.getQi(player) == 73, "Clearing transient history changed saved Qi");
            helper.succeed();
        } finally {
            QiRewardManager.clearTransientHistory();
            cleanup(player);
        }
    }

    @GameTest(template = "foundation_arena", batch = "qi_history")
    public static void rewardHistoryFollowsThePlayerAcrossVanillaDimensions(GameTestHelper helper) {
        var player = player(helper);
        var server = helper.getLevel().getServer();
        var nether = server.getLevel(Level.NETHER);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        try {
            QiRewardManager.clearTransientHistory();
            QiRewardManager.calculateQiReward(player, target);
            // Only calculation is exercised: no second connected player or cross-world teleport is claimed.
            var dimensionPlayer = new ServerPlayer(server, nether, player.getGameProfile(), ClientInformation.createDefault());
            helper.assertTrue(QiRewardManager.calculateQiReward(dimensionPlayer, target).recentKillCount() == 2,
                    "Changing dimension reset the player's anti-farm history");
            helper.succeed();
        } finally {
            target.discard();
            QiRewardManager.clearTransientHistory();
            cleanup(player);
        }
    }
}
