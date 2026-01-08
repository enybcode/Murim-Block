package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.murimblock.network.MurimBlockModVariables;

public class PillsPlayerFinishesUsingItemProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false));
		{
			MurimBlockModVariables.PlayerVariables _vars = sourceentity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
			_vars.Qi = sourceentity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi + 10;
			_vars.markSyncDirty();
		}
	}
}