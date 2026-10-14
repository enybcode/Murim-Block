package com.murimblock.cultivation.inner;

import com.murimblock.Murimblock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Where each cultivator's island floats in the inner world (one slot each, far from the others) and what it was last
 * built from (its look, and its radius, to clear it before building it again). Saved with the overworld.
 */
public final class InnerWorldData extends SavedData {
    private static final String NAME = Murimblock.MOD_ID + "_inner_world";
    /** Islands are this far apart (blocks): far beyond any view distance. */
    public static final int SPACING = 2048;

    private final Map<UUID, Integer> slots = new HashMap<>();
    private final Map<UUID, String> looks = new HashMap<>();
    private final Map<UUID, Integer> radii = new HashMap<>();
    private int next;

    public static final SavedData.Factory<InnerWorldData> FACTORY = new SavedData.Factory<>(InnerWorldData::new, InnerWorldData::load);

    public static InnerWorldData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** The player's slot, given the first time. */
    public int slot(UUID player) {
        Integer slot = slots.get(player);
        if (slot == null) {
            slot = next++;
            slots.put(player, slot);
            setDirty();
        }
        return slot;
    }

    public String look(UUID player) {
        return looks.getOrDefault(player, "");
    }

    public int radius(UUID player) {
        return radii.getOrDefault(player, 0);
    }

    public void built(UUID player, String look, int radius) {
        looks.put(player, look);
        radii.put(player, radius);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> e : slots.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Player", e.getKey());
            t.putInt("Slot", e.getValue());
            t.putString("Look", looks.getOrDefault(e.getKey(), ""));
            t.putInt("Radius", radii.getOrDefault(e.getKey(), 0));
            list.add(t);
        }
        tag.put("Islands", list);
        tag.putInt("Next", next);
        return tag;
    }

    static InnerWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        InnerWorldData data = new InnerWorldData();
        for (Tag t : tag.getList("Islands", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            UUID player = c.getUUID("Player");
            data.slots.put(player, c.getInt("Slot"));
            data.looks.put(player, c.getString("Look"));
            data.radii.put(player, c.getInt("Radius"));
        }
        data.next = Math.max(tag.getInt("Next"), data.slots.values().stream().mapToInt(i -> i + 1).max().orElse(0));
        return data;
    }
}
