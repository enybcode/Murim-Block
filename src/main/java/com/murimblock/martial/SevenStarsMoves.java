package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.network.SevenStarsPayloads;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Seven Stars Sword's techniques after Shooting Star (docs/combat/styles/seven-stars-sword.md), its secret, its
 * ultimate and its finish; every blow through {@link SevenStarsSword#blow} (stars put, constellations burst). Each tells
 * the clients what to draw (SevenStarsPayloads.Fx, client/SevenStarsFx):
 * <ul>
 *   <li><b>Dipper Steps</b>: three bounds round a target 4.5 blocks off at most (a fourth if it carries four stars or
 *   more; five, and the fourth, evolved: Full Dipper), a thrust at each, ending behind it; no blow in melee lands on the
 *   player meanwhile;</li>
 *   <li><b>Orbiting Stars</b>: seven stars round the player for 6 s; each stops a blow or a projectile and strikes back at
 *   its author, marking it; those left at the end fly to the nearest marked enemies;</li>
 *   <li><b>Constellation Link</b>: every enemy marked within 12 blocks linked, then all their stars burst together, a
 *   quarter of the attack a star; each burst takes a second off Shooting Star;</li>
 *   <li><b>Falling Star Rain</b>: a circle where the player looks (12 blocks off at most), seven falling stars into it
 *   (nine evolved: Meteor Shower), the marked first; one reaching seven under the rain bursts by itself;</li>
 *   <li><b>Polaris Riposte</b>, the secret: a florett strike right after a perfect parry, on the attacker, a fifth harder,
 *   completes its constellation; found after thirty perfect parries in the art;</li>
 *   <li><b>Seven Stars Sword Array</b>, the ultimate: seven swords of light round a target held still, striking one after
 *   the other, then all together, harder for each star it carries;</li>
 *   <li><b>Extinguished Stars</b>, the finish: seven thrusts, a star lit over the body at each, all put out at once.</li>
 * </ul>
 */
public final class SevenStarsMoves {
    // ---- Dipper Steps
    public static final double DIPPER_REACH = 4.5, DIPPER_RADIUS = 1.7;
    public static final int DIPPER_GAP = 6, DIPPER_HOPS = 3, DIPPER_HOPS_EVOLVED = 5, DIPPER_EXTRA_AT = 4;
    public static final float DIPPER_SHARE = 0.6F;
    /** Walk the Dipper: a blow on the target within this long of the last bound counts. */
    public static final int WALK_WINDOW = 40;
    // ---- Orbiting Stars
    public static final int ORBIT_STARS = 7, ORBIT_TICKS = 120;
    public static final float ORBIT_SHARE = 0.4F;
    public static final double RELEASE_REACH = 12;
    // ---- Constellation Link
    public static final double LINK_REACH = 12;
    public static final float LINK_SHARE = 0.25F;
    public static final int LINK_AT = 12, LINK_REFUND = 20, BIND_GROUP = 3;
    // ---- Falling Star Rain
    public static final double RAIN_REACH = 12, RAIN_RADIUS = 2.5, RAIN_HIT = 1.3, RAIN_HEIGHT = 18;
    public static final int RAIN_FIRST = 14, RAIN_GAP = 4, RAIN_FALL = 6, RAIN_STARS = 7, RAIN_STARS_EVOLVED = 9;
    public static final float RAIN_SHARE = 0.35F, RAIN_BURST_SHARE = 0.6F;
    // ---- Polaris Riposte
    public static final int POLARIS_WINDOW = 20, POLARIS_PARRIES = 30;
    public static final float POLARIS_FACTOR = 1.2F;
    // ---- Seven Stars Sword Array
    public static final double ARRAY_REACH = 10, ARRAY_SPREAD = 1.05;
    public static final int ARRAY_RISE = 10, ARRAY_FIRST = 20, ARRAY_GAP = 4, ARRAY_FINAL = 52;
    public static final float ARRAY_SHARE = 0.4F, ARRAY_FINAL_SHARE = 2.0F, ARRAY_PER_STAR = 0.15F;
    // ---- Extinguished Stars (ticks into the finish)
    public static final int[] FINISH_STABS = {7, 10, 13, 16, 19, 22, 25};
    public static final int FINISH_OUT = 38, FINISH_HIT_TICK = 38;

    /** The Big Dipper: its seven stars from the handle's end to the bowl (units), as on the clients. */
    public static final float[][] DIPPER = {{-2.2F, 0.1F}, {-1.2F, 0.35F}, {-0.4F, 0.4F}, {0.4F, 0.4F}, {1.9F, 1.0F}, {2.0F, -0.1F}, {0.8F, -0.6F}};

    private SevenStarsMoves() {
    }

    // ---------------------------------------------------------------- targets

    /**
     * The enemy a technique goes for: the one the player looks at within `reach`, else the nearest within it in front of
     * them (null if none).
     */
    @Nullable static LivingEntity target(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(reach));
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach), e -> MartialCombat.isEnemy(e, player))) {
            var hit = e.getBoundingBox().inflate(0.5).clip(eye, end);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bestD) { bestD = eye.distanceToSqr(hit.get()); best = e; }
        }
        if (best != null) return best;
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach), e -> MartialCombat.isEnemy(e, player))) {
            Vec3 to = e.position().subtract(player.position());
            double d = to.length();
            if (d > reach || (d > 1 && new Vec3(to.x, 0, to.z).normalize().dot(flat) < 0.4)) continue;
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    /** The enemies within `reach` that carry this player's stars, the nearest first. */
    static List<LivingEntity> marked(ServerPlayer player, double reach) {
        List<LivingEntity> out = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach),
                e -> MartialCombat.isEnemy(e, player) && e.distanceTo(player) <= reach && SevenStarsSword.stars(player, e) > 0));
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return out;
    }

    /** Whether a technique has something to go for (else it is not cast, and costs nothing). */
    public static boolean hasTarget(ServerPlayer player, MartialArts.Technique technique) {
        if (technique == MartialArts.DIPPER_STEPS) return target(player, DIPPER_REACH) != null;
        if (technique == MartialArts.SEVEN_STARS_ARRAY) return target(player, ARRAY_REACH) != null;
        if (technique == MartialArts.CONSTELLATION_LINK) return !marked(player, LINK_REACH).isEmpty();
        return true;
    }

    private static void fx(ServerPlayer player, int kind, int target, List<Vec3> points, List<Integer> ids, int value) {
        List<Vector3f> p = new ArrayList<>();
        for (Vec3 v : points) p.add(new Vector3f((float) v.x, (float) v.y, (float) v.z));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SevenStarsPayloads.Fx(kind, player.getId(), target, p, ids, value));
    }

    // ---------------------------------------------------------------- techniques

    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        boolean evolved = MartialService.data(player).evolved(SevenStarsSword.art(), technique.id());
        if (technique == MartialArts.DIPPER_STEPS) return dipperSteps(player, patch, evolved);
        if (technique == MartialArts.ORBITING_STARS) return orbit(player, patch);
        if (technique == MartialArts.CONSTELLATION_LINK) return link(player, patch);
        if (technique == MartialArts.FALLING_STAR_RAIN) return rain(player, patch, evolved);
        if (technique == MartialArts.SEVEN_STARS_ARRAY) return array(player, patch);
        return false;
    }

    // ---------------------------------------------------------------- Dipper Steps

    /** Until when each player bounds (no blow in melee lands then), and the target their last steps ended behind. */
    private static final Map<UUID, Long> DIPPING = new HashMap<>();
    private record Walk(UUID target, long until) { }
    private static final Map<UUID, Walk> WALKS = new HashMap<>();

    static boolean dipperSteps(ServerPlayer player, PlayerPatch<?> patch, boolean evolved) {
        LivingEntity t = target(player, DIPPER_REACH);
        if (t == null) return false;
        ServerLevel level = player.serverLevel();
        int hops = (evolved ? DIPPER_HOPS_EVOLVED : DIPPER_HOPS) + (SevenStarsSword.stars(player, t) >= DIPPER_EXTRA_AT ? 1 : 0);
        boolean air = !player.onGround();
        Vec3 c = t.position();
        double a0 = Math.atan2(player.getZ() - c.z, player.getX() - c.x);
        List<Vec3> spots = new ArrayList<>();
        Vec3 last = player.position();
        for (int k = 0; k < hops; k++) {
            double a = a0 + Math.PI * (k + 1) / hops;
            Vec3 spot = null;
            for (double r : new double[] {DIPPER_RADIUS, 1.2})
                for (double dy : new double[] {0, 1, -1}) {
                    Vec3 p = new Vec3(c.x + Math.cos(a) * r, c.y + dy, c.z + Math.sin(a) * r);
                    if (spot == null && level.noCollision(player, player.getBoundingBox().move(p.subtract(player.position())))) spot = p;
                }
            last = spot != null ? spot : last;
            spots.add(last);
        }
        long now = level.getGameTime();
        DIPPING.put(player.getUUID(), now + (long) hops * DIPPER_GAP + 4);
        for (int k = 0; k < hops; k++) {
            int n = k;
            MartialCombat.later(player, k * DIPPER_GAP, () -> {
                if (!t.isAlive()) return;
                Vec3 at = spots.get(n);
                float yaw = (float) (Mth.atan2(t.getZ() - at.z, t.getX() - at.x) * Mth.RAD_TO_DEG) - 90;
                player.teleportTo(level, at.x, at.y, at.z, yaw, 5);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                patch.playAnimationSynchronized(MurimAnimations.SSS_DIPPER_THRUST, 0.0F);
                level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.0F + 0.15F * n);
            });
            MartialCombat.later(player, k * DIPPER_GAP + 2, () -> {
                if (!t.isAlive()) return;
                SevenStarsSword.blow(player, t, DIPPER_SHARE);
                if (n == hops - 1 && air) { t.setDeltaMovement(0, -1.2, 0); t.hurtMarked = true; }
            });
        }
        MartialCombat.later(player, hops * DIPPER_GAP, () -> {
            WALKS.put(player.getUUID(), new Walk(t.getUUID(), level.getGameTime() + WALK_WINDOW));
            MartialProgress.addMastery(player, SevenStarsSword.art(), MartialArts.DIPPER_STEPS.id(), 1);
        });
        MartialCombat.noFallFor(player, hops * DIPPER_GAP + 40);
        fx(player, SevenStarsPayloads.Fx.STEPS, t.getId(), spots, List.of(), DIPPER_GAP);
        return true;
    }

    /** A florett blow landed: right after Dipper Steps, on the target it ended behind, Walk the Dipper counts it. */
    static void struck(ServerPlayer player, LivingEntity target) {
        Walk w = WALKS.get(player.getUUID());
        if (w == null || !w.target.equals(target.getUUID())) return;
        WALKS.remove(player.getUUID());
        if (player.level().getGameTime() <= w.until) MartialProgress.count(player, MartialArts.WALK_THE_DIPPER_FINISHES, 1);
    }

    public static boolean dipping(ServerPlayer player) {
        Long until = DIPPING.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    // ---------------------------------------------------------------- Orbiting Stars

    private static final class Orbit {
        int count;
        final long until;

        Orbit(int count, long until) {
            this.count = count;
            this.until = until;
        }
    }

    private static final Map<UUID, Orbit> ORBITS = new HashMap<>();
    private static final Map<UUID, Long> ORBIT_BLOCKED = new HashMap<>();

    static boolean orbit(ServerPlayer player, PlayerPatch<?> patch) {
        patch.playAnimationSynchronized(MurimAnimations.SSS_ORBIT, 0.0F);
        ORBITS.put(player.getUUID(), new Orbit(ORBIT_STARS, player.level().getGameTime() + ORBIT_TICKS));
        fx(player, SevenStarsPayloads.Fx.ORBIT, -1, List.of(), List.of(ORBIT_TICKS), ORBIT_STARS);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.4F);
        return true;
    }

    /** The stars round this player now (0: none). */
    public static int orbiting(ServerPlayer player) {
        Orbit o = ORBITS.get(player.getUUID());
        return o == null ? 0 : o.count;
    }

    /** A blow on a player with stars round them (or bounding in Dipper Steps): stopped, and struck back. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        Entity author = source.getEntity(), direct = source.getDirectEntity();
        if (author == null && direct == null) return;
        if (dipping(player) && direct instanceof LivingEntity && direct != player) { event.setCanceled(true); return; }
        Orbit o = ORBITS.get(player.getUUID());
        if (o == null || o.count <= 0 || author == player) return;
        event.setCanceled(true);
        Vec3 from = (direct != null ? direct : author).position();
        stopped(player, o, from, author instanceof LivingEntity l ? l : null);
    }

    /** The push of a blow just stopped is stopped too. */
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long at = ORBIT_BLOCKED.get(player.getUUID());
        if (at != null && player.level().getGameTime() - at <= 1) event.setCanceled(true);
    }

    /** A projectile reaching a player with stars round them is stopped dead, and its shooter struck back. */
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player)) return;
        Orbit o = ORBITS.get(player.getUUID());
        if (o == null || o.count <= 0) return;
        Projectile p = event.getProjectile();
        if (p.getOwner() == player) return;
        event.setCanceled(true);
        Vec3 from = p.position();
        p.discard();
        stopped(player, o, from, p.getOwner() instanceof LivingEntity l ? l : null);
    }

    private static void stopped(ServerPlayer player, Orbit o, Vec3 from, @Nullable LivingEntity author) {
        o.count--;
        ORBIT_BLOCKED.put(player.getUUID(), player.level().getGameTime());
        MartialProgress.count(player, MartialArts.UNBROKEN_ORBIT_BLOCKS, 1);
        if (author != null && author != player && author.isAlive()) SevenStarsSword.blow(player, author, ORBIT_SHARE);
        fx(player, SevenStarsPayloads.Fx.ORBIT_BLOCK, author == null ? -1 : author.getId(), List.of(from), List.of(), o.count);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.2F, 1.6F);
        if (o.count <= 0) ORBITS.remove(player.getUUID());
    }

    /** The stars left at the end fly to the nearest marked enemies (else the nearest), a star each. */
    private static void release(ServerPlayer player, Orbit o) {
        ORBITS.remove(player.getUUID());
        List<LivingEntity> marked = marked(player, RELEASE_REACH);
        if (marked.isEmpty()) {
            marked = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RELEASE_REACH),
                    e -> MartialCombat.isEnemy(e, player) && e.distanceTo(player) <= RELEASE_REACH));
            marked.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        }
        List<Integer> ids = new ArrayList<>();
        if (!marked.isEmpty()) for (int i = 0; i < o.count; i++) {
            LivingEntity e = marked.get(i % marked.size());
            SevenStarsSword.addStars(player, e, 1);
            ids.add(e.getId());
        }
        fx(player, SevenStarsPayloads.Fx.ORBIT_RELEASE, -1, List.of(), ids, o.count);
    }

    // ---------------------------------------------------------------- Constellation Link

    static boolean link(ServerPlayer player, PlayerPatch<?> patch) {
        List<LivingEntity> marked = marked(player, LINK_REACH);
        if (marked.isEmpty()) return false;
        patch.playAnimationSynchronized(MurimAnimations.SSS_LINK, 0.0F);
        fx(player, SevenStarsPayloads.Fx.LINK, -1, List.of(), marked.stream().map(Entity::getId).toList(), LINK_AT);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
        MartialCombat.later(player, LINK_AT, () -> {
            int burst = 0;
            for (LivingEntity e : marked) {
                int n = SevenStarsSword.stars(player, e);
                if (n <= 0 || !e.isAlive()) continue;
                float damage = TechniquePower.damage(player, MartialArts.SEVEN_STARS_SWORD, LINK_SHARE * n);
                if (!com.murimblock.integration.epicfight.MurimGuardSkill.parriedWithin(e, 6)) e.hurt(player.damageSources().playerAttack(player), damage);
                SevenStarsSword.burst(player, e);
                burst++;
            }
            if (burst > 0) {
                MartialService.reduceCooldown(player, MartialArts.SHOOTING_STAR, burst * LINK_REFUND);
                MartialProgress.addMastery(player, SevenStarsSword.art(), MartialArts.CONSTELLATION_LINK.id(), 1);
            }
            if (burst >= BIND_GROUP) MartialProgress.count(player, MartialArts.BIND_THE_SKY_GROUPS, 1);
        });
        return true;
    }

    // ---------------------------------------------------------------- Falling Star Rain

    /** Where the rain falls: where the player looks, on the ground, 12 blocks off at most. */
    public static Vec3 aim(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(RAIN_REACH + 4));
        var hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(RAIN_REACH * 0.6)) : hit.getLocation();
        Vec3 flat = at.subtract(player.position()).multiply(1, 0, 1);
        if (flat.length() > RAIN_REACH) at = player.position().add(flat.normalize().scale(RAIN_REACH));
        int top = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z));
        double y = Math.min(top, Math.max(player.getY() - 6, at.y));
        return new Vec3(at.x, y, at.z);
    }

    static boolean rain(ServerPlayer player, PlayerPatch<?> patch, boolean evolved) {
        ServerLevel level = player.serverLevel();
        patch.playAnimationSynchronized(MurimAnimations.SSS_RAIN, 0.0F);
        Vec3 centre = aim(player);
        int stars = evolved ? RAIN_STARS_EVOLVED : RAIN_STARS;
        int length = RAIN_FIRST + stars * RAIN_GAP + RAIN_FALL + 4;
        if (!player.onGround()) {
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            MartialCombat.later(player, length, () -> player.setNoGravity(false));
            MartialCombat.noFallFor(player, length + 60);
        }
        fx(player, SevenStarsPayloads.Fx.RAIN_CIRCLE, -1, List.of(centre), List.of(length), (int) (RAIN_RADIUS * 10));
        level.playSound(null, BlockPos.containing(centre), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
        var random = player.getRandom();
        for (int k = 0; k < stars; k++) {
            int n = k;
            MartialCombat.later(player, RAIN_FIRST + k * RAIN_GAP, () -> {
                List<LivingEntity> marked = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(RAIN_RADIUS + 0.5, 3, RAIN_RADIUS + 0.5),
                        e -> MartialCombat.isEnemy(e, player) && SevenStarsSword.stars(player, e) > 0));
                marked.sort(Comparator.comparingInt((LivingEntity e) -> -SevenStarsSword.stars(player, e)));
                Vec3 at;
                if (!marked.isEmpty()) at = marked.get(n % marked.size()).position();
                else {
                    double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * RAIN_RADIUS;
                    at = centre.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                }
                Vec3 impact = at;
                Vec3 sky = impact.add(-4 + random.nextDouble() * 2, RAIN_HEIGHT, -3 + random.nextDouble() * 2);
                fx(player, SevenStarsPayloads.Fx.RAIN_STAR, -1, List.of(sky, impact), List.of(), RAIN_FALL);
                MartialCombat.later(player, RAIN_FALL, () -> impact(player, impact));
            });
        }
        MartialCombat.later(player, length, () -> MartialProgress.addMastery(player, SevenStarsSword.art(), MartialArts.FALLING_STAR_RAIN.id(), 1));
        return true;
    }

    /** A falling star lands: those it strikes take a star; one reaching seven bursts at once. */
    private static void impact(ServerPlayer player, Vec3 at) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RAIN_HIT, 2.5, RAIN_HIT),
                e -> MartialCombat.isEnemy(e, player))) {
            if (Math.hypot(e.getX() - at.x, e.getZ() - at.z) > RAIN_HIT + e.getBbWidth() / 2) continue;
            if (SevenStarsSword.blow(player, e, RAIN_SHARE) && SevenStarsSword.stars(player, e) >= SevenStarsSword.MAX_STARS) {
                e.invulnerableTime = 0;
                e.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.SEVEN_STARS_SWORD, RAIN_BURST_SHARE));
                SevenStarsSword.burst(player, e);
            }
        }
        level.playSound(null, BlockPos.containing(at), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    // ---------------------------------------------------------------- Polaris Riposte

    /** Whom each player perfectly parried last, and when. */
    private record Parry(UUID attacker, long tick) { }
    private static final Map<UUID, Parry> PARRIES = new HashMap<>();

    /** A perfect parry in the stance: the riposte's window opens; the secret is found at the thirtieth. */
    static void parried(ServerPlayer player, Entity attacker) {
        PARRIES.put(player.getUUID(), new Parry(attacker.getUUID(), player.level().getGameTime()));
        MartialData data = MartialService.data(player);
        if (data.hasTechnique(MartialArts.POLARIS_RIPOSTE.id())) return;
        int n = data.progress(MartialArts.POLARIS_PARRIES) + 1;
        if (n < POLARIS_PARRIES) { MartialService.set(player, data.withProgress(MartialArts.POLARIS_PARRIES, n)); return; }
        MartialService.set(player, data.withProgress(MartialArts.POLARIS_PARRIES, n).unlock(MartialArts.POLARIS_RIPOSTE));
        player.sendSystemMessage(Component.translatable("technique.murimblock.polaris_riposte.discovered"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    /** Whether a florett strike on this target now is Polaris Riposte (consumed when it lands: {@link #riposted}). */
    static boolean riposte(ServerPlayer player, LivingEntity target) {
        Parry p = PARRIES.get(player.getUUID());
        return p != null && p.attacker.equals(target.getUUID()) && player.level().getGameTime() - p.tick <= POLARIS_WINDOW
                && MartialService.data(player).hasTechnique(MartialArts.POLARIS_RIPOSTE.id());
    }

    /** Polaris Riposte landed: the target's constellation completed, the pole star over the player. */
    static void riposted(ServerPlayer player, LivingEntity target) {
        PARRIES.remove(player.getUUID());
        if (target.isAlive()) SevenStarsSword.addStars(player, target, SevenStarsSword.MAX_STARS);
        fx(player, SevenStarsPayloads.Fx.POLARIS, target.getId(), List.of(), List.of(), 0);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 1.8F);
    }

    // ---------------------------------------------------------------- Seven Stars Sword Array

    static boolean array(ServerPlayer player, PlayerPatch<?> patch) {
        LivingEntity t = target(player, ARRAY_REACH);
        if (t == null) return false;
        ServerLevel level = player.serverLevel();
        patch.playAnimationSynchronized(MurimAnimations.SSS_ARRAY, 0.0F);
        // Seven swords round it in the Dipper, the target in its middle.
        float cx = 0, cy = 0;
        for (float[] d : DIPPER) { cx += d[0] / 7; cy += d[1] / 7; }
        List<Vec3> swords = new ArrayList<>();
        Vec3 c = t.position();
        double yaw = Math.toRadians(player.getYRot());
        for (float[] d : DIPPER) {
            double u = (d[0] - cx) * ARRAY_SPREAD, v = (d[1] - cy) * ARRAY_SPREAD;
            // Turned with the player: the Dipper laid out across their view.
            double x = u * Math.cos(yaw) - v * Math.sin(yaw), z = u * Math.sin(yaw) + v * Math.cos(yaw);
            Vec3 p = new Vec3(c.x + x, c.y + 1.4, c.z + z);
            // Not too close: at least a block and a half off the body.
            Vec3 off = p.subtract(c).multiply(1, 0, 1);
            if (off.length() < 1.5) p = c.add(off.lengthSqr() < 1e-4 ? new Vec3(1.5, 1.4, 0) : off.normalize().scale(1.5).add(0, 1.4, 0));
            swords.add(p);
        }
        hold(t, ARRAY_FINAL + 6);
        fx(player, SevenStarsPayloads.Fx.ARRAY, t.getId(), swords, List.of(ARRAY_RISE, ARRAY_FIRST, ARRAY_GAP, ARRAY_FINAL), 0);
        level.playSound(null, BlockPos.containing(c), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.2F);
        for (int k = 0; k < 7; k++) {
            int n = k;
            MartialCombat.later(player, ARRAY_FIRST + k * ARRAY_GAP, () -> {
                if (!t.isAlive()) return;
                t.invulnerableTime = 0;
                SevenStarsSword.blow(player, t, ARRAY_SHARE);
                level.playSound(null, t.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.8F, 1.4F + 0.05F * n);
            });
        }
        MartialCombat.later(player, ARRAY_FINAL, () -> {
            if (!t.isAlive()) return;
            int stars = SevenStarsSword.stars(player, t);
            t.invulnerableTime = 0;
            float damage = TechniquePower.damage(player, MartialArts.SEVEN_STARS_SWORD, ARRAY_FINAL_SHARE + ARRAY_PER_STAR * stars);
            if (!com.murimblock.integration.epicfight.MurimGuardSkill.parriedWithin(t, 6)) t.hurt(player.damageSources().playerAttack(player), damage);
            if (stars > 0) SevenStarsSword.burst(player, t);
            level.playSound(null, t.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.6F);
            MartialProgress.addMastery(player, SevenStarsSword.art(), MartialArts.SEVEN_STARS_ARRAY.id(), 1);
        });
        return true;
    }

    /** Holds a body still for a while: Epic Fight's hold if it fights through it, slowed to nothing either way. */
    private static void hold(LivingEntity t, int ticks) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(t, LivingEntityPatch.class);
        if (patch != null) patch.applyStun(StunType.HOLD, ticks / 20.0F);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
        t.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
    }

    // ---------------------------------------------------------------- Extinguished Stars

    public static boolean finishes(ServerPlayer executor) {
        return SevenStarsSword.active(executor);
    }

    /** The finish's ticks: a star lit over the body at each thrust; all put out together. */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        for (int i = 0; i < FINISH_STABS.length; i++) if (elapsed == FINISH_STABS[i]) {
            fx(executor, SevenStarsPayloads.Fx.FINISH_STAR, victim.getId(), List.of(), List.of(), i + 1);
            executor.serverLevel().playSound(null, victim.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.0F + 0.12F * i);
        }
        if (elapsed == FINISH_OUT) {
            fx(executor, SevenStarsPayloads.Fx.FINISH_OUT, victim.getId(), List.of(), List.of(), 0);
            executor.serverLevel().playSound(null, victim.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
        }
    }

    // ---------------------------------------------------------------- ticks

    static void tick(ServerPlayer player) {
        Orbit o = ORBITS.get(player.getUUID());
        if (o != null && player.level().getGameTime() > o.until) release(player, o);
        Long d = DIPPING.get(player.getUUID());
        if (d != null && player.level().getGameTime() > d) DIPPING.remove(player.getUUID());
    }

    static void forget(@Nullable UUID player) {
        if (player == null) { DIPPING.clear(); WALKS.clear(); ORBITS.clear(); ORBIT_BLOCKED.clear(); PARRIES.clear(); return; }
        DIPPING.remove(player);
        WALKS.remove(player);
        ORBITS.remove(player);
        ORBIT_BLOCKED.remove(player);
        PARRIES.remove(player);
    }

    /** For tests: ends the Orbiting Stars now, as their 6 s would. */
    public static void releaseForTests(ServerPlayer player) {
        Orbit o = ORBITS.get(player.getUUID());
        if (o != null) release(player, o);
    }

    static boolean busy(ServerPlayer player) {
        return EpicFightBridge.isBusy(player);
    }
}
