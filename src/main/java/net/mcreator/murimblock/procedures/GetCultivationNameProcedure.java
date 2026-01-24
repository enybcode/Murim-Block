package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class GetCultivationNameProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		String setrealname = "";
		if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage == 0) {
			setrealname = "Not awakened";
		} else if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage == 1) {
			setrealname = "Qi Sensing";
		} else if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage == 2) {
			setrealname = "Qi Guiding";
		} else if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage == 3) {
			setrealname = "Qi Condensation";
		} else {
			setrealname = "Unknow Realm";
		}
		return setrealname;
	}
}