import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Paints the Golden Dragon's scroll of wares (client/ScrollShopScreen, mob/GoldenDragonShop) and the martial guides'
 * item sprites (item/MartialGuideItem), from the mock-up Enzo approved on 17/10/2026. Everything is drawn at twice the
 * GUI's pixel density.
 *   - textures/gui/sage_scroll.png: the mounting, 592 x 502: dark red brocade with a gold key border, and the aged
 *     rice paper (540 x 454 at 26, 24) with mountains in mist and a faint golden dragon, the dragon seal at its corner.
 *   - textures/gui/sage_scroll_parts.png, 644 x 128: a roller (0,0 644x26), a tassel (0,30 20x50), ink drops full and
 *     empty (30,30 / 50,30 14x14), a gold ingot (70,30 24x18), the buy seal plain, hot and grey (100 / 140 / 180,30
 *     32x32), a brush stroke (0,84 488x6), the hover glow (0,88 500x40).
 *   - textures/item/<art>_guide.png and its model: a closed book seen a little from the side, in the art's colour,
 *     with its emblem in gold.
 * Run from the repository root: java scripts/animation/ScrollShopArt.java
 */
public class ScrollShopArt {
    static final Path ASSETS = Path.of("src/main/resources/assets/murimblock");
    static final Random R = new Random(17);

