package net.mcreator.murimblock.command;

@EventBusSubscriber
public class CheckqiCommand {

	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("checkqi").requires(s -> s.hasPermission(2))

		);
	}

}