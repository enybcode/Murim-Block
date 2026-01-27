package net.mcreator.murimblock.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class GetQiMaxCapProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity());
	}

	public static double execute(Entity entity) {
		return execute(null, entity);
	}

	private static double execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return 0;
		if (1 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.QiMaxCap = 50;
				_vars.markSyncDirty();
			}
		} else if (2 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.QiMaxCap = 100;
				_vars.markSyncDirty();
			}
		} else if (3 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.QiMaxCap = 200;
				_vars.markSyncDirty();
			}
		} else if (4 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.QiMaxCap = 400;
				_vars.markSyncDirty();
			}
		}
		return entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMaxCap;
	}
}