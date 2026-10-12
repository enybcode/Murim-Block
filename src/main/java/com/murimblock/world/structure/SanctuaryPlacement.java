package com.murimblock.world.structure;

import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiomes;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * The Cloud Cliff Sanctuary is unique: built once per world, in the cloud-sea peaks biome nearest to the world spawn
 * (within 4000 blocks; otherwise in the nearest Murim biome, since it brings its own spire), never on the spawn itself:
 * players start in the wild and find it with the sect token. Its position is kept in the overworld saved data. Only in worlds with a normal multi-noise overworld (not flat or test worlds).
 */
public final class SanctuaryPlacement extends SavedData {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String NAME = Murimblock.MOD_ID + "_sanctuary";
    public static final int SEARCH_RADIUS = 4000;
    /** Closest the spire may stand to the world spawn, so nobody spawns on or inside it. */
    public static final int MIN_SPAWN_DISTANCE = 64;
    /** The registered structure /locate knows the sanctuary by. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> STRUCTURE =
            net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "cloud_cliff_sanctuary"));
    private static final TagKey<Biome> MURIM = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "murim_biomes"));

    @Nullable private BlockPos centre;
    @Nullable private BlockPos foot;
    private int top;

    public static final SavedData.Factory<SanctuaryPlacement> FACTORY = new SavedData.Factory<>(SanctuaryPlacement::new, SanctuaryPlacement::load);

    public static SanctuaryPlacement get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Centre of the spire at its base, or null if the sanctuary is not built in this world. */
    @Nullable public BlockPos centre() { return centre; }
    /** The foot of the plank road. */
    @Nullable public BlockPos foot() { return foot; }
    public int top() { return top; }

    /** For tests: forget the recorded sanctuary. */
    public void clear() {
        centre = null;
        foot = null;
        top = 0;
        setDirty();
    }

    /** For tests and commands: record a sanctuary built elsewhere. */
    public void record(BlockPos centre, BlockPos foot, int top) {
        this.centre = centre;
        this.foot = foot;
        this.top = top;
        setDirty();
    }

    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        SanctuaryPlacement data = get(overworld);
        if (data.centre != null) return;
        if (!(overworld.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource)) return;
        BlockPos spawn = overworld.getSharedSpawnPos();
        BlockPos site = findSite(overworld, spawn);
        build(overworld, site, data);
    }

    /** Builds the sanctuary on the site's ground and records it. The world spawn stays where it is. */
    public static void build(ServerLevel level, BlockPos site, SanctuaryPlacement data) {
        // Generate the whole footprint (and a margin, so neighbouring decoration is done before we build).
        int cx = site.getX() >> 4, cz = site.getZ() >> 4;
        for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 4; z <= cz + 4; z++) level.getChunk(x, z);
        // The road starts on the lowest side around the spire; the arrival landing sits on the ground there.
        double roadStart = Math.PI / 2;
        int ground = Integer.MAX_VALUE;
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (site.getX() + Math.cos(a) * SanctuaryBuilder.FOOT_RADIUS),
                    (int) (site.getZ() + Math.sin(a) * SanctuaryBuilder.FOOT_RADIUS));
            if (h < ground) { ground = h; roadStart = a; }
        }
        ground = Math.max(level.getSeaLevel() + 1, Math.min(ground, level.getMaxBuildHeight() - SanctuaryBuilder.HEIGHT - 40));
        SanctuaryBuilder builder = new SanctuaryBuilder(level, site.getX(), ground, site.getZ(), RandomSource.create(level.getSeed()), roadStart);
        builder.build();
        BlockPos foot = builder.foot();
        data.record(new BlockPos(site.getX(), ground, site.getZ()), foot, builder.top());
        LOGGER.info("Cloud Cliff Sanctuary built at {} (top y {}), road foot {}", data.centre, builder.top(), foot);
    }

    /**
     * The point most inside the cloud-sea peaks nearest to the spawn: biome samples every 32 blocks out to 4000; the
     * nearest sample whose 7x7 neighbourhood is mostly peaks. Falls back to the nearest Murim biome, then next to the
     * spawn. Always at least {@link #MIN_SPAWN_DISTANCE} blocks from the spawn.
     */
    static BlockPos findSite(ServerLevel level, BlockPos spawn) {
        return awayFromSpawn(nearestSite(level, spawn), spawn);
    }

    /** Pushes a site straight away from the spawn until it is far enough (east if it is right on it). */
    static BlockPos awayFromSpawn(BlockPos site, BlockPos spawn) {
        double dx = site.getX() - spawn.getX(), dz = site.getZ() - spawn.getZ(), d = Math.hypot(dx, dz);
        if (d >= MIN_SPAWN_DISTANCE) return site;
        if (d < 1) { dx = 1; dz = 0; d = 1; }
        return new BlockPos((int) Math.round(spawn.getX() + dx / d * MIN_SPAWN_DISTANCE), site.getY(), (int) Math.round(spawn.getZ() + dz / d * MIN_SPAWN_DISTANCE));
    }

    private static BlockPos nearestSite(ServerLevel level, BlockPos spawn) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        int step = 32, n = SEARCH_RADIUS / step;
        java.util.function.BiPredicate<Integer, Integer> peaks = (i, j) -> source.getNoiseBiome((spawn.getX() + i * step) >> 2, 80 >> 2,
                (spawn.getZ() + j * step) >> 2, sampler).is(MurimBiomes.CLOUD_SEA_PEAKS);
        // Ring by ring outwards, so the first good sample is the nearest.
        for (int ring = 0; ring <= n; ring++) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int i = -ring; i <= ring; i++) for (int j = -ring; j <= ring; j++) {
                if (Math.max(Math.abs(i), Math.abs(j)) != ring || !peaks.test(i, j)) continue;
                int inside = 0;
                for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) if (peaks.test(i + a, j + b)) inside++;
                if (inside < 30) continue;
                double d = Math.hypot(i, j);
                if (d < bestDist) { bestDist = d; best = new BlockPos(spawn.getX() + i * step, 80, spawn.getZ() + j * step); }
            }
            if (best != null) return best;
        }
        var murim = level.findClosestBiome3d(h -> h.is(MURIM), spawn, SEARCH_RADIUS, 32, 64);
        return murim != null ? murim.getFirst() : spawn;
    }

    // ---------------------------------------------------------------- saving

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (centre != null) {
            tag.put("centre", NbtUtils.writeBlockPos(centre));
            tag.put("foot", NbtUtils.writeBlockPos(foot));
            tag.putInt("top", top);
        }
        return tag;
    }

    private static SanctuaryPlacement load(CompoundTag tag, HolderLookup.Provider registries) {
        SanctuaryPlacement data = new SanctuaryPlacement();
        NbtUtils.readBlockPos(tag, "centre").ifPresent(p -> data.centre = p);
        NbtUtils.readBlockPos(tag, "foot").ifPresent(p -> data.foot = p);
        data.top = tag.getInt("top");
        return data;
    }
}
