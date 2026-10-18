package com.murimblock.martial;

import com.murimblock.combat.ShatteredGround;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MurimEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Overlord Spear in play (docs/combat/styles/overlord-spear.md, Enzo's design of 18/10/2026): one does not pierce,
 * one pulverises. A spear in hand (any Epic Fight spear; the Spear of Ruin is its own). Its M1 are Epic Fight's spear
 * strikes; its passive, Conqueror's Weight, adds a charge of Domination for each that lands, and at five the next is a
 * battering ram (x1.8, the guard broken, a shock wave splitting the ground four blocks ahead). Its techniques:
 * <ul>
 *   <li><b>Tyrant's Thrust</b>: held to charge, let go: a beam of Qi through all in its line (5 to 8 blocks); under a fifth
 *   of one's health it is the secret, <b>Breaking the Cauldrons</b>: three blocks wide, twice as strong;</li>
 *   <li><b>Earth-Splitting Sweep</b>: the spear brought down, then round at the knees: a ring of cracks, all thrown up;</li>
 *   <li><b>Falling Mountain</b>: a leap up and toward the target, the spear planted as it lands: a crater of 5 blocks;</li>
 *   <li><b>Overlord's Charge</b>: ten blocks, the bodies struck carried on the point, crushed against the first wall;</li>
 *   <li><b>Hundred Battles Rampage</b>: twelve thrusts in two seconds, each with a little burst of Qi, the last throws;</li>
 *   <li><b>Conqueror Descends</b>, the ultimate: a giant spear of Qi falls from the sky on the place aimed at.</li>
 * </ul>
 * Its finish is Overlord's Verdict: the fallen lifted on the point, then driven into the ground. Broken ground always
 * comes back ({@link ShatteredGround}). Its manual comes to whoever brings the Celestial Peak Spear to mastery 50.
 */
public final class OverlordSpear {
    /** Conqueror's Weight: the charges at most, how long they last without a hit, the ram's damage and its wave. */
    public static final int DOMINATION_MAX = 5, DOMINATION_TICKS = 80;
    public static final float RAM_FACTOR = 1.8F, RAM_WAVE_SHARE = 0.6F;
    public static final double RAM_WAVE = 4;
    /** Tyrant's Thrust: the beam's length (uncharged, full), width, share; the secret's. */
    public static final double BEAM_MIN = 5, BEAM_MAX = 8, BEAM_WIDTH = 1.0, CAULDRONS_WIDTH = 3.0;
    public static final float BEAM_SHARE = 1.5F, CAULDRONS_FACTOR = 2.0F;
    /** Under this share of their health, Tyrant's Thrust is Breaking the Cauldrons. */
    public static final float CAULDRONS_HEALTH = 0.2F;
    /** Earth-Splitting Sweep: its ring, how high it throws. */
    public static final double SWEEP_RING = 4;
    public static final double[] LIFT_STEPS = {0.5, 0.35, 0.22, 0.12};
    /** Falling Mountain: how far and high it leaps, its crater, its shares. */
    public static final double LEAP_REACH = 8, LEAP_HEIGHT = 5, MOUNTAIN_CRATER = 5, MOUNTAIN_CORE = 2.5;
    /** The hole Falling Mountain leaves (the blow reaches MOUNTAIN_CRATER): small and shallow, it comes back. */
    public static final double MOUNTAIN_HOLE = 2.5;
    public static final float MOUNTAIN_CORE_SHARE = 1.8F, MOUNTAIN_SHARE = 1.0F;
    /** Overlord's Charge: how many it carries, the crush against a wall. */
    public static final int CHARGE_CARRY = 3;
    public static final float CRUSH_SHARE = 1.6F;
    /** Hundred Battles Rampage: each thrust's little burst of Qi (share, reach). */
    public static final float BURST_SHARE = 0.15F;
    public static final double BURST_REACH = 1.2;
    /** Conqueror Descends: how far it may fall from the player, how high it starts, its length, its blast. */
    public static final double ULT_REACH = 20, ULT_HEIGHT = 46, ULT_RADIUS = 10, ULT_CORE = 3;
    public static final float ULT_LENGTH = 40, ULT_CORE_SHARE = 3.0F, ULT_SHARE = 1.5F;
    public static final int ULT_FALL_TICKS = 8, ULT_CRATER_DEPTH = 2;
    /** The finish: when the fallen is lifted, when driven into the ground (ticks in). */
    public static final int FINISH_LIFT_TICK = 18, FINISH_HIT_TICK = 32;

    private OverlordSpear() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.OVERLORD_SPEAR);
    }

    /** Whether this player fights with Conqueror's Weight: the Overlord Spear is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.OVERLORD_SPEAR::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.OVERLORD_SPEAR::equals).isPresent();
    }

    public static boolean holdsSpear(Player player) {
        return CelestialPeakSpear.holdsSpear(player);
    }

    /** The art's finish is the executor's when it is their main style, a spear in hand. */
    public static boolean finishes(ServerPlayer executor) {
        return stanceActive(executor) && holdsSpear(executor);
    }

    // ---------------------------------------------------------------- the manual: the Celestial Peak Spear at mastery 50

    /** The Celestial Peak Spear's mastery went from `was` to `now`: at 50, the manual (once a player). */
    static void celestialMastery(ServerPlayer player, int was, int now) {
        if (was >= MartialArts.OVERLORD_FROM_MASTERY || now < MartialArts.OVERLORD_FROM_MASTERY) return;
        MartialData data = MartialService.data(player);
        if (data.knowsArt(MartialArts.OVERLORD_SPEAR) || data.manualsReceived().contains(MartialArts.OVERLORD_SPEAR)) return;
        MartialService.set(player, data.receivedManual(MartialArts.OVERLORD_SPEAR));
        ItemStack manual = new ItemStack(MurimItems.OVERLORD_SPEAR_SCROLL.get());
        if (!player.getInventory().add(manual)) player.drop(manual, false);
        ServerLevel level = player.serverLevel();
        player.sendSystemMessage(Component.translatable("art.murimblock.overlord_spear.enlightenment").withStyle(net.minecraft.ChatFormatting.GOLD));
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.7F);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
    }

    // ---------------------------------------------------------------- swings

    /** What an animation counts for in this art: the passive (the spear strikes, in the stance) or a technique. */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (CelestialPeakSpear.isStrike(a)) return stanceActive(player) && holdsSpear(player) ? MartialArts.CONQUERORS_WEIGHT.id() : null;
        if (a == MurimAnimations.OVERLORD_EARTH_SWEEP) return MartialArts.EARTH_SPLITTING_SWEEP.id();
        if (a == MurimAnimations.OVERLORD_CHARGE) return MartialArts.OVERLORDS_CHARGE.id();
        if (a == MurimAnimations.OVERLORD_RAMPAGE) return MartialArts.HUNDRED_BATTLES_RAMPAGE.id();
        return null;
    }

    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return CelestialPeakSpear.isStrike(a);
    }

    static boolean knocksDown(AnimationAccessor<? extends StaticAnimation> a) {
        return a == MurimAnimations.OVERLORD_RAMPAGE;
    }

    // ---------------------------------------------------------------- Conqueror's Weight

    private record Domination(int charges, long last, boolean ram) { }

    private static final Map<UUID, Domination> DOMINATION = new HashMap<>();

    /** The charges of Domination this player holds now (0 when they have run out). */
    public static int charges(ServerPlayer player) {
        Domination d = DOMINATION.get(player.getUUID());
        return d == null || player.level().getGameTime() - d.last > DOMINATION_TICKS ? 0 : d.charges;
    }

    /** A strike begins: with five charges, it is the battering ram. */
    static void strikeBegan(ServerPlayer player) {
        if (charges(player) >= DOMINATION_MAX) {
            DOMINATION.put(player.getUUID(), new Domination(0, player.level().getGameTime(), true));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.5F, 1.4F);
        }
    }

    /** Whether the strike under way is the battering ram (it strikes x1.8). */
    static boolean ramming(ServerPlayer player) {
        Domination d = DOMINATION.get(player.getUUID());
        return d != null && d.ram;
    }

    /** Tests: sets this player's charges. */
    public static void setChargesForTests(ServerPlayer player, int charges) {
        DOMINATION.put(player.getUUID(), new Domination(charges, player.level().getGameTime(), false));
    }

    /** A strike of the passive landed: a charge more; or the ram's blow: the guard broken, the wave. */
    private static void strikeLanded(ServerPlayer player, LivingEntity target, boolean enemy) {
        Domination d = DOMINATION.get(player.getUUID());
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (d != null && d.ram) {
            DOMINATION.put(player.getUUID(), new Domination(0, now, false));
            if (enemy) MartialCombat.breakGuard(target);
            ramWave(player);
            return;
        }
        if (!enemy) return;
        int charges = Math.min(DOMINATION_MAX, charges(player) + 1);
        DOMINATION.put(player.getUUID(), new Domination(charges, now, false));
        // The spear's cracks flare a little more with each charge; a crackle at five.
        Vec3 tip = tip(player, 2.0);
        level.sendParticles(ParticleTypes.SMALL_FLAME, tip.x, tip.y, tip.z, 2 + charges * 2, 0.15, 0.15, 0.15, 0.01);
        if (charges == DOMINATION_MAX) {
            level.sendParticles(ParticleTypes.LAVA, tip.x, tip.y, tip.z, 4, 0.2, 0.2, 0.2, 0);
            level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.4F, 0.6F);
        }
    }

    /** The ram's wave: the ground split four blocks ahead, all in the line pushed away. */
    static void ramWave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = flatLook(player);
        float damage = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, RAM_WAVE_SHARE);
        for (int k = 1; k <= (int) RAM_WAVE; k++) {
            Vec3 p = player.position().add(dir.scale(k + 0.5));
            BlockPos ground = BlockPos.containing(p.x, player.getY() - 0.5, p.z);
            crack(level, ground);
        }
        OverlordFx.spark(level, player.position().add(dir.scale(1.5)), 1.5);
        OverlordFx.shock(level, player.position().add(dir.scale(RAM_WAVE * 0.6)), RAM_WAVE * 0.5);
        for (LivingEntity e : inLine(player, dir, RAM_WAVE + 0.5, 1.2)) {
            if (blow(player, e, damage)) push(e, dir.scale(1.2), 0.25);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.9F);
    }

    // ---------------------------------------------------------------- techniques

    /** Starts one of the art's techniques. The spear is checked before. */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (FLIGHTS.containsKey(player.getUUID()) || ULTS.containsKey(player.getUUID())) return false;
        if (technique == MartialArts.TYRANTS_THRUST) return release(player, patch);
        if (technique == MartialArts.EARTH_SPLITTING_SWEEP) {
            play(player, patch, MurimAnimations.OVERLORD_EARTH_SWEEP, technique);
            MartialCombat.later(player, ticks(MurimAnimations.OVERLORD_SLAM), () -> sweepSlam(player));
            return true;
        }
        if (technique == MartialArts.FALLING_MOUNTAIN) return leap(player, patch);
        if (technique == MartialArts.OVERLORDS_CHARGE) {
            play(player, patch, MurimAnimations.OVERLORD_CHARGE, technique);
            CHARGES.put(player.getUUID(), new Charge());
            for (int t = ticks(MurimAnimations.OVERLORD_CHARGE_FROM); t <= ticks(MurimAnimations.OVERLORD_CHARGE_TO); t++)
                MartialCombat.later(player, t, () -> chargeTick(player));
            MartialCombat.later(player, ticks(MurimAnimations.OVERLORD_CHARGE_TO) + 1, () -> chargeEnd(player, false));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.7F, 1.0F);
            return true;
        }
        if (technique == MartialArts.HUNDRED_BATTLES_RAMPAGE) {
            play(player, patch, MurimAnimations.OVERLORD_RAMPAGE, technique);
            for (float t : MurimAnimations.OVERLORD_RAMPAGE_THRUSTS) MartialCombat.later(player, ticks(t), () -> burst(player, false));
            MartialCombat.later(player, ticks(MurimAnimations.OVERLORD_RAMPAGE_LAST), () -> burst(player, true));
            return true;
        }
        if (technique == MartialArts.CONQUEROR_DESCENDS) return descend(player, patch);
        return false;
    }

    private static int ticks(float seconds) {
        return Math.round(seconds * 20);
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation,
                             MartialArts.Technique technique) {
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 0.6F, 0.8F);
    }

    // ---------------------------------------------------------------- 1. Tyrant's Thrust

    /** Since when each player charges Tyrant's Thrust. */
    private static final Map<UUID, Long> AIMING = new HashMap<>();

    public static boolean charging(ServerPlayer player) {
        return AIMING.containsKey(player.getUUID());
    }

    /** The key went down: drawn far back, charging. Nothing is spent until it is let go ({@link #release}). */
    static boolean aimThrust(ServerPlayer player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || charging(player) || MurimAnimations.OVERLORD_TYRANT_CHARGE == null || !holdsSpear(player)) return false;
        patch.playAnimationSynchronized(MurimAnimations.OVERLORD_TYRANT_CHARGE, 0.0F);
        AIMING.put(player.getUUID(), player.level().getGameTime());
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MartialArts.TYRANTS_THRUST_MAX_CHARGE + 20, 2, false, false));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 0.6F);
        return true;
    }

    /** Let go (or cast at once, uncharged): the thrust and its beam. */
    static boolean release(ServerPlayer player, PlayerPatch<?> patch) {
        Long since = AIMING.remove(player.getUUID());
        float charge = since == null ? 0 : Mth.clamp((player.level().getGameTime() - since) / (float) MartialArts.TYRANTS_THRUST_MAX_CHARGE, 0, 1);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        boolean cauldrons = player.getHealth() <= player.getMaxHealth() * CAULDRONS_HEALTH;
        patch.playAnimationSynchronized(MurimAnimations.OVERLORD_TYRANT_RELEASE, 0.0F);
        if (cauldrons) discover(player);
        MartialCombat.later(player, ticks(MurimAnimations.OVERLORD_RELEASE_HIT), () -> beam(player, charge, cauldrons));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
        return true;
    }

    /** The first time, Breaking the Cauldrons is revealed. */
    private static void discover(ServerPlayer player) {
        MartialData data = MartialService.data(player);
        if (data.hasTechnique(MartialArts.BREAKING_THE_CAULDRONS.id())) return;
        MartialService.set(player, data.unlock(MartialArts.BREAKING_THE_CAULDRONS));
        player.sendSystemMessage(Component.translatable("technique.murimblock.breaking_the_cauldrons.discovered").withStyle(net.minecraft.ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
    }

    /** The beam: through all in its line, the ground cracked under it, its lance of light shown along it. */
    static void beam(ServerPlayer player, float charge, boolean cauldrons) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, Mth.clamp(look.y, -0.35, 0.35), look.z).normalize();
        double length = cauldrons ? BEAM_MAX * 1.5 : BEAM_MIN + (BEAM_MAX - BEAM_MIN) * charge;
        double width = cauldrons ? CAULDRONS_WIDTH : BEAM_WIDTH;
        Vec3 from = player.position().add(0, 1.2, 0).add(dir.scale(0.8));
        // Stopped by what is solid in its way.
        var hit = level.clip(new ClipContext(from, from.add(dir.scale(length)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) length = hit.getLocation().distanceTo(from);
        float damage = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, BEAM_SHARE * (cauldrons ? CAULDRONS_FACTOR : 1));
        int struck = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from.add(dir.scale(length))).inflate(width / 2 + 1),
                e -> e != player && e.isAlive())) {
            Vec3 c = e.getBoundingBox().getCenter().subtract(from);
            double along = c.dot(dir);
            if (along < -0.5 || along > length + 0.5 || c.subtract(dir.scale(along)).length() > width / 2 + e.getBbWidth() / 2) continue;
            if (cauldrons) MartialCombat.breakGuard(e);
            if (blow(player, e, damage)) { push(e, dir.scale(0.8), 0.15); struck++; }
        }
        if (struck > 0) MartialProgress.addMastery(player, art(), MartialArts.TYRANTS_THRUST.id(), 1);
        // The lance of light, the cracks under it, the flash.
        OverlordQiEntity qi = MurimEntities.OVERLORD_QI.get().create(level);
        if (qi != null) {
            qi.setPos(from.x, from.y, from.z);
            qi.shape(OverlordQiEntity.BEAM, (float) width, 1.0F, (float) length, 3, turnTo(dir), 14);
            level.addFreshEntity(qi);
        }
        for (double s = 1; s < length; s += 1.0) {
            Vec3 p = from.add(dir.scale(s));
            level.sendParticles(new DustParticleOptions(new Vector3f(0.9F, 0.15F, 0.1F), 1.6F), p.x, p.y, p.z, 3, width * 0.25, width * 0.25, width * 0.25, 0);
            if (cauldrons || s % 2 < 1) crack(level, BlockPos.containing(p.x, player.getY() - 0.5, p.z));
        }
        level.sendParticles(ParticleTypes.FLASH, from.x, from.y, from.z, 1, 0, 0, 0, 0);
        level.playSound(null, BlockPos.containing(from), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, cauldrons ? 1.6F : 1.0F, cauldrons ? 0.5F : 0.9F);
        OverlordFx.flash(level, from.add(dir.scale(length / 2)), cauldrons ? 16 : 10);
    }

    // ---------------------------------------------------------------- 2. Earth-Splitting Sweep

    /** The spear meets the ground: a ring of cracks round the body. The ring's blow is the animation's (OVERLORD_RING). */
    static void sweepSlam(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos centre = player.blockPosition().below();
        int r = (int) SWEEP_RING;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d < SWEEP_RING - 1.2 || d > SWEEP_RING + 0.3) continue;
            if ((x * 7 + z * 13) % 3 == 0) continue;
            crack(level, centre.offset(x, 0, z));
        }
        BlockState under = level.getBlockState(centre);
        if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), player.getX(), player.getY() + 0.2, player.getZ(), 120, SWEEP_RING * 0.7, 0.2, SWEEP_RING * 0.7, 0.3);
        OverlordFx.shock(level, player.position(), SWEEP_RING + 0.5);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.1F, 0.7F);
        OverlordFx.flash(level, player.position().add(0, 0.5, 0), 10);
    }

    // ---------------------------------------------------------------- 3. Falling Mountain, and the ultimate's flight state

    private static final class Flight {
        int ticks;
    }

    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    /** Up and toward the target (8 blocks at most), the spear over the head. */
    static boolean leap(ServerPlayer player, PlayerPatch<?> patch) {
        MartialCombat.expect(player, MurimAnimations.OVERLORD_FALLING_LEAP, MartialArts.FALLING_MOUNTAIN.id(), false);
        patch.playAnimationSynchronized(MurimAnimations.OVERLORD_FALLING_LEAP, 0.0F);
        LivingEntity target = AzureCloudSword.targetAhead(player, LEAP_REACH);
        Vec3 to = target != null ? target.position() : player.position().add(flatLook(player).scale(LEAP_REACH * 0.6));
        Vec3 flat = to.subtract(player.position()).multiply(1, 0, 1);
        if (flat.length() > LEAP_REACH) flat = flat.normalize().scale(LEAP_REACH);
        // A jump of LEAP_HEIGHT that lands `flat` away: up speed for the height, across speed for the time it stays up.
        double g = 0.08, vy = Math.sqrt(2 * g * LEAP_HEIGHT) * 1.05, air = 2 * vy / g;
        Vec3 across = flat.scale(1 / air);
        MartialCombat.later(player, 3, () -> {
            player.setDeltaMovement(across.x, vy, across.z);
            player.hurtMarked = true;
            BlockState under = player.serverLevel().getBlockState(player.blockPosition().below());
            if (!under.isAir()) player.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), player.getX(), player.getY() + 0.1, player.getZ(), 30, 0.6, 0.1, 0.6, 0.15);
        });
        MartialCombat.noFallFor(player, 200);
        FLIGHTS.put(player.getUUID(), new Flight());
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.3F);
        return true;
    }

    /** The landing: the spear planted, a crater, the blow all round, the bodies thrown out. */
    static void land(ServerPlayer player) {
        FLIGHTS.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(MurimAnimations.OVERLORD_FALLING_PLANT, 0.0F);
        BlockPos ground = player.blockPosition().below();
        BlockState state = level.getBlockState(ground);
        ShatteredGround.crater(level, ground, MOUNTAIN_HOLE, 1);
        float core = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, MOUNTAIN_CORE_SHARE);
        float edge = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, MOUNTAIN_SHARE);
        double reach = MOUNTAIN_CRATER + 0.5;
        int struck = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach, 3, reach), e -> e != player && e.isAlive())) {
            double d = Math.sqrt(e.distanceToSqr(player));
            if (d > reach) continue;
            if (blow(player, e, d <= MOUNTAIN_CORE ? core : edge)) {
                Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
                Vec3 out = away.lengthSqr() < 1e-4 ? flatLook(player) : away.normalize();
                double k = 1 - d / reach;
                push(e, out.scale(0.6 + k), 0.4 + 0.4 * k);
                struck++;
            }
        }
        if (struck > 0) MartialProgress.addMastery(player, art(), MartialArts.FALLING_MOUNTAIN.id(), 1);
        if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), player.getX(), player.getY() + 0.2, player.getZ(), 160, MOUNTAIN_CRATER * 0.6, 0.3, MOUNTAIN_CRATER * 0.6, 0.35);
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.4, player.getZ(), 1, 0, 0, 0, 0);
        OverlordFx.shock(level, player.position(), reach);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6F, 0.6F);
        OverlordFx.flash(level, player.position().add(0, 0.5, 0), 14);
    }

    /** Tests: Falling Mountain lands here now (a mock player does not fall by itself on the server). */
    public static void landForTests(ServerPlayer player) {
        land(player);
    }

    public static boolean flying(Player player) {
        return FLIGHTS.containsKey(player.getUUID());
    }

    // ---------------------------------------------------------------- 4. Overlord's Charge

    private static final class Charge {
        final List<LivingEntity> carried = new ArrayList<>();
        boolean crushed;
    }

    private static final Map<UUID, Charge> CHARGES = new HashMap<>();

    /** One tick of the charge: the bodies before the point picked up and carried on it; a wall ends it, crushing them. */
    static void chargeTick(ServerPlayer player) {
        Charge c = CHARGES.get(player.getUUID());
        if (c == null || c.crushed) return;
        ServerLevel level = player.serverLevel();
        Vec3 dir = flatLook(player);
        Vec3 point = player.position().add(dir.scale(1.8));
        if (c.carried.size() < CHARGE_CARRY) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(1.1, 1.5, 1.1).move(0, 1, 0),
                    e -> e != player && e.isAlive() && !c.carried.contains(e) && MartialCombat.isEnemy(e, player))) {
                if (c.carried.size() >= CHARGE_CARRY) break;
                c.carried.add(e);
            }
        }
        c.carried.removeIf(e -> !e.isAlive());
        for (int i = 0; i < c.carried.size(); i++) {
            LivingEntity e = c.carried.get(i);
            Vec3 at = player.position().add(dir.scale(1.9 + i * 0.5));
            carryTo(e, new Vec3(at.x, player.getY(), at.z));
        }
        // A wall before the body: the charge stops there, the carried crushed into it.
        BlockPos ahead = BlockPos.containing(player.position().add(dir.scale(2.6 + c.carried.size() * 0.5)).add(0, 1, 0));
        if (player.horizontalCollision || !level.getBlockState(ahead).getCollisionShape(level, ahead).isEmpty() && !c.carried.isEmpty()) crush(player, c, dir, ahead);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.3, 0.05, 0.3, 0.02);
        crack(level, player.blockPosition().below());
    }

    /** Into the wall: the carried take the crush, stunned; the wall's face bursts. */
    private static void crush(ServerPlayer player, Charge c, Vec3 dir, BlockPos wall) {
        c.crushed = true;
        ServerLevel level = player.serverLevel();
        float damage = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, CRUSH_SHARE);
        for (LivingEntity e : c.carried) {
            e.invulnerableTime = 0;
            if (blow(player, e, damage)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
            }
        }
        for (int w = -1; w <= 1; w++) for (int y = 0; y <= 1; y++) {
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            ShatteredGround.smash(level, BlockPos.containing(Vec3.atCenterOf(wall).add(side.scale(w)).add(0, y - 0.5, 0)));
        }
        BlockState state = level.getBlockState(wall);
        if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), wall.getX() + 0.5, wall.getY() + 0.5, wall.getZ() + 0.5, 60, 0.6, 0.6, 0.6, 0.2);
        OverlordFx.spark(level, Vec3.atCenterOf(wall).add(0, -0.5, 0), 2.0);
        level.playSound(null, wall, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.3F, 0.8F);
        if (!c.carried.isEmpty()) MartialProgress.addMastery(player, art(), MartialArts.OVERLORDS_CHARGE.id(), 1);
        OverlordFx.flash(level, Vec3.atCenterOf(wall), 10);
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE, 0.1F);
        CHARGES.remove(player.getUUID());
    }

    /** The end of the run: no wall met, the carried thrown off the point. */
    static void chargeEnd(ServerPlayer player, boolean forced) {
        Charge c = CHARGES.remove(player.getUUID());
        if (c == null || c.crushed) return;
        Vec3 dir = flatLook(player);
        for (LivingEntity e : c.carried) push(e, dir.scale(1.4), 0.35);
    }

    // ---------------------------------------------------------------- 5. Hundred Battles Rampage

    /** A thrust lands: its little burst of Qi at the point (the last one, a cone that throws). */
    static void burst(ServerPlayer player, boolean last) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = flatLook(player), tip = tip(player, 3.0);
        float damage = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, BURST_SHARE * (last ? 3 : 1));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(tip, tip).inflate(last ? 2.2 : BURST_REACH),
                e -> e != player && e.isAlive())) {
            e.invulnerableTime = 0;
            if (blow(player, e, damage) && last) push(e, dir.scale(1.6), 0.4);
        }
        if (last) level.sendParticles(ParticleTypes.LAVA, tip.x, tip.y, tip.z, 3, 0.3, 0.3, 0.3, 0);
        level.sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, last ? 30 : 8, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, BlockPos.containing(tip), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, last ? 1.2F : 0.35F, last ? 0.8F : 1.6F);
        if (last) { OverlordFx.flash(level, tip, 10); OverlordFx.spark(level, tip.add(0, -1.0, 0), 2.5); }
    }

    // ---------------------------------------------------------------- the ultimate: Conqueror Descends

    private static final class Ult {
        final Vec3 target;
        final OverlordQiEntity spear;
        int ticks;

        Ult(Vec3 target, OverlordQiEntity spear) {
            this.target = target;
            this.spear = spear;
        }
    }

    private static final Map<UUID, Ult> ULTS = new HashMap<>();

    /** The spear raised to the sky: the giant spear appears high over the place aimed at, a red circle warns, it falls. */
    static boolean descend(ServerPlayer player, PlayerPatch<?> patch) {
        ServerLevel level = player.serverLevel();
        Vec3 target = aim(player);
        patch.playAnimationSynchronized(MurimAnimations.OVERLORD_ASCEND, 0.0F);
        OverlordQiEntity spear = MurimEntities.OVERLORD_QI.get().create(level);
        if (spear != null) {
            spear.setPos(target.x, target.y + ULT_HEIGHT + ULT_LENGTH, target.z);
            // Point down: turned half round about x.
            spear.shape(OverlordQiEntity.SPEAR, 1.0F, ULT_LENGTH * 0.2F, ULT_LENGTH, 20, new Quaternionf().rotateX((float) Math.PI), 400);
            level.addFreshEntity(spear);
        }
        ULTS.put(player.getUUID(), new Ult(target, spear));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, false));
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, BlockPos.containing(target), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.5F);
        player.displayClientMessage(Component.translatable("technique.murimblock.conqueror_descends.called").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD), true);
        return true;
    }

    /** Where the giant spear falls: where the player looks, on the ground, 20 blocks off at most. */
    static Vec3 aim(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(80));
        var hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? player.position().add(flatLook(player).scale(ULT_REACH)) : hit.getLocation();
        Vec3 flat = at.subtract(player.position()).multiply(1, 0, 1);
        if (flat.length() > ULT_REACH) at = player.position().add(flat.normalize().scale(ULT_REACH));
        int top = player.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z));
        return new Vec3(at.x, top, at.z);
    }

    private static void ultTick(ServerPlayer player, Ult u) {
        ServerLevel level = player.serverLevel();
        u.ticks++;
        int call = ticks(MurimAnimations.OVERLORD_ASCEND_CALL);
        if (u.ticks < call) {
            // The warning: a red circle on the ground, embers rising from it.
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24 + u.ticks * 0.05;
                level.sendParticles(new DustParticleOptions(new Vector3f(0.95F, 0.1F, 0.05F), 2.0F), u.target.x + Math.cos(a) * ULT_RADIUS * 0.6,
                        u.target.y + 0.15, u.target.z + Math.sin(a) * ULT_RADIUS * 0.6, 1, 0, 0, 0, 0);
            }
            if (u.ticks % 4 == 0) level.sendParticles(ParticleTypes.LAVA, u.target.x, u.target.y + 0.2, u.target.z, 2, ULT_RADIUS * 0.3, 0, ULT_RADIUS * 0.3, 0);
            return;
        }
        // The fall: the spear brought down point first in a few ticks.
        int fall = u.ticks - call;
        if (u.spear != null && u.spear.isAlive()) {
            double k = Math.min(1.0, (fall + 1) / (double) ULT_FALL_TICKS);
            double y = u.target.y + ULT_LENGTH * 0.85 + (ULT_HEIGHT + ULT_LENGTH * 0.15) * (1 - k * k);
            u.spear.setPos(u.target.x, y, u.target.z);
            level.sendParticles(ParticleTypes.FLAME, u.target.x, y - ULT_LENGTH, u.target.z, 20, 0.8, 2.0, 0.8, 0.05);
        }
        if (fall + 1 >= ULT_FALL_TICKS) {
            ULTS.remove(player.getUUID());
            impact(player, u);
        }
    }

    /** The impact: the blast, the crater, all thrown; the spear stays planted a moment, then fades. */
    private static void impact(ServerPlayer player, Ult u) {
        ServerLevel level = player.serverLevel();
        BlockPos ground = BlockPos.containing(u.target.x, u.target.y - 1, u.target.z);
        BlockState state = level.getBlockState(ground);
        ShatteredGround.crater(level, ground, ULT_RADIUS * 0.6, ULT_CRATER_DEPTH);
        float core = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, ULT_CORE_SHARE);
        float edge = TechniquePower.damage(player, MartialArts.OVERLORD_SPEAR, ULT_SHARE);
        int struck = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(u.target, u.target).inflate(ULT_RADIUS, 6, ULT_RADIUS),
                e -> e != player && e.isAlive())) {
            double d = Math.sqrt(e.distanceToSqr(u.target));
            if (d > ULT_RADIUS) continue;
            if (blow(player, e, d <= ULT_CORE ? core : edge)) {
                Vec3 away = e.position().subtract(u.target).multiply(1, 0, 1);
                Vec3 out = away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : away.normalize();
                double k = 1 - d / ULT_RADIUS;
                push(e, out.scale(0.8 + 1.4 * k), 0.6 + 0.6 * k);
                e.igniteForSeconds(3);
                struck++;
            }
        }
        if (struck > 0) MartialProgress.addMastery(player, art(), MartialArts.CONQUEROR_DESCENDS.id(), 1);
        if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), u.target.x, u.target.y + 0.5, u.target.z, 400, ULT_RADIUS * 0.5, 0.6, ULT_RADIUS * 0.5, 0.5);
        level.sendParticles(ParticleTypes.LAVA, u.target.x, u.target.y + 0.5, u.target.z, 4, ULT_RADIUS * 0.3, 0.2, ULT_RADIUS * 0.3, 0);
        level.sendParticles(ParticleTypes.FLAME, u.target.x, u.target.y + 0.5, u.target.z, 300, ULT_RADIUS * 0.5, 0.4, ULT_RADIUS * 0.5, 0.15);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, u.target.x, u.target.y + 1, u.target.z, 30, ULT_RADIUS * 0.3, 0.5, ULT_RADIUS * 0.3, 0.02);
        OverlordFx.shock(level, u.target, ULT_RADIUS);
        level.playSound(null, ground, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0F, 0.4F);
        level.playSound(null, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F, 0.6F);
        OverlordFx.flash(level, u.target.add(0, 2, 0), 30);
        if (u.spear != null && u.spear.isAlive()) u.spear.liveFor(30);
    }

    /** Tests: the ultimate's giant spear falls now. */
    public static void impactForTests(ServerPlayer player) {
        Ult u = ULTS.remove(player.getUUID());
        if (u != null) impact(player, u);
    }

    public static boolean calling(Player player) {
        return ULTS.containsKey(player.getUUID());
    }

    // ---------------------------------------------------------------- hits

    /** What one of the art's hits does besides its damage. */
    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean enemy) {
        if (isStrike(animation)) {
            strikeLanded(player, target, enemy);
            return;
        }
        if (MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target)) return;
        if (animation == MurimAnimations.OVERLORD_EARTH_SWEEP) {
            // Thrown up into the air, then down stunned.
            double resist = Math.max(0.4, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            for (int i = 0; i < LIFT_STEPS.length; i++) {
                Vec3 step = new Vec3(0, LIFT_STEPS[i] * resist, 0);
                MartialCombat.later(player, i + 1, () -> carryBy(target, step));
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
        }
    }

    /** A blow of the art that is not an animation's: damage from the player, a guard raised in time stops it. */
    static boolean blow(ServerPlayer player, LivingEntity e, float damage) {
        if (!MartialCombat.isEnemy(e, player) || MurimGuardSkill.parriedWithin(e, 6)) return false;
        boolean hurt = e.hurt(player.damageSources().playerAttack(player), damage);
        if (hurt) e.hurtMarked = true;
        return hurt;
    }

    /** The living in a line from the body, `length` long, `width` wide. */
    private static List<LivingEntity> inLine(ServerPlayer player, Vec3 dir, double length, double width) {
        Vec3 from = player.position();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from.add(dir.scale(length))).inflate(width, 2, width),
                e -> e != player && e.isAlive())) {
            Vec3 c = e.position().subtract(from);
            double along = c.dot(dir);
            if (along < 0 || along > length || c.subtract(dir.scale(along)).multiply(1, 0, 1).length() > width) continue;
            out.add(e);
        }
        return out;
    }

    /** Where the ground is struck without a hole: its dust thrown up (the cracks themselves are drawn: OverlordFx). */
    private static void crack(ServerLevel level, BlockPos ground) {
        BlockState state = level.getBlockState(ground);
        if (state.isAir()) return;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), ground.getX() + 0.5, ground.getY() + 1.05, ground.getZ() + 0.5, 6, 0.35, 0.05, 0.35, 0.12);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot()).normalize();
    }

    /** A point `reach` blocks ahead of the body at the height of the spear. */
    private static Vec3 tip(ServerPlayer player, double reach) {
        return player.position().add(0, 1.1, 0).add(flatLook(player).scale(reach));
    }

    /** The turn that points +Y along `dir`. */
    private static Quaternionf turnTo(Vec3 dir) {
        return new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f((float) dir.x, (float) dir.y, (float) dir.z));
    }

    /** A push given to a body (a player's by its velocity sent to them). */
    private static void push(LivingEntity e, Vec3 across, double up) {
        double resist = Math.max(0.2, 1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        e.push(across.x * resist, up * resist, across.z * resist);
        e.hurtMarked = true;
    }

    /** A body moved by `step` (a mob through its collisions; a player by a teleport), its fall forgiven. */
    private static void carryBy(LivingEntity target, Vec3 step) {
        if (!target.isAlive()) return;
        if (target instanceof ServerPlayer other) {
            if (!other.level().noCollision(other, other.getBoundingBox().move(step))) return;
            Vec3 at = other.position().add(step);
            other.connection.teleport(at.x, at.y, at.z, other.getYRot(), other.getXRot());
        } else {
            target.move(net.minecraft.world.entity.MoverType.SELF, step);
        }
        target.setDeltaMovement(0, 0, 0);
        target.resetFallDistance();
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    /** A body carried on the point to this place. */
    private static void carryTo(LivingEntity target, Vec3 at) {
        carryBy(target, at.subtract(target.position()));
    }

    // ---------------------------------------------------------------- the finish: Overlord's Verdict

    /** The fallen lifted on the point, held, then driven into the ground, a ring of cracks round them. */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        ServerLevel level = executor.serverLevel();
        if (elapsed > 6 && elapsed <= FINISH_LIFT_TICK) {
            carryBy(victim, new Vec3(0, 1.6 / (FINISH_LIFT_TICK - 6), 0));
            level.sendParticles(ParticleTypes.SMALL_FLAME, victim.getX(), victim.getY() + 0.5, victim.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
        }
        if (elapsed == FINISH_HIT_TICK) {
            carryBy(victim, new Vec3(0, -(victim.getY() - executor.getY()), 0));
            BlockState under = level.getBlockState(victim.blockPosition().below());
            if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), victim.getX(), victim.getY() + 0.2, victim.getZ(), 140, 1.2, 0.2, 1.2, 0.3);
            level.sendParticles(ParticleTypes.LAVA, victim.getX(), victim.getY() + 0.3, victim.getZ(), 1, 0, 0, 0, 0);
            ShatteredGround.crater(level, victim.blockPosition().below(), 1.8, 1);
            OverlordFx.shock(level, victim.position(), 4);
            level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6F, 0.6F);
            OverlordFx.flash(level, victim.position().add(0, 0.5, 0), 14);
        }
    }

    // ---------------------------------------------------------------- ticks and state

    /** Every tick of a player: the charge given up, the leap's landing, the ultimate. */
    static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        Long since = AIMING.get(id);
        if (since != null && (!holdsSpear(player) || !player.isAlive() || player.level().getGameTime() - since > MartialArts.TYRANTS_THRUST_MAX_CHARGE + 40)) {
            AIMING.remove(id);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        } else if (since != null && player.tickCount % 3 == 0) {
            Vec3 tip = tip(player, 1.8);
            player.serverLevel().sendParticles(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 2, 0.15, 0.15, 0.15, 0.02);
        }
        Flight f = FLIGHTS.get(id);
        if (f != null) {
            f.ticks++;
            if (!player.isAlive() || f.ticks > 160) FLIGHTS.remove(id);
            else {
                player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
                if (f.ticks > 8 && (player.onGround() || player.isInWater())) land(player);
            }
        }
        Ult u = ULTS.get(id);
        if (u != null) ultTick(player, u);
    }

    static void forget(@Nullable UUID player) {
        if (player == null) {
            AIMING.clear(); FLIGHTS.clear(); CHARGES.clear(); DOMINATION.clear();
            ULTS.values().forEach(u -> { if (u.spear != null) u.spear.discard(); });
            ULTS.clear();
            return;
        }
        AIMING.remove(player);
        FLIGHTS.remove(player);
        CHARGES.remove(player);
        DOMINATION.remove(player);
        Ult u = ULTS.remove(player);
        if (u != null && u.spear != null) u.spear.discard();
    }
}
