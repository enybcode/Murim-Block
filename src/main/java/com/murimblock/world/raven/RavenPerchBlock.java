package com.murimblock.world.raven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A raven's perch (the Raven Art): a post of black stone, a raven of stone on top. Set down by a student of the art, it
 * becomes one of their perches (three at most, the oldest taken down), reached through the doors of their hall.
 */
public class RavenPerchBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public RavenPerchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server && placer instanceof ServerPlayer player) RavenHalls.perchSet(player, GlobalPos.of(server.dimension(), pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server) RavenHalls.perchGone(server, GlobalPos.of(server.dimension(), pos));
        super.onRemove(state, level, pos, next, moved);
    }
}
