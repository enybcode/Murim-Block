package net.mcreator.murimblock.procedures;

import net.neoforged.bus.api.Event;

@EventBusSubscriber
public class FirstBreakthroughTickProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity().level(), event.getEntity());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

private static void execute(
@Nullable Event event,
LevelAccessor world,
Entity entity ) {
if (
entity == null ) return ;
if (entity instanceof Player) {if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).CultivationStage==2) {if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax>=100) {if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).Qi>=100) {if (entity.getData(MurimBlockModVariables.PLAYER_VARIABLES).AuraActive==true) {if (>=2400) {{MurimBlockModVariables.PlayerVariables _vars = entity.getData(MurimBlockModVariables.PLAYER_VARIABLES);
_vars.CultivationStage = 3;
_vars.markSyncDirty();}if(entity instanceof ServerPlayer _player && _player.level() instanceof ServerLevel _level) {
AdvancementHolder _adv = _level.getServer().getAdvancements().get(ResourceLocation.parse("murim_block:deleted_mod_element"));
if (_adv != null) {
AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
if (!_ap.isDone()) {
for (String criteria : _ap.getRemainingCriteria())
_player.getAdvancements().award(_adv, criteria);
}
}
}for (int index0 = 0; index0<10; index0++) {if (world instanceof ServerLevel _level)
_level.sendParticles(ParticleTypes.SOUL, (entity.getX()), (entity.getY()+Math.random()), (entity.getZ()), 60, 0, 2, 0, 0.25);if (world instanceof ServerLevel _level)
_level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, (entity.getX()), (entity.getY()+Math.random()), (entity.getZ()), 60, 0, 2, 0, 0.25);}}}else{}}}else{}}}
}
}