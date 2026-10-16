package com.murimblock.world.map;

import com.murimblock.world.MurimBiomes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Murim map's layout: the fixed regions, the bubble and its ocean ring (docs/biomes/murim-map.md). */
class MurimMapTest {
    @Test
    void theRegionsLieWhereTheMapSays() {
        assertEquals(MurimMap.Region.CENTER, MurimMap.region(0, 0));
        assertEquals(MurimMap.Region.NORTH, MurimMap.region(0, -16_000));
        assertEquals(MurimMap.Region.SOUTH, MurimMap.region(0, 16_000));
        assertEquals(MurimMap.Region.WEST, MurimMap.region(-16_000, 0));
        assertEquals(MurimMap.Region.EAST, MurimMap.region(16_000, 0));
    }

    @Test
    void theAllianceOutpostsStandInTheCentreOrOnAFrontier() {
        assertTrue(MurimMap.centreOrFrontier(0, 0, 1500));
        // Deep in a region, far from any frontier: refused.
        assertFalse(MurimMap.centreOrFrontier(0, -15_000, 1500));
        assertFalse(MurimMap.centreOrFrontier(15_000, 0, 1500));
        // Walking out from the centre to the north, the frontier is crossed once: near it allowed, past it refused.
        int frontier = 0;
        for (int z = 0; z > -18_000; z -= 50) if (MurimMap.region(0, z) != MurimMap.Region.CENTER) { frontier = z; break; }
        assertTrue(frontier < -3_000, "the centre ends at " + frontier);
        assertTrue(MurimMap.centreOrFrontier(0, frontier - 800, 1500));
        assertFalse(MurimMap.centreOrFrontier(0, frontier - 5_000, 1500));
        // A frontier between two outer regions (on the diagonal, far out) also counts.
        boolean onDiagonal = false;
        for (int k = 6_000; k < 16_000 && !onDiagonal; k += 200) onDiagonal = MurimMap.region(k, -k) != MurimMap.region(k + 1_000, -k + 1_000) && MurimMap.centreOrFrontier(k, -k, 1500);
        assertTrue(onDiagonal);
    }

    @Test
    void eachWorldDealsTheRegionsAnewAndTheyHoldTheSameLand() {
        try {
            // The same point, deep out from the centre, falls in each of the four outer regions in some world.
            java.util.Set<MurimMap.Region> seen = java.util.EnumSet.noneOf(MurimMap.Region.class);
            java.util.Set<Long> turns = new java.util.HashSet<>();
            for (long seed = 0; seed < 64; seed++) {
                MurimMap.configure(seed);
                seen.add(MurimMap.region(0, -15_000));
                turns.add(Math.round(Math.toDegrees(MurimMap.layout().turn())));
                // Each region's heart lies in that region.
                for (MurimMap.Region r : MurimMap.Region.values()) {
                    int[] h = MurimMap.heart(r, 0.75);
                    assertEquals(r, MurimMap.region(h[0], h[1]), "seed " + seed + ": the heart of the " + r);
                }
            }
            assertEquals(4, seen.size(), "the outer regions do not move from world to world: " + seen);
            assertTrue(turns.size() > 40, "the layouts are hardly turned: " + turns.size());
            // In every world the five regions hold about the same land: each a seventh to a quarter of the bubble.
            for (long seed : new long[]{1, 42, 20_261_012, -7}) {
                MurimMap.configure(seed);
                java.util.Map<MurimMap.Region, Integer> n = new java.util.EnumMap<>(MurimMap.Region.class);
                int land = 0;
                for (int x = -19_800; x <= 19_800; x += 200) for (int z = -19_800; z <= 19_800; z += 200) {
                    if (!MurimMap.inBubble(x, z)) continue;
                    n.merge(MurimMap.region(x, z), 1, Integer::sum);
                    land++;
                }
                for (MurimMap.Region r : MurimMap.Region.values())
                    assertTrue(n.getOrDefault(r, 0) > land / 7 && n.getOrDefault(r, 0) < land / 4, "seed " + seed + ": " + n);
            }
        } finally {
            MurimMap.useClassic();
        }
    }

