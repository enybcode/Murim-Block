package com.murimblock.api.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public Combat API for Murimblock addons.
 *
 * <p>The combat mode is a temporary server-authoritative player state. It is not saved after logout
 * and is reset after death. Mutations require a {@link ServerPlayer}; addons should not use packets
 * or attachments directly.</p>
 */
public interface CombatApi {
    boolean isInCombatMode(Player player);

    boolean setCombatMode(ServerPlayer player, boolean enabled);

    boolean toggleCombatMode(ServerPlayer player);
}
