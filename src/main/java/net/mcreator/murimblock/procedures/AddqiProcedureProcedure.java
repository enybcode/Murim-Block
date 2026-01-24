package net.mcreator.murimblock.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import net.mcreator.murimblock.network.MurimBlockModVariables;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.DoubleArgumentType;

public class AddqiProcedureProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		if ((commandParameterEntity(arguments, "target")) instanceof Player) {
			{
				MurimBlockModVariables.PlayerVariables _vars = (commandParameterEntity(arguments, "target")).getData(MurimBlockModVariables.PLAYER_VARIABLES);
				_vars.QiMax = (commandParameterEntity(arguments, "target")).getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax + DoubleArgumentType.getDouble(arguments, "amount");
				_vars.Qi = (commandParameterEntity(arguments, "target")).getData(MurimBlockModVariables.PLAYER_VARIABLES).QiMax;
				_vars.markSyncDirty();
			}
		}
	}

	private static Entity commandParameterEntity(CommandContext<CommandSourceStack> arguments, String parameter) {
		try {
			return EntityArgument.getEntity(arguments, parameter);
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}