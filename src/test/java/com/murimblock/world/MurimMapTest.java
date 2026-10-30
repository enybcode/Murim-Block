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

    @Test
    void reliefFollowsTheGuide() {
        assertEquals(132, relief(-3000, -800).height(), 6, "Qinghai plateau");
        assertEquals(67, relief(1100, -1100).height(), 4, "central plains");
        assertEquals(65, relief(500, 1700).height(), 4, "Hubei");
        assertEquals(100, relief(-1300, -700).height(), 0.5, "Chang-An is flat at Y 100");
        assertEquals(1, relief(-1300, -700).weight(), 1e-6, "Chang-An replaces vanilla's ground");
        // Off the confluence, which the rivers cut through.
        assertEquals(66, relief(1000, 950).height(), 0.5, "Wuhan is flat at Y 66");
        assertTrue(relief(3400, -1100).height() > 190, "the sacred mountain rises above 190");
        // The north wall stands 20 to 40 above its feet, and its passes cut through it.
        double wall = relief(-2000, -2100).height();
        assertTrue(wall - relief(-2000, -2600).height() > 20, "wall above the steppe: " + wall);
        assertTrue(relief(-900, -2100).height() < wall - 15, "a pass is lower than the wall");
        assertEquals(0, relief(0, 7000).weight(), 1e-6, "the sea stays vanilla");
    }

    @Test
    void ravinesCutTheLoess() {
        // Somewhere on a line across the loess, a gully 15 blocks or more below the tableland.
        double lowest = Double.MAX_VALUE;
        double highest = 0;
        for (int wx = -1900; wx <= -100; wx += 6) {
            double h = relief(wx, -1600).height();
            lowest = Math.min(lowest, h);
            highest = Math.max(highest, h);
        }
        assertTrue(highest - lowest > 15, "loess ravines: " + lowest + " to " + highest);
    }

    @Test
    void riversAndLakesHoldWater() {
        // Each river's bed lies under sea level (Y 63) somewhere across it, wherever the meander puts it.
        assertTrue(lowestAcross(30, -1250) < 56, "the yellow river through the loess");
        assertTrue(lowestAcross(-1300, 1150) < 56, "the great river in the Sichuan basin");
        assertTrue(lowestAcross(-200, 1100) < 56, "the great river through the gorges");
        assertTrue(lowestAcross(2150, 1450) < 56, "the great river below Wuhan");
        assertTrue(relief(1250, 2350).height() < 50, "the great lake");
        // The salt lake's bowl lies under its level, high on the plateau.
        double salt = relief(-3500, -1100).height();
        assertTrue(salt < 128 && salt > 110, "the Qinghai salt lake's bed: " + salt);
        double[] block = MurimMap.unwarp(-3500, -1100);
        assertEquals(130, MurimMap.lakeAt(block[0], block[1], 0).level());
    }

    /** The lowest ground along a north-south line 240 blocks long through this point. */
    private static double lowestAcross(int wx, int wz) {
        double lowest = Double.MAX_VALUE;
        for (int d = -120; d <= 120; d += 4) {
            lowest = Math.min(Math.min(lowest, relief(wx, wz + d).height()), relief(wx + d, wz).height());
        }
        return lowest;
    }

    private static MurimMap.Relief relief(int wx, int wz) {
        double[] block = MurimMap.unwarp(wx, wz);
        return MurimMap.relief((int) Math.round(block[0]), (int) Math.round(block[1]));
    }

    /** Every biome takes a real share of the land, and the map is drawn to build/murim-map/murim-map.png (rivers and
     * lakes in blue). */
    @Test
    void everyBiomeIsThereAndTheMapIsDrawn() throws IOException {
        int size = 2 * EXTENT / STEP;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Map<MurimBiome, Integer> counts = new EnumMap<>(MurimBiome.class);
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                MurimBiome biome = MurimMap.biome(-EXTENT + (i + .5) * STEP, -EXTENT + (j + .5) * STEP);
                counts.merge(biome, 1, Integer::sum);
                int x = -EXTENT + i * STEP;
                int z = -EXTENT + j * STEP;
                // Not the low shore, which falls toward the sea.
                boolean water = biome.ours() && (MurimMap.relief(x, z).height() < 60 && MurimMap.landDistance(x, z) > 250 || MurimMap.lakeAt(x, z, 0) != null);
                image.setRGB(i, j, water ? 0x3D7FC4 : COLORS[biome.ordinal()]);
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
