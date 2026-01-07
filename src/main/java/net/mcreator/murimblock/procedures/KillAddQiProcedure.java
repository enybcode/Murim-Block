package net.mcreator.murimblock.procedures;

import net.neoforged.bus.api.Event;

@EventBusSubscriber
public class KillAddQiProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		execute(null, world, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof Player == (entity instanceof Mob _mobEnt1 && _mobEnt1.isAggressive())) {
			if (world instanceof ServerLevel _serverLevel)
				_serverLevel.getGameRules().getRule(MurimBlockModGameRules.QI).set((world instanceof ServerLevel _serverLevelGR2 ? _serverLevelGR2.getGameRules().getInt(MurimBlockModGameRules.QI) : 0) + 5, world.getServer());
		}
	}
}