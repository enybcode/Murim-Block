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
 * The Golden Bell's manual (docs/combat/styles/golden-bell.md): a monk's sutra in saffron covers, a golden bell on it. Read,
 * it teaches the art and its first technique (MartialManualItem). Sold by the Golden Bell monk (Enzo, 17/10/2026).
 */
public final class GoldenBellItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredItem<MartialManualItem> MANUAL = ITEMS.register("golden_bell_manual",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), MartialArts.GOLDEN_BELL));

    private GoldenBellItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(MurimItems.MURIMBLOCK_TAB.getKey())) event.accept(MANUAL.get());
        });
    }
}
