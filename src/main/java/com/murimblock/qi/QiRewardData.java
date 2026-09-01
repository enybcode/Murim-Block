package com.murimblock.qi;

import net.minecraft.resources.ResourceLocation;

public record QiRewardData(
        ResourceLocation entityTypeId,
        int baseReward,
        int firstVictoryBonus,
        double antiFarmMultiplier,
        int recentKillCount,
        int finalReward,
        boolean firstVictory,
        boolean repeatedBossReward
) {
}
