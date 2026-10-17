package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.network.ThunderSaberPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/**
 * The Purple Heaven Thunder Saber in play (docs/combat/styles/purple-heaven-thunder-saber.md), an art of the greatsword
 * only (Enzo, 17/10/2026: "une grosse arme à deux mains"). Here its passive and the state its techniques share; the
 * techniques, its secret, its ultimate and its finish are in {@link ThunderSaberMoves}:
 * <ul>
 *   <li><b>Rolling Drums</b>: each greatsword strike that lands beats a thunder drum ({@link #BEATS_PER_DRUM} beats a drum,
 *   {@link #MAX_DRUMS} drums); a perfect parry beats twice. A technique beats one full drum if there is one: its thunder
 *   version. The beats fade one by one after {@link #FADE_AFTER} ticks without a blow; under a real thunderstorm, out in
 *   the open, every beat counts double;</li>
 *   <li><b>unshaken</b>: in the stance, while it swings and while a technique plays, the player cannot be staggered or
 *   pushed (they still take the damage);</li>
 *   <li>the chain's last strike, a drum full, lets go a small discharge round the target;</li>
 *   <li><b>electrified</b>: a body the thunder struck is slowed {@link #ELECTRIFIED_TICKS} ticks; the techniques do more to
 *   it, the arcs leap from one electrified body to the next.</li>
 * </ul>
 * The clients are told every change (ThunderSaberPayloads) and draw it (client/ThunderSaberFx).
 */
public final class ThunderSaber {
    public static final int MAX_DRUMS = 3, BEATS_PER_DRUM = 3, MAX_BEATS = MAX_DRUMS * BEATS_PER_DRUM;
    /** Beats a perfect parry gives. */
    public static final int PARRY_BEATS = 2;
    /** After this long without a blow (8 s) the beats fade, one every {@link #FADE_EVERY} ticks. */
    public static final int FADE_AFTER = 160, FADE_EVERY = 40;
    /** The chain's last strike with a drum full: a small discharge round the target. */
    public static final float DISCHARGE_SHARE = 0.3F;
    public static final double DISCHARGE_REACH = 2.0;
    /** How long a body stays electrified, and how much more the art's blows do to it. */
    public static final int ELECTRIFIED_TICKS = 60;
    public static final float ELECTRIFIED_FACTOR = 1.3F;
    /** How long a strike keeps the player unshaken once it starts. */
    public static final int STRIKE_UNSHAKEN = 16;

