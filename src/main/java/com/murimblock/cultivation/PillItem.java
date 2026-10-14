package com.murimblock.cultivation;

import com.murimblock.qi.QiFormat;
import com.murimblock.qi.QiService;
import java.util.List;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.Level;

/**
 * An elixir of cultivation (docs/cultivation/qi-cultivation.md), swallowed like food. Each adds toxicity, which fades
 * a point a minute; a pill gives only what toxicity leaves of its effect, and past 100 it poisons instead.
 * <ul>
 *   <li>Qi Gathering Pill: Qi Max, and the reserve filled;</li>
 *   <li>Realm-Breaking Pill: for the next ten minutes, the Heart Demon of a great breakthrough is weaker.</li>
 * </ul>
 */
public class PillItem extends Item {
    public enum Kind { QI_GATHERING, REALM_BREAKING }

    private final Kind kind;

    public PillItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 24; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            swallow(player);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    /** What the pill does to this player now (also for tests and commands). */
    public void swallow(ServerPlayer player) {
        long now = player.level().getGameTime();
        CultivationJournal journal = CultivationService.journal(player);
        double toxicity = journal.toxicityAt(now);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.3F);
        if (toxicity >= CultivationRules.TOXICITY_POISON) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200));
            player.displayClientMessage(Component.translatable("cultivation.murimblock.pill.poisoned"), true);
            return;
        }
        double efficacy = CultivationRules.pillEfficacy(toxicity);
        CultivationService.setJournal(player, journal.withToxicity(toxicity + CultivationRules.PILL_TOXICITY, now));
        if (kind == Kind.QI_GATHERING) {
            double gained = CultivationService.gainQiMax(player, CultivationRules.PILL_QI_MAX * efficacy, CultivationService.Source.PILL);
            QiService.refillQi(player);
            player.displayClientMessage(Component.translatable("cultivation.murimblock.pill.gathering", QiFormat.format(gained),
                    Math.round(toxicity + CultivationRules.PILL_TOXICITY)), true);
        } else {
            CultivationService.setJournal(player, CultivationService.journal(player).withBreakingPill(now + Math.round(CultivationRules.BREAKING_PILL_TICKS * efficacy)));
            player.displayClientMessage(Component.translatable("cultivation.murimblock.pill.breaking", Math.round(toxicity + CultivationRules.PILL_TOXICITY)), true);
        }
        CultivationEffects.show(player, com.murimblock.network.CultivationPayloads.Breakthrough.MINOR);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.murimblock." + (kind == Kind.QI_GATHERING ? "qi_gathering_pill" : "realm_breaking_pill") + ".tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.murimblock.pill.toxicity").withStyle(ChatFormatting.DARK_RED));
    }
}
