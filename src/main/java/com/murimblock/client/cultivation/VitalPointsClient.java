package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.VitalPoints;
import com.murimblock.network.CultivationPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The vital points lit on a body (VitalPoints): small beating stars of jade Qi on its front, for as long as they stay lit;
 * one struck bursts. Shown to the player who parried only.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class VitalPointsClient {
    /** Jade: apart from an aura's colour and from the foresight's gold, and coloured enough to keep in Qi vision. */
    static final float[] JADE = {0.1F, 1.0F, 0.7F};

    private static int entityId = -1, mask;
    private static long until;

    private VitalPointsClient() {
    }

    public static void receive(CultivationPayloads.Vital payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (payload.struck() >= 0 && mc.level.getEntity(payload.entityId()) instanceof LivingEntity e) burst(mc, e, payload.struck());
        entityId = payload.entityId();
        mask = payload.mask();
        until = mc.level.getGameTime() + payload.ticks();
    }

    /** The lit points (a bit each), for the tours. */
    public static int lit() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || mc.level.getGameTime() > until ? 0 : mask;
    }

    /** Whether this body has points lit. */
    static boolean on(LivingEntity e) {
        return e.getId() == entityId && lit() != 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || lit() == 0) return;
        if (!(mc.level.getEntity(entityId) instanceof LivingEntity e) || !e.isAlive()) {
            mask = 0;
            return;
        }
        long time = mc.level.getGameTime();
        // Beating, faster as they are about to go out.
        float left = (until - time) / (float) VitalPoints.WINDOW_TICKS;
        float beat = 0.5F + 0.5F * (float) Math.sin(time * (left < 0.3F ? 1.6F : 0.8F));
        RandomSource random = mc.level.random;
        for (int i = 0; i < VitalPoints.POINTS.length; i++) {
            if ((mask & 1 << i) == 0) continue;
            // Just before the body, so that nothing of it hides the point.
            Vec3 p = VitalPoints.position(e, i).add(Vec3.directionFromRotation(0, e.yBodyRot).scale(0.12));
            AuraParticle core = AuraClient.spark(mc.level, p.x, p.y, p.z, 0, 0, 0, JADE, false);
            if (core != null) core.big(0.16F + 0.1F * beat, 2);
            // Its four rays, turning slowly: a star.
            for (int r = 0; r < 4; r++) {
                double a = time * 0.12 + r * Math.PI / 2, d = 0.16 + 0.05 * beat;
                Vec3 side = new Vec3(Math.cos(a) * d, Math.sin(a) * d, 0).yRot(-e.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD);
                AuraParticle ray = AuraClient.spark(mc.level, p.x + side.x, p.y + side.y, p.z + side.z, 0, 0, 0, JADE, false);
                if (ray != null) ray.big(0.06F, 2);
            }
            // A few specks drawn into it, so that it reads as Qi, not a light.
            if (random.nextInt(3) == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                Vec3 from = p.add(Math.cos(a) * 0.3, (random.nextDouble() - 0.5) * 0.3, Math.sin(a) * 0.3);
                Vec3 in = p.subtract(from).scale(0.25);
                AuraParticle s = AuraClient.spark(mc.level, from.x, from.y, from.z, in.x, in.y, in.z, JADE, false);
                if (s != null) s.big(0.04F, 4);
            }
        }
    }

    /** A point struck: its Qi bursts out. */
    static void burst(Minecraft mc, LivingEntity e, int index) {
        Vec3 p = VitalPoints.position(e, index);
        RandomSource random = mc.level.random;
        for (int i = 0; i < 24; i++) {
            double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI - Math.PI / 2;
            AuraParticle s = AuraClient.spark(mc.level, p.x, p.y, p.z,
                    Math.cos(a) * Math.cos(b) * 0.22, Math.sin(b) * 0.22, Math.sin(a) * Math.cos(b) * 0.22, JADE, false);
            if (s != null) s.big(0.2F, 10);
        }
    }
}
