package com.murimblock.martial;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A vial of blood (15/10/2026, Enzo's request), filled from the blood of the Blood Iceberg's temple with a glass bottle
 * (SanguineItems.fill). Drunk in the Crimson Stance, in combat, it gives 10 blood crystals: in Blood Crystal Form, 10
 * dorsal crystals (up to the 10 a form holds); outside it, 10 points of the blood gauge. The empty bottle goes to the bag;
 * the gauge holds while a vial is in hand (SanguineArt.tick).
 */
public class BloodVialItem extends Item {
    /** What a vial gives: crystals in the form, points of the gauge outside it. */
    public static final int CRYSTALS = 10;

    public BloodVialItem(Properties properties) {
        super(properties);
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Only the Sanguine Art, in combat, can take the blood in (the gauge is a fight's).
        if (!SanguineArt.stanceActive(player) || !com.murimblock.combat.CombatService.isInCombatMode(player)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("item.murimblock.blood_vial.refused"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        drink(player);
        if (player.getAbilities().instabuild) return stack;
        // The empty bottle goes to the bag, not the hand: the art fights bare-handed.
        stack.shrink(1);
        if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        return stack;
    }

    /** The blood taken in (also for tests). */
    public static void drink(ServerPlayer player) {
        SanguineArt.bloodCrystals(player, CRYSTALS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1F, .6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.murimblock.blood_vial.desc").withStyle(ChatFormatting.DARK_RED));
    }
}
