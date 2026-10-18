package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.martial.OverlordQiEntity;
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
 * The Overlord Spear's shapes of Qi (martial/OverlordQiEntity), in one palette texture (scripts/animation/overlord-fx.cjs):
 * Tyrant's Thrust's beam (geo/overlord_beam, one block at scale 1, stretched to its width and length), the ultimate's
 * giant spear (geo/overlord_giant_spear, made as long as it is told), the shock wave (geo/overlord_ring) and the cracks
 * (geo/overlord_cracks), spread flat to their radius. Both shine (full light, their glowmasks); the beam's sheath is see-through, and both fade
 * out over their last ticks.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class OverlordQiRenderer extends GeoEntityRenderer<OverlordQiEntity> {
    /** The giant spear's model's own length at scale 1 (blocks); the ring and the cracks are one block across. */
    private static final float SPEAR_LENGTH = 3.82F;

    public OverlordQiRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        shadowRadius = 0;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.OVERLORD_QI.get(), OverlordQiRenderer::new);
    }

    @Override
    public void render(OverlordQiEntity qi, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(qi, yaw, partial, pose, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(OverlordQiEntity qi, PoseStack pose, float age, float yaw, float partial, float nativeScale) {
        pose.mulPose(qi.turn());
        float length = qi.length(partial);
        if (qi.kind() == OverlordQiEntity.BEAM) {
            pose.scale(qi.width(), length, qi.width());
        } else if (qi.kind() == OverlordQiEntity.RING || qi.kind() == OverlordQiEntity.CRACKS) {
            // Spread flat to twice its length (its radius), as thin as it was made.
            pose.scale(length * 2, 1, length * 2);
        } else {
            float s = length / SPEAR_LENGTH;
            pose.scale(s, s, s);
        }
    }

    @Override
    public Color getRenderColor(OverlordQiEntity qi, float partial, int light) {
        return Color.ofARGB(qi.alpha(partial), 1.0F, 1.0F, 1.0F);
    }

    @Override
    public RenderType getRenderType(OverlordQiEntity qi, ResourceLocation texture, MultiBufferSource buffers, float partial) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    private static final class Model extends GeoModel<OverlordQiEntity> {
        private static ResourceLocation asset(String path) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
        }

        @Override public ResourceLocation getModelResource(OverlordQiEntity qi) {
            return asset(switch (qi.kind()) {
                case OverlordQiEntity.BEAM -> "geo/overlord_beam.geo.json";
                case OverlordQiEntity.RING -> "geo/overlord_ring.geo.json";
                case OverlordQiEntity.CRACKS -> "geo/overlord_cracks.geo.json";
                default -> "geo/overlord_giant_spear.geo.json";
            });
        }

        @Override public ResourceLocation getTextureResource(OverlordQiEntity qi) {
            return asset("textures/entity/overlord_qi.png");
        }

        @Override public ResourceLocation getAnimationResource(OverlordQiEntity qi) {
            return asset("animations/overlord_qi.animation.json");
        }
    }
}
