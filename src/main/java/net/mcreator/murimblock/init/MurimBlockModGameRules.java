/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.GameRules;

@EventBusSubscriber
public class MurimBlockModGameRules {
	public static GameRules.Key<GameRules.IntegerValue> QI;

	@SubscribeEvent
	public static void registerGameRules(FMLCommonSetupEvent event) {
		QI = GameRules.register("qi", GameRules.Category.PLAYER, GameRules.IntegerValue.create(0));
	}
}