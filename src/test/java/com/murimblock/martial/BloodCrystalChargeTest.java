package com.murimblock.martial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloodCrystalChargeTest {
    @Test
    void pointsAreDrawnEveryTenthOfASecondAfterTheMinimumCharge() {
        assertFalse(BloodCrystalCharge.drawsPoint(11, 0));
        assertTrue(BloodCrystalCharge.drawsPoint(12, 0));
        assertFalse(BloodCrystalCharge.drawsPoint(13, 1));
        assertTrue(BloodCrystalCharge.drawsPoint(14, 1));
        assertFalse(BloodCrystalCharge.drawsPoint(52, 20), "no twenty-first point");
        int points = 0;
        for (int held = 0; held <= BloodCrystalCharge.AUTO_RELEASE_TICKS; held++) if (BloodCrystalCharge.drawsPoint(held, points)) points++;
        assertEquals(BloodCrystalState.MAX_GAUGE, points, "a full charge draws the whole gauge before it leaves on its own");
    }

    @Test
    void theCrystalGrowsToThreeBlocksAtAFullGauge() {
        assertEquals(0.2F, BloodCrystalCharge.scale(0, 0), 1e-6);
        assertEquals(0.6F, BloodCrystalCharge.scale(12, 0), 1e-6);
        assertEquals(4.0F, BloodCrystalCharge.scale(60, 20), 1e-5);
        assertEquals(3.0, 0.75 * BloodCrystalCharge.scale(60, 20), 1e-5, "the great crystal is 3 blocks across");
    }

    @Test
    void pointsAddPowerAndAFullGaugeMakesTheGreatCrystal() {
        assertEquals(1.0, BloodCrystalCharge.share(0), 1e-9);
        assertEquals(2.4, BloodCrystalCharge.share(20), 1e-9);
        assertFalse(BloodCrystalCharge.great(19));
        assertTrue(BloodCrystalCharge.great(20));
    }

    @Test
    void theExplosionFadesToHalfAtItsEdge() {
        assertEquals(1.6, BloodCrystalCharge.explosionShare(0), 1e-9);
        assertEquals(0.8, BloodCrystalCharge.explosionShare(BloodCrystalCharge.EXPLOSION_RADIUS), 1e-9);
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
