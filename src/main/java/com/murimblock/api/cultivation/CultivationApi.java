package com.murimblock.api.cultivation;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Public Cultivation contract for Murimblock addons.
 *
 * <p>The API exposes read-only snapshots for normal addon logic so callers do not depend on internal
 * attachments, codecs, or progression tables. Progression mutations remain deliberately narrow.</p>
 */
public interface CultivationApi {
    CultivationSnapshot getCultivation(Player player);

    Optional<CultivationSnapshot> getNextCultivation(Player player);

    boolean canAttemptBreakthrough(Player player);

    boolean advanceAfterSuccessfulBreakthrough(ServerPlayer player);
}
