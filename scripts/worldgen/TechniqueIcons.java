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
 * And the Sanguine Art's, "blood night": the same frame, crimson black, the moves in glowing blood crystal:
 *   bloodbane_drain (Blood Crystal): an open hand, blood rising from it into a great crystal, fragments circling it;
 *   scarlet_tear: the blood sword thrown point first, the great arc of its swing behind it, a burst where it strikes;
 *   devourer_of_worlds (Blood Crystal Form): a body in shadow, crystals bursting from its shoulders and back, its heart lit;
 *   crystal_barrage: a volley of crystals flying in a fan, speed lines behind them;
 *   crimson_stance: the passive, a fist in guard, drops of blood rising round it;
 *   blood_iceberg: the blood temple's arena, a wall of red bricks, gold spires lit at their tips, blood falling down it.
 * And the Celestial Peak Spear's, "peak night": a navy night, a gold frame, the spear in steel and azure, the Alliance's:
 *   piercing_dragon: the spear driven level across the square, a long trail behind it, the dragon's line;
 *   plum_blossom_thrusts: five spear points fanned out from one hand like a plum flower, a petal of light at each;
 *   thousand_soldier_sweep: the spear round in a full ring at the knees, dust thrown out from it;
 *   long_reach: the passive, a spear level with a gold mark at its point, 3 blocks out.
 */
