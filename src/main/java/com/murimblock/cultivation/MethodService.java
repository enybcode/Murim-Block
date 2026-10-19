package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The cultivation methods in play (CultivationMethod): which one a player practises, learning one (a method's scroll),
 * changing (its Qi out of tune for a day: meditation condenses half as much), what meditation gains from it, and the
 * effects of the methods that have one in combat.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class MethodService {
    /** A change of method: the Qi out of tune for one day (24 000 ticks). */
    public static final int DISCORD_TICKS = 24_000;
    /** Out of tune: meditation condenses this much. */
    public static final double DISCORD_FACTOR = 0.5;
    /** Azure Sky Breathing: the Qi back out of combat, more by level. */
    public static final double[] AZURE_REGEN = {0, 0.15, 0.25, 0.35};
    /** Frost Lotus Heart: how long the attacker is slowed (ticks) by level, and how often (ticks). */
    public static final int[] FROST_SLOW = {0, 20, 30, 40};
    public static final int FROST_EVERY = 160;
    /** Blazing Sun Scripture: the chance a blow sets alight, and for how long (seconds), by level (none before Foundation). */
    public static final float[] SUN_CHANCE = {0, 0, 0.10F, 0.15F};
    public static final int[] SUN_FIRE = {0, 0, 3, 4};

    private static final Map<UUID, Long> FROST_READY = new ConcurrentHashMap<>();

    private MethodService() {
    }

    public static MethodData data(Player player) {
        return player.getData(CultivationAttachments.PLAYER_METHOD);
    }

    static void set(Player player, MethodData data) {
        player.setData(CultivationAttachments.PLAYER_METHOD, data);
    }

    public static CultivationMethod method(Player player) {
        return data(player).method();
    }

    /** Its level of the method practised (by realm). */
    public static int level(Player player) {
        return CultivationMethod.level(CultivationService.getRealm(player));
    }

    /** Learns a method (its scroll read): false if already known. */
    public static boolean learn(ServerPlayer player, CultivationMethod method) {
        MethodData data = data(player);
        if (data.knows(method)) return false;
        set(player, data.learn(method));
        return true;
    }

    public enum Change { UNKNOWN, ALREADY, CHANGED }

    /** Practises another known method: the Qi out of tune for a day. */
    public static Change practise(ServerPlayer player, CultivationMethod method) {
        MethodData data = data(player);
        if (!data.knows(method)) return Change.UNKNOWN;
        if (data.method() == method) return Change.ALREADY;
        set(player, data.practise(method, player.level().getGameTime() + DISCORD_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.7F);
        player.displayClientMessage(Component.translatable("cultivation.murimblock.method.changed", Component.translatable(method.key())), true);
        return Change.CHANGED;
    }

    /** What meditation condenses here under the method practised: its place's factor, halved while out of tune. */
    public static double factor(Player player) {
        MethodData data = data(player);
        double f = data.method().factor(player);
        return data.discordAt(player.level().getGameTime()) ? f * DISCORD_FACTOR : f;
    }

    /** Azure Sky Breathing: how much more Qi comes back out of combat (0 under another method). */
    public static double regenBonus(Player player) {
        return method(player) == CultivationMethod.AZURE_SKY_BREATHING ? AZURE_REGEN[level(player)] : 0;
    }

    /** Frost Lotus Heart (struck in melee: the attacker slowed) and Blazing Sun Scripture (a blow sets alight). */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        var source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker || attacker == victim) return;
        if (victim instanceof ServerPlayer player && method(player) == CultivationMethod.FROST_LOTUS_HEART) {
            int level = level(player);
            long now = player.level().getGameTime();
            if (level > 0 && now >= FROST_READY.getOrDefault(player.getUUID(), 0L)) {
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FROST_SLOW[level], 1), player);
                FROST_READY.put(player.getUUID(), now + FROST_EVERY);
                player.level().playSound(null, attacker.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.4F, 1.6F);
            }
        }
        if (attacker instanceof ServerPlayer player && method(player) == CultivationMethod.BLAZING_SUN_SCRIPTURE) {
            int level = level(player);
            if (SUN_CHANCE[level] > 0 && player.getRandom().nextFloat() < SUN_CHANCE[level]) victim.igniteForSeconds(SUN_FIRE[level]);
        }
    }

    public static void clearForTests() {
        FROST_READY.clear();
    }
}
