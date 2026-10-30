package com.murimblock.family;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What a house hands down (docs/world/families.md): attribute bonuses held while the player belongs to it, items given
 * once, and a martial art. Every great house's is still {@link #COMING_SOON} and common houses hand down
 * {@link #NONE}; the houses are designed later, the system that grants them is ready ({@code InheritanceGrants}).
 *
 * @param comingSoon shown as "Coming soon" and grants nothing
 * @param stats      attribute bonuses, by attribute id ({@code minecraft:generic.armor}...)
 * @param items      items given once, by item id
 * @param art        the id of the martial art taught, or null
 */
public record Inheritance(boolean comingSoon, List<Stat> stats, List<Gift> items, String art) {
    public static final Inheritance COMING_SOON = new Inheritance(true, List.of(), List.of(), null);
    public static final Inheritance NONE = new Inheritance(false, List.of(), List.of(), null);

    /** An attribute bonus: added to the base, or multiplying the total ({@code multiply}: 0.1 = +10 %). */
    public record Stat(String attribute, double amount, boolean multiply) {
    }

    public record Gift(String item, int count) {
    }

    public boolean empty() {
        return stats.isEmpty() && items.isEmpty() && art == null;
    }

    /** Each house's inheritance; a dev check may put a test one in place of the real ones ({@link #override}). */
    private static final Map<Family, Inheritance> OVERRIDES = new EnumMap<>(Family.class);

    public static Inheritance of(Family house) {
        Inheritance test = OVERRIDES.get(house);
        if (test != null) {
            return test;
        }
        return house.great() ? COMING_SOON : NONE;
    }

    /** Dev checks: every house hands down {@code test} instead (null puts the real ones back). */
    public static void override(Inheritance test) {
        OVERRIDES.clear();
        if (test != null) {
            for (Family f : Family.values()) {
                OVERRIDES.put(f, test);
            }
        }
    }
}
