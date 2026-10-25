package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemButton;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.data.MartialArtsData;
import com.murimblock.client.gui.data.MartialArtsData.Art;
import com.murimblock.client.gui.data.MartialArtsData.Technique;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** The MARTIAL ART page: one active art with all its techniques, and the other arts the player can switch to. */
public final class TechniquesPage implements SystemPageView {
    private static final int CARD_HEIGHT = 66;
    private static final int ART_ROW = 26;
    private static final int TECHNIQUE_ROW = 22;
    private static final int ROW_GAP = 3;
    private static final int LABEL_HEIGHT = 14;

    private int picked;

    @Override
    public void init(SystemScreen screen, Rect area) {
        TechniquesLayout layout = TechniquesLayout.of(area);
        Art active = MartialArtsData.active();
        picked = Math.min(picked, active.techniques().size() - 1);

        int y = layout.arts().y() + CARD_HEIGHT + 8 + LABEL_HEIGHT;
        for (Art art : MartialArtsData.known()) {
            if (art == active) {
                continue;
            }
            Rect row = new Rect(layout.arts().x(), y, layout.arts().width(), ART_ROW);
            Component label = Component.translatable("gui.murimblock.techniques.switch");
            int bw = SystemTheme.spacedWidth(Minecraft.getInstance().font,label.getString(), 1) + 10;
            screen.addPageWidget(new SystemButton(row.right() - bw - 5, row.y() + 6, bw, 14,
                    label, SystemButton.Style.ACTION, () -> {
                        MartialArtsData.activate(art);
                        picked = 0;
                        screen.refresh();
                    }).enabled(!MartialArtsData.inCombat()));
            y += ART_ROW + ROW_GAP;
        }

        y = layout.techniques().y() + LABEL_HEIGHT;
        List<Technique> techniques = active.techniques();
        for (int i = 0; i < techniques.size(); i++) {
            Technique t = techniques.get(i);
            int index = i;
            String key = MartialArtsData.keyOf(active, t);
            screen.addPageWidget(new SystemButton(layout.techniques().x(), y, layout.techniques().width(), TECHNIQUE_ROW,
                    t.name(), SystemButton.Style.ROW, () -> {
                        picked = index;
                        screen.refresh();
                    }).selected(i == picked).content((g, font, x, ry, w, h) -> techniqueRow(g, font, x, ry, h, key, t)));
            y += TECHNIQUE_ROW + ROW_GAP;
        }
    }

    private static void techniqueRow(GuiGraphics g, Font font, int x, int y, int h, String key, Technique t) {
        int box = 14;
        int bx = x + 4;
        int by = y + (h - box) / 2;
        g.renderOutline(bx, by, box, box, SystemTheme.CYAN);
        g.drawCenteredString(font, key, bx + box / 2 + 1, by + 3, 0xFF9FE2FF);
        g.drawString(font, t.name(), bx + box + 6, y + 3, SystemTheme.TEXT, false);
        g.drawString(font, t.kind().label(), bx + box + 6, y + 12, SystemTheme.TEXT_MUTED, false);
    }

    @Override
    public void render(GuiGraphics g, Font font, Rect area, int mouseX, int mouseY, float partialTick) {
        TechniquesLayout layout = TechniquesLayout.of(area);
        Art active = MartialArtsData.active();
        renderArts(g, font, layout.arts(), active);
        label(g, font, layout.techniques().x(), layout.techniques().y(), "gui.murimblock.techniques.of_art");
        renderDetail(g, font, layout.detail(), active.techniques().get(Math.min(picked, active.techniques().size() - 1)));
    }

