package com.murimblock.alchemy;

import com.murimblock.cultivation.CultivationJournal;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.qi.QiService;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * The furnace's pills and decoctions beyond the two first ones (docs/cultivation/alchemy.md). Each adds its toxicity
 * (less, the purer it is) and does what toxicity and purity leave of its effect:
 * <ul>
 *   <li>Healing Decoction: regeneration for a while;</li>
 *   <li>Qi Decoction: a third of the Qi reserve back;</li>
 *   <li>Purifying Brew: toxicity drawn off (no toxicity of its own; once a minute);</li>
 *   <li>Martial Elixir: strength for a minute;</li>
 *   <li>Soul-Soothing Pill: the Heart Demon weaker for longer than a Realm-Breaking Pill, and a little toxicity drawn off.</li>
 * </ul>
 * Purity also rides on the two first pills (PillItem): AlchemyEvents makes a pure one less toxic.
 */
public class AlchemyPillItem extends Item {
    public enum Kind { HEALING_DECOCTION, QI_DECOCTION, PURIFYING_BREW, MARTIAL_ELIXIR, SOUL_SOOTHING_PILL }

    private final Kind kind;

    public AlchemyPillItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    /** A stack with its purity (0 to 100) on it. */
    public static ItemStack withPurity(ItemStack stack, int purity) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("purity", purity);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static int purity(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || !data.contains("purity") ? 60 : data.copyTag().getInt("purity");
    }

    /** How much of its toxicity a pill of this purity adds: 1.0 at 50, 0.7 at 100, 1.18 at 20. */
    public static double toxicityShare(int purity) {
        return 1.3 - 0.6 * purity / 100.0;
    }

    /** How much of its effect a pill of this purity gives (before toxicity): 0.85 at 50, 1.0 at 100. */
    public static double puritySpread(int purity) {
        return 0.7 + 0.3 * purity / 100.0;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.MARTIAL_ELIXIR || kind == Kind.SOUL_SOOTHING_PILL ? UseAnim.EAT : UseAnim.DRINK;
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 28; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            take(player, purity(stack));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (kind == Kind.PURIFYING_BREW) player.getCooldowns().addCooldown(this, 1200);
        }
        return stack;
    }

    /** What it does to this player now (also for tests). */
    public void take(ServerPlayer player, int purity) {
        long now = player.level().getGameTime();
        CultivationJournal journal = CultivationService.journal(player);
        double toxicity = journal.toxicityAt(now);
        if (toxicity >= CultivationRules.TOXICITY_POISON && kind != Kind.PURIFYING_BREW) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            player.displayClientMessage(Component.translatable("cultivation.murimblock.pill.poisoned"), true);
            return;
        }
        double effect = CultivationRules.pillEfficacy(toxicity) * puritySpread(purity), share = toxicityShare(purity);
        double added = switch (kind) {
            case HEALING_DECOCTION -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) Math.round(200 * effect), 1));
                yield 8;
            }
            case QI_DECOCTION -> {
                QiService.addQi(player, QiService.getQiMax(player) * 0.33 * effect);
                yield 12;
            }
            case PURIFYING_BREW -> -25 * puritySpread(purity);
            case MARTIAL_ELIXIR -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) Math.round(1200 * effect), 0));
                yield 20;
            }
            case SOUL_SOOTHING_PILL -> {
                CultivationService.setJournal(player, CultivationService.journal(player).withBreakingPill(now + Math.round(CultivationRules.BREAKING_PILL_TICKS * 1.5 * effect)));
                yield -10;
            }
        };
        double next = Math.max(0, toxicity + (added > 0 ? added * share : added));
        CultivationService.setJournal(player, CultivationService.journal(player).withToxicity(next, now));
        player.level().playSound(null, player.blockPosition(), kind == Kind.MARTIAL_ELIXIR || kind == Kind.SOUL_SOOTHING_PILL ? SoundEvents.GENERIC_EAT : SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(Component.translatable("alchemy.murimblock.taken." + kind.name().toLowerCase(java.util.Locale.ROOT), Math.round(next)), true);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("alchemy.murimblock.effect." + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.murimblock.herb.purity", purity(stack)).withStyle(ChatFormatting.GOLD));
    }
}
