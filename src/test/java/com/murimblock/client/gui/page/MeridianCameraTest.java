package com.murimblock.client.gui.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MeridianCameraTest {
    @Test
    void fitCentresOnTheBoxAndZoomsIn() {
        // The Lung meridian: chest to thumb on the left arm.
        MeridianCamera.View v = MeridianCamera.fit(4, 92, 46, 196, 206, 230);
        assertEquals(25f, v.x(), 0.01f);
        assertEquals(144f, v.y(), 0.01f);
        assertTrue(v.scale() > 1, "an arm should be zoomed in: " + v.scale());
    }

    @Test
    void fitNeverZoomsPastTheLimits() {
        assertEquals(MeridianCamera.MAX_SCALE, MeridianCamera.fit(50, 50, 50, 50, 2000, 2000).scale(), 0.001f);
        assertEquals(MeridianCamera.MIN_SCALE, MeridianCamera.fit(0, 0, 2000, 2000, 100, 100).scale(), 0.001f);
    }

    @Test
    void followPlacesAtOnceThenGlides() {
        MeridianCamera camera = new MeridianCamera();
        MeridianCamera.View a = new MeridianCamera.View(0, 0, 1);
        MeridianCamera.View b = new MeridianCamera.View(100, 0, 2);
        assertEquals(a, camera.follow(a, 1));
        MeridianCamera.View mid = camera.follow(b, 0.05f);
        assertTrue(mid.x() > 0 && mid.x() < 100, "should glide: " + mid.x());
        assertEquals(b, camera.follow(b, 10));
    }
}
