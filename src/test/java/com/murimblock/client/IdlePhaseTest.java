package com.murimblock.client;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IdlePhaseTest {
    @Test
    void aBandOfBanditsStartsItsIdlesSpreadOverTheLoop() {
        // Four bandits spawned together (consecutive ids): four different points of the loop, well apart.
        for (int first = 0; first < 400; first += 4) {
            float[] p = new float[4];
            for (int i = 0; i < 4; i++) {
                p[i] = IdlePhase.phase(first + i);
                assertTrue(p[i] >= 0 && p[i] < 1, "phase out of the loop: " + p[i]);
            }
            Set<Integer> tenths = new HashSet<>();
            for (float x : p) tenths.add((int) (x * 10));
            assertTrue(tenths.size() >= 2, "a band of four all start in the same tenth of the loop");
        }
    }

    @Test
    void theSameBanditAlwaysStartsAtTheSamePoint() {
        assertTrue(IdlePhase.phase(123) == IdlePhase.phase(123));
    }
}
