package com.murimblock.alchemy;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.qi.QiService;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The elixirs' own passives (docs/cultivation/alchemy.md), beside Minecraft's effects, each stronger by its level:
 * <ul>
 *   <li>Qi Flow: half a percent of the Qi reserve back each second per level;</li>
 *   <li>Iron Skin: 15 % less damage taken per level (at most 60 %);</li>
 *   <li>Tiger Might: 15 % more damage dealt with one's own blows per level;</li>
 *   <li>Thorn Body: a fifth of a blow taken sent back to its striker per level;</li>
 *   <li>Blood Vigor: a tenth of the damage one's blows deal healed per level;</li>
 *   <li>Keen Senses: the enemies round about seen glowing through walls (16 blocks, 8 more per level);</li>
 *   <li>Clear Mind: a point of toxicity drawn off every ten seconds per level.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class AlchemyEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Murimblock.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> QI_FLOW = EFFECTS.register("qi_flow", () -> new Passive(0x4FA8E8));
    public static final DeferredHolder<MobEffect, MobEffect> IRON_SKIN = EFFECTS.register("iron_skin", () -> new Passive(0x9A9A9A));
    public static final DeferredHolder<MobEffect, MobEffect> TIGER_MIGHT = EFFECTS.register("tiger_might", () -> new Passive(0xE08A28));
    public static final DeferredHolder<MobEffect, MobEffect> THORN_BODY = EFFECTS.register("thorn_body", () -> new Passive(0x4E8A3A));
    public static final DeferredHolder<MobEffect, MobEffect> BLOOD_VIGOR = EFFECTS.register("blood_vigor", () -> new Passive(0xB0202A));
    public static final DeferredHolder<MobEffect, MobEffect> KEEN_SENSES = EFFECTS.register("keen_senses", () -> new Passive(0xE8D060));
    public static final DeferredHolder<MobEffect, MobEffect> CLEAR_MIND = EFFECTS.register("clear_mind", () -> new Passive(0xB8F0E0));

    private AlchemyEffects() {
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static final class Passive extends MobEffect {
        Passive(int colour) {
            super(MobEffectCategory.BENEFICIAL, colour);
        }
    }

    /** The effect's level (1 for level I), 0 if it has none. */
    static int level(LivingEntity entity, Holder<MobEffect> effect) {
        MobEffectInstance i = entity.getEffect(effect);
        return i == null ? 0 : i.getAmplifier() + 1;
    }

    /** A blow struck by the attacker's own hand or weapon (not an arrow, a spell, thorns). */
    private static boolean ownBlow(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        int qi = level(player, QI_FLOW);
        if (qi > 0) QiService.addQi(player, QiService.getQiMax(player) * 0.005 * qi);
        int keen = level(player, KEEN_SENSES);
        if (keen > 0) {
            double reach = 16 + 8 * (keen - 1);
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach), e -> e instanceof Enemy && e.isAlive()))
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
        }
        int clear = level(player, CLEAR_MIND);
        if (clear > 0 && player.tickCount % 200 == 0) {
            long now = player.level().getGameTime();
            var journal = CultivationService.journal(player);
            CultivationService.setJournal(player, journal.withToxicity(Math.max(0, journal.toxicityAt(now) - clear), now));
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        if (ownBlow(source) && source.getEntity() instanceof LivingEntity attacker) {
            int might = level(attacker, TIGER_MIGHT);
            if (might > 0) amount *= 1 + 0.15F * might;
        }
        int skin = level(target, IRON_SKIN);
        if (skin > 0) amount *= 1 - Math.min(0.6F, 0.15F * skin);
        event.setAmount(amount);
        int thorns = level(target, THORN_BODY);
        if (thorns > 0 && ownBlow(source) && source.getEntity() instanceof LivingEntity attacker && attacker != target)
            attacker.hurt(target.damageSources().thorns(target), amount * 0.2F * thorns);
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!ownBlow(source) || !(source.getEntity() instanceof LivingEntity attacker)) return;
        int vigor = level(attacker, BLOOD_VIGOR);
        if (vigor > 0 && event.getNewDamage() > 0) attacker.heal(event.getNewDamage() * 0.1F * vigor);
    }
}
