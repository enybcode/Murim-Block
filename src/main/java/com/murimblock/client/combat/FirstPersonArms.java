package com.murimblock.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.combat.CombatMove;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Both arms in front of the camera during a strike, from the player's own skin. Each arm hangs from a fixed shoulder
 * below and beside the eyes and reaches to its fist, placed by the move's first-person keys; a fist pulled in slides
 * the arm back past the shoulder, so a short arm stays a short arm.
 */
final class FirstPersonArms {
    /** Camera space, blocks: x right, y up, -z ahead. */
    private static final float SHOULDER_X = 0.36f;
    private static final float SHOULDER_Y = -0.42f;
    private static final float SHOULDER_Z = 0.15f;
    /** From the arm's pivot to the knuckles: 10 of the model's pixels. */
    private static final float ARM_LENGTH = 10 / 16f;
    /** The model's arm pivots, 5 px beside and 2 px below the neck; cancelled so the pivot sits where we place it. */
    private static final float PIVOT_X = 5 / 16f;
    private static final float PIVOT_Y = 2 / 16f;
    private static final Vector3f DOWN = new Vector3f(0, -1, 0);

    private FirstPersonArms() {
    }

    static void render(RenderHandEvent event, AbstractClientPlayer player, CombatMove.FirstPerson pose, float tick) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        arm(event, renderer, player, pose.left(), tick, -1);
        arm(event, renderer, player, pose.right(), tick, 1);
    }

    private static void arm(RenderHandEvent event, PlayerRenderer renderer, AbstractClientPlayer player,
                            List<CombatMove.Key> keys, float tick, int side) {
        Vec3 at = CombatMove.FirstPerson.fist(keys, tick);
        if (at == null) {
            return;
        }
        Vector3f fist = new Vector3f((float) at.x, (float) at.y, (float) at.z);
        Vector3f shoulder = new Vector3f(side * SHOULDER_X, SHOULDER_Y, SHOULDER_Z);
        Vector3f reach = new Vector3f(fist).sub(shoulder);
        if (reach.lengthSquared() < 1.0E-6f) {
            return;
        }
        Vector3f dir = reach.normalize();
        Vector3f pivot = new Vector3f(dir).mul(-ARM_LENGTH).add(fist);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(new Quaternionf().rotationTo(DOWN, dir));
        // The model stands upside down until turned half a circle, as every entity is drawn.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.translate(side * PIVOT_X, -PIVOT_Y, 0);
        if (side > 0) {
            renderer.renderRightHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            renderer.renderLeftHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        poseStack.popPose();
    }
}
