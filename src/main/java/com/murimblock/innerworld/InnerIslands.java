package com.murimblock.innerworld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Which island belongs to whom: every player gets a slot, one island per slot, far apart in the inner world dimension,
 * so nobody ever sees another's island. Saved with the overworld; slots and tag names are saved IDs: keep them.
 */
public final class InnerIslands extends SavedData {
    private static final String NAME = "murimblock_inner_islands";
    /** Distance between two islands, far beyond any view distance. */
    public static final int SPACING = 4096;

    private final Map<UUID, Integer> slots = new HashMap<>();
    private final Map<UUID, Integer> built = new HashMap<>();
    private int next;

    public static InnerIslands get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(InnerIslands::new, InnerIslands::load, null), NAME);
    }

    /** The island center of this player (top of the grass), giving them a slot on first use. */
    public BlockPos center(UUID player) {
        int slot = slots.computeIfAbsent(player, id -> {
            setDirty();
            return next++;
        });
        return new BlockPos((slot + 1) * SPACING, InnerIsland.SURFACE_Y, 0);
    }

    /** The island center of this player if they have one, else null. */
    public BlockPos existingCenter(UUID player) {
        Integer slot = slots.get(player);
        return slot == null ? null : new BlockPos((slot + 1) * SPACING, InnerIsland.SURFACE_Y, 0);
    }

    /** Builds (or rebuilds after a layout change) this player's island; returns its center. */
    public BlockPos prepare(UUID player, ServerLevel level) {
        BlockPos center = center(player);
        int version = built.getOrDefault(player, 0);
        if (version < InnerIsland.VERSION) {
            int chunks = (InnerIsland.CLEAR_RADIUS >> 4) + 1;
            for (int cx = -chunks; cx <= chunks; cx++) {
                for (int cz = -chunks; cz <= chunks; cz++) {
                    level.getChunk((center.getX() >> 4) + cx, (center.getZ() >> 4) + cz);
                }
            }
            InnerIsland.build(level, center, version > 0);
            built.put(player, InnerIsland.VERSION);
            setDirty();
        }
        return center;
    }

    private static InnerIslands load(CompoundTag tag, HolderLookup.Provider registries) {
        InnerIslands islands = new InnerIslands();
        islands.next = tag.getInt("next");
        for (Tag t : tag.getList("islands", Tag.TAG_COMPOUND)) {
            CompoundTag island = (CompoundTag) t;
            UUID owner = island.getUUID("owner");
            islands.slots.put(owner, island.getInt("slot"));
            islands.built.put(owner, island.getInt("built"));
        }
        return islands;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("next", next);
        ListTag list = new ListTag();
        slots.forEach((owner, slot) -> {
            CompoundTag island = new CompoundTag();
            island.putUUID("owner", owner);
            island.putInt("slot", slot);
            island.putInt("built", built.getOrDefault(owner, 0));
            list.add(island);
        });
        tag.put("islands", list);
        return tag;
    }
}
