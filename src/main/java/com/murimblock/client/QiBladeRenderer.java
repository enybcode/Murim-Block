package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.martial.QiBladeEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Thousand Drizzle's wave of Qi drawn as a great ghost of the sword (docs/combat/styles/azure-cloud-sword.md): a pale
 * azure blade about three blocks long, point first, its edges brighter, glowing in the dark (additive, full light), with
 * three fading afterimages behind it. It stretches out of nothing as it leaves and fades at the end of its flight; its
 * point is where it hits (the entity's position). The other blades of Qi (needle, crescent) are still drawn by their
 * specks.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiBladeRenderer extends EntityRenderer<QiBladeEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/qi_blade.png");
    /** The real sword's sizes (pixels) times this: the great ghost blade. */
    private static final float SCALE = 2.1F / 16;
    private static final float BLADE = 22 * SCALE, HEIGHT = 2.4F * SCALE, THICK = 0.9F * SCALE;
    /** Ticks to stretch out of nothing, and to fade at the end of the flight. */
    private static final float GROW = 2.5F, FADE = 2.5F;
    /** Afterimages: how far behind (blocks) and how bright, as a share. */
    private static final float[][] AFTERIMAGES = {{0.55F, 0.3F}, {1.1F, 0.12F}};

    public QiBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.QI_BLADE.get(), QiBladeRenderer::new);
    }

    @Override
    public ResourceLocation getTextureLocation(QiBladeEntity blade) {
        return TEXTURE;
    }

    @Override
    public void render(QiBladeEntity blade, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (blade.kind() != QiBladeEntity.WAVE) return;
        float age = blade.tickCount + partialTick;
        float grow = Mth.clamp(age / GROW, 0.05F, 1);
        float fade = Mth.clamp((QiBladeEntity.lifetime(QiBladeEntity.WAVE) - age) / FADE, 0, 1);
        if (fade <= 0) return;
        VertexConsumer out = buffers.getBuffer(RenderType.eyes(TEXTURE));
        pose.pushPose();
        // At the height of what it sweeps, turned to its flight: the model points along +z, its point on the entity.
        pose.translate(0, QiBladeEntity.sweptHeight(QiBladeEntity.WAVE) / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, blade.yRotO, blade.getYRot())));
        pose.scale(1, 1, grow);
        sword(pose, out, 0, fade);
        for (float[] a : AFTERIMAGES) sword(pose, out, -a[0], fade * a[1]);
        pose.popPose();
    }

    /** One ghost sword, its point at z = `at`, the rest behind it, at `bright` (0 to 1). */
    private static void sword(PoseStack pose, VertexConsumer out, float at, float bright) {
        // Blade: a clear azure, its two edges bright; the point tapers in three steps.
        float tip = 0.36F, bladeEnd = at - tip;
        box(pose, out, -THICK / 2, -HEIGHT / 2, at - BLADE, THICK / 2, HEIGHT / 2, bladeEnd, 0.3F, 0.6F, 0.95F, bright);
        box(pose, out, -THICK / 3, -HEIGHT / 2 - 0.01F, at - BLADE, THICK / 3, -HEIGHT / 2 + 0.04F, bladeEnd, 0.85F, 0.95F, 1, bright);
        box(pose, out, -THICK / 3, HEIGHT / 2 - 0.04F, at - BLADE, THICK / 3, HEIGHT / 2 + 0.01F, bladeEnd, 0.85F, 0.95F, 1, bright * 0.8F);
        for (int s = 0; s < 3; s++) {
            float h = HEIGHT * (0.7F - 0.22F * s) / 2, z0 = bladeEnd + tip * s / 3, z1 = bladeEnd + tip * (s + 1) / 3;
            box(pose, out, -THICK / 3, -h, z0, THICK / 3, h, z1, 0.55F, 0.8F, 1.0F, bright);
        }
        // A broad guard, then the grip and its pommel, dimmer.
        float g = at - BLADE;
        box(pose, out, -1.6F * SCALE, -3.2F * SCALE, g - 1.4F * SCALE, 1.6F * SCALE, 3.2F * SCALE, g, 0.45F, 0.72F, 1.0F, bright * 0.85F);
        box(pose, out, -0.7F * SCALE, -0.7F * SCALE, g - 6.4F * SCALE, 0.7F * SCALE, 0.7F * SCALE, g - 1.4F * SCALE, 0.25F, 0.45F, 0.8F, bright * 0.7F);
        box(pose, out, -1.1F * SCALE, -1.1F * SCALE, g - 7.6F * SCALE, 1.1F * SCALE, 1.1F * SCALE, g - 6.4F * SCALE, 0.45F, 0.72F, 1.0F, bright * 0.8F);
    }

    /** An axis-aligned box, every face lit full and glowing (an additive render: a dimmer colour is fainter). */
    private static void box(PoseStack pose, VertexConsumer out, float x0, float y0, float z0, float x1, float y1, float z1,
                            float r, float g, float b, float bright) {
        PoseStack.Pose p = pose.last();
        int cr = (int) (r * bright * 255), cg = (int) (g * bright * 255), cb = (int) (b * bright * 255);
        // -x, +x, -y, +y, -z, +z faces, counter-clockwise from outside.
        quad(p, out, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, cr, cg, cb);
        quad(p, out, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, cr, cg, cb);
        quad(p, out, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, cr, cg, cb);
        quad(p, out, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, cr, cg, cb);
        quad(p, out, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, cr, cg, cb);
        quad(p, out, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, cr, cg, cb);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer out, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz, int r, int g, int b) {
        vertex(p, out, ax, ay, az, 0, 1, nx, ny, nz, r, g, b);
        vertex(p, out, bx, by, bz, 1, 1, nx, ny, nz, r, g, b);
        vertex(p, out, cx, cy, cz, 1, 0, nx, ny, nz, r, g, b);
        vertex(p, out, dx, dy, dz, 0, 0, nx, ny, nz, r, g, b);
    }

    private static void vertex(PoseStack.Pose p, VertexConsumer out, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int r, int g, int b) {
        out.addVertex(p, x, y, z).setColor(r, g, b, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(p, nx, ny, nz);
    }
}
