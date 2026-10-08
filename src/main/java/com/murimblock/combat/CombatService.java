package com.murimblock.combat;

import com.murimblock.api.combat.CombatModeChangedEvent;
import com.murimblock.integration.epicfight.EpicFightBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Internal server-authoritative combat mode service.
 *
 * <p>Addons should prefer {@code com.murimblock.api.MurimblockApi#combat()}.</p>
 */
public final class CombatService {
    private CombatService() {
    }

    public static CombatData getData(Player player) {
        return player.getData(CombatAttachments.PLAYER_COMBAT);
    }

    public static boolean isInCombatMode(Player player) {
        return EpicFightBridge.isCombatMode(player);
    }

    public static boolean toggleCombatMode(ServerPlayer player) {
        return setCombatMode(player, !isInCombatMode(player));
    }

    public static boolean setCombatMode(ServerPlayer player, boolean enabled) {
        if (!canUseCombatMode(player)) {
            enabled = false;
        }

        boolean changed = EpicFightBridge.setMode(player, enabled);
        refreshMode(player);
        if (changed) sendActionBar(player, isInCombatMode(player));
        return changed;
    }

    public static void refreshMode(ServerPlayer player) {
        CombatData current = getData(player);
        CombatData updated = current.withCombatMode(isInCombatMode(player));
        if (current.equals(updated)) {
            return;
        }

        player.setData(CombatAttachments.PLAYER_COMBAT, updated);
        NeoForge.EVENT_BUS.post(new CombatModeChangedEvent(player, updated.combatMode()));
    }

    public static boolean resetCombatMode(ServerPlayer player) {
        return setCombatMode(player, false);
    }

    static boolean canUseCombatMode(Player player) {
        return player.isAlive() && !player.isSpectator();
    }

    private static void sendActionBar(ServerPlayer player, boolean enabled) {
        Component message = Component.translatable(enabled
                ? "message.murimblock.combat_mode.on"
                : "message.murimblock.combat_mode.off");
        player.displayClientMessage(message, true);
    }
}
