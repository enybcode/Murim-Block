package com.murimblock.client;

import com.murimblock.alchemy.AlchemyRecipes;
import com.murimblock.alchemy.PillFurnaceBlockEntity;
import com.murimblock.alchemy.PillFurnaceMenu;
import com.murimblock.qi.QiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * The pill furnace's screen (docs/cultivation/alchemy.md) in Minecraft's own look, as Enzo chose it: on the left a recipe
 * book (Minecraft's) with the recipes, those known to choose (which puts their water and herbs in), the others in red
 * with where to learn them, and the heat bands of the one shown; on the right the furnace's panel: the water and the
 * three herbs in their order, the arrow to the pills, the flame over the charcoal, the heat as a thermometer with the
 * gold band of the step under way, the bellows and the Qi held down, the Qi, the four steps and the purity; the
 * inventory under it.
 */
public class PillFurnaceScreen extends AbstractContainerScreen<PillFurnaceMenu> {
    private static final ResourceLocation BOOK = ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png"),
            FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png"),
            BURN = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress"),
            FLAME = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress"),
            RECIPE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay"),
            RECIPE_HOT = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_highlighted"),
            LOCKED = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled"),
            LOCKED_HOT = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled_highlighted"),
            BUTTON = ResourceLocation.withDefaultNamespace("widget/button"),
            BUTTON_HOT = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final int GREY = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, SLOT = 0xFF8B8B8B, SLOT_D = 0xFF373737, TEXT = 0x404040,
            STATUS = 0x8A6A10, PURE = 0x2E7D32, QI = 0xFF2F8FD8, BAND = 0xFFF0C040;
    private static final String[] STEPS = {"prepare", "extract", "stabilize", "refine"};
    private static final int FAN_X = 7, QI_X = 55, BUTTONS_Y = 42, BUTTON_W = 44, BUTTON_H = 16, THERMO_X = 152, THERMO_Y = 17, THERMO_H = 56;
    private EditBox search;
    private int chosen = -1;
    private boolean fanHeld, qiHeld;

    public PillFurnaceScreen(PillFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PillFurnaceMenu.PANEL_X + 176;
        imageHeight = 176;
    }

    private static net.minecraft.network.chat.MutableComponent t(String key, Object... args) {
        return Component.translatable("gui.murimblock.furnace." + key, args);
    }

    @Override protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 25, topPos + 13, 81, 14, Component.translatable("itemGroup.search"));
        search.setMaxLength(50);
        search.setTextColor(0xFFFFFF);
        search.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        addRenderableWidget(search);
    }

    private int panel() { return leftPos + PillFurnaceMenu.PANEL_X; }

    /** The recipes the search lets through, in their order. */
    private List<AlchemyRecipes.Recipe> listed() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<AlchemyRecipes.Recipe> list = new ArrayList<>();
        for (AlchemyRecipes.Recipe r : AlchemyRecipes.ALL)
            if (q.isEmpty() || Component.translatable(r.nameKey()).getString().toLowerCase(Locale.ROOT).contains(q)) list.add(r);
        return list;
    }

    private int recipeX(int i) { return leftPos + 11 + (i % 5) * 25; }
    private int recipeY(int i) { return topPos + 31 + (i / 5) * 25; }

    /** The recipe the screen shows: the one in the furnace, else the one chosen in the book. */
    private AlchemyRecipes.Recipe shown() {
        AlchemyRecipes.Recipe inIt = AlchemyRecipes.byIndex(menu.data.get(7));
        return inIt != null ? inIt : AlchemyRecipes.byIndex(chosen);
    }

    // ---------------------------------------------------------------- drawing

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        book(g, mouseX, mouseY);
        furnacePanel(g, mouseX, mouseY);
    }

    private void book(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(BOOK, x, y, 1, 1, 147, 166);
        List<AlchemyRecipes.Recipe> list = listed();
        for (int i = 0; i < list.size() && i < 20; i++) {
            AlchemyRecipes.Recipe r = list.get(i);
            int bx = recipeX(i), by = recipeY(i);
            boolean hot = mouseX >= bx && mouseX < bx + 25 && mouseY >= by && mouseY < by + 25;
            g.blitSprite(menu.knows(r) ? hot ? RECIPE_HOT : RECIPE : hot ? LOCKED_HOT : LOCKED, bx, by, 25, 25);
            g.renderItem(new ItemStack(r.resultItem()), bx + 4, by + 4);
            if (shown() == r) outline(g, bx, by, 25, 25, LIGHT);
        }
        AlchemyRecipes.Recipe r = shown();
        if (r == null) return;
        int ty = y + 86, phase = menu.data.get(1);
        g.drawString(font, Component.translatable(r.nameKey()), x + 12, ty, 0xFFFFFF, false);
        for (int i = 1; i <= 3; i++) {
            AlchemyRecipes.Band b = r.band(i);
            boolean now = phase == i && AlchemyRecipes.byIndex(menu.data.get(7)) == r;
            g.drawString(font, t("step." + STEPS[i]), x + 12, ty + 1 + i * 10, now ? 0xFFD860 : 0xC0C0C0, false);
            g.drawString(font, b.low() + "-" + b.high(), x + 72, ty + 1 + i * 10, now ? 0xFFD860 : 0xC0C0C0, false);
        }
        g.drawString(font, t("book.qi", String.format(Locale.ROOT, "%.0f", r.qiPerTick() * 20)), x + 12, ty + 44, 0x73C2F2, false);
        if (!menu.knows(r)) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(t(r.inManual() ? "book.manual" : "book.scroll"), 124);
            for (int i = 0; i < lines.size() && i < 2; i++) g.drawString(font, lines.get(i), x + 12, ty + 58 + i * 10, 0xFF8080, false);
        }
    }

    private void furnacePanel(GuiGraphics g, int mouseX, int mouseY) {
        int x = panel(), y = topPos, w = 176, h = 176;
        // Minecraft's container: black rim, grey, white light top-left, grey shade bottom-right.
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GREY);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, LIGHT);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADE);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADE);
        for (Slot s : menu.slots) slotFrame(g, leftPos + s.x - 1, topPos + s.y - 1);

        var d = menu.data;
        int heat = d.get(0), phase = d.get(1), held = d.get(2), hold = d.get(3), lo = d.get(4), hi = d.get(5), fuel = d.get(6), herbsIn = d.get(9), purity = d.get(10);
        boolean water = (d.get(11) & 4) != 0, known = d.get(8) == 1;
        AlchemyRecipes.Recipe inIt = AlchemyRecipes.byIndex(d.get(7)), shown = shown();
        boolean complete = inIt != null && inIt.herbs().size() == herbsIn;

        g.drawString(font, title, x + 8, y + 6, TEXT, false);
        Component status = phase >= PillFurnaceBlockEntity.EXTRACT && phase <= PillFurnaceBlockEntity.REFINE && hold > 0
                ? t("status.step", t("step." + STEPS[phase]), held / 20, hold / 20)
                : t(!water ? "status.water" : !complete ? "status.herbs" : !known ? "status.unknown" : fuel <= 0 ? "status.fire" : "status.heat");
        g.drawString(font, status, x + 168 - font.width(status), y + 6, STATUS, false);
        g.drawString(font, playerInventoryTitle, x + 8, y + 82, TEXT, false);

        // What still goes in: shown faintly in its empty slot, as Minecraft's recipe book does.
        if (!water) {
            if (menu.slots.get(PillFurnaceBlockEntity.WATER_SLOT).getItem().isEmpty()) ghost(g, new ItemStack(Items.WATER_BUCKET), 0);
        } else {
            g.fill(x + 8, y + 32, x + 24, y + 34, 0xFF4F96C8);
        }
        if (shown != null) {
            for (int i = 0; i < shown.herbs().size(); i++)
                if (menu.slots.get(PillFurnaceBlockEntity.HERB_SLOT + i).getItem().isEmpty()) ghost(g, new ItemStack(shown.herbs().get(i).get()), PillFurnaceBlockEntity.HERB_SLOT + i);
            if (menu.slots.get(PillFurnaceBlockEntity.OUT_SLOT).getItem().isEmpty()) ghost(g, new ItemStack(shown.resultItem()), PillFurnaceBlockEntity.OUT_SLOT);
        }

        // The arrow fills with the step's hold; the flame with the fire left.
        g.blit(FURNACE, x + 88, y + 18, 79, 34, 24, 17);
        int done = phase >= PillFurnaceBlockEntity.EXTRACT && phase <= PillFurnaceBlockEntity.REFINE ? ((phase - 1) * 8 + (hold > 0 ? 8 * held / hold : 0)) : 0;
        if (done > 0) g.blitSprite(BURN, 24, 16, 0, 0, x + 88, y + 18, done, 16);
        if (fuel > 0) {
            int f = Math.max(1, Math.min(14, 14 * fuel / PillFurnaceBlockEntity.FUEL_PER_COAL));
            g.blitSprite(FLAME, 14, 14, 0, 14 - f, x + 117, y + 38 + 14 - f, 14, f);
        }

        // The heat: a thermometer, its band in gold either side, the needle white.
        int tx = x + THERMO_X, ty = y + THERMO_Y;
        g.fill(tx - 1, ty - 1, tx + 9, ty + THERMO_H + 1, SLOT_D);
        g.fill(tx, ty, tx + 8, ty + THERMO_H, 0xFF2A2A2A);
        int level = Math.min(THERMO_H, heat * THERMO_H / PillFurnaceBlockEntity.MAX_HEAT);
        for (int i = 0; i < level; i++) g.fill(tx + 1, ty + THERMO_H - 1 - i, tx + 7, ty + THERMO_H - i, heatColour((float) i / THERMO_H));
        if (hi > 0) {
            int top = ty + THERMO_H - hi * THERMO_H / PillFurnaceBlockEntity.MAX_HEAT, bottom = ty + THERMO_H - lo * THERMO_H / PillFurnaceBlockEntity.MAX_HEAT;
            g.fill(tx - 3, top, tx - 1, bottom + 1, BAND);
            g.fill(tx + 9, top, tx + 11, bottom + 1, BAND);
        }
        if (heat > 0) g.fill(tx - 1, ty + THERMO_H - level, tx + 9, ty + THERMO_H - level + 1, LIGHT);

        // The bellows and the Qi, held down.
        button(g, x + FAN_X, y + BUTTONS_Y, t("fan"), fanHeld, mouseX, mouseY);
        button(g, x + QI_X, y + BUTTONS_Y, t("qi"), qiHeld, mouseX, mouseY);
        double qi = QiService.getQi(minecraft.player), qiMax = Math.max(1, QiService.getQiMax(minecraft.player));
        g.fill(x + 7, y + 62, x + 99, y + 66, SLOT_D);
        g.fill(x + 8, y + 63, x + 8 + (int) (90 * Math.min(1, qi / qiMax)), y + 65, QI);

        // The four steps, and the purity it is heading for.
        for (int i = 0; i < 4; i++) {
            boolean stepDone = phase > i || i == 0 && complete, now = phase == i && !stepDone;
            int px = x + 8 + i * 7;
            g.fill(px, y + 70, px + 5, y + 75, SLOT_D);
            g.fill(px + 1, y + 71, px + 4, y + 74, stepDone ? 0xFF3C9A3C : now ? BAND : SLOT);
        }
        if (complete) {
            String p = purity + "%";
            g.drawString(font, p, x + 99 - font.width(p), y + 69, PURE, false);
        }
    }

    private void slotFrame(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT);
        g.fill(x, y, x + 17, y + 1, SLOT_D);
        g.fill(x, y, x + 1, y + 17, SLOT_D);
        g.fill(x + 1, y + 17, x + 18, y + 18, LIGHT);
        g.fill(x + 17, y + 1, x + 18, y + 18, LIGHT);
    }

    private void ghost(GuiGraphics g, ItemStack stack, int slot) {
        Slot s = menu.slots.get(slot);
        int sx = leftPos + s.x, sy = topPos + s.y;
        g.renderFakeItem(stack, sx, sy);
        g.fill(net.minecraft.client.renderer.RenderType.guiGhostRecipeOverlay(), sx, sy, sx + 16, sy + 16, 0x808B8B8B);
    }

    private void button(GuiGraphics g, int x, int y, Component label, boolean held, int mouseX, int mouseY) {
        boolean hot = held || over(mouseX, mouseY, x, y, BUTTON_W, BUTTON_H);
        g.blitSprite(hot ? BUTTON_HOT : BUTTON, x, y, BUTTON_W, BUTTON_H);
        g.drawCenteredString(font, label, x + BUTTON_W / 2, y + (BUTTON_H - 8) / 2, held ? 0xFFFFA0 : 0xFFFFFF);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c); g.fill(x, y + h - 1, x + w, y + h, c); g.fill(x, y, x + 1, y + h, c); g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int heatColour(float v) {
        return v < 0.5F ? lerp(0xFF3C6EAA, 0xFFE08C28, v / 0.5F) : lerp(0xFFE08C28, 0xFFC83C28, (v - 0.5F) / 0.5F);
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t), gr = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t),
                bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | r << 16 | gr << 8 | bl;
    }

    // ---------------------------------------------------------------- what the mouse says

    private void tooltips(GuiGraphics g, int mouseX, int mouseY) {
        int x = panel(), y = topPos;
        var d = menu.data;
        List<AlchemyRecipes.Recipe> list = listed();
        for (int i = 0; i < list.size() && i < 20; i++)
            if (over(mouseX, mouseY, recipeX(i), recipeY(i), 25, 25)) {
                AlchemyRecipes.Recipe r = list.get(i);
                List<Component> lines = new ArrayList<>(List.of(Component.translatable(r.nameKey())));
                if (!menu.knows(r)) lines.add(t(r.inManual() ? "book.manual" : "book.scroll").withStyle(ChatFormatting.RED));
                else lines.add(t("book.fill").withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        AlchemyRecipes.Recipe shown = shown();
        for (int i = 0; i < PillFurnaceBlockEntity.SLOTS; i++) {
            Slot s = menu.slots.get(i);
            if (!s.getItem().isEmpty() || !over(mouseX, mouseY, leftPos + s.x - 1, topPos + s.y - 1, 18, 18)) continue;
            Component tip = null;
            if (i == PillFurnaceBlockEntity.WATER_SLOT) tip = t((d.get(11) & 4) != 0 ? "slot.water_in" : "slot.water");
            else if (i == PillFurnaceBlockEntity.FUEL_SLOT) tip = t("slot.fuel");
            else if (i == PillFurnaceBlockEntity.OUT_SLOT) tip = t("slot.out");
            else if (shown != null && i - PillFurnaceBlockEntity.HERB_SLOT < shown.herbs().size()) {
                var ing = shown.herbs().get(i - PillFurnaceBlockEntity.HERB_SLOT);
                tip = t("slot.herb", t("role." + ing.role().name().toLowerCase(Locale.ROOT)), ing.get().getDescription());
            } else tip = t("slot.herb_any");
            g.renderTooltip(font, tip, mouseX, mouseY);
            return;
        }
        if (over(mouseX, mouseY, x + THERMO_X - 3, y + THERMO_Y - 1, 14, THERMO_H + 2)) {
            List<Component> lines = new ArrayList<>(List.of(t("heat_tip", d.get(0))));
            if (d.get(5) > 0) lines.add(t("band_tip", d.get(4), d.get(5)).withStyle(ChatFormatting.GOLD));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else if (over(mouseX, mouseY, x + FAN_X, y + BUTTONS_Y, BUTTON_W, BUTTON_H)) {
            g.renderTooltip(font, t("fan_tip"), mouseX, mouseY);
        } else if (over(mouseX, mouseY, x + QI_X, y + BUTTONS_Y, BUTTON_W, BUTTON_H)) {
            g.renderTooltip(font, t("qi_tip"), mouseX, mouseY);
        } else if (over(mouseX, mouseY, x + 7, y + 69, 92, 8)) {
            List<Component> lines = new ArrayList<>();
            for (int i = 0; i < 4; i++) lines.add(t("step." + STEPS[i]).withStyle(d.get(1) > i ? ChatFormatting.GREEN : d.get(1) == i ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            int purity = d.get(10);
            if (purity > 0) lines.add(t("forecast", purity, Component.translatable("gui.murimblock.furnace.toxicity." + (purity >= 75 ? "low" : purity >= 50 ? "medium" : "high"))));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    // ---------------------------------------------------------------- input

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int x = panel(), y = topPos;
            if (over(mx, my, x + FAN_X, y + BUTTONS_Y, BUTTON_W, BUTTON_H)) { fanHeld = true; press(PillFurnaceMenu.BELLOWS_DOWN); return true; }
            if (over(mx, my, x + QI_X, y + BUTTONS_Y, BUTTON_W, BUTTON_H)) { qiHeld = true; press(PillFurnaceMenu.QI_DOWN); return true; }
            List<AlchemyRecipes.Recipe> list = listed();
            for (int i = 0; i < list.size() && i < 20; i++)
                if (over(mx, my, recipeX(i), recipeY(i), 25, 25)) {
                    AlchemyRecipes.Recipe r = list.get(i);
                    chosen = r.index();
                    if (menu.knows(r)) press(PillFurnaceMenu.FILL_RECIPE + r.index());
                    return true;
                }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (fanHeld) { fanHeld = false; press(PillFurnaceMenu.BELLOWS_UP); }
        if (qiHeld) { qiHeld = false; press(PillFurnaceMenu.QI_UP); }
        return super.mouseReleased(mx, my, button);
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        // Typing in the search must not close the screen (the inventory key) or drop items.
        if (search.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) return search.keyPressed(key, scan, modifiers) || true;
        return super.keyPressed(key, scan, modifiers);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && (hoveredSlot == null || !hoveredSlot.hasItem())) tooltips(g, mouseX, mouseY);
    }

    @Override public boolean isPauseScreen() { return false; }
}
