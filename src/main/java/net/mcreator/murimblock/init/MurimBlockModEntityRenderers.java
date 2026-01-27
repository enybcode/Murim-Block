/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.murimblock.client.renderer.CrabRenderer;

@EventBusSubscriber(Dist.CLIENT)
public class MurimBlockModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(MurimBlockModEntities.CRAB.get(), CrabRenderer::new);
	}
}