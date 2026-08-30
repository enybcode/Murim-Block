package com.murimblock.client;

import com.murimblock.Murimblock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiChargeFovHandler {
    private static final float MIN_FOV_FACTOR = 0.85F;
    private static final float TRANSITION_STEP = 1.0F / 10.0F;
    private static float transition;

    private QiChargeFovHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        transition = nextTransition(transition, QiChargeClientHandler.isVisualCharging(), TRANSITION_STEP);
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (transition <= 0.0F) {
            return;
        }

        float factor = fovFactor(transition);
        event.setNewFovModifier(event.getNewFovModifier() * factor);
    }

    static float nextTransition(float current, boolean charging, float step) {
        float delta = charging ? step : -step;
        return clamp01(current + delta);
    }

    static float fovFactor(float transition) {
        return 1.0F - ((1.0F - MIN_FOV_FACTOR) * clamp01(transition));
    }

    static void resetForTests() {
        transition = 0.0F;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
