package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Celestial Peak Spear's manual: a military booklet bound in blue, the three peaks of the Alliance on its back. Read,
 * it teaches the art (MartialManualItem). Where it is found is the Peak Captain's to give (docs/mobs).
 */
public final class CelestialPeakItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredItem<MartialManualItem> MANUAL = ITEMS.register("celestial_peak_spear_manual",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), MartialArts.CELESTIAL_PEAK_SPEAR));

    private CelestialPeakItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(MurimItems.MURIMBLOCK_TAB.getKey())) event.accept(MANUAL.get());
        });
    }
}
