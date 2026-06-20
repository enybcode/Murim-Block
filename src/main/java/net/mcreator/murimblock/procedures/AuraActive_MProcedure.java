package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.murimblock.network.MurimBlockModVariables;
import net.mcreator.murimblock.util.MurimVFX;

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
			if (entity.level() instanceof ServerLevel _level)
				MurimVFX.auraCollapse(_level, entity);
		} else {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.AuraActive = true;
				_vars.markSyncDirty();
			}
			if (entity.level() instanceof ServerLevel _level)
				MurimVFX.auraIgnition(_level, entity);
		}
	}
}
