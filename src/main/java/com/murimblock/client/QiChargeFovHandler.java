package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.qi.charge.QiChargeVisuals;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiChargeFovHandler {
    private static float transition;

    private QiChargeFovHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        transition = nextTransition(
                transition,
                QiChargeClientHandler.isVisualCharging(),
                QiChargeVisuals.QI_CHARGE_FOV_TRANSITION_STEP
        );
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
        float eased = smoothstep(clamp01(transition));
        return 1.0F - ((1.0F - QiChargeVisuals.QI_CHARGE_FOV_MULTIPLIER) * eased);
    }

    static void resetForTests() {
        transition = 0.0F;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float smoothstep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
