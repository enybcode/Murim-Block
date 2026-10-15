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
        if (realm != null && com.murimblock.cultivation.CultivationService.getRealm(player).ordinal() < realm.ordinal()) {
            player.displayClientMessage(Component.translatable("art.murimblock.realm_required", Component.translatable(definition.translationKey()),
                    Component.literal(realm.displayName())), true);
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MartialArts.Art definition = MartialArts.art(art);
        if (definition != null) tooltip.add(Component.translatable("item.murimblock.martial_manual.tooltip", Component.translatable(definition.translationKey())));
        var realm = MartialArts.requiredRealm(art);
        if (realm != null) tooltip.add(Component.translatable("item.murimblock.martial_manual.realm", Component.literal(realm.displayName()))
                .withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }
}
