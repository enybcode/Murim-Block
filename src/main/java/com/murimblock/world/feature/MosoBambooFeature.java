package com.murimblock.world.feature;

import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A grove of giant moso bamboo: 6 to 14 culms 12 to 22 blocks tall, spaced like a real bamboo sea, feathery leaf
 * tufts on the upper third that touch to make a closed canopy; fresh shoots at the foot.
 */
public class MosoBambooFeature extends Feature<NoneFeatureConfiguration> {
    public MosoBambooFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int culms = 8 + random.nextInt(9);
        int placed = 0;
        BlockState culm = MurimBlocks.MOSO_BAMBOO.get().defaultBlockState();
        BlockState leaves = MurimBlocks.MOSO_BAMBOO_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int i = 0; i < culms; i++) {
            int x = origin.getX() + random.nextInt(9) - 4, z = origin.getZ() + random.nextInt(9) - 4;
            int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos base = new BlockPos(x, ground, z);
            if (!level.getBlockState(base.below()).is(BlockTags.DIRT) || !level.isEmptyBlock(base)) continue;
            int height = 12 + random.nextInt(11);
            for (int y = 0; y < height; y++) {
                BlockPos p = base.above(y);
                if (!level.isEmptyBlock(p) && !level.getBlockState(p).is(BlockTags.LEAVES)) break;
                level.setBlock(p, culm, 2);
            }
            // Leaf tufts on short twigs in the upper third, and a crown.
            for (int y = height * 2 / 3; y <= height; y++) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    // Sparse feathery tufts: the green culms must stay visible between them.
                    if (y % 2 == 0 && random.nextFloat() < 0.32f) setLeaf(level, base.above(y).relative(d), leaves);
                }
            }
            setLeaf(level, base.above(height), leaves);
            if (random.nextFloat() < 0.3f && level.isEmptyBlock(base.east())
                    && level.getBlockState(base.east().below()).is(BlockTags.DIRT)) {
                level.setBlock(base.east(), MurimBlocks.BAMBOO_SHOOT.get().defaultBlockState(), 2);
            }
            placed++;
        }
        return placed > 0;
    }

    private static void setLeaf(WorldGenLevel level, BlockPos pos, BlockState leaves) {
        if (level.isEmptyBlock(pos)) level.setBlock(pos, leaves, 2);
    }
}
