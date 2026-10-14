package com.murimblock.combat;

import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.cultivation.inner.HeartDemonEntity;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * No combo kills at once (docs/combat/technique-balance.md): in any {@link #WINDOW_TICKS} (3 s), one player cannot take
 * more than {@link #SHARE} (60 %) of a player's maximum health, whatever they strike with: techniques, strikes, Qi
 * blades, arrows. What goes past it does no damage (the blow still lands: stagger, push). A cultivator at full health
 * survives any combo; it takes a second one. The Heart Demon, a player's double, is held to the same. Creatures are not:
 * a technique may fell a zombie. A finish on a downed player is not a combo, and nothing stops the void or /kill.
 */
public final class BurstGuard {
    public static final double SHARE = 0.6;
    public static final int WINDOW_TICKS = 60;

    private record Hit(long at, float amount) { }

    /** Victim, then attacker: their recent blows. */
    private static final Map<UUID, Map<UUID, ArrayDeque<Hit>>> HITS = new HashMap<>();

    private BurstGuard() {
    }

    /** What may still be dealt of `amount`, after `dealt` within the window, to a body of `maxHealth`. */
    public static float allowed(double maxHealth, double dealt, float amount) {
        return (float) Math.max(0, Math.min(amount, SHARE * maxHealth - dealt));
    }

    /** Whether blows on this body are bounded so (a player, or a Heart Demon). */
    public static boolean guards(LivingEntity victim) {
        return victim instanceof Player || victim instanceof HeartDemonEntity;
    }

    /** The blow of `attacker` on `victim` now, bounded; it is counted. */
    public static float limit(LivingEntity victim, ServerPlayer attacker, float amount) {
        long now = victim.level().getGameTime();
        ArrayDeque<Hit> hits = HITS.computeIfAbsent(victim.getUUID(), v -> new HashMap<>()).computeIfAbsent(attacker.getUUID(), a -> new ArrayDeque<>());
        while (!hits.isEmpty() && now - hits.peekFirst().at >= WINDOW_TICKS) hits.pollFirst();
        double dealt = 0;
        for (Hit h : hits) dealt += h.amount;
        float allowed = allowed(victim.getMaxHealth(), dealt, amount);
        if (allowed > 0) hits.addLast(new Hit(now, allowed));
        return allowed;
    }

    /** Every blow on a guarded body from a player (or what they shot), last of all the changes to it. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !guards(victim) || !(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (victim instanceof Player downed && KnockdownService.isDowned(downed) && event.getSource().is(KnockdownService.EXECUTION)) return;
        event.setAmount(limit(victim, attacker, event.getAmount()));
    }

    /** Forgets a body (gone, or logged out). */
    public static void forget(UUID victim) {
        HITS.remove(victim);
    }

    public static void clearForTests() {
        HITS.clear();
    }
}
