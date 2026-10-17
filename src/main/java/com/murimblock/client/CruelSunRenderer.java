package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.majin.CruelSunEntity;
import com.murimblock.network.SunFlashPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/**
 * A sun of the Majin Fist as seen: layers of light facing the camera, drawn additively, from a wide orange corona to a
 * white core, breathing; and a Photon light of its own that lights the arena round it (volumetric where Photon allows),
 * moving with it. The impacts' flashes (SunFlashPayload) are Photon lights too. Nothing of Photon if its lights are off.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CruelSunRenderer extends EntityRenderer<CruelSunEntity> {
    /** A soft round glow whose light falls off from its middle (additive drawing ignores alpha). */
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/sun_glow.png");
    /** The layers: size (times the sun's), red, green, blue (premultiplied: the light it adds). */
    private static final float[][] LAYERS = {
            {5.0F, 0.45F, 0.18F, 0.04F},
            {3.0F, 0.80F, 0.45F, 0.10F},
            {1.8F, 1.00F, 0.80F, 0.35F},
            {1.1F, 1.00F, 1.00F, 0.75F},
            {0.6F, 1.00F, 1.00F, 1.00F}};

    public CruelSunRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(CruelSunEntity sun) {
        return GLOW;
    }

    @Override protected int getBlockLightLevel(CruelSunEntity sun, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override public void render(CruelSunEntity sun, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = sun.tickCount + partial, size = sun.size() * (1 + 0.05F * Mth.sin(t * 0.3F)), fade = sun.fade();
        VertexConsumer out = buffers.getBuffer(RenderType.eyes(GLOW));
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        for (int i = 0; i < LAYERS.length; i++) {
            float[] l = LAYERS[i];
            float s = size * l[0] * (i == 0 ? 1 + 0.08F * Mth.sin(t * 0.11F) : 1);
            pose.pushPose();
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(t * (i % 2 == 0 ? 0.6F : -0.9F)));
            quad(out, pose.last().pose(), s, l[1] * fade, l[2] * fade, l[3] * fade);
            pose.popPose();
        }
        pose.popPose();
        super.render(sun, yaw, partial, pose, buffers, light);
    }

    private static void quad(VertexConsumer out, Matrix4f m, float size, float r, float g, float b) {
        float h = size / 2;
        vertex(out, m, -h, -h, 0, 1, r, g, b);
        vertex(out, m, h, -h, 1, 1, r, g, b);
        vertex(out, m, h, h, 1, 0, r, g, b);
        vertex(out, m, -h, h, 0, 0, r, g, b);
    }

    private static void vertex(VertexConsumer out, Matrix4f m, float x, float y, float u, float v, float r, float g, float b) {
        out.addVertex(m, x, y, 0).setColor(r, g, b, 1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.CRUEL_SUN.get(), CruelSunRenderer::new);
    }

    // ---------------------------------------------------------------- its light

    private static final Map<Integer, DynamicLight> LIGHTS = new HashMap<>();

    /** Each sun's Photon light, moved with it and sized with it; dropped when it goes. */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !PhotonLights.isEnabled()) {
            if (!LIGHTS.isEmpty()) { LIGHTS.values().forEach(PhotonLights::remove); LIGHTS.clear(); }
            return;
        }
        for (Iterator<Map.Entry<Integer, DynamicLight>> it = LIGHTS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, DynamicLight> e = it.next();
            Entity sun = mc.level.getEntity(e.getKey());
            if (!(sun instanceof CruelSunEntity) || sun.isRemoved()) { PhotonLights.remove(e.getValue()); it.remove(); }
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof CruelSunEntity sun)) continue;
            DynamicLight l = LIGHTS.computeIfAbsent(sun.getId(), id -> PhotonLights.add(new DynamicLight().point().color(1.0F, 0.72F, 0.32F)));
            float s = sun.size();
            l.at(sun.getX(), sun.getY(), sun.getZ()).intensity(1.5F + s * 0.6F).range(4 + s * 6).sourceRadius(s * 0.4F)
                    .volumetric(PhotonLights.isVolumetricEnabled() ? 0.6F : 0);
        }
    }

    /** An impact's flash of sunlight. */
    public static void flash(SunFlashPayload p) {
        if (!PhotonLights.isEnabled()) return;
        PhotonLights.flash(new DynamicLight().point().at(p.x(), p.y(), p.z()).color(1.0F, 0.78F, 0.4F)
                .intensity(p.intensity()).range(p.range()).sourceRadius(p.range() * 0.1F)
                .volumetric(PhotonLights.isVolumetricEnabled() ? 0.5F : 0), p.ticks());
    }
}
