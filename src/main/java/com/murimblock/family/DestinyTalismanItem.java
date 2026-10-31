package com.murimblock.family;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * The Destiny Talisman, a white jade disc (crafted from every ore but netherite): broken, it draws the player's
 * house again. That draw is final; the old house's bonuses are taken back and the new one's granted. Refused while
 * the first window's free reroll is still waiting.
 */
public final class DestinyTalismanItem extends Item {
    public DestinyTalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        FamilyState state = FamilyEvents.state(server);
        if (state.family() == null || state.freeReroll()) {
            server.displayClientMessage(Component.translatable("message.murimblock.talisman.window")
                    .withStyle(ChatFormatting.GOLD), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.consume(1, player);
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
        FamilyEvents.redraw(server);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.murimblock.destiny_talisman.desc").withStyle(ChatFormatting.GRAY));
    }
}
