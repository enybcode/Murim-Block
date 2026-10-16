package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.mob.CrabMistEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Inside the Crab King's mist (CrabMistEntity) the world shuts down to a few blocks of grey-white: the fog closes in as
 * thick as the mist is where the camera stands. The mist itself is drawn by its own puffs; it has no model.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrabMistClient {
    /** At its thickest, the view ends this far (blocks). */
    private static final float NEAR_VIEW = 7.0F;
    private static final float[] GREY = {0.74F, 0.76F, 0.78F};

    private CrabMistClient() { }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.CRAB_MIST.get(), NoopRenderer::new);
    }

    /** How thick the mist is where the camera stands, 0 to 1. */
    static float thickness(Vec3 eye, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return 0;
        float k = 0;
        for (CrabMistEntity mist : mc.level.getEntitiesOfClass(CrabMistEntity.class, mc.player.getBoundingBox().inflate(CrabMistEntity.MAX_RADIUS + 2)))
            k = Math.max(k, mist.densityAt(eye, partial));
        return k;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        float k = thickness(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (k <= 0.01F) return;
        float far = Mth.lerp(k, event.getFarPlaneDistance(), NEAR_VIEW), near = Mth.lerp(k, event.getNearPlaneDistance(), -2.0F);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(near);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float k = thickness(event.getCamera().getPosition(), (float) event.getPartialTick());
        if (k <= 0.01F) return;
        event.setRed(Mth.lerp(k, event.getRed(), GREY[0]));
        event.setGreen(Mth.lerp(k, event.getGreen(), GREY[1]));
        event.setBlue(Mth.lerp(k, event.getBlue(), GREY[2]));
    }
}
