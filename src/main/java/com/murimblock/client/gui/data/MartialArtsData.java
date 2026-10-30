package com.murimblock.client.gui.data;

import com.murimblock.client.combat.TechniqueKeys;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * The martial arts the player knows. Only one flows through the meridians at a time; the others can be
 * switched to outside combat. Placeholder data (the mockup's example arts) until martial arts exist.
 */
public final class MartialArtsData {
    public enum Kind {
        PASSIVE, TECHNIQUE, ULTIMATE;

        public Component label() {
            return Component.translatable("gui.murimblock.technique.kind." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public record Technique(String id, Kind kind, int qiCost, int cooldownSeconds, int unlockMastery) {
        public Component name() {
            return Component.translatable("murimblock.technique." + id);
        }

        public Component description() {
            return Component.translatable("murimblock.technique." + id + ".desc");
        }
    }

    public record Art(String id, String weapon, String rank, int masteryPercent, List<Technique> techniques) {
        public Component name() {
            return Component.translatable("murimblock.art." + id);
        }

        public Component weaponName() {
            return Component.translatable("murimblock.weapon." + weapon);
        }
    }

    private static final List<Art> ARTS = List.of(
            new Art("flowing_river_sword", "sword", "C", 42, List.of(
                    new Technique("calm_current", Kind.PASSIVE, 0, 0, 0),
                    new Technique("river_thrust", Kind.TECHNIQUE, 10, 4, 0),
                    new Technique("whirlpool_cut", Kind.TECHNIQUE, 18, 8, 25),
                    new Technique("waterfall_descent", Kind.TECHNIQUE, 25, 12, 50),
                    new Technique("thousand_streams", Kind.ULTIMATE, 60, 60, 100))),
            new Art("iron_mountain_fist", "bare_hands", "D", 15, List.of(
                    new Technique("rooted_stance", Kind.PASSIVE, 0, 0, 0),
                    new Technique("mountain_breaker", Kind.TECHNIQUE, 12, 5, 0),
                    new Technique("stone_skin", Kind.TECHNIQUE, 15, 15, 25),
                    new Technique("avalanche", Kind.ULTIMATE, 50, 60, 100))),
            new Art("wandering_staff", "staff", "E", 3, List.of(
                    new Technique("long_reach", Kind.PASSIVE, 0, 0, 0),
                    new Technique("sweeping_arc", Kind.TECHNIQUE, 10, 4, 0))));

    /** Index of the active art; kept for the client session only, like the rest of this placeholder. */
    private static int active;

    private MartialArtsData() {
    }

    public static List<Art> known() {
        return ARTS;
    }

    public static Art active() {
        return ARTS.get(active);
    }

    public static void activate(Art art) {
        int index = ARTS.indexOf(art);
        if (index >= 0) {
            active = index;
        }
    }

    /** Arts cannot be switched during combat. Combat detection does not exist yet, so never. */
    public static boolean inCombat() {
        return false;
    }

    /** The key shown before a technique, as bound in Controls: none for the passive, the ultimate key, the n-th technique key. */
    public static String keyOf(Art art, Technique technique) {
        if (technique.kind() == Kind.PASSIVE) {
            return "-";
        }
        if (technique.kind() == Kind.ULTIMATE) {
            return TechniqueKeys.ultimateLabel();
        }
        int n = 0;
        for (Technique t : art.techniques()) {
            if (t.kind() == Kind.TECHNIQUE) {
                n++;
            }
            if (t == technique) {
                break;
            }
        }
        return TechniqueKeys.label(n - 1);
    }
}
