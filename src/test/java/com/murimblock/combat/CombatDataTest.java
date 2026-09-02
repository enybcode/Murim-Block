package com.murimblock.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatDataTest {
    @Test
    void initialCombatModeIsFalse() {
        assertFalse(CombatData.initial().combatMode());
    }

    @Test
    void toggleEnablesCombatMode() {
        assertTrue(CombatData.initial().toggle().combatMode());
    }

    @Test
    void secondToggleDisablesCombatMode() {
        assertFalse(CombatData.initial().toggle().toggle().combatMode());
    }

    @Test
    void setTrueEnablesCombatMode() {
        assertTrue(CombatData.initial().withCombatMode(true).combatMode());
    }

    @Test
    void setFalseDisablesCombatMode() {
        assertFalse(new CombatData(true).withCombatMode(false).combatMode());
    }

    @Test
    void settingTheSameValueReturnsSameInstance() {
        CombatData data = new CombatData(true);

        assertSame(data, data.withCombatMode(true));
    }
}
