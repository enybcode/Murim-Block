package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.api.MurimblockApi;
import com.murimblock.martial.BloodCrystalState;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.network.TechniquePayloads;
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
 * held item; and the bar takes the hotbar's place at the bottom of the screen, with each technique's cooldown. The server decides everything.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatBarClient {
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation OFFHAND_LEFT = ResourceLocation.withDefaultNamespace("hud/hotbar_offhand_left");
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

    /** Whether a technique is held to aim before it goes (Cloud Step Slash: its draw stance). */
    static boolean heldToAim(Optional<ResourceLocation> technique) {
        return technique.filter(id -> id.equals(MartialArts.CLOUD_STEP_SLASH.id()) || id.equals(MartialArts.BLOOD_CRYSTAL.id())).isPresent();
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
                } else if (++HELD[i] >= (data.slot(i).filter(MartialArts.BLOOD_CRYSTAL.id()::equals).isPresent() ? MartialArts.BLOOD_CRYSTAL_MAX_CHARGE : MartialArts.CLOUD_STEP_AIM_MAX_TICKS) && !FIRED[i]) {
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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || !inCombat(player) || !VanillaGuiLayers.HOTBAR.equals(event.getName())) return;
        event.setCanceled(true);
        // F1 (hidden interface) hides it, like the hotbar it replaces.
        if (!mc.options.hideGui) render(event.getGuiGraphics(), mc, player);
    }

    /**
     * The combat bar where the hotbar was: Minecraft's hotbar frame widened to ten slots (the first eight slots of its
     * sprite, then its last two and its edge), each slot showing its technique, its cooldown and its key; the held weapon
     * in a small slot on the left, like the offhand slot.
     */
    static void render(GuiGraphics g, Minecraft mc, LocalPlayer player) {
        MartialData data = MartialService.data(player);
        int width = 202, x0 = (g.guiWidth() - width) / 2, y = g.guiHeight() - 22;
        g.blitSprite(HOTBAR, 182, 22, 0, 0, x0, y, 161, 22);
        g.blitSprite(HOTBAR, 182, 22, 141, 0, x0 + 161, y, 41, 22);
        // A riposte is open: a gold frame pulsing around the bar, and its name above it.
        if (riposteOpen() && mc.level != null) {
            int gold = (mc.level.getGameTime() / 3) % 2 == 0 ? 0xFFE8C25A : 0xFFB8902F;
            g.fill(x0 - 1, y - 1, x0 + width + 1, y, gold);
            g.fill(x0 - 1, y + 22, x0 + width + 1, y + 23, gold);
            g.fill(x0 - 1, y, x0, y + 22, gold);
            g.fill(x0 + width, y, x0 + width + 1, y + 22, gold);
            // On the right of the bar (above it are the Qi bar and the level).
            g.drawString(mc.font, Component.translatable("technique.murimblock.riposte"), x0 + width + 6, y + 7, 0xFFE8C25A, true);
        }
        if (data.mainArt().filter(MartialArts.SANGUINE_ART::equals).isPresent()) bloodDrops(g, mc, player, x0 - 52, y - 2);
        // The weapon in hand.
        g.blitSprite(OFFHAND_LEFT, x0 - 29, y - 1, 29, 24);
        g.renderItem(player.getMainHandItem(), x0 - 26, y + 3);
        g.renderItemDecorations(mc.font, player.getMainHandItem(), x0 - 26, y + 3);
        for (int i = 0; i < MartialData.BAR_SLOTS; i++) {
            int x = x0 + 3 + i * 20;
            Optional<ResourceLocation> id = data.slot(i);
            MartialArts.Technique technique = id.map(MartialArts::technique).orElse(null);
            if (technique != null) {
                TechniqueIcons.draw(g, technique, x, y + 3, 16);
                // Hanging at the top of the crescent: its slot glows, pulsing azure, "Slam" beside the bar.
                if (technique == MartialArts.HEAVEN_SPLITTING_CRESCENT && slamReady() && mc.level != null) {
                    float pulse = 0.5F + 0.5F * net.minecraft.util.Mth.sin((mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false)) * 0.9F);
                    int alpha = (int) (110 + 120 * pulse);
                    g.fill(x, y + 3, x + 16, y + 19, ((alpha / 3) << 24) | 0x8FD3FF);
                    int frame = (alpha << 24) | 0xA8E0FF;
                    g.fill(x - 2, y + 1, x + 18, y + 2, frame);
                    g.fill(x - 2, y + 20, x + 18, y + 21, frame);
                    g.fill(x - 2, y + 2, x - 1, y + 20, frame);
                    g.fill(x + 17, y + 2, x + 18, y + 20, frame);
                    g.drawString(mc.font, Component.translatable("technique.murimblock.slam_ready"), x0 + width + 6, y - 4, 0xFFA8E0FF, true);
                }
                long[] cd = COOLDOWNS.get(technique.id());
                if (cd != null && mc.level != null) {
                    long left = cd[0] - mc.level.getGameTime();
                    if (left > 0) {
                        int h = (int) Math.ceil(16 * (double) left / Math.max(1, cd[1]));
                        g.fill(x, y + 19 - h, x + 16, y + 19, 0xB0000000);
                    }
                }
            }
            // The slot's key, small, in the top left corner.
            String key = slotKey(mc, i).getTranslatedKeyMessage().getString();
            String label = key.length() > 1 ? key.substring(0, 1) : key;
            g.pose().pushPose();
            g.pose().translate(x - 1, y + 2, 200);
            g.pose().scale(0.5F, 0.5F, 1);
            g.drawString(mc.font, label, 0, 0, 0xFFE8DCB8, true);
            g.pose().popPose();
        }
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

    /** The key of a combat bar slot: the nine hotbar keys, then Murim's tenth. */
    public static KeyMapping slotKey(Minecraft mc, int slot) {
        return slot < mc.options.keyHotbarSlots.length ? mc.options.keyHotbarSlots[slot] : MurimblockKeyMappings.COMBAT_SLOT_10;
    }

}
