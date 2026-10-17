package com.murimblock.alchemy;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Alchemy Manual (the recipes it holds: the common ones) or a recipe scroll (one rare recipe), read with a use:
 * the player learns what they did not know (docs/cultivation/alchemy.md). A scroll is used up; the manual is kept.
 */
public class AlchemyBookItem extends Item {
    private final List<String> recipes;
    private final boolean consumed;

    public AlchemyBookItem(List<String> recipes, boolean consumed, Properties properties) {
        super(properties);
        this.recipes = recipes;
        this.consumed = consumed;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        int learned = 0;
        for (String id : recipes) {
            AlchemyRecipes.Recipe r = AlchemyRecipes.byId(id);
            if (r != null && AlchemyKnowledge.learn(player, r)) {
                learned++;
                player.sendSystemMessage(Component.translatable("alchemy.murimblock.learned", Component.translatable(r.nameKey())).withStyle(ChatFormatting.GOLD));
            }
        }
        if (learned == 0) {
            player.displayClientMessage(Component.translatable("alchemy.murimblock.known"), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (consumed && !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String id : recipes) tooltip.add(Component.translatable("alchemy.murimblock.recipe." + id).withStyle(ChatFormatting.GRAY));
    }
}
