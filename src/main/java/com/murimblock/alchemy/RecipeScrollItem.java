package com.murimblock.alchemy;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * A rare recipe scroll (docs/cultivation/alchemy.md): the recipe it holds on it ("recipe" in its custom data, set by the
 * chests' loot), learned and the scroll used up when read.
 */
public class RecipeScrollItem extends Item {
    public RecipeScrollItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(AlchemyRecipes.Recipe recipe) {
        ItemStack stack = new ItemStack(AlchemyItems.RECIPE_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("recipe", recipe.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    static AlchemyRecipes.Recipe recipe(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : AlchemyRecipes.byId(data.copyTag().getString("recipe"));
    }

    @Override public Component getName(ItemStack stack) {
        AlchemyRecipes.Recipe r = recipe(stack);
        return r == null ? super.getName(stack) : Component.translatable("item.murimblock.recipe_scroll.of", Component.translatable(r.nameKey()));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        AlchemyRecipes.Recipe r = recipe(stack);
        if (r == null || !AlchemyKnowledge.learn(player, r)) {
            player.displayClientMessage(Component.translatable(r == null ? "alchemy.murimblock.blank" : "alchemy.murimblock.known"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.sendSystemMessage(Component.translatable("alchemy.murimblock.learned", Component.translatable(r.nameKey())).withStyle(ChatFormatting.GOLD));
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("alchemy.murimblock.scroll_hint").withStyle(ChatFormatting.GRAY));
    }
}
