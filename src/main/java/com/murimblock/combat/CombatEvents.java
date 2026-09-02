package com.murimblock.combat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class CombatEvents {
    private CombatEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setData(CombatAttachments.PLAYER_COMBAT, CombatData.initial());
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatService.resetCombatMode(player);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setData(CombatAttachments.PLAYER_COMBAT, CombatData.initial());
        }
    }
}
