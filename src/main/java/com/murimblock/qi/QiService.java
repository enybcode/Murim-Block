package com.murimblock.qi;

import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public API for reading Qi and for performing server-authoritative mutations.
 */
public final class QiService {
    private QiService() {
    }

    public static QiData getData(Player player) {
        return player.getData(QiAttachments.PLAYER_QI);
    }

    public static double getQi(Player player) {
        return getData(player).qi();
    }

    public static double getQiMax(Player player) {
        return getData(player).qiMax();
    }

    public static boolean setQi(ServerPlayer player, double value) {
        return update(player, data -> data.withQi(value));
    }

    public static boolean addQi(ServerPlayer player, double amount) {
        return update(player, data -> data.addQi(amount));
    }

    public static boolean removeQi(ServerPlayer player, double amount) {
        return update(player, data -> data.removeQi(amount));
    }

    public static boolean setQiMax(ServerPlayer player, double value) {
        return update(player, data -> data.withQiMax(value));
    }

    public static boolean addQiMax(ServerPlayer player, double amount) {
        return update(player, data -> data.addQiMax(amount));
    }

    public static boolean removeQiMax(ServerPlayer player, double amount) {
        return update(player, data -> data.removeQiMax(amount));
    }

    public static boolean refillQi(ServerPlayer player) {
        return update(player, QiData::refill);
    }

    static boolean regenerate(ServerPlayer player, long elapsedTicks) {
        return update(player, data -> data.regenerateForTicks(elapsedTicks));
    }

    private static boolean update(ServerPlayer player, UnaryOperator<QiData> operation) {
        QiData current = getData(player);
        QiData updated = operation.apply(current);
        if (current.equals(updated)) {
            return false;
        }

        player.setData(QiAttachments.PLAYER_QI, updated);
        return true;
    }
}
