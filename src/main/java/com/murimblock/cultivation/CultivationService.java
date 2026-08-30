package com.murimblock.cultivation;

import com.murimblock.qi.QiService;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CultivationService {
    private CultivationService() {
    }

    public static CultivationData getCultivation(Player player) {
        return player.getData(CultivationAttachments.PLAYER_CULTIVATION);
    }

    public static CultivationRealm getRealm(Player player) {
        return getCultivation(player).realm();
    }

    public static CultivationStage getStage(Player player) {
        return getCultivation(player).stage();
    }

    public static Optional<CultivationData> getNextCultivation(Player player) {
        return CultivationProgression.getNext(getCultivation(player));
    }

    public static Optional<Double> getRequiredQiMaxForNextStage(Player player) {
        return CultivationProgression.getRequiredQiMaxForNext(getCultivation(player));
    }

    public static boolean canAttemptBreakthrough(Player player) {
        return CultivationProgression.canAttemptBreakthrough(getCultivation(player), QiService.getQiMax(player));
    }

    public static Optional<BreakthroughType> getBreakthroughType(Player player) {
        return CultivationProgression.getBreakthroughType(getCultivation(player));
    }

    public static boolean advanceAfterSuccessfulBreakthrough(ServerPlayer player) {
        return advance(player, false);
    }

    public static boolean forceAdvance(ServerPlayer player) {
        return advance(player, true);
    }

    public static void setCultivation(ServerPlayer player, CultivationRealm realm, CultivationStage stage) {
        player.setData(CultivationAttachments.PLAYER_CULTIVATION, new CultivationData(realm, stage));
    }

    public static void resetCultivation(ServerPlayer player) {
        player.setData(CultivationAttachments.PLAYER_CULTIVATION, CultivationProgression.reset());
    }

    private static boolean advance(ServerPlayer player, boolean ignoreQiMax) {
        CultivationData current = getCultivation(player);
        Optional<CultivationData> next = CultivationProgression.getNext(current);
        if (next.isEmpty()) {
            return false;
        }
        if (!ignoreQiMax && !CultivationProgression.canAttemptBreakthrough(current, QiService.getQiMax(player))) {
            return false;
        }
        player.setData(CultivationAttachments.PLAYER_CULTIVATION, next.get());
        return true;
    }
}
