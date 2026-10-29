package com.murimblock.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MurimMapTest {
    private static final int EXTENT = 6000;
    private static final int STEP = 25;

    @Test
    void eachSpotLiesInItsBiome() {
        for (MurimMap.Spot spot : MurimMap.SPOTS) {
            double[] block = MurimMap.unwarp(spot.x(), spot.z());
            assertEquals(spot.biome(), MurimMap.biome(block[0], block[1]), spot.name());
            // Not on a border: the whole 64-block square around it is the same biome.
            for (int dx = -32; dx <= 32; dx += 16) {
                for (int dz = -32; dz <= 32; dz += 16) {
                    assertEquals(spot.biome(), MurimMap.biome(block[0] + dx, block[1] + dz), spot.name() + " surroundings");
                }
            }
        }
    }

    @Test
    void unwarpInvertsTheWarp() {
        for (MurimMap.Spot spot : MurimMap.SPOTS) {
            double[] block = MurimMap.unwarp(spot.x(), spot.z());
            double[] back = MurimMap.warp(block[0], block[1]);
            assertEquals(spot.x(), back[0], 0.5, spot.name());
            assertEquals(spot.z(), back[1], 0.5, spot.name());
        }
    }

    @Test
    void landInTheMiddleOceanAround() {
        assertTrue(MurimMap.landDistance(0, 0) > 2000, "the centre is deep inland");
        for (int[] far : new int[][] {{0, -7000}, {0, 7000}, {-7000, 0}, {7000, 0}, {6000, 6000}}) {
            assertEquals(MurimBiome.OCEAN, MurimMap.biome(far[0], far[1]), far[0] + ", " + far[1]);
            assertTrue(MurimMap.landDistance(far[0], far[1]) < -500, "deep sea at " + far[0] + ", " + far[1]);
        }
    }

    /** Every biome takes a real share of the land, and the map is drawn to build/murim-map/murim-map.png. */
    @Test
    void everyBiomeIsThereAndTheMapIsDrawn() throws IOException {
        int size = 2 * EXTENT / STEP;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Map<MurimBiome, Integer> counts = new EnumMap<>(MurimBiome.class);
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                MurimBiome biome = MurimMap.biome(-EXTENT + (i + .5) * STEP, -EXTENT + (j + .5) * STEP);
                counts.merge(biome, 1, Integer::sum);
                image.setRGB(i, j, COLORS[biome.ordinal()]);
            }
        }
        File out = new File("build/murim-map/murim-map.png");
        out.getParentFile().mkdirs();
        ImageIO.write(image, "png", out);
        for (MurimBiome biome : MurimBiome.values()) {
            // 40 pixels of 25 x 25 blocks: at least 25 000 square blocks each.
            assertTrue(counts.getOrDefault(biome, 0) >= 40, biome + " covers " + counts.getOrDefault(biome, 0) + " pixels");
        }
    }

    // The mockup's zone colours, in MurimBiome order.
    private static final int[] COLORS = {
        0xDCE3E8, 0xBDB39C, 0xC9C291, 0xA9C77A, 0xE3ECF3, 0xDDBB66, 0xCBD47C, 0x8FC68C, 0xB6C2C6, 0xC48C70,
        0x5C9E5A, 0x7CC4B2, 0xA3CCAE, 0xAAAAA0, 0x7E9A8A, 0x3E9A48, 0xA2C6B0, 0xBCDB8E, 0x5FA867, 0x8CC490,
        0xE4D7A8, 0x3D6E9C,
    };
}