    /** Draws the regions of six worlds side by side (build/murim-layouts.png), for the docs. */
    @Test
    void drawSixWorldsLayouts() throws java.io.IOException {
        int cell = 220, step = 200, gap = 12;
        long[] seeds = {1, 2, 3, 42, 20_261_012, 8_675_309};
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(3 * cell + 4 * gap, 2 * cell + 3 * gap, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.util.Map<MurimMap.Region, Integer> colour = java.util.Map.of(MurimMap.Region.CENTER, 0xE8A0B8, MurimMap.Region.NORTH, 0xEEF4FA,
                MurimMap.Region.WEST, 0xE2C27A, MurimMap.Region.SOUTH, 0x4E9A3A, MurimMap.Region.EAST, 0x4AA8C0);
        for (int i = 0; i < img.getWidth(); i++) for (int j = 0; j < img.getHeight(); j++) img.setRGB(i, j, 0x2A2F3A);
        try {
            for (int k = 0; k < seeds.length; k++) {
                MurimMap.configure(seeds[k]);
                int ox = gap + (k % 3) * (cell + gap), oy = gap + (k / 3) * (cell + gap);
                for (int px = 0; px < cell; px++) for (int pz = 0; pz < cell; pz++) {
                    int x = (px - cell / 2) * step, z = (pz - cell / 2) * step;
                    img.setRGB(ox + px, oy + pz, MurimMap.inBubble(x, z) ? colour.get(MurimMap.region(x, z)) : 0x1A3A6A);
                }
            }
        } finally {
            MurimMap.useClassic();
        }
        java.io.File out = new java.io.File("build/murim-layouts.png");
        out.getParentFile().mkdirs();
        javax.imageio.ImageIO.write(img, "png", out);
    }

    @Test
    void theLayoutIsTheSameInEveryWorld() {
        // No seed in it: asked twice, the same answers.
        for (int x = -18_000; x <= 18_000; x += 2_659) for (int z = -18_000; z <= 18_000; z += 2_111) {
            assertEquals(MurimMap.region(x, z), MurimMap.region(x, z));
        }
    }

    @Test
    void theBubbleIsLandAndTheRingIsDeepSea() {
        // Inside, even vanilla's deepest sea becomes land; the ring is deep sea whatever vanilla had; beyond, vanilla.
        assertTrue(MurimMap.continents(3_000, -6_000, -1.0) >= MurimMap.LAND);
        assertEquals(0.7, MurimMap.continents(0, 0, 0.7), 1e-9);
        assertTrue(MurimMap.inBubble(0, 0) && MurimMap.inBubble(18_500, -18_500));
        assertFalse(MurimMap.inBubble(21_000, 0));
        assertEquals(MurimMap.DEEP_SEA, MurimMap.continents(22_000, 0, 0.8), 1e-9);
        assertEquals(MurimMap.DEEP_SEA, MurimMap.continents(-2_000, -22_000, 0.3), 1e-9);
        assertEquals(0.42, MurimMap.continents(26_000, 0, 0.42), 1e-9);
        // The coastline wanders: not one straight edge.
        double a = MurimMap.reach(20_000, 0), b = MurimMap.reach(20_000, 6_000);
        assertNotEquals(a, b, 1.0);
    }

    @Test
    void theNorthRisesIntoRangesTheEastAndSouthIntoHills() {
        // Nothing raised at the centre, in the west, nor in the ocean.
        for (int[] p : new int[][]{{0, 0}, {-13_000, 0}, {0, -22_000}, {22_000, 0}}) {
            assertEquals(0, MurimMap.uplift(p[0], p[1]), 1e-9, "raised at " + p[0] + ", " + p[1]);
        }
        // The east and the south: rolling hills, never more than the hills' height, the south's lower.
        double east = 0, south = 0;
        for (int k = -4_000; k <= 4_000; k += 100) {
            east = Math.max(east, MurimMap.uplift(15_500, k));
            south = Math.max(south, MurimMap.uplift(k, 15_500));
        }
        assertTrue(east > MurimMap.HILLS * 0.5 && east <= MurimMap.HILLS + 1e-9, "the east's hills: " + east);
        assertTrue(south > 0 && south <= MurimMap.HILLS * 0.5 + 1e-9, "the south's hills: " + south);
        // The north: at least its plateau everywhere, and somewhere along it the ranges reach high.
        double highest = 0;
        for (int x = -3_000; x <= 3_000; x += 100) { // the heart of the north (its edges fade out on purpose)
            double u = MurimMap.uplift(x, -15_500);
            assertTrue(u >= MurimMap.UPLIFT * MurimMap.PLATEAU - 1e-9, "no plateau at " + x);
            highest = Math.max(highest, u);
        }
        assertTrue(highest > MurimMap.UPLIFT * 0.6, "the north's ranges stay low: " + highest);
    }

    @Test
    void eachRegionGrowsItsOwnKindOfBiomes() {
        assertEquals(MurimBiomes.KUNLUN_GLACIERS, MurimBiomeSource.pick(MurimMap.Region.NORTH, -0.6F, 0));
        assertEquals(MurimBiomes.TIANSHAN_SNOWFIELDS, MurimBiomeSource.pick(MurimMap.Region.NORTH, 0.3F, 0));
        assertEquals(MurimBiomes.HEXI_FRONTIER_DESERT, MurimBiomeSource.pick(MurimMap.Region.WEST, 0, -0.5F));
        assertEquals(MurimBiomes.LOESS_PLATEAU, MurimBiomeSource.pick(MurimMap.Region.WEST, 0, 0.3F));
        assertEquals(MurimBiomes.JADE_KARST_FOREST, MurimBiomeSource.pick(MurimMap.Region.SOUTH, 0, 0.5F));
        assertEquals(MurimBiomes.SHU_BAMBOO_SEA, MurimBiomeSource.pick(MurimMap.Region.SOUTH, -0.5F, -0.5F));
        assertEquals(MurimBiomes.CAMPHOR_HILLS, MurimBiomeSource.pick(MurimMap.Region.SOUTH, 0.2F, -0.5F));
        assertEquals(MurimBiomes.CLOUD_SEA_PEAKS, MurimBiomeSource.pick(MurimMap.Region.CENTER, -0.6F, 0));
        assertEquals(MurimBiomes.PEACH_BLOSSOM_VALLEY, MurimBiomeSource.pick(MurimMap.Region.CENTER, 0.2F, 0));
        assertEquals(MurimBiomes.JIANGNAN_WATERLANDS, MurimBiomeSource.pick(MurimMap.Region.EAST, 0.2F, 0));
    }
}
