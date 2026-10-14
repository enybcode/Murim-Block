package com.murimblock.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudSizeTest {
    @Test
    void normalKeepsTheGuiScale() {
        for (int gui = 1; gui <= 6; gui++) assertEquals(gui, HudSize.NORMAL.pixels(gui, 3840));
    }

    @Test
    void sizesStepOneScreenPixelAtATime() {
        assertEquals(2, HudSize.SMALL.pixels(3, 1920));
        assertEquals(4, HudSize.LARGE.pixels(3, 1920));
        assertEquals(5, HudSize.HUGE.pixels(3, 1920));
    }

    @Test
    void neverBelowOnePixel() {
        assertEquals(1, HudSize.SMALL.pixels(1, 1920));
    }

    @Test
    void largerSizesStopWhereTheHudNoLongerFits() {
        // 1280 px wide: 5 screen pixels per HUD pixel at most (240 x 5 = 1200).
        assertEquals(5, HudSize.HUGE.pixels(4, 1280));
        assertEquals(5, HudSize.HUGE.pixels(5, 1280));
        // A window already at its largest GUI scale is never shrunk by a larger size.
        assertEquals(4, HudSize.LARGE.pixels(4, 854));
    }
}
