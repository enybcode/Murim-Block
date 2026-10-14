package com.murimblock.martial;

import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Long Fist in play (docs/combat/styles/long-fist.md), the first art of the fist, bare-handed. Its M1 are Epic
 * Fight's own bare-hand strikes; its passive, Steady Breath, only gives Qi back on them (and 5 on a perfect parry, in
 * {@link MartialCombat}). Every technique picks its variant on the server when cast, then plays its animation, whose
 * boxes deal the damage:
 * <ul>
 *   <li><b>Tiger Step Punch</b>: a bound of 3 blocks (5 evolved: Tiger Leap) and a punch; from the air a dive that knocks
 *   down; already at the chest, within a second of a perfect parry, it is <b>Inch Force</b>, the secret;</li>
 *   <li><b>Rapid Fists</b>: five punches that hold the target, then one that pushes (seven evolved: Hundred Fists); from
 *   the air three, then a hammer fist that knocks down;</li>
 *   <li><b>Whirlwind Kick</b>: the tornado kick, all round (wider and knocking down evolved: Tornado Kick); from the air
 *   the turn coming down, all round at the landing.</li>
 * </ul>
 * Its finish is Final Palm.
 */
public final class LongFist {
    /** Inch Force: the target must already be this close in front (blocks)... */
    public static final double INCH_REACH = 1.4;
    /** ...and the perfect parry this recent (ticks: one second). */
    public static final int INCH_WINDOW_TICKS = 20;
    /** How long Inch Force holds the target still before it flies (the moment everything stops), in ticks. */
    public static final int INCH_HOLD_TICKS = 3;
    /**
     * Inch Force sends the target 6 blocks in a low arc: how far it goes each tick of its flight, and how much it rises
     * (then falls). The body is moved by hand: Epic Fight's stun would cancel any speed given to it.
     */
    private static final double[] INCH_FLIGHT = {1.1, 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4};
    private static final double[] INCH_ARC = {0.3, 0.2, 0.1, 0.0, -0.1, -0.2, -0.3, -0.3};
    /** How long Inch Force stuns the target (ticks: one second). */
    public static final int INCH_STUN_TICKS = 20;
    /** Qi given back by a perfect parry with Steady Breath (and 1 by each punch that lands, as for the sword). */
    public static final double PARRY_QI = 5;
    /** The finish's palm lands this many ticks after it starts (1.4 s). */
    public static final int FINISH_HIT_TICK = 28;
    /** The descent of the air Whirlwind Kick: its downward speed (blocks per tick). */
    public static final double DESCENT_SPEED = 1.4;

