package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.murimblock.Murimblock;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.martial.MartialArts;
import com.murimblock.network.CrescentMoonPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The Crescent Moon Halberd on this client (martial/CrescentMoonHalberd): its moonlight (Photon lights: a pale flash at
 * each impact, the steady light of the full moon), night falling round the ultimate (the sky and the screen darkened
 * deep blue, then the light coming back), and the moon by the crosshair: a small disc that fills, phase by phase, as
 * the halberd's blows land (Waxing Moon), white and glowing when full.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrescentMoonClient {
    private static int phases;
    private static long nightUntil, nightFrom;
    private static Vec3 nightAt = Vec3.ZERO;
    private static float nightRange;
    /** How long night takes to fall and to lift (ticks). */
    private static final int NIGHT_FADE = 20;

    private CrescentMoonClient() {
    }

    public static void phases(CrescentMoonPayloads.Phases p) {
        phases = p.phases();
    }

    public static int phasesForTests() {
        return phases;
    }

    public static void fx(CrescentMoonPayloads.Fx p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.kind() == CrescentMoonPayloads.Fx.NIGHT) {
            if (mc.level == null) return;
            nightFrom = mc.level.getGameTime();
            nightUntil = nightFrom + p.ticks();
            nightAt = new Vec3(p.x(), p.y(), p.z());
            nightRange = p.range();
            return;
        }
        if (!PhotonLights.isEnabled()) return;
        boolean volumetric = PhotonLights.isVolumetricEnabled();
        if (p.kind() == CrescentMoonPayloads.Fx.MOONLIGHT) {
            // The full moon's light: wide, soft, cold, for as long as it stands in the sky.
            PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(0.62F, 0.8F, 1.0F)
                    .intensity(2.4F).range(p.range()).sourceRadius(p.range() * 0.15F).volumetric(volumetric ? 0.7F : 0), p.ticks());
            return;
        }
        // An impact: a flash of pale moonlight and a white heart.
        PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(0.5F, 0.75F, 1.0F)
                .intensity(2.0F).range(p.range()).sourceRadius(p.range() * 0.1F).volumetric(volumetric ? 0.4F : 0), p.ticks());
        PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(0.95F, 0.98F, 1.0F)
                .intensity(1.4F).range(p.range() * 0.4F).sourceRadius(p.range() * 0.05F).volumetric(0), Math.max(3, p.ticks() / 2));
    }

    /** How deep the night is for this player now (0 to 1): fallen over a second, lifted over its last second. */
    static float night() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return 0;
        long now = mc.level.getGameTime();
        if (now >= nightUntil) return 0;
        if (mc.player.position().distanceTo(nightAt) > nightRange) return 0;
        float in = Mth.clamp((now - nightFrom) / (float) NIGHT_FADE, 0, 1), out = Mth.clamp((nightUntil - now) / (float) NIGHT_FADE, 0, 1);
        return Math.min(in, out);
    }

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float n = night();
        if (n <= 0) return;
        event.setRed(Mth.lerp(n, event.getRed(), 0.02F));
        event.setGreen(Mth.lerp(n, event.getGreen(), 0.04F));
        event.setBlue(Mth.lerp(n, event.getBlue(), 0.12F));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        phases = 0;
        nightUntil = 0;
    }

    /** The HUD: the night's veil over the screen, and the moon by the crosshair for whoever fights with the art. */
    public static void renderLayer(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float n = night();
        if (n > 0) g.fill(0, 0, w, h, ((int) (n * 120) << 24) | 0x040A24);
        boolean art = MeditationService.aura(mc.player).mainArt().filter(MartialArts.CRESCENT_MOON_HALBERD::equals).isPresent();
        if (!art || phases <= 0 || !com.murimblock.martial.CelestialPeakSpear.holdsSpear(mc.player)) return;
        moon(g, w / 2 + 10, h / 2 - 4, phases);
    }

    /** A small moon of radius 4: lit from the right, phase by phase; full, it shines white with a halo. */
    static void moon(GuiGraphics g, int cx, int cy, int lit) {
        int full = com.murimblock.martial.CrescentMoonHalberd.PHASES;
        boolean whole = lit >= full;
        for (int y = -4; y <= 4; y++) for (int x = -4; x <= 4; x++) {
            double d = Math.hypot(x, y);
            if (d > 4.3) {
                if (whole && d <= 5.6) g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, 0x559FD8FF);
                continue;
            }
            // The terminator moves from the right edge to the left as the phases fill.
            double edge = 4 - 8.0 * lit / full;
            boolean on = x >= edge * Math.sqrt(Math.max(0, 1 - (y * y) / 16.0));
            int c = on ? (whole ? 0xFFF4FBFF : 0xFFBFE4FF) : 0x88203050;
            g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, c);
        }
    }
}
