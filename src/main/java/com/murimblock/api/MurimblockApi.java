package com.murimblock.api;

import com.murimblock.api.cultivation.CultivationApi;
import com.murimblock.api.cultivation.CultivationSnapshot;
import com.murimblock.api.qi.QiApi;
import com.murimblock.cultivation.CultivationData;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.qi.QiService;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Stable entry point for addons that want to interact with Murimblock systems.
 *
 * <p>During the Murimblock 0.x series this API is intentionally small and may still receive breaking changes.
 * Addons should prefer this class over implementation packages such as {@code com.murimblock.qi},
 * {@code com.murimblock.cultivation}, {@code com.murimblock.network}, or {@code com.murimblock.client}.</p>
 */
public final class MurimblockApi {
    private static final QiApi QI = new DefaultQiApi();
    private static final CultivationApi CULTIVATION = new DefaultCultivationApi();

    private MurimblockApi() {
    }

    /**
     * Returns the public Qi API for reading and changing server-authoritative Qi state.
     */
    public static QiApi qi() {
        return QI;
    }

    /**
     * Returns the public Cultivation API for reading progression state.
     */
    public static CultivationApi cultivation() {
        return CULTIVATION;
    }

    private static final class DefaultQiApi implements QiApi {
        @Override
        public double getQi(Player player) {
            return QiService.getQi(player);
        }

        @Override
        public double getQiMax(Player player) {
            return QiService.getQiMax(player);
        }

        @Override
        public boolean setQi(ServerPlayer player, double value) {
            return QiService.setQi(player, value);
        }

        @Override
        public boolean addQi(ServerPlayer player, double amount) {
            return QiService.addQi(player, amount);
        }

        @Override
        public boolean removeQi(ServerPlayer player, double amount) {
            return QiService.removeQi(player, amount);
        }

        @Override
        public boolean setQiMax(ServerPlayer player, double value) {
            return QiService.setQiMax(player, value);
        }

        @Override
        public boolean addQiMax(ServerPlayer player, double amount) {
            return QiService.addQiMax(player, amount);
        }

        @Override
        public boolean removeQiMax(ServerPlayer player, double amount) {
            return QiService.removeQiMax(player, amount);
        }

        @Override
        public boolean refillQi(ServerPlayer player) {
            return QiService.refillQi(player);
        }
    }

    private static final class DefaultCultivationApi implements CultivationApi {
        @Override
        public CultivationSnapshot getCultivation(Player player) {
            return toSnapshot(CultivationService.getCultivation(player));
        }

        @Override
        public Optional<CultivationSnapshot> getNextCultivation(Player player) {
            return CultivationService.getNextCultivation(player).map(DefaultCultivationApi::toSnapshot);
        }

        @Override
        public boolean canAttemptBreakthrough(Player player) {
            return CultivationService.canAttemptBreakthrough(player);
        }

        @Override
        public boolean advanceAfterSuccessfulBreakthrough(ServerPlayer player) {
            return CultivationService.advanceAfterSuccessfulBreakthrough(player);
        }

        private static CultivationSnapshot toSnapshot(CultivationData data) {
            return new CultivationSnapshot(
                    data.realm().serializedName(),
                    data.realm().displayName(),
                    data.realm().frenchName(),
                    data.realm().status(),
                    data.stage().serializedName(),
                    data.stage().displayName(),
                    data.displayName()
            );
        }
    }
}
