package com.murimblock.qi;

import com.murimblock.combat.CombatService;
import com.murimblock.cultivation.CultivationRegistries;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.MeditationService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class QiEvents {
    private QiEvents() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        QiRewardManager.clearTransientHistory();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        QiRewardManager.clearTransientHistory();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();
        if (gameTime % 200 == 0) QiRewardManager.cleanupTransientHistory(gameTime);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QiService.getData(player);
            QiService.getBossProgress(player);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % QiConstants.REGENERATION_INTERVAL_TICKS == 0) {
            double rate = regenerationRate(player);
            if (rate > 0) QiService.regenerate(player, QiConstants.REGENERATION_INTERVAL_TICKS, rate);
        }
    }

    /** How fast the reserve comes back (share of Qi Max a minute): fast in meditation, slow in combat, none in a deviation. */
    public static double regenerationRate(ServerPlayer player) {
        if (player.hasEffect(CultivationRegistries.QI_DEVIATION)) return 0;
        // Out of combat, Azure Sky Breathing brings more back.
        double calm = 1 + com.murimblock.cultivation.MethodService.regenBonus(player);
        if (MeditationService.isMeditating(player)) return CultivationRules.REGEN_MEDITATION * calm;
        return CombatService.isInCombatMode(player) ? CultivationRules.REGEN_COMBAT : CultivationRules.REGEN_IDLE * calm;
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        ServerPlayer killer = findResponsiblePlayer(event);
        if (killer == null || killer == event.getEntity()) {
            return;
        }

        QiRewardManager.awardKillReward(killer, event.getEntity());
    }

    /**
     * Every real death (a knock-down is not one; a finish is) costs a share of the maximum Qi for good. Registered
     * last, without canceled events, so a death another rule cancels costs nothing.
     */
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Never below the stage reached (CultivationRules.afterLoss).
        double lost = CultivationService.deathPenalty(player);
        if (lost > 0) {
            player.sendSystemMessage(Component.translatable("qi.murimblock.death_penalty",
                    QiFormat.format(lost), QiFormat.format(QiService.getQiMax(player))));
        }
    }

    private static ServerPlayer findResponsiblePlayer(LivingDeathEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof ServerPlayer player) {
            return player;
        }
        if (event.getEntity().getKillCredit() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }
}
