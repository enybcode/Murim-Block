package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class OverlayMurimOnKeyPressedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).MurimUIOpen == false) {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.MurimUIMode = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).MurimUIMode + 1;
				_vars.markSyncDirty();
			}
		} else {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.MurimUIOpen = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).MurimUIOpen == false;
				_vars.markSyncDirty();
			}
		}
	}
}