package com.murimblock.world.map;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The map of the Murim world (docs/biomes/murim-map.md): a bubble of 40 000 x 40 000 blocks round the origin where
 * every land biome is a Murim one, an ocean 4 000 blocks wide around it, then the vanilla world. (150 000 until
 * 16/10/2026: Enzo chose a smaller world, every region within reach of a session, room left for the structures.)
 * <p>
 * Inside, the land is split into five regions of about the same size: the central mountains and valleys, and round
 * them in four quarters the snows, the dry lands, the humid lands and the water country. Since 16/10/2026 (Enzo:
 * random regions, of about equal size) each world draws its own layout from its seed ({@link #configure}): which of
 * the four quarters each outer region takes, how the quarters are turned, and how their borders wander, so no line is
 * straight and no two worlds are alike. The centre stays round the origin. The names (north, west...) are the
 * regions', not compass points any more. Without a world (unit tests) the classic layout holds: the snows to the
 * north (-z), the dry lands west, the humid lands south, the water country east.
 */
public final class MurimMap {
    /** Half the bubble's side: the Murim lands span -20 000 to 20 000 on both axes. */
    public static final int RADIUS = 20_000;
    /** The ocean round the bubble. */
    public static final int RING = 4_000;
    /** Width of the shore at the bubble's edge, and of the ocean's return to the vanilla relief. */
    public static final int EDGE = 1_000;
    /** How far the coastline wanders from the square (blocks). */
    public static final int COAST_WANDER = 600;
    /** The scale of the regions' wandering borders, against the map's first size (75 000). */
    private static final double SCALE = RADIUS / 75_000.0;
    /** The relief's fade towards the coast: from this far inside the shore, over this width. */
    private static final int COAST_FADE_FROM = 1_000, COAST_FADE = 2_000;

    /** Continentalness kept at least inside (near inland: no sea), and the ocean's (deep ocean, not mushroom fields). */
    public static final double LAND = 0.05, SHORE_SEA = -0.6, DEEP_SEA = -0.8;

    /** The regions (named after the classic layout's directions; a world's own layout puts them anywhere). */
    public enum Region { CENTER, NORTH, WEST, SOUTH, EAST }

    /**
     * Within this share of the radius from the origin, the centre: a disc of a fifth of the square bubble (pi r^2 = 4/5),
     * so the centre and each of the four quarters round it hold about the same land.
     */
    public static final double CENTER_SHARE = Math.sqrt(0.8 / Math.PI);

    /** The classic layout's seed ("MuRIMMAP"). */
    private static final long LAYOUT_SEED = 0x4D7552494D4D4150L;

    /**
     * A world's layout: the region in each quarter (counted from {@code turn}, a quarter turn on each time, as atan2
     * runs), and the noises of the borders (a, b), the coast (c), the ranges (d, e) and the hills (f, g).
     */
    public record Layout(long seed, double turn, Region[] quarters, SimplexNoise a, SimplexNoise b, SimplexNoise c,
                         SimplexNoise d, SimplexNoise e, SimplexNoise f, SimplexNoise g) {
        /** The classic layout: the snows to the north (-z), then the water country east, the humid lands south, the dry lands west. */
        static Layout classic() {
            XoroshiroRandomSource random = new XoroshiroRandomSource(LAYOUT_SEED);
            SimplexNoise b = new SimplexNoise(random), c = new SimplexNoise(random), d = new SimplexNoise(random), e = new SimplexNoise(random);
            SimplexNoise a = new SimplexNoise(random), f = new SimplexNoise(random), g = new SimplexNoise(random);
            return new Layout(LAYOUT_SEED, 0, new Region[]{Region.NORTH, Region.EAST, Region.SOUTH, Region.WEST}, a, b, c, d, e, f, g);
        }

        /** A world's own layout, drawn from its seed. */
        public static Layout of(long worldSeed) {
            XoroshiroRandomSource random = new XoroshiroRandomSource(worldSeed ^ LAYOUT_SEED);
            double turn = random.nextDouble() * 2 * Math.PI;
            java.util.List<Region> outer = new java.util.ArrayList<>(java.util.List.of(Region.NORTH, Region.EAST, Region.SOUTH, Region.WEST));
            for (int i = outer.size() - 1; i > 0; i--) java.util.Collections.swap(outer, i, random.nextInt(i + 1));
            SimplexNoise a = new SimplexNoise(random), b = new SimplexNoise(random), c = new SimplexNoise(random), d = new SimplexNoise(random);
            SimplexNoise e = new SimplexNoise(random), f = new SimplexNoise(random), g = new SimplexNoise(random);
            return new Layout(worldSeed, turn, outer.toArray(Region[]::new), a, b, c, d, e, f, g);
        }

        /** Which quarter (0 to 3) an angle (atan2) falls in, and how far into it (0 to pi/2). */
        double[] quarter(double angle) {
            double t = ((angle - turn + 3 * Math.PI / 4) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
            int k = Math.min(3, (int) (t / (Math.PI / 2)));
            return new double[]{k, t - k * Math.PI / 2};
        }

        /** The quarter a region holds (-1 for the centre). */
        int quarterOf(Region region) {
            for (int k = 0; k < 4; k++) if (quarters[k] == region) return k;
            return -1;
        }
    }

    private static volatile Layout layout = Layout.classic();

    private MurimMap() {
    }

    /** Draws the layout of the world being started (its seed); called before any chunk is generated. */
    public static void configure(long worldSeed) {
        layout = Layout.of(worldSeed);
    }

    /** Back to the classic layout (unit tests). */
    static void useClassic() {
        layout = Layout.classic();
    }

    /** The layout in use. */
    public static Layout layout() {
        return layout;
    }

    /**
     * The heart of a region: a block {@code share} of the radius out from the origin, in the middle of the region's
     * quarter (the origin for the centre). Where to look for a region's biomes and structures.
     */
    public static int[] heart(Region region, double share) {
        Layout l = layout;
        int k = l.quarterOf(region);
        if (k < 0) return new int[]{0, 0};
        double a = l.turn() - 3 * Math.PI / 4 + (k + 0.5) * Math.PI / 2;
        return new int[]{(int) Math.round(Math.cos(a) * share * RADIUS), (int) Math.round(Math.sin(a) * share * RADIUS)};
    }

    /** How strongly (0 to 1) a block at this angle lies inside a region's quarter, fading over its edges. */
    private static double inQuarter(Layout l, Region region, double angle) {
        double[] q = l.quarter(angle);
        if (l.quarters()[(int) q[0]] != region) return 0;
        return smooth(Math.min(q[1], Math.PI / 2 - q[1]) / 0.22);
    }

    /** A wandering of about -1 to 1, broad and then finer. */
    private static double wander(SimplexNoise noise, double x, double z) {
        return 0.75 * noise.getValue(x / (14_000.0 * SCALE), z / (14_000.0 * SCALE)) + 0.25 * noise.getValue(x / (3_500.0 * SCALE), z / (3_500.0 * SCALE));
    }

    /** How far out a block lies: the square's distance, its coastline wandering. */
    public static double reach(int x, int z) {
        Layout l = layout;
        return Math.max(Math.abs(x), Math.abs(z)) + COAST_WANDER * wander(l.c(), x, z);
    }

    /** Whether a block is within the Murim bubble (its coast included). */
    public static boolean inBubble(int x, int z) {
        return reach(x, z) < RADIUS;
    }

    /** Which region a block of the bubble belongs to. */
    public static Region region(int x, int z) {
        Layout l = layout;
        double u = (double) x / RADIUS + 0.16 * wander(l.a(), x, z);
        double v = (double) z / RADIUS + 0.16 * wander(l.b(), x, z);
        if (u * u + v * v < CENTER_SHARE * CENTER_SHARE) return Region.CENTER;
        // The four quarters round the centre, as the world's layout turns and deals them.
        return l.quarters()[(int) l.quarter(Math.atan2(v, u))[0]];
    }

    /**
     * Whether a block lies in the centre, or within {@code distance} blocks of a frontier between two regions: one of
     * eight points that far round it (and the block itself) falls in another region.
     */
    public static boolean centreOrFrontier(int x, int z, int distance) {
        Region here = region(x, z);
        if (here == Region.CENTER) return true;
        int d = (int) Math.round(distance / Math.sqrt(2));
        int[][] around = {{distance, 0}, {-distance, 0}, {0, distance}, {0, -distance}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
        for (int[] p : around) if (region(x + p[0], z + p[1]) != here) return true;
        return false;
    }

    // ---------------------------------------------------------------- the mountains of the north

    /**
     * How much the north is raised at most (in vanilla's depth: 1 is about 128 blocks), and the share of it every block of
     * the region gets (a high plateau) before the ranges rise on it.
     */
    public static final double UPLIFT = 1.2, PLATEAU = 0.3;

    /**
     * How far the terrain is raised here (vanilla depth units, 0 outside the north): immense ice ranges, long ridges
     * winding over the snows (ridged noise, broad and then finer), on a plateau, fading out towards the region's
     * borders and the coast. Same in every world, as the regions.
     */
    public static double uplift(int x, int z) {
        double[] wr = ranges(x, z);
        double north = wr[0] <= 0 ? 0 : UPLIFT * wr[0] * (PLATEAU + (1 - PLATEAU) * wr[1]);
        return north + HILLS * hills(x, z);
    }

    /** How high the rolling hills of the east (and, lower, of the south) rise at most (vanilla depth: about 45 blocks). */
    public static final double HILLS = 0.35;
    /** The long hills run this way (30 degrees off east). */
    private static final double COS = Math.cos(Math.toRadians(30)), SIN = Math.sin(Math.toRadians(30));

    /**
     * The long rolling hills of the east, and lower ones in the south (0 to 1, times HILLS): crests kilometres long, side
     * by side, the Jiangnan's water country left in the long valleys between them. Fades out towards the regions' borders and the coast.
     */
    public static double hills(int x, int z) {
        Layout l = layout;
        double u = (double) x / RADIUS + 0.16 * wander(l.a(), x, z);
        double v = (double) z / RADIUS + 0.16 * wander(l.b(), x, z);
        double r = Math.sqrt(u * u + v * v), a = Math.atan2(v, u);
        double out = smooth((r - CENTER_SHARE) / 0.12);
        double coast = smooth((RADIUS - COAST_FADE_FROM - reach(x, z)) / COAST_FADE);
        // The east's quarter in full, the south's at half.
        double east = inQuarter(l, Region.EAST, a);
        double south = inQuarter(l, Region.SOUTH, a) * 0.5;
        double w = (east + south) * out * coast;
        if (w <= 0) return 0;
        // Long hills: ridged noise stretched along one direction (turned 30 degrees off east, made to wander), so the
        // crests run on for kilometres, side by side, with long valleys between them.
        double along = x * COS + z * SIN, across = -x * SIN + z * COS + 900 * l.g().getValue(x / 3_500.0, z / 3_500.0);
        double crest = 1 - Math.abs(l.f().getValue(along / 5_000.0, across / 1_700.0));
        double fine = 1 - Math.abs(l.g().getValue(along / 2_600.0, across / 650.0));
        double h = crest * crest * 0.75 + fine * fine * 0.25;
        return w * Math.max(0, (h - 0.3) / 0.7);
    }

    /** Jaggedness added on the north's ranges (vanilla's jagged peaks: spires and knife-edge ridges of ice and rock). */
    public static final double JAGGED = 0.85;

    /** How jagged the north's ranges are here (0 off them): the higher on a range, the sharper. */
    public static double jaggedness(int x, int z) {
        double[] wr = ranges(x, z);
        return wr[0] <= 0 ? 0 : JAGGED * wr[0] * wr[1] * wr[1];
    }

    /** {weight of the north here (0 to 1), height of the ranges here (0 to 1)}. */
    private static double[] ranges(int x, int z) {
        Layout l = layout;
        double u = (double) x / RADIUS + 0.16 * wander(l.a(), x, z);
        double v = (double) z / RADIUS + 0.16 * wander(l.b(), x, z);
        double r = Math.sqrt(u * u + v * v), a = Math.atan2(v, u);
        // Inside the north's quarter, out of the centre, away from the coast: 1; fading over the edges.
        double sector = inQuarter(l, Region.NORTH, a);
        double out = smooth((r - CENTER_SHARE) / 0.12);
        double coast = smooth((RADIUS - COAST_FADE_FROM - reach(x, z)) / COAST_FADE);
        double w = sector * out * coast;
        if (w <= 0) return new double[]{0, 0};
        double broad = 1 - Math.abs(l.d().getValue(x / 3_500.0, z / 3_500.0));
        double fine = 1 - Math.abs(l.e().getValue(x / 1_400.0, z / 1_400.0));
        double ridge = broad * broad * 0.7 + fine * fine * 0.3;
        return new double[]{w, Math.max(0, (ridge - 0.32) / 0.68)};
    }

    /**
     * Vanilla's continentalness {@code value} at a block, shaped by the map: land everywhere inside (no sea, rivers
     * stay), a shore at the edge, the deep ocean of the ring, then vanilla again.
     */
    public static double continents(int x, int z, double value) {
        double d = reach(x, z);
        double land = Math.max(value, LAND);
        if (d <= RADIUS - EDGE) return land;
        if (d < RADIUS) return Mth.lerp(smooth((d - (RADIUS - EDGE)) / EDGE), land, SHORE_SEA);
        if (d < RADIUS + EDGE) return Mth.lerp(smooth((d - RADIUS) / EDGE), SHORE_SEA, DEEP_SEA);
        if (d < RADIUS + RING - EDGE) return DEEP_SEA;
        if (d < RADIUS + RING) return Mth.lerp(smooth((d - (RADIUS + RING - EDGE)) / EDGE), DEEP_SEA, value);
        return value;
    }

    private static double smooth(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
