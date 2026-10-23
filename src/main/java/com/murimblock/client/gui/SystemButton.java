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

    private final Style style;
    private final Runnable onPress;
    private boolean selected;

    public SystemButton(int x, int y, int width, int height, Component label, Style style, Runnable onPress) {
        super(x, y, width, height, label);
        this.style = style;
        this.onPress = onPress;
    }

    public SystemButton selected(boolean selected) {
        this.selected = selected;
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
                if (selected) {
                    g.renderOutline(x - 1, y - 1, w + 2, h + 2, SystemTheme.withAlpha(SystemTheme.CYAN, 0x40));
                    g.fill(x, y, x + w, y + h, SystemTheme.CYAN);
                } else if (hot) {
                    g.fill(x, y, x + w, y + h, SystemTheme.withAlpha(SystemTheme.CYAN, 0x1A));
                }
                g.renderOutline(x, y, w, h, selected || hot ? SystemTheme.CYAN : SystemTheme.CYAN_SOFT);
                int color = selected ? SystemTheme.TEXT_ON_CYAN : hot ? SystemTheme.TEXT : 0xFF9FDCFF;
                int tw = SystemTheme.spacedWidth(font, label, 1);
                SystemTheme.spaced(g, font, label, x + (w - tw) / 2, textY, color, 1);
            }
            case SUB_TAB -> {
                int tw = SystemTheme.spacedWidth(font, label, 1);
                int tx = x + (w - tw) / 2;
                if (selected) {
                    g.fill(x, y + h - 2, x + w, y + h, SystemTheme.CYAN);
                    SystemTheme.glowSpaced(g, font, label, tx, textY, SystemTheme.TEXT, 1);
                } else {
                    SystemTheme.spaced(g, font, label, tx, textY, hot ? SystemTheme.TEXT : SystemTheme.TEXT_MUTED, 1);
                }
            }
            case ACTION -> {
                if (hot) {
                    g.fill(x, y, x + w, y + h, SystemTheme.withAlpha(SystemTheme.CYAN, 0x2E));
                }
                int frame = active ? SystemTheme.CYAN : SystemTheme.withAlpha(SystemTheme.CYAN, 0x59);
                g.renderOutline(x, y, w, h, frame);
                int tw = SystemTheme.spacedWidth(font, label, 1);
                int color = active ? 0xFFD6F4FF : SystemTheme.withAlpha(SystemTheme.TEXT_MUTED, 0x80);
                SystemTheme.spaced(g, font, label, x + (w - tw) / 2, textY, color, 1);
            }
            case ROW -> {
                if (selected) {
                    g.fill(x, y, x + w, y + h, SystemTheme.CYAN_FILL);
                } else if (hot) {
                    g.fill(x, y, x + w, y + h, SystemTheme.withAlpha(SystemTheme.CYAN, 0x14));
                }
                g.renderOutline(x, y, w, h, selected || hot ? SystemTheme.CYAN : SystemTheme.CYAN_FAINT);
                g.drawString(font, getMessage(), x + 6, textY, SystemTheme.TEXT, false);
            }
        }
    }
}
