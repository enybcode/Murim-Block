package net.mcreator.murimblock.network;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.util.ProblemReporter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import net.mcreator.murimblock.MurimBlockMod;

import java.util.function.Supplier;

@EventBusSubscriber
public class MurimBlockModVariables {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MurimBlockMod.MODID);
	public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables", () -> AttachmentType.serializable(() -> new PlayerVariables()).build());

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		MurimBlockMod.addNetworkMessage(SavedDataSyncMessage.TYPE, SavedDataSyncMessage.STREAM_CODEC, SavedDataSyncMessage::handleData);
		MurimBlockMod.addNetworkMessage(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handleData);
	}

	@SubscribeEvent
	public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
	}

	@SubscribeEvent
	public static void onPlayerTickUpdateSyncPlayerVariables(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && player.getData(PLAYER_VARIABLES)._syncDirty) {
			PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES)));
			player.getData(PLAYER_VARIABLES)._syncDirty = false;
		}
	}

	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
		PlayerVariables clone = new PlayerVariables();
		clone.Qi = original.Qi;
		clone.AuraActive = original.AuraActive;
		clone.QiMax = original.QiMax;
		clone.QIaffichage = original.QIaffichage;
		clone.QiTimer = original.QiTimer;
		clone.QiTimerMax = original.QiTimerMax;
		clone.PassiveQiRegen = original.PassiveQiRegen;
		clone.CultivationStage = original.CultivationStage;
		clone.MurimUIOpen = original.MurimUIOpen;
		clone.MurimUIMode = original.MurimUIMode;
		clone.QiMaxCap = original.QiMaxCap;
		clone.BT1_MeditTicks = original.BT1_MeditTicks;
		clone.AuraFX_Active = original.AuraFX_Active;
		clone.AuraFX_Style = original.AuraFX_Style;
		clone.AuraFX_TicksLeft = original.AuraFX_TicksLeft;
		clone.AuraFX_Radius = original.AuraFX_Radius;
		clone.AuraFX_Height = original.AuraFX_Height;
		clone.AuraFX_Density = original.AuraFX_Density;
		clone.AuraFX_Rings = original.AuraFX_Rings;
		if (!event.isWasDeath()) {
		}
		event.getEntity().setData(PLAYER_VARIABLES, clone);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			SavedData mapdata = MapVariables.get(event.getEntity().level());
			SavedData worlddata = WorldVariables.get(event.getEntity().level());
			if (mapdata != null)
				PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(0, mapdata));
			if (worlddata != null)
				PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(1, worlddata));
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			SavedData worlddata = WorldVariables.get(event.getEntity().level());
			if (worlddata != null)
				PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(1, worlddata));
		}
	}

	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel level) {
			WorldVariables worldVariables = WorldVariables.get(level);
			if (worldVariables._syncDirty) {
				PacketDistributor.sendToPlayersInDimension(level, new SavedDataSyncMessage(1, worldVariables));
				worldVariables._syncDirty = false;
			}
			MapVariables mapVariables = MapVariables.get(level);
			if (mapVariables._syncDirty) {
				PacketDistributor.sendToAllPlayers(new SavedDataSyncMessage(0, mapVariables));
				mapVariables._syncDirty = false;
			}
		}
	}

	public static class WorldVariables extends SavedData {
		public static final SavedDataType<WorldVariables> TYPE = new SavedDataType<>("murim_block_worldvars", ctx -> new WorldVariables(), ctx -> CompoundTag.CODEC.xmap(tag -> {
			WorldVariables instance = new WorldVariables();
			instance.read(tag, ctx.levelOrThrow().registryAccess());
			return instance;
		}, instance -> instance.save(new CompoundTag(), ctx.levelOrThrow().registryAccess())));
		boolean _syncDirty = false;

		public void read(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
		}

		public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
			return nbt;
		}

		public void markSyncDirty() {
			this.setDirty();
			this._syncDirty = true;
		}

		static WorldVariables clientSide = new WorldVariables();

		public static WorldVariables get(LevelAccessor world) {
			if (world instanceof ServerLevel level) {
				return level.getDataStorage().computeIfAbsent(WorldVariables.TYPE);
			} else {
				return clientSide;
			}
		}
	}

	public static class MapVariables extends SavedData {
		public static final SavedDataType<MapVariables> TYPE = new SavedDataType<>("murim_block_mapvars", ctx -> new MapVariables(), ctx -> CompoundTag.CODEC.xmap(tag -> {
			MapVariables instance = new MapVariables();
			instance.read(tag, ctx.levelOrThrow().registryAccess());
			return instance;
		}, instance -> instance.save(new CompoundTag(), ctx.levelOrThrow().registryAccess())));
		boolean _syncDirty = false;
		public double AuraFX_Interval = 2.0;

		public void read(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
			AuraFX_Interval = nbt.getDoubleOr("AuraFX_Interval", 0);
		}

		public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
			nbt.putDouble("AuraFX_Interval", AuraFX_Interval);
			return nbt;
		}

		public void markSyncDirty() {
			this.setDirty();
			this._syncDirty = true;
		}

		static MapVariables clientSide = new MapVariables();

		public static MapVariables get(LevelAccessor world) {
			if (world instanceof ServerLevelAccessor serverLevelAccessor) {
				return serverLevelAccessor.getLevel().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(MapVariables.TYPE);
			} else {
				return clientSide;
			}
		}
	}

	public record SavedDataSyncMessage(int dataType, SavedData data) implements CustomPacketPayload {
		public static final Type<SavedDataSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MurimBlockMod.MODID, "saved_data_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SavedDataSyncMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, SavedDataSyncMessage message) -> {
			buffer.writeInt(message.dataType);
			if (message.data instanceof MapVariables mapVariables)
				buffer.writeNbt(mapVariables.save(new CompoundTag(), buffer.registryAccess()));
			else if (message.data instanceof WorldVariables worldVariables)
				buffer.writeNbt(worldVariables.save(new CompoundTag(), buffer.registryAccess()));
		}, (RegistryFriendlyByteBuf buffer) -> {
			int dataType = buffer.readInt();
			CompoundTag nbt = buffer.readNbt();
			SavedData data = null;
			if (nbt != null) {
				data = dataType == 0 ? new MapVariables() : new WorldVariables();
				if (data instanceof MapVariables mapVariables)
					mapVariables.read(nbt, buffer.registryAccess());
				else if (data instanceof WorldVariables worldVariables)
					worldVariables.read(nbt, buffer.registryAccess());
			}
			return new SavedDataSyncMessage(dataType, data);
		});

		@Override
		public Type<SavedDataSyncMessage> type() {
			return TYPE;
		}

		public static void handleData(final SavedDataSyncMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
				context.enqueueWork(() -> {
					if (message.dataType == 0)
						MapVariables.clientSide.read(((MapVariables) message.data).save(new CompoundTag(), context.player().registryAccess()), context.player().registryAccess());
					else
						WorldVariables.clientSide.read(((WorldVariables) message.data).save(new CompoundTag(), context.player().registryAccess()), context.player().registryAccess());
				}).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}
	}

	public static class PlayerVariables implements ValueIOSerializable {
		boolean _syncDirty = false;
		public double Qi = 1.0;
		public boolean AuraActive = false;
		public double QiMax = 1.0;
		public String QIaffichage = "\"\"";
		public double QiTimer = 0;
		public double QiTimerMax = 0;
		public double PassiveQiRegen = 0;
		public double CultivationStage = 1.0;
		public boolean MurimUIOpen = false;
		public double MurimUIMode = 0;
		public double QiMaxCap = 0;
		public double BT1_MeditTicks = 0;
		public boolean AuraFX_Active = false;
		public double AuraFX_Style = 1.0;
		public double AuraFX_TicksLeft = 0;
		public double AuraFX_Radius = 0.9;
		public double AuraFX_Height = 10.0;
		public double AuraFX_Density = 16.0;
		public double AuraFX_Rings = 10.0;

		@Override
		public void serialize(ValueOutput output) {
			output.putDouble("Qi", Qi);
			output.putBoolean("AuraActive", AuraActive);
			output.putDouble("QiMax", QiMax);
			output.putString("QIaffichage", QIaffichage);
			output.putDouble("QiTimer", QiTimer);
			output.putDouble("QiTimerMax", QiTimerMax);
			output.putDouble("PassiveQiRegen", PassiveQiRegen);
			output.putDouble("CultivationStage", CultivationStage);
			output.putBoolean("MurimUIOpen", MurimUIOpen);
			output.putDouble("MurimUIMode", MurimUIMode);
			output.putDouble("QiMaxCap", QiMaxCap);
			output.putDouble("BT1_MeditTicks", BT1_MeditTicks);
			output.putBoolean("AuraFX_Active", AuraFX_Active);
			output.putDouble("AuraFX_Style", AuraFX_Style);
			output.putDouble("AuraFX_TicksLeft", AuraFX_TicksLeft);
			output.putDouble("AuraFX_Radius", AuraFX_Radius);
			output.putDouble("AuraFX_Height", AuraFX_Height);
			output.putDouble("AuraFX_Density", AuraFX_Density);
			output.putDouble("AuraFX_Rings", AuraFX_Rings);
		}

		@Override
		public void deserialize(ValueInput input) {
			Qi = input.getDoubleOr("Qi", 0);
			AuraActive = input.getBooleanOr("AuraActive", false);
			QiMax = input.getDoubleOr("QiMax", 0);
			QIaffichage = input.getStringOr("QIaffichage", "");
			QiTimer = input.getDoubleOr("QiTimer", 0);
			QiTimerMax = input.getDoubleOr("QiTimerMax", 0);
			PassiveQiRegen = input.getDoubleOr("PassiveQiRegen", 0);
			CultivationStage = input.getDoubleOr("CultivationStage", 0);
			MurimUIOpen = input.getBooleanOr("MurimUIOpen", false);
			MurimUIMode = input.getDoubleOr("MurimUIMode", 0);
			QiMaxCap = input.getDoubleOr("QiMaxCap", 0);
			BT1_MeditTicks = input.getDoubleOr("BT1_MeditTicks", 0);
			AuraFX_Active = input.getBooleanOr("AuraFX_Active", false);
			AuraFX_Style = input.getDoubleOr("AuraFX_Style", 0);
			AuraFX_TicksLeft = input.getDoubleOr("AuraFX_TicksLeft", 0);
			AuraFX_Radius = input.getDoubleOr("AuraFX_Radius", 0);
			AuraFX_Height = input.getDoubleOr("AuraFX_Height", 0);
			AuraFX_Density = input.getDoubleOr("AuraFX_Density", 0);
			AuraFX_Rings = input.getDoubleOr("AuraFX_Rings", 0);
		}

		public void markSyncDirty() {
			_syncDirty = true;
		}
	}

	public record PlayerVariablesSyncMessage(PlayerVariables data) implements CustomPacketPayload {
		public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MurimBlockMod.MODID, "player_variables_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> {
			TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
			message.data.serialize(output);
			buffer.writeNbt(output.buildResult());
		}, (RegistryFriendlyByteBuf buffer) -> {
			PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables());
			message.data.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, buffer.registryAccess(), buffer.readNbt()));
			return message;
		});

		@Override
		public Type<PlayerVariablesSyncMessage> type() {
			return TYPE;
		}

		public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
				context.enqueueWork(() -> {
					TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, context.player().registryAccess());
					message.data.serialize(output);
					context.player().getData(PLAYER_VARIABLES).deserialize(TagValueInput.create(ProblemReporter.DISCARDING, context.player().registryAccess(), output.buildResult()));
				}).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}
	}
}