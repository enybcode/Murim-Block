package com.colleuhu95.bloodsect;

import com.colleuhu95.bloodsect.ninja.client.ReferenceNinjaModel;
import com.colleuhu95.bloodsect.ninja.client.BloodNinjaRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = BloodSectDecor.ID, value = Dist.CLIENT)
public final class BloodClient {
    @SubscribeEvent
    public static void particles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BloodSectDecor.BLOOD_DROPLET.get(), com.colleuhu95.bloodsect.ninja.client.BloodDropletParticle.Provider::new);
    }
    @SubscribeEvent
    public static void modelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ReferenceNinjaModel.LAYER, ReferenceNinjaModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BloodSectDecor.BLOOD_NINJA.get(), BloodNinjaRenderer::new);
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.withDefaultNamespace("block/water_still"); }
            @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.withDefaultNamespace("block/water_flow"); }
            @Override public int getTintColor() { return 0xFF670914; }
        }, BloodSectDecor.BLOOD_TYPE.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(BloodSectDecor.BLOOD.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(BloodSectDecor.FLOWING_BLOOD.get(), RenderType.translucent());
        });
    }
}
