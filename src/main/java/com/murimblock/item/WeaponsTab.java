package com.murimblock.item;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightContentPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.common.Tags;

import java.util.Comparator;
import java.util.List;

/**
 * The "Murimblock: Weapons" creative tab (Enzo, 15/10/2026): every weapon of every mod installed, found by itself, so a
 * new weapon (ours or another mod's) appears without a list to keep. Murimblock's first, then Epic Fight's, Dawn Day's,
 * Minecraft's own, then the other mods' by name; within a mod, in the order they were registered.
 * <p>A weapon here: a sword, an axe, a trident, a mace, a bow or crossbow, a shield, or one of Epic Fight's weapon items
 * (greatswords, spears, tachi, daggers, gloves, and Dawn Day's, which build on them), or anything in the common tags
 * c:tools/melee_weapon and c:tools/ranged_weapon. Tools that only dig are left out.
 */
public final class WeaponsTab {
    private WeaponsTab() { }

    /** Whether this item is a weapon (see the class). */
    public static boolean isWeapon(Item item) {
        return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem
                || item instanceof ProjectileWeaponItem || item instanceof ShieldItem || item instanceof yesman.epicfight.world.item.WeaponItem
                // Other mods' weapons, by the common tags (once tags are loaded).
                || item.builtInRegistryHolder().is(Tags.Items.MELEE_WEAPON_TOOLS) || item.builtInRegistryHolder().is(Tags.Items.RANGED_WEAPON_TOOLS);
    }

    /** The mods' order in the tab. */
    private static int rank(String namespace) {
        return switch (namespace) {
            case Murimblock.MOD_ID -> 0;
            case "epicfight" -> 1;
            case EpicFightContentPolicy.DAWN_DAY -> 2;
            case "minecraft" -> 3;
            default -> 4;
        };
    }

    /** Every weapon, in the tab's order. */
    public static List<Item> weapons() {
        return BuiltInRegistries.ITEM.stream().filter(WeaponsTab::isWeapon)
                .sorted(Comparator.comparingInt((Item item) -> rank(key(item).getNamespace()))
                        .thenComparing(item -> rank(key(item).getNamespace()) == 4 ? key(item).getNamespace() : "")
                        .thenComparingInt(BuiltInRegistries.ITEM::getId))
                .toList();
    }

    private static ResourceLocation key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    /** The tab's contents (its displayItems). */
    static void fill(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (Item item : weapons()) {
            ItemStack stack = new ItemStack(item);
            if (stack.isItemEnabled(parameters.enabledFeatures())) output.accept(stack);
        }
    }
}
