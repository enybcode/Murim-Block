package com.murimblock.client.combat;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.gui.data.MartialArtsData;
import com.murimblock.combat.CombatState;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * What combat mode shows (board {@code docs/gui/boards/combat-bar-j3.png}): in the bottom right, the active art's
 * techniques in System boxes on keys 1 to 3 and its ultimate on 4, under the Qi drawn as segments (10 Qi each, more once
 * that would pass 20 segments, the last one half full when started). The block slides in from the right when combat mode starts. A
 * short white flash covers the screen when a heavy blow lands on the player or breaks its guard.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatHud {
    /** Box sizes and gaps, in GUI pixels. */
    private static final int BOX = 18, ULT = 22, GAP = 3, SEGMENT = 6, SEGMENT_GAP = 1, SEGMENT_HEIGHT = 4, MARGIN = 8;
    private static final int GOLD = 0xFFFFD77A;
    /** Ticks the block takes to slide in. */
    private static final int SLIDE = 5;
    private static int flash;
    private static int shown;

    private CombatHud() {
    }

    /** A flash over the screen for a few ticks (a heavy blow taken). */
    public static void flash(int ticks) {
        flash = Math.max(flash, ticks);
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,
                "combat"), CombatHud::draw);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (flash > 0) {
            flash--;
        }
        shown = CombatClient.local().mode ? Math.min(SLIDE, shown + 1) : 0;
    }

    /** Qi held by one segment for a pool of {@code maxQi}: 10, or a round ten keeping the row to 20 segments. */
    static int segmentQi(int maxQi) {
        return maxQi <= 200 ? 10 : Mth.ceil(maxQi / 200f) * 10;
    }

    private static void draw(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        if (flash > 0) {
            int alpha = Math.min(110, flash * 40);
            graphics.fill(0, 0, width, height, alpha << 24 | 0xFFFFFF);
        }
        CombatState local = CombatClient.local();
        if (!local.mode || mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        float slide = Mth.clamp((shown + delta.getGameTimeDeltaPartialTick(false)) / SLIDE, 0, 1);
        int offset = Math.round((1 - slide) * (1 - slide) * 110);
        int right = width - MARGIN + offset, bottom = height - MARGIN - 10;
        var qi = ClientCultivation.get();
        MartialArtsData.Art art = MartialArtsData.active();
        segments(graphics, mc.font, right, bottom - ULT - 6, (float) qi.qi(), qi.maxQi());
        List<MartialArtsData.Technique> techniques = art.techniques().stream()
                .filter(t -> t.kind() == MartialArtsData.Kind.TECHNIQUE).limit(3).toList();
        MartialArtsData.Technique ultimate = art.techniques().stream()
                .filter(t -> t.kind() == MartialArtsData.Kind.ULTIMATE).findFirst().orElse(null);
        int ultX = right - ULT;
        slot(graphics, mc.font, ultX, bottom - ULT, ULT, ultimate, "4", art, (float) qi.qi(), true);
        for (int i = 0; i < 3; i++) {
            int x = ultX - (3 - i) * (BOX + GAP) - 1;
            slot(graphics, mc.font, x, bottom - BOX - 2, BOX, i < techniques.size() ? techniques.get(i) : null,
                    String.valueOf(i + 1), art, (float) qi.qi(), false);
        }
    }

    /** The Qi as a row of segments ending at {@code right}, its numbers above. */
    private static void segments(GuiGraphics g, Font font, int right, int y, float qi, int maxQi) {
        int per = segmentQi(maxQi), count = Math.max(1, Mth.ceil(maxQi / (float) per));
        int step = SEGMENT + SEGMENT_GAP, left = right - count * step + SEGMENT_GAP;
        for (int k = 0; k < count; k++) {
            int x = left + k * step;
            float fill = Mth.clamp((qi - k * per) / per, 0, 1);
            g.fill(x, y, x + SEGMENT, y + SEGMENT_HEIGHT, SystemTheme.withAlpha(SystemTheme.CYAN, 0x1A));
            if (fill >= 1) {
                g.fill(x, y, x + SEGMENT, y + SEGMENT_HEIGHT, SystemTheme.CYAN);
            } else if (fill > 0) {
                g.fill(x, y, x + SEGMENT / 2, y + SEGMENT_HEIGHT, SystemTheme.CYAN);
            }
            g.renderOutline(x, y, SEGMENT, SEGMENT_HEIGHT, SystemTheme.withAlpha(SystemTheme.CYAN, fill >= 1 ? 0xFF : 0x73));
        }
        Component label = Component.translatable("hud.murimblock.qi", (int) qi, maxQi);
        g.pose().pushPose();
        g.pose().translate(left, y - 6, 0);
        g.pose().scale(0.5f, 0.5f, 1);
        g.drawString(font, label, 0, 0, SystemTheme.TEXT_MUTED, false);
        g.pose().popPose();
    }

    /** One technique's box: its initials until icons exist, the key under it, greyed and pink when its Qi is lacking,
     * a lock with the mastery it waits for, gold for the ultimate. */
    private static void slot(GuiGraphics g, Font font, int x, int y, int size, MartialArtsData.Technique technique,
                             String key, MartialArtsData.Art art, float qi, boolean ultimate) {
        boolean locked = technique != null && technique.unlockMastery() > art.masteryPercent();
        boolean poor = technique != null && !locked && technique.qiCost() > qi;
        int edge = poor ? SystemTheme.WARNING : ultimate ? SystemTheme.withAlpha(GOLD, 0xA6) : SystemTheme.CYAN_SOFT;
        g.fill(x, y, x + size, y + size, SystemTheme.PANEL);
        g.renderOutline(x - 1, y - 1, size + 2, size + 2, SystemTheme.CYAN_FAINT);
        g.renderOutline(x, y, size, size, edge);
        int corner = ultimate ? GOLD : SystemTheme.CORNER;
        g.fill(x - 1, y - 1, x + 3, y, corner);
        g.fill(x - 1, y - 1, x, y + 3, corner);
        g.fill(x + size - 3, y + size, x + size + 1, y + size + 1, corner);
        g.fill(x + size, y + size - 3, x + size + 1, y + size + 1, corner);
        if (locked) {
            // A small padlock and the mastery it waits for.
            int cx = x + size / 2, top = y + size / 2 - 7;
            g.renderOutline(cx - 2, top, 5, 4, SystemTheme.TEXT_MUTED);
            g.fill(cx - 3, top + 3, cx + 4, top + 8, SystemTheme.TEXT_MUTED);
            String mastery = technique.unlockMastery() + "%";
            g.pose().pushPose();
            g.pose().translate(cx - font.width(mastery) / 4f, top + 10, 0);
            g.pose().scale(0.5f, 0.5f, 1);
            g.drawString(font, mastery, 0, 0, SystemTheme.TEXT_MUTED, false);
            g.pose().popPose();
        } else if (technique != null) {
            String text = initials(technique.name().getString());
            int color = poor ? SystemTheme.withAlpha(SystemTheme.WARNING, 0xB0) : ultimate ? GOLD : SystemTheme.TEXT;
            g.drawString(font, text, x + (size - font.width(text)) / 2 + 1, y + (size - 8) / 2 + 1, color, true);
        }
        g.pose().pushPose();
        g.pose().translate(x + size / 2f - font.width(key) / 4f, y + size + 2, 0);
        g.pose().scale(0.5f, 0.5f, 1);
        g.drawString(font, key, 0, 0, ultimate ? GOLD : SystemTheme.TEXT_MUTED, false);
        g.pose().popPose();
    }

    /** "River Thrust" → "RT": what a box shows until each technique has its icon. */
    static String initials(String name) {
        StringBuilder letters = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty() && letters.length() < 2) {
                letters.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return letters.toString();
    }
}
