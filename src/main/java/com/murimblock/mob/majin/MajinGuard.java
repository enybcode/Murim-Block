package com.murimblock.mob.majin;

import com.murimblock.Murimblock;
import com.murimblock.mob.SonOfTheDemonEntity;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The Son of the Demon never kills a player in one blow (Enzo, 17/10/2026: "balance him for the middle of the game, no
 * one-shot"): a blow of his on a player takes at most {@link #HIT_SHARE} of their whole health, and all his blows within
 * {@link #WINDOW_TICKS} at most {@link #WINDOW_SHARE} (before armour, which takes its own share after). His heat (fire,
 * with no one behind it) is not counted: it is slow by itself.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class MajinGuard {
    public static final double HIT_SHARE = 0.35, WINDOW_SHARE = 0.6;
    public static final int WINDOW_TICKS = 60;

    private record Hit(long at, float amount) { }

    private static final Map<UUID, ArrayDeque<Hit>> HITS = new HashMap<>();

    private MajinGuard() { }

    /** What may still be dealt of {@code amount} to a body of {@code maxHealth}, after {@code dealt} within the window. */
    public static float allowed(double maxHealth, double dealt, float amount) {
        return (float) Math.max(0, Math.min(Math.min(amount, HIT_SHARE * maxHealth), WINDOW_SHARE * maxHealth - dealt));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getSource().getEntity() instanceof SonOfTheDemonEntity)) return;
        long now = player.level().getGameTime();
        ArrayDeque<Hit> hits = HITS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        while (!hits.isEmpty() && now - hits.peekFirst().at() >= WINDOW_TICKS) hits.pollFirst();
        double dealt = 0;
        for (Hit h : hits) dealt += h.amount();
        float allowed = allowed(player.getMaxHealth(), dealt, event.getAmount());
        if (allowed <= 0) { event.setCanceled(true); return; }
        event.setAmount(allowed);
        hits.addLast(new Hit(now, allowed));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HITS.clear();
    }
}
