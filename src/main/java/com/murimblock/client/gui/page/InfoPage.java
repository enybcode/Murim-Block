package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SubTabs;
import com.murimblock.client.gui.SystemButton;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.data.InfoData;
import com.murimblock.client.gui.data.InfoData.Control;
import com.murimblock.client.gui.data.InfoData.Goal;
import com.murimblock.client.gui.data.InfoData.LogEntry;
import com.murimblock.client.gui.data.InfoData.Quest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** The INFO page: sub-tabs Quests (daily training, main quests), System log and Guide (controls, topics). */
public final class InfoPage implements SystemPageView {
    public static final int QUESTS = 0;
    public static final int LOG = 1;
    public static final int GUIDE = 2;

    private static final int QUEST_ROW = 24;
    private static final int HEADER = 13;
    private static final int ROW_GAP = 3;
    private static final int LOG_ROW = 22;
    private static final int CONTROL_ROW = 18;
    private static final int TOPIC_ROW = 18;

    private int tab;
    private int quest;
    private int topic;

    public InfoPage(int tab) {
        this.tab = tab;
    }

    /** Where each quest row sits, and whether a group header comes before it. */
    private record QuestSlot(Quest quest, int index, int y, boolean header) {
    }

    private static List<QuestSlot> questSlots(Rect list) {
        List<QuestSlot> slots = new ArrayList<>();
        int y = list.y();
        InfoData.Group group = null;
        for (int i = 0; i < InfoData.QUESTS.size(); i++) {
            Quest q = InfoData.QUESTS.get(i);
            boolean header = q.group() != group;
            if (header) {
                group = q.group();
                y += HEADER;
            }
            slots.add(new QuestSlot(q, i, y, header));
            y += QUEST_ROW + ROW_GAP;
        }
        return slots;
    }

    @Override
    public void init(SystemScreen screen, Rect area) {
        InfoLayout layout = InfoLayout.of(area);
        SubTabs.add(screen, layout.subTabs(), List.of(
                Component.translatable("gui.murimblock.info.tab.quests"),
                Component.translatable("gui.murimblock.info.tab.log"),
                Component.translatable("gui.murimblock.info.tab.guide")), tab, index -> {
                    tab = index;
                    screen.refresh();
                });
        if (tab == QUESTS) {
            Rect list = layout.questColumns()[0];
            for (QuestSlot slot : questSlots(list)) {
                Quest q = slot.quest();
                screen.addPageWidget(new SystemButton(list.x(), slot.y(), list.width(), QUEST_ROW, q.name(),
                        SystemButton.Style.ROW, () -> {
                            quest = slot.index();
                            screen.refresh();
                        }).selected(slot.index() == quest).content((g, font, x, y, w, h) -> questRow(g, font, x, y, w, q)));
            }
        } else if (tab == GUIDE) {
            Rect topics = layout.guideColumns()[1];
            int y = topics.y() + HEADER;
            for (int i = 0; i < InfoData.TOPICS.size(); i++) {
                int index = i;
                screen.addPageWidget(new SystemButton(topics.x(), y, topics.width(), TOPIC_ROW,
                        topicName(InfoData.TOPICS.get(i)), SystemButton.Style.ROW, () -> {
                            topic = index;
                            screen.refresh();
                        }).selected(i == topic));
                y += TOPIC_ROW + ROW_GAP;
            }
        }
    }

    private static void questRow(GuiGraphics g, Font font, int x, int y, int w, Quest q) {
        g.drawString(font, q.name(), x + 6, y + 3, SystemTheme.TEXT, false);
        g.drawString(font, q.giverName(), x + 6, y + 13, SystemTheme.TEXT_MUTED, false);
        String progress = q.completedGoals() + " / " + q.goals().size();
        g.drawString(font, progress, x + w - 6 - font.width(progress), y + 8, 0xFFBFEEFF, false);
    }

    @Override
    public void render(GuiGraphics g, Font font, Rect area, int mouseX, int mouseY, float partialTick) {
        InfoLayout layout = InfoLayout.of(area);
        g.fill(area.x(), layout.subTabs().bottom(), area.right(), layout.subTabs().bottom() + 1, SystemTheme.CYAN_FAINT);
        switch (tab) {
            case QUESTS -> renderQuests(g, font, layout.questColumns());
            case LOG -> renderLog(g, font, layout.body());
            default -> renderGuide(g, font, layout.guideColumns());
        }
    }

    // Quests

