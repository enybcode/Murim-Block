package com.murimblock.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QiDebugOverlayTest {
    @Test
    void buildsTemporaryQiTextWithSharedFormatting() {
        assertEquals("Qi : 72.5 / 100", QiDebugOverlay.buildText(72.5, 100.0));
    }

    @Test
    void positionsTextFromBottomRightCorner() {
        assertEquals(150, QiDebugOverlay.getRightAlignedX(200, 40));
        assertEquals(175, QiDebugOverlay.getBottomAlignedY(200, 9));
    }

    @Test
    void positionsStayOnScreenWhenTextIsWiderThanScreen() {
        assertEquals(0, QiDebugOverlay.getRightAlignedX(40, 80));
        assertEquals(0, QiDebugOverlay.getBottomAlignedY(20, 30));
    }
}
