package com.murimblock.client.cultivation.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VisionRulesTest {
    @Test
    void aMortalSeesNothingAndTheReachGrowsByRealm() {
        assertEquals(0, VisionRules.blockReach(0));
        assertEquals(0, VisionRules.starReach(0));
        assertEquals(6, VisionRules.blockReach(1));
        assertEquals(64, VisionRules.starReach(5));
        // Realms past Transcendent see as Transcendent.
        assertEquals(32, VisionRules.blockReach(7));
    }

    @Test
    void onlyTheFirstSeeingRealmFlickersAndHalosComeAtFirstRate() {
        assertTrue(VisionRules.flickers(1));
        assertFalse(VisionRules.flickers(2));
        assertFalse(VisionRules.halos(2));
        assertTrue(VisionRules.halos(3));
    }

    @Test
    void biggerBodiesGiveBiggerStars() {
        float chicken = VisionRules.starRadius(0.4f, 0.7f);
        float golem = VisionRules.starRadius(1.4f, 2.7f);
        assertEquals(0.145, chicken, 0.005);
        assertEquals(0.52, golem, 0.01);
    }

    @Test
    void strongerMobsTwinkleFaster() {
        float chicken = VisionRules.strength(4, 0);
        float zombie = VisionRules.strength(20, 3);
        float golem = VisionRules.strength(100, 15);
        assertTrue(chicken < zombie && zombie < golem);
        assertEquals(1, golem, 1e-6);
        assertTrue(VisionRules.twinkleHz(chicken) < VisionRules.twinkleHz(golem));
    }
}
