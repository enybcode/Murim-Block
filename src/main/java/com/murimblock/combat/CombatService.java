package com.murimblock.combat;

import com.murimblock.api.combat.CombatModeChangedEvent;
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
        return getData(player).combatMode();
    }

    public static boolean toggleCombatMode(ServerPlayer player) {
        return setCombatMode(player, !isInCombatMode(player));
    }

    public static boolean setCombatMode(ServerPlayer player, boolean enabled) {
        if (!canUseCombatMode(player)) {
            enabled = false;
        }

        CombatData current = getData(player);
        CombatData updated = current.withCombatMode(enabled);
        if (current.equals(updated)) {
            return false;
        }

        player.setData(CombatAttachments.PLAYER_COMBAT, updated);
        NeoForge.EVENT_BUS.post(new CombatModeChangedEvent(player, enabled));
        sendActionBar(player, enabled);
        return true;
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
