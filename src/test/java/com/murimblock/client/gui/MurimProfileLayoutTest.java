package com.murimblock.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static com.murimblock.client.gui.MurimProfileLayout.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MurimProfileLayoutTest {
    @Test
    void onlyProfileHasAPlayerPreview() {
        assertEquals(java.util.List.of(Page.PROFILE),
                Arrays.stream(Page.values()).filter(Page::showsPlayer).toList());
    }

    @Test
    void everyPageFitsTheMinimumMinecraftViewportWithoutOverlappingPanels() {
        Box screen = new Box(0, 0, 320, 240);
        assertTrue(screen.contains(new Box(0, 0, MurimProfileScreen.PANEL_WIDTH, MurimProfileScreen.PANEL_HEIGHT)));
        for (Page page : Page.values()) {
            var panels = PANELS.get(page);
            for (int i = 0; i < panels.size(); i++) {
                assertTrue(CONTENT.contains(panels.get(i)), page + " panel outside content area");
                for (int j = i + 1; j < panels.size(); j++) {
                    assertFalse(panels.get(i).intersects(panels.get(j)), page + " panels overlap");
                }
            }
        }
    }

    @Test
    void everyTextFieldHasItsOwnAreaInsideAPanelAndOutsideTheIllustrations() {
        for (Page page : Page.values()) {
            var areas = new ArrayList<>(TEXT.get(page).entrySet());
            for (int i = 0; i < areas.size(); i++) {
                var area = areas.get(i);
                Box box = area.getValue();
                assertTrue(box.height() >= 9, page + " " + area.getKey() + " cannot fit a text line");
                assertTrue(PANELS.get(page).stream().anyMatch(panel -> panel.contains(box)), page + " " + area.getKey());
                for (Box decoration : decorations(page)) {
                    assertFalse(box.intersects(decoration), page + " " + area.getKey() + " overlaps artwork");
                }
                for (int j = i + 1; j < areas.size(); j++) {
                    assertFalse(box.intersects(areas.get(j).getValue()), page + " text fields overlap");
                }
            }
        }
    }

    @Test
    void tabLabelsAndIconsFitTheirClickTargetsWithAFourPixelGap() {
        Page[] pages = Page.values();
        for (int i = 0; i < pages.length; i++) {
            Page page = pages[i];
            assertTrue(page.tab.contains(page.tabLabel()));
            assertTrue(page.tab.contains(page.tabIcon()));
            assertTrue(page.tabLabel().y() - page.tabIcon().bottom() >= 4);
            for (int j = i + 1; j < pages.length; j++) {
                assertFalse(page.tab.intersects(pages[j].tab));
            }
        }
    }

    @Test
    void layoutRejectsInvalidDimensionsAndUnknownFields() {
        assertThrows(IllegalArgumentException.class, () -> new Box(0, 0, 0, 9));
        assertThrows(IllegalArgumentException.class, () -> new Box(0, 0, 9, -1));
        assertThrows(IllegalArgumentException.class, () -> Page.TECHNIQUES.text(Field.NAME));
    }

    @Test
    void hitTestingMatchesMinecraftExclusiveRightAndBottomEdges() {
        Box tab = Page.PROFILE.tab;
        assertTrue(tab.contains(tab.x(), tab.y()));
        assertTrue(tab.contains(tab.right() - 1, tab.bottom() - 1));
        assertFalse(tab.contains(tab.right(), tab.y()));
        assertFalse(tab.contains(tab.x(), tab.bottom()));
    }
}
