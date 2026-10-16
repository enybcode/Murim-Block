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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * The Crab King's lair (CrabLairBuilder) is unique and secret: built once per world, under the heart of the first
 * Jiangnan Waterlands found at least {@link #MIN_DISTANCE} blocks from the world spawn, by rings of samples out to
 * {@link #SEARCH_RADIUS} from where the search starts: on the Murim map the Jiangnan Waterlands lie only in the East
 * region (docs/biomes/murim-map.md), so the search starts at the spawn if it is there, else a little inside the East
 * region on the way from the spawn toward it ({@link #origin}). None found, no lair. Its place is kept in the overworld's saved data.
 * Only in worlds with a normal overworld (not flat or test worlds). Searched and built over server ticks, as the
 * Cloud Cliff Sanctuary (SanctuaryPlacement).
 */
public final class CrabLairPlacement extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAME = Murimblock.MOD_ID + "_crab_lair";
    public static final int MIN_DISTANCE = 800, SEARCH_RADIUS = 6000;

    @Nullable private BlockPos centre;
    /** Whether the search ran and found no place (it is not run again). */
    private boolean none;

    public static final SavedData.Factory<CrabLairPlacement> FACTORY = new SavedData.Factory<>(CrabLairPlacement::new, CrabLairPlacement::load);

    public static CrabLairPlacement get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** The middle of the lair's floor, or null if this world has none (yet). */
    @Nullable public BlockPos centre() { return centre; }

    public void record(BlockPos centre) {
        this.centre = centre;
        setDirty();
    }

    @Nullable private static volatile Job job;

    /** True while this world's lair is still being searched for or built. */
    public static boolean building() {
        return job != null;
    }

    public static void onServerStarted(ServerStartedEvent event) {
        job = null;
        ServerLevel overworld = event.getServer().overworld();
        CrabLairPlacement data = get(overworld);
        if (data.centre != null || data.none) return;
        var source = overworld.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof MultiNoiseBiomeSource) && !(source instanceof com.murimblock.world.map.MurimBiomeSource)) return;
        job = new Job(overworld, data);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        Job current = job;
        if (current == null || current.level.getServer() != event.getServer()) return;
        // After the sanctuary: one big build at a time.
        if (SanctuaryPlacement.building()) return;
        if (!current.tick()) job = null;
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        job = null;
    }

    static final class Job {
        static final long SEARCH_BUDGET_NANOS = 4_000_000L, BUILD_BUDGET_NANOS = 8_000_000L;
        static final int MARGIN_CHUNKS = 4, LOAD_TIMEOUT_TICKS = 600;
        static final TicketType<ChunkPos> TICKET = TicketType.create(Murimblock.MOD_ID + ":crab_lair", Comparator.comparingLong(ChunkPos::toLong));

        final ServerLevel level;
        final CrabLairPlacement data;
        final Search search;
        final long started = System.nanoTime();
        @Nullable BlockPos site;
        int loadTicks;
        @Nullable CrabLairBuilder builder;

        Job(ServerLevel level, CrabLairPlacement data) {
            this.level = level;
            this.data = data;
            this.search = new Search(level, origin(level), level.getSharedSpawnPos());
        }

        /** One tick of work; false when done. */
        boolean tick() {
            if (site == null) {
                Search.Result found = search.run(SEARCH_BUDGET_NANOS);
                if (found == null) return true;
                if (found.site() == null) {
                    data.none = true;
                    data.setDirty();
                    LOGGER.info("No Jiangnan Waterlands within {} blocks of the search origin: this world has no Crab King's lair", SEARCH_RADIUS);
                    return false;
                }
                site = found.site();
                ChunkPos c = new ChunkPos(site);
                level.getChunkSource().addRegionTicket(TICKET, c, MARGIN_CHUNKS, c);
                return true;
            }
            if (builder == null) {
                if (!chunksReady() && ++loadTicks < LOAD_TIMEOUT_TICKS) return true;
                int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, site.getX(), site.getZ());
                builder = new CrabLairBuilder(level, site.getX(), surface, site.getZ(), level.getSeed() ^ 0xC7AB);
            }
            if (builder.buildSome(BUILD_BUDGET_NANOS)) return true;
            ChunkPos c = new ChunkPos(site);
            level.getChunkSource().removeRegionTicket(TICKET, c, MARGIN_CHUNKS, c);
            data.record(builder.centre());
            LOGGER.info("Crab King's lair built at {}: {} ms in all, {} biome samples", builder.centre(), (System.nanoTime() - started) / 1_000_000L, search.samples());
            return false;
        }

        private boolean chunksReady() {
            int cx = site.getX() >> 4, cz = site.getZ() >> 4;
            for (int x = cx - MARGIN_CHUNKS; x <= cx + MARGIN_CHUNKS; x++) for (int z = cz - MARGIN_CHUNKS; z <= cz + MARGIN_CHUNKS; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            return true;
        }
    }

    /**
     * Where the search starts: the spawn when it is in the East region of the Murim map (or the world has no Murim map),
     * else 3000 blocks inside the East region along the way from the spawn toward the region's middle.
     */
    static BlockPos origin(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof com.murimblock.world.map.MurimBiomeSource)) return spawn;
        if (com.murimblock.world.map.MurimMap.region(spawn.getX(), spawn.getZ()) == com.murimblock.world.map.MurimMap.Region.EAST) return spawn;
        double tx = com.murimblock.world.map.MurimMap.RADIUS * 0.55, tz = 0;
        double dx = tx - spawn.getX(), dz = tz - spawn.getZ(), d = Math.hypot(dx, dz);
        for (double t = 0; t <= d; t += 250) {
            int x = (int) (spawn.getX() + dx / d * t), z = (int) (spawn.getZ() + dz / d * t);
            if (com.murimblock.world.map.MurimMap.region(x, z) == com.murimblock.world.map.MurimMap.Region.EAST)
                return new BlockPos((int) (x + dx / d * 3000), 64, (int) (z + dz / d * 3000));
        }
        return new BlockPos((int) tx, 64, (int) tz);
    }

    /**
     * Rings of biome samples every 32 blocks round the search's origin, never nearer than MIN_DISTANCE to the spawn: the nearest sample whose 7 x 7
     * neighbourhood is mostly Jiangnan Waterlands (a heart of it, room for the cavern under it).
     */
    static final class Search {
        static final int STEP = 32, RINGS = SEARCH_RADIUS / STEP;
        record Result(@Nullable BlockPos site) { }
        private final BiomeSource source;
        private final Climate.Sampler sampler;
        private final BlockPos spawn, origin;
        private final Map<Long, Holder<Biome>> cache = new HashMap<>();
        private int ring;

        Search(ServerLevel level, BlockPos origin, BlockPos spawn) {
            this.origin = origin;
            this.source = level.getChunkSource().getGenerator().getBiomeSource();
            this.sampler = level.getChunkSource().randomState().sampler();
            this.spawn = spawn;
        }

        int samples() { return cache.size(); }

        private boolean jiangnan(int i, int j) {
            return cache.computeIfAbsent(((long) i << 32) ^ (j & 0xffffffffL), k ->
                    source.getNoiseBiome((origin.getX() + i * STEP) >> 2, 64 >> 2, (origin.getZ() + j * STEP) >> 2, sampler)).is(MurimBiomes.JIANGNAN_WATERLANDS);
        }

        /** Searches for about the budget: a result once found or given up on, else null. */
        @Nullable Result run(long budgetNanos) {
            long end = System.nanoTime() + budgetNanos;
            while (ring <= RINGS) {
                BlockPos best = null;
                double bestDist = Double.MAX_VALUE;
                for (int i = -ring; i <= ring; i++) for (int j = -ring; j <= ring; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
                    int x = origin.getX() + i * STEP, z = origin.getZ() + j * STEP;
                    if (Math.hypot(x - spawn.getX(), z - spawn.getZ()) < MIN_DISTANCE) continue;
                    if (!jiangnan(i, j)) continue;
                    int in = 0;
                    for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) if (jiangnan(i + a, j + b)) in++;
                    if (in < 36) continue;
                    double d = Math.hypot(i, j);
                    if (d < bestDist) { bestDist = d; best = new BlockPos(x, 64, z); }
                }
                ring++;
                if (best != null) return new Result(best);
                if (System.nanoTime() >= end) return null;
            }
            return new Result(null);
        }
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (centre != null) tag.put("centre", NbtUtils.writeBlockPos(centre));
        if (none) tag.putBoolean("none", true);
        return tag;
    }

    private static CrabLairPlacement load(CompoundTag tag, HolderLookup.Provider registries) {
        CrabLairPlacement data = new CrabLairPlacement();
        NbtUtils.readBlockPos(tag, "centre").ifPresent(p -> data.centre = p);
        data.none = tag.getBoolean("none");
        return data;
    }
}
