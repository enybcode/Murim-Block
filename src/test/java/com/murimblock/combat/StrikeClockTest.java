package com.murimblock.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrikeClockTest {
    private static final int WINDUP = 3;
    private static final int ACTIVE = 2;
    private static final int RECOVERY = 7;
    private static final int LENGTH = WINDUP + ACTIVE + RECOVERY;

    private static StrikeClock jab() {
        StrikeClock clock = new StrikeClock();
        clock.strike(WINDUP, ACTIVE, RECOVERY);
        return clock;
    }

    @Test
    void activeOnlyAfterWindupThenBackToIdle() {
        StrikeClock clock = jab();
        StringBuilder phases = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            phases.append(clock.active() ? 'A' : clock.starting() ? 'S' : '.');
            clock.advance();
        }
        assertEquals("S..AA.......", phases.toString());
        assertEquals(FighterState.IDLE, clock.state());
    }

    @Test
    void hitStopDelaysTheRestOfTheMove() {
        StrikeClock clock = jab();
        int ticks = 0;
        while (!clock.active()) {
            clock.advance();
            ticks++;
        }
        clock.hitStop(3);
        while (clock.state() == FighterState.ATTACK) {
            clock.advance();
            ticks++;
        }
        assertEquals(LENGTH + 3, ticks);
    }

    @Test
    void eachTargetStruckOncePerStrike() {
        StrikeClock clock = jab();
        assertTrue(clock.markStruck(7));
        assertFalse(clock.markStruck(7));
        clock.strike(WINDUP, ACTIVE, RECOVERY);
        assertTrue(clock.markStruck(7));
    }

    @Test
    void newStrikeOnlyWhenIdleOrNearlyRecovered() {
        StrikeClock clock = new StrikeClock();
        assertTrue(clock.canStrike(0));
        clock.strike(WINDUP, ACTIVE, RECOVERY);
        assertFalse(clock.canStrike(3));
        for (int i = 0; i < LENGTH - 3; i++) {
            clock.advance();
        }
        assertFalse(clock.canStrike(0));
        assertTrue(clock.canStrike(3));
    }

    @Test
    void stunWearsOff() {
        StrikeClock clock = jab();
        clock.stun(2);
        assertFalse(clock.canStrike(0));
        clock.advance();
        clock.advance();
        assertEquals(FighterState.IDLE, clock.state());
    }
}
