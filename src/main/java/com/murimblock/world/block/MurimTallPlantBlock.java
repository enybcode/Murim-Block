package com.murimblock.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A two-block plant (rhododendron, tree fern, wild rice, tamarisk) rooted in its own kind of soil. */
public class MurimTallPlantBlock extends DoublePlantBlock {
    public static final MapCodec<MurimTallPlantBlock> CODEC = simpleCodec(p -> new MurimTallPlantBlock(MurimPlantBlock.Soil.GRASS, p));
    private final MurimPlantBlock.Soil soil;

    public MurimTallPlantBlock(MurimPlantBlock.Soil soil, Properties properties) {
        super(properties);
        this.soil = soil;
    }

    @Override public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    }

    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return soil.accepts(state);
    }
}
