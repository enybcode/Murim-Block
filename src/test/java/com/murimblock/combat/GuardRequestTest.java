package com.murimblock.combat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardRequestTest {
    @Test
    void lostReleaseExpiresWithAnExclusiveServerTickBoundary() {
        var request = GuardRequest.start(0, new ItemStack(Items.IRON_SWORD), 100);
        assertFalse(request.isLive(99));
        assertTrue(request.isLive(100));
        assertTrue(request.isLive(129));
        assertFalse(request.isLive(130));
        assertFalse(request.isLive(Long.MAX_VALUE));
        assertTrue(MeleeCombatService.GUARD_TIMEOUT_TICKS > MeleeCombatService.GUARD_REFRESH_TICKS);
    }

    @Test
    void refreshKeepsWeaponAndSlotBindingWithoutMutatingTheOriginal() {
        var request = GuardRequest.start(2, new ItemStack(Items.IRON_SWORD), 100);
        var refreshed = request.refresh(110);
        assertFalse(request.isLive(130));
        assertTrue(refreshed.isLive(130));
        assertTrue(refreshed.matches(2, new ItemStack(Items.IRON_SWORD)));
        assertFalse(refreshed.matches(1, new ItemStack(Items.IRON_SWORD)));
        assertFalse(refreshed.matches(2, new ItemStack(Items.DIAMOND_SWORD)));
        assertThrows(IllegalArgumentException.class, () -> request.refresh(99));
    }

    @Test
    void weaponCopiesAndComponentChecksPreventStaleGuardAfterAnEquipmentChange() {
        ItemStack weapon = new ItemStack(Items.IRON_SWORD);
        var request = GuardRequest.start(0, weapon, 100);
        weapon.setDamageValue(1);
        assertFalse(request.matches(0, weapon));
        assertTrue(request.matches(0, new ItemStack(Items.IRON_SWORD)));
        ItemStack exposed = request.weapon();
        exposed.setDamageValue(2);
        assertTrue(request.matches(0, new ItemStack(Items.IRON_SWORD)));
    }

    @Test
    void invalidInputCannotCreateARequest() {
        assertThrows(IllegalArgumentException.class, () -> GuardRequest.start(9, new ItemStack(Items.IRON_SWORD), 0));
        assertThrows(IllegalArgumentException.class, () -> GuardRequest.start(-1, new ItemStack(Items.IRON_SWORD), 0));
        assertThrows(IllegalArgumentException.class, () -> GuardRequest.start(0, ItemStack.EMPTY, 0));
        assertThrows(IllegalArgumentException.class, () -> GuardRequest.start(0, new ItemStack(Items.IRON_SWORD), -1));
    }
}
