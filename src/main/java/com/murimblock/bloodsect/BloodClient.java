package com.murimblock.bloodsect;

import com.murimblock.bloodsect.ninja.client.ReferenceNinjaModel;
import com.murimblock.bloodsect.ninja.client.BloodNinjaRenderer;
import com.murimblock.bloodsect.ninja.client.BloodSoldierRenderer;
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

@EventBusSubscriber(modid = BloodSect.ID, value = Dist.CLIENT)
public final class BloodClient {
    @SubscribeEvent
    public static void particles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BloodSect.BLOOD_DROPLET.get(), com.murimblock.bloodsect.ninja.client.BloodDropletParticle.Provider::new);
    }
    @SubscribeEvent
    public static void modelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ReferenceNinjaModel.LAYER, ReferenceNinjaModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BloodSect.BLOOD_NINJA.get(), BloodNinjaRenderer::new);
        event.registerEntityRenderer(BloodSect.BLOOD_SOLDIER.get(), BloodSoldierRenderer::new);
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.withDefaultNamespace("block/water_still"); }
            @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.withDefaultNamespace("block/water_flow"); }
            @Override public int getTintColor() { return 0xFF670914; }
        }, BloodSect.BLOOD_TYPE.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(BloodSect.BLOOD.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(BloodSect.FLOWING_BLOOD.get(), RenderType.translucent());
        });
    }
}
