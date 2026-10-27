package com.murimblock.anim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.anim.Armature;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Runs the player renderer's layers (cape, elytra, arrows and stingers stuck in the body, head items, parrots, other
 * mods' layers) around our skinned body. They draw against the vanilla player model, so its parts are first moved to
 * follow our skeleton: each rigid vanilla part takes the turn and place of the joint it hangs from. The cape and the
 * elytra hang from the chest. Armor and held items we draw ourselves, except what only vanilla knows how to draw:
 * armor with a model of its own (other mods) and a spyglass held to the eye. Our own vanilla-model layers are left
 * out.
 */
final class VanillaLayers {
    private static final float PIXEL = 1 / 16f;
    /** Vanilla's model sits upside down and lifted: its origin is 24 px up in ours, its Y and Z reversed. */
    private static final float MODEL_TOP = 24;
    private static final Field LAYERS = layersField();
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.HEAD};
    private static final Method ARMOR_MODEL = method(HumanoidArmorLayer.class, "getArmorModel", EquipmentSlot.class);
    private static final Method RENDER_ARMOR_PIECE = method(HumanoidArmorLayer.class, "renderArmorPiece",
            PoseStack.class, MultiBufferSource.class, LivingEntity.class, EquipmentSlot.class, int.class,
            HumanoidModel.class, float.class, float.class, float.class, float.class, float.class, float.class);
    private static final Method RENDER_ARM_WITH_ITEM = method(ItemInHandLayer.class, "renderArmWithItem",
            LivingEntity.class, ItemStack.class, ItemDisplayContext.class, HumanoidArm.class, PoseStack.class,
            MultiBufferSource.class, int.class);

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
    private static List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers(
            PlayerRenderer renderer) {
        try {
            return (List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>>) LAYERS.get(renderer);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static void render(PlayerRenderer renderer, AbstractClientPlayer player, Armature armature, Matrix4f[] joints,
                       PoseStack poseStack, MultiBufferSource buffers, int light, float partialTick, float headYaw,
                       float headPitch) {
        List<RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>> layers = layers(renderer);
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
            if (layer instanceof HumanoidArmorLayer<?, ?, ?> armor) {
                // Our armor meshes draw vanilla-shaped armor; armor with a model of its own (other mods) draws here.
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    if (customArmor(armor, player, slot)) {
                        invoke(RENDER_ARMOR_PIECE, armor, poseStack, buffers, player, slot, light,
                                invoke(ARMOR_MODEL, armor, slot), limbSwing, limbSwingAmount, partialTick, age,
                                headYaw, headPitch);
                    }
                }
                continue;
            }
            if (layer instanceof ItemInHandLayer<?, ?>) {
                // We hold items at our grip joints; a spyglass in use is held at the eye, which vanilla draws.
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(hand);
                    if (atTheEye(player, stack)) {
                        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm()
                                : player.getMainArm().getOpposite();
                        invoke(RENDER_ARM_WITH_ITEM, layer, player, stack, arm == HumanoidArm.LEFT
                                        ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                                        : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, arm, poseStack, buffers, light);
                    }
                }
                continue;
            }
            if (layer.getClass().getName().startsWith("com.murimblock.")) {
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

    /** Whether {@code stack} is a spyglass held to the eye, which vanilla draws at the head instead of the hand. */
    static boolean atTheEye(AbstractClientPlayer player, ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SPYGLASS_SCOPE) && player.getUseItem() == stack
                && player.swingTime == 0;
    }

    /** Whether the armor in {@code slot} has a model of its own (other mods), drawn by vanilla's armor layer. */
    static boolean customArmor(PlayerRenderer renderer, AbstractClientPlayer player, EquipmentSlot slot) {
        for (RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> layer : layers(renderer)) {
            if (layer instanceof HumanoidArmorLayer<?, ?, ?> armor && customArmor(armor, player, slot)) {
                return true;
            }
        }
        return false;
    }

    private static boolean customArmor(HumanoidArmorLayer<?, ?, ?> layer, AbstractClientPlayer player,
                                       EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) {
            return false;
        }
        HumanoidModel<?> standard = (HumanoidModel<?>) invoke(ARMOR_MODEL, layer, slot);
        return ClientHooks.getArmorModel(player, stack, slot, standard) != standard;
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(owner.getSimpleName() + "." + name + " not found", e);
        }
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(method.getName(), e);
        }
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
