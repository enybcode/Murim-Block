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
            int cooldown = QiChargeClientEffects.nextForegroundCooldown(random, 100.0);

            assertTrue(cooldown >= 16);
            assertTrue(cooldown <= 22);
        }
    }

    @Test
    void foregroundCooldownGetsShorterAtHighQiMax() {
        RandomSource random = RandomSource.create(456L);

        for (int i = 0; i < 100; i++) {
            int cooldown = QiChargeClientEffects.nextForegroundCooldown(random, 10_000.0);

            assertTrue(cooldown >= 7);
            assertTrue(cooldown <= 11);
        }
    }
}
