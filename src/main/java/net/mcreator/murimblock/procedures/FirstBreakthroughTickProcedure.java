package net.mcreator.murimblock.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementHolder;

import net.mcreator.murimblock.network.MurimBlockModVariables;
import net.mcreator.murimblock.util.MurimVFX;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FirstBreakthroughTickProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player) {
			if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage == 2) {
				if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax >= 100) {
					if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi >= 100) {
						if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).AuraActive == true) {
							{
								MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
								_vars.BT1_MeditTicks = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).BT1_MeditTicks + 1;
								_vars.markSyncDirty();
							}
							if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).BT1_MeditTicks >= 2400) {
								{
									MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
									_vars.CultivationStage = 3;
									_vars.markSyncDirty();
								}
								if (entity instanceof ServerPlayer _player && _player.level() instanceof ServerLevel _level) {
									AdvancementHolder _adv = _level.getServer().getAdvancements().get(ResourceLocation.parse("murim_block:first_breakthrough"));
									if (_adv != null) {
										AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
										if (!_ap.isDone()) {
											for (String criteria : _ap.getRemainingCriteria())
												_player.getAdvancements().award(_adv, criteria);
										}
									}
								}
								{
									MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
									_vars.BT1_MeditTicks = 0;
									_vars.markSyncDirty();
								}
								if (world instanceof ServerLevel _level)
									MurimVFX.breakthroughAscension(_level, entity);
							}
						} else {
							{
								MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
								_vars.BT1_MeditTicks = 0;
								_vars.markSyncDirty();
							}
						}
					}
				} else {
					{
						MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
						_vars.BT1_MeditTicks = 0;
						_vars.markSyncDirty();
					}
				}
			}
		}
	}
}
