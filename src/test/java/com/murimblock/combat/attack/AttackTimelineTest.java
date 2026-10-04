package com.murimblock.combat.attack;

import org.junit.jupiter.api.Test;

import static com.murimblock.combat.attack.AttackTimeline.Phase.*;
import static org.junit.jupiter.api.Assertions.*;

class AttackTimelineTest {
    private final AttackTimeline timeline = new AttackTimeline(3, 2, 4);

    @Test
    void phasesUseHalfOpenServerTickIntervals() {
        assertEquals(NOT_STARTED, timeline.phaseAt(-0.01));
        assertEquals(WINDUP, timeline.phaseAt(0));
        assertEquals(WINDUP, timeline.phaseAt(2.99));
        assertEquals(CONTACT, timeline.phaseAt(3));
        assertEquals(CONTACT, timeline.phaseAt(4.99));
        assertEquals(RECOVERY, timeline.phaseAt(5));
        assertEquals(RECOVERY, timeline.phaseAt(8.99));
        assertEquals(FINISHED, timeline.phaseAt(9));
        assertEquals(5, timeline.contactEndsAt());
        assertEquals(9, timeline.durationTicks());
    }

    @Test
    void optionalWindupAndRecoveryCanBeEmpty() {
        var immediate = new AttackTimeline(0, 1, 0);
        assertEquals(CONTACT, immediate.phaseAt(0));
        assertEquals(FINISHED, immediate.phaseAt(1));
    }

    @Test
    void skippedContactPhaseIsStillReturnedByIntervalClipping() {
        assertEquals(new AttackTimeline.ContactWindow(3, 5), timeline.contactWindow(0, 9).orElseThrow());
        assertEquals(new AttackTimeline.ContactWindow(3.5, 4.5), timeline.contactWindow(3.5, 4.5).orElseThrow());
        assertEquals(new AttackTimeline.ContactWindow(3, 3.5), timeline.contactWindow(-10, 3.5).orElseThrow());
    }

    @Test
    void touchingAnExclusiveBoundaryOrEmptyIntervalDoesNotCreateContactTime() {
        assertTrue(timeline.contactWindow(0, 3).isEmpty());
        assertTrue(timeline.contactWindow(5, 9).isEmpty());
        assertTrue(timeline.contactWindow(4, 4).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> timeline.contactWindow(5, 4));
    }

    @Test
    void invalidDurationsAndOverflowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AttackTimeline(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new AttackTimeline(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new AttackTimeline(1, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new AttackTimeline(Integer.MAX_VALUE, 1, 0));
        assertEquals(Integer.MAX_VALUE, new AttackTimeline(Integer.MAX_VALUE - 1, 1, 0).durationTicks());
    }

    @Test
    void nonFiniteTimesAndInvalidWindowsAreRejected() {
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> timeline.phaseAt(invalid));
            assertThrows(IllegalArgumentException.class, () -> timeline.contactWindow(invalid, 5));
            assertThrows(IllegalArgumentException.class, () -> timeline.contactWindow(3, invalid));
            assertThrows(IllegalArgumentException.class, () -> new AttackTimeline.ContactWindow(3, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> new AttackTimeline.ContactWindow(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new AttackTimeline.ContactWindow(1, 1));
    }
}
