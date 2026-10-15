import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Draws the 16x16 block textures of the Murim biomes: original pixel art, made the way Minecraft's own textures are
 * made, without using any of them:
 * <ul>
 *   <li>every material has a short hand-picked palette (5 to 7 shades), its shadows cooler and its lights warmer;</li>
 *   <li>pixels come in clusters (chunks of rock, pebbles, bark plates, leaf tufts), never as one-pixel noise;</li>
 *   <li>shapes are lit from the top left: each material is a small height field whose slopes facing the light are a
 *       shade lighter and the others a shade darker, which gives the chunky relief of Minecraft's blocks;</li>
 *   <li>leaves are cut out (tufts with gaps the sky shows through), plants are clean silhouettes;</li>
 *   <li>everything tiles, and every texture is seeded by its name (re-running gives the same pixels).</li>
 * </ul>
 *   java scripts/worldgen/NatureTextures.java src/main/resources/assets/murimblock/textures/block
 */
public class NatureTextures {
    static final int S = 16;
    static File out;

    // ------------------------------------------------------------------ image

    static final class Img {
        final BufferedImage im = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        final Random r;
        final long seed;

        Img(String name) {
            seed = name.hashCode() * 31L + 7;
            r = new Random(seed);
        }

        void set(int x, int y, int c) {
            im.setRGB(Math.floorMod(x, S), Math.floorMod(y, S), c);
        }

        /** Inside the tile only (no wrap): plants and saplings. */
        void put(int x, int y, int c) {
            if (x >= 0 && y >= 0 && x < S && y < S) im.setRGB(x, y, c);
        }

        int get(int x, int y) {
            return im.getRGB(Math.floorMod(x, S), Math.floorMod(y, S));
        }

        boolean empty(int x, int y) {
            return x < 0 || y < 0 || x >= S || y >= S || (im.getRGB(x, y) >>> 24) == 0;
        }

        void save(String name) throws Exception {
            ImageIO.write(im, "png", new File(out, name + ".png"));
            System.out.println("  " + name);
        }
    }

    static int rgb(int hex) {
        return 0xFF000000 | hex;
    }

    static int[] ramp(int... hex) {
        int[] r = new int[hex.length];
        for (int i = 0; i < hex.length; i++) r[i] = rgb(hex[i]);
        return r;
    }

    static int at(int[] ramp, double i) {
        return ramp[Math.max(0, Math.min(ramp.length - 1, (int) Math.round(i)))];
    }

    static int mix(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------------ noise (all tileable on 16)

    static double h(long seed, int x, int y) {
        long v = seed * 6364136223846793005L + x * 1442695040888963407L + y * 2862933555777941757L;
        v ^= v >>> 29;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 32;
        return (v >>> 11) / (double) (1L << 53);
    }

    /** Value noise with cells cx by cy (both dividing 16), smooth, in [0, 1). */
    static double noise(long seed, double x, double y, int cx, int cy) {
        double fx = x / cx, fy = y / cy;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy), nx = S / cx, ny = S / cy;
        double tx = fx - x0, ty = fy - y0;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        double a = h(seed, Math.floorMod(x0, nx), Math.floorMod(y0, ny)), b = h(seed, Math.floorMod(x0 + 1, nx), Math.floorMod(y0, ny));
        double c = h(seed, Math.floorMod(x0, nx), Math.floorMod(y0 + 1, ny)), d = h(seed, Math.floorMod(x0 + 1, nx), Math.floorMod(y0 + 1, ny));
        return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty;
    }

    /** Three octaves: broad shapes, then clusters, then a little grain. */
    static double fbm(long seed, double x, double y) {
        return noise(seed, x, y, 8, 8) * 0.55 + noise(seed + 1, x, y, 4, 4) * 0.3 + noise(seed + 2, x, y, 2, 2) * 0.15;
    }

    /** Jittered cells (Voronoi) on a g by g grid, tileable: distance to the nearest and second nearest point, and its id. */
    static final class Cells {
        final int g;
        final double[] px, py;

        Cells(long seed, int g, double jitter) {
            this.g = g;
            px = new double[g * g];
            py = new double[g * g];
            double cell = (double) S / g;
            for (int j = 0; j < g; j++) for (int i = 0; i < g; i++) {
                px[j * g + i] = (i + 0.5 + (h(seed, i, j) - 0.5) * jitter) * cell;
                py[j * g + i] = (j + 0.5 + (h(seed + 9, i, j) - 0.5) * jitter) * cell;
            }
        }

