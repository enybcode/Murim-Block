package com.murimblock.qi.charge;

import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

public final class QiChargeVisuals {
    public static final double[] QI_MAX_INTENSITY_THRESHOLDS = {
            100.0,
            250.0,
            500.0,
            1_000.0,
            2_500.0,
            5_000.0,
            10_000.0
    };
    private static final double[] QI_MAX_INTENSITY_VALUES = {
            0.0,
            0.16,
            0.32,
            0.5,
            0.68,
            0.84,
            1.0
    };
    private static final int[] QI_MAX_WORLD_PARTICLE_COUNTS = {
            2,
            2,
            3,
            4,
            5,
            6,
            8
    };

    public static final Vector3f QI_PARTICLE_COLOR = new Vector3f(0.15F, 0.55F, 1.0F);
    public static final float QI_CHARGE_PARTICLE_SCALE = 0.65F;
    public static final DustParticleOptions QI_DUST_PARTICLE = new DustParticleOptions(
            new Vector3f(QI_PARTICLE_COLOR),
            QI_CHARGE_PARTICLE_SCALE
    );

    public static final int QI_CHARGE_WORLD_INTERVAL_TICKS = 5;
    public static final int MIN_QI_CHARGE_WORLD_PARTICLES = 2;
    public static final int MAX_QI_CHARGE_WORLD_PARTICLES = 8;
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

    public static final float QI_CHARGE_FOV_MULTIPLIER = 1.04F;
    public static final float QI_CHARGE_FOV_TRANSITION_STEP = 1.0F / 10.0F;

    private QiChargeVisuals() {
    }

    public static double computeIntensityFromQiMax(double qiMax) {
        if (!Double.isFinite(qiMax) || qiMax <= QI_MAX_INTENSITY_THRESHOLDS[0]) {
            return 0.0;
        }

        int lastIndex = QI_MAX_INTENSITY_THRESHOLDS.length - 1;
        if (qiMax >= QI_MAX_INTENSITY_THRESHOLDS[lastIndex]) {
            return 1.0;
        }

        for (int i = 1; i < QI_MAX_INTENSITY_THRESHOLDS.length; i++) {
            double upperThreshold = QI_MAX_INTENSITY_THRESHOLDS[i];
            if (qiMax <= upperThreshold) {
                double lowerThreshold = QI_MAX_INTENSITY_THRESHOLDS[i - 1];
                double localProgress = (qiMax - lowerThreshold) / (upperThreshold - lowerThreshold);
                double lowerIntensity = QI_MAX_INTENSITY_VALUES[i - 1];
                double upperIntensity = QI_MAX_INTENSITY_VALUES[i];
                return lowerIntensity + (upperIntensity - lowerIntensity) * localProgress;
            }
        }

        return 1.0;
    }

    public static int computeBaseWorldParticleCount(double qiMax) {
        if (!Double.isFinite(qiMax) || qiMax <= QI_MAX_INTENSITY_THRESHOLDS[0]) {
            return MIN_QI_CHARGE_WORLD_PARTICLES;
        }

        int lastIndex = QI_MAX_INTENSITY_THRESHOLDS.length - 1;
        if (qiMax >= QI_MAX_INTENSITY_THRESHOLDS[lastIndex]) {
            return MAX_QI_CHARGE_WORLD_PARTICLES;
        }

        for (int i = 1; i < QI_MAX_INTENSITY_THRESHOLDS.length; i++) {
            double upperThreshold = QI_MAX_INTENSITY_THRESHOLDS[i];
            if (qiMax <= upperThreshold) {
                double lowerThreshold = QI_MAX_INTENSITY_THRESHOLDS[i - 1];
                double localProgress = (qiMax - lowerThreshold) / (upperThreshold - lowerThreshold);
                return clampParticleCount((int) Math.round(lerp(
                        QI_MAX_WORLD_PARTICLE_COUNTS[i - 1],
                        QI_MAX_WORLD_PARTICLE_COUNTS[i],
                        localProgress
                )));
            }
        }

        return MAX_QI_CHARGE_WORLD_PARTICLES;
    }

    public static int computeForegroundCooldownMin(double qiMax) {
        double intensity = computeIntensityFromQiMax(qiMax);
        return (int) Math.round(lerp(16.0, 7.0, intensity));
    }

    public static int computeForegroundCooldownMaxInclusive(double qiMax) {
        double intensity = computeIntensityFromQiMax(qiMax);
        return (int) Math.round(lerp(22.0, 12.0, intensity));
    }

    public static int clampParticleCount(int count) {
        return Math.max(MIN_QI_CHARGE_WORLD_PARTICLES, Math.min(MAX_QI_CHARGE_WORLD_PARTICLES, count));
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}
