package com.murimblock.api.qi;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public Qi contract for Murimblock addons.
 *
 * <p>Qi is the player's current usable energy. Qi Max is the player's current capacity and is allowed
 * to clamp current Qi through Murimblock's normal rules. Mutating methods are server-authoritative
 * and require a {@link ServerPlayer}; addons should not calculate or apply permanent Qi changes on
 * the logical client.</p>
 */
public interface QiApi {
    double getQi(Player player);

    double getQiMax(Player player);

    boolean setQi(ServerPlayer player, double value);

    boolean addQi(ServerPlayer player, double amount);

    boolean removeQi(ServerPlayer player, double amount);

    boolean setQiMax(ServerPlayer player, double value);

    boolean addQiMax(ServerPlayer player, double amount);

    boolean removeQiMax(ServerPlayer player, double amount);

    boolean refillQi(ServerPlayer player);
}
