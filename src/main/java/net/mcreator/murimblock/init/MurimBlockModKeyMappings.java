/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

@EventBusSubscriber(Dist.CLIENT)
public class MurimBlockModKeyMappings {
	public static final KeyMapping AURA_ACTIVATION = new KeyMapping("key.murim_block.aura_activation", GLFW.GLFW_KEY_M, "key.categories.misc");

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(AURA_ACTIVATION);
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
			}
		}
	}
}