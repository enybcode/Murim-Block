package net.mcreator.murimblock.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.murimblock.init.MurimBlockModGameRules;

import javax.annotation.Nullable;

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