package com.murimblock.combat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CombatEvents {
    private CombatEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setData(CombatAttachments.PLAYER_COMBAT, CombatData.initial());
            MeleeCombatService.clear(player);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatService.resetCombatMode(player);
            MeleeCombatService.clear(player);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setData(CombatAttachments.PLAYER_COMBAT, CombatData.initial());
            MeleeCombatService.clear(player);
        }
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MeleeCombatService.clear(player);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MeleeCombatService.tick(event.getServer());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        MeleeCombatService.clearAll();
    }
}
