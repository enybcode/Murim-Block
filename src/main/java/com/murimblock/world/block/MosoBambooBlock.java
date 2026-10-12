package com.murimblock.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Culm of the giant moso bamboo (Phyllostachys edulis): much thicker than vanilla bamboo, stacked into stems 12 to 22
 * blocks tall by the bamboo sea feature. A plain building material once cut.
 */
public class MosoBambooBlock extends Block {
    public static final MapCodec<MosoBambooBlock> CODEC = simpleCodec(MosoBambooBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public MosoBambooBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
