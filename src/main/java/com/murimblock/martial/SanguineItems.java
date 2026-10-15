package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.item.MurimItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Sanguine Art's items: the blood vial, filled from blood with a glass bottle. */
public final class SanguineItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredItem<BloodVialItem> BLOOD_VIAL = ITEMS.register("blood_vial",
            () -> new BloodVialItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));

    private SanguineItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(MurimItems.MURIMBLOCK_TAB.getKey())) event.accept(BLOOD_VIAL.get());
        });
        NeoForge.EVENT_BUS.addListener(SanguineItems::fill);
    }

    /**
     * A glass bottle used on blood (the Blood Iceberg's channel and falls) fills a blood vial, as on water. Vanilla's bottle
     * only knows water, so the use is taken here first.
     */
    static void fill(PlayerInteractEvent.RightClickItem event) {
        var player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!held.is(Items.GLASS_BOTTLE)) return;
        var level = event.getLevel();
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(SanguineFluids.BLOOD.get())) return;
        level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1F, .8F);
        if (!level.isClientSide) player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, player, new ItemStack(BLOOD_VIAL.get())));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
