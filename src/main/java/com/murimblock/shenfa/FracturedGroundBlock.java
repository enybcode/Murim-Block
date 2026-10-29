package com.murimblock.shenfa;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of fractured ground (registry.MurimBlocks): solid to walk on, drawn by nobody, so that Shenfa can draw the
 * real block it stands in for tilted and lifted. Light and sky pass through it.
 */
public final class FracturedGroundBlock extends Block {
    public FracturedGroundBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1;
    }
}
