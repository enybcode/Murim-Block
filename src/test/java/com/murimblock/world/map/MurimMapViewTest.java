package com.murimblock.world.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MurimMapViewTest {
    @Test
    void theGridHoldsTheBubbleItsFiveRegionsItsOceanAndTheWorldBeyond() {
        MurimMap.useClassic();
        byte[] cells = MurimMapView.cells();
        int n = MurimMapView.SIZE;
        assertEquals(n * n, cells.length);
        assertEquals(MurimMap.Region.CENTER.ordinal(), cells[(n / 2) * n + n / 2], "the middle is the centre");
        assertEquals(MurimMapView.OUTSIDE, cells[0], "the corner is beyond the ocean");
        Set<Byte> seen = new HashSet<>();
        for (byte c : cells) seen.add(c);
        for (MurimMap.Region r : MurimMap.Region.values()) assertTrue(seen.contains((byte) r.ordinal()), r + " is on the map");
        assertTrue(seen.contains(MurimMapView.OCEAN), "the ocean is on the map");
        // The classic layout: the snows to the north (-z), up on the map.
        assertEquals(MurimMap.Region.NORTH.ordinal(), cells[(n / 2 - n / 4) * n + n / 2]);
        int[] hearts = MurimMapView.hearts();
        assertEquals(0, hearts[0]);
        assertEquals(0, hearts[1]);
        assertTrue(hearts[MurimMap.Region.NORTH.ordinal() * 2 + 1] < 0, "the north's name is up");
    }
}
