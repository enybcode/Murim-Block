package net.mcreator.murimblock.procedures;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class ClampQiAndMaxProcedure {
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Pre event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player) {
			if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax > entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMaxCap) {
				{
					MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
					_vars.QiMax = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMaxCap;
					_vars.markSyncDirty();
				}
			}
			if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi > entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax) {
				{
					MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
					_vars.Qi = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax;
					_vars.markSyncDirty();
				}
			}
			if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi < 0) {
				{
					MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
					_vars.Qi = 0;
					_vars.markSyncDirty();
				}
			}
		}
	}
}