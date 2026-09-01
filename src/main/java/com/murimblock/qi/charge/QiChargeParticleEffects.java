package com.murimblock.qi.charge;

import com.murimblock.qi.QiService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public final class QiChargeParticleEffects {
    private QiChargeParticleEffects() {
    }

    public static void spawnWorldParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        RandomSource random = player.getRandom();
        double height = player.getBbHeight() + QiChargeVisuals.QI_CHARGE_WORLD_EXTRA_HEIGHT;
        double qiMax = QiService.getQiMax(player);
        int particleCount = computeWorldParticleCount(qiMax, random);
        int auraCount = Math.min(QiChargeVisuals.computeAuraParticleCount(qiMax), particleCount);
        int normalCount = particleCount - auraCount;
        double intensity = QiChargeVisuals.computeIntensityFromQiMax(qiMax);
        double upwardSpeedRange = QiChargeVisuals.QI_CHARGE_WORLD_UPWARD_SPEED_RANGE * (1.0 + intensity * 0.5);
        for (int i = 0; i < normalCount; i++) {
            double x = player.getX() + randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_RADIUS);
            double y = player.getY() + random.nextDouble() * height;
            double z = player.getZ() + randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_RADIUS);
            double xSpeed = randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_HORIZONTAL_DRIFT);
            double ySpeed = QiChargeVisuals.QI_CHARGE_WORLD_UPWARD_SPEED_MIN
                    + random.nextDouble() * upwardSpeedRange;
            double zSpeed = randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_HORIZONTAL_DRIFT);

            level.sendParticles(QiChargeVisuals.QI_DUST_PARTICLE, x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0);
        }

        double auraIntensity = QiChargeVisuals.computeAuraIntensityFromQiMax(qiMax);
        for (int i = 0; i < auraCount; i++) {
            spawnAuraParticle(level, player, random, auraIntensity);
        }
    }

    static boolean shouldSpawnWorldParticles(int tickCount) {
        return tickCount % QiChargeVisuals.QI_CHARGE_WORLD_INTERVAL_TICKS == 0;
    }

    static boolean shouldSpawnWorldParticles(int tickCount, double qiMax) {
        return tickCount % QiChargeVisuals.computeWorldIntervalTicks(qiMax) == 0;
    }

    static int computeWorldParticleCount(double qiMax, RandomSource random) {
        int baseCount = QiChargeVisuals.computeBaseWorldParticleCount(qiMax);
        int fluctuation = QiChargeVisuals.computeIntensityFromQiMax(qiMax) >= 0.25 ? random.nextInt(3) - 1 : 0;
        return QiChargeVisuals.clampParticleCount(baseCount + fluctuation);
    }

    private static void spawnAuraParticle(ServerLevel level, ServerPlayer player, RandomSource random, double auraIntensity) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = lerp(
                QiChargeVisuals.QI_CHARGE_AURA_RADIUS_INNER,
                QiChargeVisuals.QI_CHARGE_AURA_RADIUS_OUTER,
                random.nextDouble()
        );
        double horizontalX = Math.cos(angle);
        double horizontalZ = Math.sin(angle);
        double x = player.getX() + horizontalX * radius;
        double z = player.getZ() + horizontalZ * radius;

        double extraHeight = QiChargeVisuals.QI_CHARGE_AURA_HEAD_EXTRA_HEIGHT_MIN
                + random.nextDouble() * QiChargeVisuals.QI_CHARGE_AURA_HEAD_EXTRA_HEIGHT_RANGE;
        double yProgress = Math.pow(random.nextDouble(), 1.35);
        double y = player.getY() + yProgress * (player.getBbHeight() + extraHeight);

        double inwardSpeed = QiChargeVisuals.QI_CHARGE_AURA_INWARD_SPEED_MIN
                + random.nextDouble() * QiChargeVisuals.QI_CHARGE_AURA_INWARD_SPEED_RANGE * auraIntensity;
        double tangentialSpeed = randomSigned(random, QiChargeVisuals.QI_CHARGE_AURA_TANGENTIAL_SPEED * auraIntensity);
        double xSpeed = -horizontalX * inwardSpeed - horizontalZ * tangentialSpeed;
        double zSpeed = -horizontalZ * inwardSpeed + horizontalX * tangentialSpeed;
        double ySpeed = QiChargeVisuals.QI_CHARGE_AURA_UPWARD_SPEED_MIN
                + random.nextDouble() * QiChargeVisuals.QI_CHARGE_AURA_UPWARD_SPEED_RANGE;

        level.sendParticles(QiChargeVisuals.QI_DUST_PARTICLE, x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0);
    }

    private static double randomSigned(RandomSource random, double range) {
        return (random.nextDouble() * 2.0 - 1.0) * range;
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}
