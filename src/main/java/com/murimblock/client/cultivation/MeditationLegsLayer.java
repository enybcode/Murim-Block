package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Minecraft legs have no knee, so a meditating player's legs are drawn here instead, each as a thigh and a shin cut
 * from the player's own skin: seated cross-legged, knees out to the sides, shins crossed in front. The vanilla legs
 * are skipped while meditating.
 */
public final class MeditationLegsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Thighs go forward and out from the hips; shins come back inward and cross in front. */
    private static final float THIGH_OUT = (float) Math.toRadians(58);
    private static final float SHIN_IN = (float) Math.toRadians(-80);
    private static final float LEVEL = (float) Math.toRadians(-90);
    private static final float SEGMENT = 6;

    private final ModelPart legs = createLegs();

    public MeditationLegsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Thigh and shin of each leg, base and trousers layer, from the 64 x 64 player skin. */
    private static ModelPart createLegs() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation pants = new CubeDeformation(0.25f);
        segment(root, "right_thigh", 0, 16, 0, 32, CubeDeformation.NONE, pants);
        segment(root, "right_shin", 0, 22, 0, 38, CubeDeformation.NONE, pants);
        segment(root, "left_thigh", 16, 48, 0, 48, CubeDeformation.NONE, pants);
        segment(root, "left_shin", 16, 54, 0, 54, CubeDeformation.NONE, pants);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    /** One leg segment, 4 x 6 x 4 along +y from its pivot: the top half of the skin's leg, or the bottom half. */
    private static void segment(PartDefinition root, String name, int u, int v, int pantsU, int pantsV,
                                CubeDeformation base, CubeDeformation pants) {
        PartDefinition part = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(-2, 0, -2, 4, SEGMENT, 4, base), PartPose.ZERO);
        part.addOrReplaceChild("pants", CubeListBuilder.create().texOffs(pantsU, pantsV)
                .addBox(-2, 0, -2, 4, SEGMENT, 4, pants), PartPose.ZERO);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (!ClientCultivation.isMeditating(player) || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        place("right", model.rightLeg, -1, 0);
        place("left", model.leftLeg, 1, -1);
        legs.getChild("right_thigh").getChild("pants").visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        legs.getChild("right_shin").getChild("pants").visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        legs.getChild("left_thigh").getChild("pants").visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        legs.getChild("left_shin").getChild("pants").visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);

        PlayerSkin skin = player.getSkin();
        ResourceLocation texture = skin.texture();
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0);
        legs.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(texture)), light, overlay);
    }

    /**
     * Places a thigh at the hip (where the animation left the vanilla leg) and its shin at the knee. {@code side} is
     * -1 for the right leg (model -x) and 1 for the left; {@code lift} raises one shin a pixel so they cross cleanly.
     */
    private void place(String name, ModelPart hip, int side, float lift) {
        float thighYaw = -side * THIGH_OUT;
        ModelPart thigh = legs.getChild(name + "_thigh");
        thigh.setPos(hip.x, hip.y, hip.z);
        thigh.setRotation(LEVEL, thighYaw, 0);
        // Pitched level, a segment points forward along (-sin yaw, 0, -cos yaw): the player faces -z in model space.
        float kneeX = hip.x - SEGMENT * (float) Math.sin(thighYaw);
        float kneeZ = hip.z - SEGMENT * (float) Math.cos(thighYaw);
        ModelPart shin = legs.getChild(name + "_shin");
        shin.setPos(kneeX, hip.y + lift, kneeZ);
        shin.setRotation(LEVEL, -side * SHIN_IN, 0);
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new MeditationLegsLayer(renderer));
                }
            }
        }

        /** skipDraw survives the renderer's own visibility reset, unlike {@code visible}. */
        @SubscribeEvent
        public static void beforePlayer(RenderPlayerEvent.Pre event) {
            hideVanillaLegs(event.getRenderer().getModel(), ClientCultivation.isMeditating(event.getEntity()));
        }

        @SubscribeEvent
        public static void afterPlayer(RenderPlayerEvent.Post event) {
            hideVanillaLegs(event.getRenderer().getModel(), false);
        }

        private static void hideVanillaLegs(PlayerModel<AbstractClientPlayer> model, boolean hide) {
            model.rightLeg.skipDraw = hide;
            model.leftLeg.skipDraw = hide;
            model.rightPants.skipDraw = hide;
            model.leftPants.skipDraw = hide;
        }
    }
}
