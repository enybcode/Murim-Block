package com.murimblock.world;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Draws the Murim map from above as the generator sees it, to build/murim-map/murim-map-relief.png: zone colours
 * shaded by the wanted relief (light from the north-west), rivers and lakes, the sea by depth, and stippling where rock
 * pillars stand. One pixel is 10 blocks; north is up.
 */
class MurimMapRenderTest {
    private static final int EXTENT = 6000;
    private static final int STEP = 10;

    @Test
    void drawTheMapFromAbove() throws IOException {
        int n = 2 * EXTENT / STEP;
        double[][] height = new double[n][n];
        MurimBiome[][] biome = new MurimBiome[n][n];
        boolean[][] water = new boolean[n][n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int x = -EXTENT + i * STEP;
                int z = -EXTENT + j * STEP;
                biome[i][j] = MurimMap.biome(x, z);
                height[i][j] = MurimMap.relief(x, z).height();
                water[i][j] = biome[i][j].ours() && (height[i][j] < 60 && MurimMap.landDistance(x, z) > 250
                        || MurimMap.lakeAt(x, z, 0) != null);
            }
        }
        BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int x = -EXTENT + i * STEP;
                int z = -EXTENT + j * STEP;
                MurimBiome b = biome[i][j];
                int rgb;
                if (b == MurimBiome.OCEAN) {
                    // Lighter over the shelf, darker out at sea.
                    double t = Math.min(1, -MurimMap.landDistance(x, z) / 1500);
                    rgb = mix(0x7FB2D0, 0x2E5A86, Math.max(0, t));
                } else if (water[i][j]) {
                    rgb = 0x3F86C2;
                } else {
                    // Hill shading from the slope, light from the north-west.
                    double dx = height[Math.min(n - 1, i + 1)][j] - height[Math.max(0, i - 1)][j];
                    double dz = height[i][Math.min(n - 1, j + 1)] - height[i][Math.max(0, j - 1)];
                    double shade = Math.max(-1, Math.min(1, -(dx + dz) / 14));
                    int base = COLORS[b.ordinal()];
                    // High ground fades toward snow and pale rock.
                    double high = Math.max(0, Math.min(1, (height[i][j] - 150) / 70));
                    base = mix(base, 0xF2F4F6, high * .8);
                    rgb = shade >= 0 ? mix(base, 0xFFFFFF, shade * .45) : mix(base, 0x1E2A24, -shade * .5);
                    if (pillars(b) && Math.floorMod(i * 7 + j * 13 + (i * j) % 5, 11) == 0) {
                        rgb = mix(rgb, 0x3A3F3A, .55);
                    }
                }
                image.setRGB(i, j, rgb);
            }
        }
        File out = new File("build/murim-map/murim-map-relief.png");
        out.getParentFile().mkdirs();
        ImageIO.write(image, "png", out);
        // Where the named places fall on the picture (0-1 across and down), for a page to label it.
        StringBuilder json = new StringBuilder("[\n");
        for (MurimMap.Spot s : MurimMap.SPOTS) {
            label(json, "zone", s.name(), s.x(), s.z());
        }
        for (MurimMap.Site c : MurimMap.CAPITALS) {
            label(json, "capital", c.name(), c.x(), c.z());
        }
        for (MurimMap.Lake l : MurimMap.LAKES) {
            label(json, "lake", l.name(), l.x(), l.z());
        }
        label(json, "river", "yellow river", 100, -1000);
        label(json, "river", "great river", -1300, 1150);
        label(json, "pass", "fortified pass", 1200, -2100);
        json.setLength(json.length() - 2);
        java.nio.file.Files.writeString(new File("build/murim-map/labels.json").toPath(), json + "\n]\n");
    }

    private static void label(StringBuilder json, String kind, String name, double wx, double wz) {
        double[] block = MurimMap.unwarp(wx, wz);
        json.append(String.format(java.util.Locale.ROOT, "  {\"kind\": \"%s\", \"name\": \"%s\", \"u\": %.4f, \"v\": %.4f},\n",
                kind, name, (block[0] + EXTENT) / (2.0 * EXTENT), (block[1] + EXTENT) / (2.0 * EXTENT)));
    }

    private static boolean pillars(MurimBiome b) {
        return b == MurimBiome.STONE_PEAKS || b == MurimBiome.STONE_FOREST || b == MurimBiome.SACRED_MOUNTAIN;
    }

    private static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    // The mockup's zone colours, in MurimBiome order (the same as MurimMapTest's).
    private static final int[] COLORS = {
        0xDCE3E8, 0xBDB39C, 0xC9C291, 0xA9C77A, 0xE3ECF3, 0xDDBB66, 0xCBD47C, 0x8FC68C, 0xB6C2C6, 0xC48C70,
        0x5C9E5A, 0x7CC4B2, 0xA3CCAE, 0xAAAAA0, 0x7E9A8A, 0x3E9A48, 0xA2C6B0, 0xBCDB8E, 0x5FA867, 0x8CC490,
        0xE4D7A8, 0x3D6E9C,
    };
}
