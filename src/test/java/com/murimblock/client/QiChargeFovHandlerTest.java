package com.murimblock.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QiChargeFovHandlerTest {
    private static final float EPSILON = 1.0e-6F;

    @Test
    void transitionIncreasesWhileCharging() {
        assertEquals(0.25F, QiChargeFovHandler.nextTransition(0.0F, true, 0.25F), EPSILON);
    }

    @Test
    void transitionDecreasesAfterChargingStops() {
        assertEquals(0.75F, QiChargeFovHandler.nextTransition(1.0F, false, 0.25F), EPSILON);
    }

    @Test
    void fovSettlesAtEightyFivePercent() {
        assertEquals(0.85F, QiChargeFovHandler.fovFactor(1.0F), EPSILON);
    }

    @Test
    void transitionIsClamped() {
        assertEquals(1.0F, QiChargeFovHandler.nextTransition(0.95F, true, 0.25F), EPSILON);
        assertEquals(0.0F, QiChargeFovHandler.nextTransition(0.05F, false, 0.25F), EPSILON);
    }
}