        /** {d1, d2, id} at (x, y), distances measured with x and y scaled (sx, sy: anisotropy). */
        double[] at(double x, double y, double sx, double sy) {
            double d1 = 1e9, d2 = 1e9;
            int id = 0;
            for (int k = 0; k < px.length; k++) {
                double dx = Math.abs(x - px[k]), dy = Math.abs(y - py[k]);
                dx = Math.min(dx, S - dx) * sx;
                dy = Math.min(dy, S - dy) * sy;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d < d1) {
                    d2 = d1;
                    d1 = d;
                    id = k;
                } else if (d < d2) d2 = d;
            }
            return new double[]{d1, d2, id};
        }
    }

    /**
     * Paints a height field with a ramp: the level sets the shade, and the slope toward the top-left light adds a
     * shade (lit) or takes one (shadow). h in about [0, 1].
     */
    static void paint(Img img, double[][] h, int[] ramp, double base, double spread, double relief) {
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double lit = h[y][x] - h[Math.floorMod(y - 1, S)][Math.floorMod(x - 1, S)];
            img.set(x, y, at(ramp, base + (h[y][x] - 0.5) * spread + lit * relief));
        }
    }

    // ------------------------------------------------------------------ rocks and soils

    static final int[] LIMESTONE = ramp(0x55595B, 0x6B6F70, 0x828686, 0x999C9B, 0xAFB1AE, 0xC4C5C0, 0xD7D7D0);
    static final int[] MOSS = ramp(0x2C4521, 0x3A5A28, 0x4B7031, 0x5F873B, 0x76A047);
    static final int[] GRANITE = ramp(0x77726B, 0x8F8A82, 0xA6A097, 0xBAB4A9, 0xCBC6BA, 0xDCD8CD);
    static final int[] SANDSTONE = ramp(0x6A5741, 0x826C4F, 0x9A8460, 0xB09A72, 0xC3AE86, 0xD4C19B);
    static final int[] SANDSTONE_RUST = ramp(0x6B3E26, 0x844D30, 0x9C5F3C, 0xB07350);
    static final int[] LOESS = ramp(0x7A5F35, 0x937544, 0xAA8B54, 0xBE9F66, 0xCFB27A, 0xDCC28E);
    static final int[] RED_LOESS = ramp(0x5A2C1B, 0x713923, 0x89482D, 0x9F5838, 0xB26B47, 0xC17E59);
    static final int[] GOBI_SAND = ramp(0x8E7656, 0xA68C67, 0xB9A07A, 0xC9B28C);
    static final int[] KUNLUN = ramp(0x272A2E, 0x34383D, 0x42474D, 0x51575D, 0x61686F, 0x737A82);
    static final int[] JADE = ramp(0xA19D86, 0xB6B29B, 0xC9C5AF, 0xD9D5C1, 0xE6E3D1, 0xF1EFE2);
    static final int[] JADE_SKIN = ramp(0x7C4A2A, 0x96603A, 0xAE774C, 0xC28E62);

    /** Rock in chunks: cells with darker joints and lit rims, a little grain inside. */
    static double[][] chunks(long seed, int g, double jitter, double joint, double sx, double sy) {
        Cells cells = new Cells(seed, g, jitter);
        double[][] f = new double[S][S];
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double[] c = cells.at(x + 0.5, y + 0.5, sx, sy);
            double edge = Math.min(1, (c[1] - c[0]) / joint);
            double tone = h(seed + 3, (int) c[2], 0) * 0.35;
            f[y][x] = 0.25 + edge * 0.45 + tone + (noise(seed + 4, x, y, 2, 2) - 0.5) * 0.12;
        }
        return f;
    }

    static void stones() throws Exception {
        // Karst limestone (Guilin): pale cool grey in weathered chunks, a few rain streaks and pits.
        Img lime = new Img("karst_limestone");
        double[][] lf = chunks(lime.seed, 4, 0.9, 1.6, 1, 1);
        paint(lime, lf, LIMESTONE, 2.8, 2.8, 3.4);
        for (int k = 0; k < 2; k++) { // rain streaks: short vertical darker runs
            int x = lime.r.nextInt(S), y0 = lime.r.nextInt(S), len = 4 + lime.r.nextInt(5);
            for (int y = 0; y < len; y++) lime.set(x, y0 + y, mix(lime.get(x, y0 + y), LIMESTONE[1], 0.45));
        }
        for (int k = 0; k < 4; k++) { // solution pits: a dark pixel with a lit lip above-left
            int x = lime.r.nextInt(S), y = lime.r.nextInt(S);
            lime.set(x, y, LIMESTONE[1]);
            lime.set(x - 1, y - 1, LIMESTONE[5]);
        }
        lime.save("karst_limestone");

        // Mossy: the same rock, moss cushions over the top and the joints, raised and lit.
        Img mossy = new Img("mossy_karst_limestone");
        double[][] mf = chunks(lime.seed, 4, 0.9, 1.6, 1, 1);
        paint(mossy, mf, LIMESTONE, 2.8, 2.8, 3.4);
        double[][] moss = new double[S][S];
        boolean[][] has = new boolean[S][S];
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            // cushions over the top of the block and along the joints (where the rock is low)
            double m = noise(mossy.seed + 20, x, y, 4, 4) * 0.55 + noise(mossy.seed + 21, x, y, 2, 2) * 0.2 + (1 - mf[y][x]) * 0.35
                    + (y < 5 ? 0.12 : 0);
            has[y][x] = m > 0.6;
            moss[y][x] = has[y][x] ? 0.5 + (m - 0.58) * 2 + (noise(mossy.seed + 22, x, y, 2, 2) - 0.5) * 0.3 : 0;
        }
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            if (!has[y][x]) continue;
            double lit = moss[y][x] - moss[Math.floorMod(y - 1, S)][Math.floorMod(x - 1, S)];
            mossy.set(x, y, at(MOSS, 2 + (moss[y][x] - 0.6) * 3 + lit * 3));
        }
        mossy.save("mossy_karst_limestone");

        // Huangshan granite: pale warm grey in large blocks, crystals in small clusters (dark mica, pink feldspar, quartz).
        Img gran = new Img("pale_granite");
        double[][] gf = chunks(gran.seed, 3, 0.8, 1.2, 1, 1);
        paint(gran, gf, GRANITE, 2.6, 2.2, 2.8);
        int[] crystals = {rgb(0x3F3C39), rgb(0x55514C), rgb(0xD4BBA7), rgb(0xC6A994), rgb(0xEDE9DF)};
        for (int k = 0; k < 9; k++) {
            int x = gran.r.nextInt(S), y = gran.r.nextInt(S), c = crystals[gran.r.nextInt(crystals.length)];
            gran.set(x, y, c);
            if (gran.r.nextInt(3) > 0) gran.set(x + (gran.r.nextBoolean() ? 1 : 0), y + 1, mix(c, gran.get(x, y + 1), 0.4));
        }
        gran.save("pale_granite");

        // Wulingyuan sandstone (pillar sides): thin wavy laminae, a rust band, ledges lit along their top.
        // Wide flat slabs (cells stretched across), so the bedding reads without a stripe repeating every block.
        Img sand = new Img("pillar_sandstone");
        double[][] sf = chunks(sand.seed, 4, 0.95, 1.3, 0.55, 1.6);
        paint(sand, sf, SANDSTONE, 2.8, 2.4, 2.8);
        for (int x = 0; x < S; x++) { // a thin iron stain along one bed, broken here and there
            if (noise(sand.seed + 8, x, 0, 8, 16) < 0.62) continue;
            int yb = 10 + (int) Math.round(noise(sand.seed + 5, x, 0, 8, 16) * 1.4);
            sand.set(x, yb, mix(sand.get(x, yb), at(SANDSTONE_RUST, 2 + noise(sand.seed + 6, x, 0, 2, 16)), 0.6));
        }
        sand.save("pillar_sandstone");
        Img sandTop = new Img("pillar_sandstone_top");
        double[][] tf = chunks(sandTop.seed, 4, 1.0, 1.4, 1, 1);
        paint(sandTop, tf, SANDSTONE, 3.4, 2.4, 2.8);
        sandTop.save("pillar_sandstone_top");

        // Loess: soft buff silt, fine clusters, vertical hairline joints (loess splits vertically) and pale nodules.
        Img loess = new Img("loess");
        double[][] lo = new double[S][S];
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            lo[y][x] = noise(loess.seed, x, y, 8, 8) * 0.3 + noise(loess.seed + 1, x, y, 4, 4) * 0.45 + noise(loess.seed + 2, x, y, 2, 2) * 0.25;
        }
        paint(loess, lo, LOESS, 3.2, 4.4, 4.2);
        for (int k = 0; k < 3; k++) {
            int x = loess.r.nextInt(S), y0 = loess.r.nextInt(S), len = 3 + loess.r.nextInt(4);
            for (int y = 0; y < len; y++) loess.set(x, y0 + y, LOESS[1]);
            loess.set(x + 1, y0, LOESS[4]);
        }
        for (int k = 0; k < 3; k++) {
            int x = loess.r.nextInt(S), y = loess.r.nextInt(S);
            loess.set(x, y, LOESS[5]);
            loess.set(x + 1, y + 1, LOESS[2]);
        }
        loess.save("loess");

        // Red loess (fossil soil): rust-brown clay with dark root traces and pale lime nodules.
        Img red = new Img("red_loess");
        double[][] ro = chunks(red.seed, 4, 1.0, 2.0, 1, 1);
        paint(red, ro, RED_LOESS, 3.0, 3.2, 3.2);
        for (int k = 0; k < 3; k++) {
            int x = red.r.nextInt(S), y = red.r.nextInt(S), len = 3 + red.r.nextInt(4);
            for (int i = 0; i < len; i++) {
                red.set(x, y + i, RED_LOESS[0]);
                if (red.r.nextInt(3) == 0) x += red.r.nextBoolean() ? 1 : -1;
            }
        }
        int nod = rgb(0xD8C3A2), nodShadow = rgb(0xA98F70);
        for (int k = 0; k < 4; k++) {
            int x = red.r.nextInt(S), y = red.r.nextInt(S);
            red.set(x, y, nod);
            red.set(x + 1, y + 1, nodShadow);
        }
        red.save("red_loess");

        // Gobi gravel: pebbles of every colour of the Hexi corridor (black varnish, rust, grey, tan), each one domed and
        // lit, packed in pale sand.
        Img gobi = new Img("gobi_gravel");
        Cells pebbles = new Cells(gobi.seed, 5, 0.95);
        // Mostly grey and black (desert varnish), a few rusty and tan stones.
        int[][] pebbleRamps = {
                {0x1F1D1B, 0x302D2A, 0x46423D, 0x5C5751}, {0x4D4A46, 0x66625C, 0x7F7A73, 0x9A948B},
                {0x2B2826, 0x3E3A36, 0x55504A, 0x6C665F}, {0x5A5650, 0x726D66, 0x8B857C, 0xA59E94},
                {0x5C3A29, 0x784C35, 0x926045, 0xA67352}, {0x8A7556, 0xA48D6A, 0xBBA47E, 0xCDB892}};
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double[] c = pebbles.at(x + 0.5, y + 0.5, 1, 1);
            double gap = c[1] - c[0];
            int id = (int) c[2];
            if (gap < 0.45 || h(gobi.seed + 4, id, 1) < 0.08) { // sand between the stones
                gobi.set(x, y, at(GOBI_SAND, 1.2 + (noise(gobi.seed + 5, x, y, 2, 2) - 0.5) * 2.5));
                continue;
            }
            int[] pr = new int[4];
            int[] src = pebbleRamps[(int) (h(gobi.seed + 6, id, 2) * pebbleRamps.length)];
            for (int i = 0; i < 4; i++) pr[i] = rgb(src[i]);
            // A pebble: a dark rim on its bottom-right edge, a lit rim on its top-left edge, its body in between.
            double dx = x + 0.5 - pebbles.px[id], dy = y + 0.5 - pebbles.py[id];
            dx -= Math.round(dx / S) * S;
            dy -= Math.round(dy / S) * S;
            boolean rim = gap < 1.25;
            double v = rim ? (dx + dy > 0 ? 0 : 3) : 1.6 - (dx + dy) * 0.25;
            gobi.set(x, y, at(pr, v));
        }
        gobi.save("gobi_gravel");

        // Kunlun schist: dark blue-grey rock split into thin foliated slabs (cells stretched across), a few mica flakes
        // catching the light, and one pale quartz stringer wandering through the folds.
        Img kun = new Img("kunlun_rock");
        double[][] kf = chunks(kun.seed, 4, 0.95, 1.1, 0.45, 2.0);
        paint(kun, kf, KUNLUN, 2.5, 2.6, 3.2);
        int mica = rgb(0xA4ADB6), micaShadow = rgb(0x6F7780);
        for (int k = 0; k < 6; k++) {
            int x = kun.r.nextInt(S), y = kun.r.nextInt(S);
            kun.set(x, y, mica);
            if (kun.r.nextBoolean()) kun.set(x + 1, y, micaShadow);
        }
        int y0 = 3 + kun.r.nextInt(10);
        for (int x = 0; x < S; x++) {
            if (noise(kun.seed + 7, x, 0, 8, 16) < 0.35) continue; // the stringer pinches out here and there
            int y = y0 + (int) Math.round((noise(kun.seed + 8, x, 0, 4, 16) - 0.5) * 3);
            kun.set(x, y, mix(kun.get(x, y), rgb(0xC9CCC8), 0.75));
        }
        kun.save("kunlun_rock");

        // Raw white jade of Hotan: rounded river stones of waxy cream nephrite ("mutton fat"), each domed and softly
        // lit, a russet skin where the river stained it, fine seams between them.
        Img jade = new Img("raw_white_jade");
        Cells stones = new Cells(jade.seed, 3, 0.9);
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double[] c = stones.at(x + 0.5, y + 0.5, 1, 1);
            double gap = c[1] - c[0];
            int id = (int) c[2];
            if (gap < 0.5) { // the seam between two stones
                jade.set(x, y, JADE[1]);
                continue;
            }
            double dx = x + 0.5 - stones.px[id], dy = y + 0.5 - stones.py[id];
            dx -= Math.round(dx / S) * S;
            dy -= Math.round(dy / S) * S;
            double r = Math.hypot(dx, dy);
            // A domed stone lit from the top left, waxy (soft falloff, a bright sheen near its top-left shoulder).
            double v = 4.0 - r * 0.32 - (dx + dy) * 0.14 + (noise(jade.seed + 3, x, y, 2, 2) - 0.5) * 0.6;
            boolean skin = gap < 1.1 && noise(jade.seed + 5 + id, x, y, 4, 4) > 0.76;
            jade.set(x, y, skin ? at(JADE_SKIN, v - 1.4) : at(JADE, v));
        }
        jade.save("raw_white_jade");
    }

    // ------------------------------------------------------------------ wood

    /**
     * A wood: its bark (dark to light), its cut wood (dark to light), the style of its bark, plate width and height
     * (pixels) for plated barks.
     */
    record Wood(String name, int[] bark, int[] wood, String style, double plateW, double plateH) { }

    static final Wood[] WOODS = {
            // Sweet osmanthus: smooth pale grey bark, light speckles; pale yellowish wood.
            new Wood("osmanthus", ramp(0x3E3B37, 0x55514B, 0x6C6760, 0x857F76, 0x9C968B), ramp(0x8E6C47, 0xA8865A, 0xBE9C6C, 0xD0B07F, 0xDEC194), "smooth", 0, 0),
            // Banyan: smooth silvery bark with fine vertical folds; light brown wood.
            new Wood("banyan", ramp(0x4B4944, 0x625F58, 0x7A766E, 0x928E84, 0xA9A59A), ramp(0x806246, 0x997858, 0xAE8D6A, 0xC2A27E, 0xD2B48F), "folded", 0, 0),
            // Huangshan pine: red-brown bark in irregular plates with dark cracks; warm orange wood.
            new Wood("huangshan_pine", ramp(0x2E1A12, 0x48291B, 0x643925, 0x7F4B31, 0x9A5F40), ramp(0x9A6038, 0xB47446, 0xC88A57, 0xD9A069, 0xE6B47D), "plated", 4.5, 2.6),
            // Jujube: dark, deeply furrowed, flaking bark; red-brown wood.
            new Wood("jujube", ramp(0x231A15, 0x34271F, 0x47362B, 0x5B4738, 0x705A47), ramp(0x7A3A23, 0x934A2E, 0xA85B39, 0xBC6E48, 0xCC8259), "furrowed", 0, 0),
            // Dove tree: grey-brown bark flaking in small scales; cream wood.
            new Wood("dove_tree", ramp(0x3C3229, 0x524539, 0x695A4B, 0x80705F, 0x968574), ramp(0xA48F6C, 0xB9A47F, 0xCBB792, 0xDAC8A4, 0xE6D7B6), "plated", 3.2, 2.0),
            // Weeping willow: grey-brown bark with long interlacing fissures; pale wood.
            new Wood("willow", ramp(0x2C2620, 0x40372E, 0x554A3E, 0x6B5E4F, 0x817362), ramp(0x9C875F, 0xB29C72, 0xC5B086, 0xD5C29A, 0xE1D1AD), "furrowed", 0, 0),
            // Peach: glossy red-brown bark with horizontal lenticels; pinkish wood.
            new Wood("peach", ramp(0x3A1F1C, 0x522B27, 0x6B3A33, 0x844B41, 0x9C5D50), ramp(0x8F5E47, 0xA87257, 0xBC8668, 0xCD9A7C, 0xDBAF92), "lenticels", 0, 0),
            // Desert poplar (Populus euphratica): rough grey bark in deep cracks; yellow-tan wood.
            new Wood("desert_poplar", ramp(0x2F2B26, 0x45403A, 0x5C564E, 0x736C62, 0x8A8277), ramp(0x95794C, 0xAD915F, 0xC2A672, 0xD3B886, 0xE0C89A), "furrowed", 0, 0),
            // Tianshan spruce (Picea schrenkiana): grey-brown bark in thin round scales; pale, almost white wood.
            new Wood("tianshan_spruce", ramp(0x29231F, 0x3B332C, 0x4F463C, 0x645A4D, 0x7A6E5F), ramp(0xA68F6A, 0xBBA680, 0xCDBA96, 0xDCCCAA, 0xE8DCBD), "plated", 3.0, 1.8),
            // Camphor: grey-brown bark in long shallow fissures; fragrant reddish-tan wood.
            new Wood("camphor", ramp(0x2C2520, 0x40362F, 0x564A40, 0x6D5F53, 0x847466), ramp(0x965F40, 0xAE7452, 0xC28865, 0xD39D7A, 0xE1B391), "furrowed", 0, 0),
    };

    static void bark(Wood w) throws Exception {
        Img i = new Img(w.name + "_log");
        double[][] f = new double[S][S];
        switch (w.style) {
            case "plated" -> { // irregular plates (pine, dove tree): cells stretched across, dark cracks between
                Cells c = new Cells(i.seed, 4, 0.9);
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    double[] d = c.at(x + 0.5, y + 0.5, 4.0 / w.plateW, 4.0 / w.plateH);
                    double edge = Math.min(1, (d[1] - d[0]) / 1.1);
                    f[y][x] = 0.15 + edge * 0.55 + h(i.seed, (int) d[2], 5) * 0.3;
                }
                paint(i, f, w.bark, 2.4, 3.6, 4.0);
            }
            case "furrowed" -> { // long vertical ridges that split and join (willow, jujube, poplar)
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    double wob = (noise(i.seed + 1, x, y, 16, 4) - 0.5) * 3.0;
                    double ridge = Math.abs(Math.sin((x + wob) * Math.PI / 4.0));
                    f[y][x] = ridge * 0.75 + noise(i.seed + 2, x, y, 2, 8) * 0.25;
                }
                paint(i, f, w.bark, 2.2, 4.2, 3.0);
            }
            case "folded" -> { // smooth silvery bark with soft vertical folds (banyan)
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    double fold = Math.sin((x + (noise(i.seed + 1, x, y, 16, 8) - 0.5) * 4) * Math.PI / 5.3);
                    f[y][x] = 0.5 + fold * 0.25 + (noise(i.seed + 2, x, y, 4, 8) - 0.5) * 0.3;
                }
                paint(i, f, w.bark, 2.4, 3.0, 2.6);
            }
            default -> { // smooth (osmanthus) and lenticels (peach): soft mottling, low relief
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    f[y][x] = noise(i.seed + 1, x, y, 4, 8) * 0.6 + noise(i.seed + 2, x, y, 2, 4) * 0.4;
                }
                paint(i, f, w.bark, 2.3, 3.0, 2.0);
            }
        }
        if (w.style.equals("smooth")) { // pale speckles
            for (int k = 0; k < 6; k++) i.set(i.r.nextInt(S), i.r.nextInt(S), w.bark[4]);
        }
        if (w.style.equals("lenticels")) { // horizontal lenticels: short pale dashes with a dark lip under
            for (int k = 0; k < 7; k++) {
                int x = i.r.nextInt(S), y = i.r.nextInt(S), len = 2 + i.r.nextInt(2);
                for (int d = 0; d < len; d++) {
                    i.set(x + d, y, w.bark[4]);
                    i.set(x + d, y + 1, w.bark[0]);
                }
            }
        }
        i.save(w.name + "_log");
    }

    /** End grain: a bark rim, growth rings bent by noise, a darker heart. */
    static void logTop(Wood w) throws Exception {
        Img i = new Img(w.name + "_log_top");
        double cx = 7.5 + (i.r.nextDouble() - 0.5), cy = 7.5 + (i.r.nextDouble() - 0.5);
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            boolean rim = x == 0 || y == 0 || x == S - 1 || y == S - 1;
            if (rim) {
                i.set(x, y, at(w.bark, (x + y) % 3 == 0 ? 1 : 2 + (noise(i.seed, x, y, 2, 2) - 0.5) * 2));
                continue;
            }
            double d = Math.hypot(x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5) + (noise(i.seed + 1, x, y, 4, 4) - 0.5) * 1.6;
            double ring = d / 1.75;
            double frac = ring - Math.floor(ring);
            int base = frac < 0.28 ? 1 : frac < 0.55 ? 2 : 3;
            if (d > 6.2) base += 1; // sapwood lighter
            if (d < 1.2) base = 0; // heart
            i.set(x, y, at(w.wood, base));
        }
        // the inner edge of the bark
        for (int k = 1; k < S - 1; k++) {
            if (i.r.nextInt(3) == 0) i.set(k, 1, at(w.bark, 3));
            if (i.r.nextInt(3) == 0) i.set(1, k, at(w.bark, 3));
        }
        i.save(w.name + "_log_top");
    }

    /** Four boards, 4 pixels each: a dark seam under each, lit top edge, staggered end joints, grain along. */
    static void planks(Wood w) throws Exception {
        Img i = new Img(w.name + "_planks");
        for (int b = 0; b < 4; b++) {
            double tone = 2.1 + (h(i.seed, b, 0) - 0.5) * 0.9;
            int joint = (int) (h(i.seed, b, 1) * S);
            boolean hasJoint = b % 2 == 0 || h(i.seed, b, 2) < 0.5;
            for (int dy = 0; dy < 4; dy++) {
                int y = b * 4 + dy;
                for (int x = 0; x < S; x++) {
                    double grain = (noise(i.seed + 10 + b, x, y, 8, 1) - 0.5) * 1.6 + (noise(i.seed + 20 + b, x, y, 4, 1) - 0.5) * 0.8;
                    double v = tone + grain;
                    if (dy == 0) v += 0.6;  // lit top edge
                    if (dy == 3) v = 0.4;   // seam
                    i.set(x, y, at(w.wood, v));
                }
                if (hasJoint && dy < 3) {
                    i.set(joint, y, at(w.wood, 0.4));
                    i.set(joint + 1, y, at(w.wood, tone + 0.8));
                }
            }
        }
        i.save(w.name + "_planks");
    }

    // ------------------------------------------------------------------ leaves

    /**
     * Leaves the Minecraft way: tufts of leaves set on a jittered grid, each lit from the top left and shadowed at the
     * bottom right, with gaps between them (the block is cut out: sky and branches show through).
     */
    static Img leafBase(String name, int[] ramp, int spacing, double coverSkip, String shape) {
        Img i = new Img(name);
        double[][] depth = new double[S][S];
        boolean[][] filled = new boolean[S][S];
        int n = S / spacing;
        // Two passes: back tufts (darker), then front tufts (lighter) partly over them.
        for (int pass = 0; pass < 2; pass++) {
            for (int gy = 0; gy < n; gy++) for (int gx = 0; gx < n; gx++) {
                if (h(i.seed + pass, gx, gy) < coverSkip) continue;
                int cx = gx * spacing + (int) (h(i.seed + 30 + pass, gx, gy) * spacing) + pass * spacing / 2;
                int cy = gy * spacing + (int) (h(i.seed + 40 + pass, gx, gy) * spacing) + pass * spacing / 2;
                stamp(i, depth, filled, cx, cy, shape, pass, h(i.seed + 50 + pass, gx, gy));
            }
        }
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            if (!filled[y][x]) continue;
            i.set(x, y, at(ramp, depth[y][x]));
        }
        return i;
    }

    /** One tuft: its pixels and their shade (0 dark .. 4 light), lit top-left, dark bottom-right. */
    static void stamp(Img i, double[][] depth, boolean[][] filled, int cx, int cy, String shape, int pass, double rnd) {
        int[][] px = switch (shape) {
            // broad leaf tuft: a rounded clump of 3 to 4 leaves
            case "broad" -> new int[][]{{0, -1}, {1, -1}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {-1, 1}, {0, 1}, {1, 1}, {0, 2}};
            // small leaves (jujube, willow): a lighter, smaller tuft
            case "small" -> new int[][]{{0, -1}, {-1, 0}, {0, 0}, {1, 0}, {0, 1}, {1, 1}};
            // needles: a fan of short needle strokes
            case "needles" -> new int[][]{{-2, -1}, {-1, 0}, {0, 0}, {1, 0}, {2, -1}, {0, -1}, {0, 1}, {-1, 1}, {1, 1}, {-2, 1}, {2, 1}, {0, -2}};
            // slender leaves: a diagonal blade
            default -> new int[][]{{-2, -2}, {-1, -1}, {0, -1}, {0, 0}, {1, 0}, {1, 1}, {2, 1}, {2, 2}};
        };
        boolean flip = rnd < 0.5 && !shape.equals("broad");
        for (int[] p : px) {
            int dx = flip ? -p[0] : p[0], dy = p[1];
            int x = Math.floorMod(cx + dx, S), y = Math.floorMod(cy + dy, S);
            // shade: back tufts darker; inside a tuft, light toward the top-left
            double s = (pass == 0 ? 1.3 : 2.3) + (-dx - dy) * 0.45 + (rnd - 0.5) * 0.6;
            depth[y][x] = filled[y][x] ? Math.max(depth[y][x], s) : s;
            filled[y][x] = true;
        }
    }

    static void leaves() throws Exception {
        // Osmanthus: dense dark glossy evergreen, tiny golden flower clusters.
        Img osm = leafBase("osmanthus_leaves", ramp(0x1F4426, 0x2E5C30, 0x40763C, 0x56904C, 0x72A85F), 4, 0.05, "broad");
        flowers(osm, 6, rgb(0xF0C04A), rgb(0xC98F2A));
        osm.save("osmanthus_leaves");
        // Banyan: deep glossy green, large leaves.
        leafBase("banyan_leaves", ramp(0x1B3E1E, 0x2A5627, 0x3B7031, 0x52893F, 0x6CA150), 4, 0.0, "broad").save("banyan_leaves");
        // Huangshan pine: dark blue-green needle fans.
        leafBase("huangshan_pine_leaves", ramp(0x17332B, 0x224536, 0x2E5841, 0x3D6B4C, 0x52805E), 4, 0.1, "needles").save("huangshan_pine_leaves");
        // Jujube: small bright leaves, red dates.
        Img juj = leafBase("jujube_leaves", ramp(0x324E1B, 0x456A24, 0x5A832D, 0x729D39, 0x8CB548), 3, 0.2, "small");
        fruits(juj, 4, rgb(0xB8342A), rgb(0x7E1D18), rgb(0xE07A5E));
        juj.save("jujube_leaves");
        // Dove tree: fresh green, white bracts hanging like doves.
        Img dove = leafBase("dove_tree_leaves", ramp(0x26421F, 0x355A29, 0x477333, 0x5D8C40, 0x77A552), 4, 0.05, "broad");
        for (int k = 0; k < 4; k++) {
            int x = dove.r.nextInt(S), y = dove.r.nextInt(S);
            dove.set(x, y, rgb(0xF2F1EA));
            dove.set(x, y + 1, rgb(0xE2E1D6));
            dove.set(x + 1, y + 1, rgb(0xF2F1EA));
            dove.set(x, y + 2, rgb(0xC9C8BB));
        }
        dove.save("dove_tree_leaves");
        // Willow: light yellow-green narrow leaves in hanging strings.
        Img wil = new Img("willow_leaves");
        int[] wr = ramp(0x40612A, 0x557A33, 0x6C933F, 0x86AC4E, 0xA2C463);
        for (int s = 0; s < 16; s++) {
            int x = s + (s % 2 == 0 ? 0 : wil.r.nextInt(2)), y0 = wil.r.nextInt(S), len = 8 + wil.r.nextInt(7);
            if (s % 3 == 2) continue; // a gap now and then between strings
            for (int k = 0; k < len; k++) {
                int y = y0 + k;
                // narrow leaves along each string: alternately lit and shaded
                wil.set(x, y, at(wr, (s % 2 == 0 ? 2.5 : 1.5) + (k % 3 == 0 ? 1 : 0) + (wil.r.nextDouble() - 0.5) * 0.8));
            }
        }
        wil.save("willow_leaves");
        // Peach: masses of deep pink blossom, darker hearts, a few young leaves.
        Img peach = leafBase("peach_leaves", ramp(0x8F2F52, 0xB24C72, 0xCF6E93, 0xE596B2, 0xF5BDD0), 4, 0.0, "broad");
        for (int k = 0; k < 7; k++) {
            int x = peach.r.nextInt(S), y = peach.r.nextInt(S);
            if ((peach.get(x, y) >>> 24) == 0) continue;
            peach.set(x, y, rgb(0x7A1D3F));
        }
        for (int k = 0; k < 4; k++) {
            int x = peach.r.nextInt(S), y = peach.r.nextInt(S);
            peach.set(x, y, rgb(0x6C9446));
            peach.set(x + 1, y, rgb(0x4F7533));
        }
        peach.save("peach_leaves");
        // Desert poplar: golden autumn crown, a few orange and green leaves.
        Img pop = leafBase("desert_poplar_leaves", ramp(0x6E4E10, 0x926E16, 0xBA921F, 0xD6B034, 0xEDCC57), 3, 0.18, "small");
        for (int k = 0; k < 4; k++) pop.set(pop.r.nextInt(S), pop.r.nextInt(S), rgb(0xD47E2A));
        pop.save("desert_poplar_leaves");
        // Tianshan spruce: short stiff needles, very dark blue-green (the darkest crown of the mod), densely set.
        leafBase("tianshan_spruce_leaves", ramp(0x0D241E, 0x143428, 0x1D4432, 0x28553E, 0x386A4E), 3, 0.08, "needles").save("tianshan_spruce_leaves");
        // Camphor: broad glossy dark leaves in round tufts, a few bronze-red young leaves of the spring flush.
        Img camphor = leafBase("camphor_leaves", ramp(0x163A1D, 0x225026, 0x316732, 0x447E41, 0x5C9756), 4, 0.0, "broad");
        flowers(camphor, 5, rgb(0xA4643A), rgb(0x6E3F24));
        camphor.save("camphor_leaves");
        // Moso bamboo: slender lanceolate leaves at a slant.
        leafBase("moso_bamboo_leaves", ramp(0x253F1A, 0x345722, 0x46702B, 0x5B8A35, 0x76A445), 4, 0.05, "slender").save("moso_bamboo_leaves");
    }

    static void flowers(Img i, int count, int c, int shadow) {
        for (int k = 0; k < count; k++) {
            int x = i.r.nextInt(S), y = i.r.nextInt(S);
            if ((i.get(x, y) >>> 24) == 0) continue;
            i.set(x, y, c);
            i.set(x + 1, y + 1, shadow);
        }
    }

    static void fruits(Img i, int count, int c, int dark, int light) {
        for (int k = 0; k < count; k++) {
            int x = i.r.nextInt(S - 1), y = i.r.nextInt(S - 1);
            i.set(x, y, light);
            i.set(x + 1, y, c);
            i.set(x, y + 1, c);
            i.set(x + 1, y + 1, dark);
        }
    }

    // ------------------------------------------------------------------ hand-drawn sprites

    /**
     * A sprite drawn by hand, pixel by pixel: 16 rows of 16 characters, '.' for clear, every other letter a colour of
     * the palette given as pairs ('a', 0xRRGGBB, ...).
     */
    static void sprite(String name, String[] rows, Object... palette) throws Exception {
        java.util.Map<Character, Integer> pal = new java.util.HashMap<>();
        for (int k = 0; k < palette.length; k += 2) pal.put((Character) palette[k], rgb((Integer) palette[k + 1]));
        if (rows.length != S) throw new IllegalArgumentException(name + ": " + rows.length + " rows");
        Img i = new Img(name);
        for (int y = 0; y < S; y++) {
            if (rows[y].length() != S) throw new IllegalArgumentException(name + " row " + y + ": " + rows[y].length() + " columns");
            for (int x = 0; x < S; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') continue;
                Integer c = pal.get(ch);
                if (c == null) throw new IllegalArgumentException(name + ": no colour for '" + ch + "'");
                i.put(x, y, c);
            }
        }
        i.save(name);
    }

    static void handDrawn() throws Exception {
        // Feather grass (Stipa): a tuft of fine blades, culms fanning out, long silky awns curling at their tips.
        sprite("feather_grass", new String[]{
                "................",
                ".e...........e..",
                "..d...e..e..d...",
                "..d....d.d..d...",
                "...d...d.d.d....",
                "...c..d..c.c....",
                "....c.c..c.c....",
                "....c.c.c..c....",
                ".....cc.c.c.....",
                "..b..c.cc.c..b..",
                "...b.bccb.cb.b..",
                "...b.bbcbbcbb...",
                "....bbabbabb....",
                "....babaabab....",
                ".....aaaaaa.....",
                "....aaaaaaaa...."},
                'a', 0x5E5C34, 'b', 0x86834E, 'c', 0xB2A974, 'd', 0xD6CFA6, 'e', 0xF0ECD5);
        // Sea buckthorn: a twiggy shrub, narrow silvery leaves, orange berries packed along the stems.
        sprite("sea_buckthorn", new String[]{
                "................",
                ".....i.....h....",
                "...h.hi...ihg...",
                "..ihg.v..g.v.h..",
                "...g.qov.oqv.g..",
                "..h..ppvoopv.ih.",
                ".ihg..ovppv..hg.",
                "..g.oq.vvv.qo.g.",
                "...hpp.vv.vpp.i.",
                "..ih.o.vvvv.h...",
                "...g...wvv..ihg.",
                "..h.qo.wv.oq.g..",
                "...g.pp.wvpp.h..",
                ".......wwv......",
                ".......wwv......",
                "......wwwvv....."},
                'w', 0x3E3127, 'v', 0x5F4D3C, 'g', 0x56694F, 'h', 0x7B906F, 'i', 0xA5B79A,
                'o', 0xEE9A2C, 'p', 0xC26C1A, 'q', 0xFFD27A);
        // Saxaul: a gnarled low trunk, a broom of jointed green twigs pointing up, no leaves.
        sprite("saxaul", new String[]{
                "................",
                "...i...i..i..i..",
                "..ih..ih.hi.ih..",
                "...h.gh..hg.h...",
                "..hg.h.g.h.gh...",
                "...g..hg.g.g....",
                "..g.h..gvg.h.g..",
                "...g.g.vh.g.g...",
                "....hg.v..gh....",
                ".....v.vv.v.....",
                "....v..vu.v.....",
                ".....v.vuv......",
                "......vvu.......",
                "......wvu.......",
                ".....wwvuu......",
                "....ww.wvv.u...."},
                'w', 0x3B3027, 'v', 0x6A5A4B, 'u', 0x8B7966, 'g', 0x55693A, 'h', 0x789050, 'i', 0xA9BB76);
        // Rapeseed: three stalks of open four-petalled flowers below green buds, blue-green leaves at the foot.
        sprite("rapeseed", new String[]{
                "...z........z...",
                "..zyz......yzy..",
                "..yxy..z...xyx..",
                ".zy.yzyzy.zyy...",
                "..xyx.yxyz.yx...",
                "...gzy.y.yg.....",
                "...g.xyx..g.....",
                "...g..g...g..z..",
                "..hg..g..hgzyzy.",
                ".ih.g.g.ihg.xyx.",
                "..ih.gg.h.g..g..",
                "...h.gg.h.g..g..",
                ".....ggh..g.hg..",
                "..h..gg..ggih...",
                "...hhgg.ggh.....",
                "....hgg.gg......"},
                'x', 0xC9A818, 'y', 0xF2D52E, 'z', 0xFFEC74, 'g', 0x3F6844, 'h', 0x548557, 'i', 0x75A472);
        // Bamboo shoot: a pointed cone of brown sheaths, darker at each overlap, a green tip.
        sprite("bamboo_shoot", new String[]{
                "................",
                "........g.......",
                "........gh......",
                "........d.......",
                ".......ed.......",
                ".......edc......",
                ".......eddc.....",
                "......eddcb.....",
                "......bbbbb.....",
                ".....eddccbb....",
                ".....edddccb....",
                ".....bbbbbbbb...",
                "....eedddcccb...",
                "....edddcccbb...",
                "...eddddccccbb..",
                "..aabbbbbbbbbaa."},
                'a', 0x3A2716, 'b', 0x5A3F25, 'c', 0x7E5A34, 'd', 0xA27845, 'e', 0xC9A065, 'g', 0x4F7F31, 'h', 0x7DAA4C);
        // Arrow bamboo: thin canes with pale nodes, slender leaves in fans along them.
        sprite("arrow_bamboo", new String[]{
                "..f.........e...",
                ".eb..f....ef....",
                "..ab.eb..eb.....",
                "...ac.b.ab.c.f..",
                "....c.ab..c.eb..",
                "....d...a.cab...",
                "....c.ef..c.....",
                "...ec.b...d..f..",
                "..eb.ab...c.eb..",
                ".ab..c....cab...",
                ".....c..f.c.....",
                ".....d.eb.c.....",
                ".....c.ab.d.....",
                ".....c.c..c.....",
                ".....c.c..c.....",
                ".....c.c..c....."},
                'a', 0x355C22, 'b', 0x4B782C, 'c', 0x5E8A30, 'd', 0x9CC05A, 'e', 0x6E9E3C, 'f', 0x93BE55);
        // Wild rice (Zizania): the leafy base (bottom half), the nodding pale panicle over arching blades (top half).
        sprite("wild_rice_bottom", new String[]{
                "..e...b..e...b..",
                "..e...b..e...b..",
                "..e..b...e...b..",
                "...e.b...e..b...",
                "...e.b..e...b...",
                "...e.b..e..b....",
                "....eb..e..b....",
                "....eb.e...b....",
                "....e..e..b.....",
                "....eb.e..b.....",
                ".....e.e.bb.....",
                ".....ebe.b......",
                ".....eebbb......",
                "......ebb.......",
                "......ebb.......",
                ".....aabaa......"},
                'a', 0x3A5A24, 'b', 0x4F7F31, 'e', 0x7FAE4C);
        sprite("wild_rice_top", new String[]{
                "................",
                ".......st.......",
                "......s.tu......",
                ".....s..t.u.....",
                ".....t..s..t....",
                "....u...t...u...",
                "........b.......",
                "..f.....b....f..",
                "...e....b...e...",
                "....b...b..b....",
                ".....b..e.b.....",
                "......b.eb......",
                ".......eb.......",
                "..e....eb...f...",
                "...ee..eb..bb...",
                ".....e.eb.b....."},
                'b', 0x4F7F31, 'e', 0x7FAE4C, 'f', 0x9CC468, 's', 0xE0D7A6, 't', 0xB8AA70, 'u', 0x8F8251);
    }

    static void handDrawnPlants() throws Exception {
        Object[] fernGreens = {'a', 0x2A4A1E, 'b', 0x3A6427, 'c', 0x4F7F31, 'd', 0x689A3E, 'e', 0x88B650};
        // Karst fern: a fan of pinnate fronds from the middle, leaflets on both sides, the tips lit.
        sprite("karst_fern", new String[]{
                "................",
                ".......e........",
                "...e..dde..e....",
                "..e.d.dc.ed.....",
                "...dcd.c.dc.d.e.",
                ".e..dc.c.c.dcd..",
                "..dcd.cbc.cd....",
                "...cbc.bc.bcd.e.",
                ".e..bcbb.bcb.d..",
                "..dcb.bab.b.cd..",
                "...bcab.babcb...",
                ".....bbaab.b....",
                "...bb..aa..bb...",
                ".....bbaabb.....",
                ".......aa.......",
                "................"},
                fernGreens);
        // Huangshan rhododendron: a woody shrub, whorls of dark leathery leaves (bottom), pink trusses on top.
        Object[] leather = {'a', 0x1C3A22, 'b', 0x285130, 'c', 0x3A6E42, 'd', 0x5E9466, 't', 0x55402F};
        sprite("rhododendron_bottom", new String[]{
                "................",
                "..cd.......dc...",
                ".cbbd.d...dbbc..",
                "..abbcdc.cbba...",
                "...aab.tcbaa....",
                "....ad.t.da.....",
                "..cd...t..dc....",
                ".cbbd..t.dbbc...",
                "..abbc.tcbba....",
                "...aa..t.aa.....",
                ".....t.t........",
                "......tt..t.....",
                ".......t.t......",
                ".......tt.......",
                ".......t........",
                "......tt........"},
                leather);
        sprite("rhododendron_top", new String[]{
                "................",
                "....pq....qp....",
                "...pqqp..pqqp...",
                "..pqoqqp.pqoqp..",
                "..poqqop.poqqo..",
                "...ooppd..oppd..",
                "...dc.t..cd.t...",
                "..cbbdt..dbbc...",
                "...aabt...baa...",
                "......t..t......",
                "...pq.t..t.qp...",
                "..pqqptt.tpqqp..",
                "..oqqo.t.toqqo..",
                "...dcb.tt.bcd...",
                "....a..t...a....",
                ".......t........"},
                cat(leather, 'o', 0xC15A88, 'p', 0xDB80A6, 'q', 0xF5C3D7));
        // Tamarisk: thin red stems, feathery blue-green sprays (bottom); pink flower plumes over them (top).
        Object[] spray = {'g', 0x4E6E5A, 'h', 0x7E9E82, 't', 0x6E4038};
        sprite("tamarisk_bottom", new String[]{
                "..h.h......h.h..",
                ".hgh.hg...hgh.h.",
                "..g.hg.h.hg.hg..",
                ".h.g..gt.g..g.h.",
                "..hg.h.tg..hgh..",
                ".gh.g..t.hg.g...",
                "..g.h.tt..g.h...",
                ".h..g..t.g...g..",
                "..hg..t.t..hg...",
                "...g..t..t.g....",
                "......t..t......",
                ".....t...t......",
                ".....t..t.......",
                "......t.t.......",
                "......tt........",
                "......tt........"},
                spray);
        sprite("tamarisk_top", new String[]{
                "....q.....q.....",
                "...qpq...qpq..q.",
                "...pop..qpop.qpq",
                "..qpo...pop..pop",
                "...op..q.op..op.",
                "...o..qpq.o...o.",
                "...g..pop.g..g..",
                "..hg..op..gh.hg.",
                "...g.h.o.g...g..",
                "..h.gg..g..hg...",
                ".h..g.h.g.h.g...",
                "..hg..t..g...h..",
                "...h.t..t..hg...",
                ".....t..t.......",
                "......t.t.......",
                "......tt........"},
                cat(spray, 'o', 0xB66A8A, 'p', 0xD99AB4, 'q', 0xF0C8D7));
        // Tree fern: a crown of arching fronds over the top of its hairy trunk.
        sprite("tree_fern_top", new String[]{
                "................",
                ".....e...e......",
                "...ed.e.e.de....",
                "..d.dc.e.cd.d...",
                ".e.c..cdc..c.e..",
                ".dc..b.c.b..cd..",
                "d.b.b..c..b.b.d.",
                ".b..a.bcb.a..b..",
                "b..a.a.c.a.a..b.",
                "..a.a..c..a.a...",
                ".a.....c.....a..",
                "a.....tct.....a.",
                "......ttt.......",
                "......tst.......",
                "......tts.......",
                "......tst......."},
                cat(fernGreens, 't', 0x3E2D20, 's', 0x6C523B));
        // Cistanche: a fleshy golden spike scaled with tubular violet flowers.
        sprite("cistanche", new String[]{
                "................",
                ".......q........",
                "......pqp.......",
                ".....pqrqp......",
                ".....rpqpo......",
                ".....pyrypq.....",
                "....qryypyo.....",
                ".....pyqyrp.....",
                "....oyypyyp.....",
                ".....rypyyo.....",
                ".....pyyxyp.....",
                "......yyxy......",
                "......yyxy......",
                "......xyyx......",
                ".....wxyyxw.....",
                "....wwwxxwww...."},
                'o', 0x7A5CB3, 'p', 0x9C82CE, 'q', 0xC4B4E6, 'r', 0x4E3480, 'y', 0xD8B254, 'x', 0xA47E32, 'w', 0x6A5030);
    }

    /** The north (14/10/2026): the spruce seedling, the snow lotus, the alpine sedge. */
    static void handDrawnNorth() throws Exception {
        // Tianshan spruce seedling: a slender spire of dark needles in tiers, a leader shoot at the top.
        sprite("tianshan_spruce_sapling", new String[]{
                "................",
                ".......d........",
                "......cdc.......",
                ".......c........",
                "......bcd.......",
                ".....bccdc......",
                "......bcc.......",
                ".....abccd......",
                "....abbccdc.....",
                ".....abcc.......",
                "....abbcccd.....",
                "...aabbccdcc....",
                "......att.......",
                ".......t........",
                "......tt........",
                ".......t........"},
                'a', 0x0D241E, 'b', 0x143428, 'c', 0x1D4432, 'd', 0x386A4E, 't', 0x4F463C);
        // Snow lotus (Saussurea involucrata): a cone of overlapping pale lime and cream bracts, the purple florets just
        // showing at its tip, on a short stem, woolly grey-green leaves fanned at its foot.
        sprite("snow_lotus", new String[]{
                "................",
                ".......pp.......",
                "......pqqp......",
                ".....hjpqjh.....",
                "....hijjjjih....",
                "....hijijjih....",
                "...ghiijjiihg...",
                "...hijiijjjih...",
                "...ghiijiijhg...",
                "....ghiiiihg....",
                ".....gghhgg.....",
                "..ll...ff...ll..",
                ".lmml..ff..lmml.",
                "lmnmml.ff.lmmnml",
                ".lmnnmlffmlnnml.",
                "...llmmffmmll..."},
                'h', 0xB5C878, 'i', 0xD6E3A4, 'j', 0xEEF1CF, 'p', 0x8E5A9E, 'q', 0x5E3570, 'g', 0x7E9A55,
                'f', 0x5E7A45, 'l', 0x667F63, 'm', 0x93A98F, 'n', 0xBFCDB9);
        // Chinese milk vetch: round heads of pink-purple pea flowers on thin stems, small pinnate leaves below.
        sprite("milk_vetch", new String[]{
                "................",
                "..ppq.....pq....",
                ".prpq....prpq...",
                ".qpqp....qprp...",
                "..qq..pq..qq....",
                "...s.prpq..s....",
                "...s.qpqp..s....",
                "...s..qq...s....",
                "..ls...s..sl....",
                ".l.s...s..s.l...",
                "...sl..s.ls.....",
                "..l.s.ls.s.l....",
                "....s.l.ss......",
                "...l.sl.sl......",
                ".....ssss.......",
                "......ss........"},
                'p', 0xC77BB8, 'q', 0x9C4E92, 'r', 0xEFB5E0, 's', 0x5C7F3A, 'l', 0x7EA352);
        // Alpine sedge: a tight tussock of fine blades, yellowing and dry at the tips.
        sprite("kunlun_sedge", new String[]{
                "................",
                "..y.......y.....",
                "...y..x..y...x..",
                "...x..x..x..x...",
                "..x.x.x.x..x....",
                "....x.xwx.x..x..",
                "..w..xwxw.x.x...",
                "...w.wxwx..x....",
                "....wvwxw.wx.w..",
                "..v..wvwwxw.w...",
                "...v.vwvwvw.v...",
                "....vvwvvwvv....",
                "...u.vvuvvvu....",
                "....uuvuuvu.....",
                ".....uuuuuu.....",
                "......uuuu......"},
                'y', 0xC9A86A, 'x', 0xA6AD68, 'w', 0x7F9348, 'v', 0x5E7436, 'u', 0x44562A);
    }

    static void handDrawnSaplings() throws Exception {
        Object[] greens = {'a', 0x1F4426, 'b', 0x2E5C30, 'c', 0x40763C, 'd', 0x56904C, 'e', 0x72A85F};
        sprite("osmanthus_sapling", new String[]{
                "................",
                "......cdd.......",
                "....bcdedc......",
                "...bccddedc.....",
                "...bcyccddcb....",
                "..abbccccdcb....",
                "..abccbcyccb....",
                "...abbcccbb.....",
                "....abbbbba.....",
                ".....a.t.a......",
                ".......t........",
                ".......t........",
                ".......tt.......",
                ".......t........",
                "......tt........",
                ".......t........"},
                cat(greens, 'y', 0xF0C04A, 't', 0x5A554E));
        sprite("banyan_sapling", new String[]{
                "................",
                ".....dd...dd....",
                "....dcc..dccd...",
                "...dcbb.dcbbc...",
                "...cbba..cbba...",
                "....ba.t..ba....",
                "..dd...t.dd.....",
                ".dccd..tdcccd...",
                ".cbbc..t.cbbc...",
                "..bba..t..bba...",
                "....r..t..r.....",
                "....r..t..r.....",
                ".......t........",
                "......tt........",
                ".......tt.......",
                "......t.t......."},
                'a', 0x1B3E1E, 'b', 0x2A5627, 'c', 0x3B7031, 'd', 0x6CA150, 't', 0x6F6C64, 'r', 0x786A58);
        sprite("huangshan_pine_sapling", new String[]{
                "................",
                ".....cddc.......",
                "...bccddddc.....",
                "....abbbbba.....",
                ".......t........",
                ".bcdd..t........",
                "bccdddctt.......",
                ".abbbba.t.......",
                "........t.cdd...",
                ".......tcccddc..",
                ".......t.abbba..",
                ".......t........",
                "......tt........",
                ".......t........",
                ".......t........",
                "......t.t......."},
                'a', 0x17332B, 'b', 0x224536, 'c', 0x2E5841, 'd', 0x52805E, 't', 0x6E412A);
        sprite("jujube_sapling", new String[]{
                "................",
                "...e........e...",
                "..ed..e....ed...",
                "...dd.dc..dd....",
                "....c.ed.c......",
                "..e..t.dt..e....",
                ".edd.t..t.dde...",
                "..dct...tc.d....",
                "...r.t.t..r.....",
                ".....t.t........",
                "......tt..e.....",
                ".......t.edd....",
                ".......t..dc....",
                "......tt........",
                ".......t........",
                "......t.t......."},
                'c', 0x456A24, 'd', 0x5F8A2F, 'e', 0x8CB548, 'r', 0xB8342A, 't', 0x4A3A2E);
        sprite("dove_tree_sapling", new String[]{
                "................",
                ".....ddcd.......",
                "...cddedcdd.....",
                "..cdeddwdedc....",
                "..cdddwwcddc....",
                "..bcdddw.dcb....",
                "...bccdccccb....",
                "....bbcbccb.....",
                ".....abtba......",
                ".......t........",
                ".......t........",
                ".......t........",
                "......tt........",
                ".......t........",
                ".......t........",
                "......t.t......."},
                'a', 0x26421F, 'b', 0x355A29, 'c', 0x477333, 'd', 0x5D8C40, 'e', 0x77A552, 'w', 0xF2F1EA, 't', 0x5A4C40);
        sprite("willow_sapling", new String[]{
                "................",
                "....cdddddc.....",
                "...cdd.t.ddc....",
                "..cd..dtd..dc...",
                "..d.c..t..c.d...",
                ".cd.d..t..d.dc..",
                ".d..d..t..d..d..",
                ".c..c..t..c..c..",
                ".d..d..t..d..d..",
                ".c..c..t..c..c..",
                "....d..t..d.....",
                "....c..t..c.....",
                ".......t........",
                "......tt........",
                ".......t........",
                "......t.t......."},
                'c', 0x5B8236, 'd', 0x8FB655, 't', 0x5A4F42);
        sprite("peach_sapling", new String[]{
                "................",
                "....pq....qp....",
                "...pqo.pq.oqp...",
                "....op.qp.po....",
                ".....t.pq.t.....",
                "..pq..t.tt..qp..",
                "..qop..tt..poq..",
                "...op..t...po...",
                ".......t.pqp....",
                ".......ttqop....",
                ".......t..o.....",
                ".......t........",
                "......tt........",
                ".......t........",
                ".......t........",
                "......t.t......."},
                'o', 0xB24C72, 'p', 0xDC7FA0, 'q', 0xF5BDD0, 't', 0x6B3A33);
        sprite("desert_poplar_sapling", new String[]{
                "................",
                "....dd....dd....",
                "...dcdd..dccd...",
                "...cbc.d.cbc....",
                "....bt.c.tb.....",
                ".dd..t...t..dd..",
                "dccd..t.t..dccd.",
                ".cbc..t.t..cbc..",
                "..b....tt...b...",
                ".......t........",
                ".......t........",
                ".......t........",
                "......tt........",
                ".......t........",
                ".......t........",
                "......t.t......."},
                'b', 0x926E16, 'c', 0xBA921F, 'd', 0xEDCC57, 't', 0x5C564E);
    }

    static Object[] cat(Object[] a, Object... b) {
        Object[] r = new Object[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    // ------------------------------------------------------------------ plants (clean silhouettes)

    static final class Brush {
        final Img i;

        Brush(Img i) {
            this.i = i;
        }

        void px(int x, int y, int c) {
            i.put(x, y, c);
        }

        /** A stroke from (x0, y0) to (x1, y1). */
        void line(int x0, int y0, int x1, int y1, int c) {
            int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
            while (true) {
                px(x0, y0, c);
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 >= dy) {
                    err += dy;
                    x0 += sx;
                }
                if (e2 <= dx) {
                    err += dx;
                    y0 += sy;
                }
            }
        }

        /** A stem with a lit side: the light pixel on the left, the dark one on the right (2 px wide when wide). */
        void stem(int x0, int y0, int x1, int y1, int light, int dark, boolean wide) {
            line(x0, y0, x1, y1, light);
            if (wide) line(x0 + 1, y0, x1 + 1, y1, dark);
        }

        /**
         * A leaf from (x, y) going (dx, dy) for len pixels: a mid vein, widening then narrowing, the side toward the
         * top-left lit.
         */
        void leaf(int x, int y, double dx, double dy, int len, int[] ramp) {
            double nx = -dy, ny = dx; // normal
            for (int k = 0; k <= len; k++) {
                double t = (double) k / len;
                double w = Math.sin(t * Math.PI) * Math.max(1, len / 3.2);
                double cx = x + dx * k, cy = y + dy * k;
                for (double s = -w; s <= w + 0.01; s += 0.5) {
                    int px = (int) Math.round(cx + nx * s), py = (int) Math.round(cy + ny * s);
                    boolean litSide = (nx * s + ny * s) < 0;
                    px(px, py, at(ramp, Math.abs(s) < 0.4 ? 2 : litSide ? 3 : 1));
                }
            }
        }

        /** A small round blossom: petals around a heart. */
        void blossom(int x, int y, int petal, int petalDark, int heart) {
            px(x, y - 1, petal);
            px(x - 1, y, petal);
            px(x + 1, y, petalDark);
            px(x, y + 1, petalDark);
            px(x, y, heart);
        }
    }

    static final int[] GREEN = ramp(0x2A4A1E, 0x3A6427, 0x4F7F31, 0x689A3E, 0x84B44E);
    static final int[] GREEN_DRY = ramp(0x5A5A2C, 0x737336, 0x8C8B44, 0xA6A355, 0xC0BA6C);
    static final int[] GREY_GREEN = ramp(0x41503A, 0x546648, 0x6B7D58, 0x83966B, 0x9DAE82);

    static void plants() throws Exception {
        // Karst fern: arching fronds from the base, leaflets along each.
        Img fern = new Img("karst_fern");
        Brush b = new Brush(fern);
        int[][] fronds = {{7, 15, 2, 2}, {8, 15, 14, 3}, {7, 15, 0, 10}, {8, 15, 15, 10}, {8, 15, 8, 1}};
        for (int[] f : fronds) frond(b, f[0], f[1], f[2], f[3], GREEN);
        fern.save("karst_fern");

        // Feather grass (Stipa): pale straw tufts with long silky plumes.
        Img stipa = new Img("feather_grass");
        b = new Brush(stipa);
        int[] straw = ramp(0x7D7448, 0x998F5A, 0xB5AB73, 0xCFC690, 0xE6DFB3);
        for (int k = 0; k < 7; k++) {
            int x = 3 + k * 2 + stipa.r.nextInt(2), top = 2 + stipa.r.nextInt(6), lean = stipa.r.nextInt(3) - 1;
            b.line(x, 15, x + lean, top + 3, at(straw, 1 + stipa.r.nextInt(2)));
            b.line(x + lean, top + 3, x + lean * 2, top, straw[4]);
            b.px(x + lean * 2 + (lean >= 0 ? 1 : -1), top, straw[3]);
        }
        for (int k = 0; k < 5; k++) b.line(4 + k * 2, 15, 3 + k * 2 + stipa.r.nextInt(3), 11, at(GREEN_DRY, 1 + stipa.r.nextInt(2)));
        stipa.save("feather_grass");

        // Sea buckthorn: silvery-green twigs dense with orange berries.
        Img sea = new Img("sea_buckthorn");
        b = new Brush(sea);
        int bark = rgb(0x5B4A3A), barkDark = rgb(0x3E3127);
        b.line(8, 15, 6, 6, bark);
        b.line(8, 15, 11, 7, barkDark);
        b.line(7, 11, 3, 8, bark);
        b.line(9, 10, 13, 9, barkDark);
        for (int k = 0; k < 14; k++) {
            int x = 3 + sea.r.nextInt(11), y = 3 + sea.r.nextInt(10);
            b.leaf(x, y, sea.r.nextBoolean() ? 0.7 : -0.7, -0.7, 2, GREY_GREEN);
        }
        int[] berry = {rgb(0xF2A23A), rgb(0xD9781F), rgb(0xFFD27A)};
        for (int k = 0; k < 9; k++) {
            int x = 4 + sea.r.nextInt(9), y = 5 + sea.r.nextInt(7);
            b.px(x, y, berry[1]);
            b.px(x, y - 1, berry[2]);
            b.px(x + 1, y, berry[0]);
        }
        sea.save("sea_buckthorn");

        // Bamboo shoot: a pointed sheathed cone, brown with darker bands.
        Img shoot = new Img("bamboo_shoot");
        b = new Brush(shoot);
        int[] sheath = ramp(0x4A3622, 0x684C2F, 0x86653E, 0xA5824F, 0xC2A067);
        for (int y = 4; y <= 15; y++) {
            int w = Math.max(0, (y - 4) / 3);
            for (int x = 8 - w; x <= 8 + w; x++) {
                double v = 2 + (x < 8 ? 1 : x > 8 ? -1 : 0) + ((y % 3) == 0 ? -1 : 0);
                b.px(x, y, at(sheath, v));
            }
        }
        b.px(8, 3, sheath[3]);
        b.px(8, 2, GREEN[2]);
        b.px(9, 1, GREEN[3]);
        shoot.save("bamboo_shoot");

        // Arrow bamboo (Fargesia): thin green canes, nodes, leaf fans at the top.
        Img arrow = new Img("arrow_bamboo");
        b = new Brush(arrow);
        int[] cane = ramp(0x3E5E22, 0x547A2C, 0x6C9638, 0x88B048, 0xA5C760);
        for (int k = 0; k < 4; k++) {
            int x = 3 + k * 3 + arrow.r.nextInt(2), top = 2 + arrow.r.nextInt(4);
            for (int y = top; y <= 15; y++) b.px(x, y, (y % 4 == 0) ? cane[4] : cane[2]);
            b.leaf(x, top + 1, -0.8, -0.4, 4, GREEN);
            b.leaf(x, top + 3, 0.85, -0.3, 4, GREEN);
        }
        arrow.save("arrow_bamboo");

        // Chrysanthemum: a golden flower head on a leafy stem.
        Img chr = new Img("chrysanthemum");
        b = new Brush(chr);
        b.line(8, 15, 8, 8, GREEN[2]);
        b.leaf(8, 13, -0.85, -0.5, 3, GREEN);
        b.leaf(8, 11, 0.85, -0.5, 3, GREEN);
        int[] gold = ramp(0xA8661A, 0xD18E22, 0xEDB534, 0xFAD461, 0xFFE99A);
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
            double d = Math.hypot(dx, dy);
            if (d > 3.2) continue;
            boolean petal = (Math.abs(dx) + Math.abs(dy)) % 2 == 0 || d < 1.5;
            if (!petal) continue;
            b.px(8 + dx, 6 + dy, at(gold, d < 1.2 ? 0.5 : 3 - (dx + dy) * 0.35));
        }
        chr.save("chrysanthemum");

        // Rapeseed: bright yellow racemes of tiny flowers on blue-green stems.
        Img rape = new Img("rapeseed");
        b = new Brush(rape);
        int[] yel = ramp(0xB59A12, 0xDDBF1C, 0xF4DA34, 0xFFEC6E);
        int[] blueGreen = ramp(0x2F5034, 0x406A45, 0x548557, 0x6E9E6C);
        for (int k = 0; k < 3; k++) {
            int x = 3 + k * 5 + rape.r.nextInt(2), top = 1 + rape.r.nextInt(4);
            b.line(x, 15, x, top + 1, blueGreen[1 + k % 2]);
            // green buds at the tip, open four-petal flowers below them, alternating sides of the stem
            b.px(x, top, blueGreen[3]);
            b.px(x, top + 1, yel[1]);
            for (int f = 0; f < 3; f++) {
                int fy = top + 2 + f * 2, fx = x + (f % 2 == 0 ? -1 : 1);
                b.px(fx, fy, yel[3]);
                b.px(fx + (f % 2 == 0 ? -1 : 1), fy, yel[2]);
                b.px(fx, fy - 1, yel[2]);
                b.px(fx, fy + 1, yel[1]);
            }
            b.leaf(x, 12 + k % 2, k % 2 == 0 ? -0.85 : 0.85, -0.45, 3, blueGreen);
        }
        rape.save("rapeseed");

        // Saxaul: a twisted grey-brown shrub of jointed green twigs, no leaves.
        Img sax = new Img("saxaul");
        b = new Brush(sax);
        int[] wood = ramp(0x3B3027, 0x54453A, 0x6D5C4D, 0x877563);
        b.line(7, 15, 6, 10, wood[1]);
        b.line(8, 15, 10, 9, wood[2]);
        b.line(6, 10, 3, 7, wood[2]);
        b.line(10, 9, 13, 6, wood[1]);
        b.line(7, 12, 8, 6, wood[2]);
        int[] twig = ramp(0x55693A, 0x6F8546, 0x8AA05A, 0xA6B873);
        for (int k = 0; k < 18; k++) {
            int x = 2 + sax.r.nextInt(12), y = 2 + sax.r.nextInt(7), len = 1 + sax.r.nextInt(3);
            b.line(x, y, x + (sax.r.nextBoolean() ? 1 : -1) * len / 2, y - len, at(twig, 1 + sax.r.nextInt(3)));
        }
        sax.save("saxaul");

        // Cistanche: a fleshy yellow spike, purple-blue flowers scaled up its stem.
        Img cis = new Img("cistanche");
        b = new Brush(cis);
        int[] flesh = ramp(0x8E6A2A, 0xB38A3A, 0xD2AA4E, 0xE6C56A);
        int[] purple = ramp(0x47306E, 0x5E4291, 0x7A5CB3, 0x9C82CE);
        // a fleshy spike, three pixels wide, its upper half clad in tubular blue-violet flowers like scales
        for (int y = 4; y <= 15; y++) {
            int w = y < 6 ? 0 : 1;
            for (int x = 8 - w; x <= 8 + w; x++) b.px(x, y, at(flesh, 2 + (x < 8 ? 1 : x > 8 ? -1 : 0)));
            if (y >= 5 && y <= 11) {
                boolean left = y % 2 == 0;
                b.px(left ? 6 : 10, y, at(purple, left ? 3 : 1));
                b.px(left ? 7 : 9, y, purple[2]);
            }
        }
        b.px(8, 3, purple[3]);
        b.px(8, 2, purple[2]);
        b.px(7, 15, flesh[0]);
        b.px(9, 15, flesh[0]);
        cis.save("cistanche");

        // Lotus pad: a round blue-green leaf with radiating veins and a notch.
        Img pad = new Img("lotus_pad");
        b = new Brush(pad);
        int[] padRamp = ramp(0x2D5B3A, 0x3B7348, 0x4D8C55, 0x63A465, 0x80BB7C);
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double dx = x - 7.5, dy = y - 7.5, d = Math.hypot(dx, dy);
            if (d > 7.2) continue;
            double ang = Math.atan2(dy, dx);
            if (ang > 1.25 && ang < 1.75 && d > 1.5) continue; // the notch
            boolean vein = Math.abs(Math.sin(ang * 5)) < 0.14 && d > 1.3;
            double v = 2.2 + (-dx - dy) * 0.08 + (d > 6.3 ? -1 : 0) + (vein ? 1.2 : 0);
            b.px(x, y, at(padRamp, v));
        }
        b.px(7, 7, padRamp[4]);
        b.px(8, 8, padRamp[3]);
        pad.save("lotus_pad");

        // Lotus flower: a pink cup of pointed petals over a green pad edge.
        Img lotus = new Img("lotus_flower");
        b = new Brush(lotus);
        int[] pink = ramp(0x9C3460, 0xC4517F, 0xE07AA1, 0xF2A8C4, 0xFCD5E3);
        int[][] petals = {{8, 15, 8, 4}, {8, 15, 4, 6}, {8, 15, 12, 6}, {8, 15, 2, 10}, {8, 15, 14, 10}};
        for (int[] p : petals) {
            double dx = p[2] - p[0], dy = p[3] - p[1], len = Math.hypot(dx, dy);
            b.leaf(p[0], p[1], dx / len, dy / len, (int) len, pink);
        }
        for (int x = 6; x <= 10; x++) b.px(x, 12, rgb(0xE8C64A));
        b.px(8, 11, rgb(0xC9A23A));
        lotus.save("lotus_flower");

        // Banyan aerial roots: thin grey-brown cords hanging, a few branching.
        strands("banyan_roots", ramp(0x463B30, 0x5E5143, 0x786A58, 0x91836E), 5, false);
        // Willow strands: long strings of narrow light-green leaves.
        strands("willow_strands", ramp(0x46672C, 0x5B8236, 0x749D44, 0x8FB655), 4, true);
    }

    /**
     * A fern frond from its base (x0, y0) to its tip (x1, y1): a thin rachis, leaflets alternating on both sides
     * every pixel, shorter toward the tip, the upper side lit.
     */
    static void frond(Brush b, int x0, int y0, int x1, int y1, int[] ramp) {
        double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy);
        double ux = dx / len, uy = dy / len, nx = -uy, ny = ux; // along, across
        int steps = (int) Math.round(len);
        for (int k = 0; k <= steps; k++) {
            double t = (double) k / steps;
            // a slight arch: the frond bows outward, its tip drooping
            double bow = Math.sin(t * Math.PI) * 1.2;
            double x = x0 + dx * t + nx * bow, y = y0 + dy * t + ny * bow + t * t * 1.5;
            b.px((int) Math.round(x), (int) Math.round(y), ramp[1]);
            if (k < 2) continue;
            int leaflet = (int) Math.round((1 - t) * 1.6 + 0.6);
            int side = k % 2 == 0 ? 1 : -1;
            for (int s = 1; s <= leaflet; s++) {
                int lx = (int) Math.round(x + nx * s * side - ux * s * 0.5), ly = (int) Math.round(y + ny * s * side - uy * s * 0.5);
                b.px(lx, ly, at(ramp, (side > 0 ? 3 : 2) + (s == leaflet ? 0.5 : 0)));
            }
        }
    }

    static void strands(String name, int[] ramp, int count, boolean leafy) throws Exception {
        Img i = new Img(name);
        Brush b = new Brush(i);
        for (int k = 0; k < count; k++) {
            int x = 1 + k * (S - 2) / count + i.r.nextInt(2);
            for (int y = 0; y < S; y++) {
                if (y > 0 && i.r.nextInt(7) == 0) x += i.r.nextBoolean() ? 1 : -1;
                x = Math.max(0, Math.min(S - 1, x));
                b.px(x, y, at(ramp, 1 + (y % 4 == 0 ? 1 : 0)));
                if (leafy && y % 3 == 1) b.px(x + 1, y, at(ramp, 2 + i.r.nextInt(2)));
                if (leafy && y % 3 == 2) b.px(x - 1, y, ramp[1]);
            }
        }
        i.save(name);
    }

    /** Tall plants: a bottom half and a top half that meet at the block seam. */
    static void tallPlants() throws Exception {
        // Huangshan rhododendron: a leathery-leaved shrub crowned with pink trusses.
        Img rb = new Img("rhododendron_bottom");
        Brush b = new Brush(rb);
        int[] stems = ramp(0x3A2C22, 0x55402F, 0x6F563F);
        b.line(8, 15, 7, 0, stems[1]);
        b.line(8, 15, 11, 3, stems[2]);
        b.line(7, 9, 3, 2, stems[1]);
        int[] dark = ramp(0x1C3A22, 0x285130, 0x376A3E, 0x4C8250, 0x669A66);
        for (int[] l : new int[][]{{7, 8, -1, 0}, {8, 6, 1, 0}, {4, 3, -1, -1}, {11, 4, 1, -1}, {6, 12, -1, 1}, {9, 11, 1, 1}}) {
            b.leaf(l[0], l[1], l[2] * 0.8, l[3] * 0.5 - 0.4, 3, dark);
        }
        rb.save("rhododendron_bottom");
        Img rt = new Img("rhododendron_top");
        b = new Brush(rt);
        b.line(7, 15, 7, 9, stems[1]);
        b.line(8, 15, 10, 10, stems[2]);
        int[] pink = ramp(0x9B3A66, 0xC15A88, 0xDB80A6, 0xEDA9C3, 0xF8CFDF);
        for (int[] t : new int[][]{{7, 7}, {11, 8}, {4, 10}}) {
            for (int k = 0; k < 6; k++) {
                int x = t[0] + rt.r.nextInt(4) - 2, y = t[1] + rt.r.nextInt(4) - 2;
                b.blossom(x, y, pink[3], pink[1], pink[0]);
            }
        }
        for (int[] l : new int[][]{{5, 13, -1}, {10, 13, 1}}) b.leaf(l[0], l[1], l[2] * 0.8, -0.3, 3, dark);
        rt.save("rhododendron_top");

        // Tree fern: a dark hairy trunk (bottom), a crown of arching fronds (top).
        Img tb = new Img("tree_fern_bottom");
        b = new Brush(tb);
        int[] trunk = ramp(0x2A1E15, 0x3E2D20, 0x553F2D, 0x6C523B);
        for (int y = 0; y < S; y++) for (int x = 6; x <= 9; x++) {
            double v = 1.5 + (x == 6 ? 1 : x == 9 ? -1 : 0) + ((x + y * 3) % 5 == 0 ? -1 : 0);
            b.px(x, y, at(trunk, v));
        }
        tb.save("tree_fern_bottom");
        Img tt = new Img("tree_fern_top");
        b = new Brush(tt);
        for (int y = 9; y < S; y++) for (int x = 6; x <= 9; x++) b.px(x, y, at(trunk, 1.5 + (x == 6 ? 1 : x == 9 ? -1 : 0)));
        for (int[] f : new int[][]{{7, 9, 0, 7}, {8, 9, 15, 7}, {7, 9, 2, 2}, {8, 9, 13, 2}, {8, 9, 8, 0}, {7, 9, 0, 12}, {8, 9, 15, 12}}) {
            frond(b, f[0], f[1], f[2], f[3], GREEN);
        }
        tt.save("tree_fern_top");

        // Wild rice (Zizania): tall arching grass blades, a nodding pale seed head on top.
        Img wb = new Img("wild_rice_bottom");
        b = new Brush(wb);
        for (int k = 0; k < 6; k++) {
            int x = 3 + k * 2 + wb.r.nextInt(2);
            b.line(x, 15, x + wb.r.nextInt(3) - 1, 0, at(GREEN, 1.5 + wb.r.nextInt(3)));
        }
        wb.save("wild_rice_bottom");
        Img wt = new Img("wild_rice_top");
        b = new Brush(wt);
        for (int k = 0; k < 5; k++) {
            int x = 3 + k * 2 + wt.r.nextInt(2), top = 3 + wt.r.nextInt(6), lean = wt.r.nextBoolean() ? 1 : -1;
            b.line(x, 15, x, top + 3, at(GREEN, 2 + wt.r.nextInt(2)));
            b.line(x, top + 3, x + lean * 2, top, GREEN[3]);
        }
        int[] seed = ramp(0x8F8251, 0xB2A46A, 0xD0C489, 0xE6DCAA);
        for (int y = 1; y < 8; y++) {
            b.px(8 + (y > 4 ? 1 : 0), y, at(seed, 1 + (y % 2)));
            b.px(9 + (y > 4 ? 1 : 0), y + 1, seed[0]);
        }
        wt.save("wild_rice_top");

        // Tamarisk: a feathery shrub, pink flower plumes over blue-green sprays.
        Img mb = new Img("tamarisk_bottom");
        b = new Brush(mb);
        int[] tw = ramp(0x4A2E2A, 0x63403A, 0x7D5248);
        b.line(8, 15, 6, 0, tw[1]);
        b.line(8, 15, 10, 0, tw[2]);
        b.line(7, 8, 3, 2, tw[1]);
        b.line(9, 7, 13, 1, tw[2]);
        int[] spray = ramp(0x3D5A48, 0x50705A, 0x66876D, 0x7E9E82);
        for (int k = 0; k < 22; k++) {
            int x = 2 + mb.r.nextInt(12), y = mb.r.nextInt(12);
            b.px(x, y, at(spray, 1 + mb.r.nextInt(3)));
            b.px(x, y + 1, spray[0]);
        }
        mb.save("tamarisk_bottom");
        Img mt = new Img("tamarisk_top");
        b = new Brush(mt);
        b.line(6, 15, 5, 9, tw[1]);
        b.line(10, 15, 11, 8, tw[2]);
        int[] plume = ramp(0xA45A7A, 0xC57A98, 0xDDA0B8, 0xEFC6D5);
        for (int[] p : new int[][]{{5, 4}, {11, 3}, {8, 7}, {3, 8}, {13, 8}}) {
            for (int k = 0; k < 7; k++) {
                int x = p[0] + mt.r.nextInt(3) - 1, y = p[1] + mt.r.nextInt(5) - 2;
                b.px(x, y, at(plume, 1 + mt.r.nextInt(3)));
            }
        }
        for (int k = 0; k < 10; k++) b.px(3 + mt.r.nextInt(10), 9 + mt.r.nextInt(6), at(spray, 1 + mt.r.nextInt(3)));
        mt.save("tamarisk_top");
    }

    /** Saplings: a little trunk with the tree's own leaves. */
    static void saplings() throws Exception {
        sapling("osmanthus", ramp(0x1B3720, 0x274D2C, 0x376338, 0x4B7B45, 0x639455), rgb(0x55514B), "round", rgb(0xF0C04A));
        sapling("banyan", ramp(0x173219, 0x234723, 0x325E2D, 0x46763C, 0x5E8F4D), rgb(0x625F58), "round", 0);
        sapling("huangshan_pine", ramp(0x122822, 0x1B382D, 0x264A38, 0x335C43, 0x456F51), rgb(0x643925), "tiers", 0);
        sapling("jujube", ramp(0x324E1B, 0x456A24, 0x5A832D, 0x729D39, 0x8CB548), rgb(0x47362B), "sparse", rgb(0xB8342A));
        sapling("dove_tree", ramp(0x26421F, 0x355A29, 0x477333, 0x5D8C40, 0x77A552), rgb(0x524539), "round", rgb(0xF2F1EA));
        sapling("willow", ramp(0x40612A, 0x557A33, 0x6C933F, 0x86AC4E, 0xA2C463), rgb(0x554A3E), "weeping", 0);
        sapling("peach", ramp(0x8F2F52, 0xB24C72, 0xCF6E93, 0xE596B2, 0xF5BDD0), rgb(0x6B3A33), "sparse", 0);
        sapling("desert_poplar", ramp(0x6E4E10, 0x926E16, 0xBA921F, 0xD6B034, 0xEDCC57), rgb(0x5C564E), "sparse", 0);
        sapling("tianshan_spruce", ramp(0x0D241E, 0x143428, 0x1D4432, 0x28553E, 0x386A4E), rgb(0x4F463C), "tiers", 0);
        sapling("camphor", ramp(0x163A1D, 0x225026, 0x316732, 0x447E41, 0x5C9756), rgb(0x564A40), "round", rgb(0xA4643A));
    }

    static void sapling(String name, int[] leaves, int bark, String form, int accent) throws Exception {
        Img i = new Img(name + "_sapling");
        Brush b = new Brush(i);
        b.line(8, 15, 8, 8, bark);
        b.px(7, 15, bark);
        switch (form) {
            case "tiers" -> { // pine: flat needle tiers
                for (int[] t : new int[][]{{9, 4}, {6, 3}, {3, 2}}) {
                    int y = t[0], w = t[1];
                    for (int x = 8 - w; x <= 8 + w; x++) {
                        b.px(x, y, at(leaves, 2 + (x < 8 ? 1 : 0)));
                        if (Math.abs(x - 8) < w) b.px(x, y + 1, leaves[1]);
                    }
                }
                b.line(8, 8, 8, 2, bark);
            }
            case "weeping" -> { // willow: an arch of hanging strings
                b.line(8, 8, 8, 4, bark);
                for (int x = 3; x <= 13; x += 2) {
                    int top = 4 + Math.abs(x - 8) / 2;
                    for (int y = top; y < top + 6 + i.r.nextInt(3); y++) b.px(x, y, at(leaves, 2 + ((y + x) % 3 == 0 ? 1 : 0)));
                }
            }
            case "sparse" -> { // jujube, peach, poplar: a forked twig with small clusters
                b.line(8, 9, 5, 5, bark);
                b.line(8, 10, 11, 6, bark);
                b.line(8, 9, 8, 4, bark);
                for (int[] c : new int[][]{{5, 4}, {11, 5}, {8, 3}, {4, 7}, {12, 8}}) {
                    for (int k = 0; k < 4; k++) b.px(c[0] + i.r.nextInt(3) - 1, c[1] + i.r.nextInt(3) - 1, at(leaves, 1.5 + i.r.nextInt(3)));
                }
            }
            default -> { // round crown
                for (int y = 2; y <= 9; y++) for (int x = 3; x <= 13; x++) {
                    double d = Math.hypot((x - 8) / 5.0, (y - 5.5) / 3.6);
                    if (d > 1 || h(i.seed, x, y) < 0.18) continue;
                    b.px(x, y, at(leaves, 2.6 - (x - 8) * 0.18 - (y - 5.5) * 0.3 + (h(i.seed + 1, x, y) - 0.5)));
                }
            }
        }
        if (accent != 0) {
            for (int k = 0; k < 3; k++) {
                int x = 4 + i.r.nextInt(9), y = 2 + i.r.nextInt(6);
                if (!i.empty(x, y)) b.px(x, y, accent);
            }
        }
        i.save(name + "_sapling");
    }

    // ------------------------------------------------------------------ moso bamboo culm

    static void bamboo() throws Exception {
        // The culm side: jade green, a lit left edge, a dark right edge, a pale ring and dark line at each node.
        Img side = new Img("moso_bamboo");
        int[] jade = ramp(0x2E4A1D, 0x3E6125, 0x51792F, 0x67923A, 0x81AC4A, 0x9DC264);
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            int rel = x - 4; // the culm is the middle 8 pixels of the model
            double v = 2.6 - Math.abs(rel - 2.5) * 0.25 + (rel <= 1 ? 1.2 : rel >= 6 ? -1.0 : 0) + (noise(side.seed, x, y, 1, 8) - 0.5) * 0.8;
            if (y == 7) v = 0.3;      // node line
            if (y == 6 || y == 8) v += 1.0; // swollen ring around it
            side.set(x, y, at(jade, v));
        }
        side.save("moso_bamboo");
        // The top: the hollow culm seen from above, a pale wall around a dark hollow.
        Img top = new Img("moso_bamboo_top");
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double d = Math.hypot(x - 7.5, y - 7.5);
            if (d > 4.2) {
                top.set(x, y, 0);
                continue;
            }
            top.set(x, y, d > 3.2 ? at(jade, 3 - (x - 7.5) * 0.2) : d > 2.4 ? rgb(0xC9D49A) : rgb(0x6B6A42));
        }
        top.save("moso_bamboo_top");
    }

    public static void main(String[] args) throws Exception {
        out = new File(args.length > 0 ? args[0] : "src/main/resources/assets/murimblock/textures/block");
        out.mkdirs();
        stones();
        for (Wood w : WOODS) {
            bark(w);
            logTop(w);
            planks(w);
        }
        leaves();
        plants();
        tallPlants();
        saplings();
        // The hand-drawn ones replace the drawn-by-rule versions above.
        handDrawn();
        handDrawnPlants();
        handDrawnSaplings();
        handDrawnNorth();
        bamboo();
    }
}
