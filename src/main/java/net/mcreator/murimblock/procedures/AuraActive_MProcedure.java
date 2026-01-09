package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class AuraActive_MProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).AuraActive == true) {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.AuraActive = false;
				_vars.markSyncDirty();
			}
		} else {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.AuraActive = true;
				_vars.markSyncDirty();
			}
		}
	}
}