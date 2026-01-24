package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class QiBarProcedureProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "Qi:" + Math.round(entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi) + "/" + Math.round(entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax);
	}
}