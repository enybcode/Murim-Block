package com.murimblock.alchemy;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/**
 * The pill furnace (丹炉, docs/cultivation/alchemy.md): drawn by its block entity (GeckoLib). Used with spring water,
 * a herb, charcoal or coal, it takes them; with an empty hand it opens its fire screen; sneaking with an empty hand,
 * before the work starts, it gives the herbs back.
 */
public class PillFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<PillFurnaceBlock> CODEC = simpleCodec(PillFurnaceBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public PillFurnaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PillFurnaceBlockEntity(pos, state);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AlchemyItems.FURNACE_ENTITY.get(), PillFurnaceBlockEntity::serverTick);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PillFurnaceBlockEntity furnace)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean free = player.getAbilities().instabuild;
        if (stack.is(Items.WATER_BUCKET) || stack.is(Items.POTION) && stack.getComponents().has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)
                && stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).is(net.minecraft.world.item.alchemy.Potions.WATER)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!furnace.pourWater(player)) return ItemInteractionResult.FAIL;
            if (!free) player.setItemInHand(hand, new ItemStack(stack.is(Items.WATER_BUCKET) ? Items.BUCKET : Items.GLASS_BOTTLE));
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.2F);
            return ItemInteractionResult.CONSUME;
        }
        if (stack.is(Items.CHARCOAL) || stack.is(Items.COAL)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!furnace.addFuel(stack)) return ItemInteractionResult.FAIL;
            if (!free) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
            return ItemInteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof com.murimblock.world.plant.HerbItem) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            String refused = furnace.addHerb(player, stack);
            if (refused != null && !refused.equals("alchemy.murimblock.unknown")) {
                player.displayClientMessage(Component.translatable(refused), true);
                return ItemInteractionResult.FAIL;
            }
            if (!free) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.4F);
            if (refused != null) player.displayClientMessage(Component.translatable(refused), true);
            else if (furnace.recipe() != null) player.displayClientMessage(Component.translatable("alchemy.murimblock.ready",
                    Component.translatable(furnace.recipe().nameKey())), true);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PillFurnaceBlockEntity furnace)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            List<ItemStack> back = furnace.takeBack();
            for (ItemStack s : back) if (!player.getInventory().add(s)) player.drop(s, false);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer sp) sp.openMenu(furnace, pos);
        return InteractionResult.CONSUME;
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PillFurnaceBlockEntity furnace) {
            for (ItemStack s : furnace.herbs()) popResource(level, pos, s.copy());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.2, pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.02, 0);
        if (random.nextInt(8) == 0) level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F, false);
    }
}
