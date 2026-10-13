import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 32x32 icons of the techniques (pixel art, drawn point by point so it stays crisp at 16 px too).
 * Usage: java scripts/worldgen/TechniqueIcons.java src/main/resources/assets/murimblock/textures/gui/technique
 *   cloud_step_slash: a silver crescent cut sweeping across a deep jade sky to a spark at its tip, speed streaks behind it (the dash) and
 *   pale clouds at its foot (the cloud step).
 *   thousand_drizzle: a fan of fine silver thrusts converging on one bright point (the drizzle of thrusts), a longer one through the middle.
 *   heaven_splitting_crescent: a crescent rising from the bottom to the top, splitting a bank of cloud in two at the top.
 *   swallow_returns: a swallow in ink turning back on its own trail (the secret; red seal corners).
 *   flowing_cloud_stance: the passive, a blade lying across slow drifting cloud bands.
 */
public class TechniqueIcons {
    static final int W = 32;

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        write(dir, "cloud_step_slash", cloudStepSlash());
        write(dir, "thousand_drizzle", thousandDrizzle());
        write(dir, "heaven_splitting_crescent", heavenSplittingCrescent());
        write(dir, "swallow_returns", swallowReturns());
        write(dir, "flowing_cloud_stance", flowingCloudStance());
    }

    static void write(File dir, String name, BufferedImage img) throws Exception {
        File f = new File(dir, name + ".png");
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f);
    }

    /** The art's jade sky: deep at the top left, lighter toward the bottom right. */
    static BufferedImage sky() {
        BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        int skyTop = rgb(14, 40, 52), skyBottom = rgb(30, 92, 96);
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) img.setRGB(x, y, mix(skyTop, skyBottom, (y + x * 0.35) / (W * 1.35)));
        return img;
    }

    static void glow(BufferedImage img, double cx, double cy, double radius, double strength) {
        int glow = rgb(70, 150, 150);
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - cx, y - cy);
            if (d < radius) img.setRGB(x, y, mix(img.getRGB(x, y), glow, (radius - d) / radius * strength));
        }
    }

    static void corners(BufferedImage img, int color) {
        for (int[] p : new int[][]{{0, 0}, {1, 0}, {0, 1}, {31, 0}, {30, 0}, {31, 1}, {0, 31}, {1, 31}, {0, 30}, {31, 31}, {30, 31}, {31, 30}}) img.setRGB(p[0], p[1], color);
    }

    /** Pale cloud puffs {x, y, radius}, their underside shaded. */
    static void puffs(BufferedImage img, int[][] puffs) {
        int cloud = rgb(224, 240, 236), shade = rgb(160, 196, 192);
        for (int[] p : puffs) for (int y = p[1] - p[2]; y <= Math.min(W - 1, p[1] + p[2]); y++)
            for (int x = p[0] - p[2]; x <= p[0] + p[2]; x++) {
                if (x < 0 || x >= W || y < 0) continue;
                if (Math.hypot(x - p[0], (y - p[1]) * 1.3) > p[2] + 0.3) continue;
                img.setRGB(x, y, y > p[1] ? shade : cloud);
            }
    }

    /** A line from (x0, y0) to (x1, y1), its colour going from `from` to `to`. */
    static void line(BufferedImage img, double x0, double y0, double x1, double y1, int from, int to) {
        int n = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            int x = (int) Math.round(x0 + (x1 - x0) * t), y = (int) Math.round(y0 + (y1 - y0) * t);
            if (x >= 0 && y >= 0 && x < W && y < W) img.setRGB(x, y, mix(from, to, t));
        }
    }

    static BufferedImage thousandDrizzle() {
        BufferedImage img = sky();
        glow(img, 26, 16, 10, 0.55);
        int faint = rgb(90, 150, 160), silver = rgb(200, 222, 228), edge = rgb(250, 252, 245);
        // Fine thrusts coming from the left, all aimed at the same point on the right.
        double tx = 27, ty = 16;
        int[][] starts = {{2, 4}, {1, 9}, {3, 13}, {1, 20}, {3, 24}, {2, 28}};
        for (int[] s : starts) {
            double dx = tx - s[0], dy = ty - s[1], l = Math.hypot(dx, dy);
            double ex = tx - dx / l * 4, ey = ty - dy / l * 4;
            line(img, s[0] + dx * 0.35, s[1] + dy * 0.35, ex, ey, faint, silver);
        }
        // The last, long thrust through the middle, white at its point.
        line(img, 1, 16, 25, 16, silver, edge);
        line(img, 1, 17, 22, 17, faint, silver);
        // The point where they meet.
        int spark = rgb(255, 255, 230);
        for (int[] p : new int[][]{{27, 16}, {28, 16}, {27, 15}, {27, 17}, {29, 14}, {29, 18}, {30, 16}}) img.setRGB(p[0], p[1], spark);
        corners(img, rgb(214, 172, 74));
        return img;
    }

    static BufferedImage heavenSplittingCrescent() {
        BufferedImage img = sky();
        glow(img, 16, 14, 12, 0.4);
        // A bank of cloud across the top, split in two where the crescent passes.
        puffs(img, new int[][]{{3, 5, 4}, {9, 4, 3}, {22, 4, 3}, {28, 5, 4}, {13, 6, 2}, {19, 6, 2}});
        // The crescent: an arc rising from the bottom left to the top, opening to the left.
        int edge = rgb(250, 252, 245), silver = rgb(200, 222, 228), steel = rgb(110, 160, 176);
        double cx = 4, cy = 18, r = 13;
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - cx, y - cy);
            double a = Math.atan2(cy - y, x - cx);
            if (a < -1.25 || a > 1.35) continue;
            double along = (a + 1.25) / 2.6;
            double thickness = 1.2 + 3.4 * Math.sin(Math.pow(along, 0.9) * Math.PI);
            if (d > r + 0.5 || d < r - thickness - 0.5) continue;
            double depth = (r - d) / Math.max(thickness, 0.1);
            img.setRGB(x, y, depth < 0.3 ? edge : depth < 0.66 ? silver : steel);
        }
        // Rising streaks under it.
        line(img, 22, 30, 22, 22, rgb(40, 100, 104), rgb(150, 210, 205));
        line(img, 26, 31, 26, 25, rgb(40, 100, 104), rgb(150, 210, 205));
        corners(img, rgb(214, 172, 74));
        return img;
    }

    static BufferedImage swallowReturns() {
        BufferedImage img = sky();
        glow(img, 16, 16, 13, 0.5);
        // Its trail: a pale loop it flew around.
        int trail = rgb(150, 210, 205);
        for (double a = 0.3; a < 5.6; a += 0.04) {
            int x = (int) Math.round(16 + Math.cos(a) * 11), y = (int) Math.round(17 + Math.sin(a) * 9);
            if (x >= 0 && y >= 0 && x < W && y < W) img.setRGB(x, y, mix(img.getRGB(x, y), trail, 0.35 + 0.5 * a / 5.6));
        }
        // The swallow: ink body, long forked tail, swept wings, a white throat.
        int ink = rgb(20, 26, 34), throat = rgb(236, 240, 232), red = rgb(176, 52, 44);
        int[][] body = {{20, 9}, {21, 9}, {19, 10}, {20, 10}, {21, 10}, {22, 10}, {18, 11}, {19, 11}, {20, 11}, {21, 11}, {17, 12}, {18, 12}, {19, 12},
                {20, 12}, {16, 13}, {17, 13}, {18, 13}, {15, 14}, {16, 14}, {14, 15}, {15, 15}};
        for (int[] p : body) img.setRGB(p[0], p[1], ink);
        // Wings swept back on both sides.
        line(img, 19, 11, 10, 6, ink, ink);
        line(img, 18, 11, 9, 7, ink, ink);
        line(img, 19, 12, 26, 18, ink, ink);
        line(img, 18, 13, 25, 19, ink, ink);
        // The forked tail.
        line(img, 14, 15, 9, 21, ink, ink);
        line(img, 14, 15, 13, 23, ink, ink);
        img.setRGB(21, 10, throat);
        img.setRGB(22, 10, red);
        corners(img, red);
        return img;
    }

    static BufferedImage flowingCloudStance() {
        BufferedImage img = sky();
        // Slow cloud bands drifting across.
        int band = rgb(120, 180, 182), light = rgb(196, 226, 222);
        for (int x = 0; x < W; x++) {
            int y1 = (int) Math.round(8 + Math.sin(x / 5.0) * 1.5), y2 = (int) Math.round(23 + Math.sin(x / 4.0 + 1) * 1.5);
            img.setRGB(x, y1, mix(img.getRGB(x, y1), light, 0.7));
            img.setRGB(x, y1 + 1, mix(img.getRGB(x, y1 + 1), band, 0.5));
            img.setRGB(x, y2, mix(img.getRGB(x, y2), light, 0.6));
            img.setRGB(x, y2 + 1, mix(img.getRGB(x, y2 + 1), band, 0.4));
        }
        // A sword lying across: hilt bottom left, point top right.
        int edge = rgb(250, 252, 245), silver = rgb(200, 222, 228), gold = rgb(214, 172, 74), wrap = rgb(40, 64, 90);
        line(img, 10, 22, 27, 5, silver, edge);
        line(img, 11, 22, 27, 6, rgb(110, 160, 176), silver);
        line(img, 7, 19, 12, 24, gold, gold);
        line(img, 5, 27, 9, 23, wrap, wrap);
        img.setRGB(4, 28, gold);
        corners(img, gold);
        return img;
    }

    static int rgb(int r, int g, int b) { return 0xFF000000 | r << 16 | g << 8 | b; }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return rgb(r, g, bl);
    }

    static BufferedImage cloudStepSlash() {
        BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        int skyTop = rgb(14, 40, 52), skyBottom = rgb(30, 92, 96), glow = rgb(70, 150, 150);
        // Sky: deep jade at the top, lighter toward the bottom right where the cut ends.
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double t = (y + x * 0.35) / (W * 1.35);
            img.setRGB(x, y, mix(skyTop, skyBottom, t));
        }
        // A soft glow behind the blade.
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 17, y - 14);
            if (d < 11) img.setRGB(x, y, mix(img.getRGB(x, y), glow, (11 - d) / 11 * 0.45));
        }
        // Speed streaks: three horizontal-ish lines trailing to the left (the dash).
        int streak = rgb(150, 210, 205);
        int[][] streaks = {{2, 9, 9}, {1, 13, 11}, {3, 17, 8}};
        for (int[] s : streaks) for (int i = 0; i < s[2]; i++) {
            int x = s[0] + i, y = s[1] - i / 5;
            img.setRGB(x, y, mix(img.getRGB(x, y), streak, 0.25 + 0.6 * i / s[2]));
        }
        // The crescent cut: a thick arc from bottom left to top right, white at its leading edge, silver to blue behind.
        int edge = rgb(250, 252, 245), silver = rgb(200, 222, 228), steel = rgb(110, 160, 176);
        double cx = 4, cy = 30, r = 25;
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - cx, y - cy);
            double a = Math.atan2(cy - y, x - cx); // 0 = right, pi/2 = up
            if (a < 0.18 || a > 1.42) continue;
            double along = (a - 0.18) / (1.42 - 0.18);           // 0 at the bottom right end, 1 at the top
            double thickness = 1.4 + 3.6 * Math.sin(Math.pow(along, 0.8) * Math.PI); // thickest in the middle, pointed at both ends
            double inner = r - thickness;
            if (d > r + 0.5 || d < inner - 0.5) continue;
            double depth = (r - d) / Math.max(thickness, 0.1);     // 0 at the outer (leading) edge
            int c = depth < 0.28 ? edge : depth < 0.65 ? silver : steel;
            img.setRGB(x, y, c);
        }
        // A spark at the leading tip of the cut, bottom right.
        int spark = rgb(255, 255, 230);
        int[][] sparks = {{30, 25}, {29, 24}, {31, 24}, {30, 23}, {28, 22}, {31, 27}, {29, 26}};
        for (int[] p : sparks) img.setRGB(p[0], p[1], spark);
        // Clouds at the foot: two pale puffs bottom left, a darker underside.
        int cloud = rgb(224, 240, 236), shade = rgb(160, 196, 192);
        int[][] puffs = {{6, 27, 4}, {11, 28, 3}, {3, 29, 3}, {15, 29, 2}};
        for (int[] p : puffs) for (int y = p[1] - p[2]; y <= Math.min(W - 1, p[1] + p[2]); y++)
            for (int x = p[0] - p[2]; x <= p[0] + p[2]; x++) {
                if (x < 0 || x >= W || y < 0) continue;
                if (Math.hypot(x - p[0], (y - p[1]) * 1.3) > p[2] + 0.3) continue;
                img.setRGB(x, y, y > p[1] ? shade : cloud);
            }
        // Gold corner marks, like the sect's seal.
        int gold = rgb(214, 172, 74);
        for (int[] p : new int[][]{{0, 0}, {1, 0}, {0, 1}, {31, 0}, {30, 0}, {31, 1}, {0, 31}, {1, 31}, {0, 30}, {31, 31}, {30, 31}, {31, 30}}) img.setRGB(p[0], p[1], gold);
        return img;
    }
}
