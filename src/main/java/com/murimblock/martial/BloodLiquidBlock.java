package com.murimblock.martial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Blood as a block. A spring of the Blood Iceberg dries up when its scheduled block tick comes (set when it is placed,
 * saved with the chunk); the blood flowing from it then drains away on its own.
 */
public class BloodLiquidBlock extends LiquidBlock {
    public BloodLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getFluidState().isSource()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
