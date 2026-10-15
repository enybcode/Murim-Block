package com.murimblock.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloodCrystalViewTest {
    @Test
    void ownBackCrystalsAreHiddenInFirstPerson() {
        assertTrue(BloodCrystalView.hiddenInFirstPerson(true, true, true, true));
    }

    @Test
    void backCrystalsShowInThirdPersonAndToOthers() {
        assertFalse(BloodCrystalView.hiddenInFirstPerson(true, true, true, false));
        assertFalse(BloodCrystalView.hiddenInFirstPerson(true, false, true, true));
        assertFalse(BloodCrystalView.hiddenInFirstPerson(true, true, false, true));
    }

    @Test
    void thrownCrystalsAreNeverHiddenByThisRule() {
        assertFalse(BloodCrystalView.hiddenInFirstPerson(false, true, true, true));
    }
}
