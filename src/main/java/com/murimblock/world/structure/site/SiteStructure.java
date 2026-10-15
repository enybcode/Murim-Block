package com.murimblock.world.structure.site;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murimblock.world.MurimWorldgen;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A Murim site as a real structure (structure sets place it, /locate finds it): at the centre of its chunk, its front
 * turned towards the lowest ground, on levels the site chooses from the terrain (or not at all where it does not suit),
 * wholly inside its biome. One piece per part; the JSON's terrain adaptation (beard_thin) shapes the ground round them.
 */
public final class SiteStructure extends Structure {
    public static final MapCodec<SiteStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            SitePlan.CODEC.fieldOf("plan").forGetter(SiteStructure::plan)
    ).apply(i, SiteStructure::new));

    private final SitePlan plan;

    public SiteStructure(StructureSettings settings, SitePlan plan) {
        super(settings);
        this.plan = plan;
    }

    public SitePlan plan() {
        return plan;
    }

    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        Site site = plan.site();
        if (!site.allowedAt(x, z)) { count("place"); return Optional.empty(); }
        if (site.keepsClearOfOtherSites() && nearOtherSite(context, site)) { count("crowded"); return Optional.empty(); }
        // The biome first: it is cheap, and /locate asks every cell of the grid.
        int sea = context.chunkGenerator().getSeaLevel(), r0 = site.reach();
        for (int dx = -r0; dx <= r0; dx += r0) for (int dz = -r0; dz <= r0; dz += r0) {
            if (Math.abs(dx) != Math.abs(dz)) continue; // centre and corners
            var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x + dx), QuartPos.fromBlock(sea), QuartPos.fromBlock(z + dz), context.randomState().sampler());
            if (!context.validBiome().test(biome)) { count("biome"); return Optional.empty(); }
        }
        Terrain terrain = new Terrain(context);
        // Front (local +z) towards the lowest of the four sides.
        int d = site.probe();
        int south = terrain.ground(x, z + d), west = terrain.ground(x - d, z), north = terrain.ground(x, z - d), east = terrain.ground(x + d, z);
        int low = Math.min(Math.min(south, west), Math.min(north, east));
        Rotation rotation = low == south ? Rotation.NONE : low == west ? Rotation.CLOCKWISE_90 : low == north ? Rotation.CLOCKWISE_180 : Rotation.COUNTERCLOCKWISE_90;
        int[] levels = site.levels(terrain.probe(x, z, rotation));
        if (levels == null) { count("terrain"); return Optional.empty(); }
        // The whole site in its own biome.
        int r = site.reach();
        for (int dx = -r; dx <= r; dx += r) for (int dz = -r; dz <= r; dz += r) {
            var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x + dx), QuartPos.fromBlock(levels[0]), QuartPos.fromBlock(z + dz), context.randomState().sampler());
            if (!context.validBiome().test(biome)) { count("edge"); return Optional.empty(); }
        }
        count("ok");
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, levels[0], z), builder -> {
            for (int i = 0; i < site.parts().size(); i++) builder.addPiece(new SitePiece(plan, i, x, z, rotation, seed, levels));
        }));
    }

    /** How far apart two sites' edges are kept (blocks), for a site that keeps clear of the others. */
    static final int CLEARANCE = 24;

    /**
     * Whether another Murim site may stand too near this chunk: for every other structure set of the mod (a random
     * spread), the chunk its grid offers in each cell around, if the biome there suits that site. It does not ask the
     * other site's terrain, so it errs on the side of stepping aside.
     */
    private boolean nearOtherSite(GenerationContext context, Site site) {
        ChunkPos chunk = context.chunkPos();
        var sets = context.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        for (var entry : sets.entrySet()) {
            if (!entry.getKey().location().getNamespace().equals(com.murimblock.Murimblock.MOD_ID)) continue;
            var set = entry.getValue();
            if (!(set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread)) continue;
            for (var choice : set.structures()) {
                Structure other = choice.structure().value();
                if (other == this) continue;
                int otherReach = other instanceof SiteStructure s ? s.plan().site().reach() : 32;
                int reach = (site.reach() + otherReach + CLEARANCE + 15) / 16;
                int spacing = spread.spacing();
                for (int rx = Math.floorDiv(chunk.x - reach, spacing); rx <= Math.floorDiv(chunk.x + reach, spacing); rx++)
                    for (int rz = Math.floorDiv(chunk.z - reach, spacing); rz <= Math.floorDiv(chunk.z + reach, spacing); rz++) {
                        ChunkPos there = spread.getPotentialStructureChunk(context.seed(), rx * spacing, rz * spacing);
                        if (Math.abs(there.x - chunk.x) > reach || Math.abs(there.z - chunk.z) > reach) continue;
                        var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(there.getMiddleBlockX()),
                                QuartPos.fromBlock(context.chunkGenerator().getSeaLevel()), QuartPos.fromBlock(there.getMiddleBlockZ()), context.randomState().sampler());
                        if (other.biomes().contains(biome)) return true;
                    }
            }
        }
        return false;
    }

    /** Diagnostics (site tour): how many places were refused for their biome, terrain or edge, and accepted. */
    public static final ConcurrentHashMap<String, AtomicInteger> STATS = new ConcurrentHashMap<>();

    private void count(String outcome) {
        STATS.computeIfAbsent(plan.getSerializedName() + " " + outcome, k -> new AtomicInteger()).incrementAndGet();
    }

    @Override public StructureType<?> type() {
        return MurimWorldgen.SITE_STRUCTURE.get();
    }

    /** The terrain as the noise makes it (before features), read column by column and remembered. */
    static final class Terrain {
        private final GenerationContext context;
        private final Long2ObjectOpenHashMap<int[]> columns = new Long2ObjectOpenHashMap<>();

        Terrain(GenerationContext context) {
            this.context = context;
        }

        /** {top solid block, top block including water}. */
        int[] column(int x, int z) {
            return columns.computeIfAbsent(ChunkPos.asLong(x, z), k -> {
                var height = context.heightAccessor();
                NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, height, context.randomState());
                int surface = Integer.MIN_VALUE, ground = height.getMinBuildHeight();
                for (int y = height.getMaxBuildHeight() - 1; y >= height.getMinBuildHeight(); y--) {
                    BlockState s = column.getBlock(y);
                    if (s.isAir()) continue;
                    if (surface == Integer.MIN_VALUE) surface = y;
                    if (s.getFluidState().isEmpty()) { ground = y; break; }
                }
                return new int[]{ground, surface == Integer.MIN_VALUE ? ground : surface};
            });
        }

        int ground(int x, int z) {
            return column(x, z)[0];
        }

        Site.Probe probe(int ox, int oz, Rotation rotation) {
            Canvas frame = Canvas.dry(ox, oz, rotation);
            int sea = context.chunkGenerator().getSeaLevel();
            return new Site.Probe() {
                @Override public int ground(int x, int z) { return column(frame.wx(x, z), frame.wz(x, z))[0]; }
                @Override public boolean wet(int x, int z) { int[] c = column(frame.wx(x, z), frame.wz(x, z)); return c[1] > c[0]; }
                @Override public int seaLevel() { return sea; }
            };
        }
    }
}
