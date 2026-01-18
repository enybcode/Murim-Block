/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.mcreator.murimblock.network.OpenMurimHubMessage;
import net.mcreator.murimblock.network.AuraActivationMessage;

@EventBusSubscriber(Dist.CLIENT)
public class MurimBlockModKeyMappings {
	public static final KeyMapping AURA_ACTIVATION = new KeyMapping("key.murim_block.aura_activation", GLFW.GLFW_KEY_M, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				ClientPacketDistributor.sendToServer(new AuraActivationMessage(0, 0));
				AuraActivationMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping OPEN_MURIM_HUB = new KeyMapping("key.murim_block.open_murim_hub", GLFW.GLFW_KEY_K, "key.categories.misc") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				ClientPacketDistributor.sendToServer(new OpenMurimHubMessage(0, 0));
				OpenMurimHubMessage.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(AURA_ACTIVATION);
		event.register(OPEN_MURIM_HUB);
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				AURA_ACTIVATION.consumeClick();
				OPEN_MURIM_HUB.consumeClick();
			}
		}
	}
}