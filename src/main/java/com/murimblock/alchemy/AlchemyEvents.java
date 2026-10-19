package com.murimblock.alchemy;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.PillItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The alchemy's hooks (docs/cultivation/alchemy.md): a furnace-made Qi Gathering or Realm-Breaking Pill (PillItem, its
 * purity on the stack) leaves toxicity by its purity, as the furnace's other pills; and the recipes learned are kept
 * through death.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class AlchemyEvents {
    private AlchemyEvents() {
    }

    /** After PillItem added its toxicity (a pill of purity 50 is the plain pill): the difference its purity makes. */
    @SubscribeEvent
    public static void onPillTaken(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getItem().getItem() instanceof PillItem)) return;
        if (!event.getItem().has(DataComponents.CUSTOM_DATA) || !event.getItem().get(DataComponents.CUSTOM_DATA).contains("purity")) return;
        int purity = AlchemyPillItem.purity(event.getItem());
        long now = player.level().getGameTime();
        var journal = CultivationService.journal(player);
        double correction = CultivationRules.PILL_TOXICITY * (AlchemyPillItem.toxicityShare(purity) - 1);
        CultivationService.setJournal(player, journal.withToxicity(Math.max(0, journal.toxicityAt(now) + correction), now));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData();
        if (old.contains(AlchemyKnowledge.KEY, Tag.TAG_LIST)) event.getEntity().getPersistentData().put(AlchemyKnowledge.KEY, old.getList(AlchemyKnowledge.KEY, Tag.TAG_STRING).copy());
    }
}
