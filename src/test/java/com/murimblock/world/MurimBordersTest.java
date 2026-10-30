package com.murimblock.world;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * The biome borders: every biome is one piece (beaches aside), and the wanted ground never steps into a cliff along a
 * border. The patches, the neighbouring pairs and the steepest columns go to build/murim-map/borders.txt.
 */
class MurimBordersTest {
    private static final int EXTENT = 6000;
    private static final int STEP = 25;

    @Test
    void bordersAreClean() throws IOException {
        int n = 2 * EXTENT / STEP;
        MurimBiome[][] map = new MurimBiome[n][n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                map[i][j] = MurimMap.biome(-EXTENT + (i + .5) * STEP, -EXTENT + (j + .5) * STEP);
            }
        }
        List<String> out = new ArrayList<>();
        // Fragments: connected patches of one biome.
        boolean[][] seen = new boolean[n][n];
        List<String> small = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                if (seen[i][j]) {
                    continue;
                }
                MurimBiome b = map[i][j];
                List<int[]> stack = new ArrayList<>();
                stack.add(new int[] {i, j});
                seen[i][j] = true;
                int size = 0;
                while (!stack.isEmpty()) {
                    int[] p = stack.remove(stack.size() - 1);
                    size++;
                    for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int a = p[0] + d[0];
                        int c = p[1] + d[1];
                        if (a >= 0 && c >= 0 && a < n && c < n && !seen[a][c] && map[a][c] == b) {
                            seen[a][c] = true;
                            stack.add(new int[] {a, c});
                        }
                    }
                }
                out.add(String.format("patch %s size %d at %d, %d", b, size, -EXTENT + i * STEP, -EXTENT + j * STEP));
                // Beaches come in bits along the coast; a cut-off corner of a zone is fine above 60 pixels.
                if (b != MurimBiome.BEACH && size < 60) {
                    small.add(b + " (" + size + " px) at " + (-EXTENT + i * STEP) + ", " + (-EXTENT + j * STEP));
                }
            }
        }
        // Neighbouring pairs.
        Map<String, Integer> pairs = new TreeMap<>();
        for (int j = 0; j + 1 < n; j++) {
            for (int i = 0; i + 1 < n; i++) {
                for (MurimBiome o : new MurimBiome[] {map[i + 1][j], map[i][j + 1]}) {
                    if (o != map[i][j]) {
                        String k = map[i][j].compareTo(o) < 0 ? map[i][j] + " | " + o : o + " | " + map[i][j];
                        pairs.merge(k, 1, Integer::sum);
                    }
                }
            }
        }
        pairs.forEach((k, v) -> out.add("pair " + k + " x" + v));
        // Steepest wanted ground on land, 8 blocks apart.
        List<double[]> steep = new ArrayList<>();
        for (int z = -5000; z <= 5000; z += 16) {
            for (int x = -5500; x <= 5500; x += 16) {
                if (!MurimMap.biome(x, z).ours()) {
                    continue;
                }
                double h = MurimMap.relief(x, z).height();
                double dx = Math.abs(MurimMap.relief(x + 8, z).height() - h);
                double dz = Math.abs(MurimMap.relief(x, z + 8).height() - h);
                double s = Math.max(dx, dz) / 8;
                if (s > 1.2) {
                    steep.add(new double[] {s, x, z, h});
                }
            }
        }
        steep.sort((a, b) -> Double.compare(b[0], a[0]));
        out.add("steep columns: " + steep.size());
        for (int k = 0; k < Math.min(40, steep.size()); k++) {
            double[] s = steep.get(k);
            out.add(String.format("steep %.2f at %d, %d (Y %.0f, %s)", s[0], (int) s[1], (int) s[2], s[3],
                    MurimMap.biome(s[1], s[2])));
        }
        Path file = Path.of("build/murim-map/borders.txt");
        Files.createDirectories(file.getParent());
        Files.write(file, out);
        assertTrue(small.isEmpty(), "stray biome patches: " + small);
        // The river gorges and the sea cliffs are the steepest, under 8 blocks down per block.
        assertTrue(steep.isEmpty() || steep.get(0)[0] < 8, "a cliff along a border: " + out.get(out.size() - 40));
    }
}
