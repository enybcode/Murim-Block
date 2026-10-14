package com.murimblock.world.map;

import com.murimblock.world.MurimBiomes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Murim map's layout: the fixed regions, the bubble and its ocean ring (docs/biomes/murim-map.md). */
class MurimMapTest {
    @Test
    void theRegionsLieWhereTheMapSays() {
        assertEquals(MurimMap.Region.CENTER, MurimMap.region(0, 0));
        assertEquals(MurimMap.Region.NORTH, MurimMap.region(0, -60_000));
        assertEquals(MurimMap.Region.SOUTH, MurimMap.region(0, 60_000));
        assertEquals(MurimMap.Region.WEST, MurimMap.region(-60_000, 0));
        assertEquals(MurimMap.Region.EAST, MurimMap.region(60_000, 0));
    }

    @Test
    void theLayoutIsTheSameInEveryWorld() {
        // No seed in it: asked twice, the same answers.
        for (int x = -70_000; x <= 70_000; x += 9_973) for (int z = -70_000; z <= 70_000; z += 7_919) {
            assertEquals(MurimMap.region(x, z), MurimMap.region(x, z));
        }
    }

    @Test
    void theBubbleIsLandAndTheRingIsDeepSea() {
        // Inside, even vanilla's deepest sea becomes land; the ring is deep sea whatever vanilla had; beyond, vanilla.
        assertTrue(MurimMap.continents(10_000, -20_000, -1.0) >= MurimMap.LAND);
        assertEquals(0.7, MurimMap.continents(0, 0, 0.7), 1e-9);
        assertTrue(MurimMap.inBubble(0, 0) && MurimMap.inBubble(70_000, -70_000));
        assertFalse(MurimMap.inBubble(78_000, 0));
        assertEquals(MurimMap.DEEP_SEA, MurimMap.continents(80_000, 0, 0.8), 1e-9);
        assertEquals(MurimMap.DEEP_SEA, MurimMap.continents(-5_000, -81_000, 0.3), 1e-9);
        assertEquals(0.42, MurimMap.continents(95_000, 0, 0.42), 1e-9);
        // The coastline wanders: not one straight edge.
        double a = MurimMap.reach(75_000, 0), b = MurimMap.reach(75_000, 20_000);
        assertNotEquals(a, b, 1.0);
    }

    @Test
    void theNorthRisesIntoRangesTheEastAndSouthIntoHills() {
        // Nothing raised at the centre, in the west, nor in the ocean.
        for (int[] p : new int[][]{{0, 0}, {-50_000, 0}, {0, -82_000}, {82_000, 0}}) {
            assertEquals(0, MurimMap.uplift(p[0], p[1]), 1e-9, "raised at " + p[0] + ", " + p[1]);
        }
        // The east and the south: rolling hills, never more than the hills' height, the south's lower.
        double east = 0, south = 0;
        for (int k = -15_000; k <= 15_000; k += 500) {
            east = Math.max(east, MurimMap.uplift(50_000, k));
            south = Math.max(south, MurimMap.uplift(k, 50_000));
        }
        assertTrue(east > MurimMap.HILLS * 0.5 && east <= MurimMap.HILLS + 1e-9, "the east's hills: " + east);
        assertTrue(south > 0 && south <= MurimMap.HILLS * 0.5 + 1e-9, "the south's hills: " + south);
        // The north: at least its plateau everywhere, and somewhere along it the ranges reach high.
        double highest = 0;
        for (int x = -15_000; x <= 15_000; x += 500) { // the heart of the north (its edges fade out on purpose)
            double u = MurimMap.uplift(x, -50_000);
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
