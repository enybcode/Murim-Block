package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemButton;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.SubTabs;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.data.CultivationData;
import com.murimblock.client.gui.data.CultivationData.Acupoint;
import com.murimblock.client.gui.data.CultivationData.Meridian;
import com.murimblock.client.gui.data.CultivationData.Realm;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** The CULTIVATION page: sub-tabs Realm (ranks, dantian, breakthrough trial) and Meridians (body, acupoints). */
public final class CultivationPage implements SystemPageView {
    private static final int REALM_ROW = 25;
    private static final int MERIDIAN_ROW = 15;
    private static final int ROW_GAP = 2;
    private static final int RING_RADIUS = 42;
    private static final int RING_DOTS = 132;

    private boolean meridians;
    private int picked;
    private final MeridianCamera camera = new MeridianCamera();
    private long lastFrame;

    /** Opens on a sub-tab; the Meridians one also serves the dev screenshots. */
    public CultivationPage(boolean meridians) {
        this.meridians = meridians;
    }

    @Override
    public void init(SystemScreen screen, Rect area) {
        CultivationLayout layout = CultivationLayout.of(area);
        SubTabs.add(screen, layout.subTabs(), List.of(
                Component.translatable("gui.murimblock.cultivation.tab.realm"),
                Component.translatable("gui.murimblock.cultivation.tab.meridians", Math.min(CultivationData.openMeridians(), 12), 12)),
                meridians ? 1 : 0, index -> show(screen, index == 1));

        if (meridians) {
            initMeridians(screen, layout.meridianColumns());
        } else {
            initRealm(screen, layout.realmColumns());
        }
    }

    private void show(SystemScreen screen, boolean meridians) {
        this.meridians = meridians;
        screen.refresh();
    }

    private void initRealm(SystemScreen screen, Rect[] columns) {
        Rect center = columns[1];
        Component meditate = Component.translatable(CultivationData.meditating()
                ? "gui.murimblock.cultivation.stop_meditating" : "gui.murimblock.cultivation.meditate");
        screen.addPageWidget(new SystemButton(center.x() + 10, center.bottom() - 26, center.width() - 20, 16, meditate,
                SystemButton.Style.ACTION, () -> {
                    CultivationData.toggleMeditation();
                    screen.refresh();
                }));
        Rect trial = columns[2];
        boolean ready = CultivationData.qi() >= CultivationData.qiToFill()
                && CultivationData.openMeridians() >= CultivationData.MERIDIANS_FOR_TRIAL;
        screen.addPageWidget(new SystemButton(trial.x(), trial.bottom() - 16, trial.width(), 16,
                Component.translatable("gui.murimblock.cultivation.begin_trial"), SystemButton.Style.ACTION, () -> {
                }).enabled(ready));
    }

    private void initMeridians(SystemScreen screen, Rect[] columns) {
        Rect list = columns[0];
        List<Meridian> all = CultivationData.MERIDIANS;
        int y = list.y();
        for (int i = 0; i < all.size(); i++) {
            int index = i;
            Meridian m = all.get(i);
            screen.addPageWidget(new SystemButton(list.x(), y, list.width(), MERIDIAN_ROW, m.name(), SystemButton.Style.ROW, () -> {
                picked = index;
                screen.refresh();
            }).selected(i == picked).content((g, font, x, ry, w, h) -> meridianRow(g, font, x, ry, w, index, m)));
            y += MERIDIAN_ROW + ROW_GAP;
        }
        if (CultivationData.isNext(picked)) {
            Rect detail = columns[2];
            screen.addPageWidget(new SystemButton(detail.x() + 8, detail.bottom() - 24, detail.width() - 16, 16,
                    Component.translatable("gui.murimblock.cultivation.open", CultivationData.openCost(picked)),
                    SystemButton.Style.ACTION, () -> {
                        CultivationData.openNext();
                        picked = Math.min(picked + 1, all.size() - 1);
                        screen.refresh();
                    }));
        }
    }

    private static void meridianRow(GuiGraphics g, Font font, int x, int y, int w, int index, Meridian m) {
        diamond(g, x + 7, y + 7, CultivationData.isOpen(index), CultivationData.isNext(index));
        Component name = listName(m);
        g.drawString(font, name, x + 14, y + 4, SystemTheme.TEXT, false);
        if (!m.id().equals("small_heavenly_circuit")) {
            g.drawString(font, m.code(), x + w - 4 - font.width(m.code()), y + 4, SystemTheme.TEXT_MUTED, false);
        }
    }

