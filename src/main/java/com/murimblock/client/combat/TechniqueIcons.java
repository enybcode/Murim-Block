package com.murimblock.client.combat;

import java.util.Map;

/**
 * The techniques' pixel icons for the combat bar, in the board's palette letters (HudPixels.color; rules in
 * docs/combat/combat-system.md, "Look B"): 7 x 7 for a technique's seal, 11 x 11 for an ultimate's octagon. A technique
 * without one shows its initials.
 */
final class TechniqueIcons {
    static final String[] LOCK = {"..sss..", ".s...s.", ".s...s.", "kkkkkkk", "kss.ssk", "ksssssk", "kkkkkkk"};

    private static final Map<String, String[]> ICONS = Map.of(
            // Flowing River Sword.
            "river_thrust", new String[] {"......w", ".....wl", "....wl.", "..cwl..", "..yc...", ".y..c..", "G......"},
            "whirlpool_cut", new String[] {"..lll..", ".l...l.", "l..c..l", "l.cwc.l", "l..c..l", ".l...c.", "..lll.."},
            "waterfall_descent", new String[] {"...w...", "c..l..c", "l.yGy.l", "c..l..c", "l..l..l", ".c.w.c.", "..lcl.."},
            "thousand_streams", new String[] {
                "y....y....y", ".y...y...y.", "..y..Y..y..", "...yYYYy...", "....YwY....", "yyYYwwwYYyy",
                "....YwY....", "...yYYYy...", "..y..Y..y..", ".y...y...y.", "y....y....y"});

    private TechniqueIcons() {
    }

    /** The icon of the technique {@code id}, or null. */
    static String[] of(String id) {
        return ICONS.get(id);
    }
}
