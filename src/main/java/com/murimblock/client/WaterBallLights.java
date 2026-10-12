package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.murimblock.mob.WaterBallEntity;

/**
 * Photon dynamic light for the water ball: a short blue-white flash where it bursts, lighting the blocks and
 * entities around it, fading quadratically (Photon's flash). Client only; does nothing when Photon's dynamic
 * lights are turned off in its config.
 */
public final class WaterBallLights {
    /** Big ball: colour, intensity, range in blocks, duration in ticks. Small bubbles: a smaller, shorter flash. */
    static final float BIG_INTENSITY = 24.0F, BIG_RANGE = 8.0F, SMALL_INTENSITY = 6.0F, SMALL_RANGE = 3.5F;
    static final int BIG_TICKS = 12, SMALL_TICKS = 7;

    private WaterBallLights() { }

    public static void burst(WaterBallEntity ball) {
        if (!PhotonLights.isEnabled()) return;
        boolean big = ball.isBig();
        DynamicLight light = new DynamicLight().point()
                .at(ball.getX(), ball.getY() + ball.getBbHeight() / 2, ball.getZ())
                .color(0.55F, 0.82F, 1.0F)
                .intensity(big ? BIG_INTENSITY : SMALL_INTENSITY)
                .range(big ? BIG_RANGE : SMALL_RANGE)
                .sourceRadius(0.3F * ball.ballScale());
        PhotonLights.flash(light, big ? BIG_TICKS : SMALL_TICKS);
    }
}
