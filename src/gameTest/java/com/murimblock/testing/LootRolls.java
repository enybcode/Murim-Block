package com.murimblock.testing;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

/** Rolls a mob's loot table many times, as a kill by a player (or by nothing), and counts what comes out. */
public final class LootRolls {
    private LootRolls() { }

    /**
     * What came out of {@code rolls} rolls: for each item, in how many rolls it came, and the most of it in one roll.
     */
    public record Tally(int rolls, Map<Item, Integer> rollsWith, Map<Item, Integer> mostInOneRoll) {
        public int rollsWith(Item item) {
            return rollsWith.getOrDefault(item, 0);
        }

        public int mostInOneRoll(Item item) {
            return mostInOneRoll.getOrDefault(item, 0);
        }

        /** True when nothing but these items ever came out. */
        public boolean onlyContains(Item... items) {
            return java.util.Set.of(items).containsAll(rollsWith.keySet());
        }
    }

    /** The mob's own loot table rolled {@code rolls} times, killed by {@code killer} (no Looting) or, when null, by nothing. */
    public static Tally roll(ServerLevel level, LivingEntity mob, @Nullable ServerPlayer killer, int rolls) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(mob.getLootTable());
        DamageSource source = killer == null ? level.damageSources().generic() : level.damageSources().playerAttack(killer);
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, mob)
                .withParameter(LootContextParams.ORIGIN, mob.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer);
        if (killer != null) builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer).withLuck(killer.getLuck());
        LootParams params = builder.create(LootContextParamSets.ENTITY);
        Map<Item, Integer> rollsWith = new HashMap<>(), most = new HashMap<>();
        for (int i = 0; i < rolls; i++) {
            Map<Item, Integer> thisRoll = new HashMap<>();
            for (ItemStack stack : table.getRandomItems(params)) {
                // A count of nought comes out as an empty stack.
                if (!stack.isEmpty()) thisRoll.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
            thisRoll.forEach((item, count) -> {
                rollsWith.merge(item, 1, Integer::sum);
                most.merge(item, count, Math::max);
            });
        }
        return new Tally(rolls, rollsWith, most);
    }
}
