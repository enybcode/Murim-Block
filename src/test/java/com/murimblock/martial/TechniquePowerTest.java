package com.murimblock.martial;

import com.murimblock.combat.BurstGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The techniques' damage: the weapon, the Qi, and no combo that kills at once (docs/combat/technique-balance.md). */
class TechniquePowerTest {
    @Test
    void theQiAddsTwelvePercentADoubling() {
        assertEquals(1.0, TechniquePower.qiFactor(100), 1e-9);
        assertEquals(1.0, TechniquePower.qiFactor(50), 1e-9);
        assertEquals(1.12, TechniquePower.qiFactor(200), 1e-9);
        assertEquals(1.24, TechniquePower.qiFactor(400), 1e-9);
        // The last realm's gate: a little more than twice a beginner's.
        assertTrue(TechniquePower.qiFactor(45_000) > 2.0 && TechniquePower.qiFactor(45_000) < 2.1);
    }

    @Test
    void aTrainedHandStrikesLikeAStoneSword() {
        // A beginner's bare hand (1) with the training: 4, a stone sword's 1 + 3... and no change for a weapon.
        assertEquals(4.0, 1 * TechniquePower.fistFactor(1, true), 1e-9);
        assertEquals(1.0, TechniquePower.fistFactor(6, false), 1e-9);
        // With the realm's fist bonus it matters less and less.
        assertEquals(16.0 / 13.0, TechniquePower.fistFactor(13, true), 1e-9);
    }

    @Test
    void noComboTakesMoreThanSixtyPercentInItsWindow() {
        // Hits of 7 on 20 health: 7, 5, then nothing.
        double dealt = 0;
        float[] got = new float[4];
        for (int i = 0; i < got.length; i++) {
            got[i] = BurstGuard.allowed(20, dealt, 7);
            dealt += got[i];
        }
        assertArrayEquals(new float[]{7, 5, 0, 0}, got, 1e-5F);
        // Whatever the blows, the window's total stays within 60 %.
        dealt = 0;
        for (int i = 0; i < 50; i++) dealt += BurstGuard.allowed(30, dealt, (float) (Math.random() * 40));
        assertTrue(dealt <= 18 + 1e-4, "more than 60 % of 30: " + dealt);
    }
}
