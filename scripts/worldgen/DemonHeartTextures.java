import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * The Bandit Warlord's transformation (docs/mobs/bandit-warlord.md), its textures:
 * <ul>
 *   <li>his heart, a block (Enzo, 16/10/2026: "the heart must be a block"): living flesh, dark veins, the stubs of its
 *   arteries on top; then the same heart turned to gold, its veins of light (items demon_heart, golden_demon_heart);</li>
 *   <li>his skin tearing as he screams, in three stages (textures/entity/bandit_warlord_tear_1..3.png): cracks of golden
 *   light opening in his skin only (his armour, cloth and hair untouched), a dark rim of torn flesh round each, more and
 *   wider at each stage, until the gold shows through everywhere.</li>
 * </ul>
 * Run from the repository's root: java scripts/worldgen/DemonHeartTextures.java
 */
public final class DemonHeartTextures {
    private static final String ASSETS = "src/main/resources/assets/murimblock/textures/";

    public static void main(String[] args) throws Exception {
        heart(false, ASSETS + "item/demon_heart.png", ASSETS + "item/demon_heart_top.png");
        heart(true, ASSETS + "item/golden_demon_heart.png", ASSETS + "item/golden_demon_heart_top.png");
        tears();
        System.out.println("wrote the demon heart and the three stages of torn skin");
    }

    // ---------------------------------------------------------------- the heart

    private static final String[] SIDE = {
            "LLLLLLVLLLLLLLLL",
            "LSSLLLVLLLLLVLLL",
            "LSLLLLVvLLLLVLLL",
            "LLLLLFFVLLLLVLLL",
            "LLLLFFFVvLLVLLLL",
            "FLLFFFFFVLLVLLFF",
            "FFFFFFFFVFVFFFFF",
            "FFFFFFFFFVFFFFFF",
            "FFFVFFFFFVFFFFFF",
            "FFFFVFFFFVvFFFFF",
            "FFFFFVFFFFVFFFFD",
            "DFFFFFVFFFVFFFDD",
            "DDFFFFFVFFFVFDDD",
            "DDDFFFFVFFFVDDDD",
            "DDDDDFFFVDDVDDDD",
            "DDDDDDDDVDDDVDDD"};

