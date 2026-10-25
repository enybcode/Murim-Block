package com.murimblock.client.gui.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murimblock.client.gui.SystemLayout;
import org.junit.jupiter.api.Test;

class ProfileLayoutTest {
    @Test
    void columnsFillTheContentWithoutOverlapping() {
        SystemLayout.Rect area = SystemLayout.content(SystemLayout.window(640, 360));
        ProfileLayout layout = ProfileLayout.of(area);
        assertEquals(area.x(), layout.model().x());
        assertEquals(area.right(), layout.qi().right());
        assertEquals(layout.model().right() + ProfileLayout.GAP, layout.identity().x());
        assertEquals(layout.qi().x() - ProfileLayout.GAP, layout.identity().right());
        assertTrue(layout.identity().width() >= 180, "identity column too narrow: " + layout.identity().width());
    }
}
