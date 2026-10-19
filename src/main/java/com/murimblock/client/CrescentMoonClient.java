package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.murimblock.Murimblock;
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
 * each impact, a reflection appearing or shattering, the steady light of the full moon), and night falling round the
 * ultimate (the sky and the screen darkened deep blue, then the light coming back).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrescentMoonClient {
    private static long nightUntil, nightFrom;
    private static Vec3 nightAt = Vec3.ZERO;
    private static float nightRange;
    /** How long night takes to fall and to lift (ticks). */
    private static final int NIGHT_FADE = 20;

    private CrescentMoonClient() {
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
        nightUntil = 0;
    }

    /** The HUD: the night's veil over the screen. */
    public static void renderLayer(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        float n = night();
        if (n > 0) g.fill(0, 0, g.guiWidth(), g.guiHeight(), ((int) (n * 120) << 24) | 0x040A24);
    }
}
