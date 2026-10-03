package com.murimblock.client.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MurimProfileScreenTest {
    @Test
    void qiFillTracksSyncedCurrentEnergyAndClampsToTheFrame() {
        assertEquals(0, MurimProfileScreen.computeFilledWidth(0, 100));
        assertEquals(30, MurimProfileScreen.computeFilledWidth(50, 100));
        assertEquals(59, MurimProfileScreen.computeFilledWidth(100, 100));
        assertEquals(59, MurimProfileScreen.computeFilledWidth(200, 100));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(-10, 100));
        assertEquals(59, MurimProfileScreen.computeFilledWidth(Double.MAX_VALUE, Double.MAX_VALUE));
    }

    @Test
    void invalidCapacityCannotCreateInvalidTextureCoordinates() {
        assertEquals(0, MurimProfileScreen.computeFilledWidth(100, 0));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(100, -1));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(100, Double.NaN));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(Double.NaN, 100));
        assertEquals(0, MurimProfileScreen.computeFilledWidth(Double.POSITIVE_INFINITY, 100));
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
    void allPagesArePackagedAtTheMinecraftMinimumGuiSize() throws IOException {
        for (String page : new String[]{"profile", "techniques", "cultivation", "infos"}) {
            BufferedImage image = texture("murim_" + page + "_v4");
            assertEquals(MurimProfileScreen.PANEL_WIDTH, image.getWidth(), page);
            assertEquals(MurimProfileScreen.PANEL_HEIGHT, image.getHeight(), page);
            assertTrue(image.getWidth() <= 320 && image.getHeight() <= 240, page);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    assertTrue(alpha == 0 || alpha == 255, page + " contains blended edge pixels");
                }
            }
        }
    }

    @Test
    void qiFillTextureMatchesTheRenderer() throws IOException {
        BufferedImage image = texture("murim_qi_fill_v4");
        assertEquals(MurimProfileScreen.QI_FILL_WIDTH, image.getWidth());
        assertEquals(5, image.getHeight());
    }

    private BufferedImage texture(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("assets/murimblock/textures/gui/" + name + ".png")) {
            assertNotNull(input, name);
            BufferedImage image = ImageIO.read(input);
            assertNotNull(image, name);
            return image;
        }
    }
}
