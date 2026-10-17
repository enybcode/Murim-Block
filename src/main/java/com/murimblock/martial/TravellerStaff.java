package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.network.StaffSpinPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Traveller's Staff Art in play (docs/combat/styles/traveller-staff.md, Enzo's design sheet of 16/10/2026): the
 * Traveller's Staff in both hands, Epic Fight's own strikes for its M1. Its passive, Third Resonance, doubles the third
 * strike of a chain. Its techniques (scripts/animation/staff-art.cjs), whose boxes deal the damage:
 * <ul>
 *   <li><b>Vertical Strike</b>: raised over the head and brought straight down, one hit with the top end, 2.5 blocks out;</li>
 *   <li><b>Flurry of Points</b>: five straight thrusts with the point, 3 blocks out, the last with a little push;</li>
 *   <li><b>Spinning Rampart</b>: held, the staff spun before the body like a propeller; every attack from the front (the
 *   half space the player faces) is stopped, whatever it is, with its push and its effects. 10 Qi, then 5 a second, 3 s at
 *   most; the player walks at 40 %, cannot jump, strike nor cast; its cooldown (12 s) runs from its end.</li>
 * </ul>
 */
public final class TravellerStaff {
    /** A strike begun this long after the last (ticks) goes on its chain; later, the chain starts again. */
    static final int CHAIN_GAP_TICKS = 30;
    /** The walk while spinning: 40 % of the normal. */
    private static final ResourceLocation RAMPART_SLOW = ResourceLocation.fromNamespaceAndPath(com.murimblock.Murimblock.MOD_ID, "spinning_rampart_slow");
    /** How long the rampart's start lasts (ticks): its action animation, during which Epic Fight keeps the player busy. */
    private static final int START_TICKS = 3;

