package com.murimblock.client.gui;

import java.util.Map;

/**
 * The techniques' pixel icons for the combat bar, in the board's palette letters (PixelArt.color; rules in
 * docs/combat/combat-system.md, "Look B"): 7 x 7 for a technique's seal, 11 x 11 for an ultimate's octagon. A technique
 * without one shows its initials.
 */
public final class TechniqueIcons {
    public static final String[] LOCK = {"..sss..", ".s...s.", ".s...s.", "kkkkkkk", "kss.ssk", "ksssssk", "kkkkkkk"};

    /** An art's crest by its weapon, 11 x 11, for the System menu. */
    private static final Map<String, String[]> CRESTS = Map.of(
            "sword", new String[] {
                ".....w.....", ".....l.....", "....wlw....", ".....l.....", "..y..l..y..", "...yGGGy...",
                "..y..c..y..", ".....c.....", ".....c.....", "....ccc....", ".....c....."},
            "bare_hands", new String[] {
                "...........", "..lll.lll..", ".lwwl.lwwl.", ".lwwlllwwl.", ".lwwwwwwwl.", ".lccccccl..",
                "..lccccl...", "...lccl....", "...lccl....", "...yyyy....", "..........."},
            "staff", new String[] {
                ".........yw", "........yl.", ".......cl..", "......cl...", ".....cl....", "....cl.....",
                "...cl......", "..cl.......", ".yl........", "wy.........", "..........."});
    private static final String[] CREST = {
        "...lllll...", "..l.....l..", ".l..ccc..l.", "l..c...c..l", "l.c..w..c.l", "l.c.w.w.c.l",
        "l.c..w.c..l", "l..c...c...", ".l..ccc..c.", "..l.....c..", "...lllll..."};

    /** The crest of an art wielding {@code weapon}: a weapon's own, or a Qi swirl. */
    public static String[] crest(String weapon) {
        return CRESTS.getOrDefault(weapon, CREST);
    }

    private static final Map<String, String[]> ICONS = Map.of(
            // Flowing River Sword.
            "calm_current", new String[] {".......", "lllll..", ".....l.", ".cccc.l", "......l", "wwwww..", "......."},
            "river_thrust", new String[] {"......w", ".....wl", "....wl.", "..cwl..", "..yc...", ".y..c..", "G......"},
            "whirlpool_cut", new String[] {"..lll..", ".l...l.", "l..c..l", "l.cwc.l", "l..c..l", ".l...c.", "..lll.."},
            "waterfall_descent", new String[] {"...w...", "c..l..c", "l.yGy.l", "c..l..c", "l..l..l", ".c.w.c.", "..lcl.."},
            "thousand_streams", new String[] {
                "y....y....y", ".y...y...y.", "..y..Y..y..", "...yYYYy...", "....YwY....", "yyYYwwwYYyy",
                "....YwY....", "...yYYYy...", "..y..Y..y..", ".y...y...y.", "y....y....y"});

    private TechniqueIcons() {
    }

    /** The icon of the technique {@code id}, or null. */
    public static String[] of(String id) {
        return ICONS.get(id);
    }
}
