package com.murimblock.cultivation;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Swallowed while meditating, opens the next meridian in the Qi-flow order if the realm allows it. */
public final class MeridianPillItem extends Item {
    public MeridianPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        Cultivation c = Meditation.of(server);
        String refusal = !c.meditating() ? "message.murimblock.pill.meditate"
                : c.meridians() >= CultivationRules.MERIDIANS ? "message.murimblock.pill.all_open"
                : !c.openMeridian() ? "message.murimblock.pill.realm" : null;
        if (refusal != null) {
            Meditation.system(server, refusal, ChatFormatting.GOLD);
            return InteractionResultHolder.fail(stack);
        }
        stack.consume(1, player);
        server.displayClientMessage(Component.translatable("message.murimblock.pill.opened",
                Component.translatable("murimblock.meridian." + MeridianOrder.ID.get(c.meridians() - 1)))
                .withStyle(ChatFormatting.AQUA), false);
        Meditation.sync(server);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.murimblock.meridian_opening_pill.desc").withStyle(ChatFormatting.GRAY));
    }
}
