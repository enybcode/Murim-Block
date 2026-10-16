package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.mob.CrabPincerEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws the Crab King's giant pincer with GeckoLib from geo/entity/crab_pincer.geo.json, its animation
 * animations/entity/crab_pincer.animation.json and its texture (scripts/animation/crab-pincer-geo.cjs; the Blockbench
 * project art/blockbench/crab_pincer.bbmodel), {@link #SCALE} times its size: some seven blocks tall. Its own turn sets
 * where it pinches and strikes.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrabPincerRenderer extends GeoEntityRenderer<CrabPincerEntity> {
    /** How much larger than its model it is drawn (its reach, CrabPincerEntity.REACH, is the model's length at it). */
    public static final float SCALE = 2.2F;

    public CrabPincerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "crab_pincer")));
        shadowRadius = 0.0F;
        withScale(SCALE);
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.CRAB_PINCER.get(), CrabPincerRenderer::new);
    }
}
