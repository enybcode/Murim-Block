package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationPayloads;
import com.murimblock.cultivation.CultivationRealms;
import com.murimblock.cultivation.CultivationRules;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The breathing mini-game drawn while meditating (validated mockup "Meditation — breathing mini-game"): a ring that
 * breathes around a target circle above the crosshair, the Harmony box beside it, the stage gauge above the hotbar and
 * [!] System prompts. Pressing the meditation key while meditating is a breath; the server keeps Harmony.
 */
public final class BreathingHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "breathing");
    /** Target circle radius in GUI pixels; the ring goes from 0.4 to 1.5 times it. */
    private static final int TARGET_RADIUS = 27;
    /** Ticks a GOOD or MISS stays lit, and ticks before another press counts (the server allows one per 15). */
    private static final int FEEDBACK_TICKS = 12;
    private static final int PRESS_COOLDOWN = 15;
    private static final int MISS = 0xFFFFB067;
    private static final int FLASH = 0xFFD6F4FF;

    private static int meditationTicks;
    private static int cooldown;
    private static int feedback;
    private static boolean lastGood;

    private BreathingHud() {
    }

    /** A press of the meditation key while meditating. */
    public static void breathe() {
        if (cooldown > 0 || full(ClientCultivation.get())) {
            return;
        }
        lastGood = BreathCycle.good(seconds(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
        feedback = FEEDBACK_TICKS;
        cooldown = PRESS_COOLDOWN;
        PacketDistributor.sendToServer(new CultivationPayloads.Breath(lastGood));
    }

    /** Dev screenshots: light GOOD or MISS without a press. */
    public static void showFeedback(boolean good) {
        lastGood = good;
        feedback = FEEDBACK_TICKS;
    }

    private static float seconds(float partialTick) {
        return (meditationTicks + partialTick) / 20f;
    }

    private static boolean full(Cultivation.Snapshot c) {
        return CultivationRules.gaugeFull(c.realm(), c.stage(), c.stageQi());
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Cultivation.Snapshot c = ClientCultivation.get();
        if (!c.meditating() || mc.options.hideGui || mc.player == null) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = Math.round(h * 0.35f);
        boolean full = full(c);
        float seconds = seconds(delta.getGameTimeDeltaPartialTick(false));

        drawRing(g, font, cx, cy, seconds, full);
        drawHarmony(g, font, new Rect(cx + TARGET_RADIUS + 22, cy - 22, 104, 44), c, full);
        drawGauge(g, font, c, cx, h);
        if (full) {
            Component text = Component.translatable("hud.murimblock.meditation.full");
            int bw = font.width("[!] ") + font.width(text) + 20;
            prompt(g, font, new Rect(cx - bw / 2, 8, bw, 18), text);
        } else {
            Component text = Component.translatable("hud.murimblock.meditation.prompt",
                    MurimKeys.MEDITATE.getTranslatedKeyMessage());
            prompt(g, font, new Rect(6, 6, font.width("[!] ") + font.width(text) + 12, 16), text);
        }
    }

    private static void drawRing(GuiGraphics g, Font font, int cx, int cy, float seconds, boolean full) {
        int alpha = full ? 0x59 : 0xFF;
        boolean lit = feedback > 0 && !full;
        int targetColor = lit ? (lastGood ? 0xFFFFFFFF : SystemTheme.withAlpha(MISS, 0xCC)) : 0xE6D6F4FF;
        int ringColor = lit ? (lastGood ? FLASH : MISS) : SystemTheme.CYAN;
        if (lit && lastGood) {
            circle(g, cx, cy, TARGET_RADIUS + 1, 1, SystemTheme.withAlpha(SystemTheme.CYAN, 0x80));
            circle(g, cx, cy, TARGET_RADIUS - 2, 1, SystemTheme.withAlpha(SystemTheme.CYAN, 0x80));
        }
        circle(g, cx, cy, TARGET_RADIUS, 1, SystemTheme.withAlpha(targetColor, alpha * (targetColor >>> 24) / 0xFF));
        float scale = full ? 1 : BreathCycle.scale(seconds);
        int radius = Math.round(TARGET_RADIUS * scale);
        circle(g, cx, cy, radius + 1, 1, SystemTheme.withAlpha(ringColor, alpha / 3));
        circle(g, cx, cy, radius, 2, SystemTheme.withAlpha(ringColor, alpha));
        if (full) {
            return;
        }
        String word = lit ? (lastGood ? "GOOD" : "MISS")
                : BreathCycle.inhaling(seconds) ? "INHALE" : "EXHALE";
        int color = lit ? (lastGood ? 0xFFFFFFFF : MISS) : 0xFFCFEEFF;
        int x = cx - SystemTheme.spacedWidth(font, word, 1) / 2;
        if (lit && lastGood) {
            SystemTheme.glowSpaced(g, font, word, x, cy - 4, color, 1);
        } else {
            SystemTheme.spaced(g, font, word, x, cy - 4, color, 1);
        }
    }

    /** A pixel circle: square dots along the circumference, thickness pixels thick inward. */
    private static void circle(GuiGraphics g, int cx, int cy, int radius, int thickness, int color) {
        for (int t = 0; t < thickness; t++) {
            int r = radius - t;
            if (r <= 0) {
                return;
            }
            int steps = Math.max(16, (int) (2 * Math.PI * r * 1.5));
            int lastX = Integer.MIN_VALUE;
            int lastY = Integer.MIN_VALUE;
            for (int i = 0; i < steps; i++) {
                double a = 2 * Math.PI * i / steps;
                int x = cx + (int) Math.round(Math.cos(a) * r);
                int y = cy + (int) Math.round(Math.sin(a) * r);
                if (x != lastX || y != lastY) {
                    g.fill(x, y, x + 1, y + 1, color);
                    lastX = x;
                    lastY = y;
                }
            }
        }
    }

    private static void drawHarmony(GuiGraphics g, Font font, Rect r, Cultivation.Snapshot c, boolean full) {
        hudBox(g, r);
        int x = r.x() + 6;
        SystemTheme.spaced(g, font, "HARMONY", x, r.y() + 5, 0xFF8FDCFF, 1);
        String percent = Math.round(c.harmony() * 100) + " %";
        String sign = feedback > 0 && !full ? (lastGood ? " +" : " -") : "";
        int signColor = lastGood ? 0xFF8FE4FF : MISS;
        int right = r.right() - 6;
        g.drawString(font, sign, right - font.width(sign), r.y() + 5, signColor, false);
        g.drawString(font, percent, right - font.width(sign) - font.width(percent), r.y() + 5, SystemTheme.TEXT, false);
        SystemTheme.bar(g, x, r.y() + 16, r.width() - 12, 6, c.harmony(), SystemTheme.CYAN);
        if (full) {
            g.drawString(font, Component.translatable("hud.murimblock.meditation.gauge_full"), x, r.y() + 29,
                    SystemTheme.TEXT_MUTED, false);
        } else {
            double gain = CultivationRules.meditationGain(c.harmony());
            String rate = String.format(Locale.ROOT, "+%.1f Qi/s", gain);
            SystemTheme.glowText(g, font, Component.literal(rate), x, r.y() + 29, SystemTheme.TEXT);
            g.drawString(font, String.format(Locale.ROOT, "x%.2f", gain / CultivationRules.MEDITATION_GAIN),
                    x + font.width(rate) + 5, r.y() + 29, SystemTheme.TEXT_MUTED, false);
        }
    }

    private static void drawGauge(GuiGraphics g, Font font, Cultivation.Snapshot c, int cx, int h) {
        int width = 140;
        int x = cx - width / 2;
        // Above the hotbar, hearts, armour and the action bar messages.
        int y = h - 92;
        int gauge = CultivationRules.gauge(c.realm(), c.stage());
        boolean full = c.stageQi() >= gauge;
        Component stage = Component.translatable("hud.murimblock.meditation.stage",
                Component.translatable("murimblock.cultivation_realm." + CultivationRealms.realmKey(c.realm())), c.stage());
        g.drawString(font, stage, x, y, SystemTheme.TEXT, true);
        String amount = (int) Math.min(c.stageQi(), gauge) + " / " + gauge;
        if (full) {
            SystemTheme.glowText(g, font, Component.literal(amount), x + width - font.width(amount), y, SystemTheme.TEXT);
        } else {
            g.drawString(font, amount, x + width - font.width(amount), y, SystemTheme.TEXT, true);
        }
        g.fill(x, y + 10, x + width, y + 17, SystemTheme.withAlpha(0xFF07121F, 0xB3));
        SystemTheme.bar(g, x, y + 10, width, 7, (float) (c.stageQi() / gauge), full ? FLASH : SystemTheme.CYAN);
    }

    private static void prompt(GuiGraphics g, Font font, Rect r, Component text) {
        hudBox(g, r);
        int y = r.y() + (r.height() - 8) / 2;
        SystemTheme.glowText(g, font, Component.literal("[!]"), r.x() + 6, y, SystemTheme.TEXT);
        g.drawString(font, text, r.x() + 6 + font.width("[!] "), y, SystemTheme.TEXT, false);
    }

    private static void hudBox(GuiGraphics g, Rect r) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xD107121F);
        g.renderOutline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, SystemTheme.withAlpha(SystemTheme.CYAN, 0x30));
        g.renderOutline(r.x(), r.y(), r.width(), r.height(), SystemTheme.CYAN);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerLayer(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, BreathingHud::render);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isPaused()) {
                return;
            }
            if (ClientCultivation.get().meditating()) {
                meditationTicks++;
            } else {
                meditationTicks = 0;
                feedback = 0;
            }
            cooldown = Math.max(0, cooldown - 1);
            feedback = Math.max(0, feedback - 1);
        }
    }
}
