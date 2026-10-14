package com.murimblock.martial;

import com.murimblock.network.TechniquePayloads;
import com.murimblock.qi.QiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.entity.DealDamageEvent;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The fight of a martial art on the server, around Epic Fight's own hits (docs/combat/STYLES.md, "Combos et contre-jeu"):
 * <ul>
 *   <li><b>the passive's parry</b>: a perfect parry with Flowing Cloud Stance gives back Qi and opens a riposte window,
 *   in which the next technique takes its riposte variant for half its Qi;</li>
 *   <li><b>swings</b>: each animation of an art that starts is one swing; its hits give the passive's Qi, mastery and
 *   quest progress once per swing, and its effects (lifting, breaking a guard);</li>
 *   <li><b>bounded combos</b>: each hit of an art on the same target soon after the last loses 8 % damage, down to 40 %;
 *   a knock-down ends the combo (and, after three hits or more, counts as a complete combo for the quest);</li>
 *   <li>small timed actions of the techniques (a leap, a wave sent at the thrust), run on the player's tick.</li>
 * </ul>
 * Epic Fight deals every melee hit; this only scales them and reacts to them.
 */
public final class MartialCombat {
    /** How long after a perfect parry the next technique is a riposte (1.5 s). */
    public static final int RIPOSTE_TICKS = 30;
    /** Cloud Step Slash within this many ticks of a perfect parry is Swallow Returns (a quarter second). */
    public static final int SWALLOW_TICKS = 5;
    /** Qi given back by a perfect parry and by a strike that lands (Breath of the Blade). */
    public static final double PARRY_QI = 6, STRIKE_QI = 1;
    /** How long a guard stays broken after a riposte's Thousand Drizzle (2 s). */
    public static final int GUARD_BREAK_TICKS = 40;
    /** A hit more than this many ticks after the last starts a new combo. */
    public static final int COMBO_GAP_TICKS = 30;
    /** Damage lost per hit already in the combo, and the least a hit still deals. */
    public static final float COMBO_DECAY = 0.08F, COMBO_FLOOR = 0.4F;
    /** Hits before the knock-down for a combo to count as complete (quest). */
    public static final int COMPLETE_COMBO_HITS = 3;
    /** Thousand-Li Cloud Step: a second dash within this many ticks of the first is free. */
    public static final int SECOND_STEP_TICKS = 30;

    /** The perfect parry a player may riposte to: whom, and when (game time). */
    public record Riposte(UUID attacker, long tick) { }

    /** One animation of an art being played: what its hits count for, and what they already counted for. */
    private static final class Swing {
        final AnimationAccessor<? extends StaticAnimation> animation;
        final ResourceLocation credit;
        final boolean strike, riposte;
        final Set<UUID> struck = new HashSet<>();
        boolean credited;

        Swing(AnimationAccessor<? extends StaticAnimation> animation, ResourceLocation credit, boolean strike, boolean riposte) {
            this.animation = animation;
            this.credit = credit;
            this.strike = strike;
            this.riposte = riposte;
        }
    }

    private record Combo(UUID attacker, int hits, long last) { }

    private record Scheduled(long at, Runnable action) { }

    private static final Map<UUID, Riposte> RIPOSTES = new HashMap<>();
    private static final Map<UUID, Long> GUARD_BROKEN = new HashMap<>();
    private static final Map<UUID, Swing> PENDING = new HashMap<>();
    private static final Map<UUID, Swing> SWINGS = new HashMap<>();
    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    private static final Map<UUID, List<Scheduled>> SCHEDULED = new HashMap<>();
    private static final Map<UUID, Long> SECOND_STEP = new HashMap<>();
    private static final Map<UUID, Long> NO_FALL = new HashMap<>();

    private MartialCombat() {
    }

    /** Whether a hit counts for Qi, mastery and quests: a living hostile mob or another player, not an animal or the air. */
    public static boolean isEnemy(@Nullable Entity target, Entity attacker) {
        return target != attacker && target instanceof LivingEntity living && living.isAlive()
                && (target instanceof Enemy || target instanceof Player);
    }

    // ---------------------------------------------------------------- parry and riposte

