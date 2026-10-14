package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.GuardAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * The Azure Cloud Sword Art in play (docs/combat/styles/azure-cloud-sword.md). Every technique picks its variant on the
 * server from the moment it is cast, then plays an Epic Fight animation whose own boxes deal the damage:
 * <ul>
 *   <li><b>Cloud Step Slash</b>: the dash; from the air it plunges and knocks down; as a riposte it is short and cuts
 *   twice; within a quarter second of a perfect parry it is <b>Swallow Returns</b>, the secret (behind the attacker).
 *   Evolved (Thousand-Li Cloud Step), a second dash within 1.5 s is free;</li>
 *   <li><b>Thousand Drizzle</b>: close, six thrusts that hold the target then one that pushes (as a riposte, that last
 *   one breaks the guard); with no enemy within 3 blocks in front, one thrust sending a wave straight ahead that interrupts. Evolved (Storm
 *   of Needles), wider, and the last thrust sends a needle of Qi;</li>
 *   <li><b>Heaven-Splitting Crescent</b>: on the ground, a rising crescent that lifts the target and the player (higher
 *   as a riposte); in the air, Cloud-Splitting Descent, a slam that knocks down all around. Evolved (Azure Heaven
 *   Crescent), it also sends a tall blade of Qi.</li>
 * </ul>
 * Its passive, Flowing Cloud Stance, replaces the M1 chain of a sword ({@link #autoAttackMotions}) for whoever has this
 * art as their main style; its parry is in {@link MartialCombat}. Its finish is Last Rain Over the Clouds.
 */
public final class AzureCloudSword {
    /** The sword arts need a blade of the sword family in the main hand. */
    public static final Set<WeaponCategory> SWORDS = Set.of(WeaponCategories.SWORD, WeaponCategories.LONGSWORD,
            WeaponCategories.UCHIGATANA, WeaponCategories.TACHI);
    /**
     * Thousand Drizzle's flurry when an enemy stands this close in front (blocks, as far as the thrusts reach); otherwise
     * (farther, or nobody) the wave, sent straight ahead.
     */
    public static final double FLURRY_REACH = 3.0;
    /** How high above the ground a player must be for the air variants (blocks). */
    public static final double AIR_HEIGHT = 1.2;
    /** Upward speed given by Rising Cloud, blocks per tick. */
    public static final double RISING_CLOUD_LIFT = 0.45;
    /**
     * The crescent's leap: an upward speed that takes a player 3 blocks up under Minecraft's gravity (0.08 a tick,
     * drag 0.98), the top reached about 10 ticks later.
     */
    public static final double CRESCENT_LEAP = 0.68;
    /** How long a target hit by the crescent is carried up with the player, from the leap (to the top of it). */
    public static final int CARRY_TICKS = 10;
    /** The finish's blow lands this many ticks after it starts (1.5 s). */
    public static final int FINISH_HIT_TICK = 30;

    private AzureCloudSword() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.AZURE_CLOUD_SWORD);
    }

    /** Whether this player fights with Flowing Cloud Stance: the art is their main style. */
    public static boolean stanceActive(Player player) {
        return MartialService.data(player).mainArt().filter(MartialArts.AZURE_CLOUD_SWORD::equals).isPresent();
    }

    static boolean holdsSword(Player player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        return patch != null && SWORDS.contains(patch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory());
    }

    // ---------------------------------------------------------------- the passive's M1

    /**
     * The M1 chain of Flowing Cloud Stance for a sword in hand: the four strikes the player placed, then Epic Fight's
     * two extra entries, the sprinting attack (Wind Step) and the air attack (Falling Rain). Null when the stance is
     * not the player's (Epic Fight then uses the weapon's own chain).
     */
    @Nullable public static List<AnimationAccessor<? extends AttackAnimation>> autoAttackMotions(PlayerPatch<?> patch, CapabilityItem item) {
        if (patch == null || !(patch.getOriginal() instanceof Player player) || !SWORDS.contains(item.getWeaponCategory())) return null;
        MartialData data = MartialService.data(player);
        if (!data.mainArt().filter(MartialArts.AZURE_CLOUD_SWORD::equals).isPresent() || MurimAnimations.DRIFTING_CUT == null) return null;
        List<AnimationAccessor<? extends AttackAnimation>> chain = new ArrayList<>();
        for (ResourceLocation strike : data.strikes(MartialArts.FLOWING_CLOUD_STANCE)) {
            AnimationAccessor<? extends AttackAnimation> a = strikeAnimation(strike);
            if (a != null) chain.add(a);
        }
        if (chain.size() != MartialArts.Passive.M1_SLOTS) return null;
        chain.add(MurimAnimations.WIND_STEP);
        chain.add(MurimAnimations.FALLING_RAIN_AIR);
        return chain;
    }

    @Nullable static AnimationAccessor<? extends AttackAnimation> strikeAnimation(ResourceLocation strike) {
        if (strike.equals(MartialArts.DRIFTING_CUT.id())) return MurimAnimations.DRIFTING_CUT;
        if (strike.equals(MartialArts.RETURNING_CUT.id())) return MurimAnimations.RETURNING_CUT;
        if (strike.equals(MartialArts.PIERCING_MIST.id())) return MurimAnimations.PIERCING_MIST;
        if (strike.equals(MartialArts.RISING_CLOUD.id())) return MurimAnimations.RISING_CLOUD;
        if (strike.equals(MartialArts.FALLING_RAIN.id())) return MurimAnimations.FALLING_RAIN;
        if (strike.equals(MartialArts.WIND_STEP.id())) return MurimAnimations.WIND_STEP;
        return null;
    }

    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return a == MurimAnimations.DRIFTING_CUT || a == MurimAnimations.RETURNING_CUT || a == MurimAnimations.PIERCING_MIST
                || a == MurimAnimations.RISING_CLOUD || a == MurimAnimations.FALLING_RAIN || a == MurimAnimations.FALLING_RAIN_AIR
                || a == MurimAnimations.WIND_STEP;
    }

    /** What an animation of the art counts for: the passive (strikes) or a technique; null if not the art's. */
    @Nullable static ResourceLocation creditOf(AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (isStrike(a)) return MartialArts.FLOWING_CLOUD_STANCE.id();
        if (a == MurimAnimations.CLOUD_STEP_SLASH || a == MurimAnimations.CLOUD_STEP_DIVE || a == MurimAnimations.CLOUD_STEP_RIPOSTE
                || a == MurimAnimations.SWALLOW_RETURNS) return MartialArts.CLOUD_STEP_SLASH.id();
        if (a == MurimAnimations.THOUSAND_DRIZZLE || a == MurimAnimations.THOUSAND_DRIZZLE_STORM) return MartialArts.THOUSAND_DRIZZLE.id();
        if (a == MurimAnimations.HEAVEN_SPLITTING_CRESCENT || a == MurimAnimations.CLOUD_SPLITTING_DESCENT) return MartialArts.HEAVEN_SPLITTING_CRESCENT.id();
        return null;
    }

    /** The art's hits that knock down, ending a combo. */
    static boolean knocksDown(AnimationAccessor<? extends StaticAnimation> a) {
        return a == MurimAnimations.FALLING_RAIN || a == MurimAnimations.FALLING_RAIN_AIR || a == MurimAnimations.CLOUD_STEP_DIVE
                || a == MurimAnimations.CLOUD_SPLITTING_DESCENT;
    }

    // ---------------------------------------------------------------- techniques

    /** Whether the player is in the air high enough for an air variant. */
    static boolean airborne(ServerPlayer player) {
        if (player.onGround() || player.isInWater() || player.onClimbable()) return false;
        AABB below = player.getBoundingBox().move(0, -AIR_HEIGHT, 0);
        return player.level().noCollision(player, below);
    }

    /** Since when each player holds Cloud Step Slash's draw stance (game time). */
    private static final Map<UUID, Long> AIMS = new HashMap<>();

    /**
     * Takes Cloud Step Slash's draw stance (the key held): crouched, the blade sheathed at the hip, turning with the camera
     * to aim. Not in the air nor with a riposte to answer (those variants go at once, on the release). False if not taken.
     */
    static boolean aim(ServerPlayer player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || MurimAnimations.CLOUD_STEP_AIM == null || airborne(player) || MartialCombat.riposte(player) != null) return false;
        patch.playAnimationSynchronized(MurimAnimations.CLOUD_STEP_AIM, 0.0F);
        AIMS.put(player.getUUID(), player.level().getGameTime());
        return true;
    }

    /** Whether this player is in Cloud Step Slash's draw stance (their key held). */
    public static boolean aiming(Player player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        return main != null && !main.isEmpty() && main.getRealAnimation() == MurimAnimations.CLOUD_STEP_AIM;
    }

    /** A stance held past this (the release lost) is dropped. The client lets go by itself at AIM_MAX_TICKS. */
    static final int AIM_DROP_TICKS = MartialArts.CLOUD_STEP_AIM_MAX_TICKS + 40;

    static void tickAim(ServerPlayer player, long now) {
        Long since = AIMS.get(player.getUUID());
        if (since == null) return;
        if (!aiming(player)) {
            AIMS.remove(player.getUUID());
        } else if (now - since > AIM_DROP_TICKS) {
            AIMS.remove(player.getUUID());
            PlayerPatch<?> patch = EpicFightBridge.patch(player);
            if (patch != null) patch.playAnimationSynchronized(Animations.BIPED_IDLE, 0.0F);
        }
    }

    static void forgetAim(UUID player) {
        if (player == null) AIMS.clear();
        else AIMS.remove(player);
    }

    /** Whether the player is in a guard's recoil (after a parry): a riposte may cut it short. */
    static boolean inGuardMotion(PlayerPatch<?> patch) {
        AnimationPlayer main = patch.getAnimator().getPlayerFor(null);
        return main != null && !main.isEmpty() && main.getRealAnimation() != null && main.getRealAnimation().get() instanceof GuardAnimation;
    }

    /**
     * Starts one of the art's techniques in the variant the moment calls for; false if it cannot start now. `parry` is the
     * perfect parry the player may riposte to (null if none).
     */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique, @Nullable MartialCombat.Riposte parry) {
        boolean riposte = parry != null;
        MartialData data = MartialService.data(player);
        if (technique == MartialArts.CLOUD_STEP_SLASH) {
            if (parry != null && player.level().getGameTime() - parry.tick() <= MartialCombat.SWALLOW_TICKS && swallowReturns(player, patch, parry)) return true;
            if (airborne(player)) {
                play(player, patch, MurimAnimations.CLOUD_STEP_DIVE, technique, riposte);
                MartialCombat.noFallFor(player, 40);
            } else {
                play(player, patch, riposte ? MurimAnimations.CLOUD_STEP_RIPOSTE : MurimAnimations.CLOUD_STEP_SLASH, technique, riposte);
            }
            return true;
        }
        if (technique == MartialArts.THOUSAND_DRIZZLE) {
            boolean storm = data.evolved(art(), technique.id());
            // An enemy within the thrusts' reach in front: the flurry. Otherwise, the wave straight ahead.
            if (targetAhead(player, FLURRY_REACH) == null) {
                play(player, patch, MurimAnimations.DRIZZLE_WAVE, technique, riposte);
                // The ghost sword leaves as the thrust reaches its full length (0.25 s, the arm straight out).
                MartialCombat.later(player, 5, () -> QiBladeEntity.send(player, QiBladeEntity.WAVE, technique.id()));
            } else {
                play(player, patch, storm ? MurimAnimations.THOUSAND_DRIZZLE_STORM : MurimAnimations.THOUSAND_DRIZZLE, technique, riposte);
                if (storm) MartialCombat.later(player, Math.round((MurimAnimations.DRIZZLE_FINAL + 0.05F) * 20),
                        () -> QiBladeEntity.send(player, QiBladeEntity.NEEDLE, technique.id()));
            }
            return true;
        }
        if (technique == MartialArts.HEAVEN_SPLITTING_CRESCENT) {
            if (airborne(player)) {
                // Cloud-Splitting Descent: thrown down at once; the slam's box hits all around as the body lands.
                play(player, patch, MurimAnimations.CLOUD_SPLITTING_DESCENT, technique, riposte);
                Vec3 m = player.getDeltaMovement();
                player.setDeltaMovement(m.x * 0.2, -1.6, m.z * 0.2);
                player.hurtMarked = true;
                MartialCombat.noFallFor(player, 40);
                MartialCombat.later(player, 5, () -> shockwave(player));
            } else {
                play(player, patch, MurimAnimations.HEAVEN_SPLITTING_CRESCENT, technique, riposte);
                // The player leaves the ground with the cut (0.2 s) and rises 3 blocks; whoever the crescent hits is
                // carried up with them (MartialCombat.carry, on the hit).
                MartialCombat.later(player, 4, () -> MartialCombat.leap(player, CRESCENT_LEAP));
                // As a riposte, the two hang at the top a little longer for the follow-up.
                if (riposte) MartialCombat.later(player, 4 + CARRY_TICKS, () -> MartialCombat.slowFall(player, 30));
                MartialCombat.noFallFor(player, 80);
                if (data.evolved(art(), technique.id())) MartialCombat.later(player, 5, () -> QiBladeEntity.send(player, QiBladeEntity.CRESCENT, technique.id()));
            }
            return true;
        }
        return false;
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation,
                             MartialArts.Technique technique, boolean riposte) {
        MartialCombat.expect(player, animation, technique.id(), riposte);
        patch.playAnimationSynchronized(animation, 0.0F);
    }

    /**
     * Swallow Returns: the player reappears behind the attacker of the parry, facing them, and spins into a cut. The
     * first time, the secret is revealed. False if there is no room behind the attacker (a riposte dash instead).
     */
    static boolean swallowReturns(ServerPlayer player, PlayerPatch<?> patch, MartialCombat.Riposte parry) {
        Entity attacker = player.serverLevel().getEntity(parry.attacker());
        if (!(attacker instanceof LivingEntity foe) || !foe.isAlive() || foe.distanceTo(player) > 6.0) return false;
        Vec3 back = Vec3.directionFromRotation(0, foe.getYRot()).scale(-(foe.getBbWidth() / 2 + 0.9));
        Vec3 to = foe.position().add(back.x, 0, back.z);
        AABB room = player.getBoundingBox().move(to.subtract(player.position()));
        if (!player.level().noCollision(player, room)) return false;
        Vec3 look = foe.position().subtract(to);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        player.teleportTo(player.serverLevel(), to.x, to.y, to.z, yaw, player.getXRot());
        player.setYHeadRot(yaw);
        swirl(player.serverLevel(), player.position().add(0, 1, 0));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6F, 1.8F);
        play(player, patch, MurimAnimations.SWALLOW_RETURNS, MartialArts.CLOUD_STEP_SLASH, true);
        MartialData data = MartialService.data(player);
        if (!data.hasTechnique(MartialArts.SWALLOW_RETURNS.id())) {
            MartialService.set(player, data.unlock(MartialArts.SWALLOW_RETURNS));
            player.sendSystemMessage(Component.translatable("technique.murimblock.swallow_returns.discovered"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
        return true;
    }

    /** The nearest living thing ahead (within 35 degrees of the look) in reach and in sight, or null. */
    @Nullable static LivingEntity targetAhead(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition(), look = Vec3.directionFromRotation(0, player.getYRot());
        LivingEntity best = null;
        double bestDistance = reach;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach),
                e -> e != player && e.isAlive() && !e.isSpectator() && MartialCombat.isEnemy(e, player))) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double d = player.distanceTo(e);
            Vec3 flat = new Vec3(to.x, 0, to.z);
            if (d > bestDistance || flat.lengthSqr() < 1e-4 || flat.normalize().dot(look) < Math.cos(Math.toRadians(35)) || !player.hasLineOfSight(e)) continue;
            best = e;
            bestDistance = d;
        }
        return best;
    }

    /** What one of the art's hits does besides its damage. */
    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean riposte) {
        // A hit taken on a guard (perfect or normal parry) does only its damage: no lift, no carry, no broken guard.
        if (MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target)) return;
        if (animation == MurimAnimations.RISING_CLOUD) {
            // On the next tick: the hit's own stun, which follows this hook, stops the body (lifted now, it would stay put).
            MartialCombat.later(player, 1, () -> {
                if (target.isAlive()) MartialCombat.lift(target, RISING_CLOUD_LIFT);
            });
        } else if (animation == MurimAnimations.HEAVEN_SPLITTING_CRESCENT) {
            // Taken up with the player, held in front of them until the top of the leap.
            MartialCombat.carry(player, target, CARRY_TICKS, riposte);
        } else if (animation == MurimAnimations.THOUSAND_DRIZZLE || animation == MurimAnimations.THOUSAND_DRIZZLE_STORM) {
            PlayerPatch<?> patch = EpicFightBridge.patch(player);
            AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
            if (main == null || main.getElapsedTime() < MurimAnimations.DRIZZLE_FINAL - 0.06F) return;
            // The last cut of a riposte breaks the guard.
            if (riposte) MartialCombat.breakGuard(target);
            // A beat after the last cut, all the flurry's cuts open on the body at once.
            MartialCombat.later(player, DELAYED_CUTS_TICKS, () -> delayedCuts(player, target));
        }
    }

    /** How long after the flurry's last cut its cuts open again on the body (ticks), and their damage (share of the attack). */
    public static final int DELAYED_CUTS_TICKS = 10;
    public static final double DELAYED_CUTS_SHARE = 0.6;

    /**
     * The flurry's delayed cuts: every client near the body draws the six lines of light opening on it at once
     * (DrizzleSlashEffects), and it takes a last share of damage. Not if it is gone, dead or far away by then.
     */
    static void delayedCuts(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive() || target.level() != player.level() || target.distanceToSqr(player) > 24 * 24) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new com.murimblock.network.TechniquePayloads.DelayedCuts(player.getId(), target.getId()));
        player.serverLevel().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);
        player.serverLevel().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.3F);
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player),
                (float) (player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * DELAYED_CUTS_SHARE));
    }

    // ---------------------------------------------------------------- effects

    private static final DustParticleOptions AZURE = new DustParticleOptions(new Vector3f(0.62F, 0.85F, 1.0F), 1.6F);
    private static final DustParticleOptions CLOUD = new DustParticleOptions(new Vector3f(0.86F, 0.93F, 1.0F), 2.4F);

    /** The ring of the descent's slam, three blocks wide. */
    static void shockwave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            level.sendParticles(AZURE, at.x + Math.cos(a) * 3.0, at.y + 0.15, at.z + Math.sin(a) * 3.0, 1, 0, 0.05, 0, 0);
            level.sendParticles(ParticleTypes.CLOUD, at.x + Math.cos(a) * 1.5, at.y + 0.1, at.z + Math.sin(a) * 1.5, 1, 0, 0, 0, 0.02);
        }
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    private static void swirl(ServerLevel level, Vec3 at) {
        level.sendParticles(AZURE, at.x, at.y, at.z, 16, 0.4, 0.6, 0.4, 0.02);
    }

    // ---------------------------------------------------------------- the finish: Last Rain Over the Clouds

    /** Whether this executor plays the art's finish: their main style, a sword in hand. */
    public static boolean finishes(ServerPlayer executor) {
        return stanceActive(executor) && holdsSword(executor);
    }

    /**
     * The finish's sky, tick by tick (sent by the server, seen by all): a cloud of Qi gathers over the downed player,
     * a fine shining rain falls from it, and when the blow lands the cloud is split and the rain stops at once.
     */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        ServerLevel level = executor.serverLevel();
        Vec3 v = victim.position();
        double sky = v.y + 3.2;
        if (elapsed >= 6 && elapsed < FINISH_HIT_TICK) {
            int density = Math.min(6, 1 + (elapsed - 6) / 3);
            level.sendParticles(CLOUD, v.x, sky, v.z, density, 0.9, 0.12, 0.9, 0);
            if (elapsed % 3 == 0) level.sendParticles(ParticleTypes.CLOUD, v.x, sky + 0.1, v.z, 1, 0.7, 0.05, 0.7, 0.005);
        }
        if (elapsed >= 12 && elapsed < FINISH_HIT_TICK) {
            level.sendParticles(ParticleTypes.FALLING_WATER, v.x, sky - 0.2, v.z, 5, 0.8, 0.05, 0.8, 0);
            level.sendParticles(AZURE, v.x, sky - 0.6, v.z, 2, 0.7, 0.4, 0.7, 0);
        }
        if (elapsed == 12) level.playSound(null, victim.blockPosition(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 0.5F, 1.4F);
        if (elapsed == FINISH_HIT_TICK) {
            // The cloud splits in two, the rain stops.
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, v.x, v.y + 0.4, v.z, 1, 0, 0, 0, 0);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 half = new Vec3(v.x, sky, v.z).add(Vec3.directionFromRotation(0, executor.getYRot() + 90 * side).scale(0.6));
                level.sendParticles(CLOUD, half.x, half.y, half.z, 8, 0.3, 0.1, 0.3, 0.05);
            }
            level.sendParticles(AZURE, v.x, v.y + 1.5, v.z, 20, 0.1, 1.4, 0.1, 0.01);
            level.playSound(null, victim.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.9F, 1.3F);
        }
    }
}
