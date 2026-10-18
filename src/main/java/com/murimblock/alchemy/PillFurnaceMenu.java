package com.murimblock.alchemy;

import com.murimblock.world.plant.HerbItem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The pill furnace's screen (PillFurnaceScreen), in Minecraft's own look: its slots (the water, the three herbs in their
 * order, the charcoal, the pills), the player's inventory, what the furnace is doing (its ContainerData, kept in step
 * by Minecraft), the recipes the player knows, and its buttons, pressed through Minecraft's menu button clicks: the
 * bellows and the Qi held and let go, and a known recipe chosen in the book, which puts its water and herbs in from the
 * inventory.
 */
public class PillFurnaceMenu extends AbstractContainerMenu {
    public static final int BELLOWS_DOWN = 0, BELLOWS_UP = 1, QI_DOWN = 3, QI_UP = 4, FILL_RECIPE = 10;
    /** Where the furnace's own panel starts on the screen: the recipe book is on its left. */
    public static final int PANEL_X = 149;
    /** The slots' places in the furnace's panel (the item, a pixel inside the slot's frame). */
    public static final int[][] SLOT_AT = {{8, 18}, {32, 18}, {50, 18}, {68, 18}, {116, 56}, {116, 18}};
    public static final int INVENTORY_Y = 94;
    private static final int FURNACE_SLOTS = PillFurnaceBlockEntity.SLOTS, INVENTORY_END = FURNACE_SLOTS + 36;

    @Nullable private final PillFurnaceBlockEntity furnace;
    public final ContainerData data;
    public final BlockPos pos;
    private final DataSlot known;

