package com.murimblock.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** What one System page adds and draws inside the content area of the window. */
public interface SystemPageView {
    /** Adds the page's widgets; called whenever the screen is (re)built. */
    default void init(SystemScreen screen, SystemLayout.Rect area) {
    }

    /** Draws the page under its widgets. */
    void render(GuiGraphics g, Font font, SystemLayout.Rect area, int mouseX, int mouseY, float partialTick);
}
