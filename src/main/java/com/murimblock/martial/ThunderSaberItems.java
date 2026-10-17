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
 * The Purple Heaven Thunder Saber's items (docs/combat/styles/purple-heaven-thunder-saber.md): its manual, a dark bronze
 * booklet cast with the thunder pattern (MartialManualItem: from Qi Condensation), and the art's own weapon, the Purple
 * Heaven Zhanmadao (scripts/animation/purple-heaven-zhanmadao-model.cjs): a massive horse-cutting saber held in both
 * hands, a greatsword for Epic Fight (data/murimblock/capabilities/weapons/purple_heaven_zhanmadao.json). Where both are
 * found is still to be decided (Enzo); both are in the creative tab meanwhile.
 */
public final class ThunderSaberItems {
    public static final DeferredItem<Item> MANUAL;
    public static final DeferredItem<SwordItem> ZHANMADAO;
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);

    static {
        MANUAL = ITEMS.register("thunder_saber_manual",
                () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), MartialArts.THUNDER_SABER));
        // 10 attack (3 + the diamond tier's 6 + the hand's 1), a greatsword's slowness.
        ZHANMADAO = ITEMS.register("purple_heaven_zhanmadao",
                () -> new SwordItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(Tiers.DIAMOND, 6, -3.0F))));
    }

    private ThunderSaberItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(MurimItems.MURIMBLOCK_TAB.getKey())) {
                event.accept(MANUAL.get());
                event.accept(ZHANMADAO.get());
            }
        });
    }
}
