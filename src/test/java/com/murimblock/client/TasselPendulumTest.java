package com.murimblock.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TasselPendulumTest {
    private static final long FRAME = 16_666_667L;   // 60 frames a second
    private static final double CORD = 0.2;

    /** Holds the pivot at x for a number of frames, from the frame `from`; returns the next frame. */
    private static long hold(TasselPendulum p, double x, long from, int frames) {
        for (int i = 0; i < frames; i++) p.follow(x, 0, 0, CORD, 0, -1, 0, (from + i) * FRAME);
        return from + frames;
    }

    @Test
    void aStillHandLetsTheTasselHangStraightDown() {
        TasselPendulum p = new TasselPendulum();
        hold(p, 0, 0, 120);
        double[] d = p.direction();
        assertEquals(-1, d[1], 1.0e-6);
    }

    @Test
    void theTasselTrailsBehindAMovingHandThenSwingsBackAndSettles() {
        TasselPendulum p = new TasselPendulum();
        long frame = hold(p, 0, 0, 30);
        // The hand sweeps 1 block to +x in a fifth of a second, like a cut.
        for (int i = 1; i <= 12; i++) p.follow(i / 12.0, 0, 0, CORD, 0, -1, 0, (frame + i) * FRAME);
        frame += 13;
        double[] trailing = p.direction();
        assertTrue(trailing[0] < -0.3, "the tassel should lag behind the hand: " + trailing[0]);
        // The hand stops: the tassel swings past straight down, then settles.
        boolean swungPast = false;
        for (int i = 0; i < 60; i++) {
            p.follow(1, 0, 0, CORD, 0, -1, 0, (frame + i) * FRAME);
            swungPast |= p.direction()[0] > 0.1;
        }
        assertTrue(swungPast, "the tassel should swing back past its rest");
        hold(p, 1, frame + 60, 240);
        double[] settled = p.direction();
        assertTrue(settled[1] < -0.99, "the tassel should settle hanging down: " + settled[1]);
    }

    @Test
    void aViolentWhirlNeverBreaksTheCord() {
        TasselPendulum p = new TasselPendulum();
        for (int i = 0; i < 300; i++) {
            double a = i * 1.7;
            p.follow(Math.cos(a) * 3, Math.sin(a) * 3, Math.sin(a * 0.3), CORD, 0, -1, 0, i * FRAME);
            double[] d = p.direction();
            double length = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            assertEquals(1, length, 1.0e-6, "the direction must stay a unit vector");
        }
    }

    @Test
    void anUndrawnTasselIsLaidBackAtRest() {
        TasselPendulum p = new TasselPendulum();
        long frame = hold(p, 0, 0, 10);
        p.follow(1, 0, 0, CORD, 0, -1, 0, (frame) * FRAME);
        // Unseen for a second, then drawn elsewhere: it hangs, it does not fly.
        p.follow(5, 0, 0, CORD, 0, -1, 0, (frame + 60) * FRAME);
        assertEquals(-1, p.direction()[1], 1.0e-6);
    }

    @Test
    void inAMenuDownIsTheScreensDown() {
        TasselPendulum p = new TasselPendulum();
        for (int i = 0; i < 60; i++) p.follow(100, 50, 0, 30, 0, 1, 0, i * FRAME);
        assertEquals(1, p.direction()[1], 1.0e-6);
    }
}
