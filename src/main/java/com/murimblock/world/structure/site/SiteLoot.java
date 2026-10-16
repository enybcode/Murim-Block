package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * What the sites keep. A barrel they already had (nothing added, nothing moved) holds a cache: the temple's bell bay,
 * the den chief's kang, the beacon post's bell bay, the hamlet's hall, one kind of stilt house. A cache holds what a
 * village weaponsmith's chest holds, and four times in ten the Long Fist Scroll, the most widespread martial art
 * (data/murimblock/loot_table/chests/murim_cache.json). The Lotus Moon Garden has no barrel: no cache.
 */
public final class SiteLoot {
    public static final ResourceKey<LootTable> CACHE = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "chests/murim_cache"));

    /**
     * The chests of the Alliance's arena (the troop halls, the judges' pavilion): silver taels, iron and gold, the
     * Alliance's food and arrows, sometimes a Qi Gathering Pill, rarely an Alliance weapon or a martial manual
     * (data/murimblock/loot_table/chests/alliance_arena.json).
     */
    public static final ResourceKey<LootTable> ARENA = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "chests/alliance_arena"));

    /**
     * The plunder of a bandit camp (its longhall and its loot store): silver taels, gold, iron, stolen silk and food,
     * sometimes a bandit's dao or the Long Fist Scroll (data/murimblock/loot_table/chests/bandit_camp.json).
     */
    public static final ResourceKey<LootTable> BANDIT_CAMP = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "chests/bandit_camp"));

    private SiteLoot() {
    }
}
