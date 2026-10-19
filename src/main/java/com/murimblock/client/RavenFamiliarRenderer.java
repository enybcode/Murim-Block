package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.martial.RavenFamiliarEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The Shadow Raven (martial/RavenFamiliarEntity), from geo/raven_familiar: its amber eyes and gold shine in the dark. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenFamiliarRenderer extends GeoEntityRenderer<RavenFamiliarEntity> {
    public RavenFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        shadowRadius = 0.2F;
        withScale(1.15F);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.RAVEN_FAMILIAR.get(), RavenFamiliarRenderer::new);
    }

    private static final class Model extends GeoModel<RavenFamiliarEntity> {
        private static ResourceLocation asset(String path) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
        }

        @Override public ResourceLocation getModelResource(RavenFamiliarEntity raven) { return asset("geo/raven_familiar.geo.json"); }
        @Override public ResourceLocation getTextureResource(RavenFamiliarEntity raven) { return asset("textures/entity/raven_familiar.png"); }
        @Override public ResourceLocation getAnimationResource(RavenFamiliarEntity raven) { return asset("animations/raven_familiar.animation.json"); }
    }
}
