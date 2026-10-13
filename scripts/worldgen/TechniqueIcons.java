import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 32x32 icons of the techniques (pixel art, drawn point by point so it stays crisp at 16 px too).
 * Usage: java scripts/worldgen/TechniqueIcons.java src/main/resources/assets/murimblock/textures/gui/technique
 *   cloud_step_slash: a silver crescent cut sweeping across a deep jade sky to a spark at its tip, speed streaks behind it (the dash) and
 *   pale clouds at its foot (the cloud step).
 */
public class TechniqueIcons {
    static final int W = 32;

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        ImageIO.write(cloudStepSlash(), "png", new File(dir, "cloud_step_slash.png"));
        System.out.println("wrote " + new File(dir, "cloud_step_slash.png"));
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
