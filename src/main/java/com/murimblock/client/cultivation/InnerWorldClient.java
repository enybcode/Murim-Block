package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.InnerWorldService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * How the inner world looks to its cultivator: no sun nor stars, a void whose colour is their Qi (grey and stormy when
 * the reserve is low, a clear deep blue when it is full), a soft mist far off, motes of Qi drifting; and a red haze
 * after failed great breakthroughs (an unstable foundation).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldClient {
    private InnerWorldClient() {
    }

    @SubscribeEvent
    public static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "inner_world"), new Effects());
    }

    /** The void's colour for a reserve this full, reddened by failures. */
    static Vec3 voidColour(float fill, int failures) {
        Vec3 storm = new Vec3(0.20, 0.18, 0.24), clear = new Vec3(0.08, 0.15, 0.34);
        Vec3 c = storm.lerp(clear, Mth.clamp(fill, 0, 1));
        if (failures > 0) c = c.lerp(new Vec3(0.30, 0.06, 0.08), Math.min(0.5, failures * 0.15));
        return c;
    }

    static final class Effects extends DimensionSpecialEffects {
        Effects() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return fog;
            return voidColour(MeditationService.aura(mc.player).fill(), CultivationService.journal(mc.player).failures());
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !InnerWorldService.isInside(mc.player)) return;
        Vec3 c = voidColour(MeditationService.aura(mc.player).fill(), CultivationService.journal(mc.player).failures());
        event.setRed((float) c.x);
        event.setGreen((float) c.y);
        event.setBlue((float) c.z);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !InnerWorldService.isInside(mc.player)) return;
        event.setNearPlaneDistance(24);
        event.setFarPlaneDistance(90);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !InnerWorldService.isInside(mc.player)) return;
        RandomSource random = level.random;
        Vec3 at = mc.player.position();
        // Motes of Qi drifting round, in the main style's colour.
        float[] c = AuraClient.colour(MeditationService.aura(mc.player).mainArt());
        for (int i = 0; i < 3; i++) {
            AuraParticle p = AuraClient.spark(level, at.x + (random.nextDouble() - 0.5) * 30, at.y - 3 + random.nextDouble() * 12, at.z + (random.nextDouble() - 0.5) * 30,
                    (random.nextDouble() - 0.5) * 0.02, 0.01 + random.nextDouble() * 0.01, (random.nextDouble() - 0.5) * 0.02, c, true);
            if (p != null) p.big(0.08F, 80);
        }
        int failures = CultivationService.journal(mc.player).failures();
        if (failures > 0 && random.nextInt(3) < failures) {
            DustParticleOptions mist = new DustParticleOptions(new Vector3f(0.55F, 0.05F, 0.06F), 2.5F);
            level.addParticle(mist, at.x + (random.nextDouble() - 0.5) * 16, at.y + random.nextDouble() * 3, at.z + (random.nextDouble() - 0.5) * 16, 0, 0.005, 0);
        }
    }
}
