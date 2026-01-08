package net.mcreator.murimblock.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.murimblock.init.MurimBlockModGameRules;

public class PillsPlayerFinishesUsingItemProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false));
		if (world instanceof ServerLevel _serverLevel)
			_serverLevel.getGameRules().getRule(MurimBlockModGameRules.DELETED_MOD_ELEMENT).set((world instanceof ServerLevel _serverLevelGR1 ? _serverLevelGR1.getGameRules().getInt(MurimBlockModGameRules.DELETED_MOD_ELEMENT) : 0) + 10,
					world.getServer());
	}
}