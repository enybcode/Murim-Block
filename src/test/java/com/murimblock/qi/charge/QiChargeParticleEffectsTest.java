package com.murimblock.qi.charge;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QiChargeParticleEffectsTest {
    @Test
    void worldParticlesUseConfiguredInterval() {
        assertTrue(QiChargeParticleEffects.shouldSpawnWorldParticles(0));
        assertTrue(QiChargeParticleEffects.shouldSpawnWorldParticles(QiChargeVisuals.QI_CHARGE_WORLD_INTERVAL_TICKS));
        assertFalse(QiChargeParticleEffects.shouldSpawnWorldParticles(QiChargeVisuals.QI_CHARGE_WORLD_INTERVAL_TICKS - 1));
    }

    @Test
    void worldParticleIntervalGetsShorterForStrongAuras() {
        assertEquals(5, QiChargeVisuals.computeWorldIntervalTicks(100.0));
        assertEquals(5, QiChargeVisuals.computeWorldIntervalTicks(1_000.0));
        assertEquals(4, QiChargeVisuals.computeWorldIntervalTicks(2_500.0));
        assertEquals(3, QiChargeVisuals.computeWorldIntervalTicks(10_000.0));
        assertTrue(QiChargeParticleEffects.shouldSpawnWorldParticles(12, 10_000.0));
        assertFalse(QiChargeParticleEffects.shouldSpawnWorldParticles(13, 10_000.0));
    }

    @Test
    void qiParticleColorAndScaleMatchTheDesignBaseline() {
        assertEquals(0.15F, QiChargeVisuals.QI_PARTICLE_COLOR.x, 1.0e-6F);
        assertEquals(0.55F, QiChargeVisuals.QI_PARTICLE_COLOR.y, 1.0e-6F);
        assertEquals(1.0F, QiChargeVisuals.QI_PARTICLE_COLOR.z, 1.0e-6F);
        assertEquals(0.65F, QiChargeVisuals.QI_CHARGE_PARTICLE_SCALE, 1.0e-6F);
    }

    @Test
    void intensityStartsAtMinimumForBaselineQiMax() {
        assertEquals(0.0, QiChargeVisuals.computeIntensityFromQiMax(100.0), 1.0e-6);
        assertEquals(0.0, QiChargeVisuals.computeAuraIntensityFromQiMax(100.0), 1.0e-6);
    }

    @Test
    void auraIsStillWeakBeforeOneThousandQiMax() {
        double auraIntensity = QiChargeVisuals.computeAuraIntensityFromQiMax(999.0);

        assertTrue(auraIntensity > 0.0);
        assertTrue(auraIntensity < 0.2);
    }

    @Test
    void auraActivatesAtOneThousandQiMax() {
        assertTrue(QiChargeVisuals.computeAuraIntensityFromQiMax(1_000.0) >= 0.18);
        assertTrue(QiChargeVisuals.computeAuraParticleCount(1_000.0) > 0);
    }

    @Test
    void auraIntensityKeepsIncreasingWithQiMax() {
        double atOneThousand = QiChargeVisuals.computeAuraIntensityFromQiMax(1_000.0);
        double atTwentyFiveHundred = QiChargeVisuals.computeAuraIntensityFromQiMax(2_500.0);
        double atFiveThousand = QiChargeVisuals.computeAuraIntensityFromQiMax(5_000.0);

        assertTrue(atTwentyFiveHundred > atOneThousand);
        assertTrue(atFiveThousand > atTwentyFiveHundred);
    }

    @Test
    void auraIntensityReachesMaximumAtTenThousandQiMax() {
        assertEquals(1.0, QiChargeVisuals.computeIntensityFromQiMax(10_000.0), 1.0e-6);
        assertEquals(1.0, QiChargeVisuals.computeAuraIntensityFromQiMax(10_000.0), 1.0e-6);
    }

    @Test
    void intensityStaysCappedForHugeQiMax() {
        assertEquals(1.0, QiChargeVisuals.computeIntensityFromQiMax(1_000_000.0), 1.0e-6);
        assertEquals(1.0, QiChargeVisuals.computeAuraIntensityFromQiMax(1_000_000.0), 1.0e-6);
        assertEquals(QiChargeVisuals.MAX_QI_CHARGE_WORLD_PARTICLES,
                QiChargeVisuals.computeBaseWorldParticleCount(1_000_000.0));
    }

    @Test
    void invalidQiMaxClampsToMinimum() {
        assertEquals(0.0, QiChargeVisuals.computeIntensityFromQiMax(-50.0), 1.0e-6);
        assertEquals(0.0, QiChargeVisuals.computeAuraIntensityFromQiMax(-50.0), 1.0e-6);
        assertEquals(0.0, QiChargeVisuals.computeIntensityFromQiMax(Double.NaN), 1.0e-6);
        assertEquals(0.0, QiChargeVisuals.computeAuraIntensityFromQiMax(Double.NaN), 1.0e-6);
    }

    @Test
    void intensityIgnoresCurrentQi() {
        double lowCurrentQiIntensity = QiChargeVisuals.computeIntensityFromQiMax(1_000.0);
        double highCurrentQiIntensity = QiChargeVisuals.computeIntensityFromQiMax(1_000.0);

        assertEquals(lowCurrentQiIntensity, highCurrentQiIntensity, 1.0e-6);
    }

    @Test
    void baseWorldParticleCountFollowsConfiguredQiMaxMilestones() {
        assertEquals(2, QiChargeVisuals.computeBaseWorldParticleCount(100.0));
        assertEquals(4, QiChargeVisuals.computeBaseWorldParticleCount(500.0));
        assertEquals(8, QiChargeVisuals.computeBaseWorldParticleCount(1_000.0));
        assertEquals(12, QiChargeVisuals.computeBaseWorldParticleCount(2_500.0));
        assertEquals(16, QiChargeVisuals.computeBaseWorldParticleCount(5_000.0));
        assertEquals(24, QiChargeVisuals.computeBaseWorldParticleCount(10_000.0));
    }

    @Test
    void auraParticleCountGrowsWhileNormalParticlesStayControlled() {
        assertEquals(0, QiChargeVisuals.computeAuraParticleCount(100.0));
        assertEquals(3, QiChargeVisuals.computeAuraParticleCount(1_000.0));
        assertEquals(7, QiChargeVisuals.computeAuraParticleCount(2_500.0));
        assertEquals(11, QiChargeVisuals.computeAuraParticleCount(5_000.0));
        assertEquals(19, QiChargeVisuals.computeAuraParticleCount(10_000.0));
        assertEquals(5, QiChargeVisuals.computeNormalParticleCount(10_000.0));
    }

    @Test
    void worldParticleCountNeverExceedsConfiguredLimits() {
        RandomSource random = RandomSource.create(789L);

        double[] qiMaxValues = {100.0, 999.0, 1_000.0, 2_500.0, 5_000.0, 10_000.0, 1_000_000.0};
        for (double qiMax : qiMaxValues) {
            for (int i = 0; i < 100; i++) {
                int count = QiChargeParticleEffects.computeWorldParticleCount(qiMax, random);

                assertTrue(count >= QiChargeVisuals.MIN_QI_CHARGE_WORLD_PARTICLES);
                assertTrue(count <= QiChargeVisuals.MAX_QI_CHARGE_WORLD_PARTICLES);
            }
        }
    }
}
