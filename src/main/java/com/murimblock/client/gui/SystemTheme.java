package com.murimblock.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Colours and drawing helpers of the System look (inspired by Solo Leveling's System windows), drawn as pixel art. */
public final class SystemTheme {
    public static final int PANEL = 0xEB07121F;
    public static final int CYAN = 0xFF63CFFF;
    public static final int CYAN_SOFT = 0x7363CFFF;
    public static final int CYAN_FAINT = 0x3863CFFF;
    public static final int CYAN_FILL = 0x2963CFFF;
    public static final int CORNER = 0xFFD2F3FF;
    public static final int TEXT = 0xFFE6F6FF;
    public static final int TEXT_MUTED = 0xFF8FB8D6;
    public static final int TEXT_ON_CYAN = 0xFF04101D;
    public static final int WARNING = 0xFFFF8C9A;

    private SystemTheme() {
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    /**
     * The System window, in pixel art (board {@code docs/gui/boards/system-gui-pixel.png}): a double frame with cut
     * corners over a translucent night-blue panel, faint cloud scrolls behind the content, filigree in the corners and a
     * pearl at each corner and in the middle of the sides.
     */
    public static void window(GuiGraphics g, SystemLayout.Rect r) {
        int x = r.x(), y = r.y(), w = r.width(), h = r.height();
        PixelArt.frame(g, x, y, w, h, 4, PixelArt.CYAN, 0xF00A1A2C, 0x403FB4EC);
        PixelArt.rect(g, x + 4, y + 4, w - 8, 1, 0x403FB4EC);
        PixelArt.clouds(g, x + 5, y + 7, w - 10, h - 12);
        int[][] corners = {{x + 5, y + 6, 1, 1}, {x + w - 6, y + 6, -1, 1}, {x + 5, y + h - 6, 1, -1}, {x + w - 6, y + h - 6, -1, -1}};
        for (int[] c : corners) {
            for (int k = 0; k < 14; k++) {
                PixelArt.px(g, c[0] + k * c[2], c[1], 0x5A7FD6FF);
                PixelArt.px(g, c[0], c[1] + k * c[3], 0x5A7FD6FF);
            }
            PixelArt.px(g, c[0] + 3 * c[2], c[1] + 3 * c[3], PixelArt.CYAN);
        }
        for (int[] p : new int[][] {{x, y}, {x + w - 1, y}, {x, y + h - 1}, {x + w - 1, y + h - 1}, {x + w / 2, y + h - 1},
                {x, y + h / 2}, {x + w - 1, y + h / 2}}) {
            PixelArt.pearl(g, p[0], p[1]);
        }
    }

    /** An inner panel, for the cards and columns of a page: a thin double frame; brighter when highlighted. */
    public static void box(GuiGraphics g, SystemLayout.Rect r, boolean highlighted) {
        PixelArt.frame(g, r.x(), r.y(), r.width(), r.height(), 2, highlighted ? PixelArt.LIGHT : PixelArt.CYAN,
                highlighted ? 0xE6143A5E : 0xD20F2944, highlighted ? 0x66E9FBFF : 0x403FB4EC);
    }

    /** The title sitting across the top border, like "STATUS": a banner with pointed ends and a diamond at each end. */
    public static void titleTag(GuiGraphics g, Font font, Component title, int centerX, int borderY) {
        String text = title.getString();
        int textWidth = spacedWidth(font, text, 2);
        int w = textWidth + 28;
        int x = centerX - w / 2, y = borderY - 7;
        PixelArt.rect(g, x + 3, y, w - 6, 15, PixelArt.OUTLINE);
        PixelArt.rect(g, x + 1, y + 2, w - 2, 11, PixelArt.OUTLINE);
        PixelArt.rect(g, x, y + 5, w, 5, PixelArt.OUTLINE);
        PixelArt.rect(g, x + 3, y + 1, w - 6, 13, PixelArt.NIGHT);
        PixelArt.rect(g, x + 2, y + 3, w - 4, 9, PixelArt.NIGHT);
        PixelArt.rect(g, x + 1, y + 6, w - 2, 3, PixelArt.NIGHT);
        PixelArt.rect(g, x + 3, y + 1, w - 6, 1, PixelArt.MID);
        PixelArt.rect(g, x + 3, y + 13, w - 6, 1, PixelArt.DEEP);
        PixelArt.diamond(g, x + 6, y + 7, 2, PixelArt.CYAN);
        PixelArt.diamond(g, x + w - 7, y + 7, 2, PixelArt.CYAN);
        glowSpaced(g, font, text, centerX - textWidth / 2, y + 4, TEXT, 2);
    }

    /** Text with a soft cyan halo. */
    public static void glowText(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        int halo = withAlpha(CYAN, 0x55);
        g.drawString(font, text, x - 1, y, halo, false);
        g.drawString(font, text, x + 1, y, halo, false);
        g.drawString(font, text, x, y - 1, halo, false);
        g.drawString(font, text, x, y + 1, halo, false);
        g.drawString(font, text, x, y, color, false);
    }

    /**
     * Text with one cyan drop shadow: still lit, but sharp. For numbers, which the halo of
     * {@link #glowText} blurs.
     */
    public static void crispText(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        g.drawString(font, text, x + 1, y + 1, withAlpha(CYAN, 0x90), false);
        g.drawString(font, text, x, y, color, false);
    }

    public static void crispSpaced(GuiGraphics g, Font font, String text, int x, int y, int color, int spacing) {
        spaced(g, font, text, x + 1, y + 1, withAlpha(CYAN, 0x90), spacing);
        spaced(g, font, text, x, y, color, spacing);
    }

    /** Letter-spaced text, used for labels and titles. */
    public static void spaced(GuiGraphics g, Font font, String text, int x, int y, int color, int spacing) {
        for (int i = 0; i < text.length(); i++) {
            String c = String.valueOf(text.charAt(i));
            g.drawString(font, c, x, y, color, false);
            x += font.width(c) + spacing;
        }
    }

    public static void glowSpaced(GuiGraphics g, Font font, String text, int x, int y, int color, int spacing) {
        int halo = withAlpha(CYAN, 0x55);
        spaced(g, font, text, x - 1, y, halo, spacing);
        spaced(g, font, text, x + 1, y, halo, spacing);
        spaced(g, font, text, x, y - 1, halo, spacing);
        spaced(g, font, text, x, y + 1, halo, spacing);
        spaced(g, font, text, x, y, color, spacing);
    }

    public static int spacedWidth(Font font, String text, int spacing) {
        return font.width(text) + Math.max(0, text.length() - 1) * spacing;
    }

    /**
     * A progress bar filled from the left: the pixel tube (a diamond at each end, a notch every tenth), {@code width}
     * long and 5 tall, centred in {@code height}. Shades of {@code color}: rose for health and warnings, cyan otherwise.
     */
    public static void bar(GuiGraphics g, int x, int y, int width, int height, float fraction, int color) {
        int ty = y + (height - 5) / 2;
        if ((color & 0xFFFFFF) == (WARNING & 0xFFFFFF) || ((color >> 16) & 0xFF) > 0xC0 && ((color >> 8) & 0xFF) < 0x90) {
            PixelArt.tube(g, x + 2, ty, width - 4, fraction, 0xFFFFB3B9, 0xFFE8565F, 0xFF8E2830);
        } else if ((color & 0xFFFFFF) == 0xD6F4FF) {
            PixelArt.tube(g, x + 2, ty, width - 4, fraction, PixelArt.WHITE, PixelArt.LIGHT, PixelArt.CYAN);
        } else {
            PixelArt.tube(g, x + 2, ty, width - 4, fraction, PixelArt.LIGHT, PixelArt.CYAN, PixelArt.MID);
        }
    }
}
