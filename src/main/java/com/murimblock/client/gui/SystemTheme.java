package com.murimblock.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Colours and drawing helpers of the System look (inspired by Solo Leveling's System windows). */
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

    /** Alpha of each outer glow ring, from the border outwards. */
    private static final int[] GLOW = {0x50, 0x30, 0x1A, 0x0C};
    private static final int CORNER_LENGTH = 8;

    private SystemTheme() {
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    /** The System window: translucent panel, glowing cyan border and bright corner brackets. */
    public static void window(GuiGraphics g, SystemLayout.Rect r) {
        for (int i = 0; i < GLOW.length; i++) {
            int d = i + 1;
            g.renderOutline(r.x() - d, r.y() - d, r.width() + 2 * d, r.height() + 2 * d, withAlpha(CYAN, GLOW[i]));
        }
        g.fill(r.x(), r.y(), r.right(), r.bottom(), PANEL);
        g.renderOutline(r.x() + 1, r.y() + 1, r.width() - 2, r.height() - 2, withAlpha(CYAN, 0x28));
        g.renderOutline(r.x(), r.y(), r.width(), r.height(), CYAN);
        corners(g, r.x() - 2, r.y() - 2, r.width() + 4, r.height() + 4);
    }

    /** A plain inner box, for the cards and columns of a page. */
    public static void box(GuiGraphics g, SystemLayout.Rect r, boolean highlighted) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), highlighted ? CYAN_FILL : withAlpha(CYAN, 0x0A));
        g.renderOutline(r.x(), r.y(), r.width(), r.height(), highlighted ? CYAN : CYAN_SOFT);
    }

    private static void corners(GuiGraphics g, int x, int y, int w, int h) {
        int l = CORNER_LENGTH;
        int right = x + w;
        int bottom = y + h;
        g.fill(x, y, x + l, y + 2, CORNER);
        g.fill(x, y, x + 2, y + l, CORNER);
        g.fill(right - l, y, right, y + 2, CORNER);
        g.fill(right - 2, y, right, y + l, CORNER);
        g.fill(x, bottom - 2, x + l, bottom, CORNER);
        g.fill(x, bottom - l, x + 2, bottom, CORNER);
        g.fill(right - l, bottom - 2, right, bottom, CORNER);
        g.fill(right - 2, bottom - l, right, bottom, CORNER);
    }

    /** The title tag sitting across the top border, like "STATUS". */
    public static void titleTag(GuiGraphics g, Font font, Component title, int centerX, int borderY) {
        String text = title.getString();
        int textWidth = spacedWidth(font, text, 2);
        int w = textWidth + 24;
        int h = 15;
        int x = centerX - w / 2;
        int y = borderY - h / 2;
        g.fill(x, y, x + w, y + h, 0xFF07121F);
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, withAlpha(CYAN, 0x30));
        g.renderOutline(x, y, w, h, CYAN);
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

    /** A framed progress bar, filled from the left. */
    public static void bar(GuiGraphics g, int x, int y, int width, int height, float fraction, int color) {
        g.renderOutline(x, y, width, height, withAlpha(color, 0x99));
        int inner = width - 4;
        int filled = Math.round(inner * Math.max(0, Math.min(1, fraction)));
        if (filled > 0) {
            g.fill(x + 2, y + 2, x + 2 + filled, y + height - 2, color);
        }
    }
}
