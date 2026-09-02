package com.murimblock.qi;

import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Internal Qi implementation service used by Murimblock systems.
 *
 * <p>Addons should prefer {@code com.murimblock.api.MurimblockApi#qi()} so they do not depend on
 * attachments or other implementation details.</p>
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

    public static QiBossProgress getBossProgress(Player player) {
        return player.getData(QiAttachments.PLAYER_QI_BOSS_PROGRESS);
    }

    public static boolean markBossDefeated(ServerPlayer player, ResourceLocation bossId) {
        QiBossProgress current = getBossProgress(player);
        QiBossProgress updated = current.markDefeated(bossId);
        if (current.equals(updated)) {
            return false;
        }

        player.setData(QiAttachments.PLAYER_QI_BOSS_PROGRESS, updated);
        return true;
    }

    public static void resetBossProgress(ServerPlayer player) {
        player.setData(QiAttachments.PLAYER_QI_BOSS_PROGRESS, QiBossProgress.initial());
    }

    static boolean regenerate(ServerPlayer player, long elapsedTicks) {
        return regenerate(player, elapsedTicks, QiConstants.PASSIVE_REGENERATION_RATE_PER_MINUTE);
    }

    static boolean regenerate(ServerPlayer player, long elapsedTicks, double ratePerMinute) {
        return update(player, data -> data.regenerateForTicks(elapsedTicks, ratePerMinute));
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
