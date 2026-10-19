package com.murimblock.world.plant;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * What a plant grows from (seeds, a bulb, a cutting, a rhizome, spores): used on its soil (the top of a block), or on
 * still water for the floating ones, it plants a seedling (PlantCropBlock).
 */
public class PlantSeedItem extends Item {
    private final PlantCatalog.Species species;

    public PlantSeedItem(PlantCatalog.Species species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public PlantCatalog.Species species() { return species; }

    /** Plants a seedling at `pos` if it may root there; returns whether it did. */
    public static boolean plant(PlantCatalog.Species species, Level level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty() || !PlantCropBlock.mayRoot(species, level, pos)) return false;
        BlockState state = MurimFlora.CROP.get().planted(species);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (species.soil() == PlantCatalog.Soil.WATER || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos().above();
        if (!plant(species, context.getLevel(), pos)) return InteractionResult.FAIL;
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /** The floating ones: aimed at still water, like a lily pad. */
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (species.soil() != PlantCatalog.Soil.WATER) return InteractionResultHolder.pass(stack);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos pos = hit.getBlockPos().above();
        if (!plant(species, level, pos)) return InteractionResultHolder.fail(stack);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.murimblock.seed.plant",
                Component.translatable("tooltip.murimblock.seed.soil." + species.soil().name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.murimblock.herb.rarity." + species.rarity().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
