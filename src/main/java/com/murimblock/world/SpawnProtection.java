package com.murimblock.world;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

/** Named, per-dimension no-random-spawn cuboids for player builds and imported schematics. */
public final class SpawnProtection extends SavedData {
    public static final Factory<SpawnProtection> FACTORY = new Factory<>(SpawnProtection::new, SpawnProtection::load);
    private final Map<String, BoundingBox> regions = new LinkedHashMap<>();
    public static SpawnProtection get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "murimblock_spawn_protection");
    }
    public boolean contains(BlockPos pos) { return regions.values().stream().anyMatch(box -> box.isInside(pos)); }
    public Map<String, BoundingBox> regions() { return Map.copyOf(regions); }
    public void protect(String name, BlockPos first, BlockPos second) {
        regions.put(name, BoundingBox.fromCorners(first, second)); setDirty();
    }
    public boolean remove(String name) {
        if (regions.remove(name) == null) return false;
        setDirty(); return true;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        regions.forEach((name, box) -> {
            CompoundTag entry = new CompoundTag(); entry.putString("Name", name);
            entry.putIntArray("Bounds", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            list.add(entry);
        });
        tag.put("Regions", list); return tag;
    }
    public static SpawnProtection load(CompoundTag tag, HolderLookup.Provider lookup) {
        SpawnProtection data = new SpawnProtection();
        for (Tag raw : tag.getList("Regions", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw; int[] b = entry.getIntArray("Bounds");
            if (b.length == 6 && !entry.getString("Name").isBlank()) data.regions.put(entry.getString("Name"),
                    BoundingBox.fromCorners(new BlockPos(b[0], b[1], b[2]), new BlockPos(b[3], b[4], b[5])));
        }
        return data;
    }
}
