package com.murimblock.martial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BloodCrystalStateTest {
    @Test void gaugeMustBeFullAndActivationIsManual() {
        var s = BloodCrystalState.EMPTY.gain(3).gain(16);
        assertEquals(19, s.gauge()); assertFalse(s.active());
        assertSame(s, s.activate());
        s = s.gain(1);
        assertEquals(BloodCrystalState.MAX_GAUGE, s.gauge()); assertFalse(s.active());
        s = s.activate();
        assertTrue(s.active()); assertEquals(0, s.gauge()); assertEquals(0, s.crystals());
    }
    @Test void landedM1sEarnAtMostTenCrystalsAndBarrageConsumesThem() {
        var s = BloodCrystalState.EMPTY.gain(BloodCrystalState.MAX_GAUGE).activate();
        for (int i=0;i<30;i++) s=s.gain(1);
        assertEquals(10,s.crystals());
        assertEquals(0,s.consume().crystals());
        assertTrue(s.consume().active());
        assertEquals(1,s.consume().gain(1).crystals());
    }
    @Test void malformedCountsAndNoContactCannotCreateStacks() {
        assertEquals(BloodCrystalState.EMPTY, new BloodCrystalState(-100,false,8));
        assertEquals(BloodCrystalState.EMPTY, BloodCrystalState.EMPTY.gain(0).gain(-5));
        assertEquals(BloodCrystalState.MAX_GAUGE,new BloodCrystalState(100,false,0).gauge());
        assertEquals(10,new BloodCrystalState(100,true,200).crystals());
        assertEquals(0,new BloodCrystalState(100,true,200).gauge());
    }
    @Test void defenseIsOneFixedFifteenPercentBonusDuringTheForm() {
        assertEquals(1.0F,BloodCrystalState.EMPTY.damageFactor());
        var form=BloodCrystalState.EMPTY.gain(BloodCrystalState.MAX_GAUGE).activate();
        assertEquals(.85F,form.damageFactor());
        assertEquals(.85F,form.gain(10).damageFactor());
        assertEquals(.85F,form.gain(10).consume().damageFactor());
    }
}
