import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 16x16 texture of the Long Fist Scroll: a rolled scroll of pale paper lying across the slot, dark wooden
 * knobs at both ends, a red cord tied round the middle with its tassel hanging, a few strokes of ink showing.
 * Usage: java scripts/worldgen/ScrollTexture.java src/main/resources/assets/murimblock/textures/item
 */
public class ScrollTexture {
    static final int CLEAR = 0x00000000;
    static final int PAPER = 0xFFE9DDBE, PAPER_LIGHT = 0xFFF7EFDC, PAPER_SHADE = 0xFFC8B68D, PAPER_EDGE = 0xFFB09C70;
    static final int WOOD = 0xFF5A3922, WOOD_LIGHT = 0xFF8C5C33, WOOD_DARK = 0xFF3B2414;
    static final int RED = 0xFFB8392C, RED_DARK = 0xFF7C2017, RED_LIGHT = 0xFFD9584A;
    static final int INK = 0xFF34302C;

    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            // u runs along the roll (bottom-left to top-right), v across it (+ toward the bottom-right).
            int u = x - y, v = x + y - 15;
            int c = CLEAR;
            if (Math.abs(u) <= 9 && Math.abs(v) <= 2) {
                c = v == -2 ? PAPER_LIGHT : v == 2 ? PAPER_SHADE : v == 1 ? (Math.abs(u) % 4 == 1 ? PAPER_SHADE : PAPER) : PAPER;
                if (Math.abs(u) == 9) c = PAPER_EDGE;
                // The red cord round the middle.
                if (Math.abs(u) <= 1) c = v == -2 ? RED_LIGHT : v == 2 ? RED_DARK : RED;
            } else if (Math.abs(u) <= 11 && Math.abs(v) <= 1) {
                // The wooden knobs of the rod at both ends.
                c = v == -1 ? WOOD_LIGHT : v == 1 ? WOOD_DARK : WOOD;
            }
            img.setRGB(x, y, c);
        }
        // Strokes of ink on the paper, on either side of the cord.
        for (int[] uv : new int[][]{{-7, 0}, {-6, -1}, {-5, 0}, {-4, -1}, {4, -1}, {5, 0}, {6, -1}, {7, 0}}) set(img, uv[0], uv[1], INK);
        // The tassel hanging from the knot, below the roll.
        set(img, 0, 3, RED);
        set(img, 1, 4, RED_DARK);
        set(img, -1, 4, RED_DARK);
        set(img, 0, 5, RED_LIGHT);
        File out = new File(args[0], "long_fist_scroll.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("wrote " + out);
    }

    /** Sets the pixel at (u, v) in the roll's own coordinates, if there is one there. */
    static void set(BufferedImage img, int u, int v, int color) {
        if ((u + v + 15) % 2 != 0) return;
        int x = (u + v + 15) / 2, y = (v + 15 - u) / 2;
        if (x >= 0 && x < 16 && y >= 0 && y < 16) img.setRGB(x, y, color);
    }
}
