package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightSkillService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TechniqueChangePayload(int slot, String skill, int bookSlot) implements CustomPacketPayload {
    public static final Type<TechniqueChangePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "technique_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TechniqueChangePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TechniqueChangePayload::slot,
            ByteBufCodecs.stringUtf8(128), TechniqueChangePayload::skill,
            ByteBufCodecs.VAR_INT, TechniqueChangePayload::bookSlot, TechniqueChangePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TechniqueChangePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            EpicFightSkillService.change(player, payload.slot(), payload.skill(), payload.bookSlot());
        }
    }
}
