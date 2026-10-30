package com.murimblock.client.gui;

import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Pixel art drawn one GUI pixel at a time, shared by the combat bar and the System menu (look B,
 * {@code docs/gui/boards/combat-bar-b.html} and {@code system-gui-pixel.html}, whose
 * canvas code these shapes follow): the board's palette, its 3 x 5 font, diamonds, octagons and sprites.
 */
public final class PixelArt {
    // The board's palette (P in combat-bar-b.html).
    public static final int OUTLINE = 0xFF05101B, DEEP = 0xFF0D2238, NIGHT = 0xFF123250, MID = 0xFF1D4A73, CYAN = 0xFF3FB4EC,
            LIGHT = 0xFF7FD6FF, WHITE = 0xFFE9FBFF, GOLD_DARK = 0xFF5A3D10, GOLD = 0xFFC98D2A, GOLD_LIGHT = 0xFFFFD36B,
            GOLD_PALE = 0xFFFFF1B8, ROSE = 0xFFFF8C9A, ROSE_DARK = 0xFF7A2A36, STEEL = 0xFF6B8AA3, SLATE = 0xFF24384C;

    /** The 3 x 5 letters, read row by row. */
    private static final Map<Character, String> FONT = Map.ofEntries(
            Map.entry('A', "010101111101101"), Map.entry('B', "110101110101110"), Map.entry('C', "011100100100011"),
            Map.entry('D', "110101101101110"), Map.entry('E', "111100110100111"), Map.entry('F', "111100110100100"),
            Map.entry('G', "011100101101011"), Map.entry('H', "101101111101101"), Map.entry('I', "111010010010111"),
            Map.entry('J', "001001001101010"), Map.entry('K', "101101110101101"), Map.entry('L', "100100100100111"),
            Map.entry('M', "101111111101101"), Map.entry('N', "110101101101101"), Map.entry('O', "010101101101010"),
            Map.entry('P', "110101110100100"), Map.entry('Q', "010101101110011"), Map.entry('R', "110101110101101"),
            Map.entry('S', "011100010001110"), Map.entry('T', "111010010010010"), Map.entry('U', "101101101101111"),
            Map.entry('V', "101101101101010"), Map.entry('W', "101101111111101"), Map.entry('X', "101101010101101"),
            Map.entry('Y', "101101010010010"), Map.entry('Z', "111001010100111"), Map.entry('0', "111101101101111"),
            Map.entry('1', "010110010010111"), Map.entry('2', "110001010100111"), Map.entry('3', "110001010001110"),
            Map.entry('4', "101101111001001"), Map.entry('5', "111100110001110"), Map.entry('6', "011100111101111"),
            Map.entry('7', "111001010010010"), Map.entry('8', "111101111101111"), Map.entry('9', "111101111001110"),
            Map.entry('/', "001001010100100"), Map.entry('%', "101001010100101"), Map.entry('-', "000000111000000"),
            Map.entry('\'', "010010000000000"));

    private PixelArt() {
    }

    public static void px(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
    }

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    /** Width in pixels of {@code text} in the 3 x 5 font. */
    public static int width(String text) {
        return text.length() * 4 - 1;
    }

    /** Text in the 3 x 5 font (upper case), with a shadow one pixel down and right unless {@code shadow} is 0. */
    public static int text(GuiGraphics g, int x, int y, String text, int color, int shadow) {
        for (int pass = shadow == 0 ? 1 : 0; pass < 2; pass++) {
            int cx = x;
            for (char c : text.toUpperCase().toCharArray()) {
                String glyph = FONT.get(c);
                if (glyph != null) {
                    for (int i = 0; i < 15; i++) {
                        if (glyph.charAt(i) == '1') {
                            px(g, cx + i % 3 + (pass == 0 ? 1 : 0), y + i / 3 + (pass == 0 ? 1 : 0), pass == 0 ? shadow : color);
                        }
                    }
                }
                cx += 4;
            }
        }
        return x + width(text);
    }

