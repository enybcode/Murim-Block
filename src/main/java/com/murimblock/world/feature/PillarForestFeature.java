package com.murimblock.world.feature;

import com.murimblock.world.MurimBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The stone forest of Wulingyuan (Zhangjiajie; Enzo, 17/10/2026, after his photographs; docs/biomes/wulingyuan_pillars.md):
 * thousands of quartz-sandstone pillars, the remains of one old plateau cut by water into columns 60 to 110 blocks tall,
 * so close that the gorges between them are a few blocks wide, their tops all near the same height and green with trees.
 * <p>
 * Not one pillar per placement but the whole forest, laid out in the world itself: a jittered grid of cells, one pillar
 * each, its centre, girth, height and look drawn from the cell alone. Each chunk builds only its own blocks of every
 * pillar that crosses it, from that same layout, so a pillar is whole across chunks and no two ever disagree. The tops
 * follow a slow plateau noise; each pillar steps in and out along its bedding planes, is fluted by rain, pale grey and
 * white with rusty bands and dark streaks, green on every ledge, vines hanging down its walls. Near the biome's edge and
 * near the Murim sites, a pillar is left out whole rather than cut.
 */
public class PillarForestFeature extends Feature<NoneFeatureConfiguration> {
    /** The layout's cell: one pillar per cell (blocks). */
    static final int CELL = 21;
    /** The old plateau's height (the pillars' tops), give or take its noise. */
    static final int PLATEAU = 168, PLATEAU_SWELL = 14;
    /** A pillar rises at least this far above its ground, or it is left out (no stubs on the hills). */
    static final int MIN_RISE = 30;

    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState(), DIRT = Blocks.DIRT.defaultBlockState();

