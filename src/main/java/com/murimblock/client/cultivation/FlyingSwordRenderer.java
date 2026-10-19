package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * The sword under a flyer's feet (SwordFlight), Enzo 19/10/2026: "travaille avec les épées du mod": the sword they fly
 * on, its own 3D model (the model as drawn, without any hand's turn), laid flat, its point ahead, lit by its Qi, a soft
 * glow of the aura under it. Drawn in the flyer's frame: forward +z, up +y.
 */
final class FlyingSwordRenderer {
    /** How big the sword is drawn: a little over its size in hand. */
    static final float SCALE = 1.5F;

    /**
     * How far each sword's model is turned in its own space (degrees, about its depth): most are drawn diagonal, as
     * Minecraft's swords are; the Seven Stars florett is drawn straight.
     */
    static float turn(ItemStack sword) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sword.getItem());
        return id.getPath().equals("seven_stars_florett") ? 0 : 45;
    }

    private FlyingSwordRenderer() {
    }

    /** Draws it; `pose` is at the flyer's feet, turned their way. */
    static void render(Minecraft mc, ItemStack sword, PoseStack pose, MultiBufferSource buffers, float[] c, float time) {
        float beat = 0.85F + 0.15F * (float) Math.sin(time * 0.25F);
        pose.pushPose();
        // The models stand their blade up (+y) in their block of space: laid down along the way (+y to +z), the broad of
        // the blade flat, the feet a little behind its middle.
        pose.translate(0, 0.0, -0.35);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(SCALE, SCALE, SCALE);
        // Its diagonal straightened along the way.
        pose.mulPose(Axis.ZP.rotationDegrees(turn(sword)));
        mc.getItemRenderer().renderStatic(sword, ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
        pose.popPose();
        // The aura's glow under it, the blade's length.
        Matrix4f m = pose.last().pose();
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        blade(glow, m, -1.35F, 0.95F, 0.22F, -0.06F, c[0], c[1], c[2], 0.3F * beat);
        blade(glow, m, -1.45F, 1.1F, 0.34F, -0.07F, c[0], c[1], c[2], 0.14F * beat);
    }

    /** A flat blade of light: a long shape on the plane y, `width` across, from z0 to its point at z1. */
    static void blade(VertexConsumer v, Matrix4f m, float z0, float z1, float width, float y, float r, float g, float b, float a) {
        float h = width / 2, shoulder = z1 - width * 1.6F;
        quad(v, m, -h, y, z0, h, y, z0, h, y, shoulder, -h, y, shoulder, r, g, b, a);
        quad(v, m, -h, y, shoulder, h, y, shoulder, 0, y, z1, 0, y, z1, r, g, b, a);
    }

    /** A quad seen from both sides. */
    static void quad(VertexConsumer v, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz,
                     float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b, float a) {
        v.addVertex(m, ax, ay, az).setColor(r, g, b, a);
        v.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        v.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        v.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
        v.addVertex(m, dx, dy, dz).setColor(r, g, b, a);
        v.addVertex(m, cx, cy, cz).setColor(r, g, b, a);
        v.addVertex(m, bx, by, bz).setColor(r, g, b, a);
        v.addVertex(m, ax, ay, az).setColor(r, g, b, a);
    }
}
