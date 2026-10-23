package com.murimblock.cultivation;

import java.util.List;

/** The realm ladder: each Murim rank holds a nine-stage cultivation realm. Keys are translation keys. */
public final class CultivationRealms {
    public record Realm(String rank, String realm) {
    }

    public static final List<Realm> LADDER = List.of(
            new Realm("mortal", "body_tempering"),
            new Realm("third_rate_warrior", "qi_condensation"),
            new Realm("second_rate_warrior", "foundation_establishment"),
            new Realm("first_rate_warrior", "core_formation"),
            new Realm("peak_master", "unknown"),
            new Realm("transcendent", "unknown"),
            new Realm("unknown", "unknown"),
            new Realm("unknown", "unknown"));

    private CultivationRealms() {
    }

    public static String realmKey(int realm) {
        return LADDER.get(Math.clamp(realm, 0, LADDER.size() - 1)).realm();
    }
}
