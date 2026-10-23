package com.murimblock.client.gui;

import com.murimblock.client.MurimKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The System window: one frame, four page tabs; each page draws inside {@link SystemLayout#content}. */
public class SystemScreen extends Screen {
    private SystemPage page;
    private SystemLayout.Rect window = new SystemLayout.Rect(0, 0, 0, 0);

    public SystemScreen(SystemPage page) {
        super(Component.translatable("screen.murimblock.system"));
        this.page = page;
    }

    public SystemPage page() {
        return page;
    }

    public void showPage(SystemPage page) {
        if (this.page != page) {
            this.page = page;
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        window = SystemLayout.window(width, height);
        SystemPage[] pages = SystemPage.values();
        int[] labelWidths = new int[pages.length];
        for (int i = 0; i < pages.length; i++) {
            labelWidths[i] = SystemTheme.spacedWidth(font, pages[i].tab().getString(), 1);
        }
        int[] xs = SystemLayout.tabRow(window.centerX(), labelWidths);
        for (int i = 0; i < pages.length; i++) {
            SystemPage target = pages[i];
            addRenderableWidget(new SystemButton(xs[i], window.y() + SystemLayout.TAB_TOP,
                    SystemLayout.tabWidth(labelWidths[i]), SystemLayout.TAB_HEIGHT, target.tab(),
                    SystemButton.Style.TAB, () -> showPage(target)).selected(target == page));
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        SystemTheme.window(g, window);
        SystemTheme.titleTag(g, font, page.title(), window.centerX(), window.y());
        SystemLayout.Rect content = SystemLayout.content(window);
        Component soon = Component.translatable("gui.murimblock.page.next");
        g.drawCenteredString(font, soon, content.centerX(), content.y() + content.height() / 2 - 4, SystemTheme.TEXT_MUTED);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MurimKeys.OPEN_SYSTEM.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
