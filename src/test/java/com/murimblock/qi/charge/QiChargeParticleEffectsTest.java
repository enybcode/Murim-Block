package com.murimblock.qi.charge;

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
}
