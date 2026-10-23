package com.murimblock.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SystemLayoutTest {
    @Test
    void windowKeepsItsFullSizeOnALargeScreen() {
        // 1920 x 1080 at GUI scale 3.
        SystemLayout.Rect w = SystemLayout.window(640, 360);
        assertEquals(SystemLayout.MAX_WIDTH, w.width());
        assertEquals(SystemLayout.MAX_HEIGHT, w.height());
        assertEquals((640 - w.width()) / 2, w.x());
    }

    @Test
    void windowShrinksToFitAndLeavesRoomForTheTitleTag() {
        // 1920 x 1080 at GUI scale 4.
        SystemLayout.Rect w = SystemLayout.window(480, 270);
        assertEquals(480 - 2 * SystemLayout.MARGIN, w.width());
        assertTrue(w.y() >= SystemLayout.MARGIN + SystemLayout.TITLE_ROOM / 2);
        assertTrue(w.bottom() <= 270 - SystemLayout.MARGIN);
    }

    @Test
    void displayFactorIsWholeOnCommonScreensAndAlwaysFits() {
        assertEquals(2f, SystemLayout.displayFactor(1920, 1080));
        assertEquals(4f, SystemLayout.displayFactor(3840, 2160));
        assertEquals(2f, SystemLayout.displayFactor(1366, 768));
        assertEquals(2f, SystemLayout.displayFactor(1280, 720));
        assertEquals(1f, SystemLayout.displayFactor(854, 480));
        assertTrue(SystemLayout.displayFactor(400, 240) < 1, "a tiny screen gets a fraction");
        for (int[] screen : new int[][] {{1366, 768}, {1280, 720}, {854, 480}, {2560, 1440}, {1600, 900}, {400, 240}}) {
            float f = SystemLayout.displayFactor(screen[0], screen[1]);
            assertTrue((SystemLayout.MAX_WIDTH + 2 * SystemLayout.MARGIN) * f <= screen[0], "too wide at " + screen[0]);
            assertTrue((SystemLayout.MAX_HEIGHT + 2 * SystemLayout.MARGIN + SystemLayout.TITLE_ROOM) * f <= screen[1],
                    "too tall at " + screen[1]);
        }
    }

    @Test
    void tabRowIsCentredWithGaps() {
        int[] xs = SystemLayout.tabRow(300, new int[] {40, 60});
        int first = SystemLayout.tabWidth(40);
        int second = SystemLayout.tabWidth(60);
        int total = first + SystemLayout.TAB_GAP + second;
        assertEquals(300 - total / 2, xs[0]);
        assertEquals(xs[0] + first + SystemLayout.TAB_GAP, xs[1]);
    }

    @Test
    void contentSitsBelowTheTabsInsideTheBorder() {
        SystemLayout.Rect w = SystemLayout.window(640, 360);
        SystemLayout.Rect c = SystemLayout.content(w);
        assertTrue(c.y() >= w.y() + SystemLayout.TAB_TOP + SystemLayout.TAB_HEIGHT);
        assertTrue(c.x() > w.x() && c.right() < w.right() && c.bottom() < w.bottom());
    }
}
