package com.murimblock.qi;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * Temporary server-side kill history used for anti-farm reward reductions.
 */
public final class QiKillTracker {
    static final long REPEAT_WINDOW_TICKS = 15L * 60L * 20L;
    static final long BOSS_REPEAT_WINDOW_TICKS = 30L * 60L * 20L;

    private final Map<UUID, Map<ResourceLocation, KillHistory>> killHistories = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Long>> fullBossRewardTicks = new HashMap<>();

    public int recordKill(UUID playerId, ResourceLocation entityTypeId, long gameTime) {
        cleanup(gameTime);

        KillHistory history = killHistories
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .computeIfAbsent(entityTypeId, ignored -> new KillHistory());
        history.record(gameTime);
        history.removeOlderThan(gameTime - REPEAT_WINDOW_TICKS);
        return history.count();
    }

    public double repeatMultiplier(int recentKillCount) {
        if (recentKillCount <= 10) {
            return 1.0;
        }
        if (recentKillCount <= 20) {
            return 0.75;
        }
        return 0.5;
    }

    public boolean hasRecentFullBossReward(UUID playerId, ResourceLocation bossId, long gameTime) {
        Long rewardTick = fullBossRewardTicks
                .getOrDefault(playerId, Map.of())
                .get(bossId);
        return rewardTick != null && gameTime - rewardTick < BOSS_REPEAT_WINDOW_TICKS;
    }

    public void recordFullBossReward(UUID playerId, ResourceLocation bossId, long gameTime) {
        fullBossRewardTicks
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(bossId, gameTime);
    }

    public int recentKillCount(UUID playerId, ResourceLocation entityTypeId, long gameTime) {
        KillHistory history = killHistories
                .getOrDefault(playerId, Map.of())
                .get(entityTypeId);
        if (history == null) {
            return 0;
        }
        history.removeOlderThan(gameTime - REPEAT_WINDOW_TICKS);
        return history.count();
    }

    public void reset(UUID playerId) {
        killHistories.remove(playerId);
        fullBossRewardTicks.remove(playerId);
    }

    void clear() {
        killHistories.clear();
        fullBossRewardTicks.clear();
    }

    public void cleanup(long gameTime) {
        cleanupKillHistories(gameTime);
        cleanupBossRewards(gameTime);
    }

    private void cleanupKillHistories(long gameTime) {
        long minimumTick = gameTime - REPEAT_WINDOW_TICKS;
        Iterator<Map.Entry<UUID, Map<ResourceLocation, KillHistory>>> playerIterator = killHistories.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map<ResourceLocation, KillHistory> byType = playerIterator.next().getValue();
            byType.values().forEach(history -> history.removeOlderThan(minimumTick));
            byType.values().removeIf(KillHistory::isEmpty);
            if (byType.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private void cleanupBossRewards(long gameTime) {
        long minimumTick = gameTime - BOSS_REPEAT_WINDOW_TICKS;
        Iterator<Map.Entry<UUID, Map<ResourceLocation, Long>>> playerIterator = fullBossRewardTicks.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map<ResourceLocation, Long> byType = playerIterator.next().getValue();
            byType.values().removeIf(tick -> tick < minimumTick);
            if (byType.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private static final class KillHistory {
        private final Deque<Long> killTicks = new ArrayDeque<>();

        void record(long gameTime) {
            killTicks.addLast(gameTime);
        }

        void removeOlderThan(long minimumTick) {
            while (!killTicks.isEmpty() && killTicks.peekFirst() < minimumTick) {
                killTicks.removeFirst();
            }
        }

        int count() {
            return killTicks.size();
        }

        boolean isEmpty() {
            return killTicks.isEmpty();
        }
    }
}
