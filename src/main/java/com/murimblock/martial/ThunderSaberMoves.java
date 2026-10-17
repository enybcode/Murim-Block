package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.network.ThunderSaberPayloads;
import com.murimblock.qi.QiService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Purple Heaven Thunder Saber's techniques (docs/combat/styles/purple-heaven-thunder-saber.md), its secret, its
 * ultimate and its finish; every blow through {@link ThunderSaber#blow}. Each beats a full thunder drum if the player has
 * one ({@link ThunderSaber#spendDrum}): its thunder version. Each tells the clients what to draw (ThunderSaberPayloads.Fx,
 * client/ThunderSaberFx):
 * <ul>
 *   <li><b>Thunder Descends</b>: a great leap toward the enemy looked at (7 blocks at most), the saber brought down where
 *   it lands: the target struck, a shockwave throws the others back. Thunder: a bolt strikes the spot first, the wave goes
 *   further and electrifies. Evolved (Second Thunder): once more, free, within 2 s, if it struck;</li>
 *   <li><b>Rolling Thunder</b>: two turns of the saber held level while advancing (three evolved: Third Turn), each
 *   striking all round and pulling in. Thunder: arcs leap to the enemies near, from one electrified body to the next;</li>
 *   <li><b>Heaven's Drum</b>: held, the saber planted point down: every blow and projectile from the front is stopped and
 *   beats a drum; let go, the pommel is struck: a discharge all round, stronger for each blow stopped;</li>
 *   <li><b>Severing Flash</b>: held to charge (1.5 s), let go: a cutting wave 12 blocks ahead, harder the longer charged.
 *   Thunder: a crescent of lightning through everything, walls included;</li>
 *   <li><b>Nine Heavens' Wrath</b>: a circle where the player looks, a storm over it, six bolts and one more per drum
 *   (all of them are beaten), the electrified first; the last strikes the saber, brought down: a column of lightning.
 *   Evolved (Lingering Storm): the storm stays 4 s, a bolt a second on the electrified;</li>
 *   <li><b>Heaven Answers</b>, the secret: a saber strike right after a perfect parry, on the attacker: harder, a bolt on
 *   it, a drum full at once; found after thirty perfect parries in the art;</li>
 *   <li><b>Thunder Sovereign's Descent</b>, the ultimate: struck by the purple lightning, eight seconds with full drums
 *   and a bolt on every strike, ended by a colossal bolt on the nearest enemy;</li>
 *   <li><b>The Last Thunderclap</b>, the finish: a bolt on the raised saber, then one blow.</li>
 * </ul>
 */
public final class ThunderSaberMoves {
    // ---- Thunder Descends
    public static final double LEAP_REACH = 7, LEAP_PEAK = 2.6, LEAP_HIT = 2.6, WAVE_REACH = 3.0, WAVE_REACH_THUNDER = 4.5;
    public static final int LEAP_START = 4, LEAP_TICKS = 10, RECAST_TICKS = 40;
    public static final float LEAP_SHARE = 1.4F, LEAP_WAVE_SHARE = 0.6F;
    // ---- Rolling Thunder
    public static final double ROLL_REACH = 3.6, ROLL_ADVANCE = 3.0, ARC_REACH = 6.0, ARC_HOP = 5.0;
    public static final int TURN_TICKS = 9, ROLL_TURNS = 2, ROLL_TURNS_EVOLVED = 3, ARC_HOPS = 4;
    public static final float ROLL_SHARE = 0.7F, ARC_SHARE = 0.3F;
    // ---- Heaven's Drum
    public static final double DRUM_REACH = 4.0;
    public static final float DRUM_SHARE = 0.4F, DRUM_MAX_SHARE = 2.0F;
    public static final int DRUM_PLANT_TICKS = 6;
    // ---- Severing Flash
    public static final double FLASH_LENGTH = 12, FLASH_WIDTH = 1.6, FLASH_SPEED = 1.5;
    public static final int FLASH_CUT_TICK = 3, FLASH_STOPS_AFTER = 3, FLASH_GROUP = 3;
    public static final float FLASH_SHARE = 1.0F, FLASH_CHARGED_SHARE = 0.8F;
    // ---- Nine Heavens' Wrath
    public static final double WRATH_REACH = 14, WRATH_RADIUS = 3.2, BOLT_HIT = 1.3, CLOUD_HEIGHT = 12;
    public static final int WRATH_FIRST = 22, WRATH_SPAN = 30, WRATH_BOLTS = 6, STORM_SECONDS = 4;
    public static final float BOLT_SHARE = 0.4F, COLUMN_SHARE = 1.5F;
    // ---- Heaven Answers
    public static final int ANSWERS_WINDOW = 10, ANSWERS_PARRIES = 30;
    public static final float ANSWERS_FACTOR = 1.3F, ANSWERS_BOLT_SHARE = 0.5F;
    // ---- Thunder Sovereign's Descent
    public static final int SOVEREIGN_TICKS = 160;
    public static final double SOVEREIGN_FINAL_REACH = 8, SOVEREIGN_FINAL_WAVE = 5;
    public static final float SOVEREIGN_STRIKE_SHARE = 0.4F, SOVEREIGN_FINAL_SHARE = 2.5F, SOVEREIGN_WAVE_SHARE = 1.0F;
    // ---- The Last Thunderclap (ticks into the finish)
    public static final int FINISH_BOLT = 14, FINISH_HIT_TICK = 26;

    private static final ResourceLocation DRUM_SLOW = ResourceLocation.fromNamespaceAndPath(com.murimblock.Murimblock.MOD_ID, "heavens_drum_slow");

    private ThunderSaberMoves() {
    }

    // ---------------------------------------------------------------- helpers

    static void fx(ServerPlayer player, int kind, int target, List<Vec3> points, List<Integer> ids, int value) {
        if (player.connection == null) return;
        List<Vector3f> p = new ArrayList<>();
        for (Vec3 v : points) p.add(ThunderSaber.vec(v));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ThunderSaberPayloads.Fx(kind, player.getId(), target, p, List.copyOf(ids), value));
    }

    /** The enemy the player looks at within `reach`, else the nearest in front of them within it (null if none). */
    @Nullable static LivingEntity target(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(reach));
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach), e -> MartialCombat.isEnemy(e, player))) {
            var hit = e.getBoundingBox().inflate(0.5).clip(eye, end);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bestD) { bestD = eye.distanceToSqr(hit.get()); best = e; }
        }
        if (best != null) return best;
        Vec3 flat = flatLook(player);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach), e -> MartialCombat.isEnemy(e, player))) {
            Vec3 to = e.position().subtract(player.position());
            double d = to.length();
            if (d > reach || (d > 1 && new Vec3(to.x, 0, to.z).normalize().dot(flat) < 0.4)) continue;
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle(), flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, player.getYRot()) : flat.normalize();
    }

    /** The enemies within `reach` of a point (flat distance, two blocks up or down). */
    static List<LivingEntity> around(ServerPlayer player, Vec3 at, double reach) {
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach, 2.5, reach),
                e -> MartialCombat.isEnemy(e, player) && Math.hypot(e.getX() - at.x, e.getZ() - at.z) <= reach + e.getBbWidth() / 2);
    }

    /** Where the player's body can stand nearest above or below a point (its feet), or null. */
    @Nullable static Vec3 standAt(ServerPlayer player, Vec3 p) {
        ServerLevel level = player.serverLevel();
        AABB body = player.getBoundingBox();
        Vec3 here = player.position();
        for (double dy = 2; dy >= -4; dy -= 0.5) {
            Vec3 q = p.add(0, dy, 0);
            if (!level.noCollision(player, body.move(q.subtract(here)))) continue;
            if (!level.noCollision(player, body.move(q.subtract(here).add(0, -0.5, 0)))) return q;
        }
        return null;
    }

    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        boolean evolved = MartialService.data(player).evolved(ThunderSaber.art(), technique.id());
        if (technique == MartialArts.THUNDER_DESCENDS) return descend(player, patch, evolved);
        if (technique == MartialArts.ROLLING_THUNDER) return rolling(player, patch, evolved);
        if (technique == MartialArts.SEVERING_FLASH) return flash(player, patch);
        if (technique == MartialArts.NINE_HEAVENS_WRATH) return wrath(player, patch, evolved);
        if (technique == MartialArts.THUNDER_SOVEREIGN) return sovereign(player, patch);
        return false;
    }

    // ---------------------------------------------------------------- Thunder Descends

    /** When each player may leap again, free (Second Thunder). */
    private static final Map<UUID, Long> RECAST = new HashMap<>();

    static boolean recastOpen(ServerPlayer player) {
        Long until = RECAST.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    static void takeRecast(ServerPlayer player) {
        RECAST.remove(player.getUUID());
    }

    static boolean descend(ServerPlayer player, PlayerPatch<?> patch, boolean evolved) {
        ServerLevel level = player.serverLevel();
        boolean thunder = ThunderSaber.spendDrum(player);
        LivingEntity t = target(player, LEAP_REACH);
        Vec3 start = player.position(), dir = flatLook(player);
        double length = LEAP_REACH * 0.7;
        if (t != null) {
            Vec3 to = t.position().subtract(start).multiply(1, 0, 1);
            if (to.lengthSqr() > 1e-4) dir = to.normalize();
            length = Mth.clamp(to.length() - 1.3, 0, LEAP_REACH);
        }
        // Where it lands: as far as there is room, on the ground there.
        Vec3 land = null;
        for (double d = length; d >= 0 && land == null; d -= 0.5) land = standAt(player, start.add(dir.scale(d)));
        if (land == null) land = start;
        Vec3 from = start, to = land;
        boolean air = !player.onGround();
        double peak = air ? 0.6 : LEAP_PEAK;
        patch.playAnimationSynchronized(MurimAnimations.TS_DESCENDS, 0.0F);
        ThunderSaber.unshakenFor(player, LEAP_START + LEAP_TICKS + 14);
        MartialCombat.noFallFor(player, 80);
        fx(player, ThunderSaberPayloads.Fx.LEAP, -1, List.of(from, to), List.of(thunder ? 1 : 0), LEAP_TICKS);
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90;
        for (int k = 1; k <= LEAP_TICKS; k++) {
            int n = k;
            MartialCombat.later(player, LEAP_START + k, () -> {
                if (!player.isAlive()) return;
                double s = (double) n / LEAP_TICKS, e = s * s * (3 - 2 * s);
                Vec3 p = from.lerp(to, e).add(0, Math.sin(s * Math.PI) * peak, 0);
                player.teleportTo(level, p.x, p.y, p.z, yaw, player.getXRot());
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
            });
        }
        if (thunder) MartialCombat.later(player, LEAP_START + LEAP_TICKS - 2, () -> {
            fx(player, ThunderSaberPayloads.Fx.BOLT, -1, List.of(to.add(0.4, 22, -0.3), to), List.of(), 16);
            level.playSound(null, BlockPos.containing(to), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.2F);
        });
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
        MartialCombat.later(player, LEAP_START + LEAP_TICKS, () -> descendImpact(player, t, to, thunder, evolved));
        return true;
    }

    private static void descendImpact(ServerPlayer player, @Nullable LivingEntity t, Vec3 at, boolean thunder, boolean evolved) {
        if (!player.isAlive()) return;
        ServerLevel level = player.serverLevel();
        double reach = thunder ? WAVE_REACH_THUNDER : WAVE_REACH;
        Vec3 front = at.add(flatLook(player).scale(1.1));
        boolean struck = false;
        if (t != null && t.isAlive() && t.position().distanceTo(front) <= LEAP_HIT + t.getBbWidth() / 2)
            struck = thunder ? ThunderSaber.thunderBlow(player, t, LEAP_SHARE) : ThunderSaber.blow(player, t, LEAP_SHARE);
        for (LivingEntity e : around(player, front, reach)) {
            if (e == t) continue;
            boolean hit = thunder ? ThunderSaber.thunderBlow(player, e, LEAP_WAVE_SHARE) : ThunderSaber.blow(player, e, LEAP_WAVE_SHARE);
            if (hit) { ThunderSaber.push(e, front, 0.9, 0.35); struck = true; }
        }
        fx(player, ThunderSaberPayloads.Fx.SHOCKWAVE, -1, List.of(front), List.of(thunder ? 1 : 0), (int) (reach * 10));
        level.playSound(null, BlockPos.containing(front), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F, 0.5F);
        level.playSound(null, BlockPos.containing(front), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        if (struck) {
            MartialProgress.addMastery(player, ThunderSaber.art(), MartialArts.THUNDER_DESCENDS.id(), 1);
            if (evolved) RECAST.put(player.getUUID(), level.getGameTime() + RECAST_TICKS);
        }
    }

    // ---------------------------------------------------------------- Rolling Thunder

    static boolean rolling(ServerPlayer player, PlayerPatch<?> patch, boolean evolved) {
        ServerLevel level = player.serverLevel();
        boolean thunder = ThunderSaber.spendDrum(player);
        int turns = evolved ? ROLL_TURNS_EVOLVED : ROLL_TURNS, ticks = turns * TURN_TICKS;
        patch.playAnimationSynchronized(evolved ? MurimAnimations.TS_ROLLING_LONG : MurimAnimations.TS_ROLLING, 0.0F);
        ThunderSaber.unshakenFor(player, ticks + 8);
        Vec3 dir = flatLook(player), start = player.position();
        // Forward as the saber turns, as far as there is room.
        for (int k = 1; k <= ticks; k++) {
            int n = k;
            MartialCombat.later(player, k, () -> {
                if (!player.isAlive()) return;
                Vec3 p = start.add(dir.scale(ROLL_ADVANCE * n / ticks));
                Vec3 stand = standAt(player, p);
                if (stand == null || Math.abs(stand.y - player.getY()) > 1.1) return;
                player.teleportTo(level, stand.x, stand.y, stand.z, player.getYRot(), player.getXRot());
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
            });
        }
        fx(player, ThunderSaberPayloads.Fx.SPIN, -1, List.of(), List.of(thunder ? 1 : 0), ticks);
        for (int i = 1; i <= turns; i++) {
            MartialCombat.later(player, i * TURN_TICKS, () -> {
                if (!player.isAlive()) return;
                Vec3 c = player.position();
                boolean any = false;
                for (LivingEntity e : around(player, c, ROLL_REACH)) {
                    if (ThunderSaber.blow(player, e, ROLL_SHARE)) {
                        any = true;
                        ThunderSaber.push(e, c, -0.35, 0.1);
                    }
                }
                if (thunder) any |= arcs(player, c.add(0, 1.2, 0));
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1F, 0.7F);
                if (any) MartialProgress.addMastery(player, ThunderSaber.art(), MartialArts.ROLLING_THUNDER.id(), 1);
            });
        }
        return true;
    }

    /**
     * Arcs of lightning from a point: to the nearest enemy within reach, then from body to body (the electrified first),
     * {@link #ARC_HOPS} at most; each struck and electrified. True if any.
     */
    static boolean arcs(ServerPlayer player, Vec3 from) {
        List<Vec3> chain = new ArrayList<>(List.of(from));
        Set<LivingEntity> hit = new HashSet<>();
        Vec3 at = from;
        double reach = ARC_REACH;
        for (int k = 0; k < ARC_HOPS; k++) {
            LivingEntity next = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : around(player, at, reach)) {
                if (hit.contains(e)) continue;
                // An electrified body draws the arc as if it were nearer.
                double d = e.getBoundingBox().getCenter().distanceTo(at) * (ThunderSaber.electrified(e) ? 0.5 : 1);
                if (d < best) { best = d; next = e; }
            }
            if (next == null) break;
            hit.add(next);
            Vec3 c = next.getBoundingBox().getCenter();
            chain.add(c);
            ThunderSaber.thunderBlow(player, next, ARC_SHARE);
            at = c;
            reach = ARC_HOP;
        }
        if (chain.size() < 2) return false;
        fx(player, ThunderSaberPayloads.Fx.ARCS, -1, chain, List.of(), 0);
        player.serverLevel().playSound(null, BlockPos.containing(from), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    // ---------------------------------------------------------------- Heaven's Drum

    private static final class Drum {
        final long start;
        int blocked;

        Drum(long start) {
            this.start = start;
        }
    }

    private static final Map<UUID, Drum> DRUMMING = new HashMap<>();
    private static final Map<UUID, Long> DRUM_ENDED = new HashMap<>(), DRUM_BLOCKED = new HashMap<>();

    /** Whether this player holds Heaven's Drum (on the server). */
    public static boolean drumming(ServerPlayer player) {
        return DRUMMING.containsKey(player.getUUID());
    }

    static boolean drumJustEnded(ServerPlayer player) {
        Long at = DRUM_ENDED.get(player.getUUID());
        return at != null && player.level().getGameTime() - at <= 20;
    }

    /** The key went down: the saber planted, the drum of lightning round the player. Its Qi is paid by the caller. */
    static boolean startDrum(ServerPlayer player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || drumming(player) || MurimAnimations.TS_DRUM_PLANT == null || !ThunderSaber.holdsSaber(player)) return false;
        patch.playAnimationSynchronized(MurimAnimations.TS_DRUM_PLANT, 0.0F);
        DRUMMING.put(player.getUUID(), new Drum(player.level().getGameTime()));
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(DRUM_SLOW))
            speed.addTransientModifier(new AttributeModifier(DRUM_SLOW, -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        fx(player, ThunderSaberPayloads.Fx.DRUM, -1, List.of(), List.of(), MartialArts.HEAVENS_DRUM_MAX_TICKS);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6F, 0.6F);
        return true;
    }

    /** The drum is beaten (the key let go, or 3 s, the Qi, the saber put away): the discharge, the cooldown from now. */
    static void endDrum(ServerPlayer player, boolean strike) {
        Drum d = DRUMMING.remove(player.getUUID());
        if (d == null) return;
        DRUM_ENDED.put(player.getUUID(), player.level().getGameTime());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(DRUM_SLOW);
        MartialService.startCooldown(player, MartialArts.HEAVENS_DRUM);
        if (!strike || !player.isAlive()) {
            fx(player, ThunderSaberPayloads.Fx.DRUM_END, -1, List.of(player.position()), List.of(0), 0);
            return;
        }
        boolean thunder = ThunderSaber.spendDrum(player);
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch != null && MurimAnimations.TS_DRUM_STRIKE != null) patch.playAnimationSynchronized(MurimAnimations.TS_DRUM_STRIKE, 0.0F);
        ThunderSaber.unshakenFor(player, 12);
        float share = Math.min(DRUM_MAX_SHARE, DRUM_SHARE * Math.max(1, d.blocked));
        Vec3 c = player.position();
        boolean any = false;
        for (LivingEntity e : around(player, c, DRUM_REACH)) {
            boolean hit = thunder ? ThunderSaber.thunderBlow(player, e, share) : ThunderSaber.blow(player, e, share);
            if (hit) { ThunderSaber.push(e, c, 0.8, 0.3); any = true; }
        }
        fx(player, ThunderSaberPayloads.Fx.DRUM_END, -1, List.of(c), List.of(thunder ? 1 : 0), (int) (DRUM_REACH * 10));
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 2.0F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, thunder ? 1.2F : 0.5F, 1.4F);
        if (any || d.blocked > 0) MartialProgress.addMastery(player, ThunderSaber.art(), MartialArts.HEAVENS_DRUM.id(), 1);
    }

    private static void drumTick(ServerPlayer player) {
        Drum d = DRUMMING.get(player.getUUID());
        if (d == null) return;
        long held = player.level().getGameTime() - d.start;
        double perTick = MartialArts.HEAVENS_DRUM_QI_PER_SECOND / 20.0;
        boolean qiOut = QiService.getQi(player) < perTick;
        if (!qiOut && held > 0) QiService.removeQi(player, perTick);
        if (held >= MartialArts.HEAVENS_DRUM_MAX_TICKS || qiOut || !ThunderSaber.holdsSaber(player) || !player.isAlive()
                || com.murimblock.combat.knockdown.KnockdownService.isHindered(player))
            endDrum(player, player.isAlive());
    }

    /** Every blow from the front is stopped while the drum is held, and beats it. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !drumming(player)) return;
        Vec3 from = TravellerStaff.origin(event.getSource(), player);
        if (from == null || !TravellerStaff.inFront(player, from)) return;
        event.setCanceled(true);
        drumBlocked(player, from);
    }

    /** The push of a blow just stopped is stopped too. */
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long at = DRUM_BLOCKED.get(player.getUUID());
        if (drumming(player) || (at != null && player.level().getGameTime() - at <= 1)) event.setCanceled(true);
    }

    /** A projectile reaching the drum from the front is stopped dead. */
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player) || !drumming(player)) return;
        Projectile p = event.getProjectile();
        if (p.getOwner() == player) return;
        Vec3 from = p.getDeltaMovement().lengthSqr() > 1e-6 ? p.position().subtract(p.getDeltaMovement().normalize()) : p.position();
        if (!TravellerStaff.inFront(player, from)) return;
        event.setCanceled(true);
        p.discard();
        drumBlocked(player, from);
    }

    private static void drumBlocked(ServerPlayer player, Vec3 from) {
        Drum d = DRUMMING.get(player.getUUID());
        if (d == null) return;
        d.blocked++;
        DRUM_BLOCKED.put(player.getUUID(), player.level().getGameTime());
        ThunderSaber.beat(player, 1);
        MartialProgress.count(player, MartialArts.DRUM_ANSWERS_BLOCKS, 1);
        fx(player, ThunderSaberPayloads.Fx.DRUM_BLOCK, -1, List.of(from), List.of(), d.blocked);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 1.4F, 0.6F + 0.05F * d.blocked);
    }

    // ---------------------------------------------------------------- Severing Flash

    /** Since when each player charges Severing Flash. */
    private static final Map<UUID, Long> CHARGES = new HashMap<>();

    public static boolean charging(ServerPlayer player) {
        return CHARGES.containsKey(player.getUUID());
    }

    /** The key went down: low stance, the saber back, charging. Nothing is spent until it is let go ({@link #flash}). */
    static boolean aimFlash(ServerPlayer player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || charging(player) || MurimAnimations.TS_FLASH_CHARGE == null || !ThunderSaber.holdsSaber(player)) return false;
        patch.playAnimationSynchronized(MurimAnimations.TS_FLASH_CHARGE, 0.0F);
        CHARGES.put(player.getUUID(), player.level().getGameTime());
        ThunderSaber.unshakenFor(player, MartialArts.SEVERING_FLASH_MAX_CHARGE + 12);
        fx(player, ThunderSaberPayloads.Fx.CHARGE, -1, List.of(), List.of(), MartialArts.SEVERING_FLASH_MAX_CHARGE);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.8F);
        return true;
    }

    /** Let go: the cut, and the wave it sends (charged from 0 to 1). */
    static boolean flash(ServerPlayer player, PlayerPatch<?> patch) {
        Long since = CHARGES.remove(player.getUUID());
        float charge = since == null ? 0 : Mth.clamp((player.level().getGameTime() - since) / (float) MartialArts.SEVERING_FLASH_MAX_CHARGE, 0, 1);
        boolean thunder = ThunderSaber.spendDrum(player);
        patch.playAnimationSynchronized(MurimAnimations.TS_FLASH_CUT, 0.0F);
        ThunderSaber.unshakenFor(player, 12);
        fx(player, ThunderSaberPayloads.Fx.CHARGE, -1, List.of(), List.of(), 0);
        MartialCombat.later(player, FLASH_CUT_TICK, () -> wave(player, charge, thunder));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.5F);
        return true;
    }

    /** The cutting wave: straight ahead at chest height; it stops at a wall and after three bodies, unless it is thunder. */
    private static void wave(ServerPlayer player, float charge, boolean thunder) {
        if (!player.isAlive()) return;
        ServerLevel level = player.serverLevel();
        Vec3 dir = flatLook(player), start = player.position().add(0, 1.0, 0).add(dir.scale(0.8));
        Vec3 end = start.add(dir.scale(FLASH_LENGTH));
        if (!thunder) {
            var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();
        }
        List<LivingEntity> bodies = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(FLASH_WIDTH, 1.5, FLASH_WIDTH),
                e -> MartialCombat.isEnemy(e, player)));
        Vec3 s = start, en = end;
        bodies.removeIf(e -> SevenStarsSword.distanceToSegment(e.getBoundingBox().getCenter(), s, en) > FLASH_WIDTH + e.getBbWidth() / 2);
        bodies.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(s)));
        if (!thunder && bodies.size() > FLASH_STOPS_AFTER) {
            bodies = new ArrayList<>(bodies.subList(0, FLASH_STOPS_AFTER));
            Vec3 last = bodies.get(FLASH_STOPS_AFTER - 1).position();
            end = start.add(dir.scale(Math.min(FLASH_LENGTH, last.subtract(start).multiply(1, 0, 1).length() + 0.8)));
        }
        float share = FLASH_SHARE + FLASH_CHARGED_SHARE * charge;
        int ticks = Math.max(1, (int) Math.ceil(start.distanceTo(end) / FLASH_SPEED));
        List<Integer> ids = new ArrayList<>(List.of(thunder ? 1 : 0));
        int shocked = 0;
        for (LivingEntity e : bodies) {
            ids.add(e.getId());
            if (ThunderSaber.electrified(e)) shocked++;
            int at = (int) Math.round(e.position().distanceTo(start) / FLASH_SPEED);
            MartialCombat.later(player, at, () -> {
                if (!e.isAlive()) return;
                if (thunder) ThunderSaber.thunderBlow(player, e, share); else ThunderSaber.blow(player, e, share);
            });
        }
        if (shocked >= FLASH_GROUP) MartialProgress.count(player, MartialArts.CALL_DOWN_SKY_FLASHES, 1);
        if (!bodies.isEmpty()) MartialProgress.addMastery(player, ThunderSaber.art(), MartialArts.SEVERING_FLASH.id(), 1);
        fx(player, ThunderSaberPayloads.Fx.WAVE, -1, List.of(start, end), ids, ticks);
        level.playSound(null, player.blockPosition(), thunder ? SoundEvents.TRIDENT_THUNDER.value() : SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // ---------------------------------------------------------------- Nine Heavens' Wrath

    /** Where the storm comes down: where the player looks, on the ground, 14 blocks off at most. */
    public static Vec3 aim(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(WRATH_REACH + 4));
        var hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? player.position().add(flatLook(player).scale(WRATH_REACH * 0.6)) : hit.getLocation();
        Vec3 flat = at.subtract(player.position()).multiply(1, 0, 1);
        if (flat.length() > WRATH_REACH) at = player.position().add(flat.normalize().scale(WRATH_REACH));
        int top = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z));
        double y = Math.min(top, Math.max(player.getY() - 6, at.y));
        return new Vec3(at.x, y, at.z);
    }

    static boolean wrath(ServerPlayer player, PlayerPatch<?> patch, boolean evolved) {
        ServerLevel level = player.serverLevel();
        patch.playAnimationSynchronized(MurimAnimations.TS_WRATH, 0.0F);
        Vec3 centre = aim(player);
        int spent = ThunderSaber.spendAllDrums(player);
        int bolts = WRATH_BOLTS + spent, gap = Math.max(3, WRATH_SPAN / bolts);
        int toSaber = WRATH_FIRST + bolts * gap + 4, slam = toSaber + 6;
        int storm = evolved ? STORM_SECONDS * 20 : 0, length = slam + 14 + storm;
        ThunderSaber.unshakenFor(player, slam + 10);
        if (!player.onGround()) {
            player.setNoGravity(true);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            MartialCombat.later(player, slam + 8, () -> player.setNoGravity(false));
            MartialCombat.noFallFor(player, slam + 60);
        }
        fx(player, ThunderSaberPayloads.Fx.WRATH, -1, List.of(centre), List.of(length, bolts), (int) (WRATH_RADIUS * 10));
        level.playSound(null, BlockPos.containing(centre), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        var random = player.getRandom();
        for (int k = 0; k < bolts; k++) {
            int n = k;
            MartialCombat.later(player, WRATH_FIRST + k * gap, () -> {
                List<LivingEntity> inside = around(player, centre, WRATH_RADIUS);
                inside.sort(Comparator.comparingInt((LivingEntity e) -> ThunderSaber.electrified(e) ? 0 : 1));
                Vec3 at;
                if (!inside.isEmpty() && random.nextFloat() < 0.8F) at = inside.get(n % inside.size()).position();
                else {
                    double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * WRATH_RADIUS;
                    at = centre.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                }
                bolt(player, at, BOLT_SHARE, true);
            });
        }
        MartialCombat.later(player, toSaber, () -> {
            Vec3 saber = player.position().add(0, 3.2, 0);
            fx(player, ThunderSaberPayloads.Fx.BOLT, -1, List.of(centre.add(0, CLOUD_HEIGHT, 0), saber), List.of(), 14);
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.2F, 1.0F);
        });
        MartialCombat.later(player, slam, () -> {
            if (!player.isAlive()) return;
            boolean any = false;
            for (LivingEntity e : around(player, centre, WRATH_RADIUS + 0.4)) {
                if (ThunderSaber.thunderBlow(player, e, COLUMN_SHARE)) { ThunderSaber.push(e, centre, 0.4, 0.9); any = true; }
            }
            fx(player, ThunderSaberPayloads.Fx.COLUMN, -1, List.of(centre), List.of(), (int) (WRATH_RADIUS * 10));
            level.playSound(null, BlockPos.containing(centre), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.8F);
            level.playSound(null, BlockPos.containing(centre), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
            if (any) MartialProgress.addMastery(player, ThunderSaber.art(), MartialArts.NINE_HEAVENS_WRATH.id(), 1);
        });
        // Lingering Storm: a bolt a second on the electrified in the circle.
        for (int s = 1; s <= storm / 20; s++)
            MartialCombat.later(player, slam + s * 20, () -> {
                List<LivingEntity> shocked = around(player, centre, WRATH_RADIUS + 1);
                shocked.removeIf(e -> !ThunderSaber.electrified(e));
                if (!shocked.isEmpty()) bolt(player, shocked.get(random.nextInt(shocked.size())).position(), BOLT_SHARE, true);
            });
        return true;
    }

    /** A bolt from the sky onto a point: the bodies within reach struck (and electrified, if `electrify`). */
    static void bolt(ServerPlayer player, Vec3 at, float share, boolean electrify) {
        ServerLevel level = player.serverLevel();
        var random = player.getRandom();
        Vec3 sky = at.add(-1.5 + random.nextDouble() * 3, CLOUD_HEIGHT, -1.5 + random.nextDouble() * 3);
        fx(player, ThunderSaberPayloads.Fx.BOLT, -1, List.of(sky, at), List.of(), 10);
        int hits = 0;
        for (LivingEntity e : around(player, at, BOLT_HIT)) {
            boolean hit = electrify ? ThunderSaber.thunderBlow(player, e, share) : ThunderSaber.blow(player, e, share);
            if (hit) hits++;
        }
        if (hits > 0) MartialProgress.count(player, MartialArts.SOVEREIGN_BOLTS, hits);
        level.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.9F, 1.1F + random.nextFloat() * 0.3F);
    }

    // ---------------------------------------------------------------- Heaven Answers

    private record Parry(UUID attacker, long tick) { }
    private static final Map<UUID, Parry> PARRIES = new HashMap<>();

    /** A perfect parry in the stance: the riposte's window opens; the secret is found at the thirtieth. */
    static void parried(ServerPlayer player, Entity attacker) {
        PARRIES.put(player.getUUID(), new Parry(attacker.getUUID(), player.level().getGameTime()));
        MartialData data = MartialService.data(player);
        if (data.hasTechnique(MartialArts.HEAVEN_ANSWERS.id())) return;
        int n = data.progress(MartialArts.HEAVEN_ANSWERS_PARRIES) + 1;
        if (n < ANSWERS_PARRIES) { MartialService.set(player, data.withProgress(MartialArts.HEAVEN_ANSWERS_PARRIES, n)); return; }
        MartialService.set(player, data.withProgress(MartialArts.HEAVEN_ANSWERS_PARRIES, n).unlock(MartialArts.HEAVEN_ANSWERS));
        player.sendSystemMessage(Component.translatable("technique.murimblock.heaven_answers.discovered"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    /** Whether a saber strike on this target now is Heaven Answers. */
    static boolean riposte(ServerPlayer player, LivingEntity target) {
        Parry p = PARRIES.get(player.getUUID());
        return p != null && p.attacker.equals(target.getUUID()) && player.level().getGameTime() - p.tick <= ANSWERS_WINDOW
                && MartialService.data(player).hasTechnique(MartialArts.HEAVEN_ANSWERS.id());
    }

    /** Heaven Answers landed: the sky opens over the target, a bolt falls on it, a drum is full. */
    static void answered(ServerPlayer player, LivingEntity target) {
        PARRIES.remove(player.getUUID());
        fx(player, ThunderSaberPayloads.Fx.ANSWERS, target.getId(), List.of(), List.of(), 0);
        if (target.isAlive()) {
            Vec3 at = target.position();
            fx(player, ThunderSaberPayloads.Fx.BOLT, target.getId(), List.of(at.add(0.3, CLOUD_HEIGHT, 0.2), at), List.of(), 12);
            ThunderSaber.thunderBlow(player, target, ANSWERS_BOLT_SHARE);
        }
        ThunderSaber.beat(player, ThunderSaber.BEATS_PER_DRUM);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    // ---------------------------------------------------------------- Thunder Sovereign's Descent

    static boolean sovereign(ServerPlayer player, PlayerPatch<?> patch) {
        ServerLevel level = player.serverLevel();
        patch.playAnimationSynchronized(MurimAnimations.TS_SOVEREIGN, 0.0F);
        ThunderSaber.unshakenFor(player, 30);
        MartialCombat.later(player, 10, () -> {
            if (!player.isAlive()) return;
            Vec3 at = player.position();
            fx(player, ThunderSaberPayloads.Fx.BOLT, player.getId(), List.of(at.add(0, 26, 0), at), List.of(), 26);
            fx(player, ThunderSaberPayloads.Fx.SOVEREIGN, -1, List.of(), List.of(), SOVEREIGN_TICKS);
            fx(player, ThunderSaberPayloads.Fx.SHOCKWAVE, -1, List.of(at), List.of(1), 40);
            ThunderSaber.sovereignFor(player, SOVEREIGN_TICKS);
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.7F);
        });
        return true;
    }

    /** A strike landed (the art's): during the Descent, a bolt on its target. */
    static void struck(ServerPlayer player, LivingEntity target) {
        if (!ThunderSaber.sovereign(player) || !target.isAlive()) return;
        Vec3 at = target.position();
        fx(player, ThunderSaberPayloads.Fx.BOLT, target.getId(), List.of(at.add(0.4, 14, -0.2), at), List.of(), 10);
        ThunderSaber.thunderBlow(player, target, SOVEREIGN_STRIKE_SHARE);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    /** The Descent ends: a colossal bolt on the nearest enemy, a wave round it. */
    static void sovereignEnds(ServerPlayer player) {
        if (!player.isAlive()) return;
        LivingEntity t = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : around(player, player.position(), SOVEREIGN_FINAL_REACH)) {
            double d = e.distanceToSqr(player);
            if (d < best) { best = d; t = e; }
        }
        Vec3 at = t != null ? t.position() : player.position().add(flatLook(player).scale(3));
        fx(player, ThunderSaberPayloads.Fx.BOLT, t == null ? -1 : t.getId(), List.of(at.add(0, 30, 0), at), List.of(), 34);
        fx(player, ThunderSaberPayloads.Fx.COLUMN, -1, List.of(at), List.of(), 20);
        fx(player, ThunderSaberPayloads.Fx.SHOCKWAVE, -1, List.of(at), List.of(1), (int) (SOVEREIGN_FINAL_WAVE * 10));
        if (t != null) ThunderSaber.thunderBlow(player, t, SOVEREIGN_FINAL_SHARE);
        for (LivingEntity e : around(player, at, SOVEREIGN_FINAL_WAVE)) {
            if (e == t) continue;
            if (ThunderSaber.thunderBlow(player, e, SOVEREIGN_WAVE_SHARE)) ThunderSaber.push(e, at, 1.0, 0.5);
        }
        ServerLevel level = player.serverLevel();
        level.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 0.6F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
        MartialProgress.addMastery(player, ThunderSaber.art(), MartialArts.THUNDER_SOVEREIGN.id(), 1);
    }

    // ---------------------------------------------------------------- The Last Thunderclap

    public static boolean finishes(ServerPlayer executor) {
        return ThunderSaber.active(executor);
    }

    /** The finish's ticks: a bolt on the raised saber, then the blow, and its thunder. */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        ServerLevel level = executor.serverLevel();
        if (elapsed == FINISH_BOLT) {
            Vec3 saber = executor.position().add(0, 3.2, 0);
            fx(executor, ThunderSaberPayloads.Fx.BOLT, -1, List.of(saber.add(0.5, 20, 0), saber), List.of(), 14);
            level.playSound(null, executor.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.2F, 1.0F);
        }
        if (elapsed == FINISH_HIT_TICK) {
            Vec3 at = victim.position();
            fx(executor, ThunderSaberPayloads.Fx.SHOCKWAVE, -1, List.of(at), List.of(1), 30);
            fx(executor, ThunderSaberPayloads.Fx.COLUMN, -1, List.of(at), List.of(), 8);
            level.playSound(null, victim.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.6F, 0.9F);
        }
    }

    // ---------------------------------------------------------------- ticks

    static void tick(ServerPlayer player) {
        drumTick(player);
        Long since = CHARGES.get(player.getUUID());
        if (since != null && (!ThunderSaber.holdsSaber(player) || !player.isAlive()
                || player.level().getGameTime() - since > MartialArts.SEVERING_FLASH_MAX_CHARGE + 40)) {
            // Held far past its time (the key's release never came) or the saber put away: the charge is lost.
            CHARGES.remove(player.getUUID());
            fx(player, ThunderSaberPayloads.Fx.CHARGE, -1, List.of(), List.of(), 0);
        }
        Long r = RECAST.get(player.getUUID());
        if (r != null && player.level().getGameTime() > r) RECAST.remove(player.getUUID());
    }

    static void forget(@Nullable UUID player) {
        if (player == null) {
            RECAST.clear(); DRUMMING.clear(); DRUM_ENDED.clear(); DRUM_BLOCKED.clear(); CHARGES.clear(); PARRIES.clear();
            return;
        }
        RECAST.remove(player);
        DRUMMING.remove(player);
        DRUM_ENDED.remove(player);
        DRUM_BLOCKED.remove(player);
        CHARGES.remove(player);
        PARRIES.remove(player);
    }

    /** For tests: ends Heaven's Drum now, as letting the key go would. */
    public static void endDrumForTests(ServerPlayer player) {
        endDrum(player, true);
    }

    /** For tests: starts the drum as the key going down would (without its Qi). */
    public static boolean startDrumForTests(ServerPlayer player) {
        return startDrum(player);
    }
}
