package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import com.murimblock.client.cultivation.vision.ClosedEyes;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.innerworld.TrialEntry;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationRealms;
import com.murimblock.cultivation.CultivationRules;
import java.util.Locale;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The HUD while meditating ({@code docs/cultivation/meditation-vision.md}): Qi motes drifting in the dark of closed
 * eyes, a ring under the crosshair filling with the Calm (a red dot in it while a hostile is near), a banner and a
 * chime at each Calm tier, the stage gauge above the hotbar, and what the keys do.
 */
public final class MeditationHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "meditation");
    /** No tier, Steady Breath, Flowing Qi, Still Heart. */
    private static final int[] TIER_COLORS = {0xFF7AA3C4, 0xFF5FD0FF, 0xFF5FF2C0, 0xFFFFD36A};
    private static final int DANGER = 0xFFFF5050;
    private static final int RING_RADIUS = 9;
    private static final int RING_DOTS = 40;
    private static final int MOTES = 90;
    private static final float[][] MOTE_SEEDS = new float[MOTES][];

    static {
        Random random = new Random(7);
        for (int i = 0; i < MOTES; i++) {
            MOTE_SEEDS[i] = new float[] {random.nextFloat(), 0.2f + random.nextFloat() * 0.8f,
                    random.nextFloat() * Mth.TWO_PI, random.nextFloat()};
        }
    }

    private static int lastTier;
    private static long tierAt = -10_000;
    private static boolean hostileNear;

    private MeditationHud() {
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Cultivation.Snapshot c = ClientCultivation.get();
        // A breakthrough or the trial's dive takes the screen.
        if (!c.meditating() || mc.options.hideGui || mc.player == null || StageBreakthrough.playingOwn()
                || TrialEntry.ownDive() >= 0) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        float time = (Util.getMillis() % 3_600_000L) / 1000f;
        float eyes = ClosedEyes.amount(delta.getGameTimeDeltaPartialTick(false));
        int tier = CultivationRules.calmTier(c.calm());
        int tierColor = TIER_COLORS[tier];
        if (eyes > 0) {
            motes(g, w, h, time, eyes, QiAura.power(c.qi()));
        }
        ring(g, w / 2, h / 2 + 26, c.calm(), tierColor, time);
        long since = Util.getMillis() - tierAt;
        if (tier > 0 && since < 2400) {
            String text = Component.translatable("hud.murimblock.calm.tier." + tier).getString() + "   "
                    + Component.translatable("hud.murimblock.calm.gain",
                    String.format(Locale.ROOT, "%.1f", CultivationRules.meditationGain(c.calm())
                            / CultivationRules.MEDITATION_GAIN)).getString();
            float alpha = Math.min(1, since / 250f) * Math.min(1, (2400 - since) / 500f);
            int x = w / 2 - SystemTheme.spacedWidth(font, text, 1) / 2;
            // Just above the ring, clear of the gauge.
            SystemTheme.glowSpaced(g, font, text, x, h / 2 + 4,
                    SystemTheme.withAlpha(tierColor, Math.max(8, Math.round(255 * alpha))), 1);
        }
        gauge(g, font, c, w / 2, h, tierColor);
        boolean full = CultivationRules.gaugeFull(c.realm(), c.stage(), c.stageQi());
        Component text = full ? Component.translatable("hud.murimblock.meditation.full")
                : Component.translatable(ClosedEyes.closed() ? "hud.murimblock.meditation.prompt_open"
                : "hud.murimblock.meditation.prompt_close", MurimKeys.MEDITATE.getTranslatedKeyMessage());
        prompt(g, font, new Rect(6, 6, font.width("[!] ") + font.width(text) + 12, 16), text);
    }

    /** Specks of Qi turning slowly in the dark, more of them for a greater reserve. */
    private static void motes(GuiGraphics g, int w, int h, float time, float eyes, float power) {
        int count = Math.round(MOTES * (0.35f + 0.65f * power));
        for (int i = 0; i < count; i++) {
            float[] m = MOTE_SEEDS[i];
            float speed = m[1];
            float angle = time * 0.05f * speed + m[2];
            float radius = 0.15f + ((m[0] + time * 0.012f * speed) % 1) * 0.55f;
            int x = Math.round(w / 2f + Mth.cos(angle) * radius * w * 0.6f);
            int y = Math.round(h * 0.62f + Mth.sin(angle) * radius * h * 0.55f - (1 - radius) * h * 0.08f);
            float twinkle = 0.5f + 0.5f * Mth.sin(time * 1.7f * speed + m[2]);
            int size = m[3] > 0.7f ? 2 : 1;
            g.fill(x, y, x + size, y + size, SystemTheme.withAlpha(0xFF78DCFF,
                    Math.round(255 * eyes * (0.15f + 0.45f * twinkle))));
        }
    }

    /** Dots around a circle, lit clockwise from the top as the Calm fills. */
    private static void ring(GuiGraphics g, int cx, int cy, double calm, int color, float time) {
        for (int i = 0; i < RING_DOTS; i++) {
            float angle = -Mth.HALF_PI + i * Mth.TWO_PI / RING_DOTS;
            int x = Math.round(cx + Mth.cos(angle) * RING_RADIUS);
            int y = Math.round(cy + Mth.sin(angle) * RING_RADIUS);
            boolean lit = i < calm * RING_DOTS;
            g.fill(x, y, x + 1, y + 1, lit ? color : SystemTheme.withAlpha(0xFFFFFFFF, 0x30));
        }
        if (hostileNear) {
            int a = Math.round(255 * (0.6f + 0.4f * Mth.sin(time * 8)));
            g.fill(cx - 1, cy - 1, cx + 1, cy + 1, SystemTheme.withAlpha(DANGER, a));
        }
    }

    private static void gauge(GuiGraphics g, Font font, Cultivation.Snapshot c, int cx, int h, int tierColor) {
        int gauge = CultivationRules.gauge(c.realm(), c.stage());
        boolean full = c.stageQi() >= gauge;
        Component stage = Component.translatable("hud.murimblock.meditation.stage",
                Component.translatable("murimblock.cultivation_realm." + CultivationRealms.realmKey(c.realm())), c.stage());
        // Max Qi toward the stage's peak, where it stops growing until the player advances.
        String amount = c.maxQi() + " / " + CultivationRules.peak(c.realm(), c.stage());
        // As wide as the label and the amount need: long realm names and big gauges must not overlap.
        int width = Math.max(140, font.width(stage) + font.width(amount) + 12);
        int x = cx - width / 2;
        // Above the hotbar, hearts, armour and the action bar messages.
        int y = h - 92;
        g.drawString(font, stage, x, y, SystemTheme.TEXT, true);
        if (full) {
            SystemTheme.glowText(g, font, Component.literal(amount), x + width - font.width(amount), y, SystemTheme.TEXT);
        } else {
            g.drawString(font, amount, x + width - font.width(amount), y, SystemTheme.TEXT, true);
        }
        g.fill(x, y + 10, x + width, y + 17, SystemTheme.withAlpha(0xFF07121F, 0xB3));
        SystemTheme.bar(g, x, y + 10, width, 7, (float) (c.stageQi() / gauge), full ? 0xFFD6F4FF : tierColor);
    }

    private static void prompt(GuiGraphics g, Font font, Rect r, Component text) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xD107121F);
        g.renderOutline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, SystemTheme.withAlpha(SystemTheme.CYAN, 0x30));
        g.renderOutline(r.x(), r.y(), r.width(), r.height(), SystemTheme.CYAN);
        int y = r.y() + (r.height() - 8) / 2;
        SystemTheme.glowText(g, font, Component.literal("[!]"), r.x() + 6, y, SystemTheme.TEXT);
        g.drawString(font, text, r.x() + 6 + font.width("[!] "), y, SystemTheme.TEXT, false);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerLayer(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, MeditationHud::render);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            Cultivation.Snapshot c = ClientCultivation.get();
            if (!c.meditating() || mc.player == null || mc.level == null) {
                lastTier = 0;
                hostileNear = false;
                return;
            }
            int tier = CultivationRules.calmTier(c.calm());
            if (tier > lastTier) {
                tierAt = Util.getMillis();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(),
                        0.8f + 0.2f * tier, 0.8f));
            }
            lastTier = tier;
            if (mc.player.tickCount % 10 == 0) {
                hostileNear = !mc.level.getEntitiesOfClass(Mob.class,
                        mc.player.getBoundingBox().inflate(CultivationRules.CALM_ENEMY_RANGE),
                        m -> m instanceof Enemy && m.isAlive()).isEmpty();
            }
        }
    }
}
