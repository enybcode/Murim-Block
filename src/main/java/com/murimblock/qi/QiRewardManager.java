package com.murimblock.qi;

import com.murimblock.Murimblock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Items;

public final class QiRewardManager {
    private static final QiKillTracker KILL_TRACKER = new QiKillTracker();
    private static final boolean DEBUG_REWARD_MESSAGES = false;
    private static final Map<EntityType<?>, Integer> FIXED_REWARDS = createFixedRewards();

    private QiRewardManager() {
    }

    public static Optional<QiRewardData> awardKillReward(ServerPlayer player, LivingEntity killedEntity) {
        QiRewardData reward = calculateQiReward(player, killedEntity);
        if (reward.finalReward() <= 0) {
            return Optional.empty();
        }

        // TODO: Decider plus tard si le Qi excedentaire doit alimenter une progression de cultivation ou de breakthrough.
        QiService.addQi(player, reward.finalReward());
        rememberFirstVictory(player, reward);
        rememberFullBossReward(player, reward, player.serverLevel().getGameTime());
        sendDebugMessage(player, killedEntity, reward);
        return Optional.of(reward);
    }

    public static QiRewardData calculateQiReward(ServerPlayer player, LivingEntity killedEntity) {
        return calculateQiReward(
                player.getUUID(),
                QiService.getBossProgress(player),
                resolveRewardTarget(killedEntity),
                player.serverLevel().getGameTime()
        );
    }

    static QiRewardData calculateQiReward(
            UUID playerId,
            QiBossProgress bossProgress,
            RewardTarget target,
            long gameTime
    ) {
        int recentKillCount = KILL_TRACKER.recordKill(playerId, target.entityTypeId(), gameTime);
        double multiplier = Math.min(target.originMultiplier(), KILL_TRACKER.repeatMultiplier(recentKillCount));
        boolean firstVictory = target.firstVictoryKey() != null && !bossProgress.hasDefeated(target.firstVictoryKey());
        boolean repeatedBossReward = target.fullRewardCooldownKey() != null
                && !firstVictory
                && KILL_TRACKER.hasRecentFullBossReward(playerId, target.fullRewardCooldownKey(), gameTime);

        if (firstVictory) {
            multiplier = 1.0;
        } else if (repeatedBossReward) {
            multiplier = Math.min(multiplier, 0.5);
        }

        multiplier = Math.clamp(multiplier, 0.5, 1.0);
        int normalReward = (int) Math.ceil(target.baseReward() * multiplier);
        int firstVictoryBonus = firstVictory ? target.firstVictoryBonus() : 0;
        return new QiRewardData(
                target.entityTypeId(),
                target.baseReward(),
                firstVictoryBonus,
                multiplier,
                recentKillCount,
                normalReward + firstVictoryBonus,
                firstVictory,
                repeatedBossReward
        );
    }

    public static RewardTarget resolveRewardTarget(LivingEntity entity) {
        ResourceLocation entityTypeId = entityTypeId(entity.getType());
        double originMultiplier = originMultiplier(entity);
        if (entity instanceof Slime slime && !(entity instanceof MagmaCube)) {
            return new RewardTarget(entityTypeId, sizedReward(slime.getSize(), 3, 4, 6), 0, null, null, originMultiplier);
        }
        if (entity instanceof MagmaCube magmaCube) {
            return new RewardTarget(entityTypeId, sizedReward(magmaCube.getSize(), 4, 7, 10), 0, null, null, originMultiplier);
        }
        if (entity instanceof Drowned drowned && hasTrident(drowned)) {
            return new RewardTarget(entityTypeId, 20, 0, null, null, originMultiplier);
        }
        if (entity instanceof WitherBoss) {
            return new RewardTarget(entityTypeId, 300, 1_200, entityTypeId, entityTypeId, originMultiplier);
        }
        if (entity instanceof EnderDragon) {
            return new RewardTarget(entityTypeId, 500, 2_000, entityTypeId, entityTypeId, originMultiplier);
        }

        int baseReward = FIXED_REWARDS.getOrDefault(entity.getType(), 1);
        if (entity.getType() == EntityType.ELDER_GUARDIAN) {
            return new RewardTarget(entityTypeId, baseReward, 250, entityTypeId, null, originMultiplier);
        }
        if (entity.getType() == EntityType.WARDEN) {
            return new RewardTarget(entityTypeId, baseReward, 600, entityTypeId, null, originMultiplier);
        }
        return new RewardTarget(entityTypeId, baseReward, 0, null, null, originMultiplier);
    }

