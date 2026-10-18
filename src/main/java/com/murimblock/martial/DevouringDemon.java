package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.combat.CombatService;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.integration.epicfight.DemonArtAnimations;
import java.util.HashMap;
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

    private record Pending(MartialArts.Technique technique, long at) { }
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
        Pending p = PENDING.get(id);
        if (p != null && now >= p.at()) {
            PENDING.remove(id);
            if (p.technique() == MartialArts.LASH) lashHits(player);
            else if (p.technique() == MartialArts.CENTIPEDE_BITE) biteHits(player);
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
        return false;
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
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BITE_SLOW_TICKS, 1));
        Vec3 in = player.position().subtract(e.position()).multiply(1, 0, 1);
        if (in.lengthSqr() > 1e-4) {
            in = in.normalize();
            e.push(in.x * BITE_PULL, 0.15, in.z * BITE_PULL);
            e.hurtMarked = true;
        }
        MartialProgress.addMastery(player, art(), MartialArts.LASH.id(), 1);
        ServerLevel level = player.serverLevel();
        level.playSound(null, e.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, e.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.5F);
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
        LAST_GAIN.remove(id);
        KaguneState.put(player, KaguneState.NONE);
    }

    /** For the tests: everything forgotten for this player. */
    public static void stopForTests(ServerPlayer player) {
        forget(player);
    }
}
