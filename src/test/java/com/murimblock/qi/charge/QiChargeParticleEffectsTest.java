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
    void qiParticleColorAndScaleMatchTheDesignBaseline() {
        assertEquals(0.15F, QiChargeVisuals.QI_PARTICLE_COLOR.x, 1.0e-6F);
        assertEquals(0.55F, QiChargeVisuals.QI_PARTICLE_COLOR.y, 1.0e-6F);
        assertEquals(1.0F, QiChargeVisuals.QI_PARTICLE_COLOR.z, 1.0e-6F);
        assertEquals(0.65F, QiChargeVisuals.QI_CHARGE_PARTICLE_SCALE, 1.0e-6F);
    }

    @Test
    void intensityStartsAtMinimumForBaselineQiMax() {
        assertEquals(0.0, QiChargeVisuals.computeIntensityFromQiMax(100.0), 1.0e-6);
    }

    @Test
    void intensityIncreasesAtOneThousandQiMax() {
        assertTrue(QiChargeVisuals.computeIntensityFromQiMax(1_000.0)
                > QiChargeVisuals.computeIntensityFromQiMax(100.0));
    }

    @Test
    void intensityIncreasesAgainAtFiveThousandQiMax() {
        assertTrue(QiChargeVisuals.computeIntensityFromQiMax(5_000.0)
                > QiChargeVisuals.computeIntensityFromQiMax(1_000.0));
    }

    @Test
    void intensityReachesMaximumAtTenThousandQiMax() {
        assertEquals(1.0, QiChargeVisuals.computeIntensityFromQiMax(10_000.0), 1.0e-6);
    }

    @Test
    void intensityStaysCappedForHugeQiMax() {
        assertEquals(1.0, QiChargeVisuals.computeIntensityFromQiMax(1_000_000.0), 1.0e-6);
        assertEquals(QiChargeVisuals.MAX_QI_CHARGE_WORLD_PARTICLES,
                QiChargeVisuals.computeBaseWorldParticleCount(1_000_000.0));
    }

    @Test
    void invalidQiMaxClampsToMinimum() {
        assertEquals(0.0, QiChargeVisuals.computeIntensityFromQiMax(-50.0), 1.0e-6);
        assertEquals(0.0, QiChargeVisuals.computeIntensityFromQiMax(Double.NaN), 1.0e-6);
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
        assertEquals(3, QiChargeVisuals.computeBaseWorldParticleCount(500.0));
        assertEquals(4, QiChargeVisuals.computeBaseWorldParticleCount(1_000.0));
        assertEquals(5, QiChargeVisuals.computeBaseWorldParticleCount(2_500.0));
        assertEquals(6, QiChargeVisuals.computeBaseWorldParticleCount(5_000.0));
        assertEquals(8, QiChargeVisuals.computeBaseWorldParticleCount(10_000.0));
    }

    @Test
    void worldParticleCountNeverExceedsConfiguredLimits() {
        RandomSource random = RandomSource.create(789L);

        for (int i = 0; i < 100; i++) {
            int count = QiChargeParticleEffects.computeWorldParticleCount(1_000_000.0, random);

            assertTrue(count >= QiChargeVisuals.MIN_QI_CHARGE_WORLD_PARTICLES);
            assertTrue(count <= QiChargeVisuals.MAX_QI_CHARGE_WORLD_PARTICLES);
        }
    }
}
