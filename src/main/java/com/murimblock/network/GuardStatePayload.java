package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.combat.MeleeCombatService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GuardStatePayload(boolean holding) implements CustomPacketPayload {
    public static final Type<GuardStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "guard_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuardStatePayload> STREAM_CODEC = ByteBufCodecs.BOOL
            .map(GuardStatePayload::new, GuardStatePayload::holding).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(GuardStatePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            MeleeCombatService.requestGuard(player, payload.holding());
        }
    }
}
