package com.murimblock.network;

import com.murimblock.Murimblock;
import com.murimblock.combat.CombatService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CombatModeTogglePayload() implements CustomPacketPayload {
    public static final CombatModeTogglePayload INSTANCE = new CombatModeTogglePayload();
    public static final CustomPacketPayload.Type<CombatModeTogglePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "combat_mode_toggle")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CombatModeTogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CombatModeTogglePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            CombatService.toggleCombatMode(player);
        }
    }
}
