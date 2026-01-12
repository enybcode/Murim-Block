package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class GetQiMaxCapProcedure {
	public static double execute(Entity entity) {
		if (entity == null)
			return 0;
		double cap = 0;
		cap = 400;
		if (0 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			cap = 50;
		} else if (1 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			cap = 100;
		} else if (2 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			cap = 200;
		} else if (3 == entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage) {
			cap = 400;
		} else {
			cap = 400;
		}
		return cap;
	}
}