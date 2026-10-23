package com.murimblock.cultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CultivationTest {
    @Test
    void gaugeGrowsByStageAndRealm() {
        assertEquals(100, CultivationRules.gauge(0, 1));
        assertEquals(125, CultivationRules.gauge(0, 2));
        assertEquals(596, CultivationRules.gauge(0, 9));
        assertEquals(300, CultivationRules.gauge(1, 1));
    }

    @Test
    void harmonyMultipliesTheGainUpToTwoAndAHalf() {
        assertEquals(2.0, CultivationRules.meditationGain(0), 1e-9);
        assertEquals(5.0, CultivationRules.meditationGain(1), 1e-9);
        assertEquals(5.0, CultivationRules.meditationGain(3), 1e-9);
    }

    @Test
    void onlyMeditationFillsTheGauge() {
        Cultivation c = new Cultivation();
        c.gather(10);
        assertEquals(0, c.stageQi(), 1e-9);
        c.startMeditation(0, 0, 0);
        c.gather(10);
        assertEquals(20, c.stageQi(), 1e-9);
        c.gather(1000);
        assertEquals(c.gauge(), c.stageQi(), 1e-9);
    }

    @Test
    void advanceNeedsAFullGaugeAndStopsAtStageNine() {
        Cultivation c = new Cultivation();
        assertFalse(c.advance());
        c.set(0, 1, 100, 100, 100, 0);
        assertTrue(c.advance());
        assertEquals(2, c.stage());
        assertEquals(0, c.stageQi(), 1e-9);
        assertEquals(110, c.maxQi());
        c.set(0, 9, 10_000, 0, 100, 0);
        assertTrue(c.gaugeFull());
        assertFalse(c.advance());
        assertEquals(9, c.stage());
    }

    @Test
    void meridiansOpenUpToTheRealmCap() {
        Cultivation c = new Cultivation();
        for (int i = 0; i < 4; i++) {
            assertTrue(c.openMeridian());
        }
        assertFalse(c.openMeridian());
        c.set(3, 1, 0, 0, 100, 12);
        assertTrue(c.openMeridian());
        assertFalse(c.openMeridian());
        assertEquals(CultivationRules.MERIDIANS, c.meridians());
    }

    @Test
    void harmonyRisesOnGoodBeatsAndFallsHarderOnMisses() {
        Cultivation c = new Cultivation();
        c.startMeditation(0, 0, 0);
        c.breathe(true, 0);
        c.breathe(true, 20);
        assertEquals(0.30, c.harmony(), 1e-9);
        c.breathe(false, 40);
        assertEquals(0.05, c.harmony(), 1e-9);
        c.breathe(false, 60);
        assertEquals(0, c.harmony(), 1e-9);
    }
}
