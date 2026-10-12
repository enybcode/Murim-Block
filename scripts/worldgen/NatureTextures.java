import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Draws the 16x16 block textures of the Murim biomes (original pixel art, no vanilla texture used).
 *
 *   java scripts/worldgen/NatureTextures.java src/main/resources/assets/murimblock/textures/block
 *
 * Every texture is seeded by its name, so re-running gives the same pixels. Colours follow the research notes in
 * docs/biomes (bark, leaves, flowers and rocks of the real species and landscapes).
 */
public class NatureTextures {
    static final int S = 16;
    static File out;

    // ------------------------------------------------------------------ image helpers
    static final class Img {
        final BufferedImage im = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        final Random r;
        Img(String name) { r = new Random(name.hashCode() * 31L + 7); }
        void set(int x, int y, int c) { if (x >= 0 && y >= 0 && x < S && y < S) im.setRGB(x, y, c); }
        void setIfEmpty(int x, int y, int c) { if (x >= 0 && y >= 0 && x < S && y < S && (im.getRGB(x, y) >>> 24) == 0) im.setRGB(x, y, c); }
        int get(int x, int y) { return im.getRGB(Math.floorMod(x, S), Math.floorMod(y, S)); }
        void fill(int c) { for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) set(x, y, c); }
        /** Pick from a palette with weights by random. */
        int pick(int... pal) { return pal[r.nextInt(pal.length)]; }
        void line(int x0, int y0, int x1, int y1, int c) {
            int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
            while (true) {
                set(x0, y0, c);
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 >= dy) { err += dy; x0 += sx; }
                if (e2 <= dx) { err += dx; y0 += sy; }
            }
        }
        void save(String name) throws Exception { ImageIO.write(im, "png", new File(out, name + ".png")); System.out.println("  " + name); }
    }

    static int rgb(int hex) { return 0xFF000000 | hex; }
    static int shade(int c, double f) {
        int r = (int) Math.min(255, Math.max(0, ((c >> 16) & 255) * f)), g = (int) Math.min(255, Math.max(0, ((c >> 8) & 255) * f)),
                b = (int) Math.min(255, Math.max(0, (c & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t),
                bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
    /** Tileable value noise in [0,1). */
    static double noise(long seed, int x, int y, int cell) {
        double fx = (double) x / cell, fy = (double) y / cell;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy), n = S / cell;
        double tx = fx - x0, ty = fy - y0;
        tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty);
        double a = h(seed, Math.floorMod(x0, n), Math.floorMod(y0, n)), b = h(seed, Math.floorMod(x0 + 1, n), Math.floorMod(y0, n));
        double c = h(seed, Math.floorMod(x0, n), Math.floorMod(y0 + 1, n)), d = h(seed, Math.floorMod(x0 + 1, n), Math.floorMod(y0 + 1, n));
        return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty;
    }
    /** Tileable value noise with separate cell sizes (cx, cy must divide 16): long vertical grain for bark. */
    static double noise2(long seed, int x, int y, int cx, int cy) {
        double fx = (double) x / cx, fy = (double) y / cy;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy), nx = S / cx, ny = S / cy;
        double tx = fx - x0, ty = fy - y0;
        tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty);
        double a = h(seed, Math.floorMod(x0, nx), Math.floorMod(y0, ny)), b = h(seed, Math.floorMod(x0 + 1, nx), Math.floorMod(y0, ny));
        double c = h(seed, Math.floorMod(x0, nx), Math.floorMod(y0 + 1, ny)), d = h(seed, Math.floorMod(x0 + 1, nx), Math.floorMod(y0 + 1, ny));
        return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty;
    }

    static double h(long seed, int x, int y) {
        long v = seed * 6364136223846793005L + x * 1442695040888963407L + y * 2862933555777941757L;
        v ^= v >>> 29; v *= 0xBF58476D1CE4E5B9L; v ^= v >>> 32;
        return (v >>> 11) / (double) (1L << 53);
    }

    // ------------------------------------------------------------------ rocks and soils
    /** Stone-like: base tone shaded by two noise octaves, darker cracks and lighter grains. */
    static Img rock(String name, int base, int dark, int light, double grain) {
        Img i = new Img(name);
        long seed = name.hashCode();
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double n = noise(seed, x, y, 8) * 0.6 + noise(seed + 1, x, y, 4) * 0.3 + noise(seed + 2, x, y, 2) * 0.1;
            int c = n < 0.38 ? mix(dark, base, n / 0.38) : n > 0.66 ? mix(base, light, (n - 0.66) / 0.34) : base;
            if (i.r.nextDouble() < grain) c = i.r.nextBoolean() ? shade(c, 0.86) : shade(c, 1.1);
            i.set(x, y, c);
        }
        return i;
    }

    static void stones() throws Exception {
        // Karst limestone: pale grey with vertical black and ochre streaks where rain runs down.
        Img lime = rock("karst_limestone", rgb(0xB4B3A8), rgb(0x8E8D84), rgb(0xCFCEC4), 0.12);
        for (int k = 0; k < 3; k++) {
            int x = lime.r.nextInt(S), len = 5 + lime.r.nextInt(9), y0 = lime.r.nextInt(S);
            int c = k == 0 ? rgb(0x6E6C66) : k == 1 ? rgb(0x7A7469) : rgb(0xA08A62);
            for (int y = 0; y < len; y++) lime.set(x + (y % 7 == 6 ? 1 : 0), (y0 + y) % S, mix(lime.get(x, y0 + y), c, 0.7));
        }
        lime.save("karst_limestone");
        Img mossy = rock("mossy_karst_limestone", rgb(0xB4B3A8), rgb(0x8E8D84), rgb(0xCFCEC4), 0.12);
        long ms = 99;
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double m = noise(ms, x, y, 4) + (y < 6 ? 0.18 : 0);
            if (m > 0.62) mossy.set(x, y, mossy.pick(rgb(0x4F7432), rgb(0x5E8A3A), rgb(0x6E9A44), rgb(0x46672C)));
        }
        mossy.save("mossy_karst_limestone");
        // Huangshan pale granite: buff-grey with black mica, white quartz and pinkish feldspar grains.
        Img gran = rock("pale_granite", rgb(0xC2BCB0), rgb(0xA49E92), rgb(0xD6D1C6), 0.05);
        for (int k = 0; k < 26; k++) {
            int x = gran.r.nextInt(S), y = gran.r.nextInt(S);
            gran.set(x, y, gran.pick(rgb(0x4A4744), rgb(0x5C5853), rgb(0xE8E4DA), rgb(0xD2B9A6)));
        }
        gran.save("pale_granite");
        // Wulingyuan quartz sandstone: horizontal bedding in tan, buff and rust.
        Img sand = new Img("pillar_sandstone");
        int[] beds = {rgb(0xC9A877), rgb(0xD8BE8E), rgb(0xB8925F), rgb(0xCDB083), rgb(0xA67C4E)};
        int bed = 0;
        for (int y = 0; y < S; y++) {
            if (y % 4 == 0) bed = sand.r.nextInt(beds.length);
            for (int x = 0; x < S; x++) {
                int c = beds[bed];
                if (y % 4 == 3) c = shade(c, 0.82);
                if (sand.r.nextDouble() < 0.12) c = shade(c, sand.r.nextBoolean() ? 0.9 : 1.08);
                sand.set(x, y, c);
            }
        }
        sand.save("pillar_sandstone");
        Img sandTop = rock("pillar_sandstone_top", rgb(0xCDB083), rgb(0xB8925F), rgb(0xDDC596), 0.1);
        sandTop.save("pillar_sandstone_top");
        // Loess: buff fine silt with tiny calcareous nodules; red loess: fossil soil band.
        Img loess = rock("loess", rgb(0xC8A86B), rgb(0xB38F55), rgb(0xD8BE86), 0.22);
        for (int k = 0; k < 4; k++) loess.set(loess.r.nextInt(S), loess.r.nextInt(S), rgb(0xE6D6AE));
        loess.save("loess");
        Img red = rock("red_loess", rgb(0xA8603E), rgb(0x8C4C31), rgb(0xBD7652), 0.22);
        for (int k = 0; k < 5; k++) red.set(red.r.nextInt(S), red.r.nextInt(S), rgb(0xD6B48A));
        red.save("red_loess");
        // Gobi gravel: dark desert-varnished pebbles packed in pale sand.
        Img gobi = new Img("gobi_gravel");
        gobi.fill(rgb(0xB89B70));
        for (int k = 0; k < 30; k++) {
            int cx = gobi.r.nextInt(S), cy = gobi.r.nextInt(S), rr = 1 + gobi.r.nextInt(2);
            int c = gobi.pick(rgb(0x3E322A), rgb(0x55443A), rgb(0x6B5544), rgb(0x2E2620), rgb(0x7C6650));
            for (int dy = -rr; dy <= rr; dy++) for (int dx = -rr; dx <= rr; dx++) {
                if (dx * dx + dy * dy > rr * rr + 1) continue;
                int px = Math.floorMod(cx + dx, S), py = Math.floorMod(cy + dy, S);
                gobi.set(px, py, dy < 0 && dx < 0 ? shade(c, 1.35) : dy > 0 ? shade(c, 0.8) : c);
            }
        }
        gobi.save("gobi_gravel");
    }

    // ------------------------------------------------------------------ trees
    record Tree(String name, int barkDark, int barkMid, int barkLight, String barkStyle, int wood, int woodDark) { }

    static void bark(Tree t) throws Exception {
        Img i = new Img(t.name + "_log");
        long seed = t.name.hashCode();
        // Vertical grain everywhere: stretched noise, so bark reads as fibres and ridges, not blotches.
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double n = noise2(seed, x, y, 2, 8) * 0.75 + noise2(seed + 5, x, y, 1, 4) * 0.25;
            i.set(x, y, n < 0.36 ? t.barkDark : n > 0.64 ? t.barkLight : t.barkMid);
        }
        switch (t.barkStyle) {
            case "fissured" -> { // deep continuous vertical furrows with light ridge edges (willow, jujube, poplar)
                for (int k = 0; k < 4; k++) {
                    int x = 1 + k * 4 + i.r.nextInt(2);
                    for (int y = 0; y < S; y++) {
                        if (i.r.nextInt(6) == 0) x = Math.floorMod(x + (i.r.nextBoolean() ? 1 : -1), S);
                        i.set(x, y, shade(t.barkDark, 0.8));
                        i.set(x + 1, y, t.barkLight);
                    }
                }
            }
            case "plated" -> { // pine: stacked irregular plates, orange-brown, outlined in dark cracks
                int y = 0;
                while (y < S) {
                    int hgt = 3 + i.r.nextInt(3), x = i.r.nextInt(4);
                    while (x < S + 4) {
                        int w = 3 + i.r.nextInt(3);
                        int tone = i.pick(t.barkMid, t.barkLight, mix(t.barkMid, t.barkLight, 0.5), shade(t.barkMid, 0.9));
                        int dy = i.r.nextInt(3) - 1; // plates do not line up: no brick pattern
                        for (int yy = y + dy; yy < y + dy + hgt; yy++) for (int xx = x; xx < x + w; xx++) {
                            boolean edge = (yy == y + dy && i.r.nextInt(4) > 0) || (xx == x && i.r.nextInt(3) > 0);
                            i.set(Math.floorMod(xx, S), Math.floorMod(yy, S), edge ? t.barkDark : (i.r.nextInt(5) == 0 ? shade(tone, 0.9) : tone));
                        }
                        x += w;
                    }
                    y += hgt;
                }
            }
            case "smooth" -> { // osmanthus, banyan, dove tree: smooth bark, faint ridges, light lenticels
                for (int k = 0; k < 2; k++) {
                    int x = i.r.nextInt(S);
                    for (int yy = 0; yy < S; yy++) i.set(x, yy, mix(i.get(x, yy), t.barkDark, 0.5));
                }
                for (int k = 0; k < 7; k++) { int x = i.r.nextInt(S - 1), yy = i.r.nextInt(S); i.set(x, yy, t.barkLight); i.set(x + 1, yy, t.barkLight); }
            }
            case "banded" -> { // peach: glossy dark red bark with horizontal lenticel dashes
                for (int k = 0; k < 7; k++) {
                    int yy = i.r.nextInt(S), x = i.r.nextInt(S), len = 2 + i.r.nextInt(3);
                    for (int q = 0; q < len; q++) i.set((x + q) % S, yy, t.barkLight);
                }
            }
            default -> { }
        }
        i.save(t.name + "_log");
        // End grain: wood with growth rings, bark rim.
        Img top = new Img(t.name + "_log_top");
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double d = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
            double r = Math.hypot(x - 7.5, y - 7.5);
            int c;
            if (d > 6.6) c = (x + y) % 3 == 0 ? t.barkDark : t.barkMid;
            else c = ((int) Math.round(r)) % 2 == 0 ? t.wood : mix(t.wood, t.woodDark, 0.55);
            if (r < 1.2) c = t.woodDark;
            top.set(x, y, c);
        }
        top.save(t.name + "_log_top");
        // Planks: four boards, grain lines, nail-dark seams.
        Img pl = new Img(t.name + "_planks");
        for (int y = 0; y < S; y++) {
            int board = y / 4;
            int shift = board % 2 == 0 ? 0 : 8;
            for (int x = 0; x < S; x++) {
                double g = noise(seed + board, x, 0, 4);
                int c = g > 0.6 ? mix(t.wood, t.woodDark, 0.25) : g < 0.3 ? shade(t.wood, 1.06) : t.wood;
                if (y % 4 == 3) c = mix(t.wood, t.woodDark, 0.85);
                if ((x + shift) % 16 == 0 && y % 4 != 3) c = mix(t.wood, t.woodDark, 0.6);
                if (pl.r.nextDouble() < 0.06) c = mix(c, t.woodDark, 0.3);
                pl.set(x, y, c);
            }
        }
        pl.save(t.name + "_planks");
    }

    /** Leaf mass: clustered leaflets with a few gaps, then the species' own flowers or fruit. */
    static Img leafMass(String name, int dark, int mid, int light, double gaps) {
        Img i = new Img(name);
        long seed = name.hashCode();
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double n = noise(seed, x, y, 4) * 0.7 + i.r.nextDouble() * 0.3;
            if (n < gaps) continue; // see-through gap
            i.set(x, y, n < 0.45 ? dark : n > 0.72 ? light : mid);
        }
        return i;
    }

    static void leaves() throws Exception {
        Img osm = leafMass("osmanthus_leaves", rgb(0x24452A), rgb(0x2F5A2E), rgb(0x45773A), 0.16);
        for (int k = 0; k < 7; k++) { // tight clusters of tiny golden flowers in the leaf axils
            int x = osm.r.nextInt(S), y = osm.r.nextInt(S);
            osm.set(x, y, rgb(0xF2C55C)); osm.set(x + 1, y, rgb(0xE8A93A)); if (osm.r.nextBoolean()) osm.set(x, y + 1, rgb(0xD98E2B));
        }
        osm.save("osmanthus_leaves");
        leafMass("banyan_leaves", rgb(0x2C5A26), rgb(0x3B6E2E), rgb(0x5A9142), 0.12).save("banyan_leaves");
        Img pine = new Img("huangshan_pine_leaves"); // needle tufts: short strokes radiating
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) if (noise(77, x, y, 4) > 0.3) pine.set(x, y, rgb(0x223A28));
        for (int k = 0; k < 34; k++) {
            int cx = pine.r.nextInt(S), cy = pine.r.nextInt(S);
            int c = pine.pick(rgb(0x2C4A33), rgb(0x3B5E40), rgb(0x24402B), rgb(0x4C7050));
            for (int a = 0; a < 5; a++) {
                double ang = pine.r.nextDouble() * Math.PI * 2;
                pine.line(cx, cy, cx + (int) Math.round(Math.cos(ang) * 2.4), cy + (int) Math.round(Math.sin(ang) * 2.4), c);
            }
        }
        pine.save("huangshan_pine_leaves");
        Img juj = leafMass("jujube_leaves", rgb(0x3E6E26), rgb(0x5C8F32), rgb(0x79AD43), 0.24);
        for (int k = 0; k < 5; k++) { // glossy red dates
            int x = juj.r.nextInt(S - 1), y = juj.r.nextInt(S - 2);
            juj.set(x, y, rgb(0xC0392B)); juj.set(x, y + 1, rgb(0x9E2A1E)); juj.set(x + 1, y, rgb(0xE06650));
        }
        juj.save("jujube_leaves");
        Img dove = leafMass("dove_tree_leaves", rgb(0x3F7230), rgb(0x5F9A45), rgb(0x7BB45A), 0.15);
        for (int k = 0; k < 4; k++) { // pairs of white hanging bracts, the "doves"
            int x = dove.r.nextInt(S - 2), y = dove.r.nextInt(S - 4);
            for (int d = 0; d < 3; d++) { dove.set(x, y + d, rgb(0xF3F1E6)); dove.set(x + 1, y + d + 1, rgb(0xDAD8CC)); }
        }
        dove.save("dove_tree_leaves");
        Img wil = new Img("willow_leaves"); // slender leaves hanging in vertical strokes
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) if (noise(78, x, y, 4) > 0.35) wil.set(x, y, rgb(0x5B8538));
        for (int k = 0; k < 44; k++) {
            int x = wil.r.nextInt(S), y = wil.r.nextInt(S), len = 2 + wil.r.nextInt(3);
            int c = wil.pick(rgb(0x6E9A45), rgb(0x8DB45A), rgb(0xA7C66A), rgb(0x5B8538));
            for (int s = 0; s < len; s++) wil.set(x, (y + s) % S, c);
        }
        wil.save("willow_leaves");
        Img peach = leafMass("peach_leaves", rgb(0xC94F7E), rgb(0xE86A9A), rgb(0xF6A3C2), 0.18);
        for (int k = 0; k < 6; k++) { int x = peach.r.nextInt(S), y = peach.r.nextInt(S); peach.set(x, y, rgb(0xFFFFFF)); } // petal highlights
        for (int k = 0; k < 4; k++) { int x = peach.r.nextInt(S), y = peach.r.nextInt(S); peach.set(x, y, rgb(0x6E9A45)); } // first leaves
        for (int k = 0; k < 3; k++) { int x = peach.r.nextInt(S), y = peach.r.nextInt(S); peach.set(x, y, rgb(0x7A2E44)); } // flower hearts
        peach.save("peach_leaves");
        Img pop = leafMass("desert_poplar_leaves", rgb(0xB7861E), rgb(0xD8A92E), rgb(0xEFCB57), 0.22);
        for (int k = 0; k < 6; k++) pop.set(pop.r.nextInt(S), pop.r.nextInt(S), rgb(0x8C8A4A)); // a few late olive leaves
        pop.save("desert_poplar_leaves");
        Img moso = new Img("moso_bamboo_leaves"); // feathery lance leaves in fans
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) if (noise(79, x, y, 4) > 0.38) moso.set(x, y, rgb(0x3E6A26));
        for (int k = 0; k < 30; k++) {
            int x = moso.r.nextInt(S), y = moso.r.nextInt(S);
            int c = moso.pick(rgb(0x4E7F2E), rgb(0x5E8F3A), rgb(0x7FAF4A), rgb(0x3E6A26));
            int dx = moso.r.nextBoolean() ? 1 : -1;
            for (int s = 0; s < 4; s++) moso.set(x + s * dx, y + s / 2, c);
        }
        moso.save("moso_bamboo_leaves");
    }

    // ------------------------------------------------------------------ plants (transparent sprites)
    static void saplings() throws Exception {
        sapling("osmanthus_sapling", rgb(0x6A5E50), rgb(0x2F5A2E), rgb(0xE8A93A));
        sapling("banyan_sapling", rgb(0x8A8478), rgb(0x3B6E2E), 0);
        sapling("huangshan_pine_sapling", rgb(0x6B4A34), rgb(0x2C4A33), 0);
        sapling("jujube_sapling", rgb(0x4A3426), rgb(0x5C8F32), rgb(0xC0392B));
        sapling("dove_tree_sapling", rgb(0x6E6458), rgb(0x5F9A45), rgb(0xF3F1E6));
        sapling("willow_sapling", rgb(0x5F5A50), rgb(0x8DB45A), 0);
        sapling("peach_sapling", rgb(0x5A2F2A), rgb(0xE86A9A), rgb(0xF6A3C2));
        sapling("desert_poplar_sapling", rgb(0x76705A), rgb(0xD8A92E), 0);
    }

    static void sapling(String name, int stem, int leaf, int accent) throws Exception {
        Img i = new Img(name);
        i.line(8, 15, 8, 7, stem);
        i.line(8, 11, 5, 8, stem);
        i.line(8, 9, 11, 6, stem);
        int[][] tufts = {{5, 7}, {11, 5}, {8, 4}, {4, 9}, {12, 8}};
        for (int[] t : tufts) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            if (Math.abs(dx) + Math.abs(dy) > 1 && i.r.nextBoolean()) continue;
            i.set(t[0] + dx, t[1] + dy, dy < 0 ? shade(leaf, 1.15) : leaf);
        }
        if (accent != 0) for (int k = 0; k < 3; k++) i.set(4 + i.r.nextInt(9), 3 + i.r.nextInt(6), accent);
        i.save(name);
    }

    /** A leaf blade from a base point, curving: used for ferns, grasses, bamboo. */
    static void blade(Img i, int x0, int y0, double angle, int len, double curve, int c) {
        double x = x0, y = y0, a = angle;
        for (int s = 0; s < len; s++) {
            i.set((int) Math.round(x), (int) Math.round(y), c);
            x += Math.cos(a); y -= Math.sin(a);
            a += curve;
        }
    }

    static void plants() throws Exception {
        // Karst fern: arching pinnate fronds.
        Img fern = new Img("karst_fern");
        for (int f = 0; f < 5; f++) {
            double a = Math.PI / 2 + (f - 2) * 0.45;
            int c = fern.pick(rgb(0x3E7A30), rgb(0x4F8A3A), rgb(0x2F6626));
            double x = 8, y = 15;
            for (int s = 0; s < 11; s++) {
                fern.set((int) Math.round(x), (int) Math.round(y), c);
                if (s > 2 && s % 2 == 0) { fern.set((int) Math.round(x) + 1, (int) Math.round(y), shade(c, 1.2)); fern.set((int) Math.round(x) - 1, (int) Math.round(y), shade(c, 0.85)); }
                x += Math.cos(a); y -= Math.sin(a); a += (a > Math.PI / 2 ? 0.08 : -0.08);
            }
        }
        fern.save("karst_fern");
        // Rhododendron: woody base with leathery leaves, crown of pink-white trusses.
        Img rhB = new Img("rhododendron_bottom");
        rhB.line(8, 15, 8, 4, rgb(0x5A4636)); rhB.line(8, 10, 4, 5, rgb(0x5A4636)); rhB.line(8, 9, 12, 4, rgb(0x5A4636));
        for (int k = 0; k < 9; k++) { int x = 3 + rhB.r.nextInt(11), y = 2 + rhB.r.nextInt(10); rhB.set(x, y, rgb(0x2E5A2E)); rhB.set(x + 1, y, rgb(0x3F7038)); }
        rhB.save("rhododendron_bottom");
        Img rhT = new Img("rhododendron_top");
        rhT.line(8, 15, 8, 10, rgb(0x5A4636));
        for (int k = 0; k < 6; k++) { int x = 3 + rhT.r.nextInt(11), y = 9 + rhT.r.nextInt(6); rhT.set(x, y, rgb(0x2E5A2E)); rhT.set(x + 1, y, rgb(0x3F7038)); }
        for (int[] c : new int[][]{{5, 6}, {11, 5}, {8, 3}}) for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
            if (dx * dx + dy * dy > 5) continue;
            rhT.set(c[0] + dx, c[1] + dy, rhT.pick(rgb(0xF2B6C9), rgb(0xE58BA8), rgb(0xFBE8EE), rgb(0xD96D92)));
        }
        rhT.save("rhododendron_top");
        // Sea buckthorn: silvery willow-like leaves, branches thick with orange berries.
        Img sb = new Img("sea_buckthorn");
        sb.line(8, 15, 6, 3, rgb(0x5C4A3A)); sb.line(7, 10, 12, 4, rgb(0x5C4A3A)); sb.line(7, 8, 3, 5, rgb(0x5C4A3A));
        for (int k = 0; k < 16; k++) { int x = 2 + sb.r.nextInt(12), y = 1 + sb.r.nextInt(11); sb.set(x, y, sb.pick(rgb(0x9DAF96), rgb(0xB9C6B0), rgb(0x7F9478))); }
        for (int k = 0; k < 11; k++) { int x = 3 + sb.r.nextInt(10), y = 3 + sb.r.nextInt(9); sb.set(x, y, k % 3 == 0 ? rgb(0xFFB04A) : rgb(0xF28C1A)); }
        sb.save("sea_buckthorn");
        // Feather grass: thin stalks with silver plumes bending in the wind.
        Img fg = new Img("feather_grass");
        for (int k = 0; k < 6; k++) {
            int x = 3 + k * 2;
            blade(fg, x, 15, Math.PI / 2 - 0.15 + fg.r.nextDouble() * 0.3, 7 + fg.r.nextInt(4), -0.04, rgb(0xA7A673));
        }
        for (int k = 0; k < 4; k++) blade(fg, 4 + k * 3, 6 + fg.r.nextInt(3), Math.PI / 2 + 0.6, 6, -0.18, fg.pick(rgb(0xE4E2CF), rgb(0xD0CEB6)));
        fg.save("feather_grass");
        // Bamboo shoot: a sheathed brown cone with a green tip.
        Img bs = new Img("bamboo_shoot");
        for (int y = 5; y < 16; y++) {
            int w = Math.max(1, (y - 4) / 3);
            for (int x = 8 - w; x <= 8 + w; x++) bs.set(x, y, (y + x) % 4 == 0 ? rgb(0x5E4128) : (x < 8 ? rgb(0x7A5A36) : rgb(0xA27F4E)));
        }
        bs.set(8, 4, rgb(0x6E9A45)); bs.set(8, 3, rgb(0x8DB45A)); bs.set(9, 2, rgb(0x8DB45A));
        bs.save("bamboo_shoot");
        // Arrow bamboo (panda bamboo): dense thin culms with drooping leaves.
        Img ab = new Img("arrow_bamboo");
        for (int k = 0; k < 3; k++) {
            int x0 = 4 + k * 4, lean = k == 1 ? 0 : (k == 0 ? -2 : 2);
            ab.line(x0, 15, x0 + lean, 2, rgb(0x7A9A44));
            for (int y = 4; y < 13; y += 4) {
                int x = x0 + lean * (15 - y) / 13;
                blade(ab, x, y, 0.5, 5, -0.22, ab.pick(rgb(0x5E8A3A), rgb(0x4E7A2E), rgb(0x6E9A45)));
                blade(ab, x, y + 1, Math.PI - 0.5, 5, 0.22, ab.pick(rgb(0x5E8A3A), rgb(0x4E7A2E), rgb(0x6E9A45)));
            }
        }
        ab.save("arrow_bamboo");
        // Tree fern: shaggy brown trunk, crown of long arching fronds.
        Img tfB = new Img("tree_fern_bottom");
        for (int y = 0; y < 16; y++) for (int x = 6; x <= 9; x++) tfB.set(x, y, (x + y * 3) % 5 == 0 ? rgb(0x6B513A) : (x == 6 ? rgb(0x3A2C20) : rgb(0x4B3A2A)));
        tfB.save("tree_fern_bottom");
        Img tfT = new Img("tree_fern_top");
        for (int y = 9; y < 16; y++) for (int x = 6; x <= 9; x++) tfT.set(x, y, x == 6 ? rgb(0x3A2C20) : rgb(0x4B3A2A));
        for (int f = 0; f < 6; f++) {
            double a = (f < 3 ? Math.PI - 0.2 - f * 0.35 : 0.2 + (f - 3) * 0.35);
            double x = 7.5, y = 9;
            for (int s = 0; s < 9; s++) {
                int px = (int) Math.round(x), py = (int) Math.round(y);
                tfT.set(px, py, rgb(0x3E7A30));
                if (s > 1) { tfT.set(px, py - 1, rgb(0x5A9A40)); tfT.set(px, py + 1, rgb(0x2F6626)); }
                x += Math.cos(a); y -= Math.sin(a) * 0.6; a += (a > Math.PI / 2 ? 0.12 : -0.12);
            }
        }
        tfT.save("tree_fern_top");
        // Lotus leaf: round, bluish green, pale radiating veins from the centre, a notch.
        Img lp = new Img("lotus_pad");
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double dx = x - 7.5, dy = y - 7.5, r = Math.hypot(dx, dy);
            if (r > 7.6) continue;
            double a = Math.atan2(dy, dx);
            if (r > 2 && Math.abs(a - 1.2) < 0.18) continue; // notch
            int c = r > 6.6 ? rgb(0x4E8A36) : rgb(0x5E9A3A);
            if (Math.abs(Math.sin(a * 5)) < 0.12 && r > 1.5) c = rgb(0x86BC5A);
            if (r < 1.3) c = rgb(0x9CCB6E);
            lp.set(x, y, c);
        }
        lp.save("lotus_pad");
        // Lotus flower: cup of pink petals, white-tipped, yellow seed head.
        Img lf = new Img("lotus_flower");
        int[][] petals = {{4, 8, 3}, {11, 8, 3}, {6, 5, 3}, {9, 5, 3}, {7, 3, 2}};
        for (int[] p : petals) for (int dy = 0; dy <= p[2] + 2; dy++) for (int dx = -1; dx <= 1; dx++) {
            int y = p[1] + dy;
            if (y > 12) continue;
            lf.set(p[0] + dx, y, dy == 0 ? rgb(0xFFF0F4) : dx == 0 ? rgb(0xF4A0B9) : rgb(0xE66E93));
        }
        for (int x = 6; x <= 9; x++) lf.set(x, 11, rgb(0xF2C94A));
        lf.line(7, 12, 8, 15, rgb(0x4E8A36));
        lf.save("lotus_flower");
        // Wild rice (jiaobai): tall grassy blades, top half carries a loose seed panicle.
        Img wrB = new Img("wild_rice_bottom");
        for (int k = 0; k < 6; k++) blade(wrB, 3 + k * 2, 15, Math.PI / 2 + (wrB.r.nextDouble() - 0.5) * 0.4, 16, (wrB.r.nextDouble() - 0.5) * 0.04, wrB.pick(rgb(0x6E9A45), rgb(0x7FAA4E), rgb(0x5B8538)));
        wrB.save("wild_rice_bottom");
        Img wrT = new Img("wild_rice_top");
        for (int k = 0; k < 5; k++) blade(wrT, 3 + k * 2, 15, Math.PI / 2 + (wrT.r.nextDouble() - 0.5) * 0.5, 9, (k - 2) * 0.03, wrT.pick(rgb(0x6E9A45), rgb(0x7FAA4E)));
        for (int k = 0; k < 10; k++) wrT.set(6 + wrT.r.nextInt(5), 1 + wrT.r.nextInt(6), wrT.pick(rgb(0xC8B46A), rgb(0xA89450), rgb(0xD9C88A)));
        wrT.save("wild_rice_top");
        // Chrysanthemum: dense golden many-petalled head on a leafy stem (Tao Yuanming's flower).
        Img ch = new Img("chrysanthemum");
        ch.line(8, 15, 8, 7, rgb(0x4E7A30));
        ch.set(7, 12, rgb(0x5E8A3A)); ch.set(6, 11, rgb(0x5E8A3A)); ch.set(9, 10, rgb(0x5E8A3A)); ch.set(10, 9, rgb(0x5E8A3A));
        for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
            double r = Math.hypot(dx, dy);
            if (r > 3.3) continue;
            ch.set(8 + dx, 5 + dy, r < 1.2 ? rgb(0xB9771A) : ((dx + dy) % 2 == 0 ? rgb(0xF2C232) : rgb(0xE9A526)));
        }
        ch.save("chrysanthemum");
        // Rapeseed: tall stem, bright yellow four-petalled clusters.
        Img rp = new Img("rapeseed");
        rp.line(7, 15, 7, 4, rgb(0x5E8A3A)); rp.line(7, 9, 11, 5, rgb(0x5E8A3A)); rp.line(7, 11, 4, 8, rgb(0x5E8A3A));
        for (int[] c : new int[][]{{7, 3}, {11, 4}, {4, 7}}) for (int dy = -1; dy <= 1; dy++) for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) + Math.abs(dy) > 2) continue;
            rp.set(c[0] + dx, c[1] + dy, rp.r.nextInt(4) == 0 ? rgb(0xE8B020) : rgb(0xF2D43A));
        }
        rp.save("rapeseed");
        // Tamarisk: red-brown twiggy base with feathery grey-green foliage; top half pink flower plumes.
        Img tmB = new Img("tamarisk_bottom");
        tmB.line(8, 15, 6, 0, rgb(0x7A3F2E)); tmB.line(8, 15, 11, 1, rgb(0x7A3F2E)); tmB.line(7, 9, 3, 2, rgb(0x7A3F2E));
        for (int k = 0; k < 26; k++) tmB.set(2 + tmB.r.nextInt(12), tmB.r.nextInt(12), tmB.pick(rgb(0x8FA27E), rgb(0xA6B596), rgb(0x76896A)));
        tmB.save("tamarisk_bottom");
        Img tmT = new Img("tamarisk_top");
        tmT.line(6, 15, 5, 6, rgb(0x7A3F2E)); tmT.line(11, 15, 12, 5, rgb(0x7A3F2E)); tmT.line(8, 15, 8, 4, rgb(0x7A3F2E));
        for (int k = 0; k < 40; k++) {
            int x = 2 + tmT.r.nextInt(12), y = 1 + tmT.r.nextInt(10);
            tmT.set(x, y, y < 6 ? tmT.pick(rgb(0xE59AB8), rgb(0xF2B9CF), rgb(0xD2799F)) : tmT.pick(rgb(0x8FA27E), rgb(0xE59AB8)));
        }
        tmT.save("tamarisk_top");
        // Saxaul: contorted brown branches, jointed green twigs instead of leaves.
        Img sx = new Img("saxaul");
        sx.line(8, 15, 7, 10, rgb(0x6B5136)); sx.line(7, 10, 4, 6, rgb(0x6B5136)); sx.line(7, 10, 10, 7, rgb(0x6B5136)); sx.line(10, 7, 12, 3, rgb(0x6B5136));
        for (int k = 0; k < 9; k++) {
            int x = 3 + sx.r.nextInt(10), y = 1 + sx.r.nextInt(8);
            for (int s = 0; s < 3; s++) sx.set(x + (s % 2), y + s, s == 1 ? rgb(0x5F7A3A) : rgb(0x7F9A4E));
        }
        sx.save("saxaul");
        // Cistanche: a fleshy yellow-brown spike with scales, purple flowers on the upper part (desert ginseng).
        Img ci = new Img("cistanche");
        for (int y = 3; y < 16; y++) for (int x = 6; x <= 9; x++) {
            int c = (x + y) % 3 == 0 ? rgb(0x9C7432) : rgb(0xC49A4A);
            if (y < 9 && (x + y) % 2 == 0) c = ci.pick(rgb(0x8C6AAE), rgb(0xA586C4));
            if (x == 6) c = shade(c, 0.8);
            ci.set(x, y, c);
        }
        ci.set(7, 2, rgb(0xA586C4)); ci.set(8, 2, rgb(0x8C6AAE));
        ci.save("cistanche");
        // Banyan aerial roots: thin brown strands of different lengths.
        Img br = new Img("banyan_roots");
        for (int k = 0; k < 5; k++) {
            int x = 2 + k * 3, len = 10 + br.r.nextInt(7);
            for (int y = 0; y < Math.min(16, len); y++) br.set(x + (y > 8 && k % 2 == 0 ? 1 : 0), y, y % 5 == 4 ? rgb(0x8A7458) : rgb(0x6E5A44));
        }
        br.save("banyan_roots");
        // Willow strands: hanging cords of slender leaves.
        Img ws = new Img("willow_strands");
        for (int k = 0; k < 5; k++) {
            int x = 2 + k * 3;
            for (int y = 0; y < 16; y++) {
                ws.set(x, y, rgb(0x6E8A45));
                if (y % 3 == 1) { ws.set(x - 1, y, ws.pick(rgb(0x8DB45A), rgb(0xA7C66A))); ws.set(x + 1, y + 1, ws.pick(rgb(0x8DB45A), rgb(0x7FA650))); }
            }
        }
        ws.save("willow_strands");
        // Giant bamboo culm: green with pale waxy bloom under each node; top: hollow ring.
        Img mb = new Img("moso_bamboo");
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            int c = x < 6 ? rgb(0x5C7E30) : x > 10 ? rgb(0x7FA048) : rgb(0x6E8F3A);
            if (y == 0 || y == 8) c = rgb(0x8FAA58);
            if (y == 1 || y == 9) c = mix(c, rgb(0xD5DCC4), 0.55);
            if (y == 15 || y == 7) c = shade(c, 0.85);
            mb.set(x, y, c);
        }
        mb.save("moso_bamboo");
        Img mbt = new Img("moso_bamboo_top");
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            double r = Math.hypot(x - 7.5, y - 7.5);
            mbt.set(x, y, r < 2.0 ? rgb(0xD8D2B0) : r < 3.2 ? rgb(0xB8B08A) : rgb(0x6E8F3A));
        }
        mbt.save("moso_bamboo_top");
    }

    public static void main(String[] args) throws Exception {
        out = new File(args.length > 0 ? args[0] : "src/main/resources/assets/murimblock/textures/block");
        out.mkdirs();
        System.out.println("Murim nature textures -> " + out);
        stones();
        for (Tree t : new Tree[]{
                new Tree("osmanthus", rgb(0x5E574E), rgb(0x7C7468), rgb(0x9A9284), "smooth", rgb(0xD2BE8E), rgb(0xA8915E)),
                new Tree("banyan", rgb(0x6E685C), rgb(0x9A9488), rgb(0xB4AE9F), "smooth", rgb(0xC2A57C), rgb(0x8E7450)),
                new Tree("huangshan_pine", rgb(0x3E2A1E), rgb(0x6B4A34), rgb(0x8C5E3C), "plated", rgb(0xC08A55), rgb(0x8C5A34)),
                new Tree("jujube", rgb(0x2E2018), rgb(0x4A3426), rgb(0x66482F), "fissured", rgb(0x9A5A3A), rgb(0x6E3A24)),
                new Tree("dove_tree", rgb(0x4E463E), rgb(0x6E6458), rgb(0x8A8070), "smooth", rgb(0xC9B89A), rgb(0x9C8A6A)),
                new Tree("willow", rgb(0x3E3A34), rgb(0x5F5A50), rgb(0x7A7466), "fissured", rgb(0xCDBE92), rgb(0x9E8E62)),
                new Tree("peach", rgb(0x3E1E1C), rgb(0x5A2F2A), rgb(0x8A5A50), "banded", rgb(0xC08A70), rgb(0x8E5A46)),
                new Tree("desert_poplar", rgb(0x4E4A3A), rgb(0x76705A), rgb(0x948C70), "fissured", rgb(0xC4B68C), rgb(0x968858))}) {
            bark(t);
        }
        leaves();
        saplings();
        plants();
    }
}
