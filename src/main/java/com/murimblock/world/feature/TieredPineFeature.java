package com.murimblock.world.feature;

import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Huangshan pine as it is painted: a trunk leaning a little, a few branches held out level, each ending in a flat
 * pad of needles, and a wide flat parasol on top: layered clouds, not a cone. Grows on soil or bare rock with soil
 * (it roots in the cracks of the granite).
 */
public class TieredPineFeature extends Feature<NoneFeatureConfiguration> {
    public TieredPineFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos base = context.origin();
        BlockState ground = level.getBlockState(base.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(BlockTags.BASE_STONE_OVERWORLD) && !ground.is(MurimBlocks.PALE_GRANITE.get())) return false;
        if (!level.isEmptyBlock(base) && !level.getBlockState(base).is(BlockTags.REPLACEABLE_BY_TREES)) return false;
        return grow(level, random, base, 6 + random.nextInt(5));
    }

    /** Grows a tiered pine whose trunk starts at base and is about height tall. */
    public static boolean grow(WorldGenLevel level, RandomSource random, BlockPos base, int height) {
        // Room to grow: the trunk's column must be free.
        for (int y = 0; y < height; y++) {
            BlockState s = level.getBlockState(base.above(y));
            if (!s.isAir() && !s.is(BlockTags.REPLACEABLE_BY_TREES)) return false;
        }
        BlockState log = MurimBlocks.HUANGSHAN_PINE.log().get().defaultBlockState();
        BlockState needles = MurimBlocks.HUANGSHAN_PINE.leaves().get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos.MutableBlockPos pos = base.mutable();
        BlockPos[] trunk = new BlockPos[height];
        int shifts = 0;
        for (int y = 0; y < height; y++) {
            set(level, pos, log);
            trunk[y] = pos.immutable();
            // Leans after the first third, a block aside every few blocks.
            if (y > height / 3 && shifts < 2 && random.nextInt(3) == 0) {
                pos.move(lean);
                set(level, pos, log);
                shifts++;
            }
            pos.move(Direction.UP);
        }
        BlockPos top = pos.below().immutable();
        // Branches held level from the upper half, in different directions, each with its pad.
        int branches = 2 + random.nextInt(3);
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int start = random.nextInt(4);
        for (int b = 0; b < branches; b++) {
            Direction d = dirs[(start + b) % 4];
            BlockPos from = trunk[height - 2 - random.nextInt(Math.max(1, height / 2))];
            BlockPos.MutableBlockPos at = from.mutable();
            int len = 2 + random.nextInt(3);
            BlockState branch = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
            for (int k = 0; k < len; k++) {
                at.move(d);
                if (k == len - 1 && random.nextBoolean()) at.move(Direction.UP);
                set(level, at, branch);
            }
            pad(level, random, at.above(), 2 + random.nextInt(2), needles);
        }
        // The parasol on top, wider, with a smaller layer over its middle.
        pad(level, random, top.above(), 3, needles);
        pad(level, random, top.above(2), 1 + random.nextInt(2), needles);
        set(level, top.above(), log);
        return true;
    }

    /** A flat pad of needles: an irregular disc one block thick, its rim ragged. */
    public static void pad(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, BlockState needles) {
        double rx = radius + random.nextDouble() * 0.8, rz = radius + random.nextDouble() * 0.8;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz);
                if (d > 1.0 || (d > 0.65 && random.nextFloat() < 0.35f)) continue;
                p.set(centre.getX() + dx, centre.getY(), centre.getZ() + dz);
                BlockState s = level.getBlockState(p);
                if (s.isAir() || s.is(BlockTags.REPLACEABLE_BY_TREES)) level.setBlock(p, needles, 2);
            }
        }
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir() || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.LEAVES)) level.setBlock(pos, state, 2);
    }
}
