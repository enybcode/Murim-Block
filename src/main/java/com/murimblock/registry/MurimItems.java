package com.murimblock.registry;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.MeridianPillItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murimblock items. Registry names are saved IDs: keep them. */
public final class MurimItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);

    public static final DeferredItem<MeridianPillItem> MERIDIAN_PILL = ITEMS.register("meridian_opening_pill",
            () -> new MeridianPillItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    private MurimItems() {
    }
}
