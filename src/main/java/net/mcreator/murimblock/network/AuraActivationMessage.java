package net.mcreator.murimblock.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.mcreator.murimblock.procedures.AuraActive_MProcedure;
import net.mcreator.murimblock.MurimBlockMod;

@EventBusSubscriber
public record AuraActivationMessage(int eventType, int pressedms) implements CustomPacketPayload {
	public static final Type<AuraActivationMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MurimBlockMod.MODID, "key_aura_activation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AuraActivationMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, AuraActivationMessage message) -> {
		buffer.writeInt(message.eventType);
		buffer.writeInt(message.pressedms);
	}, (RegistryFriendlyByteBuf buffer) -> new AuraActivationMessage(buffer.readInt(), buffer.readInt()));

	@Override
	public Type<AuraActivationMessage> type() {
		return TYPE;
	}

	public static void handleData(final AuraActivationMessage message, final IPayloadContext context) {
		if (context.flow() == PacketFlow.SERVERBOUND) {
			context.enqueueWork(() -> {
				pressAction(context.player(), message.eventType, message.pressedms);
			}).exceptionally(e -> {
				context.connection().disconnect(Component.literal(e.getMessage()));
				return null;
			});
		}
	}

	public static void pressAction(Player entity, int type, int pressedms) {
		Level world = entity.level();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(entity.blockPosition()))
			return;
		if (type == 0) {

			AuraActive_MProcedure.execute(entity);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		MurimBlockMod.addNetworkMessage(AuraActivationMessage.TYPE, AuraActivationMessage.STREAM_CODEC, AuraActivationMessage::handleData);
	}
}