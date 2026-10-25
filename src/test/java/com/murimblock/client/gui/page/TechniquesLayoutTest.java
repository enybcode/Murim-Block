package com.murimblock.client.gui.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murimblock.client.gui.SystemLayout;
import org.junit.jupiter.api.Test;

class TechniquesLayoutTest {
    @Test
    void columnsFillTheContentWithoutOverlapping() {
        SystemLayout.Rect area = SystemLayout.content(SystemLayout.window(640, 360));
        TechniquesLayout layout = TechniquesLayout.of(area);
        assertEquals(area.x(), layout.arts().x());
        assertEquals(layout.arts().right() + TechniquesLayout.GAP, layout.techniques().x());
        assertEquals(layout.techniques().right() + TechniquesLayout.GAP, layout.detail().x());
        assertEquals(area.right(), layout.detail().right());
        assertTrue(layout.detail().width() >= 160, "detail column too narrow: " + layout.detail().width());
    }
}
