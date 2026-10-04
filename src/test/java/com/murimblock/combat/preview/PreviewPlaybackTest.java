package com.murimblock.combat.preview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreviewPlaybackTest {
    @Test
    void lateObserverSeeksToServerAgeWithExactlyOnePartialTick() {
        var clock = new PreviewPlayback();
        clock.observe(PreviewState.initial().begin(100).sample(108), 50);
        assertEquals(8, clock.age(50, 0));
        assertEquals(11.5F, clock.age(53, 0.5F));
    }

    @Test
    void repeatedAndLaggedSnapshotsNeverRestartOrRewind() {
        var clock = new PreviewPlayback();
        PreviewState state = PreviewState.initial().begin(100);
        clock.observe(state, 20);
        clock.observe(state, 24);
        assertEquals(4, clock.age(24, 0));
        clock.observe(state.sample(104), 27);
        assertEquals(7, clock.age(27, 0));
        clock.observe(state.sample(103), 28);
        assertEquals(8, clock.age(28, 0));
        clock.observe(state.sample(111), 29);
        assertEquals(11, clock.age(29, 0));
    }

    @Test
    void expiresEvenWithoutFurtherNetworkSnapshots() {
        var clock = new PreviewPlayback();
        clock.observe(PreviewState.initial().begin(100), 20);
        assertTrue(clock.isActive(43, 0.99F));
        assertFalse(clock.isActive(44, 0));
        assertEquals(24, clock.age(500, 0.5F));
    }

    @Test
    void stopIsTerminalForItsGenerationButNextActionStartsNormally() {
        var clock = new PreviewPlayback();
        PreviewState state = PreviewState.initial().begin(100);
        clock.observe(state, 20);
        clock.observe(state.stop(105), 25);
        assertFalse(clock.isActive(25, 0));
        clock.observe(state.sample(110), 26);
        assertFalse(clock.isActive(26, 0));
        clock.observe(state.stop(105).begin(120), 30);
        assertTrue(clock.isActive(30, 0));
        assertEquals(0, clock.age(30, 0));
        clock.observe(state.sample(121), 31);
        assertEquals(2, clock.generation());
        assertEquals(1, clock.age(31, 0));
    }

    @Test
    void frozenSimulationClockDoesNotDriftAndPartialTicksAreBounded() {
        var clock = new PreviewPlayback();
        clock.observe(PreviewState.initial().begin(100), 20);
        assertEquals(0.5F, clock.age(20, 0.5F));
        assertEquals(0.5F, clock.age(20, 0.5F));
        assertEquals(0, clock.age(19, -10));
        assertEquals(1, clock.age(20, 10));
        assertThrows(IllegalArgumentException.class, () -> clock.age(20, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> clock.age(20, Float.POSITIVE_INFINITY));
    }
}
