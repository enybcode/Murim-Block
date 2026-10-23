package com.murimblock.client.cultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BreathCycleTest {
    @Test
    void ringBreathesBetweenItsBoundsOverFourSeconds() {
        assertEquals(BreathCycle.MIN_SCALE, BreathCycle.scale(0), 1e-5);
        assertEquals(BreathCycle.MAX_SCALE, BreathCycle.scale(2), 1e-5);
        assertEquals(BreathCycle.MIN_SCALE, BreathCycle.scale(4), 1e-5);
        assertTrue(BreathCycle.inhaling(1));
        assertFalse(BreathCycle.inhaling(3));
    }

    @Test
    void goodOnlyNearTheCircleOnBothBreaths() {
        assertFalse(BreathCycle.good(0));
        assertFalse(BreathCycle.good(2));
        int goodTicks = 0;
        for (int tick = 0; tick < 80; tick++) {
            if (BreathCycle.good(tick / 20f)) {
                goodTicks++;
            }
        }
        // Two crossings per cycle, each a window of a few ticks.
        assertTrue(goodTicks >= 8 && goodTicks <= 20, "good ticks " + goodTicks);
    }
}
