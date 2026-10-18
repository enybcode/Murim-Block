package com.murimblock.cultivation.inner;

import com.murimblock.martial.MartialArts;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The inner world of each martial art (docs/cultivation/inner-worlds.md, Enzo 19/10/2026): the island takes the look
 * of its cultivator's main style and of its element, its sky the element's colour. Without a main style, the plain
 * island of grass and pines.
 */
public enum InnerTheme {
    PLAIN(null, 0x14264F),
    /** Wind and clouds: a sea of clouds, a pavilion, swords planted in the rock, an azure dawn. */
    AZURE_CLOUD(MartialArts.AZURE_CLOUD_SWORD, 0x5E9BD8),
    /** Earth and the body: a training yard on a plateau, dummies, weight stones, banners, a sunset. */
    LONG_FIST(MartialArts.LONG_FIST, 0xC8784A),
    /** Metal: a temple courtyard veined with gold, a bronze bell in its tower, golden trees, a golden glow. */
    GOLDEN_BELL(MartialArts.GOLDEN_BELL, 0xC89A3C),
    /** The mountain: three snowy peaks, spear racks and the Alliance's banners, a pure high sky. */
    CELESTIAL_PEAK(MartialArts.CELESTIAL_PEAK_SPEAR, 0x8FB8E0),
    /** Wood and the road: a meadow crossed by a winding path, an old tree, a wayside shrine, lanterns, a mild evening. */
    TRAVELLER_STAFF(MartialArts.TRAVELLER_STAFF, 0xB07A9A),
    /** Thunder: a black plateau, iron rods the lightning strikes, war drums, purple crystals, a storm sky. */
    THUNDER_SABER(MartialArts.THUNDER_SABER, 0x3A2458),
    /** The stars: an eternal night, seven pillars of light in the Dipper's shape, a pool that mirrors the sky. */
    SEVEN_STARS(MartialArts.SEVEN_STARS_SWORD, 0x070B1E),
    /** Earth broken: a shattered mountain, its blocks floating round a crater, magma in the cracks, dust. */
    MOUNTAIN_SHATTERING(MartialArts.MOUNTAIN_SHATTERING_FIST, 0x6A4A36),
    /** War and fire: a battlefield at a blood-red sunset, broken spears, fallen standards, the conqueror's tent, embers. */
    OVERLORD_SPEAR(MartialArts.OVERLORD_SPEAR, 0x8A2A1A),
    /** Blood: a crimson lake, spires of blood crystal, a red mist. */
    SANGUINE(MartialArts.SANGUINE_ART, 0x4A0810),
    /** The demon and the void: an island devoured at its edges, great arches of ribs, a red eye watching from the sky. */
    DEVOURING_DEMON(MartialArts.DEVOURING_DEMON, 0x120608),
    /** Fury: an arena of black stone cracked with red, chains hanging in the air, black flames. */
    MAJIN_FIST(MartialArts.MAJIN_FIST, 0x1E0A12);

    private final ResourceLocation art;
    /** Its sky's colour (0xRRGGBB), toward which a full reserve's void turns. */
    public final int sky;

    InnerTheme(ResourceLocation art, int sky) {
        this.art = art;
        this.sky = sky;
    }

    public static InnerTheme of(Optional<ResourceLocation> mainArt) {
        if (mainArt.isEmpty()) return PLAIN;
        for (InnerTheme t : values()) if (mainArt.get().equals(t.art)) return t;
        return PLAIN;
    }

    public float red() { return (sky >> 16 & 0xFF) / 255F; }
    public float green() { return (sky >> 8 & 0xFF) / 255F; }
    public float blue() { return (sky & 0xFF) / 255F; }
}
