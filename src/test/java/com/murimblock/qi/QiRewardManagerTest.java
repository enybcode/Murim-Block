package com.murimblock.qi;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QiRewardManagerTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @BeforeEach
    void resetTracker() {
        QiRewardManager.resetTrackerForTests();
    }

    @Test
    void classicMobRewardsMatchBalanceTable() {
        assertReward(EntityType.ZOMBIE, 4);
        assertReward(EntityType.SKELETON, 6);
        assertReward(EntityType.CREEPER, 18);
        assertReward(EntityType.ENDERMAN, 15);
        assertReward(EntityType.BLAZE, 15);
        assertReward(EntityType.RAVAGER, 50);
    }

    @Test
    void passiveMobRewardsAreOneQi() {
        assertReward(EntityType.COW, 1);
        assertReward(EntityType.CHICKEN, 1);
        assertReward(EntityType.PIG, 1);
    }

    @Test
    void slimeAndMagmaCubeSizesUseExpectedRewards() {
        assertEquals(3, QiRewardManager.sizedReward(1, 3, 4, 6));
        assertEquals(4, QiRewardManager.sizedReward(2, 3, 4, 6));
        assertEquals(6, QiRewardManager.sizedReward(4, 3, 4, 6));

        assertEquals(4, QiRewardManager.sizedReward(1, 4, 7, 10));
        assertEquals(7, QiRewardManager.sizedReward(2, 4, 7, 10));
        assertEquals(10, QiRewardManager.sizedReward(4, 4, 7, 10));
    }

    @Test
    void drownedVariantsUseExpectedRewards() {
        assertReward(EntityType.DROWNED, 5);
        assertEquals(20, new QiRewardManager.RewardTarget(
                BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.DROWNED),
                20,
                0,
                null,
                null
        ).baseReward());
    }

    @Test
    void repeatedKillsReduceRewardsWithoutGoingBelowHalf() {
        QiRewardManager.RewardTarget zombie = QiRewardManager.rewardTarget(EntityType.ZOMBIE);

        for (int i = 1; i <= 25; i++) {
            QiRewardData reward = QiRewardManager.calculateQiReward(PLAYER_A, QiBossProgress.initial(), zombie, i);
            if (i <= 10) {
                assertEquals(4, reward.finalReward());
            } else if (i <= 20) {
                assertEquals(3, reward.finalReward());
            } else {
                assertEquals(2, reward.finalReward());
            }
        }
    }

    @Test
    void antiFarmIsTrackedSeparatelyPerPlayer() {
        QiRewardManager.RewardTarget zombie = QiRewardManager.rewardTarget(EntityType.ZOMBIE);
        for (int i = 1; i <= 20; i++) {
            QiRewardManager.calculateQiReward(PLAYER_A, QiBossProgress.initial(), zombie, i);
        }

        QiRewardData playerBReward = QiRewardManager.calculateQiReward(PLAYER_B, QiBossProgress.initial(), zombie, 21);

        assertEquals(4, playerBReward.finalReward());
        assertEquals(1.0, playerBReward.antiFarmMultiplier());
    }

    @Test
    void bossFirstVictoriesGrantFullRewardsThenNormalRewards() {
        assertBoss(EntityType.ELDER_GUARDIAN, 400, 150);
        assertBoss(EntityType.WARDEN, 1_000, 400);
        assertBoss(EntityType.WITHER, 1_500, 300);
        assertBoss(EntityType.ENDER_DRAGON, 2_500, 500);
    }

    @Test
    void witherAndDragonRepeatCooldownNeverDropsBelowHalf() {
        assertRepeatedBossMinimum(EntityType.WITHER, 150);
        assertRepeatedBossMinimum(EntityType.ENDER_DRAGON, 250);
    }

    @Test
    void killHistoryExpiresAfterFifteenMinutes() {
        QiRewardManager.RewardTarget zombie = QiRewardManager.rewardTarget(EntityType.ZOMBIE);
        for (int i = 0; i < 25; i++) {
            QiRewardManager.calculateQiReward(PLAYER_A, QiBossProgress.initial(), zombie, i);
        }

        QiRewardData reward = QiRewardManager.calculateQiReward(
                PLAYER_A,
                QiBossProgress.initial(),
                zombie,
                QiKillTracker.REPEAT_WINDOW_TICKS + 30
        );

        assertEquals(4, reward.finalReward());
    }

    private static void assertReward(EntityType<?> entityType, int expected) {
        QiRewardData reward = QiRewardManager.calculateQiReward(
                PLAYER_A,
                QiBossProgress.initial(),
                QiRewardManager.rewardTarget(entityType),
                0
        );

        assertEquals(expected, reward.finalReward());
    }

    private static void assertBoss(EntityType<?> entityType, int firstReward, int laterReward) {
        QiRewardManager.resetTrackerForTests();
        QiRewardManager.RewardTarget target = QiRewardManager.rewardTarget(entityType);
        ResourceLocation bossId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);

        QiRewardData first = QiRewardManager.calculateQiReward(PLAYER_A, QiBossProgress.initial(), target, 0);
        QiRewardData later = QiRewardManager.calculateQiReward(
                PLAYER_A,
                QiBossProgress.initial().markDefeated(bossId),
                target,
                QiKillTracker.BOSS_REPEAT_WINDOW_TICKS + 1
        );

        assertEquals(firstReward, first.finalReward());
        assertTrue(first.firstVictory());
        assertEquals(laterReward, later.finalReward());
        assertFalse(later.firstVictory());
    }

    private static void assertRepeatedBossMinimum(EntityType<?> entityType, int minimumReward) {
        QiRewardManager.resetTrackerForTests();
        ResourceLocation bossId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        QiRewardManager.RewardTarget target = QiRewardManager.rewardTarget(entityType);
        QiBossProgress progress = QiBossProgress.initial().markDefeated(bossId);

        QiRewardManager.recordFullBossRewardForTests(PLAYER_A, bossId, 0);
        QiRewardData repeated = QiRewardManager.calculateQiReward(PLAYER_A, progress, target, 1);

        assertEquals(minimumReward, repeated.finalReward());
        assertTrue(repeated.repeatedBossReward());
    }
}
