package com.murimblock.item;

import com.murimblock.Murimblock;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murimblock items and its own creative tab. The training opponent stays summon-only on purpose. */
public final class MurimItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Murimblock.MOD_ID);

    /** Teal shell with sand-coloured spots, matching the crab texture. */
    public static final DeferredItem<DeferredSpawnEggItem> ARMORED_CRAB_SPAWN_EGG = ITEMS.register("armored_crab_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.ARMORED_CRAB, 0x1D5C63, 0xC9B27C, new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MURIMBLOCK_TAB = TABS.register("murimblock",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.murimblock"))
                    .icon(() -> new ItemStack(ARMORED_CRAB_SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> output.accept(ARMORED_CRAB_SPAWN_EGG.get()))
                    .build());

    private MurimItems() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
