package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.integration.epicfight.DemonArtAnimations;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Devouring Demon Art in play (docs/combat/styles/devouring-demon-art.md, Enzo 18/10/2026), bare hands, its main
 * style. In combat mode the kagune comes out: the four tentacles (KaguneState.Form.HUNGER).
 * <ul>
 *   <li><b>The Fury</b> (0 to {@link KaguneState#MAX_FURY}, the ring of the combat bar's dantian): every blow he lands
 *   gives {@link #HIT_FURY}, every blow he takes {@link #HURT_FURY} (once a tick); after {@link #CALM_TICKS} without either
 *   it falls {@link #CALM_PER_SECOND} a second. No technique costs Fury.</li>
 *   <li><b>Full, the transformation</b>, by itself: the tentacles draw back, the centipedes burst out, the mask grows,
 *   and he goes on all fours ({@link KaguneState#TRANSFORM_SECONDS} s; nothing cast meanwhile). In the demon form he
 *   strikes {@link #DEMON_DAMAGE} as hard and takes {@link #DEMON_GUARD} of every blow; the Fury drains
 *   {@link #DEMON_DRAIN_PER_SECOND} a second, every blow he lands gives back {@link #DEMON_HIT_FURY}. At 0 he is himself
 *   again, spent for {@link #SPENT_TICKS} ticks. Every technique changes in it, on the same key
 *   (MartialArts.DEMON_VERSIONS).</li>
 *   <li><b>Lash</b>: the four tentacles whip the arc in front of him ({@link #LASH_REACH} blocks, {@link #LASH_ARC}
 *   degrees), every foe there struck and pushed back. In the demon form, <b>Centipede Bite</b>: a centipede shoots at the
 *   foe he looks at, up to {@link #BITE_REACH} blocks, bites (slowed, drawn in) and he devours a share of it as health.</li>
 *   <li><b>Impaling Thorns</b>: the tentacles plunge into the ground; {@link #THORNS_HIT_TICK} ticks later they burst out
 *   where the foe he looked at (up to {@link #THORNS_REACH} blocks) stood: whoever is still there is impaled, thrown up and
 *   held. In the demon form, <b>Burrow</b>: the centipedes dive into the ground and burst out under the
 *   {@link #BURROW_TARGETS} nearest foes ahead (up to {@link #BURROW_REACH} blocks).</li>
 *   <li><b>Grapple</b>: a tentacle shoots at what he looks at, up to {@link #GRAPPLE_REACH} blocks: a foe is hooked and
 *   pulled in to just in front of him; else it hooks into the block and pulls him to it (no fall damage). In the demon
 *   form, <b>Constrict</b>: a centipede shoots at the foe he looks at (up to {@link #CONSTRICT_REACH} blocks), coils round
 *   it, holds it {@link #CONSTRICT_HOLD_TICKS} ticks, crushing it every {@link #CONSTRICT_CRUSH_EVERY}, then throws it off.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class DevouringDemon {
    public static final int HIT_FURY = 4, HURT_FURY = 3, DEMON_HIT_FURY = 2;
    public static final int CALM_TICKS = 120, CALM_PER_SECOND = 2, DEMON_DRAIN_PER_SECOND = 3, SPENT_TICKS = 80;
    public static final float DEMON_DAMAGE = 1.25F, DEMON_GUARD = 0.75F;
    public static final double LASH_REACH = 4.5, LASH_ARC = 150, LASH_SHARE = 1.3, LASH_PUSH = 0.7;
    /** Into the body's animation (ticks): the tentacles sweep through the arc. */
    public static final int LASH_HIT_TICK = 6;
    public static final double BITE_REACH = 7, BITE_CONE = 22, BITE_SHARE = 2.2, BITE_PULL = 0.7;
    public static final float BITE_DEVOUR = 0.2F;
    public static final int BITE_HIT_TICK = 8, BITE_SLOW_TICKS = 50;
    public static final double THORNS_REACH = 12, THORNS_CONE = 12, THORNS_SHARE = 1.6, THORNS_RADIUS = 1.3, THORNS_LIFT = 0.65;
    /** Into the body's animation (ticks): the thorns burst out; how long the impaled stay held. */
    public static final int THORNS_HIT_TICK = 10, THORNS_HOLD_TICKS = 25;
    public static final double BURROW_REACH = 9, BURROW_ARC = 150, BURROW_SHARE = 2.0, BURROW_RADIUS = 1.5, BURROW_LIFT = 0.95;
    public static final int BURROW_HIT_TICK = 11, BURROW_HOLD_TICKS = 20, BURROW_TARGETS = 2;
    /** Grapple: how far, how wide, when the tentacle gets there (ticks), how long it pulls, how hard, to where, how fast he goes. */
    public static final double GRAPPLE_REACH = 14, GRAPPLE_CONE = 8, GRAPPLE_SHARE = 0.8, GRAPPLE_PULL_TO = 2.2, GRAPPLE_PULL_RATE = 0.4, GRAPPLE_SPEED = 1.6;
    public static final int GRAPPLE_HIT_TICK = 5, GRAPPLE_PULL_TICKS = 14;
    /** Constrict: how far, how wide, when the coils close (ticks), how long they hold, a crush every so often and its share, the throw. */
    public static final double CONSTRICT_REACH = 8, CONSTRICT_CONE = 15, CONSTRICT_SHARE = 0.6, CONSTRICT_TOSS = 0.9;
    public static final int CONSTRICT_HIT_TICK = 6, CONSTRICT_HOLD_TICKS = 40, CONSTRICT_CRUSH_EVERY = 10;

    private record Pending(MartialArts.Technique technique, long at, List<Vec3> points) {
        Pending(MartialArts.Technique technique, long at) { this(technique, at, List.of()); }
    }
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** When each player last gained Fury (game time), and the tick he last did (once a tick). */
    private static final Map<UUID, Long> LAST_GAIN = new HashMap<>();
    /** Players whose kagune this art took out (it puts it away for them). */
    private static final Map<UUID, ServerPlayer> USERS = new HashMap<>();

    private DevouringDemon() { }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.DEVOURING_DEMON);
    }

    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return MeditationService.aura(player).mainArt().filter(MartialArts.DEVOURING_DEMON::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.DEVOURING_DEMON::equals).isPresent();
    }

    public static boolean bareHanded(Player player) {
        return player.getMainHandItem().isEmpty();
    }

    /** The kagune is out: the art is his main style, his hands empty, in combat mode, free to fight. */
    private static boolean out(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && stanceActive(player) && bareHanded(player) && CombatService.isInCombatMode(player)
                && !KnockdownService.isHindered(player) && !KnockdownService.isCarried(player) && !MeditationService.isMeditating(player);
    }

    /** The Fury now (0 when the kagune is away). */
    public static int fury(Player player) {
        return KaguneState.of(player).fury();
    }

    /** Sets the Fury (the operator command, the tests); full, the transformation follows on the next tick. */
    public static void setFury(ServerPlayer player, int value) {
        KaguneState.put(player, KaguneState.of(player).withFury(value));
        LAST_GAIN.put(player.getUUID(), player.level().getGameTime());
    }

    /** Fury gained, once a tick, while the kagune is out. */
    static void gain(ServerPlayer player, int amount) {
        var state = KaguneState.of(player);
        if (state.form() == KaguneState.Form.NONE || !USERS.containsKey(player.getUUID())) return;
        long now = player.level().getGameTime();
        Long last = LAST_GAIN.get(player.getUUID());
        if (last != null && last == now) return;
        LAST_GAIN.put(player.getUUID(), now);
        KaguneState.put(player, state.withFury(state.fury() + amount));
    }

    // ---------------------------------------------------------------- the forms

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (!stanceActive(player)) {
            if (USERS.containsKey(id)) forget(player);
            return;
        }
        var state = KaguneState.of(player);
        long now = player.level().getGameTime();
        boolean out = out(player);
        if (state.form() == KaguneState.Form.NONE) {
            if (!out) return;
            USERS.put(id, player);
            KaguneState.set(player, KaguneState.Form.HUNGER);
            LAST_GAIN.put(id, now);
            return;
        }
        USERS.put(id, player);
        switch (state.form()) {
            case HUNGER -> {
                if (!out) { forget(player); return; }
                if (state.fury() >= KaguneState.MAX_FURY) { transform(player); return; }
                long calm = now - LAST_GAIN.getOrDefault(id, now);
                if (calm >= CALM_TICKS && calm % 20 == 0 && state.fury() > 0) KaguneState.put(player, state.withFury(state.fury() - CALM_PER_SECOND));
            }
            case TRANSFORM -> {
                if (KaguneState.beast(player)) {
                    var demon = new KaguneState(KaguneState.Form.DEMON, now, state.fury(), state.strike(), state.strikeSince());
                    KaguneState.put(player, demon);
                }
            }
            case DEMON -> {
                // The demon form holds out of combat too, until its Fury is spent; a knock-down or a weapon in hand ends it.
                if (!player.isAlive() || KnockdownService.isHindered(player) || !bareHanded(player)) { spent(player, false); return; }
                if ((now - state.since()) % 20 == 19) {
                    int fury = state.fury() - DEMON_DRAIN_PER_SECOND;
                    if (fury <= 0) { spent(player, true); return; }
                    KaguneState.put(player, state.withFury(fury));
                }
            }
            default -> { }
        }
        holdTick(player, now);
        Pending p = PENDING.get(id);
        if (p != null && now >= p.at()) {
            PENDING.remove(id);
            if (p.technique() == MartialArts.LASH) lashHits(player);
            else if (p.technique() == MartialArts.CENTIPEDE_BITE) biteHits(player);
            else eruptionHits(player, p.technique(), p.points());
        }
    }

    /** Full Fury: the transformation (the clients play it from the state; the hitbox and the gait follow at its end). */
    static void transform(ServerPlayer player) {
        var state = KaguneState.of(player);
        long now = player.level().getGameTime();
        KaguneState.put(player, new KaguneState(KaguneState.Form.TRANSFORM, now, KaguneState.MAX_FURY, KaguneState.STRIKE_NONE, now));
        PENDING.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.6F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.3F, 0.55F);
    }

    /** The Fury spent: himself again, the tentacles back (or put away), and if it ran out, spent for a moment. */
    static void spent(ServerPlayer player, boolean tired) {
        long now = player.level().getGameTime();
        var back = out(player) ? KaguneState.Form.HUNGER : KaguneState.Form.NONE;
        KaguneState.put(player, new KaguneState(back, now, 0, KaguneState.STRIKE_NONE, now));
        LAST_GAIN.put(player.getUUID(), now);
        PENDING.remove(player.getUUID());
        if (back == KaguneState.Form.NONE) USERS.remove(player.getUUID());
        if (tired && player.isAlive()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SPENT_TICKS, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SPENT_TICKS, 0, false, false, true));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 0.7F);
        }
    }

    // ---------------------------------------------------------------- techniques

    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        var state = KaguneState.of(player);
        if (state.form() == KaguneState.Form.NONE || !out(player) && state.form() != KaguneState.Form.DEMON) return false;
        // Nothing while the centipedes burst out.
        if (state.form() == KaguneState.Form.TRANSFORM) return false;
        MartialArts.Technique cast = state.form() == KaguneState.Form.DEMON ? MartialArts.DEMON_VERSIONS.getOrDefault(technique, technique) : technique;
        long now = player.level().getGameTime();
        ServerLevel level = player.serverLevel();
        if (cast == MartialArts.LASH) {
            patch.playAnimationSynchronized(DemonArtAnimations.LASH, 0.0F);
            KaguneState.put(player, state.withStrike(KaguneState.STRIKE_LASH, now));
            PENDING.put(player.getUUID(), new Pending(cast, now + LASH_HIT_TICK));
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.55F);
            return true;
        }
        if (cast == MartialArts.CENTIPEDE_BITE) {
            patch.playAnimationSynchronized(DemonArtAnimations.BITE, 0.0F);
            KaguneState.put(player, state.withStrike(KaguneState.STRIKE_BITE, now));
            PENDING.put(player.getUUID(), new Pending(cast, now + BITE_HIT_TICK));
            level.playSound(null, player.blockPosition(), SoundEvents.SILVERFISH_AMBIENT, SoundSource.PLAYERS, 1.4F, 0.5F);
            return true;
        }
        if (cast == MartialArts.GRAPPLE) {
            // A foe looked at, or else the block looked at: the tentacle goes for it.
            LivingEntity target = lookedAt(player, GRAPPLE_REACH, GRAPPLE_CONE);
            Vec3 anchor = null;
            if (target == null) {
                var hit = player.pick(GRAPPLE_REACH, 1, false);
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return false;
                anchor = hit.getLocation();
            }
            patch.playAnimationSynchronized(DemonArtAnimations.GRAPPLE, 0.0F);
            KaguneState.put(player, state.withStrike(KaguneState.STRIKE_GRAPPLE, now));
            HOLDS.put(player.getUUID(), new Hold(cast, target, anchor, now + GRAPPLE_HIT_TICK, now + GRAPPLE_HIT_TICK + GRAPPLE_PULL_TICKS));
            grab(player, KaguneState.STRIKE_GRAPPLE, target, anchor == null ? target.position() : anchor);
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.4F, 0.5F);
            return true;
        }
        if (cast == MartialArts.CONSTRICT) {
            LivingEntity target = lookedAt(player, CONSTRICT_REACH, CONSTRICT_CONE);
            if (target == null) return false;
            patch.playAnimationSynchronized(DemonArtAnimations.CONSTRICT, 0.0F);
            KaguneState.put(player, state.withStrike(KaguneState.STRIKE_CONSTRICT, now));
            HOLDS.put(player.getUUID(), new Hold(cast, target, null, now + CONSTRICT_HIT_TICK, now + CONSTRICT_HIT_TICK + CONSTRICT_HOLD_TICKS));
            grab(player, KaguneState.STRIKE_CONSTRICT, target, target.position());
            level.playSound(null, player.blockPosition(), SoundEvents.SILVERFISH_AMBIENT, SoundSource.PLAYERS, 1.4F, 0.4F);
            return true;
        }
        if (cast == MartialArts.IMPALING_THORNS) {
            LivingEntity target = thornsTarget(player);
            if (target == null) return false;
            List<Vec3> at = List.of(target.position());
            patch.playAnimationSynchronized(DemonArtAnimations.THORNS, 0.0F);
            KaguneState.put(player, state.withStrike(KaguneState.STRIKE_THORNS, now));
            PENDING.put(player.getUUID(), new Pending(cast, now + THORNS_HIT_TICK, at));
            erupt(player, KaguneState.STRIKE_THORNS, at);
            level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.4F, 0.6F);
            return true;
        }
        if (cast == MartialArts.BURROW) {
            List<Vec3> at = burrowTargets(player).stream().map(LivingEntity::position).toList();
            if (at.isEmpty()) return false;
            patch.playAnimationSynchronized(DemonArtAnimations.BURROW, 0.0F);
            KaguneState.put(player, state.withStrike(KaguneState.STRIKE_BURROW, now));
            PENDING.put(player.getUUID(), new Pending(cast, now + BURROW_HIT_TICK, at));
            erupt(player, KaguneState.STRIKE_BURROW, at);
            level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.4F, 0.6F);
            return true;
        }
        return false;
    }

    /** Tells the clients round him where the kagune will burst out of the ground (client/KaguneEruptions). */
    private static void erupt(ServerPlayer player, int strike, List<Vec3> at) {
        if (player.connection == null) return;
        List<Double> xyz = new java.util.ArrayList<>();
        for (Vec3 v : at) { xyz.add(v.x); xyz.add(v.y); xyz.add(v.z); }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new com.murimblock.network.KaguneEruptPayload(player.getId(), strike, xyz));
    }

    /** Impaling Thorns' foe: the nearest he looks at, within reach, in sight. */
    @Nullable static LivingEntity thornsTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(THORNS_REACH),
                e -> MartialCombat.isEnemy(e, player))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > THORNS_REACH || d < 1e-3) continue;
            if (Math.toDegrees(Math.acos(Math.clamp(to.normalize().dot(look), -1, 1))) > THORNS_CONE + Math.toDegrees(Math.atan2(e.getBbWidth(), d))) continue;
            if (!player.hasLineOfSight(e)) continue;
            if (d < bestDistance) { bestDistance = d; best = e; }
        }
        return best;
    }

    /** Burrow's foes: the two nearest in front of him, within reach (under the ground, no need to see them). */
    static List<LivingEntity> burrowTargets(ServerPlayer player) {
        Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BURROW_REACH, 3, BURROW_REACH),
                        e -> MartialCombat.isEnemy(e, player)).stream()
                .filter(e -> {
                    Vec3 flat = e.position().subtract(player.position()).multiply(1, 0, 1);
                    return flat.length() <= BURROW_REACH && flat.length() > 0.5
                            && Math.toDegrees(Math.acos(Math.clamp(flat.normalize().dot(facing), -1, 1))) <= BURROW_ARC / 2;
                })
                .sorted(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .limit(BURROW_TARGETS).toList();
    }

    /** The kagune bursts out of the ground: every foe standing where it comes up is struck, thrown up, held. */
    static void eruptionHits(ServerPlayer player, MartialArts.Technique technique, List<Vec3> at) {
        boolean burrow = technique == MartialArts.BURROW;
        double radius = burrow ? BURROW_RADIUS : THORNS_RADIUS;
        boolean hit = false;
        for (Vec3 p : at) {
            for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(p, p).inflate(radius, 2.5, radius),
                    e -> MartialCombat.isEnemy(e, player))) {
                if (e.position().multiply(1, 0, 1).distanceTo(p.multiply(1, 0, 1)) > radius + e.getBbWidth() / 2 || e.getY() < p.y - 1) continue;
                e.invulnerableTime = 0;
                if (!e.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.DEVOURING_DEMON, burrow ? BURROW_SHARE : THORNS_SHARE))) continue;
                e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, burrow ? BURROW_LIFT : THORNS_LIFT, 0));
                e.hurtMarked = true;
                // Impaled: held where it is for a moment.
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, burrow ? BURROW_HOLD_TICKS : THORNS_HOLD_TICKS, 5, false, false));
                impact(player, e, burrow ? KaguneState.STRIKE_BURROW : KaguneState.STRIKE_THORNS);
                hit = true;
            }
            player.serverLevel().playSound(null, net.minecraft.core.BlockPos.containing(p), burrow ? SoundEvents.RAVAGER_ROAR : SoundEvents.ROOTED_DIRT_BREAK,
                    SoundSource.PLAYERS, burrow ? 0.9F : 1.3F, burrow ? 1.3F : 0.5F);
        }
        if (hit) MartialProgress.addMastery(player, art(), MartialArts.IMPALING_THORNS.id(), 1);
    }

    /** Lash's sweep: every foe in the arc ahead, within reach, struck and pushed back. */
    static void lashHits(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
        boolean hit = false;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(LASH_REACH, 2.5, LASH_REACH),
                e -> MartialCombat.isEnemy(e, player))) {
            Vec3 to = e.position().subtract(player.position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            if (flat.length() > LASH_REACH + e.getBbWidth() / 2 || Math.abs(to.y) > 2.5) continue;
            if (flat.lengthSqr() > 0.01 && Math.toDegrees(Math.acos(Math.clamp(flat.normalize().dot(facing), -1, 1))) > LASH_ARC / 2) continue;
            e.invulnerableTime = 0;
            if (!e.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.DEVOURING_DEMON, LASH_SHARE))) continue;
            Vec3 away = flat.lengthSqr() < 1e-4 ? facing : flat.normalize();
            e.push(away.x * LASH_PUSH, 0.25, away.z * LASH_PUSH);
            e.hurtMarked = true;
            hit = true;
            impact(player, e, KaguneState.STRIKE_LASH);
        }
        if (hit) {
            MartialProgress.addMastery(player, art(), MartialArts.LASH.id(), 1);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    /** The foe a bite goes for: the nearest he looks at, within the centipede's reach. */
    @Nullable static LivingEntity biteTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BITE_REACH),
                e -> MartialCombat.isEnemy(e, player))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > BITE_REACH + e.getBbWidth() / 2 || d < 1e-3) continue;
            if (Math.toDegrees(Math.acos(Math.clamp(to.normalize().dot(look), -1, 1))) > BITE_CONE + Math.toDegrees(Math.atan2(e.getBbWidth(), d))) continue;
            if (!player.hasLineOfSight(e)) continue;
            if (d < bestDistance) { bestDistance = d; best = e; }
        }
        return best;
    }

    /** The centipede's bite: struck, slowed, drawn in; he devours a share of it. */
    static void biteHits(ServerPlayer player) {
        LivingEntity e = biteTarget(player);
        if (e == null) return;
        float damage = TechniquePower.damage(player, MartialArts.DEVOURING_DEMON, BITE_SHARE);
        float before = e.getHealth();
        e.invulnerableTime = 0;
        if (!e.hurt(player.damageSources().playerAttack(player), damage)) return;
        player.heal(Math.max(0, before - e.getHealth()) * BITE_DEVOUR);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BITE_SLOW_TICKS, 1, false, false));
        Vec3 in = player.position().subtract(e.position()).multiply(1, 0, 1);
        if (in.lengthSqr() > 1e-4) {
            in = in.normalize();
            e.push(in.x * BITE_PULL, 0.15, in.z * BITE_PULL);
            e.hurtMarked = true;
        }
        impact(player, e, KaguneState.STRIKE_BITE);
        MartialProgress.addMastery(player, art(), MartialArts.LASH.id(), 1);
        ServerLevel level = player.serverLevel();
        level.playSound(null, e.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, e.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    /** A blow landed: its light, blood, shake and hit-stop on the clients round (client/KaguneImpactFx). */
    static void impact(ServerPlayer player, LivingEntity e, int strike) {
        if (player.connection == null) return;
        Vec3 at = e.getBoundingBox().getCenter().add(0, e.getBbHeight() * 0.1, 0);
        Vec3 toward = player.position().subtract(e.position()).multiply(1, 0, 1);
        if (toward.lengthSqr() > 1e-4) at = at.add(toward.normalize().scale(e.getBbWidth() * 0.45));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new com.murimblock.network.KaguneImpactPayload(player.getId(), e.getId(), at.x, at.y, at.z, strike));
    }

    // ---------------------------------------------------------------- Grapple, Constrict: the kagune holds on

    /**
     * A tentacle or a centipede holding on, from {@code from} (game time) to {@code until}: to a foe (pulled in by Grapple,
     * crushed in Constrict's coils) or, for Grapple with no foe, to a point of a wall it pulls him to.
     */
    private record Hold(MartialArts.Technique technique, @Nullable LivingEntity target, @Nullable Vec3 anchor, long from, long until) { }
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    /** Tells the clients round him what the kagune goes for (client/KaguneGrabs): a foe, or a point. */
    private static void grab(ServerPlayer player, int strike, @Nullable LivingEntity target, Vec3 at) {
        if (player.connection == null) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new com.murimblock.network.KaguneGrabPayload(player.getId(), strike, target == null ? -1 : target.getId(), at.x, at.y, at.z));
    }

    /** The nearest foe he looks at, within reach and cone, in sight. */
    @Nullable static LivingEntity lookedAt(ServerPlayer player, double reach, double cone) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach),
                e -> MartialCombat.isEnemy(e, player))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > reach || d < 1e-3) continue;
            if (Math.toDegrees(Math.acos(Math.clamp(to.normalize().dot(look), -1, 1))) > cone + Math.toDegrees(Math.atan2(e.getBbWidth(), d))) continue;
            if (!player.hasLineOfSight(e)) continue;
            if (d < bestDistance) { bestDistance = d; best = e; }
        }
        return best;
    }

    /** Whether a foe is held in Constrict's coils now. */
    public static boolean constricted(LivingEntity e) {
        for (Hold h : HOLDS.values()) if (h.technique() == MartialArts.CONSTRICT && h.target() == e) return true;
        return false;
    }

    private static void holdTick(ServerPlayer player, long now) {
        Hold h = HOLDS.get(player.getUUID());
        if (h == null || now < h.from()) return;
        LivingEntity target = h.target();
        boolean gone = target != null && (!target.isAlive() || target.level() != player.level());
        if (now > h.until() || gone) {
            HOLDS.remove(player.getUUID());
            // Let go: Constrict throws the crushed foe off.
            if (h.technique() == MartialArts.CONSTRICT && target != null && target.isAlive()) {
                Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, player.getYRot()) : away.normalize();
                target.setDeltaMovement(away.x * CONSTRICT_TOSS, 0.45, away.z * CONSTRICT_TOSS);
                target.hurtMarked = true;
            }
            return;
        }
        long t = now - h.from();
        if (h.technique() == MartialArts.GRAPPLE) {
            if (target != null) {
                if (t == 0) {
                    // Hooked: struck, and pulled in to just in front of him.
                    target.invulnerableTime = 0;
                    target.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.DEVOURING_DEMON, GRAPPLE_SHARE));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GRAPPLE_PULL_TICKS + 10, 3, false, false));
                    impact(player, target, KaguneState.STRIKE_GRAPPLE);
                    MartialProgress.addMastery(player, art(), MartialArts.GRAPPLE.id(), 1);
                }
                Vec3 to = player.position().add(Vec3.directionFromRotation(0, player.getYRot()).scale(GRAPPLE_PULL_TO)).subtract(target.position());
                if (to.length() < 0.6) { HOLDS.remove(player.getUUID()); return; }
                target.setDeltaMovement(to.scale(GRAPPLE_PULL_RATE).add(0, 0.08, 0));
                target.hurtMarked = true;
            } else if (h.anchor() != null) {
                // Hooked into the wall: pulled to it, never hurt by the fall it saves.
                Vec3 to = h.anchor().subtract(player.position().add(0, 0.6, 0));
                if (to.length() < 1.2) { HOLDS.remove(player.getUUID()); player.fallDistance = 0; return; }
                if (t == 0) player.serverLevel().playSound(null, net.minecraft.core.BlockPos.containing(h.anchor()), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
                player.setDeltaMovement(to.normalize().scale(Math.min(GRAPPLE_SPEED, to.length() * 0.35)).add(0, 0.06, 0));
                player.hurtMarked = true;
                player.fallDistance = 0;
            }
            return;
        }
        if (h.technique() == MartialArts.CONSTRICT && target != null) {
            // Coiled round: held where it is, crushed again and again.
            target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
            target.hurtMarked = true;
            if (t == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CONSTRICT_HOLD_TICKS + 2, 6, false, false));
            }
            if (t % CONSTRICT_CRUSH_EVERY == CONSTRICT_CRUSH_EVERY / 2) {
                target.invulnerableTime = 0;
                if (target.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.DEVOURING_DEMON, CONSTRICT_SHARE)))
                    impact(player, target, KaguneState.STRIKE_CONSTRICT);
                player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.TURTLE_EGG_CRACK, SoundSource.PLAYERS, 1.2F, 0.5F);
            }
        }
    }

    // ---------------------------------------------------------------- blows

    /** The demon form strikes harder and takes less. */
    @SubscribeEvent
    static void incoming(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity() && KaguneState.beast(attacker))
            event.setAmount(event.getAmount() * DEMON_DAMAGE);
        if (event.getEntity() instanceof ServerPlayer victim && KaguneState.beast(victim)
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))
            event.setAmount(event.getAmount() * DEMON_GUARD);
    }

    /** Blows landed and taken feed the Fury. */
    @SubscribeEvent
    static void dealt(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0 || event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && MartialCombat.isEnemy(event.getEntity(), attacker))
            gain(attacker, KaguneState.demon(attacker) ? DEMON_HIT_FURY : HIT_FURY);
        if (event.getEntity() instanceof ServerPlayer victim && !KaguneState.demon(victim)) gain(victim, HURT_FURY);
    }

    @SubscribeEvent
    static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    @SubscribeEvent
    static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) forget(player);
    }

    /** The kagune put away, its Fury lost. */
    static void forget(ServerPlayer player) {
        UUID id = player.getUUID();
        USERS.remove(id);
        PENDING.remove(id);
        HOLDS.remove(id);
        LAST_GAIN.remove(id);
        KaguneState.put(player, KaguneState.NONE);
    }

    /** For the tests: everything forgotten for this player. */
    public static void stopForTests(ServerPlayer player) {
        forget(player);
    }
}
