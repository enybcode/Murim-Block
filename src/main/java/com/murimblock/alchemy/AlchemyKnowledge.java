package com.murimblock.alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

/**
 * The recipes a player knows (docs/cultivation/alchemy.md), kept with the player (their persistent data, carried
 * through death by AlchemyEvents): learned from the Alchemy Manual (the common ones) or from a recipe scroll (a rare one).
 */
public final class AlchemyKnowledge {
    static final String KEY = "murimblock:alchemy_recipes";

    private AlchemyKnowledge() {
    }

    private static ListTag list(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_LIST)) data.put(KEY, new ListTag());
        return data.getList(KEY, Tag.TAG_STRING);
    }

    public static boolean knows(Player player, AlchemyRecipes.Recipe recipe) {
        for (Tag t : list(player)) if (t.getAsString().equals(recipe.id())) return true;
        return false;
    }

    /** The recipes known, one bit each by their index (for the furnace's recipe book). */
    public static int mask(Player player) {
        int mask = 0;
        for (AlchemyRecipes.Recipe r : AlchemyRecipes.ALL) if (knows(player, r)) mask |= 1 << r.index();
        return mask;
    }

    /** Learns it; returns whether it was new. */
    public static boolean learn(Player player, AlchemyRecipes.Recipe recipe) {
        if (knows(player, recipe)) return false;
        ListTag l = list(player);
        l.add(StringTag.valueOf(recipe.id()));
        player.getPersistentData().put(KEY, l);
        return true;
    }

    public static void forgetAllForTests(Player player) {
        player.getPersistentData().remove(KEY);
    }
}
