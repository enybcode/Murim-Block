package com.murimblock.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.combat.CombatMove;
import com.murimblock.combat.Fighter;
import com.murimblock.combat.HitGeometry;
import com.murimblock.combat.HitShape;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The tuning view ({@code /murim debug hitboxes on}): our own strike's hit shape drawn around us, grey while it
 * winds up, red while it can hit, dark once it is spent; the boxes of living things nearby in white, and the ones
 * just struck in yellow.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class HitboxDebugView {
    private static final int FLASH_TICKS = 8;
    private static final double NEARBY = 8;
    private static boolean shown;
    /** Entities struck, with the ticks their box stays yellow. */
    private static final Map<Integer, Integer> STRUCK = new HashMap<>();

    private HitboxDebugView() {
    }

    public static void show(boolean on) {
        shown = on;
        STRUCK.clear();
    }

    public static boolean shown() {
        return shown;
    }

    static void flash(int entityId) {
        if (shown) {
            STRUCK.put(entityId, FLASH_TICKS);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        STRUCK.replaceAll((id, ticks) -> ticks - 1);
        STRUCK.values().removeIf(ticks -> ticks <= 0);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        show(false);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!shown || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || mc.player == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Entity entity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(NEARBY),
                e -> e instanceof LivingEntity)) {
            AABB box = entity.getBoundingBox();
            if (STRUCK.containsKey(entity.getId())) {
                LevelRenderer.renderLineBox(pose, lines, box.inflate(0.02), 1f, 0.85f, 0.1f, 1f);
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 1f, 1f, 1f, 0.5f);
            }
        }
        drawOwnShape(pose, lines, mc.player, partial);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static void drawOwnShape(PoseStack pose, VertexConsumer lines, LocalPlayer player, float partial) {
        Fighter own = ClientCombat.own();
        CombatMove move = own.move();
        if (move == null) {
            return;
        }
        Vec3 eyeVec = player.getEyePosition(partial);
        double[] eye = {eyeVec.x, eyeVec.y, eyeVec.z};
        float yaw = player.getViewYRot(partial), pitch = player.getViewXRot(partial);
        int index = own.clock().activeIndex(), count = own.clock().activeCount();
        boolean active = own.clock().active() || own.clock().frozen() && index >= 0 && index < count;
        float[] color = active ? new float[] {1f, 0.15f, 0.15f} : index < 0
                ? new float[] {0.7f, 0.7f, 0.7f} : new float[] {0.3f, 0.3f, 0.3f};
        HitShape shape = move.hitbox();
        if (!shape.isArc()) {
            double[][] c = HitGeometry.boxCorners(eye, yaw, pitch, shape);
            int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            for (int[] e : edges) {
                line(pose, lines, c[e[0]], c[e[1]], color);
            }
            return;
        }
        // The whole swing faintly, the slice being swept in colour.
        drawArc(pose, lines, eye, yaw + shape.from(), yaw + shape.to(), shape, new float[] {0.45f, 0.45f, 0.45f});
        if (active) {
            double[] slice = HitGeometry.arcSlice(shape, index, count);
            drawArc(pose, lines, eye, yaw + slice[0], yaw + slice[1], shape, color);
        }
    }

    private static void drawArc(PoseStack pose, VertexConsumer lines, double[] eye, double from, double to, HitShape s,
                                float[] color) {
        List<double[]> outline = HitGeometry.arcOutline(eye, from, to, s.radius());
        for (double y : new double[] {eye[1] - s.height() / 2, eye[1] + s.height() / 2}) {
            for (int i = 0; i < outline.size(); i++) {
                double[] a = outline.get(i), b = outline.get((i + 1) % outline.size());
                line(pose, lines, new double[] {a[0], y, a[1]}, new double[] {b[0], y, b[1]}, color);
            }
        }
        for (double[] p : List.of(outline.get(1), outline.get(outline.size() - 1))) {
            line(pose, lines, new double[] {p[0], eye[1] - s.height() / 2, p[1]},
                    new double[] {p[0], eye[1] + s.height() / 2, p[1]}, color);
        }
    }

    private static void line(PoseStack pose, VertexConsumer lines, double[] a, double[] b, float[] color) {
        float nx = (float) (b[0] - a[0]), ny = (float) (b[1] - a[1]), nz = (float) (b[2] - a[2]);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6f) {
            return;
        }
        PoseStack.Pose last = pose.last();
        lines.addVertex(last, (float) a[0], (float) a[1], (float) a[2]).setColor(color[0], color[1], color[2], 1f)
                .setNormal(last, nx / len, ny / len, nz / len);
        lines.addVertex(last, (float) b[0], (float) b[1], (float) b[2]).setColor(color[0], color[1], color[2], 1f)
                .setNormal(last, nx / len, ny / len, nz / len);
    }
}
