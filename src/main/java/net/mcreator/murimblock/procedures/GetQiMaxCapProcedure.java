package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class GetQiMaxCapProcedure {
	public static double execute(Entity entity) {
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