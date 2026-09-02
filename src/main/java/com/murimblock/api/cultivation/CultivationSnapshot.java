package com.murimblock.api.cultivation;

/**
 * Immutable public view of a player's cultivation state.
 */
public record CultivationSnapshot(
        String realmId,
        String realmName,
        String realmFrenchName,
        String realmStatus,
        String stageId,
        String stageName,
        String displayName
) {
}