    private static void renderArts(GuiGraphics g, Font font, Rect r, Art active) {
        Rect card = new Rect(r.x(), r.y(), r.width(), CARD_HEIGHT);
        g.renderOutline(card.x() - 1, card.y() - 1, card.width() + 2, card.height() + 2, SystemTheme.withAlpha(SystemTheme.CYAN, 0x30));
        SystemTheme.box(g, card, true);
        int x = card.x() + 7;
        label(g, font, x, card.y() + 6, "gui.murimblock.techniques.active");
        SystemTheme.glowText(g, font, active.name(), x, card.y() + 18, SystemTheme.TEXT);
        g.drawString(font, artLine(active), x, card.y() + 30, 0xFFB5DCF2, false);
        Component mastery = Component.translatable("gui.murimblock.percent", active.masteryPercent());
        g.drawString(font, Component.translatable("gui.murimblock.techniques.mastery"), x, card.y() + 42, SystemTheme.TEXT_MUTED, false);
        g.drawString(font, mastery, card.right() - 7 - font.width(mastery), card.y() + 42, SystemTheme.TEXT_MUTED, false);
        SystemTheme.bar(g, x, card.y() + 53, card.width() - 14, 7, active.masteryPercent() / 100f, SystemTheme.CYAN);

        int y = card.bottom() + 8;
        label(g, font, r.x(), y, "gui.murimblock.techniques.others");
        y += LABEL_HEIGHT;
        for (Art art : MartialArtsData.known()) {
            if (art == active) {
                continue;
            }
            Rect row = new Rect(r.x(), y, r.width(), ART_ROW);
            g.renderOutline(row.x(), row.y(), row.width(), row.height(), SystemTheme.CYAN_FAINT);
            g.drawString(font, art.name(), row.x() + 6, row.y() + 4, SystemTheme.TEXT, false);
            g.drawString(font, artLine(art), row.x() + 6, row.y() + 14, SystemTheme.TEXT_MUTED, false);
            y += ART_ROW + ROW_GAP;
        }

        Component notice = Component.translatable(MartialArtsData.inCombat()
                ? "gui.murimblock.techniques.notice_combat" : "gui.murimblock.techniques.notice");
        notice(g, font, r, notice);
    }

    private static Component artLine(Art art) {
        return Component.translatable("gui.murimblock.techniques.art_line", art.weaponName(), art.rank());
    }

    private static void notice(GuiGraphics g, Font font, Rect r, Component text) {
        int textX = r.x() + 6 + font.width("[!] ");
        List<FormattedCharSequence> lines = font.split(text, r.right() - 6 - textX);
        int height = lines.size() * 10 + 8;
        Rect box = new Rect(r.x(), r.bottom() - height, r.width(), height);
        SystemTheme.box(g, box, false);
        g.drawString(font, "[!]", r.x() + 6, box.y() + 5, SystemTheme.CYAN, false);
        int y = box.y() + 5;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, textX, y, 0xFFCFEEFF, false);
            y += 10;
        }
    }

    private static void renderDetail(GuiGraphics g, Font font, Rect r, Technique t) {
        SystemTheme.box(g, r, false);
        int x = r.x() + 10;
        int y = r.y() + 10;

        String kind = t.kind().label().getString().toUpperCase(java.util.Locale.ROOT);
        int tagWidth = SystemTheme.spacedWidth(font, kind, 1) + 12;
        g.renderOutline(x, y, tagWidth, 14, SystemTheme.CYAN);
        SystemTheme.spaced(g, font, kind, x + 6, y + 3, 0xFFBFEEFF, 1);
        y += 22;

        // Bold at normal size: scaled pixel text comes out uneven.
        SystemTheme.glowText(g, font, t.name().copy().withStyle(s -> s.withBold(true)), x, y, SystemTheme.TEXT);
        y += 16;

        int statWidth = (r.width() - 20 - 6) / 2;
        stat(g, font, x, y, statWidth, "gui.murimblock.techniques.qi_cost",
                t.qiCost() > 0 ? Component.literal(String.valueOf(t.qiCost())) : Component.literal("-"));
        stat(g, font, x + statWidth + 6, y, statWidth, "gui.murimblock.techniques.cooldown",
                t.cooldownSeconds() > 0 ? Component.translatable("gui.murimblock.seconds", t.cooldownSeconds()) : Component.literal("-"));
        y += 36;

        for (FormattedCharSequence line : font.split(t.description(), r.width() - 20)) {
            g.drawString(font, line, x, y, 0xFFD5EFFF, false);
            y += 11;
        }

        Component unlock = t.unlockMastery() > 0
                ? Component.translatable("gui.murimblock.techniques.unlock_at", t.unlockMastery())
                : Component.translatable("gui.murimblock.techniques.unlock_start");
        g.drawString(font, unlock, x, r.bottom() - 16, SystemTheme.TEXT_MUTED, false);
    }

    private static void stat(GuiGraphics g, Font font, int x, int y, int w, String labelKey, Component value) {
        g.renderOutline(x, y, w, 28, SystemTheme.CYAN_FAINT);
        g.drawString(font, Component.translatable(labelKey), x + 6, y + 5, SystemTheme.TEXT_MUTED, false);
        SystemTheme.crispText(g, font, value, x + 6, y + 16, SystemTheme.TEXT);
    }

    private static void label(GuiGraphics g, Font font, int x, int y, String key) {
        SystemTheme.spaced(g, font, Component.translatable(key).getString(), x, y, SystemTheme.TEXT_MUTED, 1);
    }
}
