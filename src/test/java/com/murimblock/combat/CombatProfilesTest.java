package com.murimblock.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatProfilesTest {
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    @Test
    void everySwordMaterialUsesTheSwordCategoryRatherThanOneItemId() {
        for (var sword : new net.minecraft.world.item.Item[]{Items.WOODEN_SWORD, Items.STONE_SWORD,
                Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD}) {
            assertEquals(WeaponCategory.SWORD, WeaponCategory.of(new ItemStack(sword)));
            assertEquals(CombatProfiles.BASIC_SWORD, CombatProfiles.basicFor(new ItemStack(sword)).orElseThrow());
        }
    }

    @Test
    void nonSwordFamiliesAreClassifiedButHaveNoInstalledCombatProfile() {
        var weapons = java.util.Map.of(Items.IRON_AXE, WeaponCategory.AXE, Items.TRIDENT, WeaponCategory.TRIDENT,
                Items.BOW, WeaponCategory.BOW, Items.CROSSBOW, WeaponCategory.CROSSBOW,
                Items.IRON_PICKAXE, WeaponCategory.OTHER, Items.SHIELD, WeaponCategory.OTHER);
        weapons.forEach((item, category) -> {
            assertEquals(category, WeaponCategory.of(new ItemStack(item)));
            assertTrue(CombatProfiles.basicFor(new ItemStack(item)).isEmpty());
        });
        assertEquals(WeaponCategory.UNARMED, WeaponCategory.of(ItemStack.EMPTY));
        assertTrue(CombatProfiles.basicFor(ItemStack.EMPTY).isEmpty());
    }

    @Test
    void unknownArtAndIncompatibleCategoryDoNotInheritSwordBehavior() {
        var swordArt = CombatProfiles.BASIC_SWORD.martialArt();
        assertTrue(CombatProfiles.find(WeaponCategory.AXE, swordArt).isEmpty());
        assertTrue(CombatProfiles.find(WeaponCategory.SWORD, ResourceLocation.parse("murimblock:unknown_art")).isEmpty());
        assertTrue(CombatProfiles.find(WeaponCategory.SWORD, swordArt).isPresent());
    }

    @Test
    void guardCoversTheFrontButNotFlanksRearOrCoincidentPositions() {
        var guard = CombatProfiles.BASIC_SWORD.guard();
        assertTrue(guard.covers(FORWARD, FORWARD));
        assertTrue(guard.covers(FORWARD, new Vec3(1, 2, 2)));
        assertFalse(guard.covers(FORWARD, new Vec3(1, 0, 0)));
        assertFalse(guard.covers(FORWARD, new Vec3(0, 0, -1)));
        assertFalse(guard.covers(Vec3.ZERO, FORWARD));
        assertFalse(guard.covers(FORWARD, Vec3.ZERO));
        assertFalse(guard.covers(new Vec3(Double.NaN, 0, 1), FORWARD));
        assertFalse(guard.covers(FORWARD, new Vec3(Double.POSITIVE_INFINITY, 0, 1)));
    }

    @Test
    void guardBudgetHasAValidMinimumAndRejectsInvalidOrInsufficientQi() {
        var guard = CombatProfiles.BASIC_SWORD.guard();
        assertEquals(4, guard.cost(0.5F));
        assertEquals(24, guard.cost(6));
        assertTrue(guard.canPay(24, 6));
        assertFalse(guard.canPay(23.99, 6));
        for (float damage : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(0, guard.cost(damage));
            assertFalse(guard.canPay(100, damage));
        }
        assertFalse(guard.canPay(Double.NaN, 6));
        assertFalse(guard.canPay(Double.POSITIVE_INFINITY, 6));
    }

    @Test
    void anotherArtCanHaveDifferentRulesWithoutChangingTheWeaponCategory() {
        var alternate = new CombatProfile(ResourceLocation.parse("murimblock:test_art"), WeaponCategory.SWORD,
                new CombatProfile.Guard(60, 2, 2, 0.6, 2, 8, 6));
        assertEquals(CombatProfiles.BASIC_SWORD.category(), alternate.category());
        assertTrue(CombatProfiles.BASIC_SWORD.guard().covers(FORWARD, new Vec3(1, 0, 1)));
        assertFalse(alternate.guard().covers(FORWARD, new Vec3(1, 0, 1)));
        assertEquals(12, alternate.guard().cost(6));
        assertEquals(24, CombatProfiles.BASIC_SWORD.guard().cost(6));
        // This fixture is not a learned or selectable art in the game.
        assertTrue(CombatProfiles.find(alternate.category(), alternate.martialArt()).isEmpty());
    }

    @Test
    void invalidProfilesFailInsteadOfGrantingFreeOrOmnidirectionalDefense() {
        assertThrows(IllegalArgumentException.class, () -> new CombatProfile.Guard(360, 4, 4, 0.35, 4, 14, 10));
        assertThrows(IllegalArgumentException.class, () -> new CombatProfile.Guard(120, 0, 4, 0.35, 4, 14, 10));
        assertThrows(IllegalArgumentException.class, () -> new CombatProfile.Guard(120, 4, 0, 0.35, 4, 14, 10));
        assertThrows(IllegalArgumentException.class, () -> new CombatProfile.Guard(120, 4, 4, 0, 4, 14, 10));
        assertThrows(IllegalArgumentException.class, () -> new CombatProfile.Guard(120, 4, 4, 2, 4, 14, 10));
        assertThrows(IllegalArgumentException.class, () -> new CombatProfile.Guard(120, 4, 4, 0.35, 0, 14, 10));
        assertThrows(IllegalArgumentException.class, () -> new CombatProfile.Guard(Double.NaN, 4, 4, 0.35, 4, 14, 10));
    }
}
