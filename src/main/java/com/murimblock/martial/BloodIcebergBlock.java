package com.murimblock.martial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blood crystal of the Blood Iceberg: it drops nothing and, when its scheduled tick comes (set as the mountain rises,
 * saved with the chunk), it crumbles away.
 */
public class BloodIcebergBlock extends Block {
    public BloodIcebergBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (random.nextInt(10) == 0)
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, .7F, .6F + random.nextFloat() * .4F);
    }
}
