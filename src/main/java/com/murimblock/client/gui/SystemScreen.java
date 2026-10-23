package com.murimblock.client.gui;

import com.murimblock.client.MurimKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The System window: one frame, four page tabs; each page draws inside {@link SystemLayout#content}.
 * It is laid out in design pixels and drawn at its own scale ({@link SystemLayout#displayFactor}), whatever
 * Minecraft's GUI scale is, so it always keeps its designed size and proportions.
 */
public class SystemScreen extends Screen {
    /** Design pixels to GUI pixels while a System screen draws; 1 otherwise. */
    private static float drawScale = 1;

    private SystemPage page;
    private SystemPageView view;
    private SystemLayout.Rect window = new SystemLayout.Rect(0, 0, 0, 0);
    private float scale = 1;

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

    /** The current design-to-GUI scale, for drawing that must leave the scaled pose (such as entities). */
    public static float drawScale() {
        return drawScale;
    }

    /** Clips to a rectangle given in design pixels (scissor boxes ignore the pose). */
    public static void scissor(GuiGraphics g, int x1, int y1, int x2, int y2) {
        g.enableScissor(Math.round(x1 * drawScale), Math.round(y1 * drawScale),
                Math.round(x2 * drawScale), Math.round(y2 * drawScale));
    }

    @Override
    protected void init() {
        var gameWindow = minecraft.getWindow();
        float factor = SystemLayout.displayFactor(gameWindow.getWidth(), gameWindow.getHeight());
        scale = (float) (factor / gameWindow.getGuiScale());
        int designWidth = Math.round(width / scale);
        int designHeight = Math.round(height / scale);

        window = SystemLayout.window(designWidth, designHeight);
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
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int mx = Math.round(mouseX / scale);
        int my = Math.round(mouseY / scale);
        drawScale = scale;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1);
        SystemTheme.window(g, window);
        SystemTheme.titleTag(g, font, page.title(), window.centerX(), window.y());
        view.render(g, font, SystemLayout.content(window), mx, my, partialTick);
        for (Renderable renderable : renderables) {
            renderable.render(g, mx, my, partialTick);
        }
        g.pose().popPose();
        drawScale = 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX / scale, mouseY / scale, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / scale, mouseY / scale, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX / scale, mouseY / scale, button, dragX / scale, dragY / scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX / scale, mouseY / scale, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / scale, mouseY / scale);
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
