package com.murimblock.qi.charge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QiChargeStateTest {
    @Test
    void initialStateIsNotCharging() {
        QiChargeState state = new QiChargeState();

        assertFalse(state.isCharging());
    }

    @Test
    void startMarksStateAsCharging() {
        QiChargeState state = new QiChargeState();

        state.start();

        assertTrue(state.isCharging());
    }

    @Test
    void stopMarksStateAsNotCharging() {
        QiChargeState state = new QiChargeState(true);

        state.stop();

        assertFalse(state.isCharging());
    }
}
