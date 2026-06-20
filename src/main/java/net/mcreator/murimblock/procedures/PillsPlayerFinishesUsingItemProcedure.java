package net.mcreator.murimblock.procedures;

import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.mcreator.murimblock.network.MurimBlockModVariables;
import net.mcreator.murimblock.util.MurimVFX;

import javax.annotation.Nullable;

@EventBusSubscriber
public class PillsPlayerFinishesUsingItemProcedure {
	@SubscribeEvent
	public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity());
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false));
		if (entity.level() instanceof ServerLevel _level)
			MurimVFX.pillRefinement(_level, entity);
		{
			MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
			_vars.QiMax = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax + 5;
			_vars.CultivationStage = 2;
			_vars.markSyncDirty();
		}
	}
}
