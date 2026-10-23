package com.murimblock.client.innerworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.innerworld.MeditatingBody;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Draws a meditating body with its owner's skin, seated with legs crossed in front and hands on the knees. */
public final class MeditatingBodyRenderer extends LivingEntityRenderer<MeditatingBody, PlayerModel<MeditatingBody>> {
    /** Player models are drawn at 15/16 scale, like {@code PlayerRenderer}. */
    private static final float SCALE = 0.9375f;
    /** Seated, the hips rest on the ground: the model sinks by the length of the legs. */
    private static final double SIT_DROP = 0.7;

    private final PlayerModel<MeditatingBody> wide;
    private final PlayerModel<MeditatingBody> slim;

    public MeditatingBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new SeatedModel(context, false), 0.5f);
        wide = model;
        slim = new SeatedModel(context, true);
    }

    @Override
    public void render(MeditatingBody body, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers,
                       int light) {
        model = skin(body).model() == PlayerSkin.Model.SLIM ? slim : wide;
        pose.pushPose();
        pose.translate(0, -SIT_DROP, 0);
        super.render(body, yaw, partialTicks, pose, buffers, light);
        pose.popPose();
    }

    @Override
    protected void scale(MeditatingBody body, PoseStack pose, float partialTicks) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(MeditatingBody body) {
        return skin(body).texture();
    }

    @Override
    protected boolean shouldShowName(MeditatingBody body) {
        return false;
    }

    /** The owner's skin while they are on the server (they are, unless the body waits for them to log back in). */
    private static PlayerSkin skin(MeditatingBody body) {
        UUID owner = body.owner().orElse(body.getUUID());
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owner);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(owner);
    }

    private static final class SeatedModel extends PlayerModel<MeditatingBody> {
        SeatedModel(EntityRendererProvider.Context context, boolean slim) {
            super(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
        }

        @Override
        public void setupAnim(MeditatingBody body, float limbSwing, float limbSwingAmount, float ageInTicks,
                              float netHeadYaw, float headPitch) {
            riding = true;
            super.setupAnim(body, 0, 0, ageInTicks, netHeadYaw, headPitch);
            // Thighs forward and spread, shins crossing in front; forearms resting on the knees, head slightly bowed.
            rightLeg.xRot = -1.45f;
            rightLeg.yRot = 0.45f;
            rightLeg.zRot = 0.1f;
            leftLeg.xRot = -1.45f;
            leftLeg.yRot = -0.45f;
            leftLeg.zRot = -0.1f;
            float breath = (float) Math.sin(ageInTicks * 0.05f) * 0.03f;
            rightArm.xRot = -0.55f + breath;
            rightArm.yRot = 0;
            rightArm.zRot = 0.18f;
            leftArm.xRot = -0.55f + breath;
            leftArm.yRot = 0;
            leftArm.zRot = -0.18f;
            head.xRot = 0.2f;
            rightPants.copyFrom(rightLeg);
            leftPants.copyFrom(leftLeg);
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            hat.copyFrom(head);
        }
    }
}
