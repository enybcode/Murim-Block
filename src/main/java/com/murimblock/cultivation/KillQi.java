package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.innerworld.InnerWorld;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Death and Qi, brought back from V1 on Enzo's request (31 Oct): every kill gives Qi (the mob's own, refilling the
 * fuel) and a share of it as max Qi, less when one kind is farmed; a boss's first defeat gives a one-off max Qi; the
 * player's own death costs a share of max Qi for good. Max Qi from kills stops at the stage's peak like meditation's.
 * Nothing is gained in the inner world (the trial is its own reward).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class KillQi {
    /** Qi a kill gives, by kind; any other mob gives 1. */
    private static final Map<EntityType<?>, Integer> REWARDS = new HashMap<>();
    /** Max Qi given once, the first time a player defeats each boss. */
    private static final Map<EntityType<?>, Integer> FIRST_VICTORY = Map.of(
            EntityType.ELDER_GUARDIAN, 100, EntityType.WARDEN, 250, EntityType.WITHER, 400, EntityType.ENDER_DRAGON, 600);
    /** Recent kill times per player and kind (game ticks), for the farming cut; kept in memory only. */
    private static final Map<UUID, Map<EntityType<?>, Deque<Long>>> RECENT = new HashMap<>();

    static {
        reward(2, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.BEE, EntityType.LLAMA, EntityType.TRADER_LLAMA,
                EntityType.PUFFERFISH);
        reward(3, EntityType.SPIDER);
        reward(4, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.WOLF, EntityType.GOAT);
        reward(5, EntityType.DROWNED);
        reward(6, EntityType.HUSK, EntityType.SKELETON);
        reward(7, EntityType.PHANTOM, EntityType.PILLAGER);
        reward(8, EntityType.STRAY, EntityType.BOGGED, EntityType.PIGLIN);
        reward(10, EntityType.BREEZE, EntityType.POLAR_BEAR);
        reward(12, EntityType.CAVE_SPIDER, EntityType.ZOMBIFIED_PIGLIN, EntityType.VEX);
        reward(15, EntityType.ENDERMAN, EntityType.GUARDIAN, EntityType.BLAZE, EntityType.GHAST);
        reward(18, EntityType.WITCH, EntityType.CREEPER, EntityType.SHULKER);
        reward(25, EntityType.WITHER_SKELETON, EntityType.HOGLIN);
        reward(30, EntityType.VINDICATOR, EntityType.ZOGLIN);
        reward(35, EntityType.PIGLIN_BRUTE);
        reward(40, EntityType.EVOKER);
        reward(45, EntityType.IRON_GOLEM);
        reward(50, EntityType.RAVAGER);
        reward(150, EntityType.ELDER_GUARDIAN);
        reward(300, EntityType.WITHER);
        reward(400, EntityType.WARDEN);
        reward(500, EntityType.ENDER_DRAGON);
    }

    private KillQi() {
    }

    private static void reward(int qi, EntityType<?>... types) {
        for (EntityType<?> type : types) {
            REWARDS.put(type, qi);
        }
    }

    /** The Qi a kill of this mob gives before the farming cut; 0 for what is not a mob or belongs to the mod. */
    static int baseReward(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (!(entity instanceof Mob) || BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Murimblock.MOD_ID)) {
            return 0;
        }
        // A slime is worth its size: the big one 6 (magma 10), the small ones less.
        if (entity instanceof Slime slime) {
            int[] sized = slime instanceof MagmaCube ? new int[] {4, 7, 10} : new int[] {3, 4, 6};
            return sized[Math.min(2, Math.max(0, Integer.numberOfTrailingZeros(slime.getSize())))];
        }
        return REWARDS.getOrDefault(type, 1);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            died(player);
        } else if (event.getSource().getEntity() instanceof ServerPlayer killer && !InnerWorld.isInnerWorld(killer.level())) {
            killed(killer, event.getEntity());
        }
    }

    private static void died(ServerPlayer player) {
        double lost = Meditation.of(player).die();
        if (lost >= 0.05) {
            player.sendSystemMessage(Component.translatable("message.murimblock.cultivation.death_loss", format(lost))
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void killed(ServerPlayer killer, LivingEntity victim) {
        int base = baseReward(victim);
        if (base <= 0) {
            return;
        }
        long now = killer.serverLevel().getGameTime();
        Deque<Long> times = RECENT.computeIfAbsent(killer.getUUID(), id -> new HashMap<>())
                .computeIfAbsent(victim.getType(), type -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() > CultivationRules.REPEAT_WINDOW_TICKS) {
            times.pollFirst();
        }
        times.addLast(now);
        Cultivation c = Meditation.of(killer);
        double qi = base * CultivationRules.repeatMultiplier(times.size());
        c.addQi(qi);
        double maxQi = qi * CultivationRules.KILL_SHARE;
        Integer bonus = FIRST_VICTORY.get(victim.getType());
        if (bonus != null && c.firstDefeat(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString())) {
            maxQi += bonus;
        }
        double gained = c.gainMaxQi(maxQi);
        killer.displayClientMessage(gained > 0
                ? Component.translatable("message.murimblock.cultivation.kill", format(qi), format(gained))
                        .withStyle(ChatFormatting.AQUA)
                : Component.translatable(c.gaugeFull() ? "message.murimblock.cultivation.kill_peak"
                        : "message.murimblock.cultivation.kill_qi", format(qi)).withStyle(ChatFormatting.AQUA), true);
        Meditation.sync(killer);
    }

    private static String format(double amount) {
        return amount == Math.rint(amount) ? String.valueOf((long) amount) : String.format(Locale.ROOT, "%.1f", amount);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RECENT.clear();
    }
}
