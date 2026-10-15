package com.murimblock.martial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloodIcebergTest {
    @Test
    void theArenaIsAFloorRingedByTiersAWallAndSpires() {
        for (long seed : new long[]{1, 42, 977}) {
            var shape = BloodIceberg.shape(seed);
            int[][] h = shape.heights();
            int size = BloodIceberg.RADIUS * 2, r = BloodIceberg.RADIUS;
            assertEquals(size, h.length);
            // The floor: flat at FLOOR across the arena, glowing in the middle.
            for (int i = r - 10; i < r + 10; i++) assertEquals(BloodIceberg.FLOOR, h[i][r]);
            assertTrue(shape.seal()[r][r], "the seal's middle");
            // Out from the middle: the floor, then the tiers, then the wall (at least WALL over the floor), then the slope.
            int prev = 0, highest = 0;
            for (int i = r; i < r + BloodIceberg.RADIUS - 1; i++) { assertTrue(h[i][r] >= prev || h[i][r] >= BloodIceberg.FLOOR + BloodIceberg.WALL, "tiers step up"); prev = h[i][r]; }
            for (int[] row : h) for (int v : row) highest = Math.max(highest, v);
            assertTrue(h[r + 20][r] >= BloodIceberg.FLOOR + BloodIceberg.WALL, "the wall: " + h[r + 20][r]);
            assertTrue(highest > BloodIceberg.FLOOR + BloodIceberg.WALL + 3 && highest <= BloodIceberg.TOP, "spires: " + highest);
            assertTrue(h[0][0] < 0, "round, not square");
        }
    }
}
