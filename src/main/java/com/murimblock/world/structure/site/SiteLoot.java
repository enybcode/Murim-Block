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

    private SiteLoot() {
    }
}
