package net.mcreator.murimblock.network;

@EventBusSubscriber
public record DatiahGuiButtonMessage(int buttonID, int x, int y, int z) implements CustomPacketPayload {

	public static final Type<DatiahGuiButtonMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MurimBlockMod.MODID, "datiah_gui_buttons"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DatiahGuiButtonMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, DatiahGuiButtonMessage message) -> {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
	}, (RegistryFriendlyByteBuf buffer) -> new DatiahGuiButtonMessage(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()));

	@Override
	public Type<DatiahGuiButtonMessage> type() {
		return TYPE;
	}

	public static void handleData(final DatiahGuiButtonMessage message, final IPayloadContext context) {
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
		MurimBlockMod.addNetworkMessage(DatiahGuiButtonMessage.TYPE, DatiahGuiButtonMessage.STREAM_CODEC, DatiahGuiButtonMessage::handleData);
	}

}