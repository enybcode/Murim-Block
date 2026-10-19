package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import java.util.Optional;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Murim's townsfolk trade in Murim's money (Enzo, 19/10/2026: "les villageois dans nos villages utilisent notre
 * monnaie"): a villager placed by a site carries {@link #TAG}; whatever Minecraft gives it to trade (its first offers,
 * those a new level brings) is paid in strings of cash instead of emeralds, one for one (a string buys four loaves at
 * the Alliance's merchant, an emerald six). Looked at every few seconds, so that new offers are changed too.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class MurimTrades {
    /** The tag of a site's villager. */
    public static final String TAG = "murimblock.townsfolk";
    static final int EVERY = 60;
    /** For the tests: how often townsfolk are looked at (ticks). */
    public static final int EVERY_FOR_TESTS = EVERY;

    private MurimTrades() {
    }

    /** Marks a site's villager: it trades in Murim's money. */
    public static void townsfolk(Villager villager) {
        villager.addTag(TAG);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.level().isClientSide || villager.tickCount % EVERY != 1) return;
        // One placed before the tag existed (an older world) is known by its Murim name.
        if (!villager.getTags().contains(TAG) && villager.getCustomName() != null
                && villager.getCustomName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents name
                && name.getKey().startsWith("villager.murimblock.")) townsfolk(villager);
        if (!villager.getTags().contains(TAG) || villager.isTrading()) return;
        coin(villager.getOffers());
    }

    /** Emeralds turned into strings of cash, one for one, in every offer (what is asked and what is given). */
    public static void coin(MerchantOffers offers) {
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer o = offers.get(i);
            boolean a = o.getItemCostA().item().value() == Items.EMERALD;
            boolean b = o.getItemCostB().filter(c -> c.item().value() == Items.EMERALD).isPresent();
            boolean r = o.getResult().is(Items.EMERALD);
            if (!a && !b && !r) continue;
            ItemCost costA = a ? new ItemCost(MurimItems.CASH_STRING.get(), o.getItemCostA().count()) : o.getItemCostA();
            Optional<ItemCost> costB = b ? o.getItemCostB().map(c -> new ItemCost(MurimItems.CASH_STRING.get(), c.count())) : o.getItemCostB();
            ItemStack result = r ? new ItemStack(MurimItems.CASH_STRING.get(), o.getResult().getCount()) : o.getResult();
            MerchantOffer coined = new MerchantOffer(costA, costB, result, o.getUses(), o.getMaxUses(), o.getXp(), o.getPriceMultiplier(), o.getDemand());
            offers.set(i, coined);
        }
    }
}
