package com.murimblock.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MurimProfileScreenTest {
    @Test
    void qiFillTracksSyncedCurrentEnergyAndClampsToTheFrame() {
        assertEquals(0, MurimProfileScreen.computeFilledWidth(0, 100, 200));
        assertEquals(100, MurimProfileScreen.computeFilledWidth(50, 100, 200));
        assertEquals(200, MurimProfileScreen.computeFilledWidth(100, 100, 200));
        assertEquals(200, MurimProfileScreen.computeFilledWidth(200, 100, 200));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(-10, 100, 200));
        assertEquals(200, MurimProfileScreen.computeFilledWidth(Double.MAX_VALUE, Double.MAX_VALUE, 200));
    }

    @Test
    void invalidCapacityCannotCreateInvalidFillCoordinates() {
        assertEquals(0, MurimProfileScreen.computeFilledWidth(100, 0, 200));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(100, -1, 200));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(100, Double.NaN, 200));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(Double.NaN, 100, 200));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(Double.POSITIVE_INFINITY, 100, 200));
    }

    @Test
    void cultivationMeterUsesItsOwnWidthWithoutExceedingTheFrame() {
        assertEquals(0, MurimProfileScreen.computeFilledWidth(0, 100, 82));
        assertEquals(41, MurimProfileScreen.computeFilledWidth(50, 100, 82));
        assertEquals(82, MurimProfileScreen.computeFilledWidth(200, 100, 82));
        assertEquals(1, MurimProfileScreen.computeFilledWidth(1, 100, 82));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(50, 100, 0));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(50, 100, -1));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(Double.NaN, 100, 82));
    }

    @Test
    void metersUseTheCurrentResponsiveLayoutInsteadOfLegacyTextureWidths() {
        for (int width : new int[]{320, 427, 640, 1920}) {
            var layout = MurimProfileLayout.forViewport(width, 360);
            for (var meter : new MurimProfileLayout.Box[]{layout.profileQiFill, layout.cultivationQiFill}) {
                assertEquals(meter.width(), MurimProfileScreen.computeFilledWidth(100, 100, meter.width()));
                assertEquals((int) Math.round(meter.width() * 0.5), MurimProfileScreen.computeFilledWidth(50, 100, meter.width()));
                assertTrue(layout.content.contains(meter));
            }
        }
    }

    @Test
    void fillIsMonotonicAndBoundedForEveryEnergyLevel() {
        var layout = MurimProfileLayout.forViewport(320, 240);
        int width = layout.profileQiFill.width();
        int previous = 0;
        for (int qi = 0; qi <= 120; qi++) {
            int fill = MurimProfileScreen.computeFilledWidth(qi, 120, width);
            assertTrue(fill >= previous && fill <= width);
            previous = fill;
        }
    }
}
