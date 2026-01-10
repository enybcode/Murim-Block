package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class OpeneyesCultivationNameProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ServerPlayer _plr0 && _plr0.level() instanceof ServerLevel _serverLevel0
				&& _plr0.getAdvancements().getOrStartProgress(_serverLevel0.getServer().getAdvancements().get(ResourceLocation.parse("murim_block:open_eyes"))).isDone()) {
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.CultivationStage = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage + 1;
				_vars.markSyncDirty();
			}
		}
	}
}