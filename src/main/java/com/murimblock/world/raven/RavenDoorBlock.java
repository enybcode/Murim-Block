package com.murimblock.world.raven;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A door of shadow in a hall of the Raven Halls (RavenHallBuilder): one walks through it. Door 0 leads back to where
 * the player came in from; doors 1 to 3 to their perches in the world (RavenHalls.throughDoor). It cannot be broken.
 */
public class RavenDoorBlock extends Block {
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, 3);

    public RavenDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INDEX, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INDEX);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) RavenHalls.throughDoor(player, state.getValue(INDEX));
    }
}
