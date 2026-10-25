package com.murimblock.client.gui.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murimblock.client.gui.SystemLayout;
import com.murimblock.client.gui.SystemLayout.Rect;
import org.junit.jupiter.api.Test;

class CultivationLayoutTest {
    private final CultivationLayout layout = CultivationLayout.of(SystemLayout.content(SystemLayout.window(640, 360)));

    @Test
    void bodySitsBelowTheSubTabs() {
        assertTrue(layout.body().y() > layout.subTabs().bottom());
    }

    @Test
    void realmColumnsLeaveRoomForTheDantian() {
        Rect[] c = layout.realmColumns();
        assertEquals(layout.body().x(), c[0].x());
        assertEquals(layout.body().right(), c[2].right());
        assertTrue(c[1].width() >= 2 * 38 + 40, "dantian column too narrow: " + c[1].width());
    }

    @Test
    void meridianViewportIsWideEnoughForAnArm() {
        Rect[] c = layout.meridianColumns();
        assertTrue(c[1].x() > c[0].right() && c[1].right() < c[2].x());
        assertTrue(c[1].width() >= 180, "viewport too narrow: " + c[1].width());
    }
}
