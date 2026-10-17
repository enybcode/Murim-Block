package com.murimblock.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClothStrandTest {
    private static final long FRAME = 16_666_667L;   // 60 frames a second
    private static final double LINK = 0.2;
    /** The rest slant of a cape: down and a little back (+z). */
    private static final double SY = -Math.cos(Math.toRadians(8)), SZ = Math.sin(Math.toRadians(8));

    /** The strip hung at (0, 0, z), against the plane z = z (normal +z), for a number of frames from `from`. */
    private static long hold(ClothStrand c, double z, long from, int frames) {
        for (int i = 0; i < frames; i++) c.follow(0, 0, z, 0, SY, SZ, LINK, 0, 0, z, 0, 0, 1, (from + i) * FRAME);
        return from + frames;
    }

    @Test
    void aStillBodyLetsTheCapeHangCloseToItsRestSlant() {
        ClothStrand c = new ClothStrand(6);
        hold(c, 0, 0, 240);
        double[] bottom = c.joint(6);
        assertTrue(bottom[1] < -6 * LINK * 0.9, "the cape should hang down: " + bottom[1]);
        assertTrue(bottom[2] >= -1.0e-9, "the cape should stay behind the back: " + bottom[2]);
    }

    @Test
    void runningForwardLiftsTheCapeBehindThenItFallsBack() {
        ClothStrand c = new ClothStrand(6);
        long frame = hold(c, 0, 0, 60);
        // Running toward -z (forward), 6 blocks a second, for a second.
        double z = 0;
        for (int i = 1; i <= 60; i++) {
            z -= 0.1;
            c.follow(0, 0, z, 0, SY, SZ, LINK, 0, 0, z, 0, 0, 1, (frame + i) * FRAME);
        }
        frame += 61;
        double[] flying = c.joint(6);
        assertTrue(flying[2] - z > 0.4, "the cape should fly out behind: " + (flying[2] - z));
        hold(c, z, frame, 300);
        double[] settled = c.joint(6);
        assertTrue(settled[2] - z < flying[2] - z, "the cape should fall back once he stops");
    }

    @Test
    void walkingBackwardNeverPushesTheCapeThroughTheBody() {
        ClothStrand c = new ClothStrand(6);
        long frame = hold(c, 0, 0, 30);
        double z = 0;
        for (int i = 1; i <= 120; i++) {
            z += 0.1;
            c.follow(0, 0, z, 0, SY, SZ, LINK, 0, 0, z, 0, 0, 1, (frame + i) * FRAME);
            for (int k = 1; k <= 6; k++) assertTrue(c.joint(k)[2] - z > -1.0e-6, "joint " + k + " went through the back");
        }
    }

    @Test
    void theLinksKeepTheirLength() {
        ClothStrand c = new ClothStrand(5);
        for (int i = 0; i < 300; i++) {
            double a = i * 0.9;
            c.follow(Math.cos(a), Math.sin(a) * 0.5, Math.sin(a * 0.3), 0, -1, 0, LINK, 0, -100, 0, 0, 1, 0, i * FRAME);
        }
        for (int k = 1; k <= 5; k++) {
            double[] a = c.joint(k - 1), b = c.joint(k);
            double d = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
            assertEquals(LINK, d, LINK * 0.15, "link " + k);
        }
    }

    @Test
    void anUndrawnStripIsLaidBackAtRest() {
        ClothStrand c = new ClothStrand(4);
        hold(c, 0, 0, 10);
        c.follow(5, 0, 0, 0, -1, 0, LINK, 5, 0, 0, 0, 0, 1, 10 * FRAME + 2_000_000_000L);
        assertEquals(-4 * LINK, c.joint(4)[1], 1.0e-9);
        assertEquals(5, c.joint(4)[0], 1.0e-9);
    }
}
