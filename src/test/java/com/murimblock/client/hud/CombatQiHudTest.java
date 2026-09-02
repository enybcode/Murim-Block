package com.murimblock.client.hud;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CombatQiHudTest {
    @Test
    void ratioIsZeroForEmptyQi() {
        assertEquals(0.0, CombatQiHud.computeQiRatio(0.0, 100.0), 1.0e-9);
    }

    @Test
    void ratioFollowsCurrentQiOverQiMax() {
        assertEquals(0.25, CombatQiHud.computeQiRatio(25.0, 100.0), 1.0e-9);
        assertEquals(0.5, CombatQiHud.computeQiRatio(50.0, 100.0), 1.0e-9);
        assertEquals(1.0, CombatQiHud.computeQiRatio(100.0, 100.0), 1.0e-9);
    }

    @Test
    void ratioClampsAboveOne() {
        assertEquals(1.0, CombatQiHud.computeQiRatio(150.0, 100.0), 1.0e-9);
    }

    @Test
    void ratioIsZeroWhenQiMaxIsInvalid() {
        assertEquals(0.0, CombatQiHud.computeQiRatio(50.0, 0.0), 1.0e-9);
        assertEquals(0.0, CombatQiHud.computeQiRatio(50.0, -100.0), 1.0e-9);
        assertEquals(0.0, CombatQiHud.computeQiRatio(50.0, Double.NaN), 1.0e-9);
    }

    @Test
    void ratioClampsNegativeQiToZero() {
        assertEquals(0.0, CombatQiHud.computeQiRatio(-25.0, 100.0), 1.0e-9);
    }

    @Test
    void filledWidthUsesVanillaExperienceWidth() {
        assertEquals(0, CombatQiHud.computeFilledWidth(0.0, 100.0));
        assertEquals(45, CombatQiHud.computeFilledWidth(25.0, 100.0));
        assertEquals(91, CombatQiHud.computeFilledWidth(50.0, 100.0));
        assertEquals(183, CombatQiHud.computeFilledWidth(100.0, 100.0));
        assertEquals(183, CombatQiHud.computeFilledWidth(150.0, 100.0));
        assertEquals(0, CombatQiHud.computeFilledWidth(-25.0, 100.0));
        assertEquals(0, CombatQiHud.computeFilledWidth(50.0, 0.0));
    }

    @Test
    void qiBarColorComesFromQiParticleIdentity() {
        assertEquals(0xFF268CFF, CombatQiHud.QI_COLOR);
    }

    @Test
    void textUsesQiFormatting() {
        assertEquals("Qi 52.5 / 100", CombatQiHud.buildText(52.5, 100.0));
        assertEquals("Qi 100 / 100", CombatQiHud.buildText(100.0, 100.0));
    }

    @Test
    void qiProgressSpriteKeepsVanillaExperienceDimensions() throws IOException {
        try (InputStream inputStream = CombatQiHudTest.class.getClassLoader()
                .getResourceAsStream("assets/murimblock/textures/gui/sprites/hud/qi_bar_progress.png")) {
            assertNotNull(inputStream);
            BufferedImage sprite = ImageIO.read(inputStream);
            assertNotNull(sprite);
            assertEquals(CombatQiHud.BAR_WIDTH, sprite.getWidth());
            assertEquals(CombatQiHud.BAR_HEIGHT, sprite.getHeight());
        }
    }
}
