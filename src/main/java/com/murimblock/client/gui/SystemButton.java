package com.murimblock.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Every clickable control of the System, drawn in one of its styles. */
public class SystemButton extends AbstractButton {
    public enum Style {
        /** Main page tab: filled cyan when selected. */
        TAB,
        /** Tab inside a page: underlined when selected. */
        SUB_TAB,
        /** Framed action button, such as MEDITATE. */
        ACTION,
        /** Row of a list: highlighted when selected. */
        ROW
    }

    /** Draws the inside of a ROW button, for rows richer than one line of text. */
    @FunctionalInterface
    public interface RowContent {
        void draw(GuiGraphics g, Font font, int x, int y, int width, int height);
    }

    private final Style style;
    private final Runnable onPress;
    private boolean selected;
    private RowContent content;

    public SystemButton(int x, int y, int width, int height, Component label, Style style, Runnable onPress) {
        super(x, y, width, height, label);
        this.style = style;
        this.onPress = onPress;
    }

    public SystemButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public SystemButton content(RowContent content) {
        this.content = content;
        return this;
    }

    public SystemButton enabled(boolean enabled) {
        this.active = enabled;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean hot = active && isHoveredOrFocused();
        String label = getMessage().getString();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int textY = y + (h - 8) / 2;
        switch (style) {
            case TAB -> {
                // A pill with cut corners; the open tab is filled cyan with a diamond before its name.
                int line = selected ? PixelArt.LIGHT : hot ? PixelArt.CYAN : PixelArt.MID;
                int fill = selected ? PixelArt.CYAN : hot ? 0xFF16395C : 0xFF0F2944;
                PixelArt.frame(g, x, y, w, h, 2, line, fill, selected ? PixelArt.WHITE : 0x333FB4EC);
                int tw = SystemTheme.spacedWidth(font, label, 1);
                int tx = x + (w - tw) / 2 + (selected ? 3 : 0);
                if (selected) {
                    PixelArt.diamond(g, tx - 6, y + h / 2, 2, PixelArt.OUTLINE);
                    PixelArt.diamond(g, tx - 6, y + h / 2, 1, PixelArt.WHITE);
                }
                int color = selected ? SystemTheme.TEXT_ON_CYAN : hot ? SystemTheme.TEXT : 0xFF9FDCFF;
                SystemTheme.spaced(g, font, label, tx, textY, color, 1);
            }
            case SUB_TAB -> {
                int tw = SystemTheme.spacedWidth(font, label, 1);
                int tx = x + (w - tw) / 2;
                if (selected) {
                    PixelArt.rect(g, x, y + h - 2, w, 2, PixelArt.CYAN);
                    PixelArt.diamond(g, x + w / 2, y + h - 1, 2, PixelArt.LIGHT);
                    SystemTheme.crispSpaced(g, font, label, tx, textY, SystemTheme.TEXT, 1);
                } else {
                    SystemTheme.spaced(g, font, label, tx, textY, hot ? SystemTheme.TEXT : SystemTheme.TEXT_MUTED, 1);
                }
            }
            case ACTION -> {
                // Cut corners and a one-pixel shadow; pressed (hovered) it sinks by a pixel.
                int sink = hot ? 1 : 0;
                PixelArt.rect(g, x + 2, y + h, w - 2, 1, PixelArt.OUTLINE);
                PixelArt.frame(g, x, y + sink, w, h, 1, active ? (hot ? PixelArt.LIGHT : PixelArt.CYAN) : PixelArt.SLATE,
                        active ? (hot ? 0xFF1A4268 : PixelArt.NIGHT) : PixelArt.DEEP, active ? 0x403FB4EC : 0);
                int tw = SystemTheme.spacedWidth(font, label, 1);
                int color = active ? 0xFFD6F4FF : SystemTheme.withAlpha(SystemTheme.TEXT_MUTED, 0x80);
                SystemTheme.spaced(g, font, label, x + (w - tw) / 2, textY + sink, color, 1);
            }
            case ROW -> {
                // A thin frame; the chosen row has a white line and a lighter fill.
                int line = selected ? PixelArt.WHITE : hot ? PixelArt.CYAN : PixelArt.MID;
                int fill = selected ? 0xF01A4268 : hot ? 0xE0143454 : 0xD20F2944;
                PixelArt.frame(g, x, y, w, h, 1, line, fill, selected ? 0x66E9FBFF : 0);
                if (content != null) {
                    content.draw(g, font, x, y, w, h);
                } else {
                    g.drawString(font, getMessage(), x + 6, textY, SystemTheme.TEXT, false);
                }
            }
        }
    }
}
