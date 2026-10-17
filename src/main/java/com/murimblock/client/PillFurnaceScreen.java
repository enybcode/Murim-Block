package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.alchemy.AlchemyRecipes;
import com.murimblock.alchemy.PillFurnaceBlockEntity;
import com.murimblock.alchemy.PillFurnaceMenu;
import com.murimblock.qi.QiService;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The pill furnace's fire screen (docs/cultivation/alchemy.md), in the Murimblock screens' manuscript look: the recipe
 * and its herbs in their order and roles; the heat gauge, its gold band for the step under way and its needle; the four
 * steps; the bellows and the Qi held down, a charcoal added; the Qi and the fuel; the purity it is heading for.
 */
public class PillFurnaceScreen extends AbstractContainerScreen<PillFurnaceMenu> {
    private static final Style MANUSCRIPT = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "manuscript"));
    private static final int FRAME = 0xFF1A1714, PAPER = 0xFF2B2721, CARD = 0xFF332E27, LINE = 0xFF4A443B, INK = 0xFFEDE3CC, MUTED = 0xFFB3A68A,
            GOLD = 0xFFC9A45C, GOLD_L = 0xFFF2D9A0, GOLD_D = 0xFF7A5E2C, SEAL = 0xFFC24A36, SEAL_HOT = 0xFFD9614B, QI = 0xFF238DD4, QI_L = 0xFF73C2F2,
            GREEN = 0xFF8FCB9B, COLD = 0xFF3C6EAA, HOT = 0xFFC83C28;
    private static final String[] STEPS = {"prepare", "extract", "stabilize", "refine"};
    private int bellowsX, bellowsY, fuelY, qiY, buttonW = 104, buttonH = 16;
    private boolean bellowsHeld, qiHeld;

    public PillFurnaceScreen(PillFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 370;
        imageHeight = 214;
    }

    private Component t(String key, Object... args) {
        return Component.literal(Component.translatable("gui.murimblock.furnace." + key, args).getString()).withStyle(MANUSCRIPT);
    }

    private Component lit(String s) {
        return Component.literal(s).withStyle(MANUSCRIPT);
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c); g.fill(x, y + h - 1, x + w, y + h, c); g.fill(x, y, x + 1, y + h, c); g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private void section(GuiGraphics g, int x, int y, Component c) {
        g.drawString(font, c, x, y, MUTED, false);
        g.fill(x, y + 10, x + 18, y + 11, GOLD);
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x0 = leftPos, y0 = topPos, pw = imageWidth, ph = imageHeight;
        g.fill(x0 - 2, y0 - 2, x0 + pw + 2, y0 + ph + 2, FRAME);
        g.fill(x0, y0, x0 + pw, y0 + ph, PAPER);
        outline(g, x0, y0, pw, ph, GOLD_D);
        Component title = t("title");
        int tw = font.width(title);
        g.drawString(font, title, x0 + (pw - tw) / 2, y0 + 5, GOLD, false);
        g.fill(x0 + 18, y0 + 9, x0 + (pw - tw) / 2 - 6, y0 + 10, GOLD_D);
        g.fill(x0 + (pw + tw) / 2 + 6, y0 + 9, x0 + pw - 10, y0 + 10, GOLD_D);
        g.fill(x0 + 6, y0 + 4, x0 + 15, y0 + 13, SEAL);

        var d = menu.data;
        int heat = d.get(0), phase = d.get(1), held = d.get(2), hold = d.get(3), lo = d.get(4), hi = d.get(5), fuel = d.get(6), recipeIndex = d.get(7);
        boolean known = d.get(8) == 1, water = (d.get(11) & 4) != 0;
        int herbsIn = d.get(9), purity = d.get(10);
        AlchemyRecipes.Recipe recipe = AlchemyRecipes.byIndex(recipeIndex);

        // ---- The recipe.
        int lx = x0 + 8, ly = y0 + 22;
        section(g, lx, ly, t("recipe"));
        Component name = recipe == null ? t(herbsIn == 0 && !water ? "empty" : "mixture") : recipe.index() >= 0 && !known && recipe.herbs().size() == herbsIn
                ? t("unknown") : lit(Component.translatable(recipe.nameKey()).getString());
        g.drawString(font, name, lx, ly + 13, recipe != null && (known || recipe.herbs().size() != herbsIn) ? GOLD_L : MUTED, false);
        int rows = recipe == null ? Math.max(1, herbsIn) : recipe.herbs().size();
        for (int i = 0; i <= rows; i++) {
            int y = ly + 26 + i * 22;
            boolean waterRow = i == 0, in = waterRow ? water : i <= herbsIn, next = waterRow ? !water : i == herbsIn + 1 && water;
            g.fill(lx, y, lx + 104, y + 19, next ? 0xFF463A22 : CARD);
            outline(g, lx, y, 104, 19, next ? GOLD : LINE);
            g.fill(lx + 2, y + 2, lx + 17, y + 17, 0xFF221F1A);
            if (waterRow) {
                g.fill(lx + 5, y + 5, lx + 14, y + 14, 0xFF4F96C8);
                g.drawString(font, t("water"), lx + 21, y + 2, in ? INK : next ? GOLD_L : MUTED, false);
                g.drawString(font, t("role.liquid"), lx + 21, y + 11, MUTED, false);
            } else if (recipe != null) {
                AlchemyRecipes.Ingredient ing = recipe.herbs().get(i - 1);
                g.renderItem(new ItemStack(ing.get()), lx + 2, y + 2);
                g.drawString(font, lit(fit(ing.get().getDescription().getString(), 80)), lx + 21, y + 2, in ? INK : next ? GOLD_L : MUTED, false);
                g.drawString(font, t("role." + ing.role().name().toLowerCase(Locale.ROOT)), lx + 21, y + 11, MUTED, false);
            } else {
                g.drawString(font, lit("?"), lx + 21, y + 6, MUTED, false);
            }
            if (in) g.drawString(font, lit("v"), lx + 96, y + 6, GREEN, false);
        }

        // ---- The heat gauge.
        int gx = x0 + 136, gy = y0 + 36, gh = 124, gw = 22;
        section(g, gx - 2, y0 + 22, t("heat"));
        g.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, FRAME);
        for (int y = 0; y < gh; y++) {
            float v = 1 - (float) y / gh;
            g.fill(gx, gy + y, gx + gw, gy + y + 1, heatColour(v));
        }
        if (hi > 0) {
            int top = gy + gh - hi * gh / PillFurnaceBlockEntity.MAX_HEAT, bottom = gy + gh - lo * gh / PillFurnaceBlockEntity.MAX_HEAT;
            g.fill(gx, top, gx + gw, bottom, 0x5AFFE68C);
            g.fill(gx - 4, top, gx + gw + 4, top + 1, GOLD_L);
            g.fill(gx - 4, bottom, gx + gw + 4, bottom + 1, GOLD_L);
            g.drawString(font, t("step." + STEPS[phase]), gx + gw + 6, (top + bottom) / 2 - 4, GOLD_L, false);
        }
        int needle = gy + gh - Math.min(heat, PillFurnaceBlockEntity.MAX_HEAT) * gh / PillFurnaceBlockEntity.MAX_HEAT;
        g.fill(gx - 6, needle, gx + gw + 6, needle + 2, INK);
        g.fill(gx - 8, needle - 1, gx - 5, needle + 3, SEAL);
        g.drawString(font, lit(String.valueOf(heat)), gx - 30, needle - 3, INK, false);
        g.drawString(font, t("hot"), gx + gw + 4, gy, HOT, false);
        g.drawString(font, t("cold"), gx - 2, gy + gh + 4, COLD, false);
        if (hold > 0) {
            g.drawString(font, t("held", String.format(Locale.ROOT, "%.1f", held / 20.0), String.format(Locale.ROOT, "%.0f", hold / 20.0)), gx - 10, gy + gh + 16, GOLD_L, false);
            g.fill(gx - 10, gy + gh + 27, gx + 50, gy + gh + 30, 0xFF221F1A);
            g.fill(gx - 10, gy + gh + 27, gx - 10 + 60 * held / hold, gy + gh + 30, GOLD);
        }

        // ---- The steps.
        int rx = x0 + 232, ry = y0 + 22;
        section(g, rx, ry, t("steps"));
        for (int i = 0; i < 4; i++) {
            int y = ry + 14 + i * 13;
            boolean done = phase > i || phase == PillFurnaceBlockEntity.PREPARE && i == 0 && recipe != null && recipe.herbs().size() == herbsIn,
                    now = phase == i && !(i == 0 && done);
            g.fill(rx, y + 1, rx + 7, y + 8, done ? GREEN : now ? GOLD_L : LINE);
            g.drawString(font, t("step." + STEPS[i]), rx + 11, y, done ? GREEN : now ? GOLD_L : MUTED, false);
            if (recipe != null && i > 0) {
                AlchemyRecipes.Band b = recipe.band(i);
                g.drawString(font, lit(b.low() + "-" + b.high() + (i == 3 ? " Qi" : "")), rx + 64, y, now ? INK : MUTED, false);
            }
        }

        // ---- The fire's controls.
        int cy = ry + 70;
        section(g, rx, cy, t("fire"));
        bellowsX = rx; bellowsY = cy + 13; fuelY = cy + 32; qiY = cy + 51;
        button(g, rx, bellowsY, t("bellows"), bellowsHeld, mouseX, mouseY);
        button(g, rx, fuelY, t("charcoal"), false, mouseX, mouseY);
        button(g, rx, qiY, t("infuse"), qiHeld, mouseX, mouseY);
        g.drawString(font, lit("Qi"), rx, cy + 72, QI_L, false);
        double qi = QiService.getQi(minecraft.player), qiMax = Math.max(1, QiService.getQiMax(minecraft.player));
        g.fill(rx + 14, cy + 74, rx + 104, cy + 77, 0xFF182838);
        g.fill(rx + 14, cy + 74, rx + 14 + (int) (90 * Math.min(1, qi / qiMax)), cy + 77, QI);
        g.drawString(font, t("fuel"), rx, cy + 84, MUTED, false);
        g.fill(rx + 22, cy + 86, rx + 104, cy + 89, 0xFF221F1A);
        g.fill(rx + 22, cy + 86, rx + 22 + 82 * fuel / PillFurnaceBlockEntity.MAX_FUEL, cy + 89, 0xFFDC7828);

        // ---- What it is heading for.
        g.fill(x0 + 8, y0 + ph - 18, x0 + pw - 8, y0 + ph - 17, LINE);
        Component forecast = recipe != null && recipe.herbs().size() == herbsIn
                ? t("forecast", purity, Component.translatable("gui.murimblock.furnace.toxicity." + (purity >= 75 ? "low" : purity >= 50 ? "medium" : "high")).getString())
                : t(water ? "add_herbs" : "add_water");
        g.drawString(font, forecast, x0 + 8, y0 + ph - 13, INK, false);
    }

    private static int heatColour(float v) {
        int a = v < 0.35F ? lerp(COLD, 0xFF966E3C, v / 0.35F) : v < 0.8F ? lerp(0xFF966E3C, 0xFFDC8C28, (v - 0.35F) / 0.45F) : lerp(0xFFDC8C28, HOT, (v - 0.8F) / 0.2F);
        return a;
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t), gr = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t),
                bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return 0xFF000000 | r << 16 | gr << 8 | bl;
    }

    private String fit(String s, int w) {
        if (font.width(lit(s)) <= w) return s;
        while (s.length() > 1 && font.width(lit(s + "...")) > w) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private void button(GuiGraphics g, int x, int y, Component label, boolean held, int mouseX, int mouseY) {
        boolean hot = held || mouseX >= x && mouseX < x + buttonW && mouseY >= y && mouseY < y + buttonH;
        g.fill(x, y, x + buttonW, y + buttonH, hot ? SEAL_HOT : SEAL);
        outline(g, x, y, buttonW, buttonH, hot ? GOLD_L : GOLD);
        g.drawString(font, label, x + (buttonW - font.width(label)) / 2, y + 4, INK, false);
    }

    private boolean over(double mx, double my, int y) {
        return mx >= bellowsX && mx < bellowsX + buttonW && my >= y && my < y + buttonH;
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (over(mx, my, bellowsY)) { bellowsHeld = true; press(PillFurnaceMenu.BELLOWS_DOWN); return true; }
            if (over(mx, my, fuelY)) { press(PillFurnaceMenu.ADD_FUEL); return true; }
            if (over(mx, my, qiY)) { qiHeld = true; press(PillFurnaceMenu.QI_DOWN); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (bellowsHeld) { bellowsHeld = false; press(PillFurnaceMenu.BELLOWS_UP); }
        if (qiHeld) { qiHeld = false; press(PillFurnaceMenu.QI_UP); }
        return super.mouseReleased(mx, my, button);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override public boolean isPauseScreen() { return false; }
}
