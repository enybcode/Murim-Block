package com.murimblock.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MurimblockNetworking {
    private static final String NETWORK_VERSION = "10";

    private MurimblockNetworking() {
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(QiChargeStatePayload.TYPE, QiChargeStatePayload.STREAM_CODEC, QiChargeStatePayload::handle);
        registrar.playToServer(
                CombatModeTogglePayload.TYPE,
                CombatModeTogglePayload.STREAM_CODEC,
                CombatModeTogglePayload::handle
        );
        registrar.playToServer(KnockdownActionPayload.TYPE, KnockdownActionPayload.STREAM_CODEC, KnockdownActionPayload::handle);
        registrar.playToServer(TechniquePayloads.Cast.TYPE, TechniquePayloads.Cast.STREAM_CODEC, TechniquePayloads.Cast::handle);
        registrar.playToServer(TechniquePayloads.Assign.TYPE, TechniquePayloads.Assign.STREAM_CODEC, TechniquePayloads.Assign::handle);
        registrar.playToClient(TechniquePayloads.Cooldown.TYPE, TechniquePayloads.Cooldown.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.CombatBarClient.cooldown(payload.technique(), payload.ticks()));
        // Client only: only ever sent to clients, so the client classes are never loaded on a dedicated server.
        registrar.playToClient(FinishCinematicPayload.TYPE, FinishCinematicPayload.STREAM_CODEC, (payload, context) -> {
            if (payload.start()) com.murimblock.client.FinishCinematic.start(payload.executorId(), payload.victimId());
            else com.murimblock.client.FinishCinematic.stop();
        });
    }
}
