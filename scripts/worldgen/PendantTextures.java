import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the locator pendants (LocatorPendantItem), 32 frames each like the vanilla compass (frame 16 points up, each
 * frame turns the needle 11.25 degrees clockwise). Each pendant looks like where it leads (Enzo, 15/10):
 * <ul>
 * <li>misty_bamboo_temple: a jade disc ringed with green bamboo and its nodes, an azure cloud carved on it, an azure
 *     tassel, and a needle shaped as a little sword (Sensei Yun's Azure Cloud);</li>
 * <li>yellow_river_bandit_den: a tarnished bronze disc studded with iron rivets, on a leather thong, and a needle
 *     shaped as an axe (the Bandit Chief's great axe);</li>
 * <li>alliance_arena: an azure enamel disc in a pale gold rim struck with the Alliance's three peaks, a gold tassel,
 *     and a needle shaped as a spearhead (the Captain of the Peak Guard).</li>
 * </ul>
 *   java scripts/worldgen/PendantTextures.java src/main/resources/assets/murimblock/textures/item
 */
public class PendantTextures {
    static final double CX = 7.5, CY = 8.5;

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "src/main/resources/assets/murimblock/textures/item");
        out.mkdirs();
        for (int frame = 0; frame < 32; frame++) {
            write(temple(frame), out, "misty_bamboo_temple", frame);
            write(den(frame), out, "yellow_river_bandit_den", frame);
            write(arena(frame), out, "alliance_arena", frame);
        }
        System.out.println("3 pendants x 32 frames -> " + out);
    }

    // ---- the bamboo temple: jade, bamboo, azure cloud, a little sword

    static BufferedImage temple(int frame) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // An azure silk cord over the top, its tassel hanging at the bottom right.
        for (int[] p : new int[][]{{6, 0}, {7, 0}, {8, 0}, {9, 0}, {5, 1}, {10, 1}}) put(im, p[0], p[1], 0x3F7FD0);
        for (int[] p : new int[][]{{13, 13}, {13, 14}, {14, 14}, {13, 15}, {14, 15}, {12, 15}}) put(im, p[0], p[1], p[1] == 13 ? 0x2B5FA8 : 0x5A9AE0);
        for (int y = 1; y < 16; y++) for (int x = 0; x < 16; x++) {
            double dx = x - CX, dy = y - CY, r = Math.hypot(dx, dy);
            if (r > 6.6) continue;
            int c;
            if (r > 5.4) {
                // A ring of bamboo: green, lit top-left, with a pale node every eighth of the way round.
                double ang = Math.atan2(dy, dx);
                boolean node = Math.abs(Math.sin(ang * 4)) < 0.22;
                c = node ? 0xC9E39A : (dx + dy < 0 ? 0x7DBE4E : 0x4E8A2E);
            } else {
                double vein = Math.sin(x * 1.3 + y * 0.7) + Math.cos(y * 1.1 - x * 0.4);
                c = vein > 1.5 ? 0x6FBE93 : 0x4E9C76;
                if (dx < -2 && dy < -2 && r < 4.6) c = 0xA9E2C0;
            }
            put(im, x, y, c);
        }
        // A carved azure cloud low on the face: two curls.
        for (int[] p : new int[][]{{5, 11}, {6, 10}, {7, 11}, {8, 11}, {9, 10}, {10, 11}, {6, 12}, {9, 12}}) put(im, p[0], p[1], 0x9CC9EC);
        // The needle: a little straight sword, silver blade towards the temple, gold guard, azure hilt.
        needle(im, frame, (s) -> s > 1.2 ? 0xEDF2F6 : s > 0.6 ? 0xC9A74B : 0x2B5FA8, -2.6, 4.6, true);
        return im;
    }

    // ---- the bandit den: bronze, rivets, leather, an axe

    static BufferedImage den(int frame) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // A leather thong, knotted.
        for (int[] p : new int[][]{{5, 0}, {6, 0}, {9, 0}, {10, 0}, {4, 1}, {11, 1}, {7, 1}, {8, 1}}) put(im, p[0], p[1], p[1] == 1 && (p[0] == 7 || p[0] == 8) ? 0x4A2E1C : 0x6B4429);
        for (int y = 1; y < 16; y++) for (int x = 0; x < 16; x++) {
            double dx = x - CX, dy = y - CY, r = Math.hypot(dx, dy);
            if (r > 6.6) continue;
            int c;
            if (r > 5.4) {
                // Dark iron rim, with four rivets.
                c = dx + dy < 0 ? 0x6E6A64 : 0x3E3B37;
                double ang = Math.atan2(dy, dx);
                if (Math.abs(Math.sin(ang * 2)) > 0.97) c = 0xB9B3A6;
            } else {
                // Tarnished bronze, verdigris in the hollows, a scratch or two.
                double n = Math.sin(x * 2.1 + y * 1.3) * Math.cos(y * 1.7 - x * 0.9);
                c = n > 0.8 ? 0x5E9C86 : n > -0.1 ? 0xA9743A : 0x96652F;
                if (dx < -2 && dy < -2 && r < 4.4) c = 0xC99456;
            }
            put(im, x, y, c);
        }
        // The needle: an axe, a wide red-brown iron head towards the den, a wooden haft.
        needle(im, frame, (s) -> s > 2.0 ? 0xE6E1D6 : 0x2E1C10, -3.0, 4.2, false);
        double a = (frame - 16) / 32.0 * Math.PI * 2, ux = Math.sin(a), uy = -Math.cos(a);
        // The axe's head flares to one side of the haft.
        for (double s = 2.0; s <= 4.0; s += 0.5) {
            int x = (int) Math.round(CX + ux * s + -uy * 1.2), y = (int) Math.round(CY + uy * s + ux * 1.2);
            put(im, x, y, s > 3.4 ? 0xB8332A : 0xE6E1D6);
        }
        return im;
    }

    // ---- the Alliance arena: azure enamel, pale gold, three peaks, a spearhead

    static BufferedImage arena(int frame) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // A gold cord and a gold tassel.
        for (int[] p : new int[][]{{6, 0}, {7, 0}, {8, 0}, {9, 0}, {5, 1}, {10, 1}}) put(im, p[0], p[1], 0xE9D9A6);
        for (int[] p : new int[][]{{2, 13}, {2, 14}, {1, 14}, {2, 15}, {1, 15}, {3, 15}}) put(im, p[0], p[1], p[1] == 13 ? 0xC9A74B : 0xF2D27A);
        for (int y = 1; y < 16; y++) for (int x = 0; x < 16; x++) {
            double dx = x - CX, dy = y - CY, r = Math.hypot(dx, dy);
            if (r > 6.6) continue;
            int c;
            if (r > 5.4) c = dx + dy < 0 ? 0xF6EAC2 : (dx + dy > 3 ? 0xB89A52 : 0xE9D9A6); // pale gold
            else {
                c = (x + y) % 5 == 0 ? 0x3A72C0 : 0x2B5FA8; // azure enamel, faintly worked
                if (dx < -2 && dy < -2 && r < 4.4) c = 0x5C8FD6;
            }
            put(im, x, y, c);
        }
        // The Alliance's three peaks in gold on the lower face.
        for (int[] p : new int[][]{{4, 12}, {5, 11}, {6, 12}, {7, 10}, {8, 9}, {9, 10}, {10, 12}, {11, 11}, {12, 12},
                {5, 12}, {8, 10}, {8, 11}, {11, 12}, {7, 11}, {9, 11}}) put(im, p[0], p[1], 0xE9D9A6);
        // The needle: a spear, a bright leaf-shaped head towards the arena, a red tuft, a dark shaft.
        needle(im, frame, (s) -> s > 2.2 ? 0xF4F6F8 : s > 1.4 ? 0xC2453A : 0x3A2A22, -3.0, 4.8, true);
        return im;
    }

    // ---- shared

    interface Shade { int at(double s); }

    /** A needle through the centre, from s0 (tail) to s1 (tip, towards the target); thick near the tip if asked. */
    static void needle(BufferedImage im, int frame, Shade shade, double s0, double s1, boolean leaf) {
        double a = (frame - 16) / 32.0 * Math.PI * 2;
        double ux = Math.sin(a), uy = -Math.cos(a);
        for (double s = s0; s <= s1; s += 0.25) {
            int x = (int) Math.round(CX + ux * s), y = (int) Math.round(CY + uy * s);
            put(im, x, y, shade.at(s));
            if (leaf && s > 2.4 && s < s1 - 0.8) {
                put(im, (int) Math.round(CX + ux * s - uy * 0.6), (int) Math.round(CY + uy * s + ux * 0.6), shade.at(s));
            }
        }
    }

    static void put(BufferedImage im, int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < 16 && y < 16) im.setRGB(x, y, 0xFF000000 | rgb);
    }

    static void write(BufferedImage im, File out, String name, int frame) throws Exception {
        ImageIO.write(im, "png", new File(out, String.format("%s_pendant_%02d.png", name, frame)));
    }
}
