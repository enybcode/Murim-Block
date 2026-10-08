package com.murimblock.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MurimblockNetworking {
    private static final String NETWORK_VERSION = "6";

    private MurimblockNetworking() {
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(TechniqueChangePayload.TYPE, TechniqueChangePayload.STREAM_CODEC, TechniqueChangePayload::handle);
        registrar.playToServer(QiChargeStatePayload.TYPE, QiChargeStatePayload.STREAM_CODEC, QiChargeStatePayload::handle);
        registrar.playToServer(
                CombatModeTogglePayload.TYPE,
                CombatModeTogglePayload.STREAM_CODEC,
                CombatModeTogglePayload::handle
        );
    }
}
