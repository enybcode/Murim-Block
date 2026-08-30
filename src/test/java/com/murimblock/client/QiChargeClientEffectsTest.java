package com.murimblock.client;

import com.murimblock.qi.charge.QiChargeVisuals;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QiChargeClientEffectsTest {
    @Test
    void foregroundCooldownStaysWithinConfiguredRange() {
        RandomSource random = RandomSource.create(123L);

        for (int i = 0; i < 100; i++) {
            int cooldown = QiChargeClientEffects.nextForegroundCooldown(random);

            assertTrue(cooldown >= QiChargeVisuals.QI_CHARGE_FOREGROUND_INTERVAL_MIN_TICKS);
            assertTrue(cooldown < QiChargeVisuals.QI_CHARGE_FOREGROUND_INTERVAL_MIN_TICKS
                    + QiChargeVisuals.QI_CHARGE_FOREGROUND_INTERVAL_RANGE_TICKS);
        }
    }
}
