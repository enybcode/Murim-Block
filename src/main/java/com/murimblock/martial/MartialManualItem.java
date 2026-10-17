package com.murimblock.martial;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * The manual of a martial art. Read (right-click), it teaches the art and its first technique, which then appear in the
 * Techniques tab and on the combat bar, and it is used up: one manual, one reading (not in creative mode). A player who
 * already knows the art keeps it, to give it to someone else.
 */
public class MartialManualItem extends Item {
    private final ResourceLocation art;

    public MartialManualItem(Properties properties, ResourceLocation art) {
        super(properties);
        this.art = art;
    }

    public ResourceLocation art() {
        return art;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        MartialArts.Art definition = MartialArts.art(art);
        if (definition == null) return InteractionResultHolder.fail(stack);
        // A high art's manual is a closed book below its realm.
        var realm = MartialArts.requiredRealm(art);
        var current = com.murimblock.cultivation.CultivationService.getRealm(player);
        if (realm != null && current.ordinal() < realm.ordinal()) {
            // Too weak: told plainly in the chat (what it needs, where they stand), a word over the hotbar, a dull sound.
            player.sendSystemMessage(tooWeak(definition, realm, current));
            player.displayClientMessage(Component.translatable("art.murimblock.too_weak.short").withStyle(net.minecraft.ChatFormatting.RED), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.0F, 0.6F);
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer && MartialService.learn(serverPlayer, definition)) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.1F);
            stack.consume(1, player);
        } else {
            player.displayClientMessage(Component.translatable("art.murimblock.already_known", Component.translatable(definition.translationKey())), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** What a player too weak for a high art is told: the art, the realm it needs, the realm they are at. */
    public static Component tooWeak(MartialArts.Art definition, com.murimblock.cultivation.CultivationRealm required, com.murimblock.cultivation.CultivationRealm current) {
        return Component.translatable("art.murimblock.too_weak", Component.translatable(definition.translationKey()),
                Component.literal(required.displayName()), Component.literal(current.displayName())).withStyle(net.minecraft.ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MartialArts.Art definition = MartialArts.art(art);
        if (definition != null) tooltip.add(Component.translatable("item.murimblock.martial_manual.tooltip", Component.translatable(definition.translationKey())));
        var realm = MartialArts.requiredRealm(art);
        if (realm != null) tooltip.add(Component.translatable("item.murimblock.martial_manual.realm", Component.literal(realm.displayName()))
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }
}
