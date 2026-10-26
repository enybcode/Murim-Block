package com.murimblock.anim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.anim.Armature;
import com.murimblock.anim.BipedPoses;
import com.murimblock.anim.Joint;
import com.murimblock.anim.Pose;
import com.murimblock.anim.SkinnedMesh;
import com.murimblock.anim.SkinnedModel;
import com.murimblock.anim.Skinning;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws players with the skinned biped instead of the vanilla model while {@link SkeletalPlayers} says so: the body
 * and outer layer from the player's skin, armor ({@link SkeletalArmor}), held items at the grip joints, the vanilla
 * layers on a model following our skeleton ({@link VanillaLayers}) and the vanilla name tag. The shadow, fire
 * and hitbox are still drawn by vanilla around us.
 * <p>
 * Placement matches the vanilla player exactly at rest: vanilla flips its model (Y down, facing -Z), scales players by
 * 15/16 and lifts them by 1.501 blocks; our model is Y up, facing +Z, in pixels, so it only needs the turn, the 15/16
 * and 0.001 blocks of lift.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkeletalPlayerRenderer {
    private static final float PIXEL = 1 / 16f;
    /** Vanilla draws every player at 15/16 of the model's size. */
    private static final float PLAYER_SCALE = 0.9375f;
    private static final Map<String, PlayerModelPart> OUTER_PARTS = Map.of("hat", PlayerModelPart.HAT,
            "jacket", PlayerModelPart.JACKET, "sleeve_l", PlayerModelPart.LEFT_SLEEVE,
            "sleeve_r", PlayerModelPart.RIGHT_SLEEVE, "pants_l", PlayerModelPart.LEFT_PANTS_LEG,
            "pants_r", PlayerModelPart.RIGHT_PANTS_LEG);
    /**
     * From each grip joint to where vanilla holds an item, worked out from vanilla's arm pivot (x ±5, y 22), its turn
     * (-90° about X, 180° about Y, which in our axes is 90° about X then 180° about Y) and its offset (±1, 2, -10 px).
     */
    private static final Matrix4f RIGHT_GRIP = new Matrix4f().translate(1 * PIXEL, 9 * PIXEL, 0)
            .rotateX((float) Math.toRadians(90)).rotateY((float) Math.toRadians(180))
            .translate(1 * PIXEL, 2 * PIXEL, -10 * PIXEL);
    private static final Matrix4f LEFT_GRIP = new Matrix4f().translate(-1 * PIXEL, 9 * PIXEL, 0)
            .rotateX((float) Math.toRadians(90)).rotateY((float) Math.toRadians(180))
            .translate(-1 * PIXEL, 2 * PIXEL, -10 * PIXEL);

    private static float[] positions = new float[0];
    private static float[] normals = new float[0];
    /** Time spent drawing players since {@link #resetTiming}, for the engine test. */
    private static long drawNanos;
    private static int draws;

    private SkeletalPlayerRenderer() {
    }

    public static void resetTiming() {
        drawNanos = 0;
        draws = 0;
    }

    /** Average time to draw one player since {@link #resetTiming}, in microseconds (0 if none was drawn). */
    public static double averageMicros() {
        return draws == 0 ? 0 : drawNanos / 1000.0 / draws;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !SkeletalPlayers.drawn(player)) {
            return;
        }
        PlayerSkin skin = player.getSkin();
        SkinnedModel model = SkeletalModels.get(skin.model() == PlayerSkin.Model.SLIM
                ? SkeletalModels.BIPED_SLIM : SkeletalModels.BIPED);
        if (model == null) {
            return;
        }
        event.setCanceled(true);
        long start = System.nanoTime();
        render(player, event.getRenderer(), model, skin, event.getPartialTick(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
        drawNanos += System.nanoTime() - start;
        draws++;
    }

    private static void render(AbstractClientPlayer player, PlayerRenderer renderer, SkinnedModel model,
                               PlayerSkin skin, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                               int light) {
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        Pose pose = SkeletalPlayers.pose(player, partialTick);
        BipedPoses.look(pose, Mth.wrapDegrees(headYaw - bodyYaw), pitch);
        Matrix4f[] skinMatrices = pose.toSkinMatrices(model.armature());
        Matrix4f[] jointMatrices = pose.toModelMatrices(model.armature());

        poseStack.pushPose();
        float scale = player.getScale();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        if (player.deathTime > 0) {
            // Vanilla's fall to the side when dying.
            float fall = Mth.sqrt((player.deathTime + partialTick - 1) / 20f * 1.6f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Math.min(fall, 1) * 90));
        }
        poseStack.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        // The vanilla layers (cape, elytra, arrows, head items, other mods' layers) draw in vanilla's model space.
        VanillaLayers.render(renderer, player, model.armature(), jointMatrices, poseStack, buffers, light, partialTick,
                Mth.wrapDegrees(headYaw - bodyYaw), pitch);
        poseStack.translate(0, 0.001f, 0);

        Minecraft mc = Minecraft.getInstance();
        // The debug skeleton view shows the bones alone: the body would hide them.
        boolean skeletonView = SkeletalPlayers.showJoints();
        boolean visible = !player.isInvisible() && !skeletonView;
        boolean ghost = player.isInvisible() && !skeletonView && mc.player != null && !player.isInvisibleTo(mc.player);
        RenderType type = visible ? RenderType.entityCutoutNoCull(skin.texture())
                : ghost ? RenderType.itemEntityTranslucentCull(skin.texture())
                : !skeletonView && mc.shouldEntityAppearGlowing(player) ? RenderType.outline(skin.texture()) : null;
        if (type != null) {
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0);
            int color = ghost ? 0x26FFFFFF : 0xFFFFFFFF;
            poseStack.pushPose();
            poseStack.scale(PIXEL, PIXEL, PIXEL);
            VertexConsumer consumer = buffers.getBuffer(type);
            for (SkinnedMesh part : model.parts()) {
                PlayerModelPart outer = OUTER_PARTS.get(part.name());
                if (part.armor() || part.outer() && outer != null && !player.isModelPartShown(outer)) {
                    continue;
                }
                draw(part, skinMatrices, poseStack.last(), consumer, color, light, overlay);
            }
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.scale(PIXEL, PIXEL, PIXEL);
        if (skeletonView) {
            drawJoints(model.armature(), jointMatrices, poseStack.last(), buffers);
        } else {
            SkeletalArmor.render(player, model, skinMatrices, poseStack.last(), buffers, light);
        }
        poseStack.popPose();

        holdItem(player, HumanoidArm.RIGHT, jointMatrices[model.armature().get("Tool_R").index()], poseStack, buffers, light);
        holdItem(player, HumanoidArm.LEFT, jointMatrices[model.armature().get("Tool_L").index()], poseStack, buffers, light);
        poseStack.popPose();

        NameTag.render(renderer, player, poseStack, buffers, light, partialTick);
    }

    /**
     * Vanilla's name tag (with the score below it), through the renderer's protected methods: they are overridden
     * as protected in vanilla subclasses, so an access transformer cannot open them.
     */
    private static final class NameTag {
        private static final Method SHOULD_SHOW = method("shouldShowName", Entity.class);
        private static final Method RENDER = method("renderNameTag", Entity.class, Component.class, PoseStack.class,
                MultiBufferSource.class, int.class, float.class);

        private static Method method(String name, Class<?>... parameters) {
            try {
                Method method = EntityRenderer.class.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("EntityRenderer." + name + " not found", e);
            }
        }

        static void render(PlayerRenderer renderer, AbstractClientPlayer player, PoseStack poseStack,
                           MultiBufferSource buffers, int light, float partialTick) {
            try {
                if ((boolean) SHOULD_SHOW.invoke(renderer, player)) {
                    RENDER.invoke(renderer, player, player.getDisplayName(), poseStack, buffers, light, partialTick);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("name tag", e);
            }
        }
    }

    /** The debug skeleton view: each joint's axes (X red, Y green, Z blue) and a white line to its parent. */
    private static void drawJoints(Armature armature, Matrix4f[] joints, PoseStack.Pose matrix, MultiBufferSource buffers) {
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Joint joint : armature.joints()) {
            Matrix4f m = joints[joint.index()];
            Vector3f at = m.getTranslation(new Vector3f());
            line(lines, matrix, at, m.transformPosition(new Vector3f(3, 0, 0)), 0xFFFF4040);
            line(lines, matrix, at, m.transformPosition(new Vector3f(0, 3, 0)), 0xFF40FF40);
            line(lines, matrix, at, m.transformPosition(new Vector3f(0, 0, 3)), 0xFF4080FF);
            if (!joint.isRoot()) {
                line(lines, matrix, at, joints[joint.parent()].getTranslation(new Vector3f()), 0xFFFFFFFF);
            }
        }
    }

    private static void line(VertexConsumer lines, PoseStack.Pose matrix, Vector3f from, Vector3f to, int color) {
        Vector3f normal = new Vector3f(to).sub(from);
        if (normal.lengthSquared() < 1e-6f) {
            return;
        }
        normal.normalize();
        lines.addVertex(matrix, from.x, from.y, from.z).setColor(color).setNormal(matrix, normal.x, normal.y, normal.z);
        lines.addVertex(matrix, to.x, to.y, to.z).setColor(color).setNormal(matrix, normal.x, normal.y, normal.z);
    }

    static void draw(SkinnedMesh part, Matrix4f[] skin, PoseStack.Pose matrix, VertexConsumer consumer,
                     int color, int light, int overlay) {
        int count = part.vertexCount() * 3;
        if (positions.length < count) {
            positions = new float[count];
            normals = new float[count];
        }
        Skinning.skin(part, skin, positions, normals);
        float[] uvs = part.uvs();
        int[] indices = part.indices();
        for (int t = 0; t < indices.length; t += 3) {
            // Entity render types draw quads: each triangle is sent as a quad with its last corner twice.
            for (int corner = 0; corner < 4; corner++) {
                int v = indices[t + Math.min(corner, 2)];
                consumer.addVertex(matrix, positions[v * 3], positions[v * 3 + 1], positions[v * 3 + 2])
                        .setColor(color)
                        .setUv(uvs[v * 2], uvs[v * 2 + 1])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(matrix, normals[v * 3], normals[v * 3 + 1], normals[v * 3 + 2]);
            }
        }
    }

    private static void holdItem(AbstractClientPlayer player, HumanoidArm arm, Matrix4f tool, PoseStack poseStack,
                                 MultiBufferSource buffers, int light) {
        ItemStack stack = player.getMainArm() == arm ? player.getMainHandItem() : player.getOffhandItem();
        if (stack.isEmpty()) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        poseStack.pushPose();
        // The joint's matrix is in pixels; the item renderer works in blocks.
        Matrix4f joint = new Matrix4f().scale(PIXEL).mul(tool).scale(16);
        poseStack.mulPose(joint.mul(left ? LEFT_GRIP : RIGHT_GRIP));
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, poseStack, buffers, light);
        poseStack.popPose();
    }
}
