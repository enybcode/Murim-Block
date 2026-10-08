package com.murimblock.qi;

import com.murimblock.qi.charge.QiChargeService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class QiEvents {
    private QiEvents() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        QiRewardManager.clearTransientHistory();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        QiRewardManager.clearTransientHistory();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();
        if (gameTime % 200 == 0) QiRewardManager.cleanupTransientHistory(gameTime);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QiService.getData(player);
            QiService.getBossProgress(player);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % QiConstants.REGENERATION_INTERVAL_TICKS == 0) {
            double rate = QiChargeService.isCharging(player)
                    ? QiConstants.ACTIVE_CHARGE_REGENERATION_RATE_PER_MINUTE
                    : QiConstants.PASSIVE_REGENERATION_RATE_PER_MINUTE;
            QiService.regenerate(player, QiConstants.REGENERATION_INTERVAL_TICKS, rate);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        ServerPlayer killer = findResponsiblePlayer(event);
        if (killer == null || killer == event.getEntity()) {
            return;
        }

        QiRewardManager.awardKillReward(killer, event.getEntity());
    }

    private static ServerPlayer findResponsiblePlayer(LivingDeathEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof ServerPlayer player) {
            return player;
        }
        if (event.getEntity().getKillCredit() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }
}
