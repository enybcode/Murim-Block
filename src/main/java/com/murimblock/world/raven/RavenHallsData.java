package com.murimblock.world.raven;

import com.murimblock.Murimblock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The Raven Halls' bookkeeping (RavenHalls), saved with the overworld: each player's place in the halls (a slot, far
 * from the others), whether their hall is built, and their perches in the world (three at most, oldest first).
 */
public final class RavenHallsData extends SavedData {
    private static final String NAME = Murimblock.MOD_ID + "_raven_halls";
    public static final int MAX_PERCHES = 3;

    private final Map<UUID, Integer> slots = new HashMap<>();
    private final Set<UUID> built = new HashSet<>();
    private final Map<UUID, List<GlobalPos>> perches = new HashMap<>();
    private int next;

    public static final SavedData.Factory<RavenHallsData> FACTORY = new SavedData.Factory<>(RavenHallsData::new, RavenHallsData::load);

    public static RavenHallsData get(ServerLevel overworld) {
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

    public boolean built(UUID player) {
        return built.contains(player);
    }

    public void markBuilt(UUID player) {
        built.add(player);
        setDirty();
    }

    /** The player's perches, oldest first. */
    public List<GlobalPos> perches(UUID player) {
        return List.copyOf(perches.getOrDefault(player, List.of()));
    }

    /** Records a new perch; returns the oldest one it pushes out (to be taken down), or null. */
    public GlobalPos addPerch(UUID player, GlobalPos at) {
        List<GlobalPos> list = perches.computeIfAbsent(player, k -> new ArrayList<>());
        list.remove(at);
        list.add(at);
        GlobalPos out = list.size() > MAX_PERCHES ? list.removeFirst() : null;
        setDirty();
        return out;
    }

    public void removePerch(GlobalPos at) {
        for (List<GlobalPos> list : perches.values()) list.remove(at);
        setDirty();
    }

    /** Whose perch this is, or null. */
    public UUID ownerOf(GlobalPos at) {
        for (Map.Entry<UUID, List<GlobalPos>> e : perches.entrySet()) if (e.getValue().contains(at)) return e.getKey();
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> e : slots.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Player", e.getKey());
            t.putInt("Slot", e.getValue());
            t.putBoolean("Built", built.contains(e.getKey()));
            ListTag ps = new ListTag();
            for (GlobalPos p : perches.getOrDefault(e.getKey(), List.of())) {
                CompoundTag pt = new CompoundTag();
                pt.putString("Dim", p.dimension().location().toString());
                pt.put("Pos", NbtUtils.writeBlockPos(p.pos()));
                ps.add(pt);
            }
            t.put("Perches", ps);
            list.add(t);
        }
        tag.put("Players", list);
        tag.putInt("Next", next);
        return tag;
    }

    private static RavenHallsData load(CompoundTag tag, HolderLookup.Provider registries) {
        RavenHallsData data = new RavenHallsData();
        for (Tag t0 : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) t0;
            UUID player = t.getUUID("Player");
            data.slots.put(player, t.getInt("Slot"));
            if (t.getBoolean("Built")) data.built.add(player);
            List<GlobalPos> ps = new ArrayList<>();
            for (Tag p0 : t.getList("Perches", Tag.TAG_COMPOUND)) {
                CompoundTag p = (CompoundTag) p0;
                ResourceKey<net.minecraft.world.level.Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(p.getString("Dim")));
                NbtUtils.readBlockPos(p, "Pos").ifPresent(pos -> ps.add(GlobalPos.of(dim, pos)));
            }
            data.perches.put(player, ps);
        }
        data.next = tag.getInt("Next");
        return data;
    }
}
