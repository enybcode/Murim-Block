import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 16x16 textures of the martial arts' scrolls, each a rolled scroll lying across the slot:
 *   long_fist_scroll: pale paper, dark wooden knobs, a red cord tied round the middle, its tassel hanging, strokes of ink;
 *   sanguine_art_scroll: paper with a few stains of blood, knobs of black lacquer, a blood crystal set in the middle where the
 *   cord would be, a dark red tassel, strokes of dark red ink;
 *   mountain_shattering_fist_scroll: thick dusty leather, knobs of black iron, a bronze band round the middle, dust stains,
 *   strokes of black ink (the Bandit Warlord's art);
 *   majin_fist_scroll: crimson silk with a gold sheen, knobs of gold, a band of white-hot sunlight round the middle, strokes
 *   of gold ink (the Son of the Demon's art).
 * Usage: java scripts/worldgen/ScrollTexture.java src/main/resources/assets/murimblock/textures/item
 */
public class ScrollTexture {
    static final int CLEAR = 0x00000000;

    /** One scroll's colours: paper (light, base, shade, edge), knobs (light, base, dark), middle (light, base, dark), ink, stain. */
    record Look(int paperLight, int paper, int paperShade, int paperEdge, int woodLight, int wood, int woodDark,
                int midLight, int mid, int midDark, int ink, int stain, boolean crystal) { }

    static final Look LONG_FIST = new Look(0xFFF7EFDC, 0xFFE9DDBE, 0xFFC8B68D, 0xFFB09C70, 0xFF8C5C33, 0xFF5A3922, 0xFF3B2414,
            0xFFD9584A, 0xFFB8392C, 0xFF7C2017, 0xFF34302C, 0, false);
    static final Look SANGUINE = new Look(0xFFF1E2D4, 0xFFDCC6B2, 0xFFB79B85, 0xFF8E6A5A, 0xFF6E2A30, 0xFF2A1416, 0xFF140809,
            0xFFFFB0BE, 0xFFE0465E, 0xFF96122A, 0xFF4A0C14, 0xFFB04050, true);

    static final Look MOUNTAIN = new Look(0xFFC9A57A, 0xFFA9845A, 0xFF84633F, 0xFF5E4428, 0xFF5A5C62, 0xFF2E3036, 0xFF16171A,
            0xFFE6B866, 0xFFB98A3E, 0xFF7A5420, 0xFF1C1612, 0xFF8E7A66, false);

    static final Look MAJIN = new Look(0xFFE24A38, 0xFFB8261E, 0xFF861A14, 0xFF5C100C, 0xFFFFE08A, 0xFFE0A830, 0xFF8A5A10,
            0xFFFFFFF0, 0xFFFFE070, 0xFFE89A20, 0xFFFFC840, 0, false);

    public static void main(String[] args) throws Exception {
        write(args[0], "long_fist_scroll", LONG_FIST);
        write(args[0], "sanguine_art_scroll", SANGUINE);
        write(args[0], "mountain_shattering_fist_scroll", MOUNTAIN);
        write(args[0], "majin_fist_scroll", MAJIN);
    }

    static void write(String dir, String name, Look k) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            // u runs along the roll (bottom-left to top-right), v across it (+ toward the bottom-right).
            int u = x - y, v = x + y - 15;
            int c = CLEAR;
            if (Math.abs(u) <= 9 && Math.abs(v) <= 2) {
                c = v == -2 ? k.paperLight() : v == 2 ? k.paperShade() : v == 1 ? (Math.abs(u) % 4 == 1 ? k.paperShade() : k.paper()) : k.paper();
                if (Math.abs(u) == 9) c = k.paperEdge();
                // The middle: a cord round it, or a crystal set in it (its light side up, its dark side down).
                if (Math.abs(u) <= 1) c = v == -2 ? k.midLight() : v == 2 ? k.midDark() : k.mid();
            } else if (Math.abs(u) <= 11 && Math.abs(v) <= 1) {
                // The knobs of the rod at both ends.
                c = v == -1 ? k.woodLight() : v == 1 ? k.woodDark() : k.wood();
            }
            img.setRGB(x, y, c);
        }
        if (k == MOUNTAIN) for (int[] uv : new int[][]{{-8, 1}, {-3, 2}, {6, 2}, {8, -1}}) set(img, uv[0], uv[1], k.stain());
        if (k.crystal()) {
            // The crystal stands out of the roll: a point beyond it on each side, a glint.
            set(img, 0, -3, k.midLight());
            set(img, 0, 3, k.midDark());
            set(img, -1, -2, k.midLight());
            set(img, 0, -1, 0xFFFFE8EC);
            // Blood soaked into the paper here and there.
            for (int[] uv : new int[][]{{-7, 2}, {7, 2}, {8, 1}}) set(img, uv[0], uv[1], k.stain());
        }
        // Strokes of ink on the paper, on either side of the middle.
        for (int[] uv : new int[][]{{-7, 0}, {-6, -1}, {-5, 0}, {-4, -1}, {4, -1}, {5, 0}, {6, -1}, {7, 0}}) set(img, uv[0], uv[1], k.ink());
        // The tassel hanging from the middle, below the roll.
        set(img, 0, k.crystal() ? 4 : 3, k.mid());
        set(img, 1, k.crystal() ? 5 : 4, k.midDark());
        set(img, -1, k.crystal() ? 5 : 4, k.midDark());
        set(img, 0, k.crystal() ? 6 : 5, k.midLight());
        File out = new File(dir, name + ".png");
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
