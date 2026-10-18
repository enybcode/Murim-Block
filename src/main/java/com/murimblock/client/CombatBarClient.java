package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.martial.BloodCrystalState;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.network.TechniquePayloads;
import com.murimblock.qi.QiService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The combat bar on the client (docs/combat/STYLES.md, "La barre de combat"): in combat mode, the keys 1 to 0 (the nine
 * hotbar keys and Murim's tenth, & é " ' ( - è _ ç à on AZERTY) cast the techniques of the bar instead of changing the
 * held item; and the bar takes the hotbar's place at the bottom of the screen: the dantian (the Qi, the weapon in hand) between
 * five techniques on each side, with their cooldowns. The server decides everything.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatBarClient {
    /** Client game time at which each technique is ready again, and how long its cooldown was (for the fill). */
    private static final Map<ResourceLocation, long[]> COOLDOWNS = new HashMap<>();
    /** Client game time until which a riposte is open (after a perfect parry with Flowing Cloud Stance). */
    private static long riposteUntil;

    private CombatBarClient() {
    }

    /** From the server: a perfect parry opened a riposte window; the bar lights up until it closes. */
    public static void riposte(int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) riposteUntil = mc.level.getGameTime() + ticks;
    }

    /** Client game time until which the crescent's slam is ready (the player hangs at the top of its rise). */
    private static long slamUntil;

    /** From the server: hanging at the top of the crescent for that many ticks, the slam ready on its key (0: no longer). */
    public static void slamReady(int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) slamUntil = ticks > 0 ? mc.level.getGameTime() + ticks : 0;
    }

    /** Whether this client shows the crescent's slam ready. */
    public static boolean slamReady() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getGameTime() < slamUntil;
    }

    /** Whether this client shows a riposte window open. */
    public static boolean riposteOpen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getGameTime() < riposteUntil;
    }

    static boolean inCombat(LocalPlayer player) {
        return MurimblockApi.combat().isInCombatMode(player);
    }

    /** From the server: a technique was used; ready again in that many ticks. */
    public static void cooldown(ResourceLocation technique, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        COOLDOWNS.put(technique, new long[]{mc.level.getGameTime() + ticks, ticks});
    }

    /** Ticks until a technique is ready again, as this client knows it (0 when ready). */
    public static int cooldownLeft(ResourceLocation technique) {
        Minecraft mc = Minecraft.getInstance();
        long[] cd = COOLDOWNS.get(technique);
        return cd == null || mc.level == null ? 0 : (int) Math.max(0, cd[0] - mc.level.getGameTime());
    }

    /** For each slot whose key is held to aim: for how many ticks (-1: not held), and whether it already went off. */
    private static final int[] HELD = new int[MartialData.BAR_SLOTS];
    private static final boolean[] FIRED = new boolean[MartialData.BAR_SLOTS];

    static {
        java.util.Arrays.fill(HELD, -1);
    }

    /** Whether a technique is held before it goes (Cloud Step Slash: its draw stance) or while it lasts (the Spinning Rampart). */
    static boolean heldToAim(Optional<ResourceLocation> technique) {
        return technique.filter(id -> id.equals(MartialArts.CLOUD_STEP_SLASH.id()) || id.equals(MartialArts.BLOOD_CRYSTAL.id())
                || id.equals(MartialArts.SPINNING_RAMPART.id()) || id.equals(MartialArts.HEAVENS_DRUM.id())
                || id.equals(MartialArts.SEVERING_FLASH.id()) || id.equals(MartialArts.TYRANTS_THRUST.id())).isPresent();
    }

    /** How long a held technique may be held before it goes (or stops) by itself. */
    private static int maxHeld(Optional<ResourceLocation> technique) {
        if (technique.filter(MartialArts.BLOOD_CRYSTAL.id()::equals).isPresent()) return MartialArts.BLOOD_CRYSTAL_MAX_CHARGE;
        if (technique.filter(MartialArts.SPINNING_RAMPART.id()::equals).isPresent()) return MartialArts.RAMPART_MAX_TICKS;
        if (technique.filter(MartialArts.HEAVENS_DRUM.id()::equals).isPresent()) return MartialArts.HEAVENS_DRUM_MAX_TICKS;
        if (technique.filter(MartialArts.SEVERING_FLASH.id()::equals).isPresent()) return MartialArts.SEVERING_FLASH_MAX_CHARGE;
        if (technique.filter(MartialArts.TYRANTS_THRUST.id()::equals).isPresent()) return MartialArts.TYRANTS_THRUST_MAX_CHARGE;
        return MartialArts.CLOUD_STEP_AIM_MAX_TICKS;
    }

    /**
     * Before Minecraft reads the hotbar keys: in combat mode they cast instead, as the key goes down; a technique held to
     * aim takes its stance as the key goes down and goes as it is let go (or by itself after
     * {@link MartialArts#CLOUD_STEP_AIM_MAX_TICKS}).
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean active = player != null && mc.screen == null && inCombat(player);
        MartialData data = player == null ? null : MartialService.data(player);
        for (int i = 0; i < MartialData.BAR_SLOTS; i++) {
            KeyMapping key = slotKey(mc, i);
            if (!active || !heldToAim(data.slot(i))) {
                // A stance left held (a screen opened, combat mode left, the technique moved): it goes now.
                if (HELD[i] >= 0) release(i);
                if (!active) continue;
                while (key.consumeClick()) PacketDistributor.sendToServer(new TechniquePayloads.Cast(i));
                continue;
            }
            boolean clicked = false;
            while (key.consumeClick()) clicked = true;
            if (key.isDown() || (clicked && HELD[i] < 0)) {
                if (HELD[i] < 0) {
                    HELD[i] = 0;
                    FIRED[i] = false;
                    PacketDistributor.sendToServer(new TechniquePayloads.Aim(i));
                } else if (++HELD[i] >= maxHeld(data.slot(i)) && !FIRED[i]) {
                    FIRED[i] = true;
                    // Blood Crystal reaches its maximum on the server; a second cast would only report cooldown.
                    if (!data.slot(i).filter(MartialArts.BLOOD_CRYSTAL.id()::equals).isPresent())
                        PacketDistributor.sendToServer(new TechniquePayloads.Cast(i));
                }
                // A tap shorter than a tick: the stance, then at once the dash.
                if (!key.isDown()) release(i);
            } else if (HELD[i] >= 0) {
                release(i);
            }
        }
    }

    private static void release(int slot) {
        if (!FIRED[slot]) PacketDistributor.sendToServer(new TechniquePayloads.Cast(slot));
        HELD[slot] = -1;
        FIRED[slot] = false;
    }

    /** In combat mode the item hotbar gives way to the combat bar, drawn in its place. */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.HOTBAR.equals(event.getName()) && shown(Minecraft.getInstance())) event.setCanceled(true);
    }

    /** Whether the combat bar takes the hotbar's place now. Not over a finish's film (FinishCinematic hides the whole interface). */
    static boolean shown(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (FinishCinematic.active() || WarlordCinematic.active() || TransformationCinematic.active()) return false;
        return player != null && !player.isSpectator() && inCombat(player);
    }

    /**
     * The combat bar's own HUD layer, above the hotbar (MurimHudLayers): drawn there rather than inside the hotbar's
     * event, so a mod that hides or redraws the hotbar (Essential, a HUD mod) never takes it away. F1 hides it.
     */
    public static void renderLayer(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui && shown(mc)) render(g, mc, mc.player);
    }

    // The dantian design (17/10/2026, Enzo's choice "C"): in the middle a round dantian holding the weapon in hand, filled
    // with Qi like a vessel; five techniques on each side. Ink wells, the key small at the bottom right of each like an
    // item count, a cooldown darkening the well from the top with its seconds over it.
    static final int SLOT = 16, PITCH = 18, RADIUS = 13, GAP = 4;
    private static final int RIM = 0xFF24211E, RING = 0xFFC9A74B, RING_LIGHT = 0xFFF0D68A, VOID = 0xFF182838,
            QI = 0xFF238DD4, QI_LIGHT = 0xFF73C2F2, QI_DARK = 0xFF15598A, WELL = 0xE6342F2A, WELL_EDGE = 0xF024211E,
            KEY = 0xFFF0E6CC, KEY_EMPTY = 0xFF5A5148, HELD_EDGE = 0xFFC9A74B;

    /** The left of a slot's well, slots 0-4 left of the dantian and 5-9 right of it. */
    static int slotX(int centre, int slot) {
        int outer = RADIUS + 2;
        return slot < 5 ? centre - outer - GAP - (5 - slot) * PITCH + 1 : centre + outer + GAP + (slot - 5) * PITCH + 1;
    }

    /** The key shown on a slot: its digit for a number key whatever the keyboard (not "&" on AZERTY), else its first letter. */
    public static String keyLabel(Minecraft mc, int slot) {
        KeyMapping mapping = slotKey(mc, slot);
        com.mojang.blaze3d.platform.InputConstants.Key key = mapping.getKey();
        int code = key.getValue();
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
            if (code >= org.lwjgl.glfw.GLFW.GLFW_KEY_0 && code <= org.lwjgl.glfw.GLFW.GLFW_KEY_9) return String.valueOf((char) code);
            if (code >= org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0 && code <= org.lwjgl.glfw.GLFW.GLFW_KEY_KP_9) return String.valueOf(code - org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0);
        }
        String name = mapping.getTranslatedKeyMessage().getString();
        return name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }

    /** The combat bar where the hotbar was: the dantian in the middle, five techniques on each side. */
    static void render(GuiGraphics g, Minecraft mc, LocalPlayer player) {
        MartialData data = MartialService.data(player);
        int centre = g.guiWidth() / 2, bottom = g.guiHeight();
        int y = bottom - 20, cy = bottom - 15;
        int leftEdge = slotX(centre, 0) - 2, rightEdge = slotX(centre, 9) + SLOT + 1;
        // A riposte is open: a gold line pulsing under and over the bar, and its name on its right.
        if (riposteOpen() && mc.level != null) {
            int gold = (mc.level.getGameTime() / 3) % 2 == 0 ? 0xFFE8C25A : 0xFFB8902F;
            g.fill(leftEdge, y - 3, rightEdge, y - 2, gold);
            g.fill(leftEdge, y + SLOT + 2, rightEdge, y + SLOT + 3, gold);
            g.drawString(mc.font, Component.translatable("technique.murimblock.riposte"), rightEdge + 6, y + 4, 0xFFE8C25A, true);
        }
        if (data.mainArt().filter(MartialArts.SANGUINE_ART::equals).isPresent()) bloodDrops(g, mc, player, leftEdge - 52, y - 2);
        if (data.mainArt().filter(MartialArts.THUNDER_SABER::equals).isPresent()) thunderDrums(g, mc, player, leftEdge - 30, y + 3);
        for (int i = 0; i < MartialData.BAR_SLOTS; i++) {
            int x = slotX(centre, i);
            Optional<ResourceLocation> id = data.slot(i);
            MartialArts.Technique technique = id.map(MartialArts::technique).orElse(null);
            g.fill(x - 1, y - 1, x + SLOT + 1, y + SLOT + 1, HELD[i] >= 0 ? HELD_EDGE : WELL_EDGE);
            g.fill(x, y, x + SLOT, y + SLOT, WELL);
            if (technique != null) {
                TechniqueIcons.draw(g, technique, x, y, SLOT);
                // Hanging at the top of the crescent: its slot glows, pulsing azure, "Slam" beside the bar.
                if (technique == MartialArts.HEAVEN_SPLITTING_CRESCENT && slamReady() && mc.level != null) {
                    float pulse = 0.5F + 0.5F * net.minecraft.util.Mth.sin((mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false)) * 0.9F);
                    int alpha = (int) (110 + 120 * pulse);
                    g.fill(x, y, x + SLOT, y + SLOT, ((alpha / 3) << 24) | 0x8FD3FF);
                    int frame = (alpha << 24) | 0xA8E0FF;
                    g.fill(x - 2, y - 2, x + SLOT + 2, y - 1, frame);
                    g.fill(x - 2, y + SLOT + 1, x + SLOT + 2, y + SLOT + 2, frame);
                    g.fill(x - 2, y - 1, x - 1, y + SLOT + 1, frame);
                    g.fill(x + SLOT + 1, y - 1, x + SLOT + 2, y + SLOT + 1, frame);
                    g.drawString(mc.font, Component.translatable("technique.murimblock.slam_ready"), rightEdge + 6, y - 6, 0xFFA8E0FF, true);
                }
                long[] cd = COOLDOWNS.get(technique.id());
                if (cd != null && mc.level != null) {
                    long left = cd[0] - mc.level.getGameTime();
                    if (left > 0) {
                        // Ink from the top, as much as is left; the seconds over it.
                        int h = (int) Math.ceil(SLOT * (double) left / Math.max(1, cd[1]));
                        g.fill(x, y, x + SLOT, y + h, 0xC0141210);
                        String secs = String.valueOf((left + 19) / 20);
                        g.pose().pushPose();
                        g.pose().translate(0, 0, 200);
                        g.drawString(mc.font, secs, x + (SLOT - mc.font.width(secs)) / 2 + 1, y + 4, 0xFFF0E6CC, true);
                        g.pose().popPose();
                    }
                }
            }
            // The key, at the bottom right like an item count.
            String key = keyLabel(mc, i);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.drawString(mc.font, key, x + SLOT + 1 - mc.font.width(key), y + SLOT - 7, technique == null ? KEY_EMPTY : KEY, true);
            g.pose().popPose();
        }
        dantian(g, centre, cy, QiService.getQi(player), QiService.getQiMax(player));
        // The weapon in hand, in the dantian.
        g.pose().pushPose();
        g.pose().translate(0, 0, 50);
        g.renderItem(player.getMainHandItem(), centre - 8, cy - 8);
        g.renderItemDecorations(mc.font, player.getMainHandItem(), centre - 8, cy - 8);
        g.pose().popPose();
    }

    /** The dantian: a dark rim, a gold ring, inside it the Qi as a level of blue, its surface lit. */
    static void dantian(GuiGraphics g, int cx, int cy, double qi, double qiMax) {
        double ratio = qiMax <= 0 || !Double.isFinite(qi) ? 0 : Math.clamp(qi / qiMax, 0, 1);
        int level = cy + RADIUS - (int) Math.round(2 * RADIUS * ratio);
        for (int dy = -RADIUS - 2; dy <= RADIUS + 1; dy++) {
            double row = dy + 0.5;
            int outer = half(RADIUS + 2, row), ring = half(RADIUS + 1, row), inner = half(RADIUS, row);
            int y = cy + dy;
            if (outer > 0) g.fill(cx - outer, y, cx + outer, y + 1, RIM);
            if (ring > 0) g.fill(cx - ring, y, cx + ring, y + 1, dy < -RADIUS + 3 ? RING_LIGHT : RING);
            if (inner > 0) {
                int colour = y < level ? VOID : y == level ? QI_LIGHT : y >= cy + RADIUS - 2 ? QI_DARK : QI;
                g.fill(cx - inner, y, cx + inner, y + 1, colour);
            }
        }
    }

    /** Half the width of a disc of radius r at this row's height above or below its centre (whole pixels). */
    private static int half(double r, double row) {
        double w = r * r - row * row;
        return w <= 0 ? 0 : (int) Math.round(Math.sqrt(w));
    }

    /**
     * The Sanguine Art's blood gauge, no text: twenty small drops in two rows of ten over and beside the weapon slot, (x,
     * y) the bottom left: low enough to stay under the chat. They fill one by one, red; full, they all turn bright with a glint, pulsing (Blood Crystal Form is ready). In
     * the form, they become a row of ten small crystals, one lit for each dorsal crystal earned.
     */
    static void bloodDrops(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        var state = com.murimblock.martial.SanguineArt.state(player);
        boolean form = state.active(), full = !form && state.gauge() >= BloodCrystalState.MAX_GAUGE;
        int lit = form ? state.crystals() : state.gauge(), count = form ? BloodCrystalState.MAX_CRYSTALS : BloodCrystalState.MAX_GAUGE;
        int top = y - (count / 10) * 5;
        boolean bright = full && mc.level != null && (mc.level.getGameTime() / 6) % 2 == 0;
        for (int i = 0; i < count; i++) {
            int dx = x + (i % 10) * 5, dy = top + (i / 10) * 5;
            boolean on = i < lit;
            if (form) {
                // A small crystal: a dark rim, a light heart.
                int rim = on ? 0xFFB0263F : 0xC83A0C12, heart = on ? 0xFFFF8FA3 : 0xC83A0C12;
                g.fill(dx + 1, dy, dx + 2, dy + 4, rim);
                g.fill(dx, dy + 1, dx + 3, dy + 3, rim);
                g.fill(dx + 1, dy + 1, dx + 2, dy + 3, heart);
            } else {
                // A drop: its point up, round below.
                int col = !on ? 0xC83A0C12 : full ? (bright ? 0xFFFF8A96 : 0xFFFF6A7C) : 0xFFC0182C;
                g.fill(dx + 1, dy, dx + 2, dy + 1, col);
                g.fill(dx, dy + 1, dx + 3, dy + 3, col);
                g.fill(dx + 1, dy + 3, dx + 2, dy + 4, col);
                if (on) g.fill(dx, dy + 1, dx + 1, dy + 2, full ? 0xFFFFFFFF : 0xFFFF6A7C);
            }
        }
    }

    /**
     * The Purple Heaven Thunder Saber's drums, no text: three small bronze drums left of the weapon slot, (x, y) the top
     * left. Each fills with violet as its beats come (three to a drum); full, its skin glows and pulses.
     */
    static void thunderDrums(GuiGraphics g, Minecraft mc, LocalPlayer player, int x, int y) {
        int beats = ThunderSaberFx.beats(player.getId());
        boolean pulse = mc.level != null && (mc.level.getGameTime() / 5) % 2 == 0;
        for (int i = 0; i < 3; i++) {
            int dx = x + i * 9, filled = Math.max(0, Math.min(3, beats - i * 3));
            // The drum: a bronze body, its rims darker, its skin on top.
            g.fill(dx, y + 1, dx + 8, y + 9, 0xFF6E5020);
            g.fill(dx + 1, y + 2, dx + 7, y + 8, 0xFF2A2138);
            // The violet rises in it with its beats.
            if (filled > 0) g.fill(dx + 1, y + 8 - filled * 2, dx + 7, y + 8, filled == 3 ? (pulse ? 0xFFD9B8FF : 0xFFB58CFF) : 0xFF7A52C8);
            g.fill(dx, y, dx + 8, y + 1, filled == 3 ? 0xFFF2C94C : 0xFFC49A3E);
            g.fill(dx, y + 9, dx + 8, y + 10, 0xFF4A3410);
        }
    }

    /** The key of a combat bar slot: the nine hotbar keys, then Murim's tenth. */
    public static KeyMapping slotKey(Minecraft mc, int slot) {
        return slot < mc.options.keyHotbarSlots.length ? mc.options.keyHotbarSlots[slot] : MurimblockKeyMappings.COMBAT_SLOT_10;
    }

}
