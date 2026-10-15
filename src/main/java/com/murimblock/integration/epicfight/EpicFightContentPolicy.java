package com.murimblock.integration.epicfight;

import com.google.gson.JsonElement;
import com.murimblock.Murimblock;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
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

/**
 * Retire engine items without unregistering IDs needed to load existing worlds: Epic Fight's, and those of its add-on
 * Dawn Day, whose animations we use (their skill books, potions, gems and materials). Their <b>weapons</b> are allowed
 * again since 15/10/2026 (Enzo): in the Weapons tab (item/WeaponsTab), in inventories, with their recipes.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class EpicFightContentPolicy {
    private EpicFightContentPolicy() { }

    public static boolean isNative(ResourceLocation id) {
        return id != null && (id.getNamespace().equals("epicfight") || id.getNamespace().equals(DAWN_DAY));
    }

    /** Dawn Day's namespace (Epic Fight: Dawn Day, a required dependency for its animations only). */
    public static final String DAWN_DAY = "epicfight_dd";

    public static boolean blocked(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isNative(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return !com.murimblock.item.WeaponsTab.isWeapon(stack.getItem());
        // Dawn Day's potions are vanilla potion items holding its own potion types.
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        return potion != null && potion.potion().flatMap(Holder::unwrapKey).map(key -> isNative(key.location())).orElse(false);
    }

    public static boolean blockedRecipe(ResourceLocation id, JsonElement json) {
        ResourceLocation made = result(json);
        // A recipe for one of their weapons stays, whatever its own name.
        if (made != null && isNative(made)) return !BuiltInRegistries.ITEM.getOptional(made).map(com.murimblock.item.WeaponsTab::isWeapon).orElse(false);
        return isNative(id);
    }

    /** The item a recipe makes, or null. */
    private static ResourceLocation result(JsonElement json) {
        if (!json.isJsonObject()) return null;
        JsonElement result = json.getAsJsonObject().get("result");
        if (result == null) return null;
        JsonElement item = result.isJsonObject() ? result.getAsJsonObject().get("id") : result;
        if (item == null && result.isJsonObject()) item = result.getAsJsonObject().get("item");
        return item != null && item.isJsonPrimitive() && item.getAsJsonPrimitive().isString() ? ResourceLocation.tryParse(item.getAsString()) : null;
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
        Stream.concat(event.getParentEntries().stream(), event.getSearchEntries().stream())
                .filter(EpicFightContentPolicy::blocked).toList().forEach(stack ->
                        event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
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
