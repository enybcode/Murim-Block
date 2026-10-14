package com.murimblock.martial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloodCrystalChargeTest {
    @Test
    void pointsAreDrawnEveryFifthOfASecondAfterTheMinimumCharge() {
        assertFalse(BloodCrystalCharge.drawsPoint(11, 0));
        assertTrue(BloodCrystalCharge.drawsPoint(12, 0));
        assertFalse(BloodCrystalCharge.drawsPoint(13, 1));
        assertTrue(BloodCrystalCharge.drawsPoint(16, 1));
        assertFalse(BloodCrystalCharge.drawsPoint(52, 10), "no eleventh point");
        int points = 0;
        for (int held = 0; held <= BloodCrystalCharge.AUTO_RELEASE_TICKS; held++) if (BloodCrystalCharge.drawsPoint(held, points)) points++;
        assertEquals(10, points, "a full charge draws the whole gauge before it leaves on its own");
    }

    @Test
    void theCrystalGrowsToThreeBlocksAtTenPoints() {
        assertEquals(0.2F, BloodCrystalCharge.scale(0, 0), 1e-6);
        assertEquals(0.6F, BloodCrystalCharge.scale(12, 0), 1e-6);
        assertEquals(4.0F, BloodCrystalCharge.scale(60, 10), 1e-5);
        assertEquals(3.0, 0.75 * BloodCrystalCharge.scale(60, 10), 1e-5, "the great crystal is 3 blocks across");
    }

    @Test
    void pointsAddPowerAndTenMakeTheGreatCrystal() {
        assertEquals(1.2, BloodCrystalCharge.share(0), 1e-9);
        assertEquals(3.0, BloodCrystalCharge.share(10), 1e-9);
        assertFalse(BloodCrystalCharge.great(9));
        assertTrue(BloodCrystalCharge.great(10));
    }

    @Test
    void theExplosionFadesToHalfAtItsEdge() {
        assertEquals(2.0, BloodCrystalCharge.explosionShare(0), 1e-9);
        assertEquals(1.0, BloodCrystalCharge.explosionShare(BloodCrystalCharge.EXPLOSION_RADIUS), 1e-9);
        assertEquals(0.0, BloodCrystalCharge.explosionShare(BloodCrystalCharge.EXPLOSION_RADIUS + 0.1), 1e-9);
    }

    @Test
    void theGaugeIsSpentOutsideTheFormOnly() {
        assertEquals(7, new BloodCrystalState(10, false, 0).spend(3).gauge());
        assertEquals(0, new BloodCrystalState(2, false, 0).spend(5).gauge());
        var form = new BloodCrystalState(0, true, 4);
        assertEquals(form, form.spend(1));
    }
}
