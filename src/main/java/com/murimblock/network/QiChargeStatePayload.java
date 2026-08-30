package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.qi.charge.QiChargeService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QiChargeStatePayload(boolean charging) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<QiChargeStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "qi_charge_state")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, QiChargeStatePayload> STREAM_CODEC = ByteBufCodecs.BOOL
            .map(QiChargeStatePayload::new, QiChargeStatePayload::charging)
            .cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QiChargeStatePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            QiChargeService.setCharging(player, payload.charging());
        }
    }
}
