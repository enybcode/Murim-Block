package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class DisplaySwordFirstProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Unlocked_FirstSwordArt) {
			return true;
		}
		return false;
	}
}
