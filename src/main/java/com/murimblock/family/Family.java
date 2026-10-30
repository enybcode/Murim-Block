package com.murimblock.family;

import java.util.Locale;

/**
 * The houses a player can be born into (docs/world/families.md): the great houses of the murim genre and ordinary
 * families. The ids ({@link #id()}) are saved: keep them. No inheritance is granted yet; each great house's is "coming
 * soon" and will be designed with Enzo one by one.
 */
public enum Family {
    JIN("陳", Rarity.LEGENDARY, "north_sky"),
    NAMGUNG("南宮", Rarity.LEGENDARY, "hubei_waterlands"),
    DOKGO("獨孤", Rarity.LEGENDARY, "western_snow_peaks"),
    MOYONG("慕容", Rarity.EPIC, "qinghai_plateau"),
    JEGAL("諸葛", Rarity.EPIC, "shandong_hills"),
    SAMA("司馬", Rarity.EPIC, "central_plains"),
    SEOMUN("西門", Rarity.EPIC, "southeast_coast"),
    DANG("唐", Rarity.RARE, "red_basin"),
    PAENG("彭", Rarity.RARE, "central_plains"),
    HWANGBO("皇甫", Rarity.RARE, "loess_plateau"),
    YEON("燕", Rarity.RARE, "frozen_steppe"),
    HA("夏", Rarity.RARE, "rice_terraces"),
    WANG("王", Rarity.COMMON, null),
    LI("李", Rarity.COMMON, null),
    ZHANG("张", Rarity.COMMON, null),
    LIU("刘", Rarity.COMMON, null),
    ZHAO("赵", Rarity.COMMON, null),
    SUN("孙", Rarity.COMMON, null),
    WU("吴", Rarity.COMMON, null),
    ZHOU("周", Rarity.COMMON, null);

    /** How rare a house is: its share of the draws, split evenly among its houses, and its colour. */
    public enum Rarity {
        COMMON(60, 0xFFC3CDD4, 0xFF7D8A94),
        RARE(25, 0xFF7FD6FF, 0xFF3FB4EC),
        EPIC(11, 0xFFD4B8FF, 0xFFB07CFF),
        LEGENDARY(4, 0xFFFFD36B, 0xFFC98D2A);

        public final int percent;
        /** Text colour and frame colour. */
        public final int text;
        public final int line;

        Rarity(int percent, int text, int line) {
            this.percent = percent;
            this.text = text;
            this.line = line;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The translation key of its name. */
        public String labelKey() {
            return "family.murimblock.rarity." + id();
        }

        /** The translation key of what a house of this rarity hands down (the specific inheritance is still coming soon). */
        public String inheritanceKey() {
            return "family.murimblock.inheritance." + id();
        }
    }

    /** The surname's characters, for the crest. */
    public final String characters;
    public final Rarity rarity;
    /** The biome id of its seat on the Murim map, or null for an ordinary family. */
    public final String seat;

    Family(String characters, Rarity rarity, String seat) {
        this.characters = characters;
        this.rarity = rarity;
        this.seat = seat;
    }

    /** The saved id. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The translation key of its name. */
    public String nameKey() {
        return "family.murimblock." + id();
    }

    public boolean great() {
        return rarity != Rarity.COMMON;
    }

    /** The house with this saved id, or null. */
    public static Family byId(String id) {
        for (Family f : values()) {
            if (f.id().equals(id)) {
                return f;
            }
        }
        return null;
    }
}
