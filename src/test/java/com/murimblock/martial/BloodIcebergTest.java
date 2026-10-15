package com.murimblock.martial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloodIcebergTest {
    @Test
    void theMountainFillsMostOfThirtyByThirtyWithItsPeakInTheMiddle() {
        for (long seed : new long[]{1, 42, 977}) {
            int[][] h = BloodIceberg.heights(seed);
            assertEquals(30, h.length);
            int filled = 0, highest = 0;
            for (int[] row : h) for (int v : row) { if (v >= 1) filled++; highest = Math.max(highest, v); }
            assertTrue(filled > 30 * 30 * 3 / 4, "too little of the 30 x 30 covered: " + filled);
            for (int i = 14; i <= 15; i++) for (int j = 14; j <= 15; j++) assertEquals(BloodIceberg.PEAK, h[i][j], "the peak to stand on");
            assertTrue(highest > BloodIceberg.PEAK, "no spire stands out: " + highest);
            assertTrue(h[0][0] < 0 && h[2][15] >= 1, "corners rounded, sides filled");
        }
    }
}
