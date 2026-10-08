package com.murimblock.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.murimblock.client.gui.MurimProfileLayout.*;
import static org.junit.jupiter.api.Assertions.*;

class MurimProfileLayoutTest {
    @Test void onlyProfileHasAPlayerPreview() {
        assertEquals(java.util.List.of(Page.PROFILE), Arrays.stream(Page.values()).filter(Page::showsPlayer).toList());
    }
    @ParameterizedTest
    @CsvSource({"320,240", "427,240", "640,360", "854,480", "1920,1080"})
    void layoutFitsViewportAndKeepsTextAndControlsSeparated(int width, int height) {
        MurimProfileLayout layout = forViewport(width, height);
        Box viewport = new Box(0, 0, width, height);
        assertTrue(viewport.contains(new Box(0, 0, layout.width + 16, layout.height + 16)));
        assertTrue(layout.width <= MAX_WIDTH && layout.height <= MAX_HEIGHT);
        for (Page page : Page.values()) {
            var panels = layout.panels(page);
            assertTrue(layout.content.contains(panels.get(0)));
            assertTrue(layout.content.contains(panels.get(1)));
            assertFalse(panels.get(0).intersects(panels.get(1)));
            var fields = new ArrayList<>(layout.fields(page).entrySet());
            for (int i = 0; i < fields.size(); i++) {
                Box box = fields.get(i).getValue();
                assertTrue(box.height() >= 9);
                assertTrue(layout.content.contains(box), page + " " + fields.get(i));
                if (page == Page.PROFILE) assertFalse(box.intersects(layout.player));
                for (int j = i + 1; j < fields.size(); j++) {
                    assertFalse(box.intersects(fields.get(j).getValue()), page + " fields overlap: " + fields.get(i) + " / " + fields.get(j));
                }
            }
            Box tab = layout.tab(page);
            assertTrue(layout.tab(page).contains(layout.tabLabel(page)));
            assertTrue(viewport.contains(tab));
            for (Page other : Page.values()) if (page != other) assertFalse(tab.intersects(layout.tab(other)));
        }
        assertTrue(layout.content.contains(layout.infoSettings));
        for (Field field : new Field[]{Field.KEY_PROFILE, Field.KEY_QI, Field.KEY_COMBAT}) {
            assertFalse(layout.infoSettings.intersects(layout.text(Page.INFOS, field)));
        }
        assertTrue(layout.content.contains(layout.keybinds));
        for (int i = 0; i < layout.settingRows.size(); i++) {
            Box row = layout.settingRows.get(i);
            assertTrue(layout.content.contains(row));
            assertFalse(row.intersects(layout.settingsHeader));
            assertFalse(row.intersects(layout.settingsStatus));
            assertFalse(row.intersects(layout.keybinds));
            for (int j = i + 1; j < layout.settingRows.size(); j++) assertFalse(row.intersects(layout.settingRows.get(j)));
        }
        for (var entry : layout.fields(Page.PROFILE).entrySet()) assertFalse(entry.getValue().intersects(layout.profileQiFill));
        for (var entry : layout.fields(Page.CULTIVATION).entrySet()) assertFalse(entry.getValue().intersects(layout.cultivationQiFill));
    }
    @Test void compactMenuIsLargerThanThePreviousContentAreaWithoutShrinkingText() {
        MurimProfileLayout layout = forViewport(427, 240);
        assertTrue(layout.width > 320);
        assertTrue(layout.content.height() > 137);
        assertEquals(18, layout.title.height());
    }
    @Test void boxesRejectInvalidDimensionsAndUnknownFields() {
        assertThrows(IllegalArgumentException.class, () -> new Box(0, 0, 0, 9));
        assertThrows(IllegalArgumentException.class, () -> new Box(0, 0, 9, -1));
        assertThrows(IllegalArgumentException.class, () -> forViewport(120, 100));
        assertThrows(IllegalArgumentException.class, () -> forViewport(320, 240).text(Page.TECHNIQUES, Field.NAME));
    }
    @Test void hitTestingUsesExclusiveRightAndBottomEdges() {
        Box tab = forViewport(320, 240).tab(Page.PROFILE);
        assertTrue(tab.contains(tab.x(), tab.y()));
        assertTrue(tab.contains(tab.right() - 1, tab.bottom() - 1));
        assertFalse(tab.contains(tab.right(), tab.y()));
        assertFalse(tab.contains(tab.x(), tab.bottom()));
    }
}