    private LongFist() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.LONG_FIST);
    }

    /** Whether this player fights with Steady Breath: the Long Fist is their main style. */
    public static boolean stanceActive(Player player) {
        return MartialService.data(player).mainArt().filter(MartialArts.LONG_FIST::equals).isPresent();
    }

    /** Bare hands: nothing in the main hand (a glove item of Epic Fight's fist category does not count). */
    public static boolean bareHanded(Player player) {
        return player.getMainHandItem().isEmpty();
    }

    /** Whether this player raises the Long Fist's bare-hand guard: the main style, bare-handed. */
    public static boolean guardsBareHanded(Player player) {
        return stanceActive(player) && bareHanded(player);
    }

    // ---------------------------------------------------------------- swings

    /** Epic Fight's bare-hand strikes: its three punches, the sprinting attack and the air attack. */
    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return a == Animations.FIST_AUTO1 || a == Animations.FIST_AUTO2 || a == Animations.FIST_AUTO3
                || a == Animations.FIST_DASH || a == Animations.FIST_AIR_SLASH;
    }

    /**
     * What an animation counts for in this art: the passive (Epic Fight's bare-hand strikes, when the Long Fist is the
     * player's main style and their hands are bare) or a technique; null if not the art's.
     */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (isStrike(a)) return guardsBareHanded(player) ? MartialArts.STEADY_BREATH.id() : null;
        if (a == MurimAnimations.TIGER_STEP_PUNCH || a == MurimAnimations.TIGER_STEP_PUNCH_LONG || a == MurimAnimations.TIGER_STEP_DIVE
                || a == MurimAnimations.INCH_FORCE) return MartialArts.TIGER_STEP_PUNCH.id();
        if (a == MurimAnimations.RAPID_FISTS || a == MurimAnimations.RAPID_FISTS_STORM || a == MurimAnimations.RAPID_FISTS_AIR)
            return MartialArts.RAPID_FISTS.id();
        if (a == MurimAnimations.WHIRLWIND_KICK || a == MurimAnimations.WHIRLWIND_KICK_GREAT || a == MurimAnimations.WHIRLWIND_DESCENT)
            return MartialArts.WHIRLWIND_KICK.id();
        return null;
    }

    /** The art's hits that knock down, ending a combo. */
    static boolean knocksDown(AnimationAccessor<? extends StaticAnimation> a) {
        return a == MurimAnimations.TIGER_STEP_DIVE || a == MurimAnimations.WHIRLWIND_KICK_GREAT;
    }

    // ---------------------------------------------------------------- techniques

    /**
     * Starts one of the art's techniques in the variant the moment calls for; false if it cannot start now. Bare hands
     * are checked before ({@link Techniques#canUse}).
     */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        MartialData data = MartialService.data(player);
        boolean air = AzureCloudSword.airborne(player);
        if (technique == MartialArts.TIGER_STEP_PUNCH) {
            LivingEntity close = inchTarget(player);
            if (close != null) {
                inchForce(player, patch, close);
            } else if (air) {
                play(player, patch, MurimAnimations.TIGER_STEP_DIVE, technique);
                MartialCombat.noFallFor(player, 40);
            } else {
                play(player, patch, data.evolved(art(), technique.id()) ? MurimAnimations.TIGER_STEP_PUNCH_LONG : MurimAnimations.TIGER_STEP_PUNCH, technique);
                dust(player.serverLevel(), player.position(), 10);
            }
            return true;
        }
        if (technique == MartialArts.RAPID_FISTS) {
            if (air) {
                play(player, patch, MurimAnimations.RAPID_FISTS_AIR, technique);
                MartialCombat.noFallFor(player, 40);
            } else {
                boolean storm = data.evolved(art(), technique.id());
                FLURRIES.put(player.getUUID(), new Flurry(storm ? MurimAnimations.RAPID_FISTS_STORM_PUNCHES : MurimAnimations.RAPID_FISTS_PUNCHES));
                play(player, patch, storm ? MurimAnimations.RAPID_FISTS_STORM : MurimAnimations.RAPID_FISTS, technique);
            }
            return true;
        }
        if (technique == MartialArts.WHIRLWIND_KICK) {
            if (air) {
                // Thrown down at once, turning; the kick sweeps all round as the body lands.
                play(player, patch, MurimAnimations.WHIRLWIND_DESCENT, technique);
                Vec3 m = player.getDeltaMovement();
                player.setDeltaMovement(m.x * 0.2, -DESCENT_SPEED, m.z * 0.2);
                player.hurtMarked = true;
                MartialCombat.noFallFor(player, 40);
                MartialCombat.later(player, 6, () -> ring(player.serverLevel(), player.position(), 2.5));
            } else {
                boolean great = data.evolved(art(), technique.id());
                play(player, patch, great ? MurimAnimations.WHIRLWIND_KICK_GREAT : MurimAnimations.WHIRLWIND_KICK, technique);
                MartialCombat.later(player, 6, () -> ring(player.serverLevel(), player.position(), great ? 3.5 : 2.5));
            }
            return true;
        }
        return false;
    }

    /**
     * The body Inch Force would strike now: on the ground, within a second of a perfect parry, a target already at the
     * fist; null if Tiger Step Punch would bound as usual.
     */
    @Nullable static LivingEntity inchTarget(ServerPlayer player) {
        if (AzureCloudSword.airborne(player) || !MurimGuardSkill.parriedWithin(player, INCH_WINDOW_TICKS)) return null;
        return AzureCloudSword.targetAhead(player, INCH_REACH);
    }

    /**
     * Whether this cast is Inch Force: like a riposte, it may cut the guard's recoil short (the parry is what opens it).
     */
    static boolean countersParry(ServerPlayer player, @Nullable MartialArts.Technique technique) {
        return technique == MartialArts.TIGER_STEP_PUNCH && inchTarget(player) != null;
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation,
                             MartialArts.Technique technique) {
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
    }

    /**
     * Inch Force: no bound, one punch from where the fist already is (its hit sends the target flying, {@link #onHit}).
     * The first time, the secret is revealed.
     */
    static void inchForce(ServerPlayer player, PlayerPatch<?> patch, LivingEntity target) {
        Vec3 look = target.position().subtract(player.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        play(player, patch, MurimAnimations.INCH_FORCE, MartialArts.TIGER_STEP_PUNCH);
        MartialData data = MartialService.data(player);
        if (!data.hasTechnique(MartialArts.INCH_FORCE.id())) {
            MartialService.set(player, data.unlock(MartialArts.INCH_FORCE));
            player.sendSystemMessage(Component.translatable("technique.murimblock.inch_force.discovered"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
    }

    // ---------------------------------------------------------------- hits

    /** A flurry of Rapid Fists on the ground: how many hits it has, and how many each target took (for the quest). */
    private static final class Flurry {
        final int hits;
        final Map<UUID, Integer> taken = new HashMap<>();
        boolean counted;

        Flurry(int punches) {
            this.hits = punches + 1;
        }
    }

    private static final Map<UUID, Flurry> FLURRIES = new HashMap<>();

    /** What one of the art's hits does besides its damage, and what it counts for in the quests. */
    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean enemy) {
        if (animation == MurimAnimations.RAPID_FISTS || animation == MurimAnimations.RAPID_FISTS_STORM) {
            // The whole flurry on one enemy, every punch and the last: a full flurry (quest Unbroken Flurry).
            Flurry f = FLURRIES.get(player.getUUID());
            if (f != null && enemy) {
                int n = f.taken.merge(target.getUUID(), 1, Integer::sum);
                if (n >= f.hits && !f.counted) {
                    f.counted = true;
                    MartialProgress.count(player, MartialArts.UNBROKEN_FLURRY_FULL, 1);
                }
            }
        }
        // A hit taken on a guard (perfect or normal parry) does only its damage.
        if (MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target)) return;
        if (animation == MurimAnimations.INCH_FORCE) {
            inchImpact(player, target);
        } else if (animation == MurimAnimations.RAPID_FISTS || animation == MurimAnimations.RAPID_FISTS_STORM) {
            ServerLevel level = player.serverLevel();
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 3, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(0.95F, 0.97F, 1.0F), 1.4F);

    /**
     * Inch Force lands: everything stops a moment (the target held still, the sound swallowed), then the target flies
     * about 6 blocks away and stays stunned a second. A ring of white Qi on its chest, a wave behind it.
     */
    static void inchImpact(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
        Vec3 dir = away.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, player.getYRot()) : away.normalize();
        // The ring on the chest, across the line of the blow.
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            Vec3 p = chest.add(side.scale(Math.cos(a) * 0.55)).add(0, Math.sin(a) * 0.55, 0);
            level.sendParticles(WHITE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.6F);
        hold(target, INCH_HOLD_TICKS);
        MartialCombat.later(player, INCH_HOLD_TICKS, () -> {
            if (!target.isAlive()) return;
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
            if (patch != null) patch.applyStun(StunType.LONG, INCH_STUN_TICKS / 20.0F);
            double scale = Math.max(0.2, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            for (int i = 0; i < INCH_FLIGHT.length; i++) {
                Vec3 step = new Vec3(dir.x * INCH_FLIGHT[i] * scale, INCH_ARC[i], dir.z * INCH_FLIGHT[i] * scale);
                MartialCombat.later(player, i, () -> fly(target, step));
            }
            // The wave in the air behind the body.
            Vec3 behind = chest.add(dir.scale(1.2));
            level.sendParticles(ParticleTypes.SONIC_BOOM, behind.x, behind.y, behind.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CLOUD, behind.x, behind.y, behind.z, 8, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.8F);
        });
    }

    /**
     * One tick of a flight: the body moved by `step` (a mob through its own collisions; a player by a teleport, not into
     * a wall), its fall forgiven.
     */
    private static void fly(LivingEntity target, Vec3 step) {
        if (!target.isAlive()) return;
        if (target instanceof ServerPlayer other) {
            Vec3 move = step;
            if (!other.level().noCollision(other, other.getBoundingBox().move(move))) move = new Vec3(step.x, Math.max(0, step.y), step.z);
            if (!other.level().noCollision(other, other.getBoundingBox().move(move))) return;
            Vec3 at = other.position().add(move);
            other.connection.teleport(at.x, at.y, at.z, other.getYRot(), other.getXRot());
        } else {
            target.move(MoverType.SELF, step);
        }
        target.setDeltaMovement(0, 0, 0);
        target.resetFallDistance();
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    /** Holds a body still for a few ticks (a mob by slowness; a player is held by the hit's own stun). */
    private static void hold(LivingEntity target, int ticks) {
        target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
        target.hurtMarked = true;
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
    }

    // ---------------------------------------------------------------- effects

    /** Dust kicked up where the bound starts. */
    static void dust(ServerLevel level, Vec3 at, int count) {
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.1, at.z, count, 0.3, 0.05, 0.3, 0.02);
    }

    /** A ring of dust raised at ground level all round (the whirlwind), `radius` blocks wide. */
    static void ring(ServerLevel level, Vec3 at, double radius) {
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2 * i / 28;
            level.sendParticles(ParticleTypes.POOF, at.x + Math.cos(a) * radius, at.y + 0.15, at.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0.01);
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    // ---------------------------------------------------------------- the finish: Final Palm

    /** Whether this executor plays the art's finish: their main style, bare hands. */
    public static boolean finishes(ServerPlayer executor) {
        return guardsBareHanded(executor);
    }

    /**
     * The finish's breath and palm, tick by tick (sent by the server, seen by all): dust under the stance, a white breath
     * gathering at the hands while the arms rise and come down, then the palm: a ring of Qi on the chest and the ground
     * shaken all round.
     */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        ServerLevel level = executor.serverLevel();
        if (elapsed == 6) dust(level, executor.position(), 12);
        if (elapsed >= 8 && elapsed < 20 && elapsed % 2 == 0) {
            Vec3 hands = executor.position().add(0, 1.4, 0);
            level.sendParticles(WHITE, hands.x, hands.y, hands.z, 3, 0.3, 0.3, 0.3, 0);
        }
        if (elapsed == 10) level.playSound(null, executor.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 0.8F, 0.8F);
        if (elapsed == FINISH_HIT_TICK) {
            Vec3 v = victim.position().add(0, 0.3, 0);
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                level.sendParticles(WHITE, v.x + Math.cos(a) * 0.6, v.y, v.z + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
            }
            ring(level, victim.position(), 2.0);
            level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.5F);
        }
    }

    // ---------------------------------------------------------------- state

    static void forget(UUID player) {
        if (player == null) FLURRIES.clear();
        else FLURRIES.remove(player);
    }
}
