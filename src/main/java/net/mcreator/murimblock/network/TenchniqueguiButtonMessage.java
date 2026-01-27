package net.mcreator.murimblock.network;

@EventBusSubscriber
public record TenchniqueguiButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {

	public static final Type<TenchniqueguiButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MurimBlockMod.MODID, "tenchniquegui_buttons"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TenchniqueguiButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, TenchniqueguiButtonMessage message) -> {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}, (RegistryFriendlyByteBuf buffer) -> new TenchniqueguiButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

	@Override
	public Type<TenchniqueguiButtonMessage> type() {
		return TYPE;
	}

	public static void handleData(final TenchniqueguiButtonMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> handleButtonAction(context.player(), message.buttonID, message.x, message.y, message.z)).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
		Level world = entity.level();

		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;

		if (buttonID == 2) {

			OpenGuiskillProcedure.execute(world, entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		MurimBlockMod.addNetworkMessage(TenchniqueguiButtonMessage.TYPE, TenchniqueguiButtonMessage.STREAM_CODEC, TenchniqueguiButtonMessage::handleData);
	}

}