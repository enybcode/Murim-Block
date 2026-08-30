package com.murimblock.qi.charge;

import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

public final class QiChargeVisuals {
    public static final Vector3f QI_PARTICLE_COLOR = new Vector3f(0.15F, 0.55F, 1.0F);
    public static final float QI_CHARGE_PARTICLE_SCALE = 0.65F;
    public static final DustParticleOptions QI_DUST_PARTICLE = new DustParticleOptions(
            new Vector3f(QI_PARTICLE_COLOR),
            QI_CHARGE_PARTICLE_SCALE
    );

    public static final int QI_CHARGE_WORLD_INTERVAL_TICKS = 5;
    public static final int QI_CHARGE_WORLD_COUNT = 3;
    public static final double QI_CHARGE_WORLD_RADIUS = 0.7;
    public static final double QI_CHARGE_WORLD_EXTRA_HEIGHT = 0.25;
    public static final double QI_CHARGE_WORLD_UPWARD_SPEED_MIN = 0.025;
    public static final double QI_CHARGE_WORLD_UPWARD_SPEED_RANGE = 0.025;
    public static final double QI_CHARGE_WORLD_HORIZONTAL_DRIFT = 0.012;

    public static final int QI_CHARGE_FOREGROUND_INTERVAL_MIN_TICKS = 8;
    public static final int QI_CHARGE_FOREGROUND_INTERVAL_RANGE_TICKS = 8;
    public static final double QI_CHARGE_FOREGROUND_DISTANCE_MIN = 0.4;
    public static final double QI_CHARGE_FOREGROUND_DISTANCE_RANGE = 0.8;
    public static final double QI_CHARGE_FOREGROUND_HORIZONTAL_MIN = 0.22;
    public static final double QI_CHARGE_FOREGROUND_HORIZONTAL_RANGE = 0.48;
    public static final double QI_CHARGE_FOREGROUND_VERTICAL_MIN = -0.42;
    public static final double QI_CHARGE_FOREGROUND_VERTICAL_RANGE = 0.36;
    public static final double QI_CHARGE_FOREGROUND_UPWARD_SPEED = 0.025;
    public static final double QI_CHARGE_FOREGROUND_HORIZONTAL_DRIFT = 0.01;

    public static final float QI_CHARGE_FOV_MULTIPLIER = 0.95F;
    public static final float QI_CHARGE_FOV_TRANSITION_STEP = 1.0F / 10.0F;

    private QiChargeVisuals() {
    }
}
