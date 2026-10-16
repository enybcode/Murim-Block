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
            east = Math.max(east, MurimMap.uplift(13_000, k));
            south = Math.max(south, MurimMap.uplift(k, 13_000));
        }
        assertTrue(east > MurimMap.HILLS * 0.5 && east <= MurimMap.HILLS + 1e-9, "the east's hills: " + east);
        assertTrue(south > 0 && south <= MurimMap.HILLS * 0.5 + 1e-9, "the south's hills: " + south);
        // The north: at least its plateau everywhere, and somewhere along it the ranges reach high.
        double highest = 0;
        for (int x = -4_000; x <= 4_000; x += 100) { // the heart of the north (its edges fade out on purpose)
            double u = MurimMap.uplift(x, -13_000);
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
