package com.murimblock.combat;

import java.util.Objects;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/** Classification does not install behavior for unsupported weapon families. */
public enum WeaponCategory {
    UNARMED, SWORD, AXE, TRIDENT, BOW, CROSSBOW, OTHER;

    public static WeaponCategory of(ItemStack stack) {
        Objects.requireNonNull(stack);
        if (stack.isEmpty()) return UNARMED;
        if (stack.is(ItemTags.SWORDS) || stack.getItem() instanceof SwordItem) return SWORD;
        if (stack.is(ItemTags.AXES) || stack.getItem() instanceof AxeItem) return AXE;
        if (stack.getItem() instanceof TridentItem) return TRIDENT;
        if (stack.getItem() instanceof BowItem) return BOW;
        if (stack.getItem() instanceof CrossbowItem) return CROSSBOW;
        return OTHER;
    }
}
