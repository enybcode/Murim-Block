/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.murimblock.client.gui.TenchniqueguiScreen;
import net.mcreator.murimblock.client.gui.DatiahGuiScreen;

@EventBusSubscriber(Dist.CLIENT)
public class MurimBlockModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(MurimBlockModMenus.DATIAH_GUI.get(), DatiahGuiScreen::new);
		event.register(MurimBlockModMenus.TENCHNIQUEGUI.get(), TenchniqueguiScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}