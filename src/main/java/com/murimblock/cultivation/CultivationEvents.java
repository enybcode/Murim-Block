package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Hooks cultivation into the game: network, ticking, interruptions and commands. */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class CultivationEvents {
    private static final String PROTOCOL = "1";

    private CultivationEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(CultivationPayloads.Sync.TYPE, CultivationPayloads.Sync.STREAM_CODEC,
                (payload, context) -> ClientCultivation.receive(payload.snapshot()));
        registrar.playToClient(CultivationPayloads.Pose.TYPE, CultivationPayloads.Pose.STREAM_CODEC,
                (payload, context) -> ClientCultivation.pose(payload.entityId(), payload.meditating()));
        registrar.playToServer(CultivationPayloads.ToggleMeditation.TYPE, CultivationPayloads.ToggleMeditation.STREAM_CODEC,
                (payload, context) -> Meditation.toggle((ServerPlayer) context.player()));
        registrar.playToServer(CultivationPayloads.Advance.TYPE, CultivationPayloads.Advance.STREAM_CODEC,
                (payload, context) -> Meditation.advance((ServerPlayer) context.player()));
        registrar.playToServer(CultivationPayloads.Breath.TYPE, CultivationPayloads.Breath.STREAM_CODEC,
                (payload, context) -> Meditation.breathe((ServerPlayer) context.player(), payload.good()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Meditation.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Meditation.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Meditation.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Meditation.stop(player, null);
            Meditation.sync(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher
                && Meditation.of(target).meditating()) {
            PacketDistributor.sendToPlayer(watcher, new CultivationPayloads.Pose(target.getId(), true));
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0) {
            Meditation.stop(player, "message.murimblock.meditation.hit");
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Meditation.stop(player, null);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CultivationCommands.register(event.getDispatcher());
    }
}
