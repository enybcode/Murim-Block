package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.network.CultivationPayloads;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Qi vision's vital points (点穴), from Foundation Establishment (docs/cultivation/refonte-cultivation.md, 18/10/2026):
 * a perfect parry made with Qi Sense open lights up {@link #COUNT} of the attacker's vital points for a moment
 * ({@link #WINDOW_TICKS}). A blow of one's own aimed at one (the look passing within {@link #AIM} of it) strikes it:
 * {@link #DAMAGE}× the damage, and the point goes out. Decided by the server; the points are shown to that player only.
 * <p>Kept aside (18/10/2026, Enzo): nothing calls {@link #onPerfectParry} in play; it waits for a technique to use it.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class VitalPoints {
    /** The realm the vital points come with. */
    public static final CultivationRealm REALM = CultivationRealm.FOUNDATION_ESTABLISHMENT;
    /** How long they stay lit after the parry (2.5 s). */
    public static final int WINDOW_TICKS = 50;
    /** How many light up. */
    public static final int COUNT = 3;
    /** A vital point struck: the blow's damage times this. */
    public static final float DAMAGE = 1.5F;
    /** How near the look must pass to the point (blocks). */
    static final double AIM = 0.5;
    /** How far a blow reaches its point (blocks, from the eyes). */
    static final double REACH = 6;

    /**
     * The points, in the body's frame: sideways (share of the width, + to its left), up (share of the height). Temple,
     * throat, heart, solar plexus, dantian, both shoulders, both knees.
     */
    public static final float[][] POINTS = {
            {0.12F, 0.93F}, {0.0F, 0.8F}, {0.18F, 0.68F}, {0.0F, 0.6F}, {0.0F, 0.45F},
            {0.38F, 0.76F}, {-0.38F, 0.76F}, {0.2F, 0.22F}, {-0.2F, 0.22F}};

    /** Open marks, by player: whose points, which ones (a bit each), till when. */
    record Mark(UUID target, int mask, long until) {
    }

    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();

    private VitalPoints() {
    }

    /** Where a vital point is now: on the front of the body, turned with it. */
    public static Vec3 position(LivingEntity e, int index) {
        float[] p = POINTS[index];
        float yaw = e.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), left = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        double w = e.getBbWidth();
        return e.position().add(forward.scale(w * 0.5)).add(left.scale(p[0] * w)).add(0, p[1] * e.getBbHeight(), 0);
    }

    /** A perfect parry: from Foundation Establishment, Qi Sense open, the attacker's vital points light up. */
    public static void onPerfectParry(ServerPlayer player, Entity attacker) {
        if (!(attacker instanceof LivingEntity target) || !target.isAlive()) return;
        if (CultivationService.getRealm(player).ordinal() < REALM.ordinal() || !QiSense.isActive(player)) return;
        int mask = 0;
        while (Integer.bitCount(mask) < COUNT) mask |= 1 << player.getRandom().nextInt(POINTS.length);
        open(player, target, mask);
    }

    static void open(ServerPlayer player, LivingEntity target, int mask) {
        MARKS.put(player.getUUID(), new Mark(target.getUUID(), mask, player.level().getGameTime() + WINDOW_TICKS));
        send(player, target, mask, -1, WINDOW_TICKS);
    }

    /** The lit points this player has on this body (a bit each), 0 if none. */
    public static int open(ServerPlayer player, LivingEntity target) {
        Mark m = MARKS.get(player.getUUID());
        if (m == null || !m.target.equals(target.getUUID())) return 0;
        if (player.level().getGameTime() > m.until) {
            MARKS.remove(player.getUUID());
            return 0;
        }
        return m.mask;
    }

    /** The lit point this player's look is on (within {@link #AIM} and reach), or -1. */
    static int aimed(ServerPlayer player, LivingEntity target, int mask) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        int best = -1;
        double bestDistance = AIM;
        for (int i = 0; i < POINTS.length; i++) {
            if ((mask & 1 << i) == 0) continue;
            Vec3 to = position(target, i).subtract(eye);
            double along = to.dot(look);
            if (along < 0 || along > REACH) continue;
            double off = to.subtract(look.scale(along)).length();
            if (off <= bestDistance) {
                bestDistance = off;
                best = i;
            }
        }
        return best;
    }

    /** A blow of the player's own on a body whose points are lit, aimed at one: it strikes it. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().getDirectEntity() != player) return;
        int mask = open(player, target);
        if (mask == 0) return;
        int point = aimed(player, target, mask);
        if (point < 0) return;
        event.setAmount(event.getAmount() * DAMAGE);
        int left = mask & ~(1 << point);
        Mark m = MARKS.get(player.getUUID());
        if (left == 0) MARKS.remove(player.getUUID());
        else MARKS.put(player.getUUID(), new Mark(m.target, left, m.until));
        send(player, target, left, point, (int) Math.max(0, m.until - player.level().getGameTime()));
    }

    static void send(ServerPlayer player, LivingEntity target, int mask, int struck, int ticks) {
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new CultivationPayloads.Vital(target.getId(), mask, struck, ticks));
    }

    public static void forget(UUID player) {
        MARKS.remove(player);
    }

    public static void clearForTests() {
        MARKS.clear();
    }
}