    public static RewardTarget rewardTarget(EntityType<?> entityType) {
        ResourceLocation entityTypeId = entityTypeId(entityType);
        int baseReward = FIXED_REWARDS.getOrDefault(entityType, 1);
        if (entityType == EntityType.ELDER_GUARDIAN) {
            return new RewardTarget(entityTypeId, baseReward, 250, entityTypeId, null);
        }
        if (entityType == EntityType.WARDEN) {
            return new RewardTarget(entityTypeId, baseReward, 600, entityTypeId, null);
        }
        if (entityType == EntityType.WITHER) {
            return new RewardTarget(entityTypeId, 300, 1_200, entityTypeId, entityTypeId);
        }
        if (entityType == EntityType.ENDER_DRAGON) {
            return new RewardTarget(entityTypeId, 500, 2_000, entityTypeId, entityTypeId);
        }
        return new RewardTarget(entityTypeId, baseReward, 0, null, null);
    }

    public static String describeAntiFarm(ServerPlayer player, EntityType<?> entityType) {
        long gameTime = player.serverLevel().getGameTime();
        ResourceLocation entityTypeId = entityTypeId(entityType);
        int count = KILL_TRACKER.recentKillCount(player.getUUID(), entityTypeId, gameTime);
        int nextCount = count + 1;
        double nextMultiplier = Math.clamp(KILL_TRACKER.repeatMultiplier(nextCount), 0.5, 1.0);
        return "Kills recents " + entityTypeId + " : " + count
                + " | prochaine recompense anti-farm : " + Math.round(nextMultiplier * 100.0) + " %";
    }

    public static void resetDevelopmentData(ServerPlayer player) {
        KILL_TRACKER.reset(player.getUUID());
        QiService.resetBossProgress(player);
    }

    static void resetTrackerForTests() {
        KILL_TRACKER.clear();
    }

    static void recordFullBossRewardForTests(UUID playerId, ResourceLocation bossId, long gameTime) {
        KILL_TRACKER.recordFullBossReward(playerId, bossId, gameTime);
    }

    private static void rememberFirstVictory(ServerPlayer player, QiRewardData reward) {
        if (reward.firstVictory()) {
            QiService.markBossDefeated(player, reward.entityTypeId());
        }
    }

    private static void rememberFullBossReward(ServerPlayer player, QiRewardData reward, long gameTime) {
        if (reward.entityTypeId().equals(entityTypeId(EntityType.WITHER))
                || reward.entityTypeId().equals(entityTypeId(EntityType.ENDER_DRAGON))) {
            if (reward.repeatedBossReward()) {
                return;
            }
            KILL_TRACKER.recordFullBossReward(player.getUUID(), reward.entityTypeId(), gameTime);
        }
    }

