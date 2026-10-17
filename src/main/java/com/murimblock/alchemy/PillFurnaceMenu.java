package com.murimblock.alchemy;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The pill furnace's fire screen (PillFurnaceScreen): no slots, only what the furnace is doing (its ContainerData,
 * kept in step by Minecraft) and its buttons, pressed through Minecraft's menu button clicks: the bellows held and let go,
 * a charcoal added from the inventory, the Qi infused held and let go.
 */
public class PillFurnaceMenu extends AbstractContainerMenu {
    public static final int BELLOWS_DOWN = 0, BELLOWS_UP = 1, ADD_FUEL = 2, QI_DOWN = 3, QI_UP = 4;
    @Nullable private final PillFurnaceBlockEntity furnace;
    public final ContainerData data;
    public final BlockPos pos;

    /** On the client, from the opening packet. */
    public PillFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, null, new SimpleContainerData(13), buf.readBlockPos());
    }

    PillFurnaceMenu(int id, Inventory inventory, PillFurnaceBlockEntity furnace, ContainerData data) {
        this(id, inventory, furnace, data, furnace.getBlockPos());
    }

    private PillFurnaceMenu(int id, Inventory inventory, @Nullable PillFurnaceBlockEntity furnace, ContainerData data, BlockPos pos) {
        super(AlchemyItems.FURNACE_MENU.get(), id);
        this.furnace = furnace;
        this.data = data;
        this.pos = pos;
        addDataSlots(data);
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (furnace == null) return false;
        switch (id) {
            case BELLOWS_DOWN -> furnace.setBellows(true);
            case BELLOWS_UP -> furnace.setBellows(false);
            case QI_DOWN -> furnace.setInfusing(player instanceof ServerPlayer sp ? sp : null, true);
            case QI_UP -> furnace.setInfusing(null, false);
            case ADD_FUEL -> {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack s = player.getInventory().getItem(i);
                    if ((s.is(Items.CHARCOAL) || s.is(Items.COAL)) && furnace.addFuel(s)) {
                        if (!player.getAbilities().instabuild) s.shrink(1);
                        return true;
                    }
                }
                return false;
            }
            default -> { return false; }
        }
        return true;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (furnace != null) {
            furnace.setBellows(false);
            furnace.setInfusing(null, false);
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getCenter()) < 64 && player.level().getBlockEntity(pos) instanceof PillFurnaceBlockEntity;
    }
}
