import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Paints the Mountain Bandit's five outfits as 64x64 skins in the player's layout (the base layer and the outer layer:
 * hat, jacket, sleeves, trousers), from the colours and the front view of its sheet. The outer layer carries what only
 * some outfits wear: the wolf's hood, pelt and fur shoulders, the dune turban, the headband's knot; and every outfit's
 * robe skirt over the thighs.
 * Usage: java scripts/worldgen/BanditSkins.java src/main/resources/assets/murimblock/textures/entity/mountain_bandit
 */
public class BanditSkins {
    record Outfit(String key, boolean band, boolean hood, boolean turban, boolean mask, Map<Character, String> c) { }

    static Map<Character, String> palette(String... kv) {
        java.util.HashMap<Character, String> m = new java.util.HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i].charAt(0), kv[i + 1]);
        return m;
    }

    static final Outfit[] OUTFITS = {
            new Outfit("ash", true, false, false, false, palette("H", "#3b3a3f", "A", "#2a2220", "S", "#c69c6d", "E", "#1b1b22", "M", "#9c7a55", "R", "#77716a", "D", "#55504a", "U", "#77716a", "B", "#a08455", "W", "#d3c7ac", "P", "#4a4640", "F", "#5b4430")),
            new Outfit("crimson", true, false, false, false, palette("H", "#a7302a", "A", "#2a2220", "S", "#c99e70", "E", "#1b1b22", "M", "#8f6c4c", "R", "#5e4c42", "D", "#3e332e", "U", "#5e4c42", "B", "#a7302a", "W", "#cbbf9f", "P", "#3b3632", "F", "#4a3626")),
            new Outfit("wolf", false, true, false, false, palette("H", "#8a7a66", "A", "#4a3524", "S", "#bf9265", "E", "#1b1b22", "M", "#5a4030", "R", "#6b4f36", "D", "#4a3524", "U", "#9a8a74", "B", "#2f2620", "W", "#8a7a66", "P", "#4d4136", "F", "#3a2a1c")),
            new Outfit("dune", false, false, true, false, palette("H", "#d9c08f", "A", "#d9c08f", "S", "#b0804f", "E", "#1b1b22", "M", "#c9ad78", "R", "#b88a4e", "D", "#8e6633", "U", "#b88a4e", "B", "#6b3d2a", "W", "#e3d3ae", "P", "#9c7a50", "F", "#5e4129")),
            new Outfit("gilded", true, false, false, true, palette("H", "#1e1b22", "A", "#1e1b22", "S", "#c69c6d", "E", "#120f14", "M", "#d4a637", "R", "#2b2a33", "D", "#17161c", "U", "#2b2a33", "B", "#d4a637", "W", "#4a4652", "P", "#23222a", "F", "#18161a")),
    };

    // The front of the body, the viewer's view (the bandit's right on the left), 16 x 32.
    static final String[] GRID = {
            "....HHHHHHHH....", "....HHHHHHHH....", "....ASSSSSSA....", "....SEESSEES....",
            "....SSSSSSSS....", "....SSMMMMSS....", "....SMMMMMMS....", "....SSSSSSSS....",
            "UUUUDRRRRRRDUUUU", "UUUUDRRDDRRDUUUU", "UUUURRDRRDRRUUUU", "RRRRRRRDDRRRRRRR",
            "RRRRRRRRRRRRRRRR", "RRRRBBBBBBBBRRRR", "WWWWRRRBBRRRWWWW", "WWWWRRRRRRRRWWWW",
            "WWWWDRRRRRRDWWWW", "WWWWDRRRRRRDWWWW", "SSSSDRRRRRRDSSSS", "SSSSDRRRRRRDSSSS",
            "....PPPPPPPP....", "....PPPPPPPP....", "....PPPPPPPP....", "....PPPPPPPP....",
            "....PPPPPPPP....", "....PPPPPPPP....", "....WWWWWWWW....", "....WWWWWWWW....",
            "....WWWWWWWW....", "....WWWWWWWW....", "....FFFFFFFF....", "....FFFFFFFF....",
    };

    static BufferedImage img;
    static Outfit o;
    static int seed;

    /** Paints `letter` (a lower-case letter: its colour a shade darker) at (x, y), with a little grain; null: nothing. */
    static void put(int x, int y, Character letter, double light) {
        if (letter == null || letter == '.') return;
        boolean darker = Character.isLowerCase(letter);
        String hex = o.c().get(Character.toUpperCase(letter));
        int r = Integer.parseInt(hex.substring(1, 3), 16), g = Integer.parseInt(hex.substring(3, 5), 16), b = Integer.parseInt(hex.substring(5, 7), 16);
        double f = light * (darker ? 0.82 : 1) * (0.94 + (((x * 73 + y * 151 + seed * 37) % 7) / 6.0) * 0.1);
        img.setRGB(x, y, 0xFF000000 | (clamp(r * f) << 16) | (clamp(g * f) << 8) | clamp(b * f));
    }

    static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }

    interface Face { Character at(int u, int v, int w, int h); }

    /** A box's six faces in the skin layout, from its corner (x, y) and its size (w wide, h tall, d deep). */
    static void box(int x, int y, int w, int h, int d, Face top, Face bottom, Face right, Face front, Face left, Face back) {
        seed++;
        fill(x + d, y, w, d, top, 1.06);
        fill(x + d + w, y, w, d, bottom, 0.8);
        fill(x, y + d, d, h, right, 0.95);
        fill(x + d, y + d, w, h, front, 1.0);
        fill(x + d + w, y + d, d, h, left, 0.95);
        fill(x + d + w + d, y + d, w, h, back, 0.9);
    }

    static void fill(int x0, int y0, int w, int h, Face f, double light) {
        if (f == null) return;
        for (int v = 0; v < h; v++) for (int u = 0; u < w; u++) put(x0 + u, y0 + v, f.at(u, v, w, h), light);
    }

    static Character grid(int row, int col) { return GRID[row].charAt(col); }
    static Face all(char l) { return (u, v, w, h) -> l; }
    static Face fur() { return (u, v, w, h) -> (u * 7 + v * 13) % 9 == 0 ? 'h' : 'H'; }

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        for (Outfit outfit : OUTFITS) {
            o = outfit;
            seed = 0;
            img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            paint();
            ImageIO.write(img, "png", new File(dir, o.key() + ".png"));
            System.out.println("wrote " + o.key());
        }
    }

    static void paint() {
        // ---- head: the face from the sheet; hair under the headwear rows; an ear on each side.
        Face face = (u, v, w, h) -> {
            char l = grid(v, 4 + u);
            if (o.mask() && v >= 2 && v <= 4) return l == 'E' ? 'E' : 'M';
            if (o.mask() && (v == 5 || v == 6)) return 'H';
            return l;
        };
        // The side faces run from the back to the front (right side) and from the front to the back (left side).
        Face rightSide = (u, v, w, h) -> sideOfHead(w - 1 - u, v);
        Face leftSide = (u, v, w, h) -> sideOfHead(u, v);
        Face headTop = (u, v, w, h) -> o.band() ? (u == 0 || v == 0 || u == w - 1 || v == h - 1 ? 'H' : 'A') : 'H';
        Face headBack = (u, v, w, h) -> v < 2 ? 'H' : (o.band() && (u == 3 || u == 4) && v < 4 ? 'H' : 'A');
        box(0, 0, 8, 8, 8, headTop, all('S'), rightSide, face, leftSide, headBack);

        // ---- hat layer: the hood (open on the face), the turban, or the headband's knot and tails.
        if (o.hood()) {
            Face hoodFront = (u, v, w, h) -> v < 2 || u == 0 || u == w - 1 ? 'h' : null;
            box(32, 0, 8, 8, 8, fur(), null, fur(), hoodFront, fur(), fur());
        } else if (o.turban()) {
            Face wraps = (u, v, w, h) -> v > 3 ? null : ((v + u / 3) % 3 == 2 ? 'h' : 'H');
            Face knot = (u, v, w, h) -> v > 3 ? null : (u >= 3 && u <= 4 ? 'D' : ((v + u / 3) % 3 == 2 ? 'h' : 'H'));
            Face top = (u, v, w, h) -> ((u + v) / 3) % 3 == 2 ? 'h' : 'H';
            box(32, 0, 8, 8, 8, top, null, wraps, knot, wraps, wraps);
        } else if (o.band()) {
            Face tails = (u, v, w, h) -> (u == 3 && v >= 2 && v <= 6) || (u == 4 && v >= 2 && v <= 5) ? 'H' : null;
            box(32, 0, 8, 8, 8, null, null, null, null, null, tails);
        }

        // ---- body: the robe from the sheet, the sash.
        Face bodyFront = (u, v, w, h) -> grid(8 + v, 4 + u);
        Face bodyBack = (u, v, w, h) -> v == 5 ? 'B' : (u == 0 || u == w - 1 ? 'D' : 'R');
        Face bodySide = (u, v, w, h) -> v == 5 ? 'B' : 'R';
        box(16, 16, 8, 12, 4, all('U'), all('P'), bodySide, bodyFront, bodySide, bodyBack);
        // Jacket layer: the wolf's pelt down the back.
        if (o.hood()) box(16, 32, 8, 12, 4, null, null, null, null, null, (u, v, w, h) -> v == h - 1 ? 'h' : ((u * 7 + v * 13) % 9 == 0 ? 'h' : 'H'));

        // ---- arms: the sleeve, the wrapped forearm, the hand. Right arm (base 40,16; sleeve 40,32), left (32,48; 48,48).
        Face arm = (u, v, w, h) -> v < 3 ? 'U' : v < 6 ? 'R' : v < 10 ? (v % 2 == 0 ? 'W' : 'w') : 'S';
        box(40, 16, 4, 12, 4, all('U'), all('S'), arm, arm, arm, arm);
        box(32, 48, 4, 12, 4, all('U'), all('S'), arm, arm, arm, arm);
        if (o.hood()) {
            Face shoulder = (u, v, w, h) -> v < 3 ? ((u * 7 + v * 13) % 9 == 0 ? 'h' : 'H') : null;
            box(40, 32, 4, 12, 4, fur(), null, shoulder, shoulder, shoulder, shoulder);
            box(48, 48, 4, 12, 4, fur(), null, shoulder, shoulder, shoulder, shoulder);
        }

        // ---- legs: trousers, wrapped shins, sandals. Right leg (base 0,16; trousers layer 0,32), left (16,48; 0,48).
        Face leg = (u, v, w, h) -> v < 6 ? 'P' : v < 10 ? (v % 2 == 1 ? 'W' : 'w') : 'F';
        box(0, 16, 4, 12, 4, all('P'), all('F'), leg, leg, leg, leg);
        box(16, 48, 4, 12, 4, all('P'), all('F'), leg, leg, leg, leg);
        // Trousers layer: the robe's skirt over the thighs, open on the inner side, a darker hem.
        Face skirt = (u, v, w, h) -> v > 4 ? null : (v == 4 ? 'D' : (u == 0 || u == w - 1 ? 'D' : 'R'));
        box(0, 32, 4, 12, 4, null, null, skirt, skirt, null, skirt);   // the right leg: its inner side is its left
        box(0, 48, 4, 12, 4, null, null, null, skirt, skirt, skirt);   // the left leg: its inner side is its right
    }

    /** One side of the head, `front` counted from the face: skin at the cheek, an ear, hair behind. */
    static Character sideOfHead(int front, int v) {
        if (v < 2) return 'H';
        if (front == 0) return v >= 5 && v <= 6 ? 'M' : 'S';
        if (front == 2 && (v == 3 || v == 4)) return 'S';
        return v == 7 ? 'S' : 'A';
    }
}
