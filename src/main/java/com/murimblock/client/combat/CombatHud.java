package com.murimblock.client.combat;

import static com.murimblock.client.combat.HudPixels.*;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.client.gui.data.MartialArtsData;
import com.murimblock.combat.CombatState;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * What combat mode shows: the combat bar, look B validated by Enzo on 30 Oct 2026 ({@code
 * docs/gui/boards/combat-bar-b.png}), drawn as pixel art one GUI pixel at a time. In the bottom right: the active art's
 * name on a banner, the Qi as a jade tube notched every segment, up to six techniques in diamond seals interlocked on
 * two rows (keys 1 to 6, left to right, each key on its seal's outer tip) and the ultimate in a gold octagon on R. The
 * block slides in from the right when combat mode starts. A short white flash covers the screen when a heavy blow lands
 * on the player or breaks its guard.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatHud {
    /** The block's size and its distance from the screen's edges, in GUI pixels. */
    private static final int WIDTH = 96, HEIGHT = 52, MARGIN = 6;
    /** The Qi tube's length, the longest art name the banner holds. */
    private static final int BAR = 66, NAME = 19;
    /** Technique seals: how many, their half-width, the distance between two on a row (one pixel apart diagonally). */
    private static final int SEALS = 6, HALF = 9, STEP = 20;
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

    /** Qi held by one segment for a pool of {@code maxQi}: 10, or a round ten keeping the notches to 20. */
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
        int offset = Math.round((1 - slide) * (1 - slide) * (WIDTH + MARGIN));
        int x = width - MARGIN - WIDTH + offset, y = height - MARGIN - HEIGHT;
        // On a narrow screen the corner would cover the hotbar: the block then sits above it.
        if (width - MARGIN - WIDTH < width / 2 + 91 + 4) {
            y -= 40;
        }
        var qi = ClientCultivation.get();
        MartialArtsData.Art art = MartialArtsData.active();
        float now = (float) qi.qi();
        String name = art.name().getString();
        banner(graphics, x + 1, y + 1, name.length() > NAME ? name.substring(0, NAME) : name);
        String amount = String.valueOf((int) now);
        text(graphics, x + WIDTH - 1 - width(amount), y + 11, amount, WHITE, OUTLINE);
        long time = mc.level == null ? 0 : mc.level.getGameTime();
        qiBar(graphics, x + 1, y + 11, now, qi.maxQi(), (int) (time % 60));
        List<MartialArtsData.Technique> techniques = art.techniques().stream()
                .filter(t -> t.kind() == MartialArtsData.Kind.TECHNIQUE).limit(SEALS).toList();
        // Interlocked: 1, 3, 5 on the top row, 2, 4, 6 on the bottom row half a seal to the right.
        int lx = x, ly = y + 19;
        for (int i = 0; i < SEALS; i++) {
            boolean top = i % 2 == 0;
            int cx = lx + HALF + (i / 2) * STEP + (top ? 0 : STEP / 2), cy = ly + HALF + (top ? 0 : STEP / 2);
            seal(graphics, cx, cy, i < techniques.size() ? techniques.get(i) : null, art, now, top);
        }
        MartialArtsData.Technique ultimate = art.techniques().stream()
                .filter(t -> t.kind() == MartialArtsData.Kind.ULTIMATE).findFirst().orElse(null);
        // No ultimate charge exists yet: the octagon stays empty.
        ultimate(graphics, x + 84, ly + 14, ultimate, 0, (int) (time / 4 % 3));
    }

    /** The art's name on a banner with pointed ends. */
    private static void banner(GuiGraphics g, int x, int y, String name) {
        int w = width(name) + 6;
        rect(g, x + 2, y, w, 9, OUTLINE);
        rect(g, x + 1, y + 1, w + 2, 7, OUTLINE);
        rect(g, x, y + 3, w + 4, 3, OUTLINE);
        rect(g, x + 2, y + 1, w, 7, NIGHT);
        rect(g, x + 1, y + 3, w + 2, 3, NIGHT);
        rect(g, x + 2, y + 1, w, 1, MID);
        text(g, x + 4, y + 2, name, WHITE, DEEP);
    }

    /** The Qi tube: three rows of colour, a dark notch every segment, a diamond at each end, a shine gliding along. */
    private static void qiBar(GuiGraphics g, int x, int y, float qi, int maxQi, int shine) {
        int inner = BAR - 4;
        int filled = maxQi <= 0 ? 0 : Math.round(inner * Mth.clamp(qi / maxQi, 0, 1));
        for (int ex : new int[] {x, x + BAR - 1}) {
            diamond(g, ex, y + 2, 3, OUTLINE);
            diamond(g, ex, y + 2, 2, MID);
            px(g, ex, y + 2, LIGHT);
        }
        rect(g, x + 2, y, inner, 5, OUTLINE);
        rect(g, x + 2, y + 1, inner, 3, DEEP);
        rect(g, x + 2, y + 1, filled, 1, LIGHT);
        rect(g, x + 2, y + 2, filled, 1, CYAN);
        rect(g, x + 2, y + 3, filled, 1, MID);
        int per = segmentQi(maxQi);
        for (int v = per; v < maxQi; v += per) {
            rect(g, x + 2 + Math.round(inner * (float) v / maxQi), y + 1, 1, 3, OUTLINE);
        }
        if (shine < filled - 1) {
            px(g, x + 2 + shine, y + 1, WHITE);
            px(g, x + 3 + shine, y + 1, WHITE);
            px(g, x + 3 + shine, y + 2, LIGHT);
        }
    }

    /**
     * A technique's diamond seal: double frame, its icon (or initials) inside, its key in a tag on the outer tip (top
     * for the top row, bottom for the bottom row). Pink when its Qi is lacking, grey with a padlock until its mastery is
     * reached, an empty grey frame when the art has no technique there.
     */
    private static void seal(GuiGraphics g, int cx, int cy, MartialArtsData.Technique t, MartialArtsData.Art art, float qi,
                             boolean top) {
        boolean locked = t != null && t.unlockMastery() > art.masteryPercent();
        boolean poor = t != null && !locked && t.qiCost() > qi;
        int line = poor ? ROSE : locked || t == null ? STEEL : CYAN;
        diamond(g, cx, cy, HALF, OUTLINE);
        diamond(g, cx, cy, HALF - 1, line);
        diamond(g, cx, cy, HALF - 2, OUTLINE);
        diamond(g, cx, cy, HALF - 3, t == null ? DEEP : poor ? ROSE_DARK : NIGHT);
        for (int dy = 1; dy <= HALF - 3; dy++) {
            int w = HALF - 3 - dy;
            rect(g, cx - w, cy + dy, 2 * w + 1, 1, t == null ? DEEP : poor ? 0xFF5C1F29 : DEEP);
        }
        if (t == null) {
            return;
        }
        if (locked) {
            sprite(g, cx - 3, cy - 3, TechniqueIcons.LOCK, c -> c);
        } else {
            String[] icon = TechniqueIcons.of(t.id());
            if (icon != null) {
                sprite(g, cx - 3, cy - 3, icon, c -> poor ? (c == GOLD_DARK ? OUTLINE : ROSE) : c);
            } else {
                String letters = initials(t.name().getString()).substring(0, 1);
                text(g, cx - 1, cy - 2, letters, poor ? ROSE : WHITE, 0);
            }
        }
        tag(g, cx, top ? cy - HALF - 2 : cy + HALF - 4, MartialArtsData.keyOf(art, t), line, poor ? ROSE : LIGHT);
    }

    /** A key in a small tag: a 7 x 7 frame of {@code line} around one letter. */
    private static void tag(GuiGraphics g, int cx, int y, String key, int line, int color) {
        rect(g, cx - 3, y, 7, 7, line);
        rect(g, cx - 2, y + 1, 5, 5, OUTLINE);
        text(g, cx - 1, y + 1, key.substring(0, 1), color, 0);
    }

    /** The ultimate's gold octagon: four rivets, gold rising from the bottom with its charge, a spark when full. */
    private static void ultimate(GuiGraphics g, int cx, int cy, MartialArtsData.Technique t, float charge, int frame) {
        boolean ready = charge >= 1;
        octagon(g, cx, cy, 11, 4, OUTLINE);
        octagon(g, cx, cy, 10, 3, ready ? GOLD_LIGHT : GOLD);
        octagon(g, cx, cy, 9, 3, GOLD_DARK);
        octagon(g, cx, cy, 8, 3, OUTLINE);
        octagon(g, cx, cy, 7, 3, 0xFF14243A);
        int top = cy + 7 - Math.round(14 * Mth.clamp(charge, 0, 1));
        for (int row = top; row <= cy + 7; row++) {
            int half = octagonHalf(7, 3, row - cy);
            rect(g, cx - half, row, 2 * half + 1, 1, ready ? 0xFF7A5A1A : 0xFF4A3610);
        }
        if (!ready && charge > 0) {
            int half = octagonHalf(7, 3, top - cy);
            rect(g, cx - half, top, 2 * half + 1, 1, GOLD_LIGHT);
        }
        for (int[] d : new int[][] {{-10, 0}, {10, 0}, {0, -10}}) {
            px(g, cx + d[0], cy + d[1], ready ? GOLD_PALE : GOLD_LIGHT);
        }
        if (t != null) {
            String[] icon = TechniqueIcons.of(t.id());
            if (icon != null) {
                sprite(g, cx - 5, cy - 5, icon, c -> ready ? c : c == WHITE ? GOLD_LIGHT : c == GOLD_PALE ? GOLD : 0xFF8A6A2A);
            } else {
                String letters = initials(t.name().getString());
                text(g, cx - width(letters) / 2, cy - 2, letters, GOLD_LIGHT, OUTLINE);
            }
        }
        if (ready) {
            int r = 13 + frame * 2;
            for (int[] d : new int[][] {{0, -r}, {r, 0}, {-r, 0}}) {
                px(g, cx + d[0], cy + d[1], GOLD_PALE);
            }
        }
        tag(g, cx, cy + 9, "R", ready ? GOLD_LIGHT : GOLD, GOLD_LIGHT);
    }

    /** "River Thrust" → "RT": what a seal shows until its technique has an icon. */
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
