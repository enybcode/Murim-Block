package com.murimblock.qi.charge;

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
        for (int i = 0; i < QiChargeVisuals.QI_CHARGE_WORLD_COUNT; i++) {
            double x = player.getX() + randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_RADIUS);
            double y = player.getY() + random.nextDouble() * height;
            double z = player.getZ() + randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_RADIUS);
            double xSpeed = randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_HORIZONTAL_DRIFT);
            double ySpeed = QiChargeVisuals.QI_CHARGE_WORLD_UPWARD_SPEED_MIN
                    + random.nextDouble() * QiChargeVisuals.QI_CHARGE_WORLD_UPWARD_SPEED_RANGE;
            double zSpeed = randomSigned(random, QiChargeVisuals.QI_CHARGE_WORLD_HORIZONTAL_DRIFT);

            level.sendParticles(QiChargeVisuals.QI_DUST_PARTICLE, x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0);
        }
    }

    static boolean shouldSpawnWorldParticles(int tickCount) {
        return tickCount % QiChargeVisuals.QI_CHARGE_WORLD_INTERVAL_TICKS == 0;
    }

    private static double randomSigned(RandomSource random, double range) {
        return (random.nextDouble() * 2.0 - 1.0) * range;
    }
}
