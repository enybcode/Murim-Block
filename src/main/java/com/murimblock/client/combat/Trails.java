package com.murimblock.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.combat.Colliders;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.AttackData;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.Pose;
import com.murimblock.shenfa.client.PlayerAnimators;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Trails behind fists and blades while a strike's hit window is open (Epic Fight draws weapon trails from its
 * animations): each frame the striking hand's tip and base are placed in the world from the clip, and a fading
 * ribbon joins the last places. Players and humanoid mobs.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class Trails {
    /** How long a point of the ribbon lasts (milliseconds). */
    private static final long LIFE = 140;
    /** A trail starts a little before the window opens and ends a little after it closes (seconds). */
    private static final float EARLY = 0.05f, LATE = 0.05f;
    private static final Map<String, Deque<Point>> RIBBONS = new HashMap<>();

    private Trails() {
    }

    private record Point(Vec3 tip, Vec3 base, long at) {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = System.currentTimeMillis();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity body && body.distanceToSqr(mc.player) < 48 * 48) {
                sample(body, partialTick, now);
            }
        }
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = event.getPoseStack().last();
        for (Iterator<Map.Entry<String, Deque<Point>>> it = RIBBONS.entrySet().iterator(); it.hasNext(); ) {
            Deque<Point> ribbon = it.next().getValue();
            while (!ribbon.isEmpty() && now - ribbon.peekFirst().at() > LIFE) {
                ribbon.pollFirst();
            }
            if (ribbon.isEmpty()) {
                it.remove();
                continue;
            }
            draw(ribbon, consumer, pose, camera, now);
        }
        buffers.endBatch(RenderType.lightning());
    }

    /** Adds this frame's place of each striking hand of {@code body}, if a strike's window is open. */
    private static void sample(LivingEntity body, float partialTick, long now) {
        if (!(body instanceof AbstractClientPlayer player)) {
            return;
        }
        Animation animation = PlayerAnimators.playing(player, Animator.Priority.HIGH);
        float time = PlayerAnimators.time(player, Animator.Priority.HIGH);
        if (animation == null || animation.attack() == null || time < 0) {
            return;
        }
        time += partialTick * 0.05f;
        for (AttackData.Phase phase : animation.attack().phases()) {
            if (time < phase.activeFrom() - EARLY || time > phase.activeTo() + LATE) {
                continue;
            }
            boolean cut = phase.kind().equals("cut");
            Pose pose = animation.clip().sample(time, Biped.ARMATURE.bindPose());
            Biped.settleHelpers(pose);
            Matrix4f[] joints = pose.toModelMatrices(Biped.ARMATURE);
            for (String side : switch (phase.hand()) {
                case "left" -> new String[] {"L"};
                case "right" -> new String[] {"R"};
                case "both" -> new String[] {"L", "R"};
                default -> new String[0];
            }) {
                Matrix4f tool = joints[Biped.ARMATURE.get("Tool_" + side).index()];
                // A blade's ribbon runs from the grip to the point; a fist's is a short band across the knuckles.
                Vector3f tip = tool.transformPosition(cut ? new Vector3f(0, -2, 17) : new Vector3f(0, -2.5f, 0));
                Vector3f base = tool.transformPosition(cut ? new Vector3f(0, -1, 3) : new Vector3f(0, 1.5f, 0));
                String key = body.getId() + side;
                Deque<Point> ribbon = RIBBONS.computeIfAbsent(key, k -> new ArrayDeque<>());
                ribbon.addLast(new Point(world(body, tip, partialTick), world(body, base, partialTick), now));
                if (ribbon.size() > 24) {
                    ribbon.pollFirst();
                }
            }
        }
    }

    private static Vec3 world(LivingEntity body, Vector3f model, float partialTick) {
        float yaw = Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot);
        Vector3f at = new Vector3f(model).mul(Colliders.pixel(body)).rotateY((float) Math.toRadians(-yaw));
        return new Vec3(Mth.lerp(partialTick, body.xo, body.getX()) + at.x,
                Mth.lerp(partialTick, body.yo, body.getY()) + at.y, Mth.lerp(partialTick, body.zo, body.getZ()) + at.z);
    }

    private static void draw(Deque<Point> ribbon, VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera, long now) {
        Point previous = null;
        for (Point point : ribbon) {
            if (previous != null) {
                float a0 = 1 - (now - previous.at()) / (float) LIFE, a1 = 1 - (now - point.at()) / (float) LIFE;
                vertex(consumer, pose, previous.base(), camera, a0 * 0.15f);
                vertex(consumer, pose, previous.tip(), camera, a0 * 0.6f);
                vertex(consumer, pose, point.tip(), camera, a1 * 0.6f);
                vertex(consumer, pose, point.base(), camera, a1 * 0.15f);
                // The other face too: the ribbon is seen from both sides.
                vertex(consumer, pose, point.base(), camera, a1 * 0.15f);
                vertex(consumer, pose, point.tip(), camera, a1 * 0.6f);
                vertex(consumer, pose, previous.tip(), camera, a0 * 0.6f);
                vertex(consumer, pose, previous.base(), camera, a0 * 0.15f);
            }
            previous = point;
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, Vec3 camera, float alpha) {
        consumer.addVertex(pose, (float) (at.x - camera.x), (float) (at.y - camera.y), (float) (at.z - camera.z))
                .setColor(1, 0.97f, 0.88f, Math.max(0, alpha));
    }
}
