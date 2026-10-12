package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.WaterBallEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws the water ball with GeckoLib from geo/entity/water_ball.geo.json and animations/entity/water_ball.animation.json
 * (exported from the Blockbench model). Translucent, centred on the hitbox, scaled for the small bubbles and turned
 * so a burst faces what it hit.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class WaterBallRenderer extends GeoEntityRenderer<WaterBallEntity> {
    public WaterBallRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "water_ball")));
        shadowRadius = 0.0F;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.WATER_BALL.get(), WaterBallRenderer::new);
    }

    @Override
    public RenderType getRenderType(WaterBallEntity ball, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected void applyRotations(WaterBallEntity ball, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                  float partialTick, float nativeScale) {
        // The model is centred on its origin: lift it to the middle of the hitbox.
        poseStack.translate(0, ball.getBbHeight() / 2, 0);
        if (ball.state() >= WaterBallEntity.SPLASH_GROUND) poseStack.mulPose(Axis.YP.rotationDegrees(ball.splashYaw()));
        float scale = ball.ballScale();
        poseStack.scale(scale, scale, scale);
    }
}
