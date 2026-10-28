package com.murimblock.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Lock-on (Epic Fight's, range 20): V locks the camera on the foe nearest the crosshair; the view turns to keep it in
 * the middle and the body strafes round it with the movement keys; a quick flick of the mouse jumps to the next foe
 * that way; when the foe dies the lock moves to the nearest other, and it lets go when the foe is lost from sight or
 * too far. A marker turns above the locked foe.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class LockOn {
    private static final double RANGE = 20, KEEP = 26;
    /** A flick of the mouse: this many degrees of turn within {@link #FLICK_NANOS} switches target. */
    private static final float FLICK = 10;
    private static final long FLICK_NANOS = 150_000_000L, SWITCH_PAUSE_NANOS = 300_000_000L;
    /** How long the foe may stay hidden behind a wall (ticks) before the lock lets go. */
    private static final int HIDDEN = 40;
    private static LivingEntity target;
    private static int hidden;
    /** The mouse's turn (degrees) since {@link #sweepStart}, and when the target last switched. */
    private static float sweep;
    private static long sweepStart, switched;

    private LockOn() {
    }

    public static LivingEntity target() {
        return target;
    }

    /** Locks on the best foe in sight, or lets go if locked. */
    public static void toggle() {
        target = target == null ? pick(null, 0) : null;
        sweep = 0;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        while (CombatClient.LOCK_ON.consumeClick()) {
            if (mc.screen == null && player != null) {
                toggle();
            }
        }
        if (target == null || player == null) {
            return;
        }
        if (!target.isAlive() || target.isRemoved()) {
            // The foe fell: the lock moves to the nearest other.
            target = pick(null, 0);
            return;
        }
        hidden = seen(player, target) ? 0 : hidden + 1;
        if (player.distanceTo(target) > KEEP || hidden > HIDDEN) {
            target = null;
        }
    }

    /**
     * Called by the mouse handler with the turn the mouse asks for (degrees, positive: to the right), before it is
     * applied. Only the mouse counts: a turn made by a strike or by the lock itself never switches foe.
     */
    public static void mouse(float degrees) {
        if (target == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - sweepStart > FLICK_NANOS) {
            sweep = 0;
            sweepStart = now;
        }
        sweep += degrees;
        if (Math.abs(sweep) > FLICK && now - switched > SWITCH_PAUSE_NANOS) {
            LivingEntity next = pick(target, sweep > 0 ? 1 : -1);
            if (next != null) {
                target = next;
                switched = now;
            }
            sweep = 0;
        }
    }

    /** Each frame the view turns toward the foe; the mouse only switches foe ({@link #mouse}). */
    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (target == null || player == null || mc.isPaused()) {
            return;
        }
        float frames = Math.min(5, mc.getTimer().getRealtimeDeltaTicks() * 3);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 at = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.6, 0);
        Vec3 toward = at.subtract(eye);
        float yaw = (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90;
        float pitch = (float) -(Mth.atan2(toward.y, toward.horizontalDistance()) * Mth.RAD_TO_DEG);
        float follow = 1 - (float) Math.pow(1 - 0.3, frames);
        float newYaw = player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()) * follow;
        float newPitch = Mth.clamp(player.getXRot() + (pitch + 8 - player.getXRot()) * follow, -60, 60);
        player.setYRot(newYaw);
        player.setXRot(newPitch);
        player.yRotO = newYaw;
        player.xRotO = newPitch;
    }

    /**
     * The best foe: the one nearest the crosshair (angle, then distance), in sight and in range; with a side, the
     * nearest on that side of {@code from} (positive: to the right).
     */
    private static LivingEntity pick(LivingEntity from, int side) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        List<LivingEntity> foes = mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(RANGE),
                entity -> entity != player && entity != from && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof OwnableEntity pet && pet.getOwner() == player)
                        && (entity instanceof Enemy || entity instanceof Player || entity == CombatClient.lastStruck())
                        && entity.distanceTo(player) <= RANGE && seen(player, entity));
        Comparator<LivingEntity> byAngle = Comparator.comparingDouble(entity -> -look.dot(
                entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(eye).normalize()));
        if (side != 0 && from != null) {
            float fromYaw = yawTo(player, from);
            return foes.stream().filter(entity -> Math.signum(Mth.wrapDegrees(yawTo(player, entity) - fromYaw)) == side)
                    .min(Comparator.comparingDouble(entity -> Math.abs(Mth.wrapDegrees(yawTo(player, entity) - fromYaw))))
                    .orElse(null);
        }
        return foes.stream().filter(entity -> look.dot(entity.position().subtract(eye).normalize()) > 0.3)
                .min(byAngle.thenComparingDouble(entity -> entity.distanceTo(player))).orElse(null);
    }

    private static float yawTo(Entity viewer, Entity entity) {
        return (float) (Mth.atan2(entity.getZ() - viewer.getZ(), entity.getX() - viewer.getX()) * Mth.RAD_TO_DEG) - 90;
    }

    private static boolean seen(Entity viewer, Entity entity) {
        Vec3 from = viewer.getEyePosition();
        Vec3 to = entity.position().add(0, entity.getBbHeight() * 0.6, 0);
        return viewer.level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer))
                .getType() == HitResult.Type.MISS;
    }

    /** A turning diamond above the locked foe. */
    @SubscribeEvent
    public static void marker(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || target == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 at = target.getPosition(partialTick).add(0, target.getBbHeight() + 0.45, 0).subtract(camera);
        float turn = (mc.level.getGameTime() + partialTick) * 0.15f;
        float size = 0.18f;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose pose = event.getPoseStack().last();
        Vec3[] corners = {new Vec3(0, size, 0), new Vec3(Mth.cos(turn) * size, 0, Mth.sin(turn) * size),
                new Vec3(0, -size, 0), new Vec3(-Mth.cos(turn) * size, 0, -Mth.sin(turn) * size)};
        for (int i = 0; i < 4; i++) {
            Vec3 a = at.add(corners[i]), b = at.add(corners[(i + 1) % 4]);
            Vec3 normal = b.subtract(a).normalize();
            lines.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(1f, 0.25f, 0.2f, 1f)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
            lines.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(1f, 0.25f, 0.2f, 1f)
                    .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        }
        buffers.endBatch(RenderType.lines());
    }
}