    private TravellerStaff() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.TRAVELLER_STAFF);
    }

    /** The Traveller's Staff in the main hand. */
    public static boolean holdsStaff(Player player) {
        return player.getMainHandItem().is(MurimItems.TRAVELLER_STAFF.get());
    }

    /** Whether this player fights with Third Resonance: the art is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.TRAVELLER_STAFF::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.TRAVELLER_STAFF::equals).isPresent();
    }

    // ---------------------------------------------------------------- swings and Third Resonance

    /** Epic Fight's strikes with the staff (it fights as a spear): the two-handed and one-handed autos, the dash, the air. */
    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return CelestialPeakSpear.isStrike(a);
    }

    /** Where each player's strike chain stands: the last strike's place in it (1 to 3) and when it began. */
    private record Chain(int place, long at) { }

    private static final Map<UUID, Chain> CHAINS = new HashMap<>();

    /** What an animation counts for in this art: a strike of the passive (in the stance, staff in hand) or a technique. */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (isStrike(a)) return stanceActive(player) && holdsStaff(player) ? MartialArts.THIRD_RESONANCE.id() : null;
        if (a == MurimAnimations.STAFF_VERTICAL_STRIKE) return MartialArts.VERTICAL_STRIKE.id();
        if (a == MurimAnimations.STAFF_POINT_FLURRY) return MartialArts.POINT_FLURRY.id();
        return null;
    }

    /**
     * A strike of the chain begins: its place is the next after the last if that one began less than 1.5 s ago, else the
     * first; after the third it starts again. The place counts whether the strikes before it hit or missed.
     */
    static int strikeBegan(ServerPlayer player) {
        long now = player.level().getGameTime();
        Chain last = CHAINS.get(player.getUUID());
        int place = last != null && now - last.at <= CHAIN_GAP_TICKS && last.place < 3 ? last.place + 1 : 1;
        CHAINS.put(player.getUUID(), new Chain(place, now));
        return place;
    }

    /** Whether the strike under way is the third of its chain (it strikes twice as hard). */
    static boolean thirdOfChain(ServerPlayer player) {
        Chain c = CHAINS.get(player.getUUID());
        return c != null && c.place == 3;
    }

    /** A technique or anything else breaks the chain: the next strike is a first again. */
    static void breakChain(ServerPlayer player) {
        CHAINS.remove(player.getUUID());
    }

    // ---------------------------------------------------------------- techniques

    /** Starts the Vertical Strike or the Flurry of Points (the rampart is held: {@link #startRampart}). */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (spinning(player)) return false;
        if (technique == MartialArts.VERTICAL_STRIKE) {
            play(player, patch, MurimAnimations.STAFF_VERTICAL_STRIKE, technique);
            return true;
        }
        if (technique == MartialArts.POINT_FLURRY) {
            play(player, patch, MurimAnimations.STAFF_POINT_FLURRY, technique);
            return true;
        }
        return false;
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation, MartialArts.Technique technique) {
        breakChain(player);
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
    }

    /** After a hit of the art: the strike's dull wood, the flurry's last thrust a little deeper. */
    static void onHit(ServerPlayer player, net.minecraft.world.entity.LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation) {
        if (animation == MurimAnimations.STAFF_VERTICAL_STRIKE || animation == MurimAnimations.STAFF_POINT_FLURRY)
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 1.0F,
                    animation == MurimAnimations.STAFF_VERTICAL_STRIKE ? 0.7F : 1.1F);
    }

    // ---------------------------------------------------------------- the Spinning Rampart

    /** Since when each player spins (game time), and when each last stopped. */
    private static final Map<UUID, Long> SPINS = new HashMap<>(), ENDED = new HashMap<>();

    /** Whether this player holds the Spinning Rampart (on the server). */
    public static boolean spinning(Player player) {
        return SPINS.containsKey(player.getUUID());
    }

    /** A player coming into sight of another mid-spin: the watcher is told, so the staff spins for them too. */
    public static void startTracking(net.minecraft.world.entity.Entity target, ServerPlayer watcher) {
        if (target instanceof ServerPlayer p && spinning(p)) PacketDistributor.sendToPlayer(watcher, new StaffSpinPayload(p.getId(), true));
    }

    /**
     * The key went down: the staff slides to its middle and spins (its start, then the loop the clients show as long as
     * the player is spinning). Its 10 Qi are paid by the caller. False if it cannot start now.
     */
    static boolean startRampart(ServerPlayer player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || spinning(player) || MurimAnimations.STAFF_RAMPART_START == null) return false;
        breakChain(player);
        patch.playAnimationSynchronized(MurimAnimations.STAFF_RAMPART_START, 0.0F);
        SPINS.put(player.getUUID(), player.level().getGameTime());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(RAMPART_SLOW))
            speed.addTransientModifier(new AttributeModifier(RAMPART_SLOW, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        broadcast(player, true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    /**
     * The rampart ends (the key let go, the Qi or the 3 s run out, the staff put away, the player struck down or acting):
     * the staff braked and back on guard, its cooldown from now.
     */
    static void endRampart(ServerPlayer player, boolean playEnd) {
        if (SPINS.remove(player.getUUID()) == null) return;
        ENDED.put(player.getUUID(), player.level().getGameTime());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(RAMPART_SLOW);
        broadcast(player, false);
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (playEnd && patch != null && player.isAlive() && MurimAnimations.STAFF_RAMPART_END != null)
            patch.playAnimationSynchronized(MurimAnimations.STAFF_RAMPART_END, 0.0F);
        MartialService.startCooldown(player, MartialArts.SPINNING_RAMPART);
    }

    /** Server tick of a spinning player: the Qi drawn (5 a second), the limits that end it (the jump: StaffClient). */
    static void tick(ServerPlayer player) {
        Long since = SPINS.get(player.getUUID());
        if (since == null) return;
        long held = player.level().getGameTime() - since;
        double perTick = MartialArts.RAMPART_QI_PER_SECOND / 20.0;
        boolean qiOut = com.murimblock.qi.QiService.getQi(player) < perTick;
        if (!qiOut && held > 0) com.murimblock.qi.QiService.removeQi(player, perTick);
        // Anything else the player starts (a strike, a dodge, a guard's motion) ends it at once.
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        boolean acting = held > START_TICKS && patch != null && patch.getEntityState().inaction();
        if (held >= MartialArts.RAMPART_MAX_TICKS || qiOut || acting || !holdsStaff(player) || !player.isAlive()
                || com.murimblock.combat.knockdown.KnockdownService.isHindered(player))
            endRampart(player, !acting);
    }

    /** Whether the rampart stopped by itself a moment ago (its key's release, arriving after, has nothing to do). */
    static boolean justEnded(ServerPlayer player) {
        Long at = ENDED.get(player.getUUID());
        return at != null && player.level().getGameTime() - at <= 20;
    }

    private static void broadcast(ServerPlayer player, boolean on) {
        if (player.connection == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StaffSpinPayload(player.getId(), on));
    }

    /** Whether something at `from` is in front of the player: in the half space they face (their body's turn). */
    static boolean inFront(Player player, Vec3 from) {
        Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 to = from.subtract(player.position()).multiply(1, 0, 1);
        return to.lengthSqr() < 1e-6 || facing.dot(to) > 0;
    }

    /**
     * Where an attack comes from: in melee the attacker, a projectile where it comes from (its own position, or its
     * owner's if it is inside the player), an explosion its centre. Null if it has none (the environment, an effect).
     */
    @Nullable static Vec3 origin(DamageSource source, Player player) {
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.STARVE)) return null;
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile p) {
            Vec3 back = p.getDeltaMovement().lengthSqr() > 1e-6 ? p.position().subtract(p.getDeltaMovement().normalize()) : p.position();
            return back;
        }
        if (direct != null && direct != player) return direct.position();
        Vec3 at = source.getSourcePosition();
        if (at != null) return at;
        Entity cause = source.getEntity();
        return cause != null && cause != player ? cause.position() : null;
    }

    /** The tick a player last stopped an attack (its push is stopped with it). */
    private static final Map<UUID, Long> BLOCKED = new HashMap<>();

    /** Every attack from the front is stopped while spinning, its damage, push, stun and effects with it. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !spinning(player)) return;
        Vec3 from = origin(event.getSource(), player);
        if (from == null || !inFront(player, from)) return;
        event.setCanceled(true);
        blocked(player, from);
    }

    /** The push of an attack just stopped is stopped too. */
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !spinning(player)) return;
        Long at = BLOCKED.get(player.getUUID());
        if (at != null && player.level().getGameTime() - at <= 1) event.setCanceled(true);
    }

    /** A projectile reaching a spinning player from the front is stopped dead (neutralised, not sent back). */
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player) || !spinning(player)) return;
        Projectile p = event.getProjectile();
        Vec3 from = p.getDeltaMovement().lengthSqr() > 1e-6 ? p.position().subtract(p.getDeltaMovement().normalize()) : p.position();
        if (!inFront(player, from)) return;
        event.setCanceled(true);
        p.discard();
        blocked(player, from);
    }

    private static void blocked(ServerPlayer player, Vec3 from) {
        BLOCKED.put(player.getUUID(), player.level().getGameTime());
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0, 1.2, 0).add(from.subtract(player.position()).multiply(1, 0, 1).normalize().scale(0.7));
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.15, 0.25, 0.15, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    static void forget(@Nullable UUID player) {
        if (player == null) { CHAINS.clear(); SPINS.clear(); ENDED.clear(); BLOCKED.clear(); return; }
        CHAINS.remove(player);
        SPINS.remove(player);
        ENDED.remove(player);
        BLOCKED.remove(player);
    }

    /** For tests: the idle, the rampart ended without its cooldown. */
    public static void stopForTests(ServerPlayer player) {
        if (SPINS.remove(player.getUUID()) != null) {
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(RAMPART_SLOW);
        }
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(Animations.BIPED_IDLE, 0.0F);
    }
}
