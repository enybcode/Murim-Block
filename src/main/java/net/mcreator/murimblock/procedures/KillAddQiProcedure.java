package net.mcreator.murimblock.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class KillAddQiProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof ServerPlayer _plr0 && _plr0.level() instanceof ServerLevel _serverLevel0
				&& _plr0.getAdvancements().getOrStartProgress(_serverLevel0.getServer().getAdvancements().get(ResourceLocation.parse("murim_block:open_eyes"))).isDone()) {
			if (sourceentity instanceof Player == (entity instanceof Mob _mobEnt2 && _mobEnt2.isAggressive())) {
				{
					MurimBlockModVariables.PlayerVariables _vars = sourceentity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
					_vars.QiMax = sourceentity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax + 5;
					_vars.markSyncDirty();
				}
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.LAVA, (sourceentity.getX()), (sourceentity.getY()), (sourceentity.getZ()), 5, 0, 0, 0, 10);
			}
		}
	}
}