    private ThunderSaber() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.THUNDER_SABER);
    }

    /** A greatsword in the main hand: the Purple Heaven Zhanmadao, or any weapon Epic Fight holds as a greatsword. */
    public static boolean holdsSaber(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        CapabilityItem cap = EpicFightCapabilities.getItemStackCapability(stack);
        return cap != null && cap.getWeaponCategory() == CapabilityItem.WeaponCategories.GREATSWORD;
    }

    /** Whether this player fights with Rolling Drums: the art is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.THUNDER_SABER::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.THUNDER_SABER::equals).isPresent();
    }

    /** In the stance, a greatsword in hand. */
    public static boolean active(Player player) {
        return stanceActive(player) && holdsSaber(player);
    }

    // ---------------------------------------------------------------- swings

    /** Epic Fight's greatsword strikes: its two in a chain, its dash, its air slash. */
    static boolean isStrike(@Nullable AnimationAccessor<? extends StaticAnimation> a) {
        return a != null && (a == Animations.GREATSWORD_AUTO1 || a == Animations.GREATSWORD_AUTO2
                || a == Animations.GREATSWORD_DASH || a == Animations.GREATSWORD_AIR_SLASH);
    }

    /** What an animation counts for in this art: a strike of the passive (in the stance, a greatsword in hand). */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (!isStrike(a) || !active(player)) return null;
        // The swing begins: unshaken while it lasts.
        if (player instanceof ServerPlayer sp) unshakenFor(sp, STRIKE_UNSHAKEN);
        LAST_STRIKE.put(player.getUUID(), a);
        return MartialArts.ROLLING_DRUMS.id();
    }

    /** The strike each player swung last. */
    private static final Map<UUID, AnimationAccessor<? extends StaticAnimation>> LAST_STRIKE = new HashMap<>();

    // ---------------------------------------------------------------- the drums

    private static final class Drums {
        int beats;
        long last, faded;
    }

    private static final Map<UUID, Drums> DRUMS = new HashMap<>();
    /** Each player's Thunder Sovereign: until when the drums stay full. */
    private static final Map<UUID, Long> SOVEREIGN = new HashMap<>();

    /** The beats this player has (0 to {@link #MAX_BEATS}). */
    public static int beats(Player player) {
        if (sovereign(player)) return MAX_BEATS;
        Drums d = DRUMS.get(player.getUUID());
        return d == null ? 0 : d.beats;
    }

    /** The full drums this player has. */
    public static int drums(Player player) {
        return beats(player) / BEATS_PER_DRUM;
    }

    /** Whether a real thunderstorm is over this player, out in the open: every beat counts double. */
    public static boolean underStorm(Player player) {
        return player.level().isThundering() && player.level().canSeeSky(player.blockPosition().above());
    }

    /** Beats `n` times (twice that under a storm). */
    public static void beat(ServerPlayer player, int n) {
        if (n <= 0) return;
        if (underStorm(player)) n *= 2;
        long now = player.level().getGameTime();
        Drums d = DRUMS.computeIfAbsent(player.getUUID(), k -> new Drums());
        int before = d.beats;
        d.beats = Math.min(MAX_BEATS, d.beats + n);
        d.last = now;
        d.faded = now;
        if (d.beats == before) return;
        int full = d.beats / BEATS_PER_DRUM - before / BEATS_PER_DRUM;
        ServerLevel level = player.serverLevel();
        if (full > 0) {
            // A drum full: its deep note.
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 1.2F, 0.6F);
            if (d.beats == MAX_BEATS) {
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.25F, 1.8F);
                MartialProgress.count(player, MartialArts.FIRST_DRUM_FULL, 1);
            }
        }
        sync(player);
    }

    /** Sets the beats outright (tests, the secret). */
    public static void setBeats(ServerPlayer player, int beats) {
        Drums d = DRUMS.computeIfAbsent(player.getUUID(), k -> new Drums());
        d.beats = Math.max(0, Math.min(MAX_BEATS, beats));
        d.last = d.faded = player.level().getGameTime();
        sync(player);
    }

    /** A technique beats one full drum if there is one: true if it did (the thunder version). */
    static boolean spendDrum(ServerPlayer player) {
        if (sovereign(player)) return true;
        Drums d = DRUMS.get(player.getUUID());
        if (d == null || d.beats < BEATS_PER_DRUM) return false;
        d.beats -= BEATS_PER_DRUM;
        sync(player);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 1.6F, 0.5F);
        return true;
    }

    /** Nine Heavens' Wrath beats every full drum: how many. */
    static int spendAllDrums(ServerPlayer player) {
        if (sovereign(player)) return MAX_DRUMS;
        Drums d = DRUMS.get(player.getUUID());
        if (d == null) return 0;
        int n = d.beats / BEATS_PER_DRUM;
        d.beats -= n * BEATS_PER_DRUM;
        if (n > 0) sync(player);
        return n;
    }

    static void sync(ServerPlayer player) {
        if (player.connection == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ThunderSaberPayloads.Drums(player.getId(), beats(player)));
    }

    // ---------------------------------------------------------------- unshaken

    private static final Map<UUID, Long> UNSHAKEN = new HashMap<>();

    /** The player cannot be staggered or pushed for this many ticks (if longer than what is left). */
    static void unshakenFor(ServerPlayer player, int ticks) {
        long until = player.level().getGameTime() + ticks;
        UNSHAKEN.merge(player.getUUID(), until, Math::max);
    }

    /** Whether this player is unshaken now: no stun, no push. */
    public static boolean unshaken(Player player) {
        Long until = UNSHAKEN.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until && active(player);
    }

    /** Hit while unshaken: Unmoved counts it. */
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0 && unshaken(player)
                && event.getSource().getEntity() instanceof LivingEntity author && MartialCombat.isEnemy(author, player))
            MartialProgress.count(player, MartialArts.UNMOVED_HITS, 1);
    }

    /** No push while unshaken. */
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && unshaken(player)) event.setCanceled(true);
    }

    // ---------------------------------------------------------------- electrified

    private record Shock(LivingEntity target, UUID owner, long until) { }

    private static final Map<UUID, Shock> SHOCKS = new HashMap<>();

    /** The thunder struck this body: slowed, and the art's blows do more to it, for a while. */
    static void electrify(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive() || target == player) return;
        long until = player.level().getGameTime() + ELECTRIFIED_TICKS;
        SHOCKS.put(target.getUUID(), new Shock(target, player.getUUID(), until));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ELECTRIFIED_TICKS, 0, false, false));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new ThunderSaberPayloads.Electrified(target.getId(), ELECTRIFIED_TICKS));
    }

    /** Whether this body is electrified now. */
    public static boolean electrified(LivingEntity target) {
        Shock s = SHOCKS.get(target.getUUID());
        return s != null && target.level().getGameTime() <= s.until;
    }

    /** The electrified enemies of this player within `reach` of a point, the nearest first. */
    static List<LivingEntity> electrifiedNear(ServerPlayer player, Vec3 at, double reach) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach),
                e -> MartialCombat.isEnemy(e, player) && electrified(e) && e.position().distanceTo(at) <= reach)) out.add(e);
        out.sort((a, b) -> Double.compare(a.position().distanceToSqr(at), b.position().distanceToSqr(at)));
        return out;
    }

    // ---------------------------------------------------------------- blows

    /**
     * A blow of a technique that is not an animation's hit: damage from the player (the art's share of the attack, more
     * on an electrified body), a guard raised in time stops it. True if it landed.
     */
    static boolean blow(ServerPlayer player, LivingEntity e, float share) {
        if (!MartialCombat.isEnemy(e, player) || MurimGuardSkill.parriedWithin(e, 6)) return false;
        float damage = TechniquePower.damage(player, MartialArts.THUNDER_SABER, share) * (electrified(e) ? ELECTRIFIED_FACTOR : 1);
        // Each blow of a technique lands on its own (the bolts come faster than a body's grace time).
        e.invulnerableTime = 0;
        if (!e.hurt(player.damageSources().playerAttack(player), damage)) return false;
        e.hurtMarked = true;
        return true;
    }

    /** A blow of the thunder: as {@link #blow}, then the body electrified. */
    static boolean thunderBlow(ServerPlayer player, LivingEntity e, float share) {
        boolean hit = blow(player, e, share);
        if (hit) electrify(player, e);
        return hit;
    }

    /** Pushes a body away from a point (up a little): the shockwaves. */
    static void push(LivingEntity e, Vec3 from, double strength, double up) {
        Vec3 d = e.position().subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        d = d.normalize().scale(strength);
        e.setDeltaMovement(e.getDeltaMovement().add(d.x, up, d.z));
        e.hurtMarked = true;
    }

    // ---------------------------------------------------------------- hits

    /** Before a hit of the art: the secret's riposte, an electrified body. */
    static float factorBefore(ServerPlayer player, LivingEntity target) {
        float factor = electrified(target) ? ELECTRIFIED_FACTOR : 1.0F;
        if (ThunderSaberMoves.riposte(player, target)) factor *= ThunderSaberMoves.ANSWERS_FACTOR;
        return factor;
    }

    /** After a hit of the art: a beat; the chain's last strike, a drum full, lets go its discharge; the ultimate's bolt. */
    static void onHit(ServerPlayer player, LivingEntity target) {
        AnimationAccessor<? extends StaticAnimation> a = LAST_STRIKE.get(player.getUUID());
        if (a == Animations.GREATSWORD_AUTO2 && drums(player) > 0) discharge(player, target);
        beat(player, 1);
        if (ThunderSaberMoves.riposte(player, target)) ThunderSaberMoves.answered(player, target);
        ThunderSaberMoves.struck(player, target);
    }

    /** A small discharge round a target: a short bolt on it, the bodies within two blocks struck a little. */
    private static void discharge(ServerPlayer player, LivingEntity target) {
        Vec3 c = target.position();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(DISCHARGE_REACH),
                e -> e != target && MartialCombat.isEnemy(e, player) && e.distanceTo(target) <= DISCHARGE_REACH)) {
            if (blow(player, e, DISCHARGE_SHARE)) electrify(player, e);
        }
        electrify(player, target);
        Vec3 top = c.add(0, target.getBbHeight() + 2.5, 0);
        ThunderSaberMoves.fx(player, ThunderSaberPayloads.Fx.BOLT, target.getId(), List.of(top, c), List.of(), 6);
        ThunderSaberMoves.fx(player, ThunderSaberPayloads.Fx.SHOCKWAVE, -1, List.of(c), List.of(1), (int) (DISCHARGE_REACH * 10));
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    /** A perfect parry with the stance: two beats; the secret's window opens. */
    static void onPerfectParry(ServerPlayer player, Entity attacker) {
        if (!active(player)) return;
        beat(player, PARRY_BEATS);
        ThunderSaberMoves.parried(player, attacker);
    }

    // ---------------------------------------------------------------- the ultimate

    static void sovereignFor(ServerPlayer player, int ticks) {
        SOVEREIGN.put(player.getUUID(), player.level().getGameTime() + ticks);
        sync(player);
    }

    public static boolean sovereign(Player player) {
        Long until = SOVEREIGN.get(player.getUUID());
        return until != null && player.level().getGameTime() <= until;
    }

    // ---------------------------------------------------------------- ticks

    /** Every tick of a player: the beats fading, the techniques under way. */
    static void tick(ServerPlayer player) {
        ThunderSaberMoves.tick(player);
        long now = player.level().getGameTime();
        Long sov = SOVEREIGN.get(player.getUUID());
        if (sov != null && now > sov) { SOVEREIGN.remove(player.getUUID()); ThunderSaberMoves.sovereignEnds(player); sync(player); }
        Drums d = DRUMS.get(player.getUUID());
        if (d == null || d.beats <= 0) return;
        if (now - d.last < FADE_AFTER || now - d.faded < FADE_EVERY) return;
        d.faded = now;
        d.beats--;
        sync(player);
    }

    /** Every server tick: the electrified bodies dead or come back to themselves forgotten. */
    public static void serverTick(MinecraftServer server) {
        for (Iterator<Shock> it = SHOCKS.values().iterator(); it.hasNext(); ) {
            Shock s = it.next();
            if (!s.target.isAlive() || s.target.isRemoved() || s.target.level().getGameTime() > s.until) it.remove();
        }
    }

    static void forget(@Nullable UUID player) {
        ThunderSaberMoves.forget(player);
        if (player == null) { DRUMS.clear(); UNSHAKEN.clear(); SHOCKS.clear(); SOVEREIGN.clear(); LAST_STRIKE.clear(); return; }
        DRUMS.remove(player);
        UNSHAKEN.remove(player);
        SOVEREIGN.remove(player);
        LAST_STRIKE.remove(player);
        SHOCKS.values().removeIf(s -> s.owner.equals(player));
    }

    /** A player coming into sight of a player of the art is told their drums. */
    public static void startTracking(Entity target, ServerPlayer watcher) {
        if (target instanceof Player p && beats(p) > 0)
            PacketDistributor.sendToPlayer(watcher, new ThunderSaberPayloads.Drums(p.getId(), beats(p)));
    }

    /** For tests: a greatsword strike landing, as the patch would report it (`last` true for the chain's last). */
    public static void strikeForTests(ServerPlayer player, LivingEntity target, boolean last) {
        LAST_STRIKE.put(player.getUUID(), last ? Animations.GREATSWORD_AUTO2 : Animations.GREATSWORD_AUTO1);
        onHit(player, target);
    }

    static boolean busy(ServerPlayer player) {
        return EpicFightBridge.isBusy(player);
    }

    static Vector3f vec(Vec3 v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    /** For tests: the patch of a player (null in a mock without Epic Fight). */
    @Nullable static PlayerPatch<?> patch(ServerPlayer player) {
        return EpicFightBridge.patch(player);
    }
}
