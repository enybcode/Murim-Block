package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.martial.SanguineFluids;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;

/** How blood looks: its own still and flowing textures (blood-iceberg-blocks.cjs), half see-through, a red murk inside it. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class BloodFluidClient {
    private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "block/blood_still");
    private static final ResourceLocation FLOW = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "block/blood_flow");

    private BloodFluidClient() {
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return STILL; }
            @Override public ResourceLocation getFlowingTexture() { return FLOW; }
            @Override public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(.32F, .01F, .04F);
            }
        }, SanguineFluids.BLOOD_TYPE.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(SanguineFluids.BLOOD.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SanguineFluids.FLOWING_BLOOD.get(), RenderType.translucent());
        });
    }
}
