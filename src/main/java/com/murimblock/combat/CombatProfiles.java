package com.murimblock.combat;

import com.murimblock.Murimblock;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CombatProfiles {
    public static final CombatProfile BASIC_SWORD = new CombatProfile(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "basic_sword"), WeaponCategory.SWORD,
            new CombatProfile.Guard(120, 4, 4, 0.35, 4, 14, 10));

    private CombatProfiles() { }

    public static Optional<CombatProfile> find(WeaponCategory category, ResourceLocation martialArt) {
        Objects.requireNonNull(category);
        Objects.requireNonNull(martialArt);
        return category == BASIC_SWORD.category() && martialArt.equals(BASIC_SWORD.martialArt())
                ? Optional.of(BASIC_SWORD) : Optional.empty();
    }

    /** No learned-art selection exists yet; only the basic sword profile is enabled. */
    public static Optional<CombatProfile> basicFor(ItemStack weapon) {
        return find(WeaponCategory.of(weapon), BASIC_SWORD.martialArt());
    }
}
