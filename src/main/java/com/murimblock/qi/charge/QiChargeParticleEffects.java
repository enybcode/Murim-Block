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
        double intensity = QiChargeVisuals.computeIntensityFromQiMax(qiMax);
        double upwardSpeedRange = QiChargeVisuals.QI_CHARGE_WORLD_UPWARD_SPEED_RANGE * (1.0 + intensity * 0.5);
        for (int i = 0; i < particleCount; i++) {
            double x = player.getX() + randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_RADIUS);
            double y = player.getY() + random.nextDouble() * height;
            double z = player.getZ() + randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_RADIUS);
            double xSpeed = randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_HORIZONTAL_DRIFT);
            double ySpeed = QiChargeVisuals.QI_CHARGE_WORLD_UPWARD_SPEED_MIN
                    + random.nextDouble() * upwardSpeedRange;
            double zSpeed = randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_HORIZONTAL_DRIFT);

            level.sendParticles(QiChargeVisuals.QI_DUST_PARTICLE, x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0);
        }
    }

    static boolean shouldSpawnWorldParticles(int tickCount) {
        return tickCount % QiChargeVisuals.QI_CHARGE_WORLD_INTERVAL_TICKS == 0;
    }

    static int computeWorldParticleCount(double qiMax, RandomSource random) {
        int baseCount = QiChargeVisuals.computeBaseWorldParticleCount(qiMax);
        int fluctuation = QiChargeVisuals.computeIntensityFromQiMax(qiMax) >= 0.25 ? random.nextInt(3) - 1 : 0;
        return QiChargeVisuals.clampParticleCount(baseCount + fluctuation);
    }

    private static double randomSigned(RandomSource random, double range) {
        return (random.nextDouble() * 2.0 - 1.0) * range;
    }
}
