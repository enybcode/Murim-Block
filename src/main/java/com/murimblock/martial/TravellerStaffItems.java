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
 * The Traveller's Staff Art's scroll: a road traveller's scroll on two wooden rods, a staff drawn on it. Read, it teaches
 * the art and its three techniques (MartialManualItem). Where it is found is still to be decided (Enzo).
 */
public final class TravellerStaffItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredItem<MartialManualItem> SCROLL = ITEMS.register("traveller_staff_scroll",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), MartialArts.TRAVELLER_STAFF));

    private TravellerStaffItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(MurimItems.MURIMBLOCK_TAB.getKey())) event.accept(SCROLL.get());
        });
    }
}
