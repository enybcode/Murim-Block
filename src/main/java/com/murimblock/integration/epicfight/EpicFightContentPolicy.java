package com.murimblock.integration.epicfight;

import com.google.gson.JsonElement;
import com.murimblock.Murimblock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Retire engine items without unregistering IDs needed to load existing worlds. */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class EpicFightContentPolicy {
    private EpicFightContentPolicy() { }

    public static boolean isNative(ResourceLocation id) {
        return id != null && id.getNamespace().equals("epicfight");
    }

    public static boolean blocked(ItemStack stack) {
        return !stack.isEmpty() && isNative(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean blockedRecipe(ResourceLocation id, JsonElement json) {
        if (isNative(id)) return true;
        if (!json.isJsonObject()) return false;
        JsonElement result = json.getAsJsonObject().get("result");
        if (result == null) return false;
        JsonElement item = result.isJsonObject() ? result.getAsJsonObject().get("id") : result;
        if (item == null && result.isJsonObject()) item = result.getAsJsonObject().get("item");
        return item != null && item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()
                && isNative(ResourceLocation.tryParse(item.getAsString()));
    }

    public static int clear(Container container) {
        int removed = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (blocked(container.getItem(i))) {
                removed += container.getItem(i).getCount();
                container.setItem(i, ItemStack.EMPTY);
            }
        }
        if (removed > 0) container.setChanged();
        return removed;
    }

    private static boolean clearMenu(AbstractContainerMenu menu) {
        boolean changed = false;
        for (var slot : menu.slots) {
            if (blocked(slot.getItem())) { slot.set(ItemStack.EMPTY); changed = true; }
        }
        if (blocked(menu.getCarried())) { menu.setCarried(ItemStack.EMPTY); changed = true; }
        return changed;
    }

    private static void clearPlayer(ServerPlayer player) {
        boolean changed = clear(player.getInventory()) + clear(player.getEnderChestInventory()) > 0;
        changed |= clearMenu(player.containerMenu);
        if (changed) { player.inventoryMenu.broadcastChanges(); player.containerMenu.broadcastChanges(); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void creative(BuildCreativeModeTabContentsEvent event) {
        java.util.stream.Stream.concat(event.getParentEntries().stream(), event.getSearchEntries().stream())
                .filter(EpicFightContentPolicy::blocked).toList().forEach(stack ->
                        event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clearPlayer(player);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) clearPlayer(player);
    }

    @SubscribeEvent
    public static void open(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer && clearMenu(event.getContainer())) event.getContainer().broadcastChanges();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ItemEntity item && blocked(item.getItem())) event.setCanceled(true);
        if (event.getEntity() instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (blocked(living.getItemBySlot(slot))) living.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public static void equipment(LivingEquipmentChangeEvent event) {
        if (!event.getEntity().level().isClientSide() && blocked(event.getTo())) {
            event.getEntity().setItemSlot(event.getSlot(), ItemStack.EMPTY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickItem event) {
        if (blocked(event.getItemStack())) { event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true); }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (blocked(event.getItemStack())) { event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true); }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        if (blocked(event.getEntity().getMainHandItem())) event.setCanceled(true);
    }
}
