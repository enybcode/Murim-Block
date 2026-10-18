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
 * is open, the world is seen as Qi vision (QiVisionClient) and the edges of the view darken; the names of the auras read
 * are hidden, and before a far stronger one the view trembles under the pressure. The realms' seals that floated over
 * the auras are gone until Enzo reworks them (18/10/2026): the readings still come, for the pressure.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiSenseClient {
    private static boolean sentActive;
    private static boolean open;
    private static final Map<Integer, CultivationPayloads.Reading> READINGS = new HashMap<>();

    private QiSenseClient() {
    }


    public static void readings(CultivationPayloads.SenseReadings payload) {
        open = payload.open();
        READINGS.clear();
        for (CultivationPayloads.Reading r : payload.readings()) READINGS.put(r.entityId(), r);
    }

    /** What the server let us read of an aura (empty: nothing). */
    public static java.util.Optional<CultivationPayloads.Reading> reading(int entityId) {
        return java.util.Optional.ofNullable(READINGS.get(entityId));
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

    /**
     * The pressure of a stronger aura (Enzo, 18/10/2026: "si le mec est très puissant, l'écran tremble"): while the sense
     * is open, the view trembles before an aura above one's own, a little for one realm above, hard for one far above,
     * the harder the nearer (within the sense's far reach).
     */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float pressure = pressure();
        if (pressure <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        double t = mc.level.getGameTime() + event.getPartialTick();
        float jolt = (float) (Math.sin(t * 2.9) * Math.sin(t * 0.7 + 1.3));
        event.setRoll(event.getRoll() + (float) Math.sin(t * 1.7) * 1.6F * pressure + jolt * 0.8F * pressure);
        event.setPitch(event.getPitch() + (float) Math.sin(t * 2.3 + 0.5) * 0.9F * pressure);
        event.setYaw(event.getYaw() + (float) Math.cos(t * 1.9) * 0.9F * pressure);
    }

    /** How hard the strongest aura above one's own presses on the view now (0: none, 1: far above, close). */
    public static float pressure() {
        Minecraft mc = Minecraft.getInstance();
        if (!open || mc.level == null || mc.player == null) return 0;
        double far = CultivationRules.farSenseRange(com.murimblock.cultivation.CultivationService.getRealm(mc.player));
        float strongest = 0;
        for (CultivationPayloads.Reading r : READINGS.values()) {
            float weight = r.kind() == CultivationRules.Reading.ABYSS.ordinal() ? 1.0F : r.kind() == CultivationRules.Reading.VEILED.ordinal() ? 0.3F : 0;
            if (weight == 0) continue;
            Entity e = mc.level.getEntity(r.entityId());
            if (e == null) continue;
            float near = (float) Math.max(0, 1 - e.distanceTo(mc.player) / far);
            strongest = Math.max(strongest, weight * (0.25F + 0.75F * near * near));
        }
        return strongest;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        open = false;
        sentActive = false;
        READINGS.clear();
    }
}
