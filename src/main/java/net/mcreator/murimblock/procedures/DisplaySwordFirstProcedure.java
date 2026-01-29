package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;

public class DisplaySwordFirstProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (entity instanceof Player && entity instanceof ServerPlayer _plr1 && _plr1.level() instanceof ServerLevel _serverLevel1
				&& _plr1.getAdvancements().getOrStartProgress(_serverLevel1.getServer().getAdvancements().get(ResourceLocation.parse("murim_block:firstswordart"))).isDone()) {
			return true;
		}
		return false;
	}
}