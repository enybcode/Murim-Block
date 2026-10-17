package com.murimblock.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MurimblockNetworking {
    /**
     * 23: the Seven Stars Sword (its stars shown over their targets, a constellation bursting, its techniques' effects).
     * 22: the Son of the Demon's transformation film.
     * 20: the Traveller's Staff Art (the Spinning Rampart's spin shown to everyone around).
     * 18: synchronized blood crystal combat state and projectile entity.
     * 17: conversations (the old master's dialogue, its pages and answers).
     * 16: cultivation (meditation and the inner world, Qi Sense, breakthroughs; the charge's payload gone). 15: the Long Fist (its finish in the cinematic, the main style set from the Techniques tab). 14: the crescent's
     * slam ready on the bar; 13: Thousand Drizzle's delayed cuts; 12: Cloud Step Slash held to aim.
     */
    private static final String NETWORK_VERSION = "23";

    private MurimblockNetworking() {
    }

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        // Cultivation: meditation (and the inner world), Qi Sense, breakthroughs shown.
        registrar.playToServer(CultivationPayloads.Meditate.TYPE, CultivationPayloads.Meditate.STREAM_CODEC, CultivationPayloads.Meditate::handle);
        registrar.playToServer(CultivationPayloads.Sense.TYPE, CultivationPayloads.Sense.STREAM_CODEC, CultivationPayloads.Sense::handle);
        // Conversations: a page shown, an answer chosen.
        registrar.playToClient(DialoguePayloads.Page.TYPE, DialoguePayloads.Page.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.DialogueScreen.open(payload));
        registrar.playToServer(DialoguePayloads.Choose.TYPE, DialoguePayloads.Choose.STREAM_CODEC, DialoguePayloads.Choose::handle);
        registrar.playToClient(CultivationPayloads.SenseReadings.TYPE, CultivationPayloads.SenseReadings.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.cultivation.QiSenseClient.readings(payload));
        registrar.playToClient(CultivationPayloads.Breakthrough.TYPE, CultivationPayloads.Breakthrough.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.cultivation.AuraClient.breakthrough(payload.entityId(), payload.kind()));
        registrar.playToServer(
                CombatModeTogglePayload.TYPE,
                CombatModeTogglePayload.STREAM_CODEC,
                CombatModeTogglePayload::handle
        );
        registrar.playToServer(KnockdownActionPayload.TYPE, KnockdownActionPayload.STREAM_CODEC, KnockdownActionPayload::handle);
        registrar.playToServer(TechniquePayloads.Cast.TYPE, TechniquePayloads.Cast.STREAM_CODEC, TechniquePayloads.Cast::handle);
        registrar.playToServer(TechniquePayloads.Aim.TYPE, TechniquePayloads.Aim.STREAM_CODEC, TechniquePayloads.Aim::handle);
        registrar.playToServer(TechniquePayloads.Assign.TYPE, TechniquePayloads.Assign.STREAM_CODEC, TechniquePayloads.Assign::handle);
        registrar.playToServer(TechniquePayloads.SetStrike.TYPE, TechniquePayloads.SetStrike.STREAM_CODEC, TechniquePayloads.SetStrike::handle);
        registrar.playToServer(TechniquePayloads.SetMainArt.TYPE, TechniquePayloads.SetMainArt.STREAM_CODEC, TechniquePayloads.SetMainArt::handle);
        registrar.playToClient(TechniquePayloads.Cooldown.TYPE, TechniquePayloads.Cooldown.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.CombatBarClient.cooldown(payload.technique(), payload.ticks()));
        registrar.playToClient(TechniquePayloads.Riposte.TYPE, TechniquePayloads.Riposte.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.CombatBarClient.riposte(payload.ticks()));
        registrar.playToClient(TechniquePayloads.SlamReady.TYPE, TechniquePayloads.SlamReady.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.CombatBarClient.slamReady(payload.ticks()));
        registrar.playToClient(TechniquePayloads.DelayedCuts.TYPE, TechniquePayloads.DelayedCuts.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.DrizzleSlashEffects.delayedCuts(payload.attackerId(), payload.targetId()));
        // The Spinning Rampart spun or stopped: its loop shown on everyone's screen.
        registrar.playToClient(StaffSpinPayload.TYPE, StaffSpinPayload.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.StaffClient.spin(payload.entityId(), payload.spinning()));
        // The Seven Stars Sword: the stars on a target, a constellation burst.
        registrar.playToClient(SevenStarsPayloads.Marks.TYPE, SevenStarsPayloads.Marks.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.SevenStarsFx.marks(payload.entityId(), payload.owner(), payload.count()));
        registrar.playToClient(SevenStarsPayloads.Burst.TYPE, SevenStarsPayloads.Burst.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.SevenStarsFx.burst(payload.entityId()));
        registrar.playToClient(SevenStarsPayloads.Fx.TYPE, SevenStarsPayloads.Fx.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.SevenStarsFx.fx(payload));
        registrar.playToClient(SevenStarsPayloads.Streak.TYPE, SevenStarsPayloads.Streak.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.SevenStarsFx.streak(payload.playerId(), payload.from(), payload.to(), payload.hits()));
        // Client only: only ever sent to clients, so the client classes are never loaded on a dedicated server.
        registrar.playToClient(FinishCinematicPayload.TYPE, FinishCinematicPayload.STREAM_CODEC, (payload, context) -> {
            if (payload.start()) com.murimblock.client.FinishCinematic.start(payload.executorId(), payload.victimId(), payload.finish());
            else com.murimblock.client.FinishCinematic.stop();
        });
        registrar.playToClient(WarlordEntrancePayload.TYPE, WarlordEntrancePayload.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.WarlordCinematic.start(payload.plaza(), payload.drum()));
        // A Majin Fist impact's flash of sunlight (a Photon light).
        registrar.playToClient(SunFlashPayload.TYPE, SunFlashPayload.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.CruelSunRenderer.flash(payload));
        registrar.playToClient(TransformationPayload.TYPE, TransformationPayload.STREAM_CODEC,
                (payload, context) -> com.murimblock.client.TransformationCinematic.start(payload));
    }
}
