package com.murimblock.combat;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Server-timed input lease bound to the held weapon; refreshes cannot change that binding. */
record GuardRequest(int slot, ItemStack weapon, long refreshedAt) {
    GuardRequest {
        Objects.requireNonNull(weapon);
        if (slot < 0 || slot >= 9 || weapon.isEmpty() || refreshedAt < 0) {
            throw new IllegalArgumentException("Invalid guard request");
        }
        weapon = weapon.copy();
    }

    static GuardRequest start(int slot, ItemStack weapon, long tick) {
        return new GuardRequest(slot, weapon, tick);
    }

    GuardRequest refresh(long tick) {
        if (tick < refreshedAt) throw new IllegalArgumentException("Guard refresh moved backwards");
        return new GuardRequest(slot, weapon, tick);
    }

    boolean matches(int slot, ItemStack weapon) {
        return this.slot == slot && ItemStack.matches(this.weapon, weapon);
    }

    boolean isLive(long tick) {
        return tick >= refreshedAt && tick - refreshedAt < MeleeCombatService.GUARD_TIMEOUT_TICKS;
    }

    @Override
    public ItemStack weapon() {
        return weapon.copy();
    }
}
