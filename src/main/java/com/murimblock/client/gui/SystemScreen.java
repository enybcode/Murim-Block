package com.murimblock.client.gui;

import com.murimblock.client.MurimKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The System window: one frame, four page tabs; each page draws inside {@link SystemLayout#content}. */
public class SystemScreen extends Screen {
    private SystemPage page;
    private SystemPageView view;
    private SystemLayout.Rect window = new SystemLayout.Rect(0, 0, 0, 0);

    public SystemScreen(SystemPage page) {
        this(page, page.createView());
    }

    /** Opens a page in a given state, such as a sub-tab. */
    public SystemScreen(SystemPage page, SystemPageView view) {
        super(Component.translatable("screen.murimblock.system"));
        this.page = page;
        this.view = view;
    }

    public SystemPage page() {
        return page;
    }

    public void showPage(SystemPage page) {
        if (this.page != page) {
            this.page = page;
            this.view = page.createView();
            rebuildWidgets();
        }
    }

    /** Rebuilds the page's widgets after its state changed. */
    public void refresh() {
        rebuildWidgets();
    }

    /** Lets a page add its own widgets. */
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addPageWidget(T widget) {
        return addRenderableWidget(widget);
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
        view.init(this, SystemLayout.content(window));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        SystemTheme.window(g, window);
        SystemTheme.titleTag(g, font, page.title(), window.centerX(), window.y());
        view.render(g, font, SystemLayout.content(window), mouseX, mouseY, partialTick);
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
