package com.murimblock.api.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public combat-mode bridge for Murimblock addons.
 *
 * <p>Reads reflect Epic Fight's actual player mode; its engine owns persistence and lifecycle.
 * Mutations require a {@link ServerPlayer} and respect the engine's mode-switch gamerule.
 * Addons should not use packets or the temporary Murimblock HUD mirror directly.</p>
 *
 * <p>Murimblock does not add a second attack or damage engine.</p>
 */
public interface CombatApi {
    boolean isInCombatMode(Player player);

    boolean setCombatMode(ServerPlayer player, boolean enabled);

    boolean toggleCombatMode(ServerPlayer player);
}
