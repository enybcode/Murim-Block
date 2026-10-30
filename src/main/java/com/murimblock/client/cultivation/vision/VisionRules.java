package com.murimblock.client.cultivation.vision;

/**
 * The numbers of the spiritual vision seen with closed eyes ({@code docs/cultivation/meditation-vision.md}), kept apart
 * from Minecraft so they can be tested. Realms past the last row see as the last row.
 */
public final class VisionRules {
    /** Blocks seen as outlines, and living things seen as stars, within this many blocks: Mortal first. */
    private static final int[] BLOCK_REACH = {0, 6, 10, 16, 24, 32};
    private static final int[] STAR_REACH = {0, 12, 20, 32, 48, 64};
    /** 0, a soft wide glow, to 1, sharp thin edges. */
    private static final float[] SHARPNESS = {0, 0, 0.3f, 0.6f, 0.8f, 1};
    /** How strongly the faces between the outlines glow: the highest realms see blocks nearly solid. */
    private static final float[] FACE_FILL = {0, 0, 0, 0, 0.02f, 0.045f};
    /** Strength at which a star twinkles fastest: log(hp + 4 x attack) of an Iron Golem is about this. */
    private static final double FULL_STRENGTH = Math.log(160);

    /** What a star's colour tells: whether the thing means harm. */
    public enum Temper {
        PASSIVE(0xBFE3FF),
        NEUTRAL(0xFFD84A),
        HOSTILE(0xFF9A3C),
        /** A hostile or angered mob after the player. */
        TARGETING(0xFF4040),
        /** Another player: their Qi shows as a gold halo from First-Rate Warrior on. */
        CULTIVATOR(0xE6F0FF);

        private final int rgb;

        Temper(int rgb) {
            this.rgb = rgb;
        }

        public int rgb() {
            return rgb;
        }
    }

    private VisionRules() {
    }

    private static int row(int realm) {
        return Math.clamp(realm, 0, BLOCK_REACH.length - 1);
    }

    public static int blockReach(int realm) {
        return BLOCK_REACH[row(realm)];
    }

    public static int starReach(int realm) {
        return STAR_REACH[row(realm)];
    }

    /** The first realm that sees outlines sees them flicker. */
    public static boolean flickers(int realm) {
        return row(realm) == 1;
    }

    public static float sharpness(int realm) {
        return SHARPNESS[row(realm)];
    }

    public static float faceFill(int realm) {
        return FACE_FILL[row(realm)];
    }

    public static boolean halos(int realm) {
        return row(realm) >= 3;
    }

    /** Star radius in blocks for a body this wide and tall: a chicken about 0.15, an Iron Golem about 0.5. */
    public static float starRadius(float width, float height) {
        return 0.3f * (float) Math.cbrt(width * width * height);
    }

    /** Strength 0-1 from the health and attack damage, on a log scale. */
    public static float strength(float maxHealth, float attack) {
        return (float) Math.clamp(Math.log(Math.max(1, maxHealth + 4 * attack)) / FULL_STRENGTH, 0, 1);
    }

    /** Twinkles a second: a chicken slowly, a strong mob fast. */
    public static float twinkleHz(float strength) {
        return 0.35f + 2.6f * Math.clamp(strength, 0, 1);
    }
}
