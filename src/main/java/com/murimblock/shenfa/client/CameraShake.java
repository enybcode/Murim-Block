package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Shakes the camera (a clip's {@code shake} event): the view turns a little back and forth, the strength fading out
 * over the shake's ticks. A stronger shake started during a weaker one takes over.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CameraShake {
    /** The most the view turns (degrees) at strength 1. */
    private static final float DEGREES = 3;
    private static float strength;
    private static int ticks, left;

    private CameraShake() {
    }

    /** Shakes the camera at {@code strength} (1 turns it {@link #DEGREES}) over {@code ticks} ticks. */
    public static void start(float strength, int ticks) {
        if (ticks <= 0 || strength < now()) {
            return;
        }
        CameraShake.strength = strength;
        CameraShake.ticks = CameraShake.left = ticks;
    }

    /** The shake's strength now (for checks); 0 when still. */
    public static float now() {
        return left <= 0 ? 0 : strength * left / ticks;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (left > 0 && !Minecraft.getInstance().isPaused()) {
            left--;
        }
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles event) {
        if (left <= 0) {
            return;
        }
        float t = (float) (ticks - left + event.getPartialTick());
        float fade = Math.max(0, 1 - t / ticks);
        float size = strength * fade * fade * DEGREES;
        // Three unrelated waves, so the shake does not read as a swing.
        event.setPitch(event.getPitch() + size * Mth.sin(t * 2.9f));
        event.setYaw(event.getYaw() + size * 0.7f * Mth.sin(t * 2.3f + 1.7f));
        event.setRoll(event.getRoll() + size * 0.5f * Mth.sin(t * 3.7f + 0.6f));
    }
}