    /** On the client, from the opening packet. */
    public PillFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, null, new SimpleContainer(FURNACE_SLOTS), new SimpleContainerData(13), buf.readBlockPos(), DataSlot.standalone());
    }

    PillFurnaceMenu(int id, Inventory inventory, PillFurnaceBlockEntity furnace, ContainerData data, Player player) {
        this(id, inventory, furnace, furnace.items(), data, furnace.getBlockPos(), new DataSlot() {
            @Override public int get() { return AlchemyKnowledge.mask(player); }
            @Override public void set(int value) { }
        });
        furnace.claim(player);
    }

    private PillFurnaceMenu(int id, Inventory inventory, @Nullable PillFurnaceBlockEntity furnace, Container items, ContainerData data, BlockPos pos, DataSlot known) {
        super(AlchemyItems.FURNACE_MENU.get(), id);
        this.furnace = furnace;
        this.data = data;
        this.pos = pos;
        this.known = known;
        addSlot(new Slot(items, PillFurnaceBlockEntity.WATER_SLOT, x(0), SLOT_AT[0][1]) {
            @Override public boolean mayPlace(ItemStack stack) {
                return PillFurnaceBlockEntity.isWater(stack) && (furnace == null || !furnace.hasWater() && furnace.phase() == PillFurnaceBlockEntity.PREPARE);
            }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < PillFurnaceBlockEntity.HERB_SLOTS; i++) addSlot(new HerbSlot(items, i));
        addSlot(new Slot(items, PillFurnaceBlockEntity.FUEL_SLOT, x(4), SLOT_AT[4][1]) {
            @Override public boolean mayPlace(ItemStack stack) { return PillFurnaceBlockEntity.isFuel(stack); }
        });
        addSlot(new Slot(items, PillFurnaceBlockEntity.OUT_SLOT, x(5), SLOT_AT[5][1]) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, 9 + r * 9 + c, PANEL_X + 8 + c * 18, INVENTORY_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, PANEL_X + 8 + c * 18, INVENTORY_Y + 58));
        addDataSlots(data);
        addDataSlot(known);
    }

    private static int x(int slot) {
        return PANEL_X + SLOT_AT[slot][0];
    }

    /** A herb's place: one herb, put in only in the recipe's order, taken back only from the last one in. */
    private final class HerbSlot extends Slot {
        private final int place;

        HerbSlot(Container items, int place) {
            super(items, PillFurnaceBlockEntity.HERB_SLOT + place, x(1 + place), SLOT_AT[1 + place][1]);
            this.place = place;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return furnace == null ? stack.getItem() instanceof HerbItem : furnace.accepts(place, stack);
        }

        @Override public boolean mayPickup(Player player) {
            if (furnace == null) return true;
            return furnace.phase() == PillFurnaceBlockEntity.PREPARE
                    && (place == PillFurnaceBlockEntity.HERB_SLOTS - 1 || container.getItem(getContainerSlot() + 1).isEmpty());
        }

        @Override public int getMaxStackSize() { return 1; }
    }

    /** Whether the player knows this recipe (the book's buttons). */
    public boolean knows(AlchemyRecipes.Recipe recipe) {
        return (known.get() >> recipe.index() & 1) != 0;
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (furnace == null) return false;
        switch (id) {
            case BELLOWS_DOWN -> furnace.setBellows(true);
            case BELLOWS_UP -> furnace.setBellows(false);
            case QI_DOWN -> furnace.setInfusing(player instanceof ServerPlayer sp ? sp : null, true);
            case QI_UP -> furnace.setInfusing(null, false);
            default -> {
                AlchemyRecipes.Recipe recipe = AlchemyRecipes.byIndex(id - FILL_RECIPE);
                return recipe != null && fill(player, recipe);
            }
        }
        return true;
    }

    /** A known recipe chosen in the book: its water and herbs, in order, from the inventory (what is in it and does not belong goes back). */
    private boolean fill(Player player, AlchemyRecipes.Recipe recipe) {
        if (furnace.phase() != PillFurnaceBlockEntity.PREPARE || !AlchemyKnowledge.knows(player, recipe)) return false;
        furnace.claim(player);
        Container items = furnace.items();
        Inventory inv = player.getInventory();
        List<ItemStack> in = furnace.herbs();
        boolean onTheWay = in.size() <= recipe.herbs().size();
        for (int i = 0; onTheWay && i < in.size(); i++) onTheWay = in.get(i).is(recipe.herbs().get(i).get());
        if (!onTheWay) for (int i = PillFurnaceBlockEntity.HERB_SLOTS - 1; i >= 0; i--) {
            ItemStack back = items.removeItemNoUpdate(PillFurnaceBlockEntity.HERB_SLOT + i);
            if (!back.isEmpty() && !inv.add(back)) player.drop(back, false);
        }
        items.setChanged();
        if (!furnace.hasWater()) {
            ItemStack old = items.getItem(PillFurnaceBlockEntity.WATER_SLOT);
            int w = find(inv, PillFurnaceBlockEntity::isWater);
            if (w < 0) return true;
            if (!old.isEmpty()) {
                items.setItem(PillFurnaceBlockEntity.WATER_SLOT, ItemStack.EMPTY);
                if (!inv.add(old)) player.drop(old, false);
            }
            items.setItem(PillFurnaceBlockEntity.WATER_SLOT, inv.removeItem(w, 1));
        }
        for (int i = furnace.herbs().size(); i < recipe.herbs().size(); i++) {
            var wanted = recipe.herbs().get(i).get();
            int best = -1;
            for (int k = 0; k < inv.getContainerSize(); k++) {
                ItemStack s = inv.getItem(k);
                if (s.is(wanted) && (best < 0 || HerbItem.purity(s) > HerbItem.purity(inv.getItem(best)))) best = k;
            }
            if (best < 0 || !furnace.accepts(i, inv.getItem(best))) break;
            items.setItem(PillFurnaceBlockEntity.HERB_SLOT + i, inv.removeItem(best, 1));
        }
        return true;
    }

    private static int find(Inventory inv, java.util.function.Predicate<ItemStack> what) {
        for (int k = 0; k < inv.getContainerSize(); k++) if (what.test(inv.getItem(k))) return k;
        return -1;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (furnace != null) {
            furnace.setBellows(false);
            furnace.setInfusing(null, false);
        }
    }

    /** Shift-click: from the furnace to the inventory; from the inventory to the slot it belongs in, else across. */
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < FURNACE_SLOTS) {
            if (!moveItemStackTo(stack, FURNACE_SLOTS, INVENTORY_END, true)) return ItemStack.EMPTY;
        } else if (PillFurnaceBlockEntity.isWater(stack) && slots.get(PillFurnaceBlockEntity.WATER_SLOT).mayPlace(stack)) {
            if (!moveItemStackTo(stack, PillFurnaceBlockEntity.WATER_SLOT, PillFurnaceBlockEntity.WATER_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (PillFurnaceBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, PillFurnaceBlockEntity.FUEL_SLOT, PillFurnaceBlockEntity.FUEL_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof HerbItem && moveItemStackTo(stack, PillFurnaceBlockEntity.HERB_SLOT,
                PillFurnaceBlockEntity.HERB_SLOT + PillFurnaceBlockEntity.HERB_SLOTS, false)) {
            // went in as the next herb
        } else if (index < FURNACE_SLOTS + 27) {
            if (!moveItemStackTo(stack, FURNACE_SLOTS + 27, INVENTORY_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, FURNACE_SLOTS, FURNACE_SLOTS + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getCenter()) < 64 && player.level().getBlockEntity(pos) instanceof PillFurnaceBlockEntity;
    }
}
