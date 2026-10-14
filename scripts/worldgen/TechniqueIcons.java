import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the 32x32 icons of the Azure Cloud Sword Art's techniques, "jade night" style: a deep jade night, a thin bright
 * frame, each move traced in azure light (pixel art, no smoothing, so it stays crisp at 16 px in the combat bar).
 * Usage: java scripts/worldgen/TechniqueIcons.java src/main/resources/assets/murimblock/textures/gui/technique
 *   cloud_step_slash: a level draw-cut across, speed lines behind, the blade at its end (the dash);
 *   thousand_drizzle: six thin sharp cuts across the square at their own angles (the flurry's lines of light);
 *   heaven_splitting_crescent: a crescent rising, the blade pointed at the sky, speed lines under it, the ground below;
 *   swallow_returns: a swallow in profile turning back on its trail (the secret; red eye);
 *   flowing_cloud_stance: the passive, the blade held in guard over drifting cloud bands.
 */
public class TechniqueIcons {
    static final int W = 32;
    static final String[] NAMES = {"cloud_step_slash", "thousand_drizzle", "heaven_splitting_crescent", "swallow_returns", "flowing_cloud_stance"};

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        for (int i = 0; i < NAMES.length; i++) {
            BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            jade(g, img, i);
            g.dispose();
            File f = new File(dir, NAMES[i] + ".png");
            ImageIO.write(img, "png", f);
            System.out.println("wrote " + f);
        }
    }


    // ------------------------------------------------------------ helpers

    static Color c(int r, int g, int b) { return new Color(r, g, b); }
    static Color c(int r, int g, int b, int a) { return new Color(r, g, b, a); }

    /** A stroke tapering from width w0 to w1 along a quadratic curve. */
    static void taper(Graphics2D g, double x0, double y0, double cx, double cy, double x1, double y1, double w0, double w1, Color col) {
        g.setColor(col);
        int n = 24;
        double[] lx = new double[n + 1], ly = new double[n + 1], rx = new double[n + 1], ry = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n, u = 1 - t;
            double px = u * u * x0 + 2 * u * t * cx + t * t * x1, py = u * u * y0 + 2 * u * t * cy + t * t * y1;
            double dx = 2 * u * (cx - x0) + 2 * t * (x1 - cx), dy = 2 * u * (cy - y0) + 2 * t * (y1 - cy);
            double len = Math.hypot(dx, dy);
            if (len < 1e-6) { dx = 1; dy = 0; len = 1; }
            double w = (w0 + (w1 - w0) * t) / 2;
            lx[i] = px - dy / len * w; ly[i] = py + dx / len * w;
            rx[i] = px + dy / len * w; ry[i] = py - dx / len * w;
        }
        Path2D p = new Path2D.Double();
        p.moveTo(lx[0], ly[0]);
        for (int i = 1; i <= n; i++) p.lineTo(lx[i], ly[i]);
        for (int i = n; i >= 0; i--) p.lineTo(rx[i], ry[i]);
        p.closePath();
        g.fill(p);
    }

    static void line(Graphics2D g, double x0, double y0, double x1, double y1, float w, Color col) {
        g.setColor(col);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(x0, y0, x1, y1));
    }

    static void px(BufferedImage img, int x, int y, Color col) {
        if (x >= 0 && y >= 0 && x < W && y < W) img.setRGB(x, y, col.getRGB());
    }

    static void disc(Graphics2D g, double cx, double cy, double r, Color col) {
        g.setColor(col);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
    }

    /** A small sword: hilt at (x, y), blade toward angle (degrees, 0 = right), length len. */
    static void sword(Graphics2D g, double x, double y, double deg, double len, Color blade, Color edge, Color hilt) {
        double a = Math.toRadians(deg), dx = Math.cos(a), dy = Math.sin(a);
        taper(g, x, y, x + dx * len / 2, y + dy * len / 2, x + dx * len, y + dy * len, 2.6, 0.6, blade);
        line(g, x + dx * 1 - dy * 0.6, y + dy * 1 + dx * 0.6, x + dx * len * 0.9 - dy * 0.6, y + dy * len * 0.9 + dx * 0.6, 0.8f, edge);
        // Guard and grip.
        line(g, x - dy * 2.5, y + dx * 2.5, x + dy * 2.5, y - dx * 2.5, 1.6f, hilt);
        line(g, x, y, x - dx * 4, y - dy * 4, 1.8f, hilt.darker());
    }

    // ------------------------------------------------------------ A: jade night, glowing azure motion

    static void jadeBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            int r = (int) (10 + 12 * (1 - d)), gg = (int) (30 + 34 * (1 - d)), b = (int) (42 + 40 * (1 - d));
            img.setRGB(x, y, c(r, gg, b).getRGB());
        }
        // Thin bright frame.
        g.setColor(c(120, 190, 210));
        g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(40, 80, 96));
        g.drawRect(1, 1, W - 3, W - 3);
    }

    static final Color AZ = c(140, 215, 255), AZ_DIM = c(70, 140, 190), WHITE = c(240, 250, 255), STEEL = c(200, 215, 225), GOLD = c(214, 172, 80);

    static void jade(Graphics2D g, BufferedImage img, int i) {
        jadeBack(img, g);
        switch (i) {
            case 0 -> { // Cloud Step Slash: a level draw-cut across, afterimages and speed lines behind.
                for (int k = 0; k < 4; k++) line(g, 3, 9 + k * 4, 10 + k % 2 * 3, 9 + k * 4, 1f, AZ_DIM);
                taper(g, 6, 20, 16, 12, 29, 15, 0.5, 4.2, c(90, 170, 220));
                taper(g, 7, 19, 17, 12.5, 29, 15, 0.3, 2.0, WHITE);
                sword(g, 24, 22, -20, 9, STEEL, WHITE, GOLD);
                for (int k = 0; k < 3; k++) px(img, 29 - k, 16 + k, WHITE);
            }
            case 1 -> { // Thousand Drizzle: six thin sharp cuts across the square at their own angles, Sukuna-style.
                for (double[] cut : CUTS) {
                    g.setColor(AZ_DIM);
                    lens(g, cut[0] - 1, cut[1] + 1, cut[2] - 1, cut[3] + 1, 2.6);
                }
                for (double[] cut : CUTS) {
                    g.setColor(WHITE);
                    lens(g, cut[0], cut[1], cut[2], cut[3], 1.6);
                }
            }
            case 2 -> { // Heaven-Splitting Crescent: a crescent rising, the blade at the sky, a crater below.
                g.setColor(c(90, 170, 220));
                g.fill(crescent(18, 16, 12, 4, 100, 260));
                g.setColor(WHITE);
                g.fill(crescent(18, 16, 12, 1.8, 105, 255));
                sword(g, 16, 13, -90, 12, STEEL, WHITE, GOLD);
                for (int k = 0; k < 3; k++) line(g, 11 + k * 5, 24, 11 + k * 5, 29, 1f, AZ_DIM);
                g.setColor(c(120, 95, 70));
                g.fillRect(6, 29, 20, 2);
                px(img, 9, 28, c(150, 120, 90)); px(img, 22, 28, c(150, 120, 90));
            }
            case 3 -> swallow(g, img, WHITE, AZ, c(200, 60, 50));
            default -> { // Flowing Cloud Stance: the blade held in guard over drifting cloud bands.
                for (int k = 0; k < 3; k++) {
                    g.setColor(k == 1 ? c(160, 210, 230) : c(110, 170, 200));
                    g.fill(new RoundRectangle2D.Double(3 + k * 4, 18 + k * 4, 22 - k * 3, 3, 3, 3));
                }
                sword(g, 9, 22, -45, 19, STEEL, WHITE, GOLD);
                disc(g, 24, 7, 1.2, WHITE);
            }
        }
    }

    /** The flurry's six cuts as lines across the square (x0, y0, x1, y1): diagonals, a level one, an upright one. */
    static final double[][] CUTS = {
            {4, 6, 22, 18}, {8, 21, 29, 21}, {5, 26, 24, 12}, {17, 3, 15, 29}, {12, 28, 28, 6}, {3, 13, 27, 10}};

    /** A thin lens (sharp at both ends) from a to b, widest `w` in the middle. */
    static void lens(Graphics2D g, double ax, double ay, double bx, double by, double w) {
        double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy), nx = -dy / len * w / 2, ny = dx / len * w / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(ax, ay);
        p.quadTo((ax + bx) / 2 + nx * 2, (ay + by) / 2 + ny * 2, bx, by);
        p.quadTo((ax + bx) / 2 - nx * 2, (ay + by) / 2 - ny * 2, ax, ay);
        g.fill(p);
    }

    /** A crescent: the ring between two circles, between two angles (degrees, counter-clockwise from right, y down). */
    static Shape crescent(double cx, double cy, double r, double thick, double from, double to) {
        Area outer = new Area(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, from, to - from, Arc2D.PIE));
        double r2 = r - thick;
        outer.subtract(new Area(new Ellipse2D.Double(cx - r2 + thick * 0.6, cy - r2, 2 * r2, 2 * r2)));
        return outer;
    }

    static void swallow(Graphics2D g, BufferedImage img, Color body, Color trail, Color eye) {
        // Its trail curling back.
        g.setColor(trail);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Arc2D.Double(9, 8, 20, 20, 200, 250, Arc2D.OPEN));
        // A swallow in profile flying left and back: the body, the raised wing, the deep forked tail.
        taper(g, 7, 17, 13, 17, 21, 16, 5.2, 1.6, body);
        disc(g, 8, 16.5, 2.8, body);
        taper(g, 12, 15, 14, 5, 25, 3, 5.0, 0.5, body);
        taper(g, 14, 18, 17, 23, 22, 26, 3.0, 0.5, body);
        taper(g, 20, 16, 25, 14, 30, 10, 2.0, 0.5, body);
        taper(g, 20, 16, 25, 18, 30, 22, 2.0, 0.5, body);
        // The beak.
        Path2D p = new Path2D.Double();
        p.moveTo(5.5, 15.5); p.lineTo(2.5, 17); p.lineTo(5.5, 18); p.closePath();
        g.setColor(body);
        g.fill(p);
        px(img, 7, 16, eye);
    }

}
