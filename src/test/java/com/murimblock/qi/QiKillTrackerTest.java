package com.murimblock.qi;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QiKillTrackerTest {
    private static final UUID PLAYER = UUID.randomUUID();
    private static final ResourceLocation ZOMBIE = ResourceLocation.parse("minecraft:zombie");
    private static final ResourceLocation WITHER = ResourceLocation.parse("minecraft:wither");

    @Test
    void recordingOnlyPrunesTheAffectedHistory() {
        QiKillTracker tracker = new QiKillTracker();
        for (int i = 0; i < 21; i++) assertEquals(i + 1, tracker.recordKill(PLAYER, ZOMBIE, i));
        assertEquals(0.5, tracker.repeatMultiplier(tracker.recentKillCount(PLAYER, ZOMBIE, 21)));
        assertEquals(1, tracker.recordKill(PLAYER, ZOMBIE, QiKillTracker.REPEAT_WINDOW_TICKS + 22));
    }

    @Test
    void maintenanceExpiresIdlePlayersAndBossCooldowns() {
        QiKillTracker tracker = new QiKillTracker();
        tracker.recordKill(PLAYER, ZOMBIE, 0);
        tracker.recordFullBossReward(PLAYER, WITHER, 0);
        tracker.cleanup(QiKillTracker.BOSS_REPEAT_WINDOW_TICKS + 1);
        assertEquals(0, tracker.recentKillCount(PLAYER, ZOMBIE, 1));
        assertFalse(tracker.hasRecentFullBossReward(PLAYER, WITHER, 1));
    }

    @Test
    void clearPreventsHistoryFromLeakingIntoTheNextServer() {
        QiKillTracker tracker = new QiKillTracker();
        tracker.recordKill(PLAYER, ZOMBIE, 100_000);
        tracker.recordFullBossReward(PLAYER, WITHER, 100_000);
        tracker.clear();
        assertEquals(1, tracker.recordKill(PLAYER, ZOMBIE, 0));
        assertFalse(tracker.hasRecentFullBossReward(PLAYER, WITHER, 0));
    }

    @Test
    void futureBossTimestampsDoNotCountAsRecent() {
        QiKillTracker tracker = new QiKillTracker();
        tracker.recordFullBossReward(PLAYER, WITHER, 100);
        assertFalse(tracker.hasRecentFullBossReward(PLAYER, WITHER, 99));
        assertTrue(tracker.hasRecentFullBossReward(PLAYER, WITHER, 100));
        assertFalse(tracker.hasRecentFullBossReward(PLAYER, WITHER, 100 + QiKillTracker.BOSS_REPEAT_WINDOW_TICKS));
    }
}
