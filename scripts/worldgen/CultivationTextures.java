import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the cultivation textures (docs/cultivation/qi-cultivation.md):
 *   gui/realm/seal_1..9.png    the seal of each realm read by Qi Sense: a round seal rimmed in the realm's colour, its
 *                              Chinese numeral inside (一 to 九), 32x32;
 *   gui/realm/seal_veiled.png  a realm one above the reader's: the seal smudged, its numeral unreadable;
 *   gui/realm/seal_abyss.png   two realms above or more: no seal, a dark swirl;
 *   gui/realm/pip.png          the stage dots: an empty dot (left half) and a full one (right half), 8x4;
 *   item/qi_gathering_pill.png, item/realm_breaking_pill.png  the two elixirs, 16x16;
 *   particle/aura_0..3.png     the aura's soft sparks (white, tinted when drawn), 8x8;
 *   particle/qi_mote.png       a mote of Qi (the spiral of a breakthrough), 8x8.
 * Usage: java scripts/worldgen/CultivationTextures.java src/main/resources/assets/murimblock/textures
 */
public class CultivationTextures {
    /** The realms' colours: rim, rim shade, numeral. From pale (Qi Sensing) to gold and black (Void Enlightenment). */
    static final int[][] REALM = {
            {0xFFE8E4DA, 0xFFA8A296, 0xFF5A554C},
            {0xFFB7E3B0, 0xFF6FA86A, 0xFF2E5A2B},
            {0xFF5FC79A, 0xFF2F8A63, 0xFF143F2C},
            {0xFF8FD3F0, 0xFF4A98BF, 0xFF1B4560},
            {0xFF6FA8FF, 0xFF3A66C4, 0xFF142B63},
            {0xFFB98CF0, 0xFF7A4FB8, 0xFF35185E},
            {0xFFF2CF5B, 0xFFB88E22, 0xFF5A3F05},
            {0xFFE8604E, 0xFFA82F22, 0xFF4F0C06},
            {0xFFF5E6A8, 0xFF2A2420, 0xFF0E0B09},
    };
    static final int PAPER = 0xFFF4EEDF, PAPER_SHADE = 0xFFDCD3BE;

