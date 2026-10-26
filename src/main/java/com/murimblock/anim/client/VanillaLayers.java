package com.murimblock.anim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.anim.Armature;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Runs the player renderer's layers (cape, elytra, arrows and stingers stuck in the body, head items, parrots, other
 * mods' layers) around our skinned body. They draw against the vanilla player model, so its parts are first moved to
 * follow our skeleton: each rigid vanilla part takes the turn and place of the joint it hangs from. The cape and the
 * elytra hang from the chest. Layers we draw ourselves (armor, held items) and our own vanilla-model layers are left
 * out.
 */
final class VanillaLayers {
    private static final float PIXEL = 1 / 16f;
    /** Vanilla's model sits upside down and lifted: its origin is 24 px up in ours, its Y and Z reversed. */
    private static final float MODEL_TOP = 24;
    private static final Field LAYERS = layersField();

    private VanillaLayers() {
    }

    private static Field layersField() {
        try {
            Field field = LivingEntityRenderer.class.getDeclaredField("layers");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("LivingEntityRenderer.layers not found", e);
        }
    }

    /**
     * @param poseStack our model space in blocks (turned, scaled by 15/16, feet at the origin)
     * @param joints    each joint's model matrix in pixels
     */
    @SuppressWarnings("unchecked")
    static void render(PlayerRenderer renderer, AbstractClientPlayer player, Armature armature, Matrix4f[] joints,
                       PoseStack poseStack, MultiBufferSource buffers, int light, float partialTick, float headYaw,
                       float headPitch) {
        List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers;
        try {
            layers = (List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>>) LAYERS.get(renderer);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        Map<ModelPart, String> follows = Map.ofEntries(
                Map.entry(model.head, "Head"), Map.entry(model.hat, "Head"),
                Map.entry(model.body, "Chest"), Map.entry(model.jacket, "Chest"),
                Map.entry(model.rightArm, "Arm_R"), Map.entry(model.rightSleeve, "Arm_R"),
                Map.entry(model.leftArm, "Arm_L"), Map.entry(model.leftSleeve, "Arm_L"),
                Map.entry(model.rightLeg, "Thigh_R"), Map.entry(model.rightPants, "Thigh_R"),
                Map.entry(model.leftLeg, "Thigh_L"), Map.entry(model.leftPants, "Thigh_L"));
        follows.forEach((part, joint) -> follow(part, moved(armature, joints, joint)));
        model.crouching = player.isCrouching();
        model.attackTime = player.getAttackAnim(partialTick);

        float limbSwing = player.walkAnimation.position(partialTick);
        float limbSwingAmount = Math.min(player.walkAnimation.speed(partialTick), 1);
        float age = player.tickCount + partialTick;
        Matrix4f chest = toVanilla(moved(armature, joints, "Chest"));
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(0, -1.501f, 0);
        for (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer : layers) {
            if (layer instanceof HumanoidArmorLayer<?, ?, ?> || layer instanceof ItemInHandLayer<?, ?>
                    || layer.getClass().getName().startsWith("com.murimblock.")) {
                continue;
            }
            boolean onChest = layer instanceof CapeLayer || layer instanceof ElytraLayer<?, ?>;
            poseStack.pushPose();
            if (onChest) {
                poseStack.mulPose(chest);
            }
            layer.render(poseStack, buffers, light, player, limbSwing, limbSwingAmount, partialTick, age, headYaw,
                    headPitch);
            poseStack.popPose();
        }
        poseStack.popPose();
        follows.keySet().forEach(ModelPart::resetPose);
    }

    /** How {@code joint} moved from rest to this pose, in our model space (pixels). */
    private static Matrix4f moved(Armature armature, Matrix4f[] joints, String joint) {
        int index = armature.get(joint).index();
        return new Matrix4f(joints[index]).mul(armature.inverseBind(index));
    }

    /** A vanilla part placed and turned as {@code moved} moves it from its rest place. */
    private static void follow(ModelPart part, Matrix4f moved) {
        PartPose rest = part.getInitialPose();
        Vector3f pivot = moved.transformPosition(new Vector3f(rest.x, MODEL_TOP - rest.y, -rest.z));
        part.x = pivot.x;
        part.y = MODEL_TOP - pivot.y;
        part.z = -pivot.z;
        // Our turn seen from vanilla's upside-down space: a half turn about X reverses Y and Z.
        Quaternionf ours = moved.getNormalizedRotation(new Quaternionf());
        Vector3f angles = new Quaternionf(ours.x, -ours.y, -ours.z, ours.w).getEulerAnglesZYX(new Vector3f());
        part.xRot = angles.x + rest.xRot;
        part.yRot = angles.y + rest.yRot;
        part.zRot = angles.z + rest.zRot;
    }

    /** {@code moved} (our space, pixels) as a transform of vanilla's model space (blocks). */
    private static Matrix4f toVanilla(Matrix4f moved) {
        // Vanilla's model space to ours in blocks: lift by 1.501, then a half turn about X.
        Matrix4f toOurs = new Matrix4f().rotateX((float) Math.PI).translate(0, -1.501f, 0);
        Matrix4f ourBlocks = new Matrix4f().scale(PIXEL).mul(moved).scale(16);
        return new Matrix4f(toOurs).invert().mul(ourBlocks).mul(toOurs);
    }
}
