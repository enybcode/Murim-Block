package com.murimblock.world.map;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The map of the Murim world (docs/biomes/murim-map.md): a bubble of 150 000 x 150 000 blocks round the origin where
 * every land biome is a Murim one, an ocean 10 000 blocks wide around it, then the vanilla world.
 * <p>
 * Inside, the land is split into fixed regions, the same in every world (the seed only changes the biomes' patches
 * within a region, the relief and where the structures fall): the central mountains and valleys, the snows of the
 * north, the deserts of the west, the humid south, the water country of the east. Their borders wander (a noise of
 * its own, seeded with a constant), so no line is straight.
 */
public final class MurimMap {
    /** Half the bubble's side: the Murim lands span -75 000 to 75 000 on both axes. */
    public static final int RADIUS = 75_000;
    /** The ocean round the bubble. */
    public static final int RING = 10_000;
    /** Width of the shore at the bubble's edge, and of the ocean's return to the vanilla relief. */
    public static final int EDGE = 2_000;
    /** How far the coastline wanders from the square (blocks). */
    public static final int COAST_WANDER = 1_500;

    /** Continentalness kept at least inside (near inland: no sea), and the ocean's (deep ocean, not mushroom fields). */
    public static final double LAND = 0.05, SHORE_SEA = -0.6, DEEP_SEA = -0.8;

    /** The regions; where each one lies is fixed. */
    public enum Region { CENTER, NORTH, WEST, SOUTH, EAST }

    /** Within this share of the radius from the origin, the centre. */
    public static final double CENTER_SHARE = 0.30;

    private static final long LAYOUT_SEED = 0x4D7552494D4D4150L; // "MuRIMMAP": the same layout in every world
    private static volatile SimplexNoise noiseA, noiseB, noiseC, noiseD, noiseE, noiseF, noiseG;

    private MurimMap() {
    }

    private static void init() {
        if (noiseA != null) return;
        synchronized (MurimMap.class) {
            if (noiseA != null) return;
            XoroshiroRandomSource random = new XoroshiroRandomSource(LAYOUT_SEED);
            noiseB = new SimplexNoise(random);
            noiseC = new SimplexNoise(random);
            noiseD = new SimplexNoise(random);
            noiseE = new SimplexNoise(random);
            // (Drawn in this order so the regions of every older world stay where they were: A, then the hills' F and G.)
            SimplexNoise a = new SimplexNoise(random);
            noiseF = new SimplexNoise(random);
            noiseG = new SimplexNoise(random);
            noiseA = a;
        }
    }

    /** A wandering of about -1 to 1, broad and then finer. */
    private static double wander(SimplexNoise noise, double x, double z) {
        return 0.75 * noise.getValue(x / 14_000.0, z / 14_000.0) + 0.25 * noise.getValue(x / 3_500.0, z / 3_500.0);
    }

    /** How far out a block lies: the square's distance, its coastline wandering. */
    public static double reach(int x, int z) {
        init();
        return Math.max(Math.abs(x), Math.abs(z)) + COAST_WANDER * wander(noiseC, x, z);
    }

    /** Whether a block is within the Murim bubble (its coast included). */
    public static boolean inBubble(int x, int z) {
        return reach(x, z) < RADIUS;
    }

    /** Which region a block of the bubble belongs to. */
    public static Region region(int x, int z) {
        init();
        double u = (double) x / RADIUS + 0.16 * wander(noiseA, x, z);
        double v = (double) z / RADIUS + 0.16 * wander(noiseB, x, z);
        if (u * u + v * v < CENTER_SHARE * CENTER_SHARE) return Region.CENTER;
        // North is -z. The four sectors meet on the diagonals.
        double a = Math.atan2(v, u);
        if (a > -3 * Math.PI / 4 && a <= -Math.PI / 4) return Region.NORTH;
        if (a > -Math.PI / 4 && a <= Math.PI / 4) return Region.EAST;
        if (a > Math.PI / 4 && a <= 3 * Math.PI / 4) return Region.SOUTH;
        return Region.WEST;
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

    /**
     * The rolling hills of the east, and lower ones in the south (0 to 1, times HILLS): broad swells and finer ones, the
     * Jiangnan's water country left in the hollows between them. Fades out towards the regions' borders and the coast.
     */
    public static double hills(int x, int z) {
        init();
        double u = (double) x / RADIUS + 0.16 * wander(noiseA, x, z);
        double v = (double) z / RADIUS + 0.16 * wander(noiseB, x, z);
        double r = Math.sqrt(u * u + v * v), a = Math.atan2(v, u);
        double out = smooth((r - CENTER_SHARE) / 0.12);
        double coast = smooth((RADIUS - 3_000 - reach(x, z)) / 5_000);
        // The east sector (-pi/4 to pi/4) in full, the south (pi/4 to 3pi/4) at half.
        double east = smooth(Math.min(a + Math.PI / 4, Math.PI / 4 - a) / 0.22);
        double south = smooth(Math.min(a - Math.PI / 4, 3 * Math.PI / 4 - a) / 0.22) * 0.5;
        double w = (east + south) * out * coast;
        if (w <= 0) return 0;
        double swell = 0.65 * noiseF.getValue(x / 3_200.0, z / 3_200.0) + 0.35 * noiseG.getValue(x / 900.0, z / 900.0);
        return w * Math.max(0, swell + 0.15) / 1.15;
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
        init();
        double u = (double) x / RADIUS + 0.16 * wander(noiseA, x, z);
        double v = (double) z / RADIUS + 0.16 * wander(noiseB, x, z);
        double r = Math.sqrt(u * u + v * v), a = Math.atan2(v, u);
        // Inside the north sector (-3pi/4 to -pi/4), out of the centre, away from the coast: 1; fading over the edges.
        double sector = smooth(Math.min(a + 3 * Math.PI / 4, -Math.PI / 4 - a) / 0.22);
        double out = smooth((r - CENTER_SHARE) / 0.12);
        double coast = smooth((RADIUS - 3_000 - reach(x, z)) / 5_000);
        double w = sector * out * coast;
        if (w <= 0) return new double[]{0, 0};
        double broad = 1 - Math.abs(noiseD.getValue(x / 7_000.0, z / 7_000.0));
        double fine = 1 - Math.abs(noiseE.getValue(x / 2_200.0, z / 2_200.0));
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
