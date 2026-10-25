package com.murimblock.client.gui.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murimblock.client.gui.SystemLayout;
import com.murimblock.client.gui.SystemLayout.Rect;
import org.junit.jupiter.api.Test;

class InfoLayoutTest {
    private final InfoLayout layout = InfoLayout.of(SystemLayout.content(SystemLayout.window(640, 360)));

    @Test
    void questDetailTakesTheRestOfTheWidth() {
        Rect[] c = layout.questColumns();
        assertEquals(c[0].right() + InfoLayout.GAP, c[1].x());
        assertEquals(layout.body().right(), c[1].right());
        assertTrue(c[1].width() >= 300, "quest detail too narrow: " + c[1].width());
    }

    @Test
    void guideColumnsDoNotOverlap() {
        Rect[] c = layout.guideColumns();
        assertTrue(c[0].right() < c[1].x() && c[1].right() < c[2].x());
        assertEquals(layout.body().right(), c[2].right());
        assertTrue(c[2].width() >= 180, "topic text too narrow: " + c[2].width());
    }
}
