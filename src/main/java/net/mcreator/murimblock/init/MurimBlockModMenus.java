/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import net.mcreator.murimblock.world.inventory.TenchniqueguiMenu;
import net.mcreator.murimblock.world.inventory.DatiahGuiMenu;
import net.mcreator.murimblock.network.MenuStateUpdateMessage;
import net.mcreator.murimblock.MurimBlockMod;

import java.util.Map;

public class MurimBlockModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, MurimBlockMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<DatiahGuiMenu>> DATIAH_GUI = REGISTRY.register("datiah_gui", () -> IMenuTypeExtension.create(DatiahGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<TenchniqueguiMenu>> TENCHNIQUEGUI = REGISTRY.register("tenchniquegui", () -> IMenuTypeExtension.create(TenchniqueguiMenu::new));

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof MurimBlockModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				ClientPacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, T defaultValue) {
			try {
				return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			} catch (ClassCastException e) {
				return defaultValue;
			}
		}
	}
}