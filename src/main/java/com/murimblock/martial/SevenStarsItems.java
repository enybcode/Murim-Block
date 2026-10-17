package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Seven Stars Sword's items (docs/combat/styles/seven-stars-sword.md): its manual, a night-blue booklet with the Big
 * Dipper in silver (MartialManualItem: from Qi Condensation), and the art's own florett, the Seven Stars Florett
 * (scripts/animation/seven-stars-florett.cjs): a little above a diamond florett, on Dawn Day's florett moveset; with the art,
 * the first blow on a target puts two stars, not one
 * (data/murimblock/capabilities/weapons/seven_stars_florett.json). Where both are found is still to be decided (Enzo).
 */
public final class SevenStarsItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredItem<MartialManualItem> MANUAL = ITEMS.register("seven_stars_manual",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), MartialArts.SEVEN_STARS_SWORD));
    /** 7 attack (3 + the diamond tier's 3 + the hand's 1), a florett's quickness. */
    public static final DeferredItem<SwordItem> FLORETT = ITEMS.register("seven_stars_florett",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.2F))));

    private SevenStarsItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(MurimItems.MURIMBLOCK_TAB.getKey())) {
                event.accept(MANUAL.get());
                event.accept(FLORETT.get());
            }
        });
    }
}