    private void renderQuests(GuiGraphics g, Font font, Rect[] c) {
        for (QuestSlot slot : questSlots(c[0])) {
            if (slot.header()) {
                String key = "gui.murimblock.info.group." + slot.quest().group().name().toLowerCase(Locale.ROOT);
                label(g, font, c[0].x(), slot.y() - HEADER + 2, key);
            }
        }
        Quest q = InfoData.QUESTS.get(quest);
        Rect r = c[1];
        SystemTheme.box(g, r, false);
        int x = r.x() + 10;
        int y = r.y() + 10;
        SystemTheme.glowText(g, font, q.name().copy().withStyle(s -> s.withBold(true)), x, y, SystemTheme.TEXT);
        if (!q.timer().isEmpty()) {
            Component timer = Component.translatable("gui.murimblock.info.resets", q.timer());
            g.drawString(font, timer, r.right() - 10 - font.width(timer), y, SystemTheme.TEXT_MUTED, false);
        }
        y += 15;
        for (FormattedCharSequence line : font.split(q.description(), r.width() - 20)) {
            g.drawString(font, line, x, y, 0xFFCFEEFF, false);
            y += 10;
        }
        y += 6;
        for (Goal goal : q.goals()) {
            g.drawString(font, goal.label(), x, y, SystemTheme.TEXT, false);
            Component count = Component.translatable("gui.murimblock.amount", goal.done(), goal.total());
            SystemTheme.glowText(g, font, count, r.right() - 10 - font.width(count), y, SystemTheme.TEXT);
            SystemTheme.bar(g, x, y + 11, r.width() - 20, 6, (float) goal.done() / goal.total(), SystemTheme.CYAN);
            y += 24;
        }

        int bottom = r.bottom() - 10;
        if (q.warning()) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("gui.murimblock.info.warning", q.warningText()),
                    r.width() - 34);
            int h = lines.size() * 10 + 8;
            Rect box = new Rect(x, bottom - h, r.width() - 20, h);
            g.renderOutline(box.x(), box.y(), box.width(), box.height(), SystemTheme.withAlpha(SystemTheme.WARNING, 0x80));
            int ly = box.y() + 5;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, box.x() + 7, ly, 0xFFFFC4CC, false);
                ly += 10;
            }
            bottom = box.y() - 6;
        }
        String reward = Component.translatable("gui.murimblock.info.reward").getString();
        SystemTheme.spaced(g, font, reward, x, bottom - 8, SystemTheme.TEXT_MUTED, 1);
        g.drawString(font, q.reward(), x + SystemTheme.spacedWidth(font, reward, 1) + 8, bottom - 8, SystemTheme.TEXT, false);
    }

    // Log

    private static void renderLog(GuiGraphics g, Font font, Rect r) {
        int y = r.y();
        int kindX = r.x() + 26;
        int textX = kindX + 72;
        for (LogEntry entry : InfoData.log()) {
            if (y + LOG_ROW > r.bottom()) {
                break;
            }
            g.renderOutline(r.x(), y, r.width(), LOG_ROW, SystemTheme.CYAN_FAINT);
            int ty = y + (LOG_ROW - 8) / 2;
            g.drawString(font, "[!]", r.x() + 7, ty, SystemTheme.CYAN, false);
            SystemTheme.spaced(g, font, entry.kindName().getString().toUpperCase(Locale.ROOT), kindX, ty, SystemTheme.CYAN, 1);
            g.drawString(font, entry.text(), textX, ty, 0xFFD5EFFF, false);
            g.drawString(font, entry.time(), r.right() - 8 - font.width(entry.time()), ty, SystemTheme.TEXT_MUTED, false);
            y += LOG_ROW + ROW_GAP;
        }
    }

    // Guide

    private void renderGuide(GuiGraphics g, Font font, Rect[] c) {
        label(g, font, c[0].x(), c[0].y() + 2, "gui.murimblock.info.controls");
        int y = c[0].y() + HEADER;
        for (Control control : InfoData.controls()) {
            int kw = Math.max(20, font.width(control.key()) + 10);
            g.renderOutline(c[0].x(), y + 1, kw, 14, SystemTheme.CYAN);
            g.drawCenteredString(font, control.key(), c[0].x() + kw / 2, y + 4, 0xFFD6F4FF);
            g.drawString(font, control.action(), c[0].x() + 38 + Math.max(0, kw - 30), y + 4, 0xFFD5EFFF, false);
            g.fill(c[0].x(), y + CONTROL_ROW, c[0].right(), y + CONTROL_ROW + 1, SystemTheme.withAlpha(SystemTheme.CYAN, 0x20));
            y += CONTROL_ROW + 2;
        }

        label(g, font, c[1].x(), c[1].y() + 2, "gui.murimblock.info.topics");

        Rect r = c[2];
        SystemTheme.box(g, r, false);
        String id = InfoData.TOPICS.get(topic);
        SystemTheme.glowText(g, font, topicName(id).copy().withStyle(s -> s.withBold(true)), r.x() + 10, r.y() + 10, SystemTheme.TEXT);
        int ty = r.y() + 26;
        for (FormattedCharSequence line : font.split(Component.translatable("murimblock.guide." + id + ".text"), r.width() - 20)) {
            g.drawString(font, line, r.x() + 10, ty, 0xFFD5EFFF, false);
            ty += 11;
        }
    }

    private static Component topicName(String id) {
        return Component.translatable("murimblock.guide." + id);
    }

    private static void label(GuiGraphics g, Font font, int x, int y, String key) {
        SystemTheme.spaced(g, font, Component.translatable(key).getString(), x, y, SystemTheme.TEXT_MUTED, 1);
    }
}
