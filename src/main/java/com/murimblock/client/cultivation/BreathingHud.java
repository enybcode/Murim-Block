package com.murimblock.client.cultivation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import com.murimblock.client.cultivation.BreathTrack.Beat;
import com.murimblock.client.cultivation.BreathTrack.Judgement;
import com.murimblock.client.cultivation.BreathTrack.Kind;
import com.murimblock.client.cultivation.BreathTrack.Word;
import com.murimblock.client.gui.SystemLayout.Rect;
import com.murimblock.client.gui.SystemTheme;
import com.murimblock.client.innerworld.TrialEntry;
import com.murimblock.cultivation.BreathResult;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationPayloads;
import com.murimblock.cultivation.CultivationRealms;
import com.murimblock.cultivation.CultivationRules;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The breathing QTE drawn while meditating (validated mockup "Breathing V2", 24 Oct): marks of a {@link BreathTrack}
 * scroll left into a frame; the meditation key is pressed as an inhale or exhale mark enters it and held through hold
 * bars. Combo and Harmony boxes sit under the lane, the stage gauge above the hotbar. Hits flash the frame and send
 * waves and sparks out of it, misses shake the lane and redden the screen edges; Harmony tints the edges in the combo
 * tier's colour and draws Qi motes to the frame, and the world brightens a little with each inhale.
 */
