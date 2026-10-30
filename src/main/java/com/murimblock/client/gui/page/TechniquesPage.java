package com.murimblock.client.gui.page;

import com.murimblock.client.gui.PixelArt;
import com.murimblock.client.gui.SystemButton;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.TechniqueIcons;
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
    /** Technique slots an art has, as on the combat bar. */
    private static final int SLOTS = 6;

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
            if (t.kind() == MartialArtsData.Kind.ULTIMATE) {
                // The free technique slots come before the ultimate (drawn by render).
                y += emptySlots(active) * (TECHNIQUE_ROW + ROW_GAP);
            }
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

    /** Technique slots the art leaves free, up to the six a bar holds. */
    private static int emptySlots(Art art) {
        long count = art.techniques().stream().filter(t -> t.kind() == MartialArtsData.Kind.TECHNIQUE).count();
        return (int) Math.max(0, SLOTS - count);
    }

    private static void techniqueRow(GuiGraphics g, Font font, int x, int y, int h, String key, Technique t) {
        int box = 14;
        int bx = x + 4;
        int by = y + (h - box) / 2;
        PixelArt.frame(g, bx, by, box, box, 1, t.kind() == MartialArtsData.Kind.ULTIMATE ? PixelArt.GOLD : PixelArt.CYAN,
                PixelArt.DEEP, 0);
        g.drawCenteredString(font, key, bx + box / 2 + 1, by + 3, 0xFF9FE2FF);
        g.drawString(font, t.name(), bx + box + 6, y + 3, SystemTheme.TEXT, false);
        g.drawString(font, t.kind().label(), bx + box + 6, y + 12, SystemTheme.TEXT_MUTED, false);
    }

    /** A free technique slot: a dim row with its key. */
    private static void emptyRow(GuiGraphics g, Font font, Rect r, int key) {
        PixelArt.frame(g, r.x(), r.y(), r.width(), r.height(), 1, PixelArt.SLATE, 0xB40D2238, 0);
        int bx = r.x() + 4, by = r.y() + (r.height() - 14) / 2;
        PixelArt.frame(g, bx, by, 14, 14, 1, PixelArt.SLATE, PixelArt.DEEP, 0);
        g.drawCenteredString(font, String.valueOf(key), bx + 8, by + 3, SystemTheme.TEXT_MUTED);
        g.drawString(font, Component.translatable("gui.murimblock.techniques.empty"), bx + 20, r.y() + 3, 0xFF6B8AA3, false);
        g.drawString(font, Component.translatable("gui.murimblock.techniques.empty_line"), bx + 20, r.y() + 12, 0xFF4E6A82, false);
    }

    /** A large seal for a crest or a technique: a framed diamond with its icon (7 x 7 icons drawn twice as big). */
    private static void seal(GuiGraphics g, int cx, int cy, int half, String[] icon, boolean gold) {
        PixelArt.diamond(g, cx, cy, half, PixelArt.OUTLINE);
        PixelArt.diamond(g, cx, cy, half - 1, gold ? PixelArt.GOLD : PixelArt.CYAN);
        PixelArt.diamond(g, cx, cy, half - 2, PixelArt.OUTLINE);
        PixelArt.diamond(g, cx, cy, half - 3, PixelArt.NIGHT);
        for (int dy = 1; dy <= half - 3; dy++) {
            int w = half - 3 - dy;
            PixelArt.rect(g, cx - w, cy + dy, 2 * w + 1, 1, PixelArt.DEEP);
        }
        if (icon == null) {
            return;
        }
        int scale = icon.length <= 7 ? 2 : 1;
        int size = icon.length * scale;
        for (int j = 0; j < icon.length; j++) {
            for (int i = 0; i < icon[j].length(); i++) {
                char c = icon[j].charAt(i);
                if (c != '.') {
                    PixelArt.rect(g, cx - size / 2 + i * scale, cy - size / 2 + j * scale, scale, scale, PixelArt.color(c));
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, Rect area, int mouseX, int mouseY, float partialTick) {
        TechniquesLayout layout = TechniquesLayout.of(area);
        Art active = MartialArtsData.active();
        renderArts(g, font, layout.arts(), active);
        label(g, font, layout.techniques().x(), layout.techniques().y(), "gui.murimblock.techniques.of_art");
        int y = layout.techniques().y() + LABEL_HEIGHT;
        int key = 0;
        for (Technique t : active.techniques()) {
            if (t.kind() == MartialArtsData.Kind.TECHNIQUE) {
                key++;
            }
            if (t.kind() == MartialArtsData.Kind.ULTIMATE) {
                for (int k = 0; k < emptySlots(active); k++) {
                    emptyRow(g, font, new Rect(layout.techniques().x(), y, layout.techniques().width(), TECHNIQUE_ROW), ++key);
                    y += TECHNIQUE_ROW + ROW_GAP;
                }
            }
            y += TECHNIQUE_ROW + ROW_GAP;
        }
        renderDetail(g, font, layout.detail(), active.techniques().get(Math.min(picked, active.techniques().size() - 1)));
    }

    private static void renderArts(GuiGraphics g, Font font, Rect r, Art active) {
        Rect card = new Rect(r.x(), r.y(), r.width(), CARD_HEIGHT);
        SystemTheme.box(g, card, true);
        seal(g, card.right() - 17, card.y() + 18, 12, TechniqueIcons.crest(active.weapon()), false);
        int x = card.x() + 7;
        label(g, font, x, card.y() + 6, "gui.murimblock.techniques.active");
        SystemTheme.glowText(g, font, active.name(), x, card.y() + 18, SystemTheme.TEXT);
        g.drawString(font, artLine(active), x, card.y() + 30, 0xFFB5DCF2, false);
        Component mastery = Component.translatable("gui.murimblock.percent", active.masteryPercent());
        g.drawString(font, Component.translatable("gui.murimblock.techniques.mastery"), x, card.y() + 42, SystemTheme.TEXT_MUTED, false);
        g.drawString(font, mastery, card.right() - 7 - font.width(mastery), card.y() + 42, SystemTheme.TEXT_MUTED, false);
        SystemTheme.bar(g, x, card.y() + 53, card.width() - 14, 7, active.masteryPercent() / 100f, SystemTheme.CYAN);
        // A diamond on the bar at each mastery that unlocks a technique, lit once reached.
        int inner = card.width() - 14 - 8;
        for (Technique t : active.techniques()) {
            if (t.unlockMastery() > 0) {
                int mx = x + 4 + Math.round(inner * t.unlockMastery() / 100f);
                PixelArt.diamond(g, mx, card.y() + 53, 2, PixelArt.OUTLINE);
                PixelArt.diamond(g, mx, card.y() + 53, 1, active.masteryPercent() >= t.unlockMastery() ? PixelArt.WHITE : PixelArt.MID);
            }
        }

        int y = card.bottom() + 8;
        label(g, font, r.x(), y, "gui.murimblock.techniques.others");
        y += LABEL_HEIGHT;
        for (Art art : MartialArtsData.known()) {
            if (art == active) {
                continue;
            }
            Rect row = new Rect(r.x(), y, r.width(), ART_ROW);
            PixelArt.frame(g, row.x(), row.y(), row.width(), row.height(), 1, PixelArt.MID, 0xD20F2944, 0);
            PixelArt.diamond(g, row.x() + 7, row.y() + ART_ROW / 2, 2, PixelArt.CYAN);
            g.drawString(font, art.name(), row.x() + 13, row.y() + 4, SystemTheme.TEXT, false);
            g.drawString(font, artLine(art), row.x() + 13, row.y() + 14, SystemTheme.TEXT_MUTED, false);
            y += ART_ROW + ROW_GAP;
        }

        Component notice = Component.translatable(MartialArtsData.inCombat()
                ? "gui.murimblock.techniques.notice_combat" : "gui.murimblock.techniques.notice");
        int noticeTop = notice(g, font, r, notice);

        // The masteries that unlock this art's techniques, ticked once reached, where room is left.
        List<Technique> unlocks = active.techniques().stream().filter(t -> t.unlockMastery() > 0).toList();
        int top = y + 4;
        if (!unlocks.isEmpty() && top + 11 + unlocks.size() * 10 <= noticeTop - 2) {
            label(g, font, r.x(), top, "gui.murimblock.techniques.unlocks");
            int uy = top + 11;
            for (Technique t : unlocks) {
                boolean reached = active.masteryPercent() >= t.unlockMastery();
                PixelArt.check(g, r.x() + 5, uy + 3, reached);
                g.drawString(font, t.unlockMastery() + "%", r.x() + 12, uy, reached ? 0xFF9FDCFF : 0xFF6B8AA3, false);
                g.drawString(font, t.name(), r.x() + 42, uy, reached ? SystemTheme.TEXT : 0xFF6B8AA3, false);
                uy += 10;
            }
        }
    }

    private static Component artLine(Art art) {
        return Component.translatable("gui.murimblock.techniques.art_line", art.weaponName(), art.rank());
    }

    /** The [!] notice at the bottom of {@code r}; returns its top. */
    private static int notice(GuiGraphics g, Font font, Rect r, Component text) {
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
        return box.y();
    }

    private static void renderDetail(GuiGraphics g, Font font, Rect r, Technique t) {
        SystemTheme.box(g, r, false);
        int x = r.x() + 10;
        int y = r.y() + 10;

        String kind = t.kind().label().getString().toUpperCase(java.util.Locale.ROOT);
        int tagWidth = SystemTheme.spacedWidth(font, kind, 1) + 12;
        PixelArt.frame(g, x, y, tagWidth, 14, 1, PixelArt.CYAN, PixelArt.DEEP, 0);
        seal(g, r.right() - 22, r.y() + 24, 15, TechniqueIcons.of(t.id()), t.kind() == MartialArtsData.Kind.ULTIMATE);
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
        PixelArt.divider(g, x, y + 4, r.width() - 20);

        Component unlock = t.unlockMastery() > 0
                ? Component.translatable("gui.murimblock.techniques.unlock_at", t.unlockMastery())
                : Component.translatable("gui.murimblock.techniques.unlock_start");
        g.drawString(font, unlock, x, r.bottom() - 16, SystemTheme.TEXT_MUTED, false);
    }

    private static void stat(GuiGraphics g, Font font, int x, int y, int w, String labelKey, Component value) {
        PixelArt.frame(g, x, y, w, 28, 1, PixelArt.MID, PixelArt.DEEP, 0);
        g.drawString(font, Component.translatable(labelKey), x + 6, y + 5, SystemTheme.TEXT_MUTED, false);
        SystemTheme.crispText(g, font, value, x + 6, y + 16, SystemTheme.TEXT);
    }

    private static void label(GuiGraphics g, Font font, int x, int y, String key) {
        SystemTheme.spaced(g, font, Component.translatable(key).getString(), x, y, SystemTheme.TEXT_MUTED, 1);
    }
}