    static Color c(int rgb) { return new Color(rgb); }
    static Color ca(int rgb, int a) { return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a); }
    static int mix(int a, int b, double t) { t = Math.max(0, Math.min(1, t)); int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bb = (int) ((a & 255) * (1 - t) + (b & 255) * t); return (r << 16) | (gg << 8) | bb; }
    static double noise(double x, double y) { return (Math.sin(x * 1.7 + Math.sin(y * 1.3)) + Math.sin(y * 2.1 + Math.sin(x * 0.9)) + 2) / 4; }

    static Graphics2D smooth(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    public static void main(String[] a) throws Exception {
        Files.createDirectories(ASSETS.resolve("textures/gui"));
        ImageIO.write(mounting(), "png", ASSETS.resolve("textures/gui/sage_scroll.png").toFile());
        ImageIO.write(parts(), "png", ASSETS.resolve("textures/gui/sage_scroll_parts.png").toFile());
        guides();
        System.out.println("ok");
    }

    // ------------------------------------------------------------------ the mounting and its paper

    static final int MW = 592, MH = 502, PX = 26, PY = 24, PW = 540, PH = 454;

    static BufferedImage mounting() {
        BufferedImage img = new BufferedImage(MW, MH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        // The brocade: deep red silk, a woven diagonal, a gold key border.
        g.setPaint(new GradientPaint(0, 0, c(0x5a0e0c), MW / 2f, 0, c(0x7a1612), true));
        g.fillRect(0, 0, MW, MH);
        g.setColor(ca(0xffffff, 10));
        for (int k = -MH; k < MW; k += 4) g.drawLine(k, 0, k + MH, MH);
        g.setStroke(new BasicStroke(2f));
        g.setColor(c(0xd8a640));
        fret(g, 8, 8, MW - 8, MH - 8);
        g.setColor(c(0xf0c860));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(PX - 4, PY - 4, PW + 7, PH + 7);
        // The paper: warm at the centre, stained towards the edges, fibres.
        BufferedImage paper = new BufferedImage(PW, PH, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < PW; x++) for (int y = 0; y < PH; y++) {
            double ex = Math.min(x, PW - 1 - x) / 40.0, ey = Math.min(y, PH - 1 - y) / 40.0, e = Math.min(1, Math.min(ex, ey));
            double n = noise(x * 0.05, y * 0.05) * 0.5 + noise(x * 0.3, y * 0.3) * 0.15;
            int base = mix(0xd9bf8a, 0xf6e8c6, e * 0.85 + n * 0.25);
            if (R.nextDouble() < 0.02) base = mix(base, 0xc8a870, 0.5);
            paper.setRGB(x, y, 0xff000000 | base);
        }
        g.drawImage(paper, PX, PY, null);
        g.setClip(new Rectangle(PX, PY, PW, PH));
        // Three ranges of mountains in mist across the top, behind the title.
        mountains(g, PX, PX + PW, PY + 120, 46, 0x7a8a8a, 60, 3);
        mountains(g, PX, PX + PW, PY + 140, 34, 0x56686a, 80, 5);
        mountains(g, PX, PX + PW, PY + 160, 22, 0x3a4a4a, 70, 9);
        g.setPaint(new LinearGradientPaint(0, PY + 110, 0, PY + 200, new float[]{0f, 0.5f, 1f},
                new Color[]{ca(0xf6e8c6, 0), ca(0xf6e8c6, 230), ca(0xf6e8c6, 0)}));
        g.fillRect(PX, PY + 110, PW, 90);
        dragon(g, PX, PY, PX + PW, PY + PH);
        g.setClip(null);
        // The dragon's seal, at the right of the paper under the title.
        seal(g, PX + PW - 30, PY + 98, 30, "龍");
        return img;
    }

    static void fret(Graphics2D g, int x0, int y0, int x1, int y1) {
        g.drawRect(x0, y0, x1 - x0, y1 - y0);
        for (int x = x0 + 6; x < x1 - 14; x += 14) {
            g.drawLine(x, y0 + 4, x + 8, y0 + 4); g.drawLine(x + 8, y0 + 4, x + 8, y0 + 10); g.drawLine(x + 8, y0 + 10, x + 4, y0 + 10);
            g.drawLine(x, y1 - 4, x + 8, y1 - 4); g.drawLine(x + 8, y1 - 4, x + 8, y1 - 10); g.drawLine(x + 8, y1 - 10, x + 4, y1 - 10);
        }
        for (int y = y0 + 20; y < y1 - 20; y += 14) {
            g.drawLine(x0 + 4, y, x0 + 4, y + 8); g.drawLine(x0 + 4, y + 8, x0 + 10, y + 8); g.drawLine(x0 + 10, y + 8, x0 + 10, y + 4);
            g.drawLine(x1 - 4, y, x1 - 4, y + 8); g.drawLine(x1 - 4, y + 8, x1 - 10, y + 8); g.drawLine(x1 - 10, y + 8, x1 - 10, y + 4);
        }
    }

    static void mountains(Graphics2D g, int x0, int x1, int base, int height, int rgb, int alpha, int seed) {
        Path2D p = new Path2D.Double();
        p.moveTo(x0, base + 30);
        for (int x = x0; x <= x1; x += 4) {
            double t = (x - x0) / (double) (x1 - x0);
            double peak = Math.pow(Math.abs(Math.sin(t * Math.PI * (2.2 + seed * 0.13) + seed)), 3) * height + Math.sin(t * 40 + seed) * 3;
            p.lineTo(x, base - peak);
        }
        p.lineTo(x1, base + 30);
        p.closePath();
        g.setPaint(new GradientPaint(0, base - height, ca(rgb, alpha), 0, base + 20, ca(rgb, 0)));
        g.fill(p);
    }

    static void dragon(Graphics2D g, int x0, int y0, int x1, int y1) {
        Path2D p = new Path2D.Double();
        double cx = (x0 + x1) / 2.0;
        p.moveTo(x1 - 40, y0 + 190);
        p.curveTo(x0 + 20, y0 + 210, x1 - 20, y0 + 300, cx, y0 + 320);
        p.curveTo(x0 + 10, y0 + 340, x0 + 60, y1 - 30, x1 - 30, y1 - 50);
        g.setStroke(new BasicStroke(26f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ca(0xd89a28, 34));
        g.draw(p);
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ca(0xffd060, 28));
        g.draw(p);
        g.setStroke(new BasicStroke(1f));
    }

    static void drawCentered(Graphics2D g, String s, int cx, int y) { g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y); }

    static void seal(Graphics2D g, int x, int y, int s, String ch) {
        g.setColor(c(0xb81e18));
        g.fill(new RoundRectangle2D.Double(x - s / 2.0, y - s / 2.0, s, s, 6, 6));
        g.setColor(c(0xf6e2c0));
        g.setFont(new Font("KaiTi", Font.BOLD, (int) (s * 0.78)));
        drawCentered(g, ch, x, y + (int) (s * 0.28));
        g.setColor(ca(0xf6e8c6, 60));
        for (int k = 0; k < 14; k++) g.fillRect(x - s / 2 + R.nextInt(s), y - s / 2 + (R.nextBoolean() ? 0 : s - 2), 2, 2);
    }

    // ------------------------------------------------------------------ the parts

    static BufferedImage parts() {
        BufferedImage img = new BufferedImage(644, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        roller(g, 0, 644, 0, 26);
        tassel(g, 10, 30);
        drop(g, 37, 38, true);
        drop(g, 57, 38, false);
        ingot(g, 71, 33);
        roundSeal(g, 116, 46, 0xa81c16);
        roundSeal(g, 156, 46, 0xd8281e);
        roundSeal(g, 196, 46, 0x8a7a64);
        brush(g, 0, 488, 87);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(250, 108), 230, new float[]{0f, 1f}, new Color[]{ca(0xffe8a0, 150), ca(0xffe8a0, 0)}));
        g.fillRect(0, 88, 500, 40);
        return img;
    }

    static void roller(Graphics2D g, int x0, int x1, int y, int h) {
        g.setPaint(new GradientPaint(0, y + 2, c(0x6a2a14), 0, y + h - 2, c(0x1a0806)));
        g.fill(new RoundRectangle2D.Double(x0 + 18, y + 3, x1 - x0 - 36, h - 6, h - 6, h - 6));
        g.setColor(ca(0xffd0a0, 70));
        g.fill(new RoundRectangle2D.Double(x0 + 24, y + 6, x1 - x0 - 48, 3, 3, 3));
        for (int cx : new int[]{x0, x1 - 26}) {
            g.setPaint(new GradientPaint(0, y, c(0xfff0b0), 0, y + h, c(0x8a5a10)));
            g.fill(new RoundRectangle2D.Double(cx, y, 26, h, 10, 10));
            g.setColor(c(0x6a4008));
            for (int k = 1; k < 4; k++) g.drawLine(cx + k * 6 + 1, y + 3, cx + k * 6 + 1, y + h - 3);
            g.setColor(ca(0xffffff, 120));
            g.drawLine(cx + 3, y + 2, cx + 22, y + 2);
        }
    }

    static void tassel(Graphics2D g, int x, int y) {
        g.setColor(c(0xd8a640)); g.fillOval(x - 4, y, 8, 8);
        g.setColor(c(0xb0201a)); g.fillRect(x - 1, y + 8, 2, 8);
        for (int k = -4; k <= 4; k++) {
            g.setColor(k % 2 == 0 ? c(0xc8281e) : c(0x8a1410));
            g.drawLine(x + k / 2, y + 16, x + k, y + 44 + Math.abs(k) % 3 * 2);
        }
        g.setColor(c(0xd8a640)); g.fillRect(x - 4, y + 15, 8, 3);
    }

    static void drop(Graphics2D g, int x, int y, boolean filled) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y - 7);
        p.curveTo(x + 6, y - 1, x + 6, y + 5, x, y + 5);
        p.curveTo(x - 6, y + 5, x - 6, y - 1, x, y - 7);
        g.setColor(filled ? c(0xb81e18) : ca(0x8a6a3a, 70));
        g.fill(p);
        if (filled) { g.setColor(ca(0xffffff, 90)); g.fillOval(x - 3, y - 1, 2, 3); }
    }

    static void ingot(Graphics2D g, int x, int y) {
        g.setPaint(new GradientPaint(0, y, c(0xffe890), 0, y + 14, c(0xb07a14)));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + 4); p.quadTo(x + 11, y + 16, x + 22, y + 4); p.lineTo(x + 18, y + 10); p.quadTo(x + 11, y + 14, x + 4, y + 10); p.closePath();
        g.fill(new Ellipse2D.Double(x + 4, y + 6, 14, 8));
        g.fill(p);
        g.setPaint(new GradientPaint(0, y - 2, c(0xfff6c8), 0, y + 8, c(0xd8a030)));
        g.fill(new Ellipse2D.Double(x + 7, y, 8, 8));
    }

    static void roundSeal(Graphics2D g, int x, int y, int rgb) {
        int r = 15;
        g.setColor(c(rgb));
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setColor(c(0xf6e2c0));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(x - r + 3, y - r + 3, 2 * r - 6, 2 * r - 6);
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("KaiTi", Font.BOLD, 17));
        drawCentered(g, "購", x, y + 6);
    }

    static void brush(Graphics2D g, int x0, int x1, int y) {
        for (int x = x0; x < x1; x++) {
            double t = (x - x0) / (double) (x1 - x0);
            double w = 2.4 * (1 - t * 0.7);
            if (t > 0.75 && R.nextDouble() < (t - 0.75) * 2.5) continue;
            g.setColor(ca(0x5a3a1a, (int) (110 * (1 - t * 0.5))));
            g.fill(new Rectangle2D.Double(x, y - w / 2 + Math.sin(t * 9) * 0.6, 1, w));
        }
    }

    // ------------------------------------------------------------------ the guides' sprites

    static final String[][] EMBLEMS = {
        {".###.", "#####", "#####", "#####", ".#.#."},   // a fist
        {"....#", "...#.", "..#..", ".#...", "#...."},   // a staff
        {"...##", "...##", "..#..", ".#...", "#...."},   // a spear
        {"..#..", "..#..", "..#..", ".###.", "..#.."},   // a sword
        {".....", "..#..", ".###.", "##.##", "#####"},   // a mountain
        {"..#..", ".###.", "#####", ".###.", ".#.#."},   // a star
        {"..#..", ".###.", ".###.", "#####", "..#.."},   // a bell
        {"#.#.#", ".###.", "#####", ".###.", "#.#.#"},   // a sun
        {"...##", "..##.", ".####", "..##.", ".##.."},   // a bolt of lightning
        {"..#..", ".###.", "#####", ".#.#.", "..#.."},   // a broad spearhead
        {".##..", "####.", ".####", "..###", "..#.#"}};  // a raven
    static final String[] ARTS = {"long_fist", "traveller_staff", "celestial_peak_spear", "azure_cloud_sword", "mountain_shattering_fist", "seven_stars_sword", "golden_bell", "majin_fist", "thunder_saber", "overlord_spear", "raven_art"};
    static final int[] COVERS = {0xb02a22, 0x7a4a22, 0x2a5aa0, 0x2f78c8, 0x5a3a20, 0x232a5a, 0xb0561c, 0x5a1e6a, 0x4a2a7a, 0x4a1414, 0x1e1a2a};

    static void guides() throws Exception {
        for (int i = 0; i < ARTS.length; i++) {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            book(img, COVERS[i], EMBLEMS[i]);
            String name = ARTS[i] + "_guide";
            ImageIO.write(img, "png", ASSETS.resolve("textures/item/" + name + ".png").toFile());
            Files.writeString(ASSETS.resolve("models/item/" + name + ".json"),
                    "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"murimblock:item/" + name + "\"}}\n");
        }
    }

    static void px(BufferedImage img, int i, int j, int rgb) { img.setRGB(i, j, 0xff000000 | rgb); }

    /** A closed book seen a little from the side: spine and its gold bands, cover lit from the top left, pages, gold corners, the emblem embossed in gold. */
    static void book(BufferedImage img, int cover, String[] emblem) {
        int dark = mix(cover, 0x000000, 0.45), light = mix(cover, 0xffffff, 0.3), shade = mix(cover, 0x000000, 0.2), out = 0x1a0e06;
        for (int i = 1; i <= 13; i++) for (int j = 1; j <= 14; j++) {
            boolean edge = i == 1 || j == 1 || i == 13 || j == 14 || (i == 12 && j == 1) || (j == 13 && i < 3);
            int col;
            if (edge) col = (i == 13 && j == 1) ? -1 : out;
            else if (i == 12 || j == 13) col = (i == 12 && j == 13) ? 0xb8a070 : ((i == 12 ? j : i) % 2 == 0 ? 0xf0e2bc : 0xd2bc8a);
            else if (i <= 3) col = (j == 4 || j == 10) ? 0xe8b844 : (i == 2 ? dark : shade);
            else if ((i == 4 || i == 11) && (j == 2 || j == 12) || i == 11 && j == 3 || i == 10 && j == 2) col = 0xe8b844;
            else if (j == 2 || i == 4) col = light;
            else if (i == 11 || j == 12) col = shade;
            else col = cover;
            if (col >= 0) px(img, i, j, col);
        }
        for (int a = 0; a < 5; a++) for (int b = 0; b < 5; b++) if (emblem[b].charAt(a) == '#'
                && (a == 4 || emblem[b].charAt(a + 1) != '#')) px(img, 6 + a, 5 + b, mix(cover, 0x000000, 0.35));
        for (int a = 0; a < 5; a++) for (int b = 0; b < 5; b++) if (emblem[b].charAt(a) == '#') px(img, 5 + a, 5 + b, b == 0 || a == 0 ? 0xfff0a8 : 0xe8b844);
    }
}
