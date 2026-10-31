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
    void peaksFollowEachOtherAcrossRealms() {
        assertEquals(100, CultivationRules.floor(0, 1));
        assertEquals(200, CultivationRules.peak(0, 1));
        assertEquals(CultivationRules.peak(0, 1), CultivationRules.floor(0, 2));
        assertEquals(CultivationRules.peak(0, 9), CultivationRules.floor(1, 1));
    }

    @Test
    void calmMultipliesTheGainUpToThree() {
        assertEquals(0.02, CultivationRules.meditationGain(0), 1e-9);
        assertEquals(0.06, CultivationRules.meditationGain(1), 1e-9);
        assertEquals(0.06, CultivationRules.meditationGain(3), 1e-9);
    }

    @Test
    void onlyMeditationGrowsMaxQiUpToThePeak() {
        Cultivation c = new Cultivation();
        c.gather(10);
        assertEquals(100, c.maxQiExact(), 1e-9);
        c.startMeditation(0, 0, 0);
        c.gather(10);
        assertEquals(100.2, c.maxQiExact(), 1e-9);
        assertEquals(0.2, c.stageQi(), 1e-9);
        c.gather(100_000);
        assertEquals(c.peak(), c.maxQiExact(), 1e-9);
        assertTrue(c.gaugeFull());
    }

    @Test
    void qiRefillsAsAShareOfMaxQi() {
        Cultivation c = new Cultivation();
        c.set(0, 1, 0, 0, 1000, 0);
        c.gather(10);
        assertEquals(50, c.qi(), 1e-9);
    }

    @Test
    void advanceNeedsAFullGaugeAndStopsAtStageNine() {
        Cultivation c = new Cultivation();
        assertFalse(c.advance());
        c.set(0, 1, 100, 100, 100, 0);
        assertTrue(c.advance());
        assertEquals(2, c.stage());
        assertEquals(0, c.stageQi(), 1e-9);
        assertEquals(200, c.maxQi());
        c.set(0, 9, 10_000, 0, 100, 0);
        assertTrue(c.gaugeFull());
        assertFalse(c.advance());
        assertEquals(9, c.stage());
    }

    @Test
    void killsGrowMaxQiUpToThePeakOnly() {
        Cultivation c = new Cultivation();
        assertEquals(0.2, c.gainMaxQi(4 * CultivationRules.KILL_SHARE), 1e-9);
        assertEquals(100.2, c.maxQiExact(), 1e-9);
        assertEquals(c.peak() - 100.2, c.gainMaxQi(1000), 1e-9);
        assertEquals(0, c.gainMaxQi(5), 1e-9);
    }

    @Test
    void farmingOneKindCutsTheReward() {
        assertEquals(1, CultivationRules.repeatMultiplier(10), 1e-9);
        assertEquals(0.75, CultivationRules.repeatMultiplier(11), 1e-9);
        assertEquals(0.5, CultivationRules.repeatMultiplier(21), 1e-9);
    }

    @Test
    void deathCostsATenthOfMaxQiButNotTheStage() {
        Cultivation c = new Cultivation();
        c.set(0, 2, 125, 300, 0, 0);
        assertEquals(32.5, c.die(), 1e-9);
        assertEquals(292.5, c.maxQiExact(), 1e-9);
        c.set(0, 2, 10, 300, 0, 0);
        c.die();
        assertEquals(CultivationRules.floor(0, 2), c.maxQiExact(), 1e-9);
        assertTrue(c.qi() <= c.maxQiExact());
    }

    @Test
    void aBossGivesItsBonusOnce() {
        Cultivation c = new Cultivation();
        assertTrue(c.firstDefeat("minecraft:warden"));
        assertFalse(c.firstDefeat("minecraft:warden"));
    }

    @Test
    void oldSavesTurnTheirGaugeIntoMaxQi() {
        Cultivation c = new Cultivation(0, 3, 50, 10, 121, 0);
        assertEquals(CultivationRules.floor(0, 3) + 50, c.maxQi());
        assertEquals(50, c.stageQi(), 1e-9);
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
    void calmRisesOnlyWithClosedEyesAndFallsNearAHostile() {
        Cultivation c = new Cultivation();
        c.startMeditation(0, 0, 0);
        c.gather(60);
        assertEquals(0, c.calm(), 1e-9);
        c.setEyesClosed(true);
        c.gather(60);
        assertEquals(0.5, c.calm(), 1e-9);
        c.setDisturbed(true);
        c.gather(6);
        assertEquals(0.3, c.calm(), 1e-9);
        c.setDisturbed(false);
        c.gather(1000);
        assertEquals(1, c.calm(), 1e-9);
        c.stopMeditation();
        assertEquals(0, c.calm(), 1e-9);
        assertFalse(c.eyesClosed());
    }

    @Test
    void eyesOnlyCloseWhileMeditating() {
        Cultivation c = new Cultivation();
        c.setEyesClosed(true);
        assertFalse(c.eyesClosed());
    }

    @Test
    void calmTiersAtAQuarterAHalfAndFull() {
        assertEquals(0, CultivationRules.calmTier(0.24));
        assertEquals(1, CultivationRules.calmTier(0.25));
        assertEquals(2, CultivationRules.calmTier(0.99));
        assertEquals(3, CultivationRules.calmTier(1));
    }

    @Test
    void wonTrialOpensTheNextRealmAtStageOne() {
        Cultivation c = new Cultivation();
        c.set(0, 9, CultivationRules.gauge(0, 9), 100, 0, 4);
        assertTrue(c.breakRealm());
        assertEquals(1, c.realm());
        assertEquals(1, c.stage());
        assertEquals(0, c.stageQi(), 1e-9);
        assertEquals(CultivationRules.peak(0, 9), c.maxQi());
    }

    @Test
    void lostTrialScattersPartOfTheGauge() {
        Cultivation c = new Cultivation();
        c.set(0, 9, CultivationRules.gauge(0, 9), 100, 0, 4);
        c.scatter(0.30);
        assertEquals(CultivationRules.gauge(0, 9) * 0.7, c.stageQi(), 1e-9);
        assertEquals(CultivationRules.floor(0, 9) + CultivationRules.gauge(0, 9) * 0.7, c.maxQiExact(), 1e-9);
        assertFalse(c.gaugeFull());
    }
}