    /** The numerals 一 to 九, 14x14, '#' inked. */
    static final String[][] NUMERALS = {
            {"", "", "", "", "", "", "##############", "##############", "", "", "", "", "", ""},
            {"", "", "", "  ##########", "  ##########", "", "", "", "", "##############", "##############", "", "", ""},
            {"", "  ##########", "  ##########", "", "", "   ########", "   ########", "", "", "", "##############", "##############", "", ""},
            {"", "", "##############", "##  ##  ##  ##", "##  ##  ##  ##", "##  ##  ##  ##", "## ##   ##  ##", "####     ## ##",
                    "##        ####", "##          ##", "##############", "##############", "", ""},
            {"", "##############", "     ##", "     ##", "  ##########", "  ##########", "     ##    ##", "     ##    ##",
                    "    ##     ##", "    ##     ##", "##############", "##############", "", ""},
            {"      ##", "       ##", "##############", "##############", "", "", "    ##    ##", "   ##      ##",
                    "  ##        ##", " ##          ##", "##", "", "", ""},
            {"", "     ##", "     ##", "     ##      ##", "     ##   ####", "     #####", "  ####", "#### ##", "     ##",
                    "     ##", "     ##      ##", "      ########", "", ""},
            {"", "", "    ##    ##", "    ##    ##", "    ##    ##", "    ##     ##", "   ##       ##", "   ##       ##",
                    "  ##         ##", " ##           #", "##", "", "", ""},
            {"", "    ##", "    ##", "##########", "    ##   ##", "    ##   ##", "    ##   ##", "   ##    ##", "   ##    ##",
                    "  ##     ##   #", " ##      ##   #", "##        #####", "", ""},
    };

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        for (int r = 0; r < 9; r++) write(seal(r), new File(root, "gui/realm/seal_" + (r + 1) + ".png"));
        write(blur(seal(4), 2), new File(root, "gui/realm/seal_veiled.png"));
        write(abyss(), new File(root, "gui/realm/seal_abyss.png"));
        write(pip(), new File(root, "gui/realm/pip.png"));
        write(pill(0xFFF3F0D8, 0xFFC9D9A8, 0xFF7FA35E, 0xFFFFFFFF, false), new File(root, "item/qi_gathering_pill.png"));
        write(pill(0xFFE8604E, 0xFFB8392C, 0xFF6B1A12, 0xFFF2CF5B, true), new File(root, "item/realm_breaking_pill.png"));
        for (int i = 0; i < 4; i++) write(spark(i), new File(root, "particle/aura_" + i + ".png"));
        write(mote(), new File(root, "particle/qi_mote.png"));
        write(deviationIcon(), new File(root, "mob_effect/qi_deviation.png"));
        write(elderSkin(), new File(root, "entity/remnant_soul.png"));
        write(demonEyes(), new File(root, "entity/heart_demon_eyes.png"));
        System.out.println("wrote the cultivation textures under " + root);
    }

    static void write(BufferedImage img, File out) throws Exception {
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    /** A round seal: a paper disc, a double rim in the realm's colour, the numeral in the middle. */
    static BufferedImage seal(int realm) {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        int[] c = REALM[realm];
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5);
            int col = 0;
            if (d < 15.5) col = d > 14.0 ? c[1] : d > 12.0 ? c[0] : d > 11.0 ? c[1] : (y > 21 ? PAPER_SHADE : PAPER);
            img.setRGB(x, y, col);
        }
        String[] glyph = NUMERALS[realm];
        for (int gy = 0; gy < glyph.length; gy++) for (int gx = 0; gx < glyph[gy].length(); gx++)
            if (glyph[gy].charAt(gx) == '#') img.setRGB(9 + gx, 9 + gy, c[2]);
        return img;
    }

    /** A box blur of `radius` pixels, twice, keeping the alpha of the disc: a seal one cannot read. */
    static BufferedImage blur(BufferedImage src, int radius) {
        BufferedImage img = src;
        for (int pass = 0; pass < 2; pass++) {
            BufferedImage out = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
                long a = 0, r = 0, g = 0, b = 0, n = 0;
                for (int dy = -radius; dy <= radius; dy++) for (int dx = -radius; dx <= radius; dx++) {
                    int sx = x + dx, sy = y + dy;
                    if (sx < 0 || sy < 0 || sx >= 32 || sy >= 32) continue;
                    int p = img.getRGB(sx, sy);
                    int pa = p >>> 24;
                    a += pa; r += ((p >> 16) & 255) * pa; g += ((p >> 8) & 255) * pa; b += (p & 255) * pa; n++;
                }
                int alpha = (int) (a / n);
                out.setRGB(x, y, a == 0 ? 0 : (alpha << 24) | ((int) (r / a) << 16) | ((int) (g / a) << 8) | (int) (b / a));
            }
            img = out;
        }
        return img;
    }

    /** No seal at all: a dark swirl, denser at the heart, fading out. */
    static BufferedImage abyss() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            double dx = x - 15.5, dy = y - 15.5, d = Math.hypot(dx, dy);
            if (d > 15.5) continue;
            double angle = Math.atan2(dy, dx), arm = Math.sin(angle * 3 + d * 0.55);
            double alpha = Math.max(0, 1 - d / 15.5) * (0.65 + 0.35 * arm);
            int shade = (int) (18 + 30 * Math.max(0, arm) * (1 - d / 16));
            int a = (int) Math.round(Math.min(1, alpha * 1.4) * 230);
            img.setRGB(x, y, (a << 24) | ((shade + 14) << 16) | (shade << 8) | (shade + 26));
        }
        return img;
    }

    /** The stage dots: an empty ring (x 0..3) and a full dot (x 4..7). */
    static BufferedImage pip() {
        BufferedImage img = new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB);
        int ink = 0xFF2B2620, gold = 0xFFF2CF5B;
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            boolean corner = (x == 0 || x == 3) && (y == 0 || y == 3);
            if (corner) continue;
            boolean rim = x == 0 || x == 3 || y == 0 || y == 3;
            img.setRGB(x, y, rim ? ink : 0x66000000);
            img.setRGB(x + 4, y, rim ? ink : gold);
        }
        return img;
    }

    /** A round pill with a light from the top left; `crack` adds the gold seam of the Realm-Breaking Pill. */
    static BufferedImage pill(int light, int body, int shade, int shine, boolean crack) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            double dx = x - 7.5, dy = y - 8.0, d = Math.hypot(dx, dy);
            if (d > 5.6) continue;
            double lit = (-dx - dy) / 8.0;
            int col = d > 4.8 ? shade : lit > 0.45 ? light : lit < -0.35 ? shade : body;
            img.setRGB(x, y, col);
        }
        img.setRGB(5, 5, shine);
        img.setRGB(6, 5, shine);
        img.setRGB(5, 6, shine);
        if (crack) for (int[] p : new int[][]{{9, 4}, {8, 5}, {8, 6}, {9, 7}, {8, 8}, {7, 9}, {8, 10}, {7, 11}}) img.setRGB(p[0], p[1], shine);
        // A small shadow under it.
        for (int x = 4; x <= 11; x++) if (img.getRGB(x, 14) == 0) img.setRGB(x, 14, 0x40000000);
        return img;
    }

    /** A soft spark, white (tinted by the aura's colour when drawn), a little smaller from frame to frame. */
    static BufferedImage spark(int frame) {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        double radius = 3.8 - frame * 0.7;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            double d = Math.hypot(x - 3.5, y - 3.5);
            if (d > radius) continue;
            int a = (int) Math.round(255 * Math.pow(1 - d / radius, 1.3));
            img.setRGB(x, y, (a << 24) | 0xFFFFFF);
        }
        return img;
    }

    /** The Qi Deviation's icon (18x18): a cracked dark-red flame. */
    static BufferedImage deviationIcon() {
        BufferedImage img = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 18; y++) for (int x = 0; x < 18; x++) {
            double dx = (x - 8.5) / 6.0, t = (y - 2) / 14.0;
            if (t < 0 || t > 1) continue;
            double width = Math.sin(t * Math.PI) * (0.6 + 0.4 * t);
            if (Math.abs(dx + Math.sin(t * 6) * 0.15) > width) continue;
            int col = Math.abs(dx) > width - 0.25 ? 0xFF4F0C06 : t > 0.55 ? 0xFFA82F22 : 0xFFE8604E;
            img.setRGB(x, y, col);
        }
        for (int[] p : new int[][]{{9, 6}, {8, 7}, {9, 8}, {10, 9}, {9, 10}, {8, 11}}) img.setRGB(p[0], p[1], 0xFF120404);
        return img;
    }

    /** Fills a rectangle of a 64x64 skin. */
    static void fill(BufferedImage img, int x0, int y0, int x1, int y1, int col) {
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) img.setRGB(x, y, col);
    }

    /**
     * The remnant soul's skin: an old master in a grey-blue robe, white hair and a long white beard, his eyes closed.
     * Standard 64x64 player layout (head 0..32 x 0..16, body 16..40 x 16..32, arms and legs).
     */
    static BufferedImage elderSkin() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int skin = 0xFFD8B593, hair = 0xFFEDEBE4, hairShade = 0xFFC9C6BE, robe = 0xFF6F8194, robeDark = 0xFF52606F, sash = 0xFF8E2E26, shoe = 0xFF2E2A26;
        // Head: hair all round, the face in front.
        fill(img, 0, 0, 32, 16, hair);
        fill(img, 8, 0, 16, 8, hairShade);
        fill(img, 8, 8, 16, 16, skin);
        fill(img, 8, 8, 16, 10, hair);
        // Closed eyes, white brows, the beard down the lower face.
        fill(img, 9, 11, 11, 12, 0xFF6B4A33);
        fill(img, 13, 11, 15, 12, 0xFF6B4A33);
        fill(img, 9, 10, 11, 11, hair);
        fill(img, 13, 10, 15, 11, hair);
        fill(img, 9, 13, 15, 16, hair);
        fill(img, 11, 12, 13, 13, 0xFFC09A78);
        // Body: the robe, a red sash, the beard falling on the chest.
        fill(img, 16, 16, 40, 32, robe);
        fill(img, 16, 26, 40, 27, sash);
        fill(img, 22, 20, 26, 25, hair);
        fill(img, 20, 20, 21, 32, robeDark);
        // Arms: wide sleeves, the hands at the cuffs.
        fill(img, 40, 16, 56, 32, robe);
        fill(img, 40, 30, 56, 32, skin);
        fill(img, 32, 48, 48, 64, robe);
        fill(img, 32, 62, 48, 64, skin);
        // Legs: the robe's skirt, cloth shoes.
        fill(img, 0, 16, 16, 32, robeDark);
        fill(img, 0, 30, 16, 32, shoe);
        fill(img, 16, 48, 32, 64, robeDark);
        fill(img, 16, 62, 32, 64, shoe);
        return img;
    }

    /** The Heart Demon's eyes: two red points where a skin's eyes are, drawn glowing over its dark copy. */
    static BufferedImage demonEyes() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        fill(img, 9, 12, 11, 13, 0xFFFF2A1A);
        fill(img, 13, 12, 15, 13, 0xFFFF2A1A);
        return img;
    }

    /** A mote of Qi: a bright core and a soft halo. */
    static BufferedImage mote() {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            double d = Math.hypot(x - 3.5, y - 3.5);
            if (d > 3.9) continue;
            int a = d < 1.2 ? 255 : (int) Math.round(200 * (1 - (d - 1.2) / 2.7));
            img.setRGB(x, y, (a << 24) | 0xFFFFFF);
        }
        return img;
    }
}