    public PillarForestFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** One pillar of the layout. */
    record Pillar(double x, double z, double radius, int top, double squash, double turn, int bedding, long seed, boolean waisted) { }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long worldSeed = level.getSeed();
        SimplexNoise noise = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(worldSeed ^ 0x57A1B2C3L)));
        ChunkPos chunk = new ChunkPos(context.origin());
        int x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ();
        // The pillars that may cross this chunk: the cells round it.
        java.util.List<Pillar> pillars = new java.util.ArrayList<>();
        int ci0 = Math.floorDiv(x0, CELL) - 1, ci1 = Math.floorDiv(x0 + 15, CELL) + 1;
        int cj0 = Math.floorDiv(z0, CELL) - 1, cj1 = Math.floorDiv(z0 + 15, CELL) + 1;
        for (int i = ci0; i <= ci1; i++) for (int j = cj0; j <= cj1; j++) {
            Pillar p = pillar(level, context.chunkGenerator(), noise, worldSeed, i, j);
            if (p != null) pillars.add(p);
        }
        if (pillars.isEmpty()) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (Pillar p : pillars) {
            int reach = (int) Math.ceil(p.radius * 1.5) + 2;
            int minX = Math.max(x0, Mth.floor(p.x) - reach), maxX = Math.min(x0 + 15, Mth.floor(p.x) + reach);
            int minZ = Math.max(z0, Mth.floor(p.z) - reach), maxZ = Math.min(z0 + 15, Mth.floor(p.z) + reach);
            if (minX > maxX || minZ > maxZ) continue;
            for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                boolean above = false;
                for (int y = p.top; y >= ground - 3; y--) {
                    if (!inside(noise, p, x, y, z)) { above = false; continue; }
                    pos.set(x, y, z);
                    // Every rock face open to the sky (the top, each ledge) is soil, two blocks deep.
                    BlockState state = !above ? GRASS : y >= p.top - 1 || !inside(noise, p, x, y + 2, z) ? DIRT : rock(noise, p, x, y, z);
                    level.setBlock(pos, state, 2);
                    above = true;
                    any = true;
                }
            }
            vines(level, noise, p, x0, z0, pos);
        }
        return any;
    }

    /** The pillar of cell (i, j), or null where there is none (a gap, the biome's edge, a site, a mountain). */
    static Pillar pillar(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, SimplexNoise noise, long worldSeed, int i, int j) {
        long seed = mix(worldSeed, i, j);
        java.util.Random r = new java.util.Random(seed);
        // A gap now and then: a wider gorge, a clearing.
        if (r.nextFloat() < 0.15F) return null;
        double x = i * CELL + 2 + r.nextDouble() * (CELL - 4), z = j * CELL + 2 + r.nextDouble() * (CELL - 4);
        // Mostly slender, a few great ones: 3.5 to 10 blocks of radius.
        double radius = 3.5 + Math.pow(r.nextDouble(), 1.4) * 6.5;
        // The plateau's height there, and this pillar's own share of it (a few stand lower: worn down further).
        double plateau = PLATEAU + noise.getValue(x * 0.004, z * 0.004) * PLATEAU_SWELL;
        int top = (int) Math.round(plateau - (r.nextFloat() < 0.25F ? 6 + r.nextDouble() * 26 : r.nextDouble() * 7));
        double squash = 0.7 + r.nextDouble() * 0.3, turn = r.nextDouble() * Math.PI;
        int bedding = 5 + r.nextInt(5);
        boolean waisted = r.nextFloat() < 0.18F;
        int bx = Mth.floor(x), bz = Mth.floor(z);
        // Whole or not at all: its centre and its sides in the biome, off the sites, high enough above its ground.
        for (int k = 0; k < 5; k++) {
            int px = bx + (k == 1 ? (int) radius + 1 : k == 2 ? -(int) radius - 1 : 0), pz = bz + (k == 3 ? (int) radius + 1 : k == 4 ? -(int) radius - 1 : 0);
            // From the biome source itself: the same answer in every chunk, built or not.
            var sampler = level.getLevel().getChunkSource().randomState().sampler();
            if (!generator.getBiomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(px), net.minecraft.core.QuartPos.fromBlock(100),
                    net.minecraft.core.QuartPos.fromBlock(pz), sampler).is(MurimBiomes.WULINGYUAN_PILLARS)) return null;
        }
        if (AwayFromSitesFilter.nearSite(level, bx, bz, (int) radius + 6)) return null;
        // Its ground from the terrain's own noise (the same in every chunk, built or not).
        int ground = generator.getBaseHeight(bx, bz, Heightmap.Types.OCEAN_FLOOR_WG, level, level.getLevel().getChunkSource().randomState());
        if (ground > top - MIN_RISE) return null;
        return new Pillar(x, z, radius, top, squash, turn, bedding, seed, waisted);
    }

    /** Whether (x, y, z) is rock of pillar p. */
    static boolean inside(SimplexNoise noise, Pillar p, int x, int y, int z) {
        if (y > p.top) return false;
        double dx = x + 0.5 - p.x, dz = z + 0.5 - p.z;
        // Its own section: a rounded, squashed square, turned.
        double c = Math.cos(p.turn), s = Math.sin(p.turn);
        double u = dx * c + dz * s, v = (-dx * s + dz * c) / p.squash;
        double dist = Math.hypot(u, v), angle = Math.atan2(v, u);
        double section = Math.max(Math.abs(u), Math.abs(v)) * 0.55 + dist * 0.45;
        double r = p.radius;
        // Bedding planes: each layer steps in or out a little.
        int layer = Math.floorDiv(y, p.bedding);
        r += (((p.seed >>> (Math.floorMod(layer, 16) * 2)) & 3) - 1.5) * 0.35;
        // Rain flutes: grooves running down the walls (the angle sets them, the height barely changes them).
        r *= 1 + noise.getValue(Math.cos(angle) * 1.8 + p.x * 0.05, y * 0.018, Math.sin(angle) * 1.8 + p.z * 0.05) * 0.16;
        // Some are worn thin at the waist.
        if (p.waisted) {
            double t = (p.top - y) / 60.0;
            r *= 1 - 0.28 * Math.exp(-Math.pow((t - 0.55) / 0.18, 2));
        }
        // The top rim, a little rounded.
        if (y >= p.top - 1) r -= 0.6;
        return section <= Math.max(1.2, r);
    }

    /**
     * The rock at a block: pale grey and white quartz sandstone in long vertical streaks, dark wet runs, rusty bands
     * along some bedding planes and under the soil, a little moss.
     */
    static BlockState rock(SimplexNoise noise, Pillar p, int x, int y, int z) {
        double streak = noise.getValue(x * 0.45, y * 0.03, z * 0.45);
        double band = noise.getValue(y * 0.11 + (p.seed & 1023), 0, (p.seed >> 10 & 1023) * 0.1);
        double fine = noise.getValue(x * 0.9 + 31, y * 0.4, z * 0.9);
        // Rust: in bands, and staining the rock just under the soil of the top.
        if (band > 0.55 || (y > p.top - 7 && fine > 0.1)) return fine > 0.35 ? Blocks.TERRACOTTA.defaultBlockState()
                : fine > -0.2 ? Blocks.GRANITE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
        if (streak > 0.5) return Blocks.CALCITE.defaultBlockState();
        if (streak < -0.55) return fine > 0.3 ? Blocks.TUFF.defaultBlockState() : Blocks.ANDESITE.defaultBlockState();
        if (fine > 0.62) return Blocks.MOSS_BLOCK.defaultBlockState();
        return fine > 0 ? Blocks.STONE.defaultBlockState() : Blocks.DIORITE.defaultBlockState();
    }

    /** Vines down the walls, hanging from the ledges and the rim, inside this chunk only. */
    private static void vines(WorldGenLevel level, SimplexNoise noise, Pillar p, int x0, int z0, BlockPos.MutableBlockPos pos) {
        java.util.Random r = new java.util.Random(p.seed ^ 0x9E3779B9L);
        int count = 10 + (int) (p.radius * 4);
        for (int k = 0; k < count; k++) {
            double a = r.nextDouble() * Math.PI * 2;
            int startY = p.top - 2 - r.nextInt(Math.max(1, p.top - 90));
            int length = 4 + r.nextInt(14);
            // Out along the angle until the first open block beside the rock.
            for (double d = 1; d < p.radius * 1.6 + 3; d += 0.5) {
                int x = Mth.floor(p.x + Math.cos(a) * d), z = Mth.floor(p.z + Math.sin(a) * d);
                if (inside(noise, p, x, startY, z)) continue;
                if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) break;
                Direction toRock = null;
                for (Direction dir : Direction.Plane.HORIZONTAL) if (inside(noise, p, x + dir.getStepX(), startY, z + dir.getStepZ())) { toRock = dir; break; }
                if (toRock == null) break;
                BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(toRock), true);
                for (int y = startY; y > startY - length; y--) {
                    pos.set(x, y, z);
                    if (!level.isEmptyBlock(pos) || !inside(noise, p, x + toRock.getStepX(), y, z + toRock.getStepZ())) break;
                    level.setBlock(pos, vine, 2);
                }
                break;
            }
        }
    }

    private static long mix(long seed, int i, int j) {
        long h = seed ^ (i * 0x9E3779B97F4A7C15L) ^ (j * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L; h ^= h >>> 33;
        return h;
    }
}
