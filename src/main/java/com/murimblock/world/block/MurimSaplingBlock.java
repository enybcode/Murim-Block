package com.murimblock.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** Sapling of a Murim tree; the desert poplar's also takes root in sand and gravel, like the real one along oases. */
public class MurimSaplingBlock extends SaplingBlock {
    private final MurimPlantBlock.Soil soil;

    public MurimSaplingBlock(TreeGrower grower, MurimPlantBlock.Soil soil, Properties properties) {
        super(grower, properties);
        this.soil = soil;
    }

    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return soil.accepts(state);
    }
}
