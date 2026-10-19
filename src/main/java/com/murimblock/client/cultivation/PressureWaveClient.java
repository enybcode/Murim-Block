package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.AuraPressure;
import com.murimblock.cultivation.MeditationService;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Space rippling round an overflowing aura (AuraPressure, Enzo 18/10/2026, after Bleach): near a player holding
 * {@link AuraPressure#OVERWHELMING_QI} Qi or more (not sneaking), every pulse a ring of bent light spreads from their feet
 * over the screen and fades (filter shaders/post/pressure_wave). The filter stays loaded while such a player is near (its
 * strength nothing between waves); never over another filter (Qi vision keeps its own).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class PressureWaveClient {
    static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "shaders/post/pressure_wave.json");
    /** How near the overflowing one must be (blocks), and how long a wave spreads (ticks). */
    static final double NEAR = 40;
    static final int SPREAD = 14;

    private static boolean effectOn;
    private static Player source;

    private PressureWaveClient() {
    }

    /** The nearest player overflowing (oneself too), or null. */
    static Player overflowing(Minecraft mc) {
        Player best = null;
        for (Player p : mc.level.players()) {
            if (p.isSpectator() || p.isCrouching() || p.distanceToSqr(mc.player) > NEAR * NEAR) continue;
            if (MeditationService.aura(p).qi() < AuraPressure.OVERWHELMING_QI) continue;
            if (best == null || p.distanceToSqr(mc.player) < best.distanceToSqr(mc.player)) best = p;
        }
        return best;
    }

    /** Whether the filter on now is ours. */
    static boolean isOn(Minecraft mc) {
        return mc.gameRenderer.currentEffect() != null && mc.gameRenderer.currentEffect().getName().equals(EFFECT.toString());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Replaced by another filter (Qi vision): ours is off.
        if (effectOn && !isOn(mc)) effectOn = false;
        if (QiSenseClient.open()) return;
        source = mc.level == null || mc.player == null ? null : overflowing(mc);
        if (source != null && !effectOn && mc.gameRenderer.currentEffect() == null) {
            mc.gameRenderer.loadEffect(EFFECT);
            effectOn = true;
        } else if (source == null && effectOn) {
            if (isOn(mc)) mc.gameRenderer.shutdownEffect();
            effectOn = false;
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !effectOn) return;
        Minecraft mc = Minecraft.getInstance();
        var effect = mc.gameRenderer.currentEffect();
        if (effect == null || mc.level == null || !effect.getName().equals(EFFECT.toString())) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float phase = (mc.level.getGameTime() % AuraPressure.PULSE) + partial;
        float strength = 0;
        if (source != null && phase < SPREAD) {
            Vec3 at = source.getPosition(partial).add(0, 0.2, 0).subtract(event.getCamera().getPosition());
            org.joml.Matrix4f toScreen = new org.joml.Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
            org.joml.Vector4f v = toScreen.transform(new org.joml.Vector4f((float) at.x, (float) at.y, (float) at.z, 1));
            if (v.w > 0.05F) {
                effect.setUniform("CenterX", v.x / v.w * 0.5F + 0.5F);
                effect.setUniform("CenterY", v.y / v.w * 0.5F + 0.5F);
                // Fainter the further off it starts.
                double far = Math.sqrt(source.distanceToSqr(mc.player)) / NEAR;
                strength = (1 - phase / SPREAD) * (float) (1 - 0.7 * far);
            }
            effect.setUniform("Radius", phase / SPREAD * 0.7F);
        }
        effect.setUniform("Strength", Math.max(0, strength));
    }
}
