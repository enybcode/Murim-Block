package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.martial.RavenGateEntity;
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

/**
 * The Raven Messenger Art's doors (martial/RavenGateEntity), from geo/raven_gate (one block across at scale 1, lying
 * flat): laid on the ground, turned face down, or stood up like a door and made a little taller than wide; sized to its
 * radius as it opens and shuts, turning slowly. Its rim and veil shine (full light, its glowmask).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenGateRenderer extends GeoEntityRenderer<RavenGateEntity> {
    public RavenGateRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        shadowRadius = 0;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.RAVEN_GATE.get(), RavenGateRenderer::new);
    }

    @Override
    public void render(RavenGateEntity gate, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(gate, yaw, partial, pose, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(RavenGateEntity gate, PoseStack pose, float age, float yaw, float partial, float nativeScale) {
        float r = gate.size() * 2 * gate.openness(partial);
        switch (gate.kind()) {
            case RavenGateEntity.SKY -> pose.mulPose(Axis.XP.rotationDegrees(180));
            case RavenGateEntity.DOOR -> {
                pose.translate(0, gate.size() * 1.25F, 0);
                pose.mulPose(Axis.YP.rotationDegrees(-gate.facing()));
                pose.mulPose(Axis.XP.rotationDegrees(90));
            }
            default -> { }
        }
        pose.mulPose(Axis.YP.rotationDegrees(gate.spin(partial)));
        float tall = gate.kind() == RavenGateEntity.DOOR ? 1.3F : 1.0F;
        pose.scale(r, 1, r * tall);
    }

    @Override
    public RenderType getRenderType(RavenGateEntity gate, ResourceLocation texture, MultiBufferSource buffers, float partial) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    private static final class Model extends GeoModel<RavenGateEntity> {
        private static ResourceLocation asset(String path) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
        }

        @Override public ResourceLocation getModelResource(RavenGateEntity gate) { return asset("geo/raven_gate.geo.json"); }
        @Override public ResourceLocation getTextureResource(RavenGateEntity gate) { return asset("textures/entity/raven_gate.png"); }
        @Override public ResourceLocation getAnimationResource(RavenGateEntity gate) { return asset("animations/raven_gate.animation.json"); }
    }
}
