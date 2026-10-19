package com.murimblock.cultivation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The aura's pressure, from Foundation Establishment (docs/cultivation/refonte-cultivation.md, 18/10/2026), after Bleach's
 * spiritual pressure (Enzo): the weaker mobs near (of a lower realm, the ordinary ones realm 0) falter, slowed and stopping
 * short now and then; the far weaker (three realms below or more) are crushed to the ground: barely moving, pressed down
 * if they leave it, the dust lifting at their feet. Not players, not one's own animals.
 * <p>Released or held in at will (Enzo, 19/10/2026: its key, H; held in at first; the choice kept); sneaking holds it in
 * too.
 * <p>Overflowing (Enzo: like the Overlord Spear's blows, the ground giving under the power): with
 * {@link #OVERWHELMING_QI} Qi held or more, the ground cracks at every step (Epic Fight's own fracture, put back by
 * itself), and where one stands still it stays broken (broken again, quietly, before it mends); space ripples on the
 * screens near every two seconds (client.cultivation.PressureWaveClient).
 */
public final class AuraPressure {
    /** The realm it comes with. */
    public static final CultivationRealm REALM = CultivationRealm.FOUNDATION_ESTABLISHMENT;
    /** Far weaker: this many realms below. */
    public static final int FAR_WEAKER = 3;
    /** How often it weighs (ticks), and how long each weighing slows (a little longer, so it holds). */
    static final int EVERY = 10, SLOW_TICKS = 15;
    /** The Qi held from which the aura overflows (Core Formation's threshold); and how often it pulses (ticks). */
    public static final double OVERWHELMING_QI = 10_000;
    public static final int PULSE = 40;
    /** A step: the ground cracks each time one has walked this far (blocks). */
    static final double STEP = 1.2;

    /** Where each one's ground last cracked, when, and whether they were standing there. */
    private record Crack(net.minecraft.world.phys.Vec3 at, long tick, boolean standing) {
    }

    private static final java.util.Map<java.util.UUID, Crack> CRACKS = new java.util.concurrent.ConcurrentHashMap<>();

    private AuraPressure() {
    }

    /** How far it reaches (blocks): 8 at Foundation Establishment, 10 at Core Formation, 12 at Nascent Soul, 16 beyond. */
    public static double reach(CultivationRealm realm) {
        if (realm.ordinal() < REALM.ordinal()) return 0;
        return switch (realm) {
            case FOUNDATION_ESTABLISHMENT -> 8;
            case CORE_FORMATION -> 10;
            case NASCENT_SOUL -> 12;
            default -> 16;
        };
    }

    /** Whether this player weighs on those around now (realm reached, the aura released, not sneaking). */
    public static boolean active(Player player) {
        return reach(CultivationService.getRealm(player)) > 0 && released(player) && !player.isShiftKeyDown() && !player.isSpectator();
    }

    /** Whether this player has released their aura (its key); on a client, only one's own is known (the aura tells the rest). */
    public static boolean released(Player player) {
        return player.getData(CultivationAttachments.PLAYER_PRESSURE_RELEASED);
    }

    public static void release(Player player, boolean on) {
        player.setData(CultivationAttachments.PLAYER_PRESSURE_RELEASED, on);
    }

    /** Its key pressed: released, or held in (below Foundation, too weak to weigh on anyone). */
    public static void toggle(ServerPlayer player) {
        if (reach(CultivationService.getRealm(player)) <= 0) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("cultivation.murimblock.pressure.locked",
                    net.minecraft.network.chat.Component.translatable("gui.murimblock.realm." + REALM.serializedName())), true);
            return;
        }
        boolean on = !released(player);
        release(player, on);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(on ? "cultivation.murimblock.pressure.released" : "cultivation.murimblock.pressure.held"), true);
        player.level().playSound(null, player.blockPosition(), on ? net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE : net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS, on ? 0.5F : 0.6F, on ? 1.4F : 1.2F);
        MeditationService.refreshAura(player);
    }

    /** A mob's realm (an ordinary one: 0, the mortals'). */
    static int realmOf(Mob mob) {
        return QiSense.cultivationOf(mob).map(d -> d.realm().ordinal()).orElse(0);
    }

    /**
     * How far the ground breaks round the feet standing, for this much Qi held (0 below the overflow; Enzo: the greater
     * the power, the greater the crack): 3.3 blocks at 10 000, 4.7 at 20 000, 8 at 45 000, 9 at most.
     */
    public static double fracture(double qi) {
        return qi < OVERWHELMING_QI ? 0 : Math.min(9, 2 + qi / 7_500);
    }

    /** How far each step cracks it: 2 blocks at 10 000, 3 at 20 000, 5 at most. */
    public static double stepFracture(double qi) {
        return qi < OVERWHELMING_QI ? 0 : Math.min(5, 1 + qi / OVERWHELMING_QI);
    }

    /**
     * Overflowing: the ground cracks under each step (smaller, quiet); standing still, it breaks round the feet (loud the
     * first time) and is broken again quietly every pulse, so that it stays broken while one stands there.
     */
    static void crack(ServerPlayer player) {
        double qi = com.murimblock.qi.QiService.getQi(player), broken = fracture(qi);
        if (broken <= 0 || !player.onGround()) {
            if (broken <= 0) CRACKS.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        var here = player.position();
        Crack last = CRACKS.get(player.getUUID());
        double moved = last == null ? Double.MAX_VALUE : Math.hypot(here.x - last.at.x, here.z - last.at.z);
        if (moved >= STEP) {
            // A step: a small crack.
            fractureAt(player, here, stepFracture(qi), false);
            CRACKS.put(player.getUUID(), new Crack(here, now, false));
        } else if (moved < 0.3 && now - last.tick >= PULSE / 2) {
            // Standing: the ground broken wide, loud once, then kept broken.
            fractureAt(player, last.at, broken, last.standing);
            CRACKS.put(player.getUUID(), new Crack(last.at, now, true));
        }
    }

    static void fractureAt(ServerPlayer player, net.minecraft.world.phys.Vec3 at, double radius, boolean quiet) {
        yesman.epicfight.api.utils.LevelUtil.circleSlamFracture(player, player.level(), at.subtract(0, 1, 0), radius, quiet, quiet, false);
    }

    public static void forget(java.util.UUID player) {
        CRACKS.remove(player);
    }

    public static void tick(ServerPlayer player) {
        if (!active(player)) return;
        crack(player);
        if (player.tickCount % EVERY != 0) return;
        CultivationRealm realm = CultivationService.getRealm(player);
        double reach = reach(realm);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(reach),
                m -> m.isAlive() && m.distanceToSqr(player) <= reach * reach)) {
            if (mob instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())) continue;
            int gap = realm.ordinal() - realmOf(mob);
            if (gap <= 0) continue;
            if (gap < FAR_WEAKER) {
                // Weaker: slowed, faltering.
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1, true, false), player);
                if (mob.getRandom().nextInt(4) == 0) mob.getNavigation().stop();
                continue;
            }
            // Far weaker: crushed to the ground, the dust lifting at its feet.
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 3, true, false), player);
            if (!mob.onGround()) mob.setDeltaMovement(mob.getDeltaMovement().add(0, -0.25, 0));
            if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                var ground = level.getBlockState(mob.blockPosition().below());
                if (!ground.isAir()) level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, ground),
                        mob.getX(), mob.getY() + 0.05, mob.getZ(), 3, mob.getBbWidth() * 0.4, 0, mob.getBbWidth() * 0.4, 0.02);
            }
        }
    }
}
