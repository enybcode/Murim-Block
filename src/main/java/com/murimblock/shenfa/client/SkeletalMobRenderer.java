package com.murimblock.shenfa.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.shenfa.Pose;
import com.murimblock.shenfa.SkinnedMesh;
import com.murimblock.shenfa.SkinnedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Draws a mob with its look ({@link SkeletalMobs}) instead of its vanilla model: the skinned model posed by Shenfa,
 * its glowing parts, worn armor (the biped's armor parts, on the same skeleton) and held items at the grip joints.
 * Vanilla's renderer still draws it when our looks are off or the model is missing; the shadow, fire, name tag and
 * hitbox stay vanilla's.
 */
public final class SkeletalMobRenderer<T extends Mob> extends EntityRenderer<T> {
    private static final float PIXEL = 1 / 16f;
    private final EntityRenderer<? super T> vanilla;

    public SkeletalMobRenderer(EntityRendererProvider.Context context, EntityRenderer<? super T> vanilla) {
        super(context);
        this.vanilla = vanilla;
        shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(T mob) {
        SkeletalMobs.Look look = SkeletalMobs.look(mob);
        return look != null ? look.texture() : vanilla.getTextureLocation(mob);
    }

    @Override
    public void render(T mob, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        SkeletalMobs.Look look = SkeletalMobs.look(mob);
        SkinnedModel model = look == null ? null : SkeletalModels.get(look.model());
        if (model == null) {
            vanilla.render(mob, yaw, partialTick, poseStack, buffers, light);
            return;
        }
        float bodyYaw = Mth.rotLerp(partialTick, mob.yBodyRotO, mob.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, mob.yHeadRotO, mob.yHeadRot);
        float pitch = Mth.lerp(partialTick, mob.xRotO, mob.getXRot());
        Pose pose = SkeletalMobs.pose(mob, partialTick, Mth.wrapDegrees(headYaw - bodyYaw), pitch);
        Matrix4f[] skin = pose.toSkinMatrices(model.armature());
        Matrix4f[] joints = pose.toModelMatrices(model.armature());

        poseStack.pushPose();
        float scale = mob.getScale();
        poseStack.scale(scale, scale, scale);
        if (mob.isFullyFrozen()) {
            bodyYaw += (float) (Math.cos(mob.tickCount * 3.25) * Math.PI * 0.4f);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        if (mob.deathTime > 0 && !SkeletalMobs.diesByClip(mob)) {
            // Vanilla's fall to the side when dying (a look with its own death clip falls by it).
            float fall = Mth.sqrt((mob.deathTime + partialTick - 1) / 20f * 1.6f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Math.min(fall, 1) * 90));
        }
        poseStack.translate(0, 0.001f, 0);

        Minecraft mc = Minecraft.getInstance();
        boolean visible = !mob.isInvisible();
        boolean ghost = !visible && mc.player != null && !mob.isInvisibleTo(mc.player);
        RenderType type = visible ? RenderType.entityCutoutNoCull(look.texture())
                : ghost ? RenderType.itemEntityTranslucentCull(look.texture())
                : mc.shouldEntityAppearGlowing(mob) ? RenderType.outline(look.texture()) : null;
        poseStack.pushPose();
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        if (type != null) {
            int overlay = LivingEntityRenderer.getOverlayCoords(mob, 0);
            draw(model, skin, poseStack, buffers.getBuffer(type), ghost ? 0x26FFFFFF : 0xFFFFFFFF, light, overlay);
            if (visible && look.glow() != null) {
                draw(model, skin, poseStack, buffers.getBuffer(RenderType.eyes(look.glow())), 0xFFFFFFFF, light,
                        OverlayTexture.NO_OVERLAY);
            }
        }
        SkinnedModel armor = SkeletalModels.get(SkeletalModels.BIPED);
        if (visible && armor != null) {
            SkeletalArmor.render(mob, slot -> false, armor, skin, poseStack.last(), buffers, light, name -> true);
        }
        poseStack.popPose();
        if (visible) {
            holdItem(mob, HumanoidArm.RIGHT, joints[model.armature().get("Tool_R").index()], poseStack, buffers, light);
            holdItem(mob, HumanoidArm.LEFT, joints[model.armature().get("Tool_L").index()], poseStack, buffers, light);
        }
        poseStack.popPose();
        // The name tag, when the mob has one.
        super.render(mob, yaw, partialTick, poseStack, buffers, light);
    }

    private static void draw(SkinnedModel model, Matrix4f[] skin, PoseStack poseStack, VertexConsumer consumer,
                             int color, int light, int overlay) {
        for (SkinnedMesh part : model.parts()) {
            if (!part.armor()) {
                SkeletalPlayerRenderer.draw(part, skin, poseStack.last(), consumer, color, light, overlay);
            }
        }
    }

    private static void holdItem(Mob mob, HumanoidArm arm, Matrix4f tool, PoseStack poseStack,
                                 MultiBufferSource buffers, int light) {
        ItemStack stack = mob.getMainArm() == arm ? mob.getMainHandItem() : mob.getOffhandItem();
        if (stack.isEmpty()) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        poseStack.pushPose();
        Matrix4f joint = new Matrix4f().scale(PIXEL).mul(tool).scale(16);
        poseStack.mulPose(joint.mul(left ? SkeletalPlayerRenderer.LEFT_GRIP : SkeletalPlayerRenderer.RIGHT_GRIP));
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(mob, stack,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, poseStack, buffers, light);
        poseStack.popPose();
    }
}
