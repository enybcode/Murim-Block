package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.martial.MoonQiEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * The Crescent Moon Halberd's ultimate's moonlight (martial/MoonQiEntity), in one palette texture
 * (scripts/animation/crescent-moon-fx.cjs): the full moon and its beams. Both shine (full light, their glowmask), are partly see-through, grow in and fade out.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MoonQiRenderer extends GeoEntityRenderer<MoonQiEntity> {
    public MoonQiRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        shadowRadius = 0;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.MOON_QI.get(), MoonQiRenderer::new);
    }

    @Override
    public void render(MoonQiEntity qi, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(qi, yaw, partial, pose, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(MoonQiEntity qi, PoseStack pose, float age, float yaw, float partial, float nativeScale) {
        pose.mulPose(qi.turn());
        float s = qi.size(partial);
        if (qi.kind() == MoonQiEntity.BEAM) pose.scale(s, qi.length(), s);
        else pose.scale(s, s, s);
    }

    @Override
    public Color getRenderColor(MoonQiEntity qi, float partial, int light) {
        return Color.ofARGB(qi.alpha(partial), 1.0F, 1.0F, 1.0F);
    }

    @Override
    public RenderType getRenderType(MoonQiEntity qi, ResourceLocation texture, MultiBufferSource buffers, float partial) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    private static final class Model extends GeoModel<MoonQiEntity> {
        private static ResourceLocation asset(String path) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
        }

        @Override public ResourceLocation getModelResource(MoonQiEntity qi) {
            return asset(switch (qi.kind()) {
                case MoonQiEntity.BEAM -> "geo/moon_beam.geo.json";
                default -> "geo/moon_full.geo.json";
            });
        }

        @Override public ResourceLocation getTextureResource(MoonQiEntity qi) {
            return asset("textures/entity/moon_qi.png");
        }

        @Override public ResourceLocation getAnimationResource(MoonQiEntity qi) {
            return asset("animations/moon_qi.animation.json");
        }
    }
}
