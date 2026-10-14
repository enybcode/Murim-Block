package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.network.CultivationPayloads;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

/**
 * Qi Sense on the client (docs/cultivation/qi-cultivation.md): the key held asks the server to open the sense; while it
 * is open, the edges of the view darken and over each aura the server let us read floats its realm's seal, with four
 * dots for the stage (its name hidden meanwhile). Of a stronger cultivator only a smudged seal; of a far stronger one,
 * no seal but a dark swirl, and the view trembles under the pressure.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiSenseClient {
    private static final ResourceLocation[] SEALS = new ResourceLocation[9];
    private static final ResourceLocation VEILED = tex("seal_veiled"), ABYSS = tex("seal_abyss"), PIP = tex("pip");
    private static boolean sentActive;
    private static boolean open;
    private static final Map<Integer, CultivationPayloads.Reading> READINGS = new HashMap<>();

    static {
        for (int i = 0; i < SEALS.length; i++) SEALS[i] = tex("seal_" + (i + 1));
    }

    private QiSenseClient() {
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/realm/" + name + ".png");
    }

    public static void readings(CultivationPayloads.SenseReadings payload) {
        open = payload.open();
        READINGS.clear();
        for (CultivationPayloads.Reading r : payload.readings()) READINGS.put(r.entityId(), r);
    }

    public static boolean open() {
        return open;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean want = mc.player != null && mc.screen == null && MurimblockKeyMappings.QI_SENSE.isDown();
        if (want != sentActive && mc.getConnection() != null) {
            sentActive = want;
            PacketDistributor.sendToServer(new CultivationPayloads.Sense(want));
        }
        if (!want && open && mc.getConnection() == null) open = false;
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (open && READINGS.containsKey(event.getEntity().getId())) event.setCanRender(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!open || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || READINGS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = mc.level.getGameTime() + partial;
        for (CultivationPayloads.Reading r : READINGS.values()) {
            Entity e = mc.level.getEntity(r.entityId());
            if (e == null) continue;
            Vec3 at = e.getPosition(partial).add(0, e.getBbHeight() + 0.65, 0);
            CultivationRules.Reading kind = CultivationRules.Reading.values()[Math.max(0, Math.min(3, r.kind()))];
            pose.pushPose();
            pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
            pose.mulPose(camera.rotation());
            switch (kind) {
                case CLEAR, FUZZY -> {
                    float s = 0.32F;
                    quad(pose, buffers.getBuffer(RenderType.entityTranslucent(SEALS[Math.max(0, Math.min(8, r.realm()))])), s, 0, 1, 0, 1, 1.0F);
                    // The stage's four dots under the seal; uncertain within the same realm (they flicker).
                    for (int i = 0; i < 4; i++) {
                        boolean full = kind == CultivationRules.Reading.CLEAR ? i <= r.stage() : (int) (time / 6 + i) % 2 == 0;
                        pose.pushPose();
                        pose.translate((i - 1.5F) * 0.16F, -0.42F, 0);
                        quad(pose, buffers.getBuffer(RenderType.entityTranslucent(PIP)), 0.06F, full ? 0.5F : 0, full ? 1 : 0.5F, 0, 1,
                                kind == CultivationRules.Reading.CLEAR ? 1.0F : 0.6F);
                        pose.popPose();
                    }
                }
                case VEILED -> {
                    pose.translate(Mth.sin(time * 0.9F) * 0.03F, Mth.cos(time * 1.3F) * 0.03F, 0);
                    quad(pose, buffers.getBuffer(RenderType.entityTranslucent(VEILED)), 0.36F, 0, 1, 0, 1, 0.85F);
                }
                case ABYSS -> {
                    pose.translate(0, -0.3F, 0);
                    pose.mulPose(com.mojang.math.Axis.ZP.rotation(time * 0.05F));
                    quad(pose, buffers.getBuffer(RenderType.entityTranslucent(ABYSS)), 0.9F, 0, 1, 0, 1, 0.95F);
                }
            }
            pose.popPose();
        }
        buffers.endBatch();
    }

    /** A square facing the camera, `s` half-wide, from the texture's (u0..u1, v0..v1). */
    private static void quad(PoseStack pose, VertexConsumer vc, float s, float u0, float u1, float v0, float v1, float alpha) {
        Matrix4f m = pose.last().pose();
        PoseStack.Pose last = pose.last();
        vc.addVertex(m, -s, s, 0).setColor(1, 1, 1, alpha).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0, 0, 1);
        vc.addVertex(m, -s, -s, 0).setColor(1, 1, 1, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0, 0, 1);
        vc.addVertex(m, s, -s, 0).setColor(1, 1, 1, alpha).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0, 0, 1);
        vc.addVertex(m, s, s, 0).setColor(1, 1, 1, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0, 0, 1);
    }

    /** The sense open: the edges of the view darken to a deep blue. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!open) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight(), band = Math.max(24, h / 6);
        int edge = 0x70081428, clear = 0x00081428;
        g.fillGradient(0, 0, w, band, edge, clear);
        g.fillGradient(0, h - band, w, h, clear, edge);
        for (int i = 0; i < band; i += 2) {
            int a = (int) (0x70 * (1 - i / (float) band));
            g.fill(i, 0, i + 2, h, (a << 24) | 0x081428);
            g.fill(w - i - 2, 0, w - i, h, (a << 24) | 0x081428);
        }
    }

    /** A far stronger aura in front of us, near: the view trembles under its pressure. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (!open || mc.level == null || mc.player == null) return;
        for (CultivationPayloads.Reading r : READINGS.values()) {
            if (r.kind() != CultivationRules.Reading.ABYSS.ordinal()) continue;
            Entity e = mc.level.getEntity(r.entityId());
            if (e == null || e.distanceTo(mc.player) > 10) continue;
            double t = mc.level.getGameTime() + event.getPartialTick();
            event.setRoll(event.getRoll() + (float) Math.sin(t * 1.7) * 0.6F);
            return;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        open = false;
        sentActive = false;
        READINGS.clear();
    }
}