    private static void heart(boolean gold, String side, String top) throws Exception {
        int[] dark = gold ? rgb(0x7a4a0c) : rgb(0x4a0a0e);
        int[] flesh = gold ? rgb(0xc8912a) : rgb(0x8e1a1e);
        int[] light = gold ? rgb(0xf4cf5c) : rgb(0xc33234);
        int[] shine = gold ? rgb(0xfff4c0) : rgb(0xe8706a);
        int[] vein = gold ? rgb(0xfffbe0) : rgb(0x2c0408);
        int[] veinEdge = gold ? rgb(0xffe27a) : rgb(0x5c0c12);
        // Drawn by hand: the flesh lit from above, the coronary arteries running down it, a gleam of wet light.
        Random r = new Random(7);
        BufferedImage s = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            char ch = SIDE[y].charAt(x);
            int[] c = switch (ch) {
                case 'S' -> shine; case 'L' -> light; case 'F' -> flesh; case 'D' -> dark; case 'V' -> vein; default -> veinEdge;
            };
            // A little life in the flesh: now and then a texel a shade off.
            if ((ch == 'F' || ch == 'L') && r.nextInt(9) == 0) c = ch == 'F' ? (r.nextBoolean() ? light : dark) : flesh;
            s.setRGB(x, y, argb(c));
        }
        ImageIO.write(s, "png", new File(side));
        // On top, the stubs of its great arteries: two rings, one smaller.
        BufferedImage t = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) t.setRGB(x, y, s.getRGB(x, y));
        ring(t, 5, 6, 3, vein, light, dark);
        ring(t, 11, 9, 2, vein, light, dark);
        ImageIO.write(t, "png", new File(top));
    }

    private static void ring(BufferedImage img, int cx, int cy, int radius, int[] hole, int[] rim, int[] shade) {
        for (int y = cy - radius - 1; y <= cy + radius + 1; y++) for (int x = cx - radius - 1; x <= cx + radius + 1; x++) {
            if (x < 0 || x > 15 || y < 0 || y > 15) continue;
            double d = Math.hypot(x - cx, y - cy);
            if (d < radius - 0.6) img.setRGB(x, y, argb(hole));
            else if (d < radius + 0.4) img.setRGB(x, y, argb(y < cy ? rim : shade));
        }
    }

    // ---------------------------------------------------------------- the torn skin

    private static void tears() throws Exception {
        BufferedImage base = ImageIO.read(new File(ASSETS + "entity/bandit_warlord.png"));
        int w = base.getWidth(), h = base.getHeight();
        boolean[][] skin = new boolean[w][h];
        List<int[]> skinCells = new ArrayList<>();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = base.getRGB(x, y);
            if ((c >>> 24) < 128) continue;
            if (isSkin(c)) { skin[x][y] = true; skinCells.add(new int[] {x, y}); }
        }
        // Each stage keeps the last one's cracks, opens more and widens them all.
        int[][] crack = new int[w][h];
        Random r = new Random(1610);
        int[] counts = {skinCells.size() / 80, skinCells.size() / 60, skinCells.size() / 55};
        int[] lengths = {5, 8, 12};
        for (int stage = 1; stage <= 3; stage++) {
            for (int n = 0; n < counts[stage - 1]; n++) {
                int[] at = skinCells.get(r.nextInt(skinCells.size()));
                int x = at[0], y = at[1];
                int dx = r.nextBoolean() ? 1 : -1;
                for (int i = 0; i < lengths[stage - 1]; i++) {
                    if (x < 0 || x >= w || y < 0 || y >= h || !skin[x][y]) break;
                    if (crack[x][y] == 0) crack[x][y] = stage;
                    // Jagged: mostly down, now and then aside.
                    int d = r.nextInt(5);
                    if (d < 3) y++; else if (d == 3) x += dx; else { x += dx; y++; }
                }
            }
            // Widen: from the second stage, a crack's skin neighbours open too.
            for (int grow = 1; grow < Math.min(stage, 2); grow++) {
                int[][] next = new int[w][h];
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    next[x][y] = crack[x][y];
                    if (crack[x][y] != 0 || !skin[x][y]) continue;
                    for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
                        if (in(x + o[0], y + o[1], w, h) && crack[x + o[0]][y + o[1]] != 0 && r.nextInt(3) > 0) { next[x][y] = stage; break; }
                }
                crack = next;
            }
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int c = base.getRGB(x, y);
                if (skin[x][y] && crack[x][y] != 0) {
                    // The core of light, whiter where the crack is oldest (widest).
                    boolean core = surrounded(crack, x, y, w, h);
                    c = argb(core ? rgb(0xfff2b8) : rgb(0xffcf4a));
                } else if (skin[x][y] && near(crack, x, y, w, h)) {
                    // The torn rim: dark, raw flesh.
                    c = argb(blend(rgbOf(c), rgb(0x5a0c08), 0.65));
                } else if (skin[x][y] && stage == 3) {
                    // The last stage: the light glowing under all his skin.
                    c = argb(blend(rgbOf(c), rgb(0xffc040), 0.22));
                }
                out.setRGB(x, y, c);
            }
            ImageIO.write(out, "png", new File(ASSETS + "entity/bandit_warlord_tear_" + stage + ".png"));
        }
    }

    /** His skin: the warm browns of S, H and their shades (not the gold of his armour, nor his crimson). */
    private static boolean isSkin(int c) {
        int[] p = rgbOf(c);
        int[][] tones = {rgb(0xb97f50), rgb(0xdcac7c), rgb(0x8f5f38), rgb(0xa06a40), rgb(0xcd9566)};
        for (int[] t : tones) if (Math.abs(p[0] - t[0]) + Math.abs(p[1] - t[1]) + Math.abs(p[2] - t[2]) < 46) return true;
        return false;
    }

    private static boolean in(int x, int y, int w, int h) {
        return x >= 0 && y >= 0 && x < w && y < h;
    }

    private static boolean surrounded(int[][] crack, int x, int y, int w, int h) {
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
            if (!in(x + o[0], y + o[1], w, h) || crack[x + o[0]][y + o[1]] == 0) return false;
        return true;
    }

    private static boolean near(int[][] crack, int x, int y, int w, int h) {
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
            if (in(x + o[0], y + o[1], w, h) && crack[x + o[0]][y + o[1]] != 0) return true;
        return false;
    }

    private static int[] rgb(int c) {
        return new int[] {(c >> 16) & 255, (c >> 8) & 255, c & 255};
    }

    private static int[] rgbOf(int argb) {
        return rgb(argb & 0xffffff);
    }

    private static int[] blend(int[] a, int[] b, double k) {
        return new int[] {(int) (a[0] + (b[0] - a[0]) * k), (int) (a[1] + (b[1] - a[1]) * k), (int) (a[2] + (b[2] - a[2]) * k)};
    }

    private static int argb(int[] c) {
        return 0xff000000 | (c[0] << 16) | (c[1] << 8) | c[2];
    }
}
