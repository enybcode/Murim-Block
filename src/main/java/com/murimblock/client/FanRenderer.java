package com.murimblock.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Supplier;

/**
 * The iron fan in a hand, opened: its pieces (scripts/animation/sect-weapon-models.cjs, FAN) built upright round the
 * pivot at (8, PIVOT, 8) and turned here about it, a steel guard at each end, lacquered ribs between and a wedge of silk
 * in each gap, then the whole fan laid on the 45-degree diagonal of the vanilla swords, its pivot in the hand. The icon
 * in the inventory, on the ground and in frames stays the flat sprite.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class FanRenderer extends BlockEntityWithoutLevelRenderer {
    /** As FAN in sect-weapon-models.cjs: the pivot's height (pixels), the ribs with the guards, how far it opens. */
    private static final float PIVOT = 1.0F / 16;
    private static final int RIBS = 13;
    private static final float OPEN = 150;

    private static final ModelResourceLocation RIB = model("iron_fan_rib"), GUARD = model("iron_fan_guard"),
            PANEL = model("iron_fan_panel"), RIVET = model("iron_fan_rivet");
    private static final Supplier<FanRenderer> INSTANCE = Suppliers.memoize(FanRenderer::new);

    private FanRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "item/" + path));
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation m : new ModelResourceLocation[] {RIB, GUARD, PANEL, RIVET}) event.register(m);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return INSTANCE.get();
            }
        }, MurimItems.IRON_FAN.get());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var models = Minecraft.getInstance().getModelManager();
        pose.pushPose();
        // On the diagonal, as a sword.
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(-45));
        pose.translate(-0.5F, -0.5F, -0.5F);
        float step = OPEN / (RIBS - 1);
        for (int i = 0; i < RIBS; i++) {
            float angle = -OPEN / 2 + i * step;
            ribAt(pose, angle, models.getModel(i == 0 || i == RIBS - 1 ? GUARD : RIB), stack, buffers, light, overlay);
            if (i < RIBS - 1) ribAt(pose, angle + step / 2, models.getModel(PANEL), stack, buffers, light, overlay);
        }
        draw(models.getModel(RIVET), stack, pose, buffers, light, overlay);
        pose.popPose();
    }

    /** A piece turned `angle` degrees about the pivot, in the fan's plane. */
    private static void ribAt(PoseStack pose, float angle, BakedModel model, ItemStack stack, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, PIVOT, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-0.5F, -PIVOT, -0.5F);
        draw(model, stack, pose, buffers, light, overlay);
        pose.popPose();
    }

    static void draw(BakedModel model, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        for (RenderType type : model.getRenderTypes(stack, true))
            items.renderModelLists(model, stack, light, overlay, pose, ItemRenderer.getFoilBufferDirect(buffers, type, true, stack.hasFoil()));
    }
}
