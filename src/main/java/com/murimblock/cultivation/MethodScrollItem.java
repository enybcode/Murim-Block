package com.murimblock.cultivation;

import java.util.List;
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
 * The scroll of a cultivation method. Read (right-click), it teaches the method, which can then be practised from the
 * Cultivation tab; it is used up (not in creative mode). One who already knows the method keeps it.
 */
public class MethodScrollItem extends Item {
    private final CultivationMethod method;

    public MethodScrollItem(Properties properties, CultivationMethod method) {
        super(properties);
        this.method = method;
    }

    public CultivationMethod method() {
        return method;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer serverPlayer && MethodService.learn(serverPlayer, method)) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 0.9F);
            player.displayClientMessage(Component.translatable("cultivation.murimblock.method.learned", Component.translatable(method.key())), true);
            stack.consume(1, player);
        } else {
            player.displayClientMessage(Component.translatable("cultivation.murimblock.method.already_known", Component.translatable(method.key())), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.murimblock.method_scroll.tooltip", Component.translatable(method.key())));
        tooltip.add(Component.translatable(method.key() + ".place").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
