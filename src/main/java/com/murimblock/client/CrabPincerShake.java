package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.mob.CrabPincerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The ground shaking under the Crab King's giant pincers (Enzo, 16/10): the view of a player near one trembles while
 * the ground cracks where it will come (its warning), hard as it bursts up, and again when it strikes down on its side;
 * the nearer, the harder. Each client by itself, from the pincers it sees and their age: no message.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CrabPincerShake {
    /** Felt up to this far (blocks). */
    private static final double FELT = 18;

    private CrabPincerShake() { }

    /** How hard the ground shakes for a pincer `t` ticks old (degrees, before distance). */
    static float strength(float t) {
        if (t < CrabPincerEntity.EMERGE) return 0.8F + 1.6F * t / CrabPincerEntity.EMERGE;
        if (t < CrabPincerEntity.EMERGE + 5) return 4.0F * (1 - (t - CrabPincerEntity.EMERGE) / 5);
        float s = t - CrabPincerEntity.STRIKE;
        if (s >= 0 && s < 6) return 3.0F * (1 - s / 6);
        return 0;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Vec3 eye = event.getCamera().getPosition();
        float partial = (float) event.getPartialTick(), amount = 0;
        for (CrabPincerEntity p : mc.level.getEntitiesOfClass(CrabPincerEntity.class, mc.player.getBoundingBox().inflate(FELT))) {
            double d = p.position().distanceTo(eye);
            if (d > FELT) continue;
            amount = Math.max(amount, strength(p.tickCount + partial) * (float) (1 - d / FELT));
        }
        if (amount <= 0.01F) return;
        double n = System.nanoTime() / 2.2e7;
        event.setRoll(event.getRoll() + amount * Mth.sin((float) n));
        event.setPitch(event.getPitch() + amount * 0.7F * Mth.cos((float) (n * 1.7)));
        event.setYaw(event.getYaw() + amount * 0.4F * Mth.sin((float) (n * 2.3)));
    }
}