    /** The list uses a short name when the full one does not fit. */
    private static Component listName(Meridian m) {
        String key = "murimblock.meridian." + m.id() + ".short";
        return Language.getInstance().has(key) ? Component.translatable(key) : m.name();
    }

    /** A small pixel diamond: filled when open, outlined when next, faint when sealed. */
    private static void diamond(GuiGraphics g, int cx, int cy, boolean open, boolean next) {
        int color = open ? SystemTheme.CYAN : next ? SystemTheme.CYAN : SystemTheme.withAlpha(SystemTheme.CYAN, 0x50);
        int[] half = {0, 1, 2, 1, 0};
        for (int row = 0; row < 5; row++) {
            int h = half[row];
            int y = cy - 2 + row;
            if (open) {
                g.fill(cx - h, y, cx + h + 1, y + 1, color);
            } else {
                g.fill(cx - h, y, cx - h + 1, y + 1, color);
                g.fill(cx + h, y, cx + h + 1, y + 1, color);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, Rect area, int mouseX, int mouseY, float partialTick) {
        CultivationLayout layout = CultivationLayout.of(area);
        g.fill(area.x(), layout.subTabs().bottom(), area.right(), layout.subTabs().bottom() + 1, SystemTheme.CYAN_FAINT);
        if (meridians) {
            renderMeridians(g, font, layout.meridianColumns());
        } else {
            renderRealm(g, font, layout.realmColumns());
        }
    }

    // Realm

    private static void renderRealm(GuiGraphics g, Font font, Rect[] c) {
        renderLadder(g, font, c[0]);
        renderDantian(g, font, c[1]);
        renderTrial(g, font, c[2]);
    }

    private static void renderLadder(GuiGraphics g, Font font, Rect r) {
        int current = CultivationData.currentRealm();
        int y = r.y();
        for (int i = 0; i < CultivationData.REALMS.size(); i++) {
            Realm realm = CultivationData.REALMS.get(i);
            Rect row = new Rect(r.x(), y, r.width(), REALM_ROW);
            int text;
            if (i == current) {
                SystemTheme.box(g, row, true);
                text = SystemTheme.TEXT;
            } else {
                g.renderOutline(row.x(), row.y(), row.width(), row.height(),
                        i < current ? SystemTheme.CYAN_FAINT : SystemTheme.withAlpha(SystemTheme.CYAN, 0x1A));
                text = i < current ? 0xFF9FC6DD : 0xFF6F93AB;
            }
            g.drawString(font, String.valueOf(i + 1), row.x() + 6, row.y() + 9, SystemTheme.CYAN, false);
            boolean shown = CultivationData.revealed(i);
            Component unknown = Component.translatable("gui.murimblock.unknown");
            g.drawString(font, shown ? realm.rankName() : unknown, row.x() + 18, row.y() + 4, text, false);
            g.drawString(font, shown && !realm.realm().equals("unknown") ? realm.realmName() : unknown,
                    row.x() + 18, row.y() + 14, SystemTheme.TEXT_MUTED, false);
            y += REALM_ROW + 3;
        }
    }

    private static void renderDantian(GuiGraphics g, Font font, Rect r) {
        SystemTheme.box(g, r, false);
        int cx = r.centerX();
        int cy = r.y() + 52;
        float fraction = (float) CultivationData.qi() / CultivationData.qiToFill();
        int lit = Math.round(fraction * RING_DOTS);
        for (int i = 0; i < RING_DOTS; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / RING_DOTS;
            int x = (int) Math.round(cx + Math.cos(angle) * RING_RADIUS);
            int y = (int) Math.round(cy + Math.sin(angle) * RING_RADIUS);
            if (i < lit) {
                g.fill(x - 2, y - 2, x + 3, y + 3, SystemTheme.withAlpha(SystemTheme.CYAN, 0x30));
            }
            g.fill(x - 1, y - 1, x + 2, y + 2, i < lit ? SystemTheme.CYAN : SystemTheme.CYAN_FAINT);
        }
        Component percent = Component.translatable("gui.murimblock.percent", Math.round(fraction * 100))
                .withStyle(s -> s.withBold(true));
        SystemTheme.glowText(g, font, percent, cx - font.width(percent) / 2, cy - 9, SystemTheme.TEXT);
        Component amount = Component.translatable("gui.murimblock.cultivation.qi_amount", CultivationData.qi(), CultivationData.qiToFill());
        g.drawCenteredString(font, amount, cx, cy + 4, SystemTheme.TEXT_MUTED);

        Realm realm = CultivationData.REALMS.get(CultivationData.currentRealm());
        int y = cy + RING_RADIUS + 6;
        Component rank = realm.rankName();
        SystemTheme.glowText(g, font, rank, cx - font.width(rank) / 2, y, SystemTheme.TEXT);
        y += 11;
        g.drawCenteredString(font, Component.translatable("gui.murimblock.cultivation.realm_line", realm.realmName(),
                CultivationData.stage(), CultivationData.STAGES), cx, y, 0xFFB5DCF2);
        y += 13;
        int pip = 10;
        int gap = 3;
        int px = cx - (CultivationData.STAGES * (pip + gap) - gap) / 2;
        for (int i = 0; i < CultivationData.STAGES; i++) {
            if (i < CultivationData.stage()) {
                g.fill(px, y, px + pip, y + 3, SystemTheme.CYAN);
            } else {
                g.renderOutline(px, y, pip, 3, SystemTheme.CYAN_SOFT);
            }
            px += pip + gap;
        }
        y += 10;
        source(g, font, r, y, "gui.murimblock.cultivation.source.meditation", "gui.murimblock.cultivation.source.meditation_value");
        source(g, font, r, y + 11, "gui.murimblock.cultivation.source.combat", "gui.murimblock.cultivation.source.combat_value");
        source(g, font, r, y + 22, "gui.murimblock.cultivation.source.pills", "gui.murimblock.cultivation.source.pills_value");
    }

    private static void source(GuiGraphics g, Font font, Rect r, int y, String labelKey, String valueKey) {
        g.drawString(font, Component.translatable(labelKey), r.x() + 10, y, SystemTheme.TEXT_MUTED, false);
        Component value = Component.translatable(valueKey);
        g.drawString(font, value, r.right() - 10 - font.width(value), y, SystemTheme.TEXT, false);
    }

    private static void renderTrial(GuiGraphics g, Font font, Rect r) {
        label(g, font, r.x(), r.y(), "gui.murimblock.cultivation.trial");
        List<FormattedCharSequence> desc = font.split(Component.translatable("murimblock.trial.heart_demon.desc"), r.width() - 16);
        Rect card = new Rect(r.x(), r.y() + 13, r.width(), 24 + desc.size() * 10);
        SystemTheme.box(g, card, false);
        SystemTheme.glowText(g, font, Component.translatable("murimblock.trial.heart_demon").withStyle(s -> s.withBold(true)),
                card.x() + 8, card.y() + 7, SystemTheme.TEXT);
        int y = card.y() + 20;
        for (FormattedCharSequence line : desc) {
            g.drawString(font, line, card.x() + 8, y, 0xFFD5EFFF, false);
            y += 10;
        }

        y = card.bottom() + 8;
        boolean full = CultivationData.qi() >= CultivationData.qiToFill();
        check(g, font, r.x(), y, full, Component.translatable("gui.murimblock.cultivation.req_dantian",
                CultivationData.qi(), CultivationData.qiToFill()));
        y += 13;
        int open = CultivationData.openMeridians();
        check(g, font, r.x(), y, open >= CultivationData.MERIDIANS_FOR_TRIAL, Component.translatable(
                "gui.murimblock.cultivation.req_meridians", Math.min(open, CultivationData.MERIDIANS_FOR_TRIAL),
                CultivationData.MERIDIANS_FOR_TRIAL));
        y += 17;

        List<FormattedCharSequence> warning = font.split(Component.translatable("gui.murimblock.cultivation.trial_warning"),
                r.width() - 14);
        Rect box = new Rect(r.x(), y, r.width(), warning.size() * 10 + 8);
        g.renderOutline(box.x(), box.y(), box.width(), box.height(), SystemTheme.withAlpha(SystemTheme.WARNING, 0x80));
        int ly = box.y() + 5;
        for (FormattedCharSequence line : warning) {
            g.drawString(font, line, box.x() + 7, ly, 0xFFFFC4CC, false);
            ly += 10;
        }
    }

    private static void check(GuiGraphics g, Font font, int x, int y, boolean done, Component text) {
        if (done) {
            g.fill(x, y, x + 8, y + 8, SystemTheme.CYAN);
        }
        g.renderOutline(x, y, 8, 8, SystemTheme.CYAN);
        g.drawString(font, text, x + 13, y, SystemTheme.TEXT, false);
    }

    // Meridians

    private void renderMeridians(GuiGraphics g, Font font, Rect[] c) {
        Rect vp = c[1];
        SystemTheme.box(g, vp, false);
        Meridian chosen = CultivationData.MERIDIANS.get(picked);

        long now = Util.getMillis();
        float seconds = lastFrame == 0 ? 0 : (now - lastFrame) / 1000f;
        lastFrame = now;
        MeridianCamera.View view = camera.follow(target(chosen, vp), seconds);
        MeridianBody.render(g, font, vp, view, picked, now);
        SystemTheme.spaced(g, font, chosen.regionName().getString().toUpperCase(Locale.ROOT), vp.x() + 6, vp.y() + 6,
                SystemTheme.TEXT_MUTED, 1);

        renderMeridianDetail(g, font, c[2], chosen);
    }

    private static MeridianCamera.View target(Meridian m, Rect vp) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Acupoint a : m.points()) {
            minX = Math.min(minX, a.x());
            minY = Math.min(minY, a.y());
            maxX = Math.max(maxX, a.x());
            maxY = Math.max(maxY, a.y());
        }
        return MeridianCamera.fit(minX, minY, maxX, maxY, vp.width(), vp.height());
    }

