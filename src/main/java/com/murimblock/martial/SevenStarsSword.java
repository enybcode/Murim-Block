package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.network.SevenStarsPayloads;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.dawn_day.gameasset.animation.DawnDayAnimations;
import net.dawn_day.world.capabilities.item.DawnDayWeaponCategories;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Seven Stars Sword in play (docs/combat/styles/seven-stars-sword.md), an art of the florett only. Built one piece at
 * a time with Enzo (17/10/2026: "les skills un par un, pour vraiment tout contrôler"), then all of it ("fais tout l'art
 * martial en gardant le niveau graphique du passif"). Here its passive and its first technique; the others, its secret,
 * its ultimate and its finish are in {@link SevenStarsMoves}:
 * <ul>
 *   <li><b>Star Marks</b>: each florett strike that lands (Dawn Day's four thrusts) puts a star on its target, seven at
 *   most; a perfect parry puts two on the attacker. Seven make a full constellation: the next blow bursts it, a
 *   {@link #BURST_FACTOR} harder and throwing the target off balance, and the stars fall back to none;</li>
 *   <li>the stars go out one by one once the target has not been struck for {@link #FADE_AFTER} ticks, and the player
 *   loses {@link #HURT_LOSS} on their last target each time they are hurt;</li>
 *   <li>the florett's strikes are {@link #STRIKE_SPEED} quicker in the stance;</li>
 *   <li><b>Shooting Star</b>: crouched, then gone in a streak of light {@link #STREAK} blocks ahead ({@link #STREAK_EVOLVED}
 *   evolved: Twin Comets, which may go again once, free, within {@link #RECAST_TICKS}); every enemy on the way is cut
 *   ({@link #STREAK_SHARE} of the attack) and takes a star, one already at seven bursts as it is passed. From the air the
 *   streak dives to the ground.</li>
 * </ul>
 * A target carries one player's stars at a time: another player's star takes their place. The clients are told every
 * change (SevenStarsPayloads) and draw the stars over the target (client/SevenStarsFx).
 */
public final class SevenStarsSword {
    public static final int MAX_STARS = 7;
    /** The blow that bursts a full constellation strikes this much harder. */
    public static final float BURST_FACTOR = 1.6F;
    /** Stars a perfect parry puts on the attacker; stars lost on the last target when hurt. */
    public static final int PARRY_STARS = 2, HURT_LOSS = 2;
    /** After this long without a blow (4 s) the stars go out, one every {@link #FADE_EVERY} ticks. */
    public static final int FADE_AFTER = 80, FADE_EVERY = 20;
    /** The florett's strikes are this much quicker (attack speed) in the stance. */
    public static final double STRIKE_SPEED = 0.10;
    private static final ResourceLocation QUICK = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "star_marks_speed");

    /** Shooting Star: how far the streak goes (evolved), how wide it cuts, its share, when it leaves (ticks into its clip). */
    public static final double STREAK = 8, STREAK_EVOLVED = 12, STREAK_REACH = 1.0;
    public static final float STREAK_SHARE = 1.3F;
    public static final int STREAK_TICK = 3;
    /** Twin Comets: the second streak, free, within this many ticks of the first. */
    public static final int RECAST_TICKS = 40;

    private SevenStarsSword() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.SEVEN_STARS_SWORD);
    }

    /** A florett in the main hand: one of Dawn Day's, or any weapon Epic Fight sees as one. */
    public static boolean holdsFlorett(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        return cap != null && cap.getWeaponCategory() == DawnDayWeaponCategories.FLORETT;
    }

    /** Whether this player fights with Star Marks: the art is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.SEVEN_STARS_SWORD::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.SEVEN_STARS_SWORD::equals).isPresent();
    }

    /** In the stance, a florett in hand. */
    public static boolean active(Player player) {
        return stanceActive(player) && holdsFlorett(player);
    }

    // ---------------------------------------------------------------- swings

    /** Dawn Day's florett strikes: its four thrusts. */
    static boolean isStrike(@Nullable AnimationAccessor<? extends StaticAnimation> a) {
        return a != null && (a == DawnDayAnimations.FLORETT_AUTO1 || a == DawnDayAnimations.FLORETT_AUTO2
                || a == DawnDayAnimations.FLORETT_AUTO3 || a == DawnDayAnimations.FLORETT_AUTO4);
    }

    /** What an animation counts for in this art: a strike of the passive (in the stance, a florett in hand). */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (isStrike(a)) return active(player) ? MartialArts.STAR_MARKS.id() : null;
        return null;
    }

    // ---------------------------------------------------------------- the stars

    /** The stars on a target: whose, how many, when it was last struck, when one last went out. */
    private static final class Marks {
        final LivingEntity target;
        final UUID owner;
        int count;
        long last, faded;

        Marks(LivingEntity target, UUID owner, long now) {
            this.target = target;
            this.owner = owner;
            this.last = now;
            this.faded = now;
        }
    }

    private static final Map<UUID, Marks> MARKS = new HashMap<>();
    /** The targets whose constellation the hit under way bursts (set before the damage, read after it). */
    private static final Set<UUID> BURSTING = new HashSet<>();

    /** The stars this player has on this target (0 if none, or another's). */
    public static int stars(Player player, LivingEntity target) {
        Marks m = MARKS.get(target.getUUID());
        return m != null && m.owner.equals(player.getUUID()) ? m.count : 0;
    }

    /** Puts `n` stars on a target (seven at most); another player's stars there are replaced. */
    public static void addStars(ServerPlayer player, LivingEntity target, int n) {
        if (n <= 0 || !target.isAlive() || target == player) return;
        long now = player.level().getGameTime();
        Marks m = MARKS.get(target.getUUID());
        if (m == null || !m.owner.equals(player.getUUID())) {
            m = new Marks(target, player.getUUID(), now);
            MARKS.put(target.getUUID(), m);
        }
        int before = m.count;
        m.count = Math.min(MAX_STARS, m.count + n);
        m.last = now;
        m.faded = now;
        if (m.count != before) {
            sync(m);
            if (m.count == MAX_STARS) player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            else player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.2F + 0.1F * m.count);
        }
    }

    /** Sets the stars on a target outright (tests, the secret). */
    public static void setStars(ServerPlayer player, LivingEntity target, int n) {
        Marks m = MARKS.get(target.getUUID());
        if (m != null && m.owner.equals(player.getUUID())) { m.count = 0; }
        if (n <= 0) { clear(target); return; }
        addStars(player, target, n);
    }

    /** The constellation bursts: the stars fall to none, the target is thrown off balance, the clients see it break. */
    static void burst(ServerPlayer player, LivingEntity target) {
        Marks m = MARKS.remove(target.getUUID());
        boolean full = m != null && m.count >= MAX_STARS;
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        if (patch != null) patch.applyStun(StunType.NEUTRALIZE, 0.8F);
        ServerLevel level = player.serverLevel();
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.4F, 0.8F);
        level.playSound(null, target.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.4F);
        if (m != null) sync(m.target, m.owner, 0);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new SevenStarsPayloads.Burst(target.getId()));
        // A full constellation burst counts for Seven Points, then Seven Swords (a Constellation Link on fewer does not).
        if (full && MartialCombat.isEnemy(target, player)) {
            MartialProgress.count(player, MartialArts.SEVEN_POINTS_BURSTS, 1);
            MartialProgress.count(player, MartialArts.SEVEN_SWORDS_BURSTS, 1);
        }
    }

    private static void clear(LivingEntity target) {
        Marks m = MARKS.remove(target.getUUID());
        if (m != null) sync(m.target, m.owner, 0);
    }

    private static void sync(Marks m) {
        sync(m.target, m.owner, m.count);
    }

    private static void sync(LivingEntity target, UUID owner, int count) {
        if (target.level().isClientSide) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new SevenStarsPayloads.Marks(target.getId(), owner, count));
    }

    // ---------------------------------------------------------------- techniques

    /** Starts one of the art's techniques; a florett in hand is checked before. */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (technique == MartialArts.SHOOTING_STAR) {
            patch.playAnimationSynchronized(MurimAnimations.SSS_SHOOTING_STAR, 0.0F);
            boolean evolved = MartialService.data(player).evolved(art(), technique.id());
            boolean air = !player.onGround();
            MartialCombat.later(player, STREAK_TICK, () -> streak(player, evolved ? STREAK_EVOLVED : STREAK, air));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
            return true;
        }
        return SevenStarsMoves.perform(player, patch, technique);
    }

    /** When each player's Twin Comets may go again, free (game time). */
    private static final Map<UUID, Long> RECAST = new HashMap<>();

    /** Whether this player may send Twin Comets' second streak now, free (once). */
    static boolean recastOpen(ServerPlayer player) {
        Long until = RECAST.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    static void takeRecast(ServerPlayer player) {
        RECAST.remove(player.getUUID());
    }

    static void openRecast(ServerPlayer player) {
        RECAST.put(player.getUUID(), player.level().getGameTime() + RECAST_TICKS);
    }

    /**
     * The streak: the body goes straight ahead (from the air, diving to the ground) as far as nothing stops it, up one
     * step if a block is in the way; every enemy within reach of the line is cut and marked.
     */
    public static void streak(ServerPlayer player, double length, boolean air) {
        if (!player.isAlive()) return;
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 dir = air ? new Vec3(look.x, Math.min(-0.35, look.y), look.z).normalize() : new Vec3(look.x, 0, look.z).normalize();
        if (!Double.isFinite(dir.x) || dir.lengthSqr() < 1e-6) dir = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 start = player.position(), end = start;
        AABB body = player.getBoundingBox();
        double lift = 0;
        for (double d = 0.25; d <= length + 1e-6; d += 0.25) {
            Vec3 p = start.add(dir.scale(d)).add(0, lift, 0);
            if (!level.noCollision(player, body.move(p.subtract(start)))) {
                // A step up; otherwise the streak ends against the wall.
                Vec3 up = p.add(0, 1.05, 0);
                if (air || !level.noCollision(player, body.move(up.subtract(start)))) break;
                lift += 1.05;
                p = up;
            }
            end = p;
        }
        // On the ground: down onto the ground where it ends (a slope, a step down).
        if (!air) for (int k = 0; k < 3 && level.noCollision(player, body.move(end.subtract(start).add(0, -0.5, 0))); k++) end = end.add(0, -0.5, 0);
        List<Integer> hits = new ArrayList<>();
        Vec3 a = start.add(0, 0.9, 0), b = end.add(0, 0.9, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(STREAK_REACH + 1, 2, STREAK_REACH + 1),
                e -> e != player && e.isAlive())) {
            if (distanceToSegment(e.getBoundingBox().getCenter(), a, b) > STREAK_REACH + e.getBbWidth() / 2) continue;
            if (blow(player, e, STREAK_SHARE)) hits.add(e.getId());
        }
        player.teleportTo(end.x, end.y, end.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        MartialCombat.noFallFor(player, 40);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SevenStarsPayloads.Streak(player.getId(),
                new Vector3f((float) start.x, (float) start.y, (float) start.z), new Vector3f((float) end.x, (float) end.y, (float) end.z), hits));
        level.playSound(null, BlockPos.containing(end), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9F, 1.6F);
        if (!hits.isEmpty()) MartialProgress.addMastery(player, art(), MartialArts.SHOOTING_STAR.id(), 1);
    }

    /** Distance from a point to the segment a-b. */
    static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /**
     * A blow of a technique that is not an animation's hit: damage from the player (the art's share of the attack), a
     * guard raised in time stops it; on a full constellation it bursts it, harder, else one more star.
     */
    static boolean blow(ServerPlayer player, LivingEntity e, float share) {
        if (!MartialCombat.isEnemy(e, player) || MurimGuardSkill.parriedWithin(e, 6)) return false;
        boolean full = stars(player, e) >= MAX_STARS;
        float damage = TechniquePower.damage(player, MartialArts.SEVEN_STARS_SWORD, share) * (full ? BURST_FACTOR : 1);
        // Each blow of a technique lands on its own (the bounds and the swords come faster than a body's grace time).
        e.invulnerableTime = 0;
        if (!e.hurt(player.damageSources().playerAttack(player), damage)) return false;
        e.hurtMarked = true;
        if (full) burst(player, e);
        else addStars(player, e, 1);
        return true;
    }

    // ---------------------------------------------------------------- hits

    /** Before a hit of the art: a full constellation on its target makes this the bursting blow. */
    static float factorBefore(ServerPlayer player, LivingEntity target) {
        float riposte = 1.0F;
        if (SevenStarsMoves.riposte(player, target)) { RIPOSTING.add(target.getUUID()); riposte = SevenStarsMoves.POLARIS_FACTOR; }
        if (stars(player, target) < MAX_STARS) return riposte;
        BURSTING.add(target.getUUID());
        return BURST_FACTOR * riposte;
    }

    /** The targets the hit under way is Polaris Riposte on. */
    private static final Set<UUID> RIPOSTING = new HashSet<>();

    /** After a hit of the art: the constellation bursts if it was full, else one more star. */
    static void onHit(ServerPlayer player, LivingEntity target) {
        if (BURSTING.remove(target.getUUID())) burst(player, target);
        // The art's own florett: its first blow on a target puts two stars.
        else addStars(player, target, stars(player, target) == 0 && player.getMainHandItem().is(SevenStarsItems.FLORETT.get()) ? 2 : 1);
        if (RIPOSTING.remove(target.getUUID())) SevenStarsMoves.riposted(player, target);
        SevenStarsMoves.struck(player, target);
    }

    /** A perfect parry with the stance: two stars on the attacker. */
    static void onPerfectParry(ServerPlayer player, Entity attacker) {
        if (!active(player)) return;
        if (attacker instanceof LivingEntity living) addStars(player, living, PARRY_STARS);
        SevenStarsMoves.parried(player, attacker);
    }

    /** A player of the art is hurt: they lose two stars on their last target. */
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0) return;
        Marks last = null;
        for (Marks m : MARKS.values()) if (m.owner.equals(player.getUUID()) && (last == null || m.last > last.last)) last = m;
        if (last == null) return;
        last.count -= HURT_LOSS;
        if (last.count <= 0) clear(last.target);
        else sync(last);
    }

    // ---------------------------------------------------------------- ticks

    /** Every tick of a player: the florett's quicker strikes in the stance. */
    static void tick(ServerPlayer player) {
        SevenStarsMoves.tick(player);
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) return;
        boolean quick = active(player);
        if (quick && !speed.hasModifier(QUICK))
            speed.addTransientModifier(new AttributeModifier(QUICK, STRIKE_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!quick && speed.hasModifier(QUICK)) speed.removeModifier(QUICK);
    }

    /** Every server tick: stars on the dead or gone forgotten, those left alone too long go out one by one. */
    public static void serverTick(MinecraftServer server) {
        for (Iterator<Marks> it = MARKS.values().iterator(); it.hasNext(); ) {
            Marks m = it.next();
            if (!m.target.isAlive() || m.target.isRemoved()) { it.remove(); continue; }
            long now = m.target.level().getGameTime();
            if (now - m.last < FADE_AFTER || now - m.faded < FADE_EVERY) continue;
            m.faded = now;
            m.count--;
            if (m.count <= 0) {
                it.remove();
                sync(m.target, m.owner, 0);
            } else {
                sync(m);
            }
        }
    }

    static void forget(@Nullable UUID player) {
        SevenStarsMoves.forget(player);
        if (player == null) { MARKS.clear(); BURSTING.clear(); RIPOSTING.clear(); RECAST.clear(); return; }
        MARKS.values().removeIf(m -> m.owner.equals(player));
        RECAST.remove(player);
    }

    /** A player coming into sight of a target is told the stars it carries. */
    public static void startTracking(Entity target, ServerPlayer watcher) {
        Marks m = MARKS.get(target.getUUID());
        if (m != null && m.count > 0) PacketDistributor.sendToPlayer(watcher, new SevenStarsPayloads.Marks(target.getId(), m.owner, m.count));
    }

    /** For tests: the stars the patch of a given hit adds, as if a florett strike landed. */
    public static void strikeForTests(ServerPlayer player, LivingEntity target) {
        if (factorBefore(player, target) != 1.0F) BURSTING.add(target.getUUID());
        onHit(player, target);
    }

    static boolean busy(ServerPlayer player) {
        return EpicFightBridge.isBusy(player);
    }
}
