package com.murimblock.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MurimblockNetworking {
    private static final String NETWORK_VERSION = "3";

    private MurimblockNetworking() {
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(QiChargeStatePayload.TYPE, QiChargeStatePayload.STREAM_CODEC, QiChargeStatePayload::handle);
        registrar.playToServer(GuardStatePayload.TYPE, GuardStatePayload.STREAM_CODEC, GuardStatePayload::handle);
        registrar.playToServer(
                CombatModeTogglePayload.TYPE,
                CombatModeTogglePayload.STREAM_CODEC,
                CombatModeTogglePayload::handle
        );
    }
}