    private void renderMeridianDetail(GuiGraphics g, Font font, Rect r, Meridian m) {
        SystemTheme.box(g, r, false);
        int x = r.x() + 8;
        int y = r.y() + 8;
        String status = Component.translatable(CultivationData.isOpen(picked) ? "gui.murimblock.cultivation.status.open"
                : CultivationData.isNext(picked) ? "gui.murimblock.cultivation.status.next"
                : "gui.murimblock.cultivation.status.sealed").getString();
        int tagWidth = SystemTheme.spacedWidth(font, status, 1) + 10;
        g.renderOutline(x, y, tagWidth, 13, SystemTheme.CYAN);
        SystemTheme.spaced(g, font, status, x + 5, y + 3, 0xFFBFEEFF, 1);
        Component count = Component.translatable("gui.murimblock.cultivation.open_count", Math.min(CultivationData.openMeridians(), 12), 12);
        g.drawString(font, count, r.right() - 8 - font.width(count), y + 3, SystemTheme.TEXT_MUTED, false);
        y += 20;

        for (FormattedCharSequence line : font.split(m.name().copy().withStyle(s -> s.withBold(true)), r.width() - 16)) {
            g.drawString(font, line, x + 1, y + 1, SystemTheme.withAlpha(SystemTheme.CYAN, 0x90), false);
            g.drawString(font, line, x, y, SystemTheme.TEXT, false);
            y += 11;
        }
        y += 3;
        for (FormattedCharSequence line : font.split(m.effect(), r.width() - 16)) {
            g.drawString(font, line, x, y, 0xFFD5EFFF, false);
            y += 10;
        }
        y += 6;
        label(g, font, x, y, "gui.murimblock.cultivation.acupoints");
        y += 12;
        boolean open = CultivationData.isOpen(picked);
        for (Acupoint a : m.points()) {
            diamond(g, x + 2, y + 3, open, !open);
            g.drawString(font, a.code(), x + 9, y, 0xFFBFEEFF, false);
            g.drawString(font, a.name(), x + 40, y, 0xFFD5EFFF, false);
            y += 10;
        }
    }

    private static void label(GuiGraphics g, Font font, int x, int y, String key) {
        SystemTheme.spaced(g, font, Component.translatable(key).getString(), x, y, SystemTheme.TEXT_MUTED, 1);
    }
}
