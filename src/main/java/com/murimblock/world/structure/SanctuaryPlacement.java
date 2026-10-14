package com.murimblock.world.structure;

import com.mojang.logging.LogUtils;
import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiomes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * The Cloud Cliff Sanctuary is unique: built once per world, in the cloud-sea peaks biome nearest to the world spawn
 * (within 4000 blocks; otherwise in the nearest Murim biome, since it brings its own spire), never on the spawn itself:
 * players start in the wild and find it with the sect token. Its position is kept in the overworld saved data. Only in
 * worlds with a normal multi-noise overworld (not flat or test worlds). The work is spread over server ticks ({@link Job}).
 */
public final class SanctuaryPlacement extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAME = Murimblock.MOD_ID + "_sanctuary";
    public static final int SEARCH_RADIUS = 4000;
    /** Closest the spire may stand to the world spawn, so nobody spawns on or inside it. */
    public static final int MIN_SPAWN_DISTANCE = 64;
    /** The registered structure /locate knows the sanctuary by. */
    public static final ResourceKey<Structure> STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "cloud_cliff_sanctuary"));
    private static final TagKey<Biome> MURIM = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "murim_biomes"));

    @Nullable private BlockPos centre;
    @Nullable private BlockPos foot;
    private int top;

    public static final SavedData.Factory<SanctuaryPlacement> FACTORY = new SavedData.Factory<>(SanctuaryPlacement::new, SanctuaryPlacement::load);

    public static SanctuaryPlacement get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Whether a position of the overworld lies within `reach` blocks (horizontally) of the sanctuary's spire. */
    public static boolean near(ServerLevel overworld, BlockPos pos, double reach) {
        BlockPos centre = get(overworld).centre();
        if (centre == null) return false;
        double dx = pos.getX() - centre.getX(), dz = pos.getZ() - centre.getZ();
        return dx * dx + dz * dz <= reach * reach;
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

    /** The build in progress, run a few milliseconds per server tick (null when there is none). */
    @Nullable private static volatile Job job;

    /** True while the sanctuary of this world is still being searched for or built. */
    public static boolean building() {
        return job != null;
    }

    public static void onServerStarted(ServerStartedEvent event) {
        job = null;
        ServerLevel overworld = event.getServer().overworld();
        SanctuaryPlacement data = get(overworld);
        if (data.centre != null) return;
        if (!(overworld.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource)) return;
        job = new Job(overworld, data);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Job current = job;
        if (current == null || current.level.getServer() != event.getServer()) return;
        long start = System.nanoTime();
        if (!current.tick()) job = null;
        current.longestTick = Math.max(current.longestTick, System.nanoTime() - start);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        // An unfinished build starts again (identically) on the next start; its forced chunks are released then.
        job = null;
    }

    /**
     * Search, chunk loading and building spread over server ticks, so neither the first start of a world nor the
     * players already in it freeze: about {@link #SEARCH_BUDGET_NANOS} of biome sampling, then
     * {@link #BUILD_BUDGET_NANOS} of block placing per tick. The chunks of the footprint are held by a ticket meanwhile
     * (generated by the server's workers, not on the main thread) and released at the end.
     */
    static final class Job {
        static final long SEARCH_BUDGET_NANOS = 5_000_000L, BUILD_BUDGET_NANOS = 10_000_000L;
        /** Chunks around the site loaded before building (the footprint and a margin, so neighbouring decoration is done). */
        static final int MARGIN_CHUNKS = 4;
        /** If the forced chunks are still not ready after this long, load them directly. */
        static final int LOAD_TIMEOUT_TICKS = 600;
        static final TicketType<ChunkPos> TICKET = TicketType.create(Murimblock.MOD_ID + ":sanctuary", Comparator.comparingLong(ChunkPos::toLong));

        final ServerLevel level;
        final SanctuaryPlacement data;
        final SiteSearch search;
        final long started = System.nanoTime();
        @Nullable BlockPos site;
        int loadTicks;
        @Nullable SanctuaryBuilder builder;
        int buildTicks;
        long longestTick;

        Job(ServerLevel level, SanctuaryPlacement data) {
            this.level = level;
            this.data = data;
            this.search = new SiteSearch(level, level.getSharedSpawnPos());
        }

        /** One tick of work; false when the sanctuary is built. */
        boolean tick() {
            if (site == null) {
                BlockPos found = search.run(SEARCH_BUDGET_NANOS);
                if (found == null) return true;
                site = awayFromSpawn(found, level.getSharedSpawnPos());
                forceChunks(true);
                return true;
            }
            if (builder == null) {
                if (!chunksReady() && ++loadTicks < LOAD_TIMEOUT_TICKS) return true;
                builder = prepare(level, site);
            }
            buildTicks++;
            if (builder.buildSome(BUILD_BUDGET_NANOS)) return true;
            forceChunks(false);
            BlockPos foot = builder.foot();
            data.record(new BlockPos(site.getX(), builder.top() - SanctuaryBuilder.HEIGHT, site.getZ()), foot, builder.top());
            LOGGER.info("Cloud Cliff Sanctuary built at {} (top y {}), road foot {}: {} ms in all, {} build ticks, longest tick {} ms, {} biome samples",
                    data.centre, builder.top(), foot, (System.nanoTime() - started) / 1_000_000L, buildTicks, longestTick / 1_000_000L, search.samples());
            return false;
        }

        /**
         * Keeps the footprint loaded with a region ticket (not forced chunks: forcing a chunk generates it at once on
         * the main thread, which froze the server for seconds). Tickets are not saved; a restarted build adds its own.
         */
        private void forceChunks(boolean force) {
            ChunkPos centre = new ChunkPos(site);
            if (force) level.getChunkSource().addRegionTicket(TICKET, centre, MARGIN_CHUNKS, centre);
            else level.getChunkSource().removeRegionTicket(TICKET, centre, MARGIN_CHUNKS, centre);
        }

        private boolean chunksReady() {
            int cx = site.getX() >> 4, cz = site.getZ() >> 4;
            for (int x = cx - MARGIN_CHUNKS; x <= cx + MARGIN_CHUNKS; x++) for (int z = cz - MARGIN_CHUNKS; z <= cz + MARGIN_CHUNKS; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false; // generated and loaded, not just requested
            return true;
        }
    }

    /**
     * Measures the site and returns its builder: the road starts on the lowest side around the spire, and the arrival
     * landing sits on the ground there. Loads the footprint chunks if they are not loaded yet.
     */
    static SanctuaryBuilder prepare(ServerLevel level, BlockPos site) {
        int cx = site.getX() >> 4, cz = site.getZ() >> 4;
        for (int x = cx - Job.MARGIN_CHUNKS; x <= cx + Job.MARGIN_CHUNKS; x++) for (int z = cz - Job.MARGIN_CHUNKS; z <= cz + Job.MARGIN_CHUNKS; z++) level.getChunk(x, z);
        double roadStart = Math.PI / 2;
        int ground = Integer.MAX_VALUE;
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (site.getX() + Math.cos(a) * SanctuaryBuilder.FOOT_RADIUS),
                    (int) (site.getZ() + Math.sin(a) * SanctuaryBuilder.FOOT_RADIUS));
            if (h < ground) { ground = h; roadStart = a; }
        }
        ground = Math.max(level.getSeaLevel() + 1, Math.min(ground, level.getMaxBuildHeight() - SanctuaryBuilder.HEIGHT - 40));
        return new SanctuaryBuilder(level, site.getX(), ground, site.getZ(), RandomSource.create(level.getSeed()), roadStart);
    }

    /** Pushes a site straight away from the spawn until it is far enough (east if it is right on it). */
    static BlockPos awayFromSpawn(BlockPos site, BlockPos spawn) {
        double dx = site.getX() - spawn.getX(), dz = site.getZ() - spawn.getZ(), d = Math.hypot(dx, dz);
        if (d >= MIN_SPAWN_DISTANCE) return site;
        if (d < 1) { dx = 1; dz = 0; d = 1; }
        return new BlockPos((int) Math.round(spawn.getX() + dx / d * MIN_SPAWN_DISTANCE), site.getY(), (int) Math.round(spawn.getZ() + dz / d * MIN_SPAWN_DISTANCE));
    }

    /**
     * The point most inside the cloud-sea peaks nearest to the spawn: biome samples every 32 blocks out to 4000, ring by
     * ring; the nearest sample whose 7x7 neighbourhood is mostly peaks. Falls back to the nearest Murim biome sample,
     * then the spawn. Each grid cell is sampled once (cached), and the search stops and resumes between ticks.
     */
    static final class SiteSearch {
        static final int STEP = 32, RINGS = SEARCH_RADIUS / STEP;
        private final BiomeSource source;
        private final Climate.Sampler sampler;
        private final BlockPos spawn;
        private final Map<Long, Holder<Biome>> cache = new HashMap<>();
        private int ring;
        @Nullable private BlockPos murim;
        private double murimDist = Double.MAX_VALUE;

        SiteSearch(ServerLevel level, BlockPos spawn) {
            this.source = level.getChunkSource().getGenerator().getBiomeSource();
            this.sampler = level.getChunkSource().randomState().sampler();
            this.spawn = spawn;
        }

        int samples() { return cache.size(); }

        private Holder<Biome> biome(int i, int j) {
            return cache.computeIfAbsent(((long) i << 32) ^ (j & 0xffffffffL), k ->
                    source.getNoiseBiome((spawn.getX() + i * STEP) >> 2, 80 >> 2, (spawn.getZ() + j * STEP) >> 2, sampler));
        }

        private BlockPos at(int i, int j) {
            return new BlockPos(spawn.getX() + i * STEP, 80, spawn.getZ() + j * STEP);
        }

        /** Searches for about the budget; returns the site once found (or given up on), else null. */
        @Nullable BlockPos run(long budgetNanos) {
            long end = System.nanoTime() + budgetNanos;
            while (ring <= RINGS) {
                BlockPos best = null;
                double bestDist = Double.MAX_VALUE;
                for (int i = -ring; i <= ring; i++) for (int j = -ring; j <= ring; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
                    Holder<Biome> here = biome(i, j);
                    double d = Math.hypot(i, j);
                    if (here.is(MURIM) && d < murimDist) { murimDist = d; murim = at(i, j); }
                    if (!here.is(MurimBiomes.CLOUD_SEA_PEAKS)) continue;
                    int inside = 0;
                    for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) if (biome(i + a, j + b).is(MurimBiomes.CLOUD_SEA_PEAKS)) inside++;
                    if (inside < 30) continue;
                    if (d < bestDist) { bestDist = d; best = at(i, j); }
                }
                ring++;
                if (best != null) return best;
                if (System.nanoTime() >= end) return null;
            }
            return murim != null ? murim : spawn;
        }
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
