package com.murimblock.combat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Temporary combat state for one player.
 */
public record CombatData(boolean combatMode) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatData> STREAM_CODEC = ByteBufCodecs.BOOL
            .map(CombatData::new, CombatData::combatMode)
            .cast();

    public static CombatData initial() {
        return new CombatData(false);
    }

    public CombatData toggle() {
        return new CombatData(!combatMode);
    }

    public CombatData withCombatMode(boolean enabled) {
        return combatMode == enabled ? this : new CombatData(enabled);
    }
}
