import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the Bandit Dao's 16x16 item texture, in the vanilla swords' way (pommel at the bottom left, point at the top
 * right, a dark outline): a broad single-edged blade that widens toward a clipped point and curves a little, its spine
 * darker and its edge bright; a round brass guard; a grip wrapped in red cloth; a brass pommel with a short red tassel.
 * Usage: java scripts/worldgen/BanditDao.java src/main/resources/assets/murimblock/textures/item [preview.png]
 */
public class BanditDao {
    static final int W = 16;

    static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }

    static final int OUTLINE = rgb(36, 33, 34), SPINE = rgb(112, 120, 128), FLAT = rgb(176, 184, 191), LIGHT = rgb(214, 220, 225),
            EDGE = rgb(246, 248, 250), BRASS = rgb(186, 146, 64), BRASS_DARK = rgb(124, 92, 34), WRAP = rgb(150, 46, 36),
            WRAP_DARK = rgb(98, 28, 22), TASSEL = rgb(196, 56, 44), TASSEL_DARK = rgb(126, 32, 24);

    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        // The blade's axis: from the pommel (bottom left) toward the point (top right).
        double ox = 3.0, oy = 13.0, ax = Math.sqrt(0.5), ay = -Math.sqrt(0.5);
        // Across the axis, + toward the bottom right: the edge's side.
        double nx = Math.sqrt(0.5), ny = Math.sqrt(0.5);
        double tip = 15.6;
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            double px = x + 0.5 - ox, py = y + 0.5 - oy;
            double u = px * ax + py * ay, v = px * nx + py * ny;
            int c = 0;
            if (u >= -1.1 && u <= 0.9 && Math.abs(v) <= 1.1) c = (u < 0 ? BRASS_DARK : BRASS);                 // pommel
            else if (u > 0.9 && u <= 3.9 && Math.abs(v) <= 0.85) c = ((int) Math.floor(u * 1.4)) % 2 == 0 ? WRAP : WRAP_DARK; // grip
            else if (u > 3.9 && u <= 5.3 && Math.abs(v) <= 2.5) c = Math.abs(v) > 1.6 || u > 4.9 ? BRASS_DARK : BRASS;    // guard
            else if (u > 5.3 && u <= tip) {
                double t = (u - 5.3) / (tip - 5.3);
                // The spine runs nearly straight, bending a little toward the edge near the point; the edge swells out.
                double spine = -1.05 + 0.5 * t * t;
                double edge = 1.0 + 1.75 * Math.sin(Math.min(1, t * 1.1) * Math.PI * 0.6);
                // The clipped point: the edge side cut back toward the spine over the last stretch.
                edge = Math.min(edge, spine + (tip - u) * 1.6);
                if (v >= spine && v <= edge) {
                    double across = (v - spine) / Math.max(0.01, edge - spine);
                    c = across < 0.25 ? SPINE : across > 0.78 ? EDGE : across > 0.5 ? LIGHT : FLAT;
                }
            }
            if (c != 0) img.setRGB(x, y, c);
        }
        // The tassel hanging from the pommel.
        img.setRGB(1, 14, TASSEL);
        img.setRGB(1, 15, TASSEL_DARK);
        img.setRGB(0, 15, TASSEL);
        img.setRGB(2, 15, TASSEL_DARK);
        // A dark outline round everything, as vanilla items have.
        BufferedImage out = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < W; y++) for (int x = 0; x < W; x++) {
            int c = img.getRGB(x, y);
            if ((c >>> 24) != 0) { out.setRGB(x, y, c); continue; }
            boolean near = false;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int xx = x + d[0], yy = y + d[1];
                if (xx >= 0 && yy >= 0 && xx < W && yy < W && (img.getRGB(xx, yy) >>> 24) != 0 && !isTassel(img.getRGB(xx, yy))) near = true;
            }
            if (near) out.setRGB(x, y, OUTLINE);
        }
        File dir = new File(args[0]);
        dir.mkdirs();
        ImageIO.write(out, "png", new File(dir, "bandit_dao.png"));
        if (args.length > 1) {
            int s = 24;
            BufferedImage big = new BufferedImage(W * s, W * s, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < W * s; y++) for (int x = 0; x < W * s; x++) {
                int c = out.getRGB(x / s, y / s);
                big.setRGB(x, y, (c >>> 24) != 0 ? c : (((x / s + y / s) % 2 == 0) ? 0xFFDAD6CC : 0xFFE8E4DA));
            }
            ImageIO.write(big, "png", new File(args[1]));
        }
    }

    static boolean isTassel(int c) {
        return c == TASSEL || c == TASSEL_DARK;
    }
}
