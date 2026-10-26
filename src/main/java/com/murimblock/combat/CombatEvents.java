package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.client.combat.ClientCombat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Hooks bare-handed combat into the game: network, move files, ticking and the vanilla punch it replaces. */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class CombatEvents {
    private static final String PROTOCOL = "2";

    private CombatEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(CombatPayloads.Moves.TYPE, CombatPayloads.Moves.STREAM_CODEC,
                (payload, context) -> CombatMoves.replace(payload.moves()));
        registrar.playToClient(CombatPayloads.StrikeRefused.TYPE, CombatPayloads.StrikeRefused.STREAM_CODEC,
                (payload, context) -> ClientCombat.refused());
        registrar.playToClient(CombatPayloads.Strike.TYPE, CombatPayloads.Strike.STREAM_CODEC,
                (payload, context) -> ClientCombat.strikeSeen(payload.entityId(), payload.move()));
        registrar.playToClient(CombatPayloads.Impact.TYPE, CombatPayloads.Impact.STREAM_CODEC,
                (payload, context) -> ClientCombat.impactSeen(payload.attackerId(), payload.targetId(), payload.hitStop()));
        registrar.playToClient(CombatPayloads.CombatMode.TYPE, CombatPayloads.CombatMode.STREAM_CODEC,
                (payload, context) -> ClientCombat.combatMode(payload.on(), payload.reason()));
        registrar.playToServer(CombatPayloads.ToggleCombatMode.TYPE, CombatPayloads.ToggleCombatMode.STREAM_CODEC,
                (payload, context) -> CombatServer.toggleCombatMode((ServerPlayer) context.player()));
        registrar.playToServer(CombatPayloads.StrikeRequest.TYPE, CombatPayloads.StrikeRequest.STREAM_CODEC,
                (payload, context) -> CombatServer.request((ServerPlayer) context.player(), payload.move()));
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new CombatMoves.Loader());
    }

    /** On join and after {@code /reload}. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        CombatPayloads.Moves moves = new CombatPayloads.Moves(CombatMoves.all());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, moves));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatServer.tick(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatServer.stop(player);
        }
    }

    /** In combat mode the click strikes with our moves only: no vanilla punch, no mining. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CombatServer.inCombatMode(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && CombatServer.inCombatMode(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        double knockback = CombatServer.dealingKnockback();
        if (knockback >= 0) {
            event.setStrength((float) knockback);
        }
    }
}
