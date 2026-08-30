package com.murimblock.qi.charge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class QiChargeEvents {
    private QiChargeEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QiChargeService.keepValid(player);
            if (QiChargeService.isCharging(player) && QiChargeParticleEffects.shouldSpawnWorldParticles(player.tickCount)) {
                QiChargeParticleEffects.spawnWorldParticles(player);
            }
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        QiChargeService.stopIfServerPlayer(event.getEntity());
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        QiChargeService.stopIfServerPlayer(event.getEntity());
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        QiChargeService.stopIfServerPlayer(event.getOriginal());
        QiChargeService.stopIfServerPlayer(event.getEntity());
    }
}
