package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.zigythebird.playeranim.accessors.IAnimatedPlayer;
import com.zigythebird.playeranim.animation.PlayerAnimManager;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Knees and elbows for our player animations. Player Animation Library reads a {@code "bend"} track on each limb (in
 * degrees, the Emotecraft format) but only bendable-cuboids, which does not exist for 1.21.1, can draw it. So while
 * a limb is bent, its vanilla cube is skipped and drawn here in two halves cut from the player's own skin: thigh and
 * shin, upper arm and forearm. The upper half follows the animated limb; the lower half turns at the joint by the
 * bend: a knee folds the shin back, an elbow folds the forearm forward.
 */
public final class BentLimbsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final String[] LIMBS = {"right_arm", "left_arm", "right_leg", "left_leg"};
    private static final float STRAIGHT = 0.001f;
    private static final float HALF = 6;
    /** Bends of the player being drawn, in radians, taken before the vanilla limbs are skipped. */
    private static final Map<Integer, float[]> BENDS = new HashMap<>();

    private final ModelPart wide = create(false);
    private final ModelPart slim = create(true);

    public BentLimbsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Each limb as an upper half with its lower half as a child at the joint; skin and outer layer. */
    private static ModelPart create(boolean slimArms) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int armWidth = slimArms ? 3 : 4;
        limb(root, "right_arm", slimArms ? -2 : -3, -2, armWidth, 40, 16, 40, 32);
        limb(root, "left_arm", -1, -2, armWidth, 32, 48, 48, 48);
        limb(root, "right_leg", -2, 0, 4, 0, 16, 0, 32);
        limb(root, "left_leg", -2, 0, 4, 16, 48, 0, 48);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    /**
     * A limb box from {@code top} down 12 pixels, cut in two: the upper half's texture is the top of the skin's limb, the
     * lower half (offset 6 down, with its own top face 4 rows lower) the bottom.
     */
    private static void limb(PartDefinition root, String name, float x, float top, int width, int u, int v, int outerU,
                             int outerV) {
        CubeDeformation outer = new CubeDeformation(0.25f);
        PartDefinition upper = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(x, top, -2, width, HALF, 4), PartPose.ZERO);
        upper.addOrReplaceChild("outer", CubeListBuilder.create().texOffs(outerU, outerV)
                .addBox(x, top, -2, width, HALF, 4, outer), PartPose.ZERO);
        PartDefinition lower = upper.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(u, v + 6)
                .addBox(x, 0, -2, width, HALF, 4), PartPose.offset(0, top + HALF, 0));
        lower.addOrReplaceChild("outer", CubeListBuilder.create().texOffs(outerU, outerV + 6)
                .addBox(x, 0, -2, width, HALF, 4, outer), PartPose.ZERO);
    }

    /** The bend of each limb now, in radians, or null when every limb is straight. */
    private static float[] bends(AbstractClientPlayer player) {
        PlayerAnimManager manager = player instanceof IAnimatedPlayer animated ? animated.playerAnimLib$getAnimManager() : null;
        if (manager == null || !manager.isActive()) {
            return null;
        }
        float[] bends = new float[LIMBS.length];
        boolean any = false;
        for (int i = 0; i < LIMBS.length; i++) {
            bends[i] = manager.get3DTransform(new PlayerAnimBone(LIMBS[i])).getBend();
            any |= Math.abs(bends[i]) > STRAIGHT;
        }
        return any ? bends : null;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        float[] bends = BENDS.get(player.getId());
        if (bends == null || player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        ModelPart limbs = player.getSkin().model() == PlayerSkin.Model.SLIM ? slim : wide;
        ModelPart[] vanilla = {model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        PlayerModelPart[] outer = {PlayerModelPart.RIGHT_SLEEVE, PlayerModelPart.LEFT_SLEEVE,
                PlayerModelPart.RIGHT_PANTS_LEG, PlayerModelPart.LEFT_PANTS_LEG};
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0);
        for (int i = 0; i < LIMBS.length; i++) {
            if (Math.abs(bends[i]) <= STRAIGHT) {
                continue;
            }
            ModelPart upper = limbs.getChild(LIMBS[i]);
            upper.copyFrom(vanilla[i]);
            boolean shown = player.isModelPartShown(outer[i]);
            upper.getChild("outer").visible = shown;
            ModelPart lower = upper.getChild("lower");
            // Knees fold the shin back (+x turn), elbows fold the forearm forward.
            lower.xRot = i < 2 ? -bends[i] : bends[i];
            lower.getChild("outer").visible = shown;
            upper.render(poseStack, buffer, light, overlay);
        }
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new BentLimbsLayer(renderer));
                }
            }
        }

        /**
         * Bent limbs are skipped in the vanilla model (skipDraw survives the renderer's own visibility reset). Late, so
         * the meditation legs' own reset does not show them again.
         */
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void beforePlayer(RenderPlayerEvent.Pre event) {
            AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
            float[] bends = bends(player);
            if (bends == null) {
                BENDS.remove(player.getId());
                return;
            }
            BENDS.put(player.getId(), bends);
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            skip(model.rightArm, model.rightSleeve, bends[0]);
            skip(model.leftArm, model.leftSleeve, bends[1]);
            skip(model.rightLeg, model.rightPants, bends[2]);
            skip(model.leftLeg, model.leftPants, bends[3]);
        }

        @SubscribeEvent
        public static void afterPlayer(RenderPlayerEvent.Post event) {
            if (BENDS.remove(event.getEntity().getId()) != null) {
                PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
                for (ModelPart part : new ModelPart[] {model.rightArm, model.rightSleeve, model.leftArm, model.leftSleeve,
                        model.rightLeg, model.rightPants, model.leftLeg, model.leftPants}) {
                    part.skipDraw = false;
                }
            }
        }

        private static void skip(ModelPart limb, ModelPart outer, float bend) {
            boolean bent = Math.abs(bend) > STRAIGHT;
            limb.skipDraw |= bent;
            outer.skipDraw |= bent;
        }
    }
}