    /** A diamond of half-width {@code r} centred on ({@code cx}, {@code cy}). */
    public static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = r - Math.abs(dy);
            rect(g, cx - w, cy + dy, 2 * w + 1, 1, color);
        }
    }

    /** An octagon of half-size {@code r} with corners cut by {@code k}. */
    public static void octagon(GuiGraphics g, int cx, int cy, int r, int k, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int cut = Math.max(0, Math.abs(dy) - (r - k));
            rect(g, cx - r + cut, cy + dy, 2 * (r - cut) + 1, 1, color);
        }
    }

    /** Row {@code dy} of an octagon: its half-width. */
    public static int octagonHalf(int r, int k, int dy) {
        return r - Math.max(0, Math.abs(dy) - (r - k));
    }

    /** A sprite of palette letters ('.' clear), each letter mapped to a colour by {@code tint}. */
    public static void sprite(GuiGraphics g, int x, int y, String[] rows, java.util.function.IntUnaryOperator tint) {
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                char c = rows[j].charAt(i);
                if (c != '.') {
                    px(g, x + i, y + j, tint.applyAsInt(color(c)));
                }
            }
        }
    }

    /** A rectangle with its four corners cut by {@code cut} pixels. */
    public static void cutRect(GuiGraphics g, int x, int y, int w, int h, int cut, int color) {
        for (int j = 0; j < h; j++) {
            int k = Math.max(0, Math.max(cut - j, cut - (h - 1 - j)));
            if (w - 2 * k > 0) {
                rect(g, x + k, y + j, w - 2 * k, 1, color);
            }
        }
    }

    /**
     * The System frame: dark outline, a coloured line, dark outline, then the fill, corners cut by {@code cut}, and a
     * faint shine along the top of the fill.
     */
    public static void frame(GuiGraphics g, int x, int y, int w, int h, int cut, int line, int fill, int shine) {
        cutRect(g, x, y, w, h, cut, OUTLINE);
        cutRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, cut - 1), line);
        cutRect(g, x + 2, y + 2, w - 4, h - 4, Math.max(0, cut - 1), OUTLINE);
        cutRect(g, x + 3, y + 3, w - 6, h - 6, Math.max(0, cut - 2), fill);
        if (shine != 0) {
            rect(g, x + 3 + cut, y + 3, w - 6 - 2 * cut, 1, shine);
        }
    }

    /** A pearl: a small framed diamond with a lit centre, for the corners of a window. */
    public static void pearl(GuiGraphics g, int cx, int cy) {
        diamond(g, cx, cy, 3, OUTLINE);
        diamond(g, cx, cy, 2, CYAN);
        px(g, cx, cy, LIGHT);
        px(g, cx, cy - 1, WHITE);
    }

    /** A diamond check box, lit when {@code on}. */
    public static void check(GuiGraphics g, int cx, int cy, boolean on) {
        diamond(g, cx, cy, 3, OUTLINE);
        diamond(g, cx, cy, 2, on ? CYAN : SLATE);
        if (on) {
            px(g, cx, cy, WHITE);
        }
    }

    /** A divider: a faint line, a dot at each end and a diamond in the middle. */
    public static void divider(GuiGraphics g, int x, int y, int w) {
        rect(g, x + 3, y, w - 6, 1, 0x5A3FB4EC);
        px(g, x, y, CYAN);
        px(g, x + w - 1, y, CYAN);
        diamond(g, x + w / 2, y, 2, OUTLINE);
        diamond(g, x + w / 2, y, 1, LIGHT);
    }

    /**
     * A tube bar {@code len} long and 5 tall: three rows of colour (light, core, shade) filled from the left, a dark
     * notch every tenth, a small diamond at each end.
     */
    public static void tube(GuiGraphics g, int x, int y, int len, float fraction, int light, int core, int shade) {
        int inner = len - 4;
        int filled = Math.round(inner * Math.max(0, Math.min(1, fraction)));
        for (int ex : new int[] {x, x + len - 1}) {
            diamond(g, ex, y + 2, 3, OUTLINE);
            diamond(g, ex, y + 2, 2, shade);
            px(g, ex, y + 2, light);
        }
        rect(g, x + 2, y, inner, 5, OUTLINE);
        rect(g, x + 2, y + 1, inner, 3, DEEP);
        rect(g, x + 2, y + 1, filled, 1, light);
        rect(g, x + 2, y + 2, filled, 1, core);
        rect(g, x + 2, y + 3, filled, 1, shade);
        for (int k = 1; k < 10; k++) {
            rect(g, x + 2 + Math.round(inner * k / 10f), y + 1, 1, 3, OUTLINE);
        }
    }

    /** Faint cloud scrolls tiled over a rectangle, behind a window's content. */
    public static void clouds(GuiGraphics g, int x, int y, int w, int h) {
        String[] cloud = {"..xxx......", ".x...x.xx..", "x..x..x..x.", "x.x.x.x.x.x", ".x...x...x."};
        for (int ty = 0; ty < h - 5; ty += 26) {
            for (int tx = (ty / 26 % 2) * 24; tx < w - 11; tx += 48) {
                for (int j = 0; j < cloud.length; j++) {
                    for (int i = 0; i < cloud[j].length(); i++) {
                        if (cloud[j].charAt(i) == 'x') {
                            px(g, x + tx + i, y + ty + j, 0x123FB4EC);
                        }
                    }
                }
            }
        }
    }

    /** The palette letter's colour, as on the board. */
    public static int color(char c) {
        return switch (c) {
            case 'o' -> OUTLINE;
            case 'd' -> DEEP;
            case 'n' -> NIGHT;
            case 'm' -> MID;
            case 'c' -> CYAN;
            case 'l' -> LIGHT;
            case 'w' -> WHITE;
            case 'G' -> GOLD_DARK;
            case 'g' -> GOLD;
            case 'y' -> GOLD_LIGHT;
            case 'Y' -> GOLD_PALE;
            case 'r' -> ROSE;
            case 'R' -> ROSE_DARK;
            case 's' -> STEEL;
            case 'k' -> SLATE;
            default -> WHITE;
        };
    }
}
