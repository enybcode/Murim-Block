package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.FlyingNeedleEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Quaternionf;

/** A thrown needle: the flying_needle model (along +x, scripts/animation/sect-weapon-models.cjs) turned to its flight. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class FlyingNeedleRenderer extends EntityRenderer<FlyingNeedleEntity> {
    private static final ModelResourceLocation MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "item/flying_needle"));

    public FlyingNeedleRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.FLYING_NEEDLE.get(), FlyingNeedleRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingNeedleEntity needle) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(FlyingNeedleEntity needle, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL);
        ItemStack stack = MurimItems.FLYING_NEEDLES.toStack();
        Vec3 dir = needle.heading();
        pose.pushPose();
        pose.translate(0, needle.getBbHeight() / 2, 0);
        pose.mulPose(new Quaternionf().rotationTo(1, 0, 0, (float) dir.x, (float) dir.y, (float) dir.z));
        // A little larger than in the hand, so it reads in flight.
        pose.scale(1.2F, 1.2F, 1.2F);
        pose.translate(-0.5F, -0.5F, -0.5F);
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        for (RenderType type : model.getRenderTypes(stack, true))
            items.renderModelLists(model, stack, light, OverlayTexture.NO_OVERLAY, pose, ItemRenderer.getFoilBufferDirect(buffers, type, true, false));
        pose.popPose();
        super.render(needle, entityYaw, partialTick, pose, buffers, light);
    }
}
