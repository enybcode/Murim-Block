package com.murimblock.cultivation;

import com.murimblock.cultivation.inner.InnerWorldService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** The cultivation's server events: meditation, realm bonuses, Qi Sense and the inner world, tick after tick. */
public final class CultivationEvents {
    private CultivationEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onClone);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CultivationEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onDamageTaken);
        NeoForge.EVENT_BUS.addListener(CultivationEvents::onServerTick);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CultivationService.getCultivation(player);
        RealmBonuses.apply(player);
        MeditationService.refreshAura(player);
        InnerWorldService.onLogin(player);
    }

    static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MeditationService.forget(player);
        QiSense.setActive(player, false);
        InnerWorldService.onLogout(player);
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MeditationService.tick(player);
        if (player.tickCount % 5 == 0) RealmBonuses.updateFist(player);
        QiSense.tick(player);
        InnerWorldService.tick(player);
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RealmBonuses.apply(player);
    }

    /** A new body after death (or the End): no meditation carried over. */
    static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) MeditationService.forget(player);
        if (event.getOriginal() instanceof ServerPlayer original) MeditationService.forget(original);
    }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MeditationService.forget(player);
        RealmBonuses.apply(player);
    }

    /** In the inner world one cannot die: a lethal blow throws the cultivator out instead. */
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && InnerWorldService.onIncomingDamage(player, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    /** A blow taken while seated ends the meditation. */
    static void onDamageTaken(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0 && MeditationService.isMeditating(player)) {
            MeditationService.onHurt(player);
        }
    }

    static void onServerTick(ServerTickEvent.Post event) {
        InnerWorldService.serverTick(event.getServer());
    }
}
