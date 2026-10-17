package com.murimblock.world.structure;

import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The Star-Plucking Peak (摘星峰, Zhāixīng Fēng), the highest point of the world (Enzo, 17/10/2026): one immense mountain in
 * the north, raised from the ground to the very top of the world, its summit a little platform of white jade on the last
 * block a world can hold. Built column by column over the ground already there: a massif whose shoulders round off under the summit and whose slopes spread far at the foot,
 * five spurs running out from it, Kunlun rock and stone inside, snow and ice above the snow line, veins of white jade.
 * Where a player at Qi Liquidation who meditates three minutes is given the Seven Stars Sword (StarPeakPlacement).
 */
public final class StarPeakBuilder {
    /** How far its slopes run out from the summit at most, its spurs included (blocks). */
    public static final int RADIUS = 224;
    /** The summit's platform reaches this far from the centre (a 5 x 5 square). */
    public static final int PLATFORM = 2;
    /** Above this height its faces are snow and ice. */
    static final int SNOW_LINE = 205;

    private final ServerLevel level;
    private final int cx, cz, base, top;
    private final SimplexNoise noise;
    /** The spurs' bearings (radians). */
    private final double[] spurs = new double[5];
    /** The next column to build, counted along the square round the centre. */
    private int next;

    /**
     * A peak centred on (cx, cz), whose slopes meet the ground at about {@code base}, its summit platform at {@code top}
     * (the world's highest block).
     */
    public StarPeakBuilder(ServerLevel level, int cx, int cz, int base, int top, long seed) {
        this.level = level;
        this.cx = cx;
        this.cz = cz;
        this.base = base;
        this.top = top;
        this.noise = new SimplexNoise(new LegacyRandomSource(seed ^ 0x5741525045414BL));
        RandomSource random = RandomSource.create(seed);
        double turn = random.nextDouble() * Mth.TWO_PI;
        for (int i = 0; i < spurs.length; i++) spurs[i] = turn + i * Mth.TWO_PI / spurs.length + (random.nextDouble() - 0.5) * 0.6;
    }

    public int top() { return top; }
    public BlockPos summit() { return new BlockPos(cx, top, cz); }

    /**
     * The mountain's height over a column (dx, dz) from the centre, or {@link Integer#MIN_VALUE} past its foot: the
     * summit at {@code top} over the platform; then a profile steep near the top and broad at the foot, pushed out along
     * the spurs, roughened by noise.
     */
    public int height(int dx, int dz) {
        if (Math.max(Math.abs(dx), Math.abs(dz)) <= PLATFORM) return top;
        double r = Math.sqrt(dx * dx + dz * dz), a = Math.atan2(dz, dx);
        double spur = 0;
        for (double s : spurs) spur = Math.max(spur, Math.pow(Math.max(0, Math.cos(a - s)), 10));
        double reach = RADIUS * (0.72 + 0.28 * spur) * (1 + 0.08 * noise.getValue(Math.cos(a) * 2.3, Math.sin(a) * 2.3));
        double t = r / reach;
        if (t >= 1) return Integer.MIN_VALUE;
        // A rounded summit, shoulders, then long slopes spreading to the foot: a massif, not a needle.
        double shape = Math.pow(1 - Math.pow(t, 1.15), 2.2);
        double rough = noise.getValue((cx + dx) / 23.0, (cz + dz) / 23.0) * 7 + noise.getValue((cx + dx) / 7.0, (cz + dz) / 7.0) * 2.5;
        double h = base + (top - 1 - base) * shape + rough * Math.min(1, t * 4) * (1 - t);
        return (int) Math.min(top - 1, Math.round(h));
    }

    /** The summit platform's block at (x, top, z) (tests). */
    public BlockState blockAt(int x, int y, int z) {
        return block(x, y, z, y);
    }

    /** What lies at height y of a column whose top is {@code columnTop}. */
    BlockState block(int x, int y, int z, int columnTop) {
        int depth = columnTop - y;
        double n = noise.getValue(x / 11.0, y / 11.0 + z / 13.0);
        if (y == top && Math.max(Math.abs(x - cx), Math.abs(z - cz)) <= PLATFORM)
            return Math.max(Math.abs(x - cx), Math.abs(z - cz)) == PLATFORM ? Blocks.POLISHED_DIORITE.defaultBlockState() : MurimBlocks.RAW_WHITE_JADE.get().defaultBlockState();
        if (depth == 0) {
            if (y >= SNOW_LINE + 6 * n) return n > 0.55 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState();
            return n > 0.35 ? Blocks.GRAVEL.defaultBlockState() : MurimBlocks.KUNLUN_ROCK.get().defaultBlockState();
        }
        if (depth <= 3 && y >= SNOW_LINE + 10) return n > 0.2 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState();
        if (Math.abs(n) < 0.012) return MurimBlocks.RAW_WHITE_JADE.get().defaultBlockState();
        return n > 0 ? MurimBlocks.KUNLUN_ROCK.get().defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    /** Columns in the square the mountain fits in. */
    private static final int SIDE = 2 * RADIUS + 1;

    /** Builds it all at once (tests). In a live world, {@link #buildSome} spreads the work over several ticks. */
    public void build() {
        while (buildSome(Long.MAX_VALUE)) { }
    }

    /** Builds the next columns for about {@code budgetNanos}; true while there are columns left. */
    public boolean buildSome(long budgetNanos) {
        long end = budgetNanos == Long.MAX_VALUE ? Long.MAX_VALUE : System.nanoTime() + budgetNanos;
        while (next < SIDE * SIDE) {
            int dx = next % SIDE - RADIUS, dz = next / SIDE - RADIUS;
            next++;
            column(dx, dz);
            if (System.nanoTime() >= end) break;
        }
        return next < SIDE * SIDE;
    }

    private void column(int dx, int dz) {
        int h = height(dx, dz);
        if (h == Integer.MIN_VALUE) return;
        int x = cx + dx, z = cz + dz;
        // The ground: its last solid block (water, plants and snow layers over it are buried).
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
        if (ground >= h) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = ground + 1; y <= h; y++) level.setBlock(pos.set(x, y, z), block(x, y, z, h), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        // Whatever stood on the ground here (a plant, a snow layer, a tree's trunk) is cleared above the new top.
        for (int y = h + 1; y <= h + 12 && y < level.getMaxBuildHeight(); y++) {
            if (level.getBlockState(pos.set(x, y, z)).isAir()) break;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
