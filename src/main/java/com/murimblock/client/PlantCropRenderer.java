package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.world.plant.MurimFlora;
import com.murimblock.world.plant.PlantCatalog;
import com.murimblock.world.plant.PlantCropBlock;
import com.murimblock.world.plant.PlantCropBlockEntity;
import com.murimblock.world.plant.PlantGrowth;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws a planted herb or flower (PlantCropBlockEntity) with GeckoLib: its stage's model (geo/block/plant/<species>_stage
 * <k>), or for a species modelled only grown, that model smaller while it grows; its colour picked by its place (the
 * chrysanthemum's gold, white or purple), turned by its place; its sway always; its glowing parts in the dark when it has
 * them (their glowmask).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class PlantCropRenderer extends GeoBlockRenderer<PlantCropBlockEntity> {
    /** Whether a texture has its glowmask (asked once). */
    private static final Map<ResourceLocation, Boolean> GLOWS = new HashMap<>();

    public PlantCropRenderer() {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override public void render(PoseStack pose, PlantCropBlockEntity crop, BakedGeoModel model, RenderType type, MultiBufferSource buffers,
                                         VertexConsumer buffer, float partial, int light, int overlay) {
                if (glows(getTextureResource(crop))) super.render(pose, crop, model, type, buffers, buffer, partial, light, overlay);
            }
        });
    }

    static boolean glows(ResourceLocation texture) {
        return GLOWS.computeIfAbsent(texture, t -> Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(t.getNamespace(), t.getPath().replace(".png", "_glowmask.png"))).isPresent());
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MurimFlora.CROP_ENTITY.get(), context -> new PlantCropRenderer());
    }

    /** A species modelled only grown is drawn smaller while it grows. */
    @Override public void preRender(PoseStack pose, PlantCropBlockEntity crop, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                                    boolean isReRender, float partial, int light, int overlay, int colour) {
        super.preRender(pose, crop, model, buffers, buffer, isReRender, partial, light, overlay, colour);
        if (isReRender) return;
        PlantCatalog.Species species = PlantCropBlock.species(crop.getBlockState());
        int age = crop.getBlockState().getValue(PlantCropBlock.AGE);
        // Each plant turned its own way, by its place.
        long seed = Mth.getSeed(crop.getBlockPos());
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((seed & 3) * 90));
        if (species.stagesModelled() < 4) {
            float s = age == PlantGrowth.SEEDLING ? 0.35F : age == PlantGrowth.YOUNG ? 0.65F : 1.0F;
            pose.scale(s, s, s);
        }
    }

    private static final class Model extends GeoModel<PlantCropBlockEntity> {
        private static ResourceLocation asset(String path) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
        }

        @Override public ResourceLocation getModelResource(PlantCropBlockEntity crop) {
            PlantCatalog.Species species = PlantCropBlock.species(crop.getBlockState());
            int stage = species.stagesModelled() >= 4 ? crop.getBlockState().getValue(PlantCropBlock.AGE) : 0;
            return asset("geo/block/plant/" + species.id() + "_stage" + stage + ".geo.json");
        }

        @Override public ResourceLocation getTextureResource(PlantCropBlockEntity crop) {
            PlantCatalog.Species species = PlantCropBlock.species(crop.getBlockState());
            var colours = species.colours();
            String colour = colours.get((int) Math.floorMod(Mth.getSeed(crop.getBlockPos()) >> 3, colours.size()));
            return asset("textures/block/plant/" + species.id() + "_" + colour + ".png");
        }

        @Override public ResourceLocation getAnimationResource(PlantCropBlockEntity crop) {
            return asset("animations/block/plant/" + PlantCropBlock.species(crop.getBlockState()).id() + ".animation.json");
        }
    }
}