    private static void sendDebugMessage(ServerPlayer player, LivingEntity killedEntity, QiRewardData reward) {
        if (!DEBUG_REWARD_MESSAGES) {
            return;
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                killedEntity.getType().toShortString()
                        + " killed | Base Qi: " + reward.baseReward()
                        + " | Anti-farm: " + Math.round(reward.antiFarmMultiplier() * 100.0) + " %"
                        + " | First bonus: " + reward.firstVictoryBonus()
                        + " | Final Qi: " + reward.finalReward()
        ));
    }

    static int sizedReward(int size, int small, int medium, int large) {
        if (size <= 1) {
            return small;
        }
        if (size <= 2) {
            return medium;
        }
        return large;
    }

    private static boolean hasTrident(Drowned drowned) {
        return drowned.getItemInHand(InteractionHand.MAIN_HAND).is(Items.TRIDENT)
                || drowned.getItemInHand(InteractionHand.OFF_HAND).is(Items.TRIDENT);
    }

    private static double originMultiplier(LivingEntity entity) {
        // NeoForge 1.21.1 does not expose a reliable vanilla spawner/farm flag here.
        return 1.0;
    }

    private static ResourceLocation entityTypeId(EntityType<?> entityType) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return id == null ? ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "unknown") : id;
    }

    private static Map<EntityType<?>, Integer> createFixedRewards() {
        Map<EntityType<?>, Integer> rewards = new HashMap<>();
        addPassiveRewards(rewards);

        rewards.put(EntityType.SILVERFISH, 2);
        rewards.put(EntityType.ENDERMITE, 2);
        rewards.put(EntityType.BEE, 2);
        rewards.put(EntityType.LLAMA, 2);
        rewards.put(EntityType.TRADER_LLAMA, 2);
        rewards.put(EntityType.PUFFERFISH, 2);

        rewards.put(EntityType.SPIDER, 3);
        rewards.put(EntityType.ZOMBIE, 4);
        rewards.put(EntityType.ZOMBIE_VILLAGER, 4);
        rewards.put(EntityType.WOLF, 4);
        rewards.put(EntityType.GOAT, 4);
        rewards.put(EntityType.DROWNED, 5);

        rewards.put(EntityType.HUSK, 6);
        rewards.put(EntityType.SKELETON, 6);
        rewards.put(EntityType.PHANTOM, 7);
        rewards.put(EntityType.PILLAGER, 7);
        rewards.put(EntityType.STRAY, 8);
        rewards.put(EntityType.BOGGED, 8);
        rewards.put(EntityType.PIGLIN, 8);
        rewards.put(EntityType.BREEZE, 10);
        rewards.put(EntityType.POLAR_BEAR, 10);

        rewards.put(EntityType.CAVE_SPIDER, 12);
        rewards.put(EntityType.ZOMBIFIED_PIGLIN, 12);
        rewards.put(EntityType.VEX, 12);
        rewards.put(EntityType.ENDERMAN, 15);
        rewards.put(EntityType.GUARDIAN, 15);
        rewards.put(EntityType.BLAZE, 15);
        rewards.put(EntityType.GHAST, 15);
        rewards.put(EntityType.WITCH, 18);
        rewards.put(EntityType.CREEPER, 18);
        rewards.put(EntityType.SHULKER, 18);

        rewards.put(EntityType.WITHER_SKELETON, 25);
        rewards.put(EntityType.HOGLIN, 25);
        rewards.put(EntityType.VINDICATOR, 30);
        rewards.put(EntityType.ZOGLIN, 30);
        rewards.put(EntityType.PIGLIN_BRUTE, 35);
        rewards.put(EntityType.EVOKER, 40);
        rewards.put(EntityType.IRON_GOLEM, 45);
        rewards.put(EntityType.RAVAGER, 50);

        rewards.put(EntityType.ELDER_GUARDIAN, 150);
        rewards.put(EntityType.WARDEN, 400);
        return rewards;
    }

    private static void addPassiveRewards(Map<EntityType<?>, Integer> rewards) {
        rewards.put(EntityType.ALLAY, 1);
        rewards.put(EntityType.ARMADILLO, 1);
        rewards.put(EntityType.AXOLOTL, 1);
        rewards.put(EntityType.BAT, 1);
        rewards.put(EntityType.CAMEL, 1);
        rewards.put(EntityType.CAT, 1);
        rewards.put(EntityType.CHICKEN, 1);
        rewards.put(EntityType.COD, 1);
        rewards.put(EntityType.COW, 1);
        rewards.put(EntityType.DONKEY, 1);
        rewards.put(EntityType.FOX, 1);
        rewards.put(EntityType.FROG, 1);
        rewards.put(EntityType.GLOW_SQUID, 1);
        rewards.put(EntityType.HORSE, 1);
        rewards.put(EntityType.MOOSHROOM, 1);
        rewards.put(EntityType.MULE, 1);
        rewards.put(EntityType.OCELOT, 1);
        rewards.put(EntityType.PARROT, 1);
        rewards.put(EntityType.PIG, 1);
        rewards.put(EntityType.RABBIT, 1);
        rewards.put(EntityType.SALMON, 1);
        rewards.put(EntityType.SHEEP, 1);
        rewards.put(EntityType.SNIFFER, 1);
        rewards.put(EntityType.SNOW_GOLEM, 1);
        rewards.put(EntityType.SQUID, 1);
        rewards.put(EntityType.STRIDER, 1);
        rewards.put(EntityType.TADPOLE, 1);
        rewards.put(EntityType.TROPICAL_FISH, 1);
        rewards.put(EntityType.TURTLE, 1);
        rewards.put(EntityType.VILLAGER, 1);
        rewards.put(EntityType.WANDERING_TRADER, 1);
    }

    public record RewardTarget(
            ResourceLocation entityTypeId,
            int baseReward,
            int firstVictoryBonus,
            ResourceLocation firstVictoryKey,
            ResourceLocation fullRewardCooldownKey,
            double originMultiplier
    ) {
        public RewardTarget(
                ResourceLocation entityTypeId,
                int baseReward,
                int firstVictoryBonus,
                ResourceLocation firstVictoryKey,
                ResourceLocation fullRewardCooldownKey
        ) {
            this(entityTypeId, baseReward, firstVictoryBonus, firstVictoryKey, fullRewardCooldownKey, 1.0);
        }

        public RewardTarget {
            originMultiplier = Math.clamp(originMultiplier, 0.5, 1.0);
        }
    }
}
