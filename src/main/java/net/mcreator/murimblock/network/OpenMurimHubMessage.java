package net.mcreator.murimblock.network;

import net.mcreator.murimblock.MurimBlockMod;

@EventBusSubscriber
public record OpenMurimHubMessage(int eventType, int pressedms) implements CustomPacketPayload {

	public static final Type<OpenMurimHubMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MurimBlockMod.MODID, "key_open_murim_hub"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenMurimHubMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, OpenMurimHubMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new OpenMurimHubMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<OpenMurimHubMessage> type() {
		return TYPE;
	}

	public static void handleData(final OpenMurimHubMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		MurimBlockMod.addNetworkMessage(OpenMurimHubMessage.TYPE, OpenMurimHubMessage.STREAM_CODEC, OpenMurimHubMessage::handleData);
	}

}