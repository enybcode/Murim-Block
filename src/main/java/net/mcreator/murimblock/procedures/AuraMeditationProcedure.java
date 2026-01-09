package net.mcreator.murimblock.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.murimblock.network.MurimBlockModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class AuraMeditationProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).AuraActive == true) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 2, false, false));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 10, false, false));
			if (entity instanceof ServerPlayer _plr3 && _plr3.level() instanceof ServerLevel _serverLevel3
					&& _plr3.getAdvancements().getOrStartProgress(_serverLevel3.getServer().getAdvancements().get(ResourceLocation.parse("murim_block:open_eyes"))).isDone()) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SOUL, x, y, z, 10, 0.5, 1.8, 0.4, 0.05);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 10, 0.2, 1.8, 0.4, 0.05);
			}
			{
				MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.QiTimer = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiTimer + 1;
				_vars.markSyncDirty();
			}
			if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiTimer >= 200) {
				{
					MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
					_vars.Qi = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi * 1.5;
					_vars.QiMax = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax * 1.03;
					_vars.QiTimer = 0;
					_vars.markSyncDirty();
				}
			}
			if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi > entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax) {
				{
					MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
					_vars.Qi = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax;
					_vars.markSyncDirty();
				}
			}
		}
	}
}