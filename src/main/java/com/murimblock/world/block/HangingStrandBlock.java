package com.murimblock.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Something that hangs down in strands: the weeping willow's fronds, the banyan's aerial roots. Holds on under
 * leaves, logs, a solid block or another strand of itself; falls apart when what holds it goes.
 */
public class HangingStrandBlock extends Block {
    public static final MapCodec<HangingStrandBlock> CODEC = simpleCodec(HangingStrandBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public HangingStrandBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) || above.is(BlockTags.LEAVES) || above.is(BlockTags.LOGS)
                || (!above.is(Blocks.AIR) && above.isFaceSturdy(level, pos.above(), Direction.DOWN));
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                               BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
