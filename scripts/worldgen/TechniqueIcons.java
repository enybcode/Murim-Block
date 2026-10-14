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
 * And the Long Fist's, "ember night": the same frame and light, warm (dark umber, amber light, a pale fist):
 *   tiger_step_punch: a fist thrown forward, streaks of the bound behind it, dust kicked up below;
 *   rapid_fists: a fist in the middle of a burst of small impacts and short streaks (the flurry);
 *   whirlwind_kick: a sweeping ring of motion round a foot at its end, dust at the ground;
 *   inch_force: a fist pressed to a line, a ring of white at the contact, waves going out beyond it (the secret; red);
 *   steady_breath: the passive, a closed fist under three rising lines of breath.
 */
public class TechniqueIcons {
    static final int W = 32;
    static final String[] NAMES = {"cloud_step_slash", "thousand_drizzle", "heaven_splitting_crescent", "swallow_returns", "flowing_cloud_stance",
            "tiger_step_punch", "rapid_fists", "whirlwind_kick", "inch_force", "steady_breath"};
    /** Where the Long Fist's icons start in NAMES. */
    static final int FIST = 5;

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        for (int i = 0; i < NAMES.length; i++) {
            BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (i < FIST) jade(g, img, i);
            else ember(g, img, i - FIST);
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

    // ------------------------------------------------------------ B: ember night, the Long Fist

    static void emberBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            int r = (int) (30 + 34 * (1 - d)), gg = (int) (16 + 18 * (1 - d)), b = (int) (12 + 10 * (1 - d));
            img.setRGB(x, y, c(r, gg, b).getRGB());
        }
        g.setColor(c(226, 168, 92));
        g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(92, 52, 30));
        g.drawRect(1, 1, W - 3, W - 3);
    }

    static final Color AMBER = c(255, 190, 110), AMBER_DIM = c(168, 98, 56), WARM_WHITE = c(255, 246, 228),
            SKIN = c(236, 198, 160), SKIN_DARK = c(168, 112, 78), RED = c(214, 64, 48),
            SLEEVE = c(120, 38, 34), CUFF = c(230, 214, 180), PANTS = c(60, 52, 70);

    /**
     * A closed fist seen from the side, knuckles toward dir (1 right, -1 left), its middle at (x, y), s pixels tall:
     * the hand, the folds between the fingers, the thumb across below, a light on the knuckles.
     */
    static void fist(Graphics2D g, BufferedImage img, double x, double y, double s, int dir) {
        double w = s * 1.15, h = s;
        // The forearm in its sleeve, behind the fist, and the cuff.
        double back = x - dir * (w / 2 + s * 0.9);
        g.setColor(SLEEVE);
        g.fill(new Rectangle2D.Double(Math.min(back, x - dir * w * 0.3), y - h * 0.32, Math.abs(x - dir * w * 0.3 - back), h * 0.64));
        line(g, x - dir * w * 0.42, y - h * 0.34, x - dir * w * 0.42, y + h * 0.3, 1.4f, CUFF);
        g.setColor(SKIN_DARK);
        g.fill(new RoundRectangle2D.Double(x - w / 2 - 1, y - h / 2 - 1, w + 2, h + 2, s * 0.6, s * 0.6));
        g.setColor(SKIN);
        g.fill(new RoundRectangle2D.Double(x - w / 2, y - h / 2, w, h, s * 0.5, s * 0.5));
        double kx = x + dir * w * 0.22;
        for (int k = 1; k <= 3; k++) {
            int py = (int) Math.round(y - h / 2 + h * k / 4.0);
            line(g, kx - dir * w * 0.12, py, kx + dir * w * 0.32, py, 1f, SKIN_DARK);
        }
        line(g, x - dir * w * 0.35, y + h * 0.3, x + dir * w * 0.15, y + h * 0.3, 1.4f, SKIN_DARK);
        px(img, (int) Math.round(x + dir * w * 0.42), (int) Math.round(y - h * 0.32), WARM_WHITE);
    }

    /** A small impact: a four-pointed burst of radius r. */
    static void burst(Graphics2D g, double x, double y, double r, Color col) {
        g.setColor(col);
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - r); p.lineTo(x + r * 0.28, y); p.lineTo(x, y + r); p.lineTo(x - r * 0.28, y); p.closePath();
        g.fill(p);
        Path2D q = new Path2D.Double();
        q.moveTo(x - r, y); q.lineTo(x, y - r * 0.28); q.lineTo(x + r, y); q.lineTo(x, y + r * 0.28); q.closePath();
        g.fill(q);
    }

    static void ember(Graphics2D g, BufferedImage img, int i) {
        emberBack(img, g);
        switch (i) {
            case 0 -> { // Tiger Step Punch: the bound's streaks behind, the fist thrown, dust below.
                for (int k = 0; k < 3; k++) taper(g, 3, 10 + k * 5, 10, 10 + k * 5, 18, 12 + k * 3, 0.3, 2.4 - k * 0.5, k == 1 ? AMBER : AMBER_DIM);
                fist(g, img, 23, 15, 9, 1);
                for (int k = 0; k < 3; k++) { px(img, 5 + k * 4, 27, AMBER_DIM); px(img, 6 + k * 4, 26, AMBER_DIM); }
                g.setColor(c(110, 80, 60));
                g.fillRect(3, 28, 26, 1);
            }
            case 1 -> { // Rapid Fists: a fist amid a burst of impacts and short streaks.
                double[][] hits = {{8, 8, 3}, {25, 8, 2.5}, {6, 23, 2.5}, {25, 24, 3}, {16, 5, 2}};
                for (double[] h : hits) burst(g, h[0], h[1], h[2], AMBER);
                for (int k = 0; k < 4; k++) line(g, 3, 13 + k * 2, 8 + k % 2 * 2, 13 + k * 2, 1f, AMBER_DIM);
                fist(g, img, 17, 17, 9, 1);
                burst(g, 27, 16, 2.5, WARM_WHITE);
            }
            case 2 -> { // Whirlwind Kick: a sweeping ring round a foot at its end, dust at the ground.
                g.setColor(AMBER_DIM);
                g.setStroke(new BasicStroke(3.2f));
                g.draw(new Arc2D.Double(4, 6, 22, 16, 30, 280, Arc2D.OPEN));
                g.setColor(AMBER);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Arc2D.Double(4, 6, 22, 16, 60, 250, Arc2D.OPEN));
                // The leg along the end of the sweep, the boot at its end.
                taper(g, 12, 12, 15, 9, 21, 8.5, 4.2, 3.6, PANTS);
                g.setColor(c(80, 56, 40));
                g.fill(new RoundRectangle2D.Double(20, 6, 9, 5, 3, 3));
                g.setColor(CUFF);
                g.fillRect(19, 7, 1, 3);
                for (int k = 0; k < 5; k++) px(img, 6 + k * 5, 26 - k % 2, AMBER_DIM);
                g.setColor(c(110, 80, 60));
                g.fillRect(3, 28, 26, 1);
            }
            case 3 -> { // Inch Force: the fist pressed to a line, a white ring at the contact, red waves beyond.
                line(g, 18, 5, 18, 27, 2f, c(150, 110, 90));
                fist(g, img, 11, 16, 9, 1);
                g.setColor(WARM_WHITE);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Ellipse2D.Double(15, 12, 7, 8));
                g.setColor(RED);
                for (int k = 0; k < 3; k++) g.draw(new Arc2D.Double(15 + k * 4, 9 + k, 9, 14 - k * 2, -60, 120, Arc2D.OPEN));
            }
            default -> { // Steady Breath: three rising lines of breath over a closed fist.
                for (int k = 0; k < 3; k++) {
                    g.setColor(k == 1 ? AMBER : AMBER_DIM);
                    g.setStroke(new BasicStroke(1.4f));
                    Path2D p = new Path2D.Double();
                    double x0 = 10 + k * 6;
                    p.moveTo(x0, 17);
                    p.curveTo(x0 - 3, 13, x0 + 3, 9, x0, 4);
                    g.draw(p);
                }
                fist(g, img, 16, 23, 10, 1);
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
