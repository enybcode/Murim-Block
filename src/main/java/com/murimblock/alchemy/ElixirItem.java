package com.murimblock.alchemy;

import com.murimblock.cultivation.CultivationJournal;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * An elixir of the furnace (AlchemyElixirs): its passives for a while, the longer the purer it is and the less toxicity
 * already weighs on its taker; it adds its own toxicity (less, the purer it is). Past the poison threshold it only poisons.
 */
public class ElixirItem extends Item {
    private final AlchemyElixirs.Elixir elixir;

    public ElixirItem(AlchemyElixirs.Elixir elixir, Properties properties) {
        super(properties);
        this.elixir = elixir;
    }

    public AlchemyElixirs.Elixir elixir() { return elixir; }

    static Holder<MobEffect> effect(String id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElseThrow();
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return elixir.flask() ? UseAnim.DRINK : UseAnim.EAT; }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 24; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            take(player, AlchemyPillItem.purity(stack));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    /** What it does to this player now (also for tests). */
    public void take(ServerPlayer player, int purity) {
        long now = player.level().getGameTime();
        CultivationJournal journal = CultivationService.journal(player);
        double toxicity = journal.toxicityAt(now);
        if (toxicity >= CultivationRules.TOXICITY_POISON && elixir.toxicity() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            player.displayClientMessage(Component.translatable("cultivation.murimblock.pill.poisoned"), true);
            return;
        }
        double strength = CultivationRules.pillEfficacy(toxicity) * AlchemyPillItem.puritySpread(purity);
        for (AlchemyElixirs.Effect e : elixir.effects())
            player.addEffect(new MobEffectInstance(effect(e.id()), (int) Math.round(e.seconds() * 20 * strength), e.amplifier()));
        double next = toxicity + elixir.toxicity() * AlchemyPillItem.toxicityShare(purity);
        CultivationService.setJournal(player, CultivationService.journal(player).withToxicity(next, now));
        player.level().playSound(null, player.blockPosition(), elixir.flask() ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(Component.translatable("alchemy.murimblock.elixir_taken", getDescription(), Math.round(next)), true);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int purity = AlchemyPillItem.purity(stack);
        double spread = AlchemyPillItem.puritySpread(purity);
        for (AlchemyElixirs.Effect e : elixir.effects()) {
            int seconds = (int) Math.round(e.seconds() * spread);
            Component name = Component.translatable(effect(e.id()).value().getDescriptionId());
            Component level = e.amplifier() > 0 ? Component.literal(" ").append(Component.translatable("enchantment.level." + (e.amplifier() + 1))) : Component.empty();
            tooltip.add(Component.empty().append(name).append(level).append(String.format(Locale.ROOT, " (%d:%02d)", seconds / 60, seconds % 60)).withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("alchemy.murimblock.passive." + elixir.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.murimblock.herb.purity", purity).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("alchemy.murimblock.toxicity", Math.round(elixir.toxicity() * AlchemyPillItem.toxicityShare(purity))).withStyle(ChatFormatting.DARK_GREEN));
    }
}
