package com.murimblock.client.gui.page;

import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemPageView;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.data.ProfileData;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

/** The STATUS page: the player, who they are, and their Qi. There are no attributes: all strength is Qi. */
public final class ProfilePage implements SystemPageView {
    private static final int ROW = 14;
    private static final int HEALTH = 0xFFFF5C74;

    @Override
    public void render(GuiGraphics g, Font font, Rect area, int mouseX, int mouseY, float partialTick) {
        ProfileData data = ProfileData.current();
        ProfileLayout layout = ProfileLayout.of(area);
        renderModel(g, font, layout.model(), mouseX, mouseY);
        renderIdentity(g, font, layout.identity(), data);
        renderQi(g, font, layout.qi(), data);
    }

    private static void renderModel(GuiGraphics g, Font font, Rect r, int mouseX, int mouseY) {
        SystemTheme.box(g, r, false);
        Component label = Component.translatable("gui.murimblock.profile.model");
        g.drawCenteredString(font, label, r.centerX(), r.bottom() - 13, SystemTheme.TEXT_MUTED);
        Rect stage = r.inset(6, 6, 6, 18);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            // Vanilla's entity drawing clips in GUI pixels, so it is given GUI pixels, outside the System's scale.
            float s = SystemScreen.drawScale();
            g.pose().pushPose();
            g.pose().scale(1 / s, 1 / s, 1);
            int size = Math.max(10, Math.round(stage.height() * 2 / 5f * s));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, Math.round(stage.x() * s), Math.round(stage.y() * s),
                    Math.round(stage.right() * s), Math.round(stage.bottom() * s), size, 0.0625F, mouseX * s, mouseY * s, player);
            g.pose().popPose();
        } else {
            silhouette(g, stage);
        }
    }

    /** A blocky outline standing in for the player when there is none (title screen). */
    private static void silhouette(GuiGraphics g, Rect stage) {
        int unit = Math.max(1, Math.min(stage.width() / 16, stage.height() / 32));
        int x = stage.centerX() - 8 * unit;
        int y = stage.y() + (stage.height() - 32 * unit) / 2;
        int color = SystemTheme.CYAN_SOFT;
        g.renderOutline(x + 4 * unit, y, 8 * unit, 8 * unit, color);
        g.renderOutline(x + 4 * unit, y + 8 * unit, 8 * unit, 12 * unit, color);
        g.renderOutline(x, y + 8 * unit, 4 * unit, 12 * unit, color);
        g.renderOutline(x + 12 * unit, y + 8 * unit, 4 * unit, 12 * unit, color);
        g.renderOutline(x + 4 * unit, y + 20 * unit, 4 * unit, 12 * unit, color);
        g.renderOutline(x + 8 * unit, y + 20 * unit, 4 * unit, 12 * unit, color);
    }

    private static final String[] LABELS = {
            "gui.murimblock.profile.name", "gui.murimblock.profile.realm", "gui.murimblock.profile.title",
            "gui.murimblock.profile.sect", "gui.murimblock.profile.art"};

    private static void renderIdentity(GuiGraphics g, Font font, Rect r, ProfileData data) {
        int valueX = r.x();
        for (String key : LABELS) {
            valueX = Math.max(valueX, r.x() + SystemTheme.spacedWidth(font, Component.translatable(key).getString(), 1) + 10);
        }
        int y = r.y() + 4;
        y = row(g, font, r.x(), valueX, y, LABELS[0], data.name(), true);
        y = row(g, font, r.x(), valueX, y, LABELS[1], data.realm(), true);
        y = row(g, font, r.x(), valueX, y, LABELS[2], data.title(), false);
        y = row(g, font, r.x(), valueX, y, LABELS[3], data.sect(), false);
        y = row(g, font, r.x(), valueX, y, LABELS[4], data.martialArt(), false);

        y += 4;
        g.fill(r.x(), y, r.right(), y + 1, SystemTheme.CYAN_SOFT);
        y += 10;
        bar(g, font, r, y, "gui.murimblock.profile.hp", HEALTH, data.health(), data.maxHealth());
        y += ROW + 2;
        bar(g, font, r, y, "gui.murimblock.profile.qi", SystemTheme.CYAN, data.qi(), data.maxQi());

        message(g, font, r, Component.translatable("gui.murimblock.profile.message"));
    }

    private static int row(GuiGraphics g, Font font, int x, int valueX, int y, String labelKey, Component value, boolean glow) {
        SystemTheme.spaced(g, font, Component.translatable(labelKey).getString(), x, y, SystemTheme.TEXT_MUTED, 1);
        if (glow) {
            SystemTheme.glowText(g, font, value, valueX, y, SystemTheme.TEXT);
        } else {
            g.drawString(font, value, valueX, y, SystemTheme.TEXT, false);
        }
        return y + ROW;
    }

    private static void bar(GuiGraphics g, Font font, Rect r, int y, String labelKey, int color, float value, float max) {
        SystemTheme.spaced(g, font, Component.translatable(labelKey).getString(), r.x(), y, color, 1);
        Component amount = Component.translatable("gui.murimblock.amount", Math.round(value), Math.round(max));
        int amountWidth = font.width(amount);
        int barX = r.x() + 22;
        int barWidth = r.right() - amountWidth - 8 - barX;
        SystemTheme.bar(g, barX, y, barWidth, 8, max > 0 ? value / max : 0, color);
        g.drawString(font, amount, r.right() - amountWidth, y, SystemTheme.TEXT, false);
    }

    /** The [!] system line at the bottom of the column. */
    private static void message(GuiGraphics g, Font font, Rect r, Component text) {
        int textX = r.x() + 8 + font.width("[!] ");
        List<FormattedCharSequence> lines = font.split(text, r.right() - 8 - textX);
        int height = lines.size() * 10 + 10;
        Rect box = new Rect(r.x(), r.bottom() - height, r.width(), height);
        SystemTheme.box(g, box, false);
        g.drawString(font, "[!]", r.x() + 8, box.y() + 6, SystemTheme.CYAN, false);
        int y = box.y() + 6;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, textX, y, 0xFFCFEEFF, false);
            y += 10;
        }
    }

    private static void renderQi(GuiGraphics g, Font font, Rect r, ProfileData data) {
        SystemTheme.glowSpaced(g, font, Component.translatable("gui.murimblock.profile.qi").getString(), r.x(), r.y() + 4,
                SystemTheme.TEXT, 2);

        Rect max = new Rect(r.x(), r.y() + 18, r.width(), 46);
        SystemTheme.box(g, max, false);
        g.drawCenteredString(font, Component.translatable("gui.murimblock.profile.max_qi"), max.centerX(), max.y() + 6,
                SystemTheme.TEXT_MUTED);
        String number = String.valueOf(data.maxQi());
        g.pose().pushPose();
        g.pose().translate(max.centerX() - font.width(number), max.y() + 19, 0);
        g.pose().scale(2, 2, 1);
        // At double size the halo smears: a single cyan drop shadow reads cleaner.
        g.drawString(font, number, 1, 1, SystemTheme.withAlpha(SystemTheme.CYAN, 0x90), false);
        g.drawString(font, number, 0, 0, SystemTheme.TEXT, false);
        g.pose().popPose();

        int y = max.bottom() + 6;
        y = stat(g, font, r, y, "gui.murimblock.profile.regen",
                Component.translatable("gui.murimblock.profile.regen_value", format(data.qiPerSecond())), true);
        y = stat(g, font, r, y, "gui.murimblock.profile.breakthrough",
                Component.translatable("gui.murimblock.percent", data.breakthroughPercent()), true);
        stat(g, font, r, y, "gui.murimblock.profile.next_realm", data.nextRealm(), false);
    }

    private static int stat(GuiGraphics g, Font font, Rect r, int y, String labelKey, Component value, boolean glow) {
        Component label = Component.translatable(labelKey);
        g.drawString(font, label, r.x(), y + 4, 0xFFB5DCF2, false);
        int x = r.right() - font.width(value);
        // A value too long to sit beside its label goes on the line below.
        int valueY = x < r.x() + font.width(label) + 6 ? y + 15 : y + 4;
        if (glow) {
            SystemTheme.crispText(g, font, value, x, valueY, SystemTheme.TEXT);
        } else {
            g.drawString(font, value, x, valueY, SystemTheme.TEXT, false);
        }
        int bottom = valueY + 13;
        g.fill(r.x(), bottom - 1, r.right(), bottom, SystemTheme.CYAN_FAINT);
        return bottom;
    }

    private static String format(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value)) : String.valueOf(value);
    }
}
