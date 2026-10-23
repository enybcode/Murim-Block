package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Stands in for a page that is not built yet. */
public final class PlaceholderPage implements SystemPageView {
    @Override
    public void render(GuiGraphics g, Font font, Rect area, int mouseX, int mouseY, float partialTick) {
        g.drawCenteredString(font, Component.translatable("gui.murimblock.page.next"), area.centerX(),
                area.y() + area.height() / 2 - 4, SystemTheme.TEXT_MUTED);
    }
}