public final class BreathingHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "breathing");
    private static final ResourceLocation VIGNETTE =
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/breath_vignette.png");
    private static final int LANE_WIDTH = 200;
    private static final int LANE_HEIGHT = 24;
    /** The frame sits this far into the lane; marks scroll this many GUI pixels a second. */
    private static final int HIT_OFFSET = 36;
    private static final float PIXELS_PER_SECOND = 40;
    private static final int HOLD_GAP = 7;
    private static final int MISS = 0xFFFFB067;
    private static final int GOLD = 0xFFFFD27A;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PALE = 0xFFFFF4D6;
    private static final int LABEL = 0xFF8FDCFF;
    private static final int[] TIER_COLORS = {SystemTheme.CYAN, 0xFF7FF0E6, GOLD, PALE};
    /** Semitones of the notes good breaths climb, a pentatonic scale. */
    private static final int[] SCALE = {0, 2, 4, 7, 9, 12};

    private record Wave(float at, int color) {
    }

    private record Spark(float at, float angle, float speed, int color) {
    }

    private static final class Mote {
        private float x;
        private float y;
        private float speed;
    }

    private static final Random RANDOM = new Random();
    private static final BreathTrack TRACK = new BreathTrack(RANDOM);
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final List<Mote> MOTES = new ArrayList<>();

    private static int meditationTicks;
    private static boolean started;
    private static boolean keyDown;
    private static float downAt;
    /** Breaths in a row without a miss, kept by the client for the HUD; the server keeps its own for the gain. */
    private static int combo;
    // Effects, timed in seconds of meditation.
    private static Word lastWord;
    private static BreathResult lastResult = BreathResult.MISS;
    private static float judgedAt = -9;
    private static float shakeAt = -9;
    private static float brokenAt = -9;
    private static float tierAt = -9;
    private static long lastFrame;

    private BreathingHud() {
    }

    /** Dev screenshots: light a perfect breath or a miss without a press. */
    public static void showFeedback(boolean good) {
        effects(good ? BreathResult.PERFECT : BreathResult.MISS, good ? Word.PERFECT : Word.MISSED,
                seconds(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
    }

    private static float seconds(float partialTick) {
        return (meditationTicks + partialTick) / 20f;
    }

    private static float now() {
        return seconds(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    private static boolean full(Cultivation.Snapshot c) {
        return CultivationRules.gaugeFull(c.realm(), c.stage(), c.stageQi());
    }

    private static boolean playing() {
        Minecraft mc = Minecraft.getInstance();
        Cultivation.Snapshot c = ClientCultivation.get();
        return c.meditating() && started && mc.screen == null && mc.player != null && !full(c)
                && !StageBreakthrough.playingOwn() || TrialEntry.ownDive() >= 0;
    }

    private static void press() {
        if (keyDown || !playing()) {
            return;
        }
        keyDown = true;
        downAt = now();
        Judgement j = TRACK.press(downAt);
        if (j != null) {
            judged(j, downAt);
        }
    }

    private static void release() {
        if (!keyDown) {
            return;
        }
        keyDown = false;
        if (playing()) {
            float t = now();
            Judgement j = TRACK.release(t, downAt);
            if (j != null) {
                judged(j, t);
            }
        }
    }

    private static void judged(Judgement j, float t) {
        PacketDistributor.sendToServer(new CultivationPayloads.Breath(j.result()));
        effects(j.result(), j.word(), t);
    }

    private static void effects(BreathResult result, Word word, float t) {
        lastWord = word;
        lastResult = result;
        judgedAt = t;
        int tierBefore = CultivationRules.comboTier(combo);
        if (result == BreathResult.MISS) {
            if (combo >= 5) {
                brokenAt = t;
            }
            combo = 0;
            shakeAt = t;
            sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 0.9f);
            return;
        }
        combo++;
        int tier = CultivationRules.comboTier(combo);
        WAVES.add(new Wave(t, result == BreathResult.PERFECT ? PALE : TIER_COLORS[tier]));
        int semitone = SCALE[(combo - 1) % SCALE.length] + (combo > SCALE.length ? 7 : 0);
        sound(SoundEvents.NOTE_BLOCK_BELL.value(), (float) Math.pow(2, (semitone - 6) / 12.0), 0.7f);
        if (result == BreathResult.PERFECT) {
            WAVES.add(new Wave(t + 0.08f, GOLD));
            for (int i = 0; i < 16; i++) {
                SPARKS.add(new Spark(t, RANDOM.nextFloat() * Mth.TWO_PI, 40 + RANDOM.nextFloat() * 50,
                        RANDOM.nextBoolean() ? GOLD : WHITE));
            }
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.8f);
        }
        if (tier > tierBefore) {
            tierAt = t;
            sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1, 0.8f);
        }
        if (Minecraft.getInstance().player != null) {
            QiAura.burst(Minecraft.getInstance().player);
        }
    }

    private static void sound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    /** 0 to 1 of the way from age 0 to life; negative or 1+ outside it. */
    private static float age(float t, float at, float life) {
        return (t - at) / life;
    }

    private static boolean alive(float age) {
        return age >= 0 && age < 1;
    }

    private static String multiplier(double m) {
        return m == Math.rint(m) ? String.valueOf((int) m) : String.valueOf(m);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Cultivation.Snapshot c = ClientCultivation.get();
        // A breakthrough or the trial's dive takes the screen: the lane steps aside until it ends.
        if (!c.meditating() || mc.options.hideGui || mc.player == null || StageBreakthrough.playingOwn() || TrialEntry.ownDive() >= 0) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int laneX = cx - LANE_WIDTH / 2;
        int laneY = Math.round(h * 0.35f) - 22;
        int hitX = laneX + HIT_OFFSET;
        boolean full = full(c);
        float t = seconds(delta.getGameTimeDeltaPartialTick(false));
        int tierColor = TIER_COLORS[CultivationRules.comboTier(combo)];

        if (!full) {
            float lung = (TRACK.level(t) - BreathTrack.EMPTY) / (BreathTrack.FULL - BreathTrack.EMPTY);
            atmosphere(g, w, h, t, lung, tierColor, c.harmony(), hitX, laneY + LANE_HEIGHT / 2);
        }
        drawLane(g, font, laneX, laneY, hitX, t, tierColor, full);
        drawCombo(g, font, new Rect(laneX, laneY + LANE_HEIGHT + 8, 84, 44), t, tierColor);
        drawHarmony(g, font, new Rect(laneX + LANE_WIDTH - 104, laneY + LANE_HEIGHT + 8, 104, 44), c, full, tierColor);
        drawGauge(g, font, c, cx, h, tierColor);
        if (full) {
            Component text = Component.translatable("hud.murimblock.meditation.full");
            int bw = font.width("[!] ") + font.width(text) + 20;
            prompt(g, font, new Rect(cx - bw / 2, 8, bw, 18), text);
            return;
        }
        Component text = Component.translatable("hud.murimblock.meditation.prompt",
                MurimKeys.MEDITATE.getTranslatedKeyMessage());
        prompt(g, font, new Rect(6, 6, font.width("[!] ") + font.width(text) + 12, 16), text);
        float broken = age(t, brokenAt, 1.2f);
        float tier = age(t, tierAt, 1.8f);
        if (alive(broken)) {
            banner(g, font, Component.translatable("hud.murimblock.breath.combo_broken").getString(), cx, MISS,
                    1 - broken);
        } else if (alive(tier)) {
            int index = CultivationRules.comboTier(combo);
            if (index > 0) {
                String name = Component.translatable("hud.murimblock.breath.tier." + index).getString() + "  x"
                        + multiplier(CultivationRules.comboMultiplier(combo));
                banner(g, font, name, cx, tierColor, tier < 0.75f ? 1 : (1 - tier) / 0.25f);
            }
        }
    }

    /** The screen effects around the HUD: breathing light, Harmony and miss vignettes, Qi motes. */
    private static void atmosphere(GuiGraphics g, int w, int h, float t, float lung, int tierColor, float harmony,
                                   int targetX, int targetY) {
        g.fill(0, 0, w, h, alpha(0.05f * lung, 0xFFF0DC));
        g.fill(0, 0, w, h, alpha(0.12f * (1 - lung), 0x000814));
        vignette(g, tierColor, harmony * (0.35f + 0.25f * lung));
        float miss = age(t, shakeAt, 0.45f);
        if (alive(miss)) {
            vignette(g, MISS, 0.7f * (1 - miss));
        }
        long millis = Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (millis - lastFrame) / 1000f);
        lastFrame = millis;
        if (harmony > 0.5f && RANDOM.nextFloat() < (harmony - 0.5f) * 2 * dt * 30) {
            Mote m = new Mote();
            int side = RANDOM.nextInt(4);
            m.x = side == 0 ? 0 : side == 1 ? w : RANDOM.nextFloat() * w;
            m.y = side == 2 ? 0 : side == 3 ? h : RANDOM.nextFloat() * h;
            m.speed = 20 + RANDOM.nextFloat() * 20;
            MOTES.add(m);
        }
        MOTES.removeIf(m -> {
            float dx = targetX - m.x;
            float dy = targetY - m.y;
            float d = Math.max(1, Mth.sqrt(dx * dx + dy * dy));
            m.speed += 60 * dt;
            m.x += dx / d * m.speed * dt;
            m.y += dy / d * m.speed * dt;
            return d <= 8;
        });
        int color = SystemTheme.withAlpha(tierColor, 0xCC);
        for (Mote m : MOTES) {
            g.fill(Math.round(m.x), Math.round(m.y), Math.round(m.x) + 1, Math.round(m.y) + 1, color);
        }
    }

    private static int alpha(float alpha, int rgb) {
        return Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24 | rgb;
    }

    /** The vignette texture stretched over the screen, tinted. */
    private static void vignette(GuiGraphics g, int color, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, Math.min(1, alpha));
        g.blit(VIGNETTE, 0, 0, g.guiWidth(), g.guiHeight(), 0, 0, 128, 128, 128, 128);
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private static void drawLane(GuiGraphics g, Font font, int x, int laneY, int hitX, float t, int tierColor,
                                 boolean full) {
        float shake = age(t, shakeAt, 0.28f);
        int y = laneY + (alive(shake) ? Math.round(Mth.sin(shake * 40) * 2 * (1 - shake)) : 0);
        int mid = y + LANE_HEIGHT / 2;
        hudBox(g, new Rect(x, y, LANE_WIDTH, LANE_HEIGHT), combo >= 5 ? tierColor : SystemTheme.CYAN);
        float flash = age(t, judgedAt, 0.3f);
        if (alive(flash) && lastResult != BreathResult.MISS) {
            g.fill(hitX - 6, y + 1, hitX + 7, y + LANE_HEIGHT - 1, SystemTheme.withAlpha(tierColor,
                    Math.round(0x73 * (1 - flash))));
        }
        if (keyDown) {
            g.fill(hitX - 6, y + 1, hitX + 7, y + LANE_HEIGHT - 1, 0x2EFFFFFF);
        }
        g.enableScissor(x, y, x + LANE_WIDTH, y + LANE_HEIGHT);
        for (Beat b : TRACK.beats()) {
            int xs = hitX + Math.round((b.start() - t) * PIXELS_PER_SECOND);
            int xe = hitX + Math.round((b.end() - t) * PIXELS_PER_SECOND);
            if (xe < x - 12 || xs > x + LANE_WIDTH + 12) {
                continue;
            }
            BreathResult r = b.result();
            int color = r == null ? SystemTheme.TEXT : SystemTheme.withAlpha(
                    r == BreathResult.PERFECT ? GOLD : r == BreathResult.GOOD ? WHITE : MISS, 0x80);
            if (b.kind() == Kind.HOLD) {
                // The bar starts a little after the mark before it, so that mark stays readable.
                int from = xs + HOLD_GAP;
                g.fill(from, mid - 2, Math.max(from + 1, xe), mid + 3, color);
                g.fill(xe - 1, mid - 7, xe + 2, mid + 8, color);
                if (r == null && b.start() <= t && hitX > from) {
                    g.fill(from, mid - 2, hitX, mid + 3, keyDown ? tierColor : SystemTheme.TEXT_MUTED);
                }
            } else {
                // An inhale points up, an exhale down.
                for (int i = 0; i < 5; i++) {
                    int row = b.kind() == Kind.INHALE ? mid - 3 + i : mid + 3 - i;
                    g.fill(xe - i, row, xe + i + 1, row + 2, color);
                }
            }
        }
        g.disableScissor();

        int frame = keyDown ? WHITE : tierColor;
        int top = y - 3;
        int bottom = y + LANE_HEIGHT + 3;
        g.fill(hitX - 7, top, hitX - 6, bottom, frame);
        g.fill(hitX + 7, top, hitX + 8, bottom, frame);
        g.fill(hitX - 7, top, hitX - 4, top + 1, frame);
        g.fill(hitX + 5, top, hitX + 8, top + 1, frame);
        g.fill(hitX - 7, bottom - 1, hitX - 4, bottom, frame);
        g.fill(hitX + 5, bottom - 1, hitX + 8, bottom, frame);
        for (Wave wave : WAVES) {
            float e = age(t, wave.at, 0.45f);
            if (alive(e)) {
                int k = Math.round(16 * (1 - (1 - e) * (1 - e) * (1 - e)));
                g.renderOutline(hitX - 7 - k, top - k, 15 + 2 * k, LANE_HEIGHT + 6 + 2 * k,
                        SystemTheme.withAlpha(wave.color, Math.round(255 * (1 - e))));
            }
        }
        WAVES.removeIf(wave -> t - wave.at > 0.45f || t < wave.at - 1);
        for (Spark s : SPARKS) {
            float a = t - s.at;
            if (a >= 0 && a < 0.55f) {
                float r = 6 + s.speed * a;
                int sx = Math.round(hitX + Mth.cos(s.angle) * r);
                int sy = Math.round(mid + Mth.sin(s.angle) * r);
                int size = a < 0.2f ? 2 : 1;
                g.fill(sx, sy, sx + size, sy + size, SystemTheme.withAlpha(s.color, Math.round(255 * (1 - a / 0.55f))));
            }
        }
        SPARKS.removeIf(s -> t - s.at > 0.55f || t < s.at - 1);
        if (!full) {
            drawWord(g, font, x + LANE_WIDTH / 2, y - 14, t);
        }
    }

    /** Above the lane: the last judgement pops for a moment, otherwise the breath to take now. */
    private static void drawWord(GuiGraphics g, Font font, int cx, int y, float t) {
        float age = age(t, judgedAt, 0.6f);
        String text;
        int color;
        float scale = 1;
        boolean glow = false;
        if (lastWord != null && alive(age)) {
            text = Component.translatable(lastWord.key()).getString();
            int base = lastResult == BreathResult.PERFECT ? GOLD : lastResult == BreathResult.GOOD ? WHITE : MISS;
            color = SystemTheme.withAlpha(base, Math.max(8, Math.round(255 * (age < 0.7f ? 1 : (1 - age) / 0.3f))));
            scale = 1 + (lastResult == BreathResult.PERFECT ? 0.5f : 0.25f) * Math.max(0, 1 - age * 4);
            glow = true;
        } else {
            Beat current = TRACK.current(t);
            if (current == null) {
                return;
            }
            text = Component.translatable("hud.murimblock.breath." + current.kind().name().toLowerCase(Locale.ROOT))
                    .getString();
            color = 0xFFCFEEFF;
        }
        int width = SystemTheme.spacedWidth(font, text, 1);
        g.pose().pushPose();
        g.pose().translate(cx, y + 4, 0);
        g.pose().scale(scale, scale, 1);
        if (glow) {
            SystemTheme.glowSpaced(g, font, text, -width / 2, -4, color, 1);
        } else {
            SystemTheme.spaced(g, font, text, -width / 2, -4, color, 1);
        }
        g.pose().popPose();
    }

    private static void banner(GuiGraphics g, Font font, String text, int cx, int color, float alpha) {
        int x = cx - SystemTheme.spacedWidth(font, text, 1) / 2;
        SystemTheme.glowSpaced(g, font, text, x, 30, SystemTheme.withAlpha(color, Math.max(8, Math.round(255 * alpha))),
                1);
    }

    private static void drawCombo(GuiGraphics g, Font font, Rect r, float t, int tierColor) {
        boolean tiered = combo >= 5;
        hudBox(g, r, tiered ? tierColor : SystemTheme.CYAN);
        SystemTheme.spaced(g, font, Component.translatable("hud.murimblock.breath.combo").getString(), r.x() + 6,
                r.y() + 5, LABEL, 1);
        float pop = lastResult != BreathResult.MISS ? Math.max(0, 1 - (t - judgedAt) / 0.15f) : 0;
        float scale = 2 + 0.5f * Math.min(1, pop);
        g.pose().pushPose();
        g.pose().translate(r.x() + 6, r.y() + 15, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, "x" + combo, 0, 0, tiered ? tierColor : SystemTheme.TEXT, true);
        g.pose().popPose();
        int next = CultivationRules.nextComboTier(combo);
        Component label = tiered
                ? Component.translatable("hud.murimblock.breath.qi_multiplier",
                multiplier(CultivationRules.comboMultiplier(combo)))
                : Component.translatable("hud.murimblock.breath.next_tier", next - combo,
                multiplier(CultivationRules.comboMultiplier(next)));
        g.drawString(font, label, r.x() + 6, r.y() + 33, tiered ? tierColor : SystemTheme.TEXT_MUTED, false);
    }

    private static void drawHarmony(GuiGraphics g, Font font, Rect r, Cultivation.Snapshot c, boolean full,
                                    int tierColor) {
        hudBox(g, r, SystemTheme.CYAN);
        int x = r.x() + 6;
        SystemTheme.spaced(g, font, Component.translatable("hud.murimblock.breath.harmony").getString(), x,
                r.y() + 5, LABEL, 1);
        String percent = Math.round(c.harmony() * 100) + " %";
        g.drawString(font, percent, r.right() - 6 - font.width(percent), r.y() + 5, SystemTheme.TEXT, false);
        SystemTheme.bar(g, x, r.y() + 16, r.width() - 12, 6, c.harmony(), tierColor);
        if (full) {
            g.drawString(font, Component.translatable("hud.murimblock.meditation.gauge_full"), x, r.y() + 29,
                    SystemTheme.TEXT_MUTED, false);
        } else {
            double gain = CultivationRules.meditationGain(c.harmony(), combo);
            String rate = String.format(Locale.ROOT, "+%.1f Qi/s", gain);
            SystemTheme.glowText(g, font, Component.literal(rate), x, r.y() + 29, SystemTheme.TEXT);
            g.drawString(font, String.format(Locale.ROOT, "x%.2f", gain / CultivationRules.MEDITATION_GAIN),
                    x + font.width(rate) + 5, r.y() + 29, SystemTheme.TEXT_MUTED, false);
        }
    }

    private static void drawGauge(GuiGraphics g, Font font, Cultivation.Snapshot c, int cx, int h, int tierColor) {
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
        SystemTheme.bar(g, x, y + 10, width, 7, (float) (c.stageQi() / gauge), full ? 0xFFD6F4FF : tierColor);
    }

    private static void prompt(GuiGraphics g, Font font, Rect r, Component text) {
        hudBox(g, r, SystemTheme.CYAN);
        int y = r.y() + (r.height() - 8) / 2;
        SystemTheme.glowText(g, font, Component.literal("[!]"), r.x() + 6, y, SystemTheme.TEXT);
        g.drawString(font, text, r.x() + 6 + font.width("[!] "), y, SystemTheme.TEXT, false);
    }

    private static void hudBox(GuiGraphics g, Rect r, int color) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xD107121F);
        g.renderOutline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, SystemTheme.withAlpha(color, 0x30));
        g.renderOutline(r.x(), r.y(), r.width(), r.height(), color);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void registerLayer(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, BreathingHud::render);
        }

        // Presses are read from the input events, not the key's click queue, so they are timed to the frame.
        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_REPEAT && MurimKeys.MEDITATE.matches(event.getKey(), event.getScanCode())) {
                input(event.getAction() == GLFW.GLFW_PRESS);
            }
        }

        @SubscribeEvent
        public static void onMouse(InputEvent.MouseButton.Post event) {
            if (MurimKeys.MEDITATE.matchesMouse(event.getButton())) {
                input(event.getAction() == GLFW.GLFW_PRESS);
            }
        }

        private static void input(boolean down) {
            if (down) {
                press();
            } else {
                release();
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isPaused()) {
                return;
            }
            Cultivation.Snapshot c = ClientCultivation.get();
            if (!c.meditating()) {
                meditationTicks = 0;
                started = false;
                keyDown = false;
                combo = 0;
                lastWord = null;
                MOTES.clear();
                return;
            }
            meditationTicks++;
            float t = seconds(0);
            if (!started || full(c) || StageBreakthrough.playingOwn() || TrialEntry.ownDive() >= 0) {
                TRACK.reset(t);
                WAVES.clear();
                SPARKS.clear();
                started = true;
                return;
            }
            // A screen opened mid-hold releases every key without an input event reaching us.
            if (keyDown && !MurimKeys.MEDITATE.isDown()) {
                release();
            }
            TRACK.fill(t, c.harmony());
            for (Judgement j : TRACK.expire(t, keyDown, downAt)) {
                judged(j, t);
            }
        }
    }
}
