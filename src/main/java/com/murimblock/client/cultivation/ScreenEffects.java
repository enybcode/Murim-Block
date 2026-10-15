package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** A flash of colour over the screen that fades (entering the inner world, a breakthrough) and a shake of the view. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ScreenEffects {
    private static int flashColour, flashTicks, flashLeft;
    private static int shakeTicks, shakeLeft;
    private static float shakeStrength;

    private ScreenEffects() {
    }

    public static void flash(int argb, int ticks) {
        flashColour = argb;
        flashTicks = ticks;
        flashLeft = ticks;
    }

    public static void shake(int ticks, float strength) {
        shakeTicks = ticks;
        shakeLeft = ticks;
        shakeStrength = strength;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (flashLeft > 0) flashLeft--;
        if (shakeLeft > 0) shakeLeft--;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (flashLeft <= 0) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float t = Math.max(0, (flashLeft - partial) / flashTicks);
        int alpha = Math.round(((flashColour >>> 24) & 255) * t * t);
        GuiGraphics g = event.getGuiGraphics();
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | (flashColour & 0xFFFFFF));
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeLeft <= 0 || Minecraft.getInstance().level == null) return;
        float t = (float) shakeLeft / shakeTicks;
        double time = Minecraft.getInstance().level.getGameTime() + event.getPartialTick();
        event.setRoll(event.getRoll() + (float) Math.sin(time * 2.3) * shakeStrength * t);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 3.1) * shakeStrength * 0.5F * t);
    }
}
