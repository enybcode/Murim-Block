package com.murimblock.dev;

import com.murimblock.Murimblock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Dev tool, off unless {@code murimblock.combatDuel} is {@code server} (run {@code gradlew runCombatDuelServer}, then
 * the two duel clients): a dedicated server that stands the two clients face to face on a platform, the attacker
 * ({@code Dev}) looking at the defender ({@code Dev2}), and stops once both have left.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class CombatDuelServer {
    private static final boolean ENABLED = "server".equals(System.getProperty("murimblock.combatDuel"));
    public static final String ATTACKER = "Dev";
    public static final String DEFENDER = "Dev2";
    private static final double DISTANCE = 2.2;
    private static boolean met;

    private CombatDuelServer() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer joined)) {
            return;
        }
        MinecraftServer server = joined.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < 2) {
            return;
        }
        met = true;
        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.overworld().setDayTime(1000);
        BlockPos center = new BlockPos(0, 200, 0);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -1, -6), center.offset(6, -1, 6))) {
            server.overworld().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        for (ServerPlayer player : players) {
            boolean attacker = player.getGameProfile().getName().equals(ATTACKER);
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            // Below 18 food nothing heals, so the health lost is exactly the blow.
            player.getFoodData().setFoodLevel(17);
            player.getFoodData().setSaturation(0);
            player.teleportTo(server.overworld(), 0.5, 200, attacker ? 0.5 : 0.5 + DISTANCE, attacker ? 0 : 180, 0);
        }
        Murimblock.LOGGER.info("Combat duel: {} and {} face to face", ATTACKER, DEFENDER);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ENABLED && met && event.getEntity() instanceof ServerPlayer left
                && left.getServer().getPlayerList().getPlayers().size() <= 1) {
            Murimblock.LOGGER.info("Combat duel: both left, stopping");
            left.getServer().halt(false);
        }
    }
}