    /** A perfect parry (MurimGuardSkill): the quest counts it; with Flowing Cloud Stance, Qi back and a riposte window. */
    public static void onPerfectParry(ServerPlayer player, Entity attacker) {
        if (isEnemy(attacker, player)) MartialProgress.count(player, MartialArts.PATIENT_BLADE_PARRIES, 1);
        if (!AzureCloudSword.stanceActive(player)) return;
        QiService.addQi(player, PARRY_QI);
        RIPOSTES.put(player.getUUID(), new Riposte(attacker.getUUID(), player.level().getGameTime()));
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new TechniquePayloads.Riposte(RIPOSTE_TICKS));
    }

    /** The riposte this player may still make, or null. */
    @Nullable public static Riposte riposte(ServerPlayer player) {
        Riposte r = RIPOSTES.get(player.getUUID());
        if (r != null && player.level().getGameTime() - r.tick > RIPOSTE_TICKS) {
            RIPOSTES.remove(player.getUUID());
            return null;
        }
        return r;
    }

    static void consumeRiposte(ServerPlayer player) {
        RIPOSTES.remove(player.getUUID());
    }

    /** A guard broken by a riposte cannot parry for a while. */
    public static boolean guardBroken(LivingEntity entity) {
        Long until = GUARD_BROKEN.get(entity.getUUID());
        return until != null && entity.level().getGameTime() < until;
    }

    /** Breaks a guard for {@link #GUARD_BREAK_TICKS} (the last thrust of a riposte's Thousand Drizzle), staggering its holder. */
    public static void breakGuard(LivingEntity target) {
        GUARD_BROKEN.put(target.getUUID(), target.level().getGameTime() + GUARD_BREAK_TICKS);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        if (patch != null) patch.applyStun(StunType.NEUTRALIZE, 0.8F);
    }

    // ---------------------------------------------------------------- Thousand-Li Cloud Step

    static void openSecondStep(ServerPlayer player) {
        SECOND_STEP.put(player.getUUID(), player.level().getGameTime() + SECOND_STEP_TICKS);
    }

    /** Takes the free second dash, if its window is open (it can be taken once). */
    static boolean takeSecondStep(ServerPlayer player) {
        Long until = SECOND_STEP.remove(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    static boolean secondStepOpen(ServerPlayer player) {
        Long until = SECOND_STEP.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    // ---------------------------------------------------------------- swings

    /** The next animation started for this player is this technique's (and a riposte or not). */
    static void expect(ServerPlayer player, AnimationAccessor<? extends StaticAnimation> animation, ResourceLocation technique, boolean riposte) {
        PENDING.put(player.getUUID(), new Swing(animation, technique, false, riposte));
    }

    /** An animation starts: one of an art's opens a swing (a technique as expected, or a strike of the passive). */
    static void onAnimationBegin(ServerPlayer player, AnimationAccessor<? extends StaticAnimation> animation) {
        Swing pending = PENDING.remove(player.getUUID());
        if (pending != null && pending.animation == animation) {
            SWINGS.put(player.getUUID(), pending);
            return;
        }
        ResourceLocation credit = AzureCloudSword.creditOf(animation);
        if (credit == null) {
            SWINGS.remove(player.getUUID());
            return;
        }
        SWINGS.put(player.getUUID(), new Swing(animation, credit, AzureCloudSword.isStrike(animation), false));
    }

    @Nullable private static Swing swing(ServerPlayer player, @Nullable AnimationAccessor<? extends StaticAnimation> animation) {
        Swing s = SWINGS.get(player.getUUID());
        return s != null && animation != null && s.animation == animation ? s : null;
    }

    /** Before an art's hit: the combo it continues takes its share off the damage. */
    static void onDealDamagePre(DealDamageEvent.Pre event) {
        if (!(event.getEntityPatch().getOriginal() instanceof ServerPlayer player)) return;
        // A body hanging at the top of the crescent is not knocked down by the attacks placed up there (a knocked-down
        // body cannot be hit: the slam would miss it); it staggers instead.
        if (isHeldUp(event.getTarget()) && event.getDamageSource().getStunType() == yesman.epicfight.world.damagesource.StunType.KNOCKDOWN)
            event.getDamageSource().setStunType(yesman.epicfight.world.damagesource.StunType.LONG);
        if (swing(player, event.getDamageSource().getAnimation()) == null) return;
        Combo c = COMBOS.get(event.getTarget().getUUID());
        int prior = c != null && c.attacker.equals(player.getUUID()) && player.level().getGameTime() - c.last <= COMBO_GAP_TICKS ? c.hits : 0;
        float factor = comboFactor(prior);
        // Epic Fight keeps the modifiers attached to the damage source (not the event's modified damage).
        if (factor < 1.0F) event.getDamageSource().attachDamageModifier(ValueModifier.multiplier(factor));
    }

    /** Share of its damage a hit keeps after `prior` hits of the same combo. */
    public static float comboFactor(int prior) {
        return Math.max(COMBO_FLOOR, 1.0F - COMBO_DECAY * prior);
    }

    /** After an art's hit: its effect, then what it counts for. */
    static void onDealDamagePost(DealDamageEvent.Post event) {
        if (!(event.getEntityPatch().getOriginal() instanceof ServerPlayer player)) return;
        AnimationAccessor<? extends StaticAnimation> animation = event.getDamageSource().getAnimation();
        Swing swing = swing(player, animation);
        if (swing == null) return;
        LivingEntity target = event.getTarget();
        AzureCloudSword.onHit(player, target, animation, swing.riposte);
        boolean enemy = isEnemy(target, player);
        // The combo: a knock-down ends it, and counts as complete after enough hits.
        long now = player.level().getGameTime();
        Combo c = COMBOS.get(target.getUUID());
        int prior = c != null && c.attacker.equals(player.getUUID()) && now - c.last <= COMBO_GAP_TICKS ? c.hits : 0;
        if (AzureCloudSword.knocksDown(animation)) {
            COMBOS.remove(target.getUUID());
            if (enemy && prior >= COMPLETE_COMBO_HITS) MartialProgress.count(player, MartialArts.SEA_OF_CLOUDS_COMBOS, 1);
        } else {
            COMBOS.put(target.getUUID(), new Combo(player.getUUID(), prior + 1, now));
        }
        if (!enemy) return;
        MartialArts.Art art = MartialArts.art(MartialArts.AZURE_CLOUD_SWORD);
        if (swing.credit.equals(MartialArts.CLOUD_STEP_SLASH.id()) && swing.struck.add(target.getUUID()))
            MartialProgress.count(player, MartialArts.PATIENT_BLADE_DASH_HITS, 1);
        if (swing.credited) return;
        swing.credited = true;
        if (swing.strike) {
            // Breath of the Blade: a strike that lands gives back Qi.
            QiService.addQi(player, STRIKE_QI);
            MartialProgress.strikeLanded(player, art);
        } else {
            MartialProgress.addMastery(player, art, swing.credit, 1);
        }
    }

    /** A blade of Qi of a technique hit something: counts for the technique's mastery like its own hits (once per blade). */
    static void qiBladeHit(ServerPlayer owner, LivingEntity target, ResourceLocation technique) {
        if (!isEnemy(target, owner)) return;
        MartialArts.Art art = MartialArts.art(MartialArts.AZURE_CLOUD_SWORD);
        MartialProgress.addMastery(owner, art, technique, 1);
    }

    /** Sends a body up (a launcher), less if it resists knock-back; its horizontal drift is mostly stopped. */
    static void lift(LivingEntity target, double up) {
        double resist = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double v = up * Math.max(0, 1 - resist);
        if (v <= 0.05) return;
        Vec3 m = target.getDeltaMovement();
        target.setDeltaMovement(m.x * 0.3, Math.max(m.y, v), m.z * 0.3);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    static void slowFall(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, false));
    }

    /** A body held in front of a player who carries it up (the crescent): where, relative to them, and until when. */
    private record Carried(LivingEntity target, Vec3 offset, long until, boolean riposte) { }

    private static final Map<UUID, List<Carried>> CARRIED = new HashMap<>();

    /** A player's leap: when, from what height, at what upward speed. */
    private record Leap(long tick, double baseY, double speed) { }

    private static final Map<UUID, Leap> LEAPS = new HashMap<>();

    /**
     * Sends a player up (the crescent's leap). Their client moves them and reports where they are, a ping later; the
     * server keeps the leap to place a carried body where the player is now, not where they were (see {@link #heightAt}).
     */
    static void leap(ServerPlayer player, double speed) {
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(m.x * 0.2, speed, m.z * 0.2);
        player.hurtMarked = true;
        LEAPS.put(player.getUUID(), new Leap(player.level().getGameTime(), player.getY(), speed));
    }

    /**
     * How high a leap has taken a player `ticks` after it: Minecraft's own physics for a player (move, then gravity 0.08
     * and drag 0.98 each tick), held at the top.
     */
    public static double riseAfter(double speed, long ticks) {
        double rise = 0, v = speed;
        for (long i = 0; i < ticks && v > 0; i++) {
            rise += v;
            v = (v - 0.08) * 0.98;
        }
        return rise;
    }

    /** Where a player stands for the server: their reported position, or ahead of it during a leap (the report lags a ping). */
    private static double heightAt(ServerPlayer player, long now) {
        Leap l = LEAPS.get(player.getUUID());
        if (l == null) return player.getY();
        // The client starts moving a tick after the server sent the leap.
        return Math.max(player.getY(), l.baseY + riseAfter(l.speed, Math.max(0, now - l.tick - 1)));
    }

    /**
     * Takes a body up with the player for `ticks`: every tick it is put back in front of them, at their height, as
     * they rise. Then it is let go to fall (as a riposte, slowly, for the follow-up). Not a mount: nothing to escape.
     */
    static void carry(ServerPlayer player, LivingEntity target, int ticks, boolean riposte) {
        if (target == player || target.isPassenger() || target.isVehicle()) return;
        Vec3 offset = target.position().subtract(player.position()).multiply(1, 0, 1);
        double d = offset.length();
        // In front of the player, close enough to keep cutting it in the air.
        if (d < 1e-3) offset = Vec3.directionFromRotation(0, player.getYRot());
        offset = offset.normalize().scale(Math.max(0.9, Math.min(1.6, d)));
        List<Carried> list = CARRIED.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        list.removeIf(c -> c.target == target);
        list.add(new Carried(target, offset, player.level().getGameTime() + ticks, riposte));
    }

    /**
     * The top of the crescent's rise: the player and the body they carried hang there (no gravity for the player, the
     * body held before them) for {@link #HOVER_TICKS}, time for other attacks; the crescent's key again brings the slam
     * (the window then holds a little longer, until the slam's cut lands). Then both come down.
     */
    private record Hover(LivingEntity target, Vec3 offset, long until, boolean riposte, boolean slamming) { }

    private static final Map<UUID, Hover> HOVERS = new HashMap<>();
    /** How long the two hang at the top (ticks: 1.5 s), and how long the slam's cut may take to land. */
    public static final int HOVER_TICKS = 30, SLAM_CUT_TICKS = 10;

    static void hover(ServerPlayer player, LivingEntity target, Vec3 offset, boolean riposte) {
        HOVERS.put(player.getUUID(), new Hover(target, offset, player.level().getGameTime() + HOVER_TICKS, riposte, false));
        player.setNoGravity(true);
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        // A clear note: the slam is ready on the crescent's key.
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.4F);
    }

    /** Whether this body hangs at the top of a player's crescent. */
    public static boolean isHeldUp(LivingEntity target) {
        for (Hover h : HOVERS.values()) if (h.target == target) return true;
        return false;
    }

    /** Whether this player hangs at the top of the crescent with a body, the slam ready on the crescent's key. */
    public static boolean hovering(Player player) {
        Hover h = HOVERS.get(player.getUUID());
        return h != null && !h.slamming;
    }

    /** The crescent's key again at the top: the slam starts (false if not hanging there). */
    static boolean startSlam(ServerPlayer player) {
        Hover h = HOVERS.get(player.getUUID());
        if (h == null || h.slamming) return false;
        HOVERS.put(player.getUUID(), new Hover(h.target, h.offset, player.level().getGameTime() + SLAM_CUT_TICKS, h.riposte, true));
        return true;
    }

    /** The hanging ends: the player falls again (slowly), the body is let go if it was not thrown down. */
    private static void endHover(ServerPlayer player, boolean letGo) {
        Hover h = HOVERS.remove(player.getUUID());
        player.setNoGravity(false);
        slowFall(player, 30);
        // Seven blocks up: the way down is forgiven.
        noFallFor(player, 100);
        if (h != null && letGo && h.target.isAlive()) {
            h.target.setDeltaMovement(0, 0, 0);
            h.target.hurtMarked = true;
            if (h.riposte) slowFall(h.target, 30);
        }
    }

    private static void tickHover(ServerPlayer player, long now) {
        Hover h = HOVERS.get(player.getUUID());
        if (h == null) return;
        LivingEntity t = h.target;
        if (!player.isAlive() || !t.isAlive() || t.level() != player.level() || now > h.until) {
            endHover(player, true);
            return;
        }
        Vec3 m = player.getDeltaMovement();
        if (m.y != 0) {
            player.setDeltaMovement(m.x, 0, m.z);
            player.hurtMarked = true;
        }
        hold(t, player.position().add(h.offset));
    }

    /** Puts a body at a place (a player by a teleport), still, its fall forgiven. */
    private static void hold(LivingEntity t, Vec3 at) {
        if (!t.level().noCollision(t, t.getBoundingBox().move(at.subtract(t.position())))) at = t.position();
        if (t instanceof ServerPlayer other) other.connection.teleport(at.x, at.y, at.z, other.getYRot(), other.getXRot());
        else t.moveTo(at.x, at.y, at.z, t.getYRot(), t.getXRot());
        t.setDeltaMovement(0, 0, 0);
        t.resetFallDistance();
        t.hasImpulse = true;
        t.hurtMarked = true;
    }

    /** A body the crescent's slam threw down, and since when (game time). */
    private record Slam(LivingEntity target, long since) { }

    private static final Map<UUID, List<Slam>> SLAMS = new HashMap<>();
    /** Downward speed the slam gives (blocks per tick), and how long a thrown body may fall before it counts as landed. */
    static final double SLAM_SPEED = 1.6;
    static final int SLAM_MAX_TICKS = 30;

    /** The crescent's slam hit: the body goes straight down, fast, from the next tick (tickSlams), to its landing. */
    static void slamDown(ServerPlayer player, LivingEntity target) {
        target.removeEffect(MobEffects.SLOW_FALLING);
        Hover h = HOVERS.get(player.getUUID());
        if (h != null && h.target == target) endHover(player, false);
        // The player dives with it, the fall forgiven.
        player.removeEffect(MobEffects.SLOW_FALLING);
        noFallFor(player, 80);
        List<Slam> list = SLAMS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        list.removeIf(s -> s.target == target);
        list.add(new Slam(target, player.level().getGameTime()));
    }

    /** Whether this body is falling from the crescent's slam. */
    public static boolean isSlammed(LivingEntity target) {
        for (List<Slam> list : SLAMS.values()) for (Slam s : list) if (s.target == target) return true;
        return false;
    }

    /** Bodies thrown down by this player's slam: on the ground (or stopped), the landing; gone or too long, forgotten. */
    private static void tickSlams(ServerPlayer player, long now) {
        List<Slam> list = SLAMS.get(player.getUUID());
        if (list == null) return;
        for (Iterator<Slam> it = list.iterator(); it.hasNext(); ) {
            Slam s = it.next();
            LivingEntity t = s.target;
            long falling = now - s.since;
            if (!t.isAlive() || t.level() != player.level() || falling > SLAM_MAX_TICKS || t.isPassenger()) {
                it.remove();
                continue;
            }
            // The player dives after it.
            if (!player.onGround()) {
                player.setDeltaMovement(0, -SLAM_SPEED, 0);
                player.hurtMarked = true;
            }
            // Driven down like the carry (every body falls the same, players and still mobs alike), to the ground.
            Vec3 at = t.position();
            double speed = SLAM_SPEED * Math.max(0.3, 1 - t.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            BlockHitResult ground = t.level().clip(new ClipContext(at, at.subtract(0, speed + 0.05, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, t));
            boolean landed = ground.getType() == HitResult.Type.BLOCK || t.onGround();
            Vec3 to = ground.getType() == HitResult.Type.BLOCK ? new Vec3(at.x, ground.getLocation().y, at.z) : at.subtract(0, speed, 0);
            if (t instanceof ServerPlayer other) other.connection.teleport(to.x, to.y, to.z, other.getYRot(), other.getXRot());
            else t.moveTo(to.x, to.y, to.z, t.getYRot(), t.getXRot());
            t.setDeltaMovement(0, landed ? 0 : -speed, 0);
            t.resetFallDistance();
            t.hasImpulse = true;
            t.hurtMarked = true;
            if (landed) {
                it.remove();
                AzureCloudSword.slamLanding(player, t);
            }
        }
        if (list.isEmpty()) SLAMS.remove(player.getUUID());
    }

    /** Whether this body is being carried up by a player's crescent. */
    public static boolean isCarriedUp(LivingEntity target) {
        for (List<Carried> list : CARRIED.values()) for (Carried c : list) if (c.target == target) return true;
        return false;
    }

    private static void tickCarried(ServerPlayer player, long now) {
        Leap leap = LEAPS.get(player.getUUID());
        if (leap != null && now - leap.tick > 60) LEAPS.remove(player.getUUID());
        List<Carried> list = CARRIED.get(player.getUUID());
        if (list == null) return;
        for (Iterator<Carried> it = list.iterator(); it.hasNext(); ) {
            Carried c = it.next();
            LivingEntity t = c.target;
            if (!t.isAlive() || t.level() != player.level() || now > c.until || !player.isAlive()) {
                it.remove();
                // At the top, together: both hang there a while (the crescent's key again brings the slam).
                if (now > c.until && t.isAlive() && player.isAlive() && t.level() == player.level()) {
                    hover(player, t, c.offset, c.riposte);
                } else if (t.isAlive()) {
                    t.setDeltaMovement(0, 0, 0);
                    t.hurtMarked = true;
                    if (c.riposte) slowFall(t, 30);
                }
                continue;
            }
            Vec3 at = new Vec3(player.getX(), heightAt(player, now), player.getZ()).add(c.offset);
            // Not into a ceiling: it stays where it was.
            if (!t.level().noCollision(t, t.getBoundingBox().move(at.subtract(t.position())))) at = t.position();
            if (t instanceof ServerPlayer other) {
                other.connection.teleport(at.x, at.y, at.z, other.getYRot(), other.getXRot());
            } else {
                t.moveTo(at.x, at.y, at.z, t.getYRot(), t.getXRot());
            }
            t.setDeltaMovement(0, Math.max(0, player.getDeltaMovement().y), 0);
            t.resetFallDistance();
            t.hasImpulse = true;
            t.hurtMarked = true;
        }
        if (list.isEmpty()) CARRIED.remove(player.getUUID());
    }

    // ---------------------------------------------------------------- timed actions

    /** Runs an action on this player's tick, `ticks` from now (cancelled if they leave or die). */
    static void later(ServerPlayer player, int ticks, Runnable action) {
        SCHEDULED.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new Scheduled(player.level().getGameTime() + ticks, action));
    }

    /** No fall damage for this player for a while (a leap or a dive of the art). */
    static void noFallFor(ServerPlayer player, int ticks) {
        NO_FALL.put(player.getUUID(), player.level().getGameTime() + ticks);
        player.resetFallDistance();
    }

    /** Server tick of a player: their timed actions, and their fall forgiven during a leap. */
    public static void onPlayerTick(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long safe = NO_FALL.get(player.getUUID());
        if (safe != null) {
            if (now > safe) NO_FALL.remove(player.getUUID());
            else player.resetFallDistance();
        }
        tickCarried(player, now);
        tickHover(player, now);
        tickSlams(player, now);
        AzureCloudSword.tickAim(player, now);
        List<Scheduled> list = SCHEDULED.get(player.getUUID());
        if (list == null) return;
        List<Runnable> due = new ArrayList<>();
        for (Iterator<Scheduled> it = list.iterator(); it.hasNext(); ) {
            Scheduled s = it.next();
            if (s.at <= now) {
                due.add(s.action);
                it.remove();
            }
        }
        if (list.isEmpty()) SCHEDULED.remove(player.getUUID());
        if (player.isAlive()) due.forEach(Runnable::run);
    }

    // ---------------------------------------------------------------- hooks

    public static void registerHooks() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(AzureCloudSword::onDetonate);
        EpicFightEventHooks.Animation.BEGIN.registerEvent(event -> {
            if (event.getEntityPatch().getOriginal() instanceof ServerPlayer player) onAnimationBegin(player, event.getAnimation());
        }, "murimblock:martial_swing", 100);
        EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE.registerEvent(MartialCombat::onDealDamagePre, "murimblock:martial_combo", 100);
        EpicFightEventHooks.Entity.DELIVER_DAMAGE_POST.registerEvent(MartialCombat::onDealDamagePost, "murimblock:martial_hit", 100);
    }

    /** Forgets a player leaving or dying: their riposte, swings, timed actions. */
    public static void forget(ServerPlayer player) {
        UUID id = player.getUUID();
        if (HOVERS.remove(id) != null) player.setNoGravity(false);
        RIPOSTES.remove(id);
        PENDING.remove(id);
        SWINGS.remove(id);
        SCHEDULED.remove(id);
        SECOND_STEP.remove(id);
        NO_FALL.remove(id);
        GUARD_BROKEN.remove(id);
        CARRIED.remove(id);
        LEAPS.remove(id);
        SLAMS.remove(id);
        AzureCloudSword.forgetAim(id);
    }

    public static void clearForTests() {
        RIPOSTES.clear();
        GUARD_BROKEN.clear();
        PENDING.clear();
        SWINGS.clear();
        COMBOS.clear();
        SCHEDULED.clear();
        SECOND_STEP.clear();
        NO_FALL.clear();
        CARRIED.clear();
        LEAPS.clear();
        SLAMS.clear();
        HOVERS.clear();
        AzureCloudSword.forgetAim(null);
    }
}
