package net.mcreator.murimblock.procedures;

import net.neoforged.bus.api.Event;

public class CloseGuiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player _player)
			_player.closeContainer();
	}
}