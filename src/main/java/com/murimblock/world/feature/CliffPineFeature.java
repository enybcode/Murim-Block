package com.murimblock.world.feature;

import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Huangshan cliff pine: grows sideways out of a crack in a sheer rock face, its trunk curving up, crowned with flat
 * layered needle clouds, like the Welcoming-Guest Pine. Looks for a cliff face around the placement position.
 */
public class CliffPineFeature extends Feature<NoneFeatureConfiguration> {
    public CliffPineFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos start = origin.offset(random.nextInt(9) - 4, random.nextInt(28) - 4, random.nextInt(9) - 4);
            Direction out = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            // A crack: rock behind, open air in front and below (a real face, not a ledge on flat ground).
            if (!level.getBlockState(start).isSolid() || !level.isEmptyBlock(start.relative(out))
                    || !level.isEmptyBlock(start.relative(out).below(2)) || !level.isEmptyBlock(start.relative(out, 2))) continue;
            grow(level, random, start.relative(out), out);
            return true;
        }
        return false;
    }

    private static void grow(WorldGenLevel level, RandomSource random, BlockPos from, Direction out) {
        BlockState sideways = MurimBlocks.HUANGSHAN_PINE.log().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, out.getAxis());
        BlockState upright = MurimBlocks.HUANGSHAN_PINE.log().get().defaultBlockState();
        BlockState needles = MurimBlocks.HUANGSHAN_PINE.leaves().get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockPos.MutableBlockPos pos = from.mutable();
        int reach = 2 + random.nextInt(3);
        for (int i = 0; i < reach; i++) {
            if (!level.isEmptyBlock(pos)) return;
            level.setBlock(pos, sideways, 2);
            pos.move(out);
            if (random.nextBoolean()) pos.move(Direction.UP);
        }
        int rise = 1 + random.nextInt(3);
        for (int i = 0; i < rise && level.isEmptyBlock(pos); i++) {
            level.setBlock(pos, upright, 2);
            pos.move(Direction.UP);
        }
        // Two flat needle clouds, the lower one wider and pushed further out.
        BlockPos crown = pos.immutable();
        cloud(level, random, crown.below().relative(out), 3, needles);
        cloud(level, random, crown, 2, needles);
    }

    private static void cloud(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, BlockState needles) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1 || random.nextFloat() < 0.15f) continue;
                BlockPos p = centre.offset(dx, 0, dz);
                if (level.isEmptyBlock(p)) level.setBlock(p, needles, 2);
            }
        }
    }
}