public class TechniqueIcons {
    static final int W = 32;
    static final String[] NAMES = {"cloud_step_slash", "thousand_drizzle", "heaven_splitting_crescent", "swallow_returns", "flowing_cloud_stance",
            "tiger_step_punch", "rapid_fists", "whirlwind_kick", "inch_force", "steady_breath",
            "bloodbane_drain", "scarlet_tear", "devourer_of_worlds", "crystal_barrage", "crimson_stance", "blood_iceberg",
            "piercing_dragon", "plum_blossom_thrusts", "thousand_soldier_sweep", "long_reach"};
    /** Where the Long Fist's and the Sanguine Art's icons start in NAMES. */
    static final int FIST = 5, BLOOD = 10, SPEAR = 16;

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        for (int i = 0; i < NAMES.length; i++) {
            BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (i < FIST) jade(g, img, i);
            else if (i < BLOOD) ember(g, img, i - FIST);
            else if (i < SPEAR) blood(g, img, i - BLOOD);
            else peak(g, img, i - SPEAR);
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

    // ------------------------------------------------------------ C: blood night, the Sanguine Art

    static void bloodBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            int r = (int) (26 + 34 * (1 - d)), gg = (int) (6 + 6 * (1 - d)), b = (int) (12 + 10 * (1 - d));
            img.setRGB(x, y, c(r, gg, b).getRGB());
        }
        g.setColor(c(232, 86, 98));
        g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(96, 18, 30));
        g.drawRect(1, 1, W - 3, W - 3);
    }

    static final Color BLOOD_RED = c(204, 28, 48), BLOOD_DIM = c(120, 18, 34), CRYSTAL = c(238, 72, 96), CRYSTAL_DARK = c(150, 18, 40),
            CRYSTAL_LIGHT = c(255, 176, 190), PINK_WHITE = c(255, 232, 236), SHADOW = c(16, 4, 8);

    /**
     * A cut crystal from (x0, y0) to its point (x1, y1), w wide at its widest (a third of the way along): a dark side, a
     * lit side, a glint along the ridge.
     */
    static void crystal(Graphics2D g, double x0, double y0, double x1, double y1, double w) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), nx = -dy / len * w / 2, ny = dx / len * w / 2;
        double mx = x0 + dx * 0.33, my = y0 + dy * 0.33;
        Path2D lit = new Path2D.Double(), dark = new Path2D.Double();
        lit.moveTo(x0, y0); lit.lineTo(mx + nx, my + ny); lit.lineTo(x1, y1); lit.closePath();
        dark.moveTo(x0, y0); dark.lineTo(mx - nx, my - ny); dark.lineTo(x1, y1); dark.closePath();
        g.setColor(CRYSTAL_DARK); g.fill(dark);
        g.setColor(CRYSTAL); g.fill(lit);
        line(g, mx + nx * 0.3, my + ny * 0.3, x1 - dx * 0.12, y1 - dy * 0.12, 0.8f, CRYSTAL_LIGHT);
    }

    /** A crystal of gold from (x0, y0) to its point (x1, y1), w wide: a dark side, a lit side, a glint. */
    static void crystalGold(Graphics2D g, double x0, double y0, double x1, double y1, double w, Color dark, Color light) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), nx = -dy / len * w / 2, ny = dx / len * w / 2;
        double mx = x0 + dx * 0.33, my = y0 + dy * 0.33;
        Path2D lit = new Path2D.Double(), shade = new Path2D.Double();
        lit.moveTo(x0, y0); lit.lineTo(mx + nx, my + ny); lit.lineTo(x1, y1); lit.closePath();
        shade.moveTo(x0, y0); shade.lineTo(mx - nx, my - ny); shade.lineTo(x1, y1); shade.closePath();
        g.setColor(dark.darker()); g.fill(shade);
        g.setColor(dark); g.fill(lit);
        line(g, mx + nx * 0.3, my + ny * 0.3, x1 - dx * 0.12, y1 - dy * 0.12, 0.8f, light);
    }

    /** A drop of blood, its round foot at (x, y), r wide. */
    static void drop(Graphics2D g, double x, double y, double r, Color col) {
        g.setColor(col);
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - r * 2.4);
        p.quadTo(x + r * 1.1, y - r * 0.6, x + r, y);
        p.quadTo(x, y + r * 1.3, x - r, y);
        p.quadTo(x - r * 1.1, y - r * 0.6, x, y - r * 2.4);
        g.fill(p);
    }

    // ------------------------------------------------------------ D: peak night, the Celestial Peak Spear

    static final Color NAVY_GOLD = c(217, 178, 95), SPEAR_STEEL = c(214, 222, 230), STEEL_DARK = c(130, 140, 150), AZURE = c(43, 95, 168),
            AZURE_LIGHT = c(120, 170, 235), SHAFT = c(34, 52, 92);

    static void peakBack(BufferedImage img, Graphics2D g) {
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double d = Math.hypot(x - 15.5, y - 15.5) / 22.0;
            img.setRGB(x, y, c((int) (10 + 14 * (1 - d)), (int) (16 + 22 * (1 - d)), (int) (34 + 40 * (1 - d))).getRGB());
        }
        g.setColor(NAVY_GOLD); g.drawRect(0, 0, W - 1, W - 1);
        g.setColor(c(60, 52, 30)); g.drawRect(1, 1, W - 3, W - 3);
    }

    /** A spear from its butt (x0, y0) to its point (x1, y1): a navy shaft, an azure socket, a steel leaf head. */
    static void spear(Graphics2D g, BufferedImage img, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
        double hx = x1 - ux * 6, hy = y1 - uy * 6;
        line(g, x0, y0, hx, hy, 1.6f, SHAFT);
        line(g, x0, y0, hx, hy, 0.6f, c(70, 96, 150));
        line(g, hx - ux * 1.5, hy - uy * 1.5, hx, hy, 2.2f, AZURE);
        Path2D head = new Path2D.Double();
        double nx = -uy * 1.6, ny = ux * 1.6;
        head.moveTo(hx, hy); head.lineTo(hx + ux * 2.5 + nx, hy + uy * 2.5 + ny); head.lineTo(x1, y1); head.lineTo(hx + ux * 2.5 - nx, hy + uy * 2.5 - ny); head.closePath();
        g.setColor(STEEL_DARK); g.fill(head);
        line(g, hx + ux * 0.5, hy + uy * 0.5, x1 - ux * 0.5, y1 - uy * 0.5, 0.7f, SPEAR_STEEL);
        px(img, (int) Math.round(hx - ux), (int) Math.round(hy - uy + 1), c(200, 60, 50));
    }

    static void peak(Graphics2D g, BufferedImage img, int i) {
        peakBack(img, g);
        switch (i) {
            case 0 -> { // Piercing Dragon: the spear driven level across, a long trail behind.
                for (int k = 0; k < 3; k++) line(g, 2, 14 + k * 2, 14, 15 + k * 1.2, 1f, k == 1 ? AZURE_LIGHT : AZURE);
                spear(g, img, 6, 17, 29, 15);
                burst(g, 29, 15, 2.5, AZURE_LIGHT);
            }
            case 1 -> { // Plum Blossom Thrusts: five points fanned from one hand, a petal of light at each.
                double[][] tips = {{27, 6}, {29, 13}, {28, 21}, {22, 27}, {20, 3}};
                for (double[] t : tips) spear(g, img, 5, 24, t[0], t[1]);
                for (double[] t : tips) { disc(g, t[0], t[1], 1.6, c(255, 200, 220)); px(img, (int) t[0], (int) t[1], c(255, 255, 255)); }
            }
            case 2 -> { // Sweep of a Thousand Soldiers: the spear round in a ring at the knees, dust thrown out.
                g.setColor(AZURE); g.setStroke(new BasicStroke(2f)); g.draw(new Ellipse2D.Double(3, 14, 26, 11));
                g.setColor(AZURE_LIGHT); g.setStroke(new BasicStroke(0.8f)); g.draw(new Arc2D.Double(3, 14, 26, 11, 200, 160, Arc2D.OPEN));
                spear(g, img, 16, 20, 29, 22);
                for (int[] q : new int[][]{{3, 27}, {6, 29}, {27, 28}, {29, 26}, {16, 29}}) px(img, q[0], q[1], c(190, 170, 130));
                // A small figure crouched in the middle.
                g.setColor(c(16, 22, 40)); g.fill(new Ellipse2D.Double(13, 10, 6, 6)); g.fill(new RoundRectangle2D.Double(12, 15, 8, 6, 3, 3));
            }
            default -> { // Long Reach: a spear level, a gold mark at its point 3 blocks out.
                spear(g, img, 3, 20, 27, 20);
                for (int k = 0; k < 3; k++) { g.setColor(NAVY_GOLD); g.fillRect(6 + k * 7, 25, 5, 1); }
                line(g, 27, 12, 27, 26, 1f, NAVY_GOLD);
                disc(g, 27, 20, 2.2, c(255, 230, 150));
            }
        }
    }

    static void blood(Graphics2D g, BufferedImage img, int i) {
        bloodBack(img, g);
        switch (i) {
            case 0 -> { // Blood Crystal: an open hand, blood rising into a great crystal, fragments circling it.
                g.setColor(BLOOD_DIM);
                g.setStroke(new BasicStroke(1f));
                g.draw(new Ellipse2D.Double(5, 6, 22, 10));
                // The great crystal, its foot over the palm.
                crystal(g, 16, 19, 16, 1.5, 10);
                crystal(g, 6, 12, 3, 9, 2.6);
                crystal(g, 26, 10, 29, 7, 2.6);
                crystal(g, 21, 15, 24, 16.5, 2);
                // Blood rising from the palm.
                disc(g, 15.5, 20.5, 0.9, BLOOD_RED);
                disc(g, 16.5, 22.5, 0.9, CRYSTAL);
                // The open hand from below, palm up: the palm, four fingers and the thumb, outlined.
                g.setColor(SKIN_DARK);
                g.fill(new RoundRectangle2D.Double(9, 24, 14, 7, 4, 4));
                for (int k = 0; k < 4; k++) g.fill(new RoundRectangle2D.Double(9.5 + k * 3.2, 20.5, 3, 6, 2, 2));
                g.fill(new RoundRectangle2D.Double(22, 23, 5, 3, 2, 2));
                g.setColor(SKIN);
                g.fill(new RoundRectangle2D.Double(10, 25, 12, 5, 3, 3));
                for (int k = 0; k < 4; k++) g.fill(new RoundRectangle2D.Double(10 + k * 3.2, 21, 2, 5, 2, 2));
                g.fill(new RoundRectangle2D.Double(22.5, 23.5, 4, 2, 2, 2));
                px(img, 16, 27, CRYSTAL);
            }
            case 1 -> { // Scarlet Tear: the great arc of the swing behind, the blood sword thrown point first, a burst where it strikes.
                g.setColor(BLOOD_DIM);
                g.setStroke(new BasicStroke(3f));
                g.draw(new Arc2D.Double(3, 6, 24, 24, 100, 140, Arc2D.OPEN));
                g.setColor(BLOOD_RED);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Arc2D.Double(3, 6, 24, 24, 110, 120, Arc2D.OPEN));
                // The sword: a long crystal blade, a dark red guard across it, a wrapped grip, flying up and right.
                crystal(g, 10, 23, 26, 7, 4.6);
                line(g, 7.5, 20.5, 12.5, 25.5, 2f, c(150, 30, 40));
                line(g, 10, 23, 5.5, 27.5, 2f, c(70, 14, 20));
                px(img, 7, 26, CRYSTAL); px(img, 5, 28, CRYSTAL_DARK);
                burst(g, 27, 6, 4.5, CRYSTAL_LIGHT);
                burst(g, 27, 6, 2.2, PINK_WHITE);
                for (int[] p : new int[][]{{29, 11}, {22, 3}, {30, 3}}) px(img, p[0], p[1], CRYSTAL);
            }
            case 2 -> { // Blood Crystal Form: a body in shadow, crystals bursting from it, its heart lit.
                for (double[] k : new double[][]{{10, 15, 3, 4}, {13, 12, 9, 2}, {16, 12, 16, 1}, {19, 12, 23, 2}, {22, 15, 29, 4}, {9, 19, 3, 15}, {23, 19, 29, 15}})
                    crystal(g, k[0], k[1], k[2], k[3], 3.2);
                g.setColor(SHADOW);
                g.fill(new Ellipse2D.Double(12, 9, 8, 8));
                g.fill(new RoundRectangle2D.Double(7, 17, 18, 14, 7, 7));
                g.setColor(c(80, 12, 24));
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Double(7, 17, 18, 14, 7, 7));
                disc(g, 16, 23, 2.6, BLOOD_RED);
                disc(g, 16, 23, 1.4, CRYSTAL_LIGHT);
                for (int[] p : new int[][]{{12, 21}, {20, 21}, {13, 27}, {19, 27}}) px(img, p[0], p[1], BLOOD_RED);
            }
            case 3 -> { // Crystal Barrage: a volley of crystals in a fan, speed lines behind.
                double[][] volley = {{4, 7, 15, 10}, {8, 14, 22, 14}, {5, 20, 17, 18}, {12, 25, 25, 21}, {14, 4, 27, 8}};
                for (double[] v : volley) line(g, v[0] - 3, v[1] + (v[1] - v[3]) * 0.25, v[0] + 1, v[1], 1f, BLOOD_DIM);
                for (double[] v : volley) crystal(g, v[0], v[1], v[2], v[3], 3);
                burst(g, 28, 14, 2.5, CRYSTAL_LIGHT);
            }
            case 5 -> { // Blood Iceberg: the blood temple's arena, its wall of red bricks, gold spires lit at their tips, blood falling.
                Color brick = c(118, 30, 24), brickDark = c(70, 14, 12), gold = c(208, 170, 58), goldLight = c(255, 236, 160), lantern = c(255, 200, 110);
                // The ground and the arena's floor seen over the near wall, a gold seal on it.
                g.setColor(SHADOW); g.fillRect(2, 27, 28, 3);
                g.setColor(c(92, 22, 18)); g.fill(new Ellipse2D.Double(5, 12, 22, 8));
                g.setColor(gold); g.setStroke(new BasicStroke(1f)); g.draw(new Ellipse2D.Double(10, 14, 12, 4));
                px(img, 16, 16, goldLight);
                // The near wall: courses of bricks, joints dark, crenels along its top.
                g.setColor(brick); g.fillRect(4, 18, 24, 10);
                g.setColor(brickDark);
                for (int y = 20; y < 28; y += 3) g.drawLine(4, y, 27, y);
                for (int y = 18, k = 0; y < 28; y += 3, k++) for (int x = 6 + (k % 2) * 2; x < 28; x += 5) g.drawLine(x, y, x, y + 2);
                g.setColor(brick); for (int x = 4; x < 28; x += 4) g.fillRect(x, 16, 2, 2);
                // Gold spires, each crowned with a lantern.
                for (int[] sp : new int[][]{{6, 6}, {16, 2}, {26, 6}}) {
                    crystalGold(g, sp[0], 18, sp[0], sp[1] + 3, 3.4, gold, goldLight);
                    g.setColor(lantern); g.fillRect(sp[0] - 1, sp[1], 3, 3);
                    px(img, sp[0], sp[1] + 1, c(255, 250, 230));
                }
                // Blood falling down the wall into the channel.
                for (int x : new int[]{11, 21}) { line(g, x, 18, x, 26, 1.6f, BLOOD_RED); px(img, x, 27, CRYSTAL); }
            }
            default -> { // Crimson Stance: a fist in guard, drops of blood rising round it.
                drop(g, 8, 10, 2, BLOOD_RED);
                drop(g, 24, 8, 1.6, CRYSTAL);
                drop(g, 16, 6, 1.3, BLOOD_DIM);
                fist(g, img, 16, 20, 10, 1);
                // A crystal over the knuckles.
                crystal(g, 20, 17, 26, 14, 2.4);
                for (int k = 0; k < 3; k++) px(img, 6 + k * 9, 28, BLOOD_DIM);
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
