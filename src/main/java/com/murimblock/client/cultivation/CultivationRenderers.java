package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.cultivation.inner.HeartDemonEntity;
import com.murimblock.cultivation.inner.MeditatingBodyEntity;
import com.murimblock.cultivation.inner.QiMoteEntity;
import com.murimblock.cultivation.inner.RemnantSoulEntity;
import com.murimblock.integration.epicfight.HeartDemonPatch;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

/** How the inner world's figures are drawn: the body left in meditation, the Heart Demon, the remnant soul, the motes. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CultivationRenderers {
    static final ResourceLocation DEMON_EYES = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/heart_demon_eyes.png");
    static final ResourceLocation MOTE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/particle/qi_mote.png");
    private static boolean patchedHooked;

    private CultivationRenderers() {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.MEDITATING_BODY.get(), BodyRenderer::new);
        event.registerEntityRenderer(MurimEntities.HEART_DEMON.get(), DemonRenderer::new);
        event.registerEntityRenderer(MurimEntities.REMNANT_SOUL.get(), SoulRenderer::new);
        event.registerEntityRenderer(MurimEntities.QI_MOTE.get(), MoteRenderer::new);
        if (!patchedHooked) {
            patchedHooked = true;
            EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(added ->
                    added.addPatchedEntityRenderer(MurimEntities.HEART_DEMON.get(), type ->
                            new PatchedDemon(added.getContext(), type).initLayerLast(added.getContext(), type)));
        }
    }

    // ---------------------------------------------------------------- the body in the lotus

    /** The body left behind: its owner's skin, seated cross-legged, the hands joined before the belly. */
    static final class BodyRenderer extends EntityRenderer<MeditatingBodyEntity> {
        private final PlayerModel<net.minecraft.world.entity.LivingEntity> wide, slim;

        BodyRenderer(EntityRendererProvider.Context context) {
            super(context);
            wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
            slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
            shadowRadius = 0.4F;
        }

        @Override
        public void render(MeditatingBodyEntity body, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            PlayerSkin skin = Skins.of(body.owner());
            PlayerModel<net.minecraft.world.entity.LivingEntity> model = skin.model() == PlayerSkin.Model.SLIM ? slim : wide;
            lotus(model, body.tickCount + partial);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180 - body.getYRot()));
            pose.scale(-1, -1, 1);
            pose.scale(0.9375F, 0.9375F, 0.9375F);
            // Seated: the hips on the ground.
            pose.translate(0, -1.501F, 0);
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(skin.texture())), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            super.render(body, yaw, partial, pose, buffers, light);
        }

        /** The vanilla model in the lotus: legs forward and crossed, back straight, hands joined, a slow breath. */
        static void lotus(PlayerModel<?> model, float time) {
            model.setAllVisible(true);
            float breath = Mth.sin(time * 0.06F) * 0.02F;
            model.head.xRot = 0.08F;
            model.head.yRot = 0;
            model.body.xRot = breath;
            model.rightLeg.xRot = -1.45F;
            model.rightLeg.yRot = -0.55F;
            model.rightLeg.zRot = 0;
            model.leftLeg.xRot = -1.45F;
            model.leftLeg.yRot = 0.55F;
            model.leftLeg.zRot = 0;
            model.rightArm.xRot = -0.75F + breath;
            model.rightArm.yRot = -0.42F;
            model.rightArm.zRot = 0;
            model.leftArm.xRot = -0.75F + breath;
            model.leftArm.yRot = 0.42F;
            model.leftArm.zRot = 0;
            model.hat.copyFrom(model.head);
            model.jacket.copyFrom(model.body);
            model.rightPants.copyFrom(model.rightLeg);
            model.leftPants.copyFrom(model.leftLeg);
            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
        }

        @Override
        public ResourceLocation getTextureLocation(MeditatingBodyEntity body) {
            return Skins.of(body.owner()).texture();
        }
    }

    // ---------------------------------------------------------------- the Heart Demon

    /** Its owner's skin (vanilla's model when Epic Fight is off; Epic Fight draws it through PatchedDemon). */
    static final class DemonRenderer extends HumanoidMobRenderer<HeartDemonEntity, PlayerModel<HeartDemonEntity>> {
        DemonRenderer(EntityRendererProvider.Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(HeartDemonEntity demon) {
            return Skins.of(demon.owner()).texture();
        }
    }

    /**
     * Epic Fight's humanoid renderer on the player's own mesh: drawn dark (every colour pressed down to a deep violet),
     * then its eyes again, glowing red.
     */
    static final class PatchedDemon extends PHumanoidRenderer<HeartDemonEntity, HeartDemonPatch, PlayerModel<HeartDemonEntity>, DemonRenderer, HumanoidMesh> {
        PatchedDemon(EntityRendererProvider.Context context, EntityType<?> type) {
            super(Meshes.BIPED, context, type);
        }

        @Override
        public void render(HeartDemonEntity demon, HeartDemonPatch patch, DemonRenderer renderer, MultiBufferSource buffers, PoseStack pose, int light, float partial) {
            float shade = demon.shadow() ? 0.55F : 0.32F;
            MultiBufferSource dark = type -> new Darkened(buffers.getBuffer(type), shade * 0.85F, shade * 0.6F, shade);
            super.render(demon, patch, renderer, dark, pose, light, partial);
            RenderType eyes = RenderType.eyes(DEMON_EYES);
            super.render(demon, patch, renderer, type -> buffers.getBuffer(eyes), pose, LightTexture.FULL_BRIGHT, partial);
        }
    }

    /** A vertex sink that darkens every colour written through it (the Heart Demon's shadowed copy of a skin). */
    record Darkened(VertexConsumer inner, float r, float g, float b) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { inner.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            inner.setColor(Math.round(red * r), Math.round(green * g), Math.round(blue * b), alpha);
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { inner.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { inner.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { inner.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { inner.setNormal(x, y, z); return this; }
    }

    // ---------------------------------------------------------------- the remnant soul

    /**
     * The old master, a ghost: the GeckoLib model of scripts/animation/remnant-soul.cjs (a robe thinning into a tail of
     * mist instead of legs), see-through and lit from within, floating above the ground, its glow breathing, a few
     * wisps of pale Qi drifting up from it.
     */
    static final class SoulRenderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<RemnantSoulEntity> {
        private static final float[] WISP = {0.72F, 0.9F, 1.0F};

        SoulRenderer(EntityRendererProvider.Context context) {
            super(context, new software.bernie.geckolib.model.DefaultedEntityGeoModel<>(
                    ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "remnant_soul"), true));
            shadowRadius = 0.0F;
            addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void render(RemnantSoulEntity soul, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.translate(0, 0.35, 0);
            super.render(soul, yaw, partial, pose, buffers, LightTexture.FULL_BRIGHT);
            pose.popPose();
            if (soul.tickCount != soul.wispTick && soul.level() instanceof net.minecraft.client.multiplayer.ClientLevel level) {
                soul.wispTick = soul.tickCount;
                var random = soul.getRandom();
                if (random.nextInt(3) == 0) {
                    double angle = random.nextDouble() * Math.PI * 2, r = 0.2 + random.nextDouble() * 0.3;
                    AuraClient.spark(level, soul.getX() + Math.cos(angle) * r, soul.getY() + 0.2 + random.nextDouble() * 0.9,
                            soul.getZ() + Math.sin(angle) * r, 0, 0.02 + random.nextDouble() * 0.02, 0, WISP, random.nextBoolean());
                }
            }
        }

        @Override
        public RenderType getRenderType(RemnantSoulEntity soul, ResourceLocation texture, MultiBufferSource buffers, float partial) {
            return RenderType.entityTranslucent(texture);
        }

        @Override
        public software.bernie.geckolib.util.Color getRenderColor(RemnantSoulEntity soul, float partial, int light) {
            // The ghost's glow breathes: more or less see-through, slowly.
            float alpha = 0.78F + Mth.sin((soul.tickCount + partial) * 0.05F) * 0.14F;
            return software.bernie.geckolib.util.Color.ofARGB(alpha, 1.0F, 1.0F, 1.0F);
        }
    }

    // ---------------------------------------------------------------- a mote of Qi

    /** A bright point of Qi facing the camera, breathing in size. */
    static final class MoteRenderer extends EntityRenderer<QiMoteEntity> {
        MoteRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(QiMoteEntity mote, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            float t = mote.tickCount + partial;
            boolean golden = mote.kind() == QiMoteEntity.GOLDEN, focus = mote.getId() == MeditationClient.focusedMote();
            float size = (0.32F + Mth.sin(t * 0.25F) * 0.06F) * (golden ? 1.45F : 1) * (focus ? 1.5F : 1) * Math.min(1, t / 12F);
            pose.pushPose();
            pose.translate(0, 0.25 + Mth.sin(t * 0.1F) * 0.12, 0);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.scale(size, size, size);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(MOTE));
            Matrix4f m = pose.last().pose();
            if (golden) quad(vc, m, pose.last(), 1.0F, 0.84F, 0.42F);
            else if (focus) quad(vc, m, pose.last(), 0.92F, 0.98F, 1.0F);
            else quad(vc, m, pose.last(), 0.75F, 0.92F, 1.0F);
            pose.popPose();
            super.render(mote, yaw, partial, pose, buffers, light);
        }

        static void quad(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, float r, float g, float b) {
            vertex(vc, m, last, -1, -1, 0, 1, r, g, b);
            vertex(vc, m, last, 1, -1, 1, 1, r, g, b);
            vertex(vc, m, last, 1, 1, 1, 0, r, g, b);
            vertex(vc, m, last, -1, 1, 0, 0, r, g, b);
        }

        private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack.Pose last, float x, float y, float u, float v, float r, float g, float b) {
            vc.addVertex(m, x, y, 0).setColor(r, g, b, 1.0F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(last, 0, 1, 0);
        }

        @Override
        public ResourceLocation getTextureLocation(QiMoteEntity mote) {
            return MOTE;
        }
    }
}
