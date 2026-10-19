package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.martial.KaguneState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The kagune's physics, like an animal's tail (Enzo, 18/10/2026: "the kagune has physics too, with the animation, like
 * an animal's tail"): when the player runs, it trails back; when he turns, it swings the other way; when he jumps or
 * falls, it lifts or drops; then it springs back, overshooting a little. Every joint of the tentacles and the centipedes
 * bends by the same small angle (KaguneModel), so the tips move the most. One under-damped spring per player and axis,
 * stepped once a tick on each client, smoothed between ticks.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KaguneTail {
    /** How hard the spring pulls back, and how much it is damped (per tick): a little under critical, so it swings. */
    private static final float STIFFNESS = 0.22F, DAMPING = 0.32F;
    /** Degrees per joint for each block per tick of forward speed, of rise, and for each degree per tick of turning. */
    private static final float PER_SPEED = 14, PER_RISE = 10, PER_TURN = 0.18F;
    /** The most a joint bends (degrees). */
    private static final float MAX = 4.5F;

    private static final class Spring {
        float yaw, yawSpeed, pitch, pitchSpeed, prevYaw, prevPitch, lastBodyYaw;
        boolean started;
    }

    private static final Map<UUID, Spring> SPRINGS = new HashMap<>();

    private KaguneTail() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) { SPRINGS.clear(); return; }
        SPRINGS.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        for (Player player : level.players()) {
            if (KaguneState.of(player).form() == KaguneState.Form.NONE) { SPRINGS.remove(player.getUUID()); continue; }
            var s = SPRINGS.computeIfAbsent(player.getUUID(), id -> new Spring());
            float bodyYaw = player.yBodyRot;
            if (!s.started) { s.lastBodyYaw = bodyYaw; s.started = true; }
            float turn = Mth.wrapDegrees(bodyYaw - s.lastBodyYaw);
            s.lastBodyYaw = bodyYaw;
            // How fast he goes forward (along his body's facing), and up.
            var motion = player.getDeltaMovement();
            double rad = Math.toRadians(bodyYaw);
            double forward = -Math.sin(rad) * motion.x + Math.cos(rad) * motion.z;
            float targetPitch = Mth.clamp((float) (forward * PER_SPEED - motion.y * PER_RISE), -MAX, MAX);
            float targetYaw = Mth.clamp(-turn * PER_TURN * 4, -MAX, MAX);
            s.prevYaw = s.yaw;
            s.prevPitch = s.pitch;
            s.yawSpeed += (targetYaw - s.yaw) * STIFFNESS - s.yawSpeed * DAMPING;
            s.yaw += s.yawSpeed;
            s.pitchSpeed += (targetPitch - s.pitch) * STIFFNESS - s.pitchSpeed * DAMPING;
            s.pitch += s.pitchSpeed;
        }
    }

    /** The bend per joint now (degrees: yaw, pitch), between the last two ticks. */
    public static float[] bend(Player player, float partial) {
        var s = SPRINGS.get(player.getUUID());
        if (s == null) return new float[]{0, 0};
        return new float[]{Mth.lerp(partial, s.prevYaw, s.yaw), Mth.lerp(partial, s.prevPitch, s.pitch)};
    }
}
