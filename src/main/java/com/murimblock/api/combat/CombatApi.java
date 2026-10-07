package com.murimblock.api.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public HUD/GUI mode API for Murimblock addons.
 *
 * <p>The combat mode is a temporary server-authoritative player state. It is not saved after logout
 * and is reset after death. Mutations require a {@link ServerPlayer}; addons should not use packets
 * or attachments directly.</p>
 *
 * <p>This flag does not intercept attacks and is not Epic Fight's battle mode.</p>
 */
public interface CombatApi {
    boolean isInCombatMode(Player player);

    boolean setCombatMode(ServerPlayer player, boolean enabled);

    boolean toggleCombatMode(ServerPlayer player);
}
