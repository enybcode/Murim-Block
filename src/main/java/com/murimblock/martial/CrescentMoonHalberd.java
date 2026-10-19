package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.MurimEntities;
import com.murimblock.network.CrescentMoonPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Crescent Moon Halberd in play (docs/combat/styles/crescent-moon-halberd.md, Enzo's choice of 19/10/2026): an
 * orthodox art of the moon and water, elegant, all in circles, at middle reach. A spear in hand (any Epic Fight spear;
 * the Crescent Moon Halberd is its own). Its M1 are Epic Fight's spear strikes; its passive, <b>Waxing Moon</b>, fills
 * the moon a phase for each that lands (shown by the crosshair), and with the moon full the next strike sends a
 * crescent of light flying 15 blocks. Its techniques:
 * <ul>
 *   <li><b>Moonlit Circle</b>: a whole turn low, then a ring of moon-water on the ground; whoever stands in it is slowed;</li>
 *   <li><b>Tide Hook</b>: the hook caught on a foe ahead: pulled in to the halberd, or, too heavy, tripped where it stands;</li>
 *   <li><b>Falling Crescent</b>: a great slash down and across, three crescents flying off in a fan;</li>
 *   <li><b>Mirror Lake Stance</b>, held: the halberd upright before the body, a mirror of still water; a blow or a
 *   projectile from the front is stopped and sent back to its sender as a crescent;</li>
 *   <li><b>Night of the Full Moon</b>, the ultimate: night falls round the cultivator, a full moon rises over them and its
 *   beams strike the enemies round about, one volley after another.</li>
 * </ul>
 * Its shapes of moonlight are GeckoLib models (MoonQiEntity), its lights Photon's (CrescentMoonPayloads.Fx).
 */
public final class CrescentMoonHalberd {
    /** Waxing Moon: the phases of a full moon, how long they last without a hit, the crescent it sends (share). */
    public static final int PHASES = 6, PHASE_TICKS = 160;
    public static final float PASSIVE_SHARE = 1.2F;
    /** A crescent of light: how far it flies, how fast (blocks a tick), how wide it cuts. */
    public static final double CRESCENT_RANGE = 15, CRESCENT_SPEED = 1.1, CRESCENT_WIDTH = 1.5;
    /** Moonlit Circle: its ring (radius, ticks), its slowness. */
    public static final double CIRCLE_RADIUS = 4.5;
    public static final int CIRCLE_TICKS = 100, CIRCLE_SLOW = 2;
    /** Tide Hook: its reach, its blow; who is too heavy to be pulled (and is tripped instead). */
    public static final double HOOK_REACH = 7;
    public static final float HOOK_SHARE = 0.7F, HEAVY_HEALTH = 60;
    public static final int HOOK_PULL_TICKS = 6;
    /** Falling Crescent: its three crescents, the fan's angle between them (degrees). */
    public static final float FALLING_SHARE = 0.9F, FAN = 18;
    /** Mirror Lake Stance: the crescent a stopped blow sends back. */
    public static final float MIRROR_SHARE = 1.2F;
    private static final ResourceLocation MIRROR_SLOW = ResourceLocation.fromNamespaceAndPath(com.murimblock.Murimblock.MOD_ID, "mirror_lake_slow");
    /** Night of the Full Moon: its reach, the moon's height, the night's length, its volleys of beams. */
    public static final double ULT_RADIUS = 12, MOON_HEIGHT = 15;
    public static final int NIGHT_TICKS = 150, VOLLEYS = 8, VOLLEY_GAP = 9, BEAMS_A_VOLLEY = 4;
    public static final float BEAM_SHARE = 0.8F, MOON_SIZE = 4.0F;

    private CrescentMoonHalberd() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.CRESCENT_MOON_HALBERD);
    }

    /** Whether this player fights with Waxing Moon: the Crescent Moon Halberd is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.CRESCENT_MOON_HALBERD::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.CRESCENT_MOON_HALBERD::equals).isPresent();
    }

    public static boolean holdsHalberd(Player player) {
        return CelestialPeakSpear.holdsSpear(player);
    }

    // ---------------------------------------------------------------- swings

    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (CelestialPeakSpear.isStrike(a)) return stanceActive(player) && holdsHalberd(player) ? MartialArts.WAXING_MOON.id() : null;
        if (a == MurimAnimations.HALBERD_MOONLIT_CIRCLE) return MartialArts.MOONLIT_CIRCLE.id();
        return null;
    }

    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return CelestialPeakSpear.isStrike(a);
    }

    // ---------------------------------------------------------------- Waxing Moon

    private record Moon(int phases, long last) { }

    private static final Map<UUID, Moon> MOONS = new HashMap<>();

    /** The phases of this player's moon now (0 when they have waned). */
    public static int phases(ServerPlayer player) {
        Moon m = MOONS.get(player.getUUID());
        return m == null || player.level().getGameTime() - m.last > PHASE_TICKS ? 0 : m.phases;
    }

    private static void setPhases(ServerPlayer player, int phases) {
        MOONS.put(player.getUUID(), new Moon(phases, player.level().getGameTime()));
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new CrescentMoonPayloads.Phases(phases));
    }

    public static void setPhasesForTests(ServerPlayer player, int phases) {
        setPhases(player, phases);
    }

    /** A strike begins: with the moon full, a crescent of light flies off the halberd. */
    static void strikeBegan(ServerPlayer player) {
        if (phases(player) < PHASES) return;
        setPhases(player, 0);
        Vec3 dir = flatLook(player);
        launch(player, chest(player).add(dir.scale(1.0)), dir, TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, PASSIVE_SHARE));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
    }

    /** A strike of the passive landed on an enemy: the moon fills a phase. */
    private static void strikeLanded(ServerPlayer player, LivingEntity target) {
        int phases = Math.min(PHASES, phases(player) + 1);
        setPhases(player, phases);
        ServerLevel level = player.serverLevel();
        Vec3 tip = chest(player).add(flatLook(player).scale(2.0));
        level.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.88F, 1.0F), 1.0F), tip.x, tip.y, tip.z, 2 + phases, 0.2, 0.2, 0.2, 0);
        if (phases == PHASES) {
            level.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 6, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    /** What one of the art's hits does besides its damage. */
    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean enemy) {
        if (isStrike(animation)) {
            if (enemy) strikeLanded(player, target);
            return;
        }
        if (animation == MurimAnimations.HALBERD_MOONLIT_CIRCLE && enemy)
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, CIRCLE_SLOW));
    }

    // ---------------------------------------------------------------- the crescents of light

    private static final class Crescent {
        final MoonQiEntity view;
        final Vec3 dir;
        Vec3 at;
        double travelled;
        final float damage;
        final Set<UUID> struck = new HashSet<>();

        Crescent(MoonQiEntity view, Vec3 at, Vec3 dir, float damage) {
            this.view = view;
            this.at = at;
            this.dir = dir;
            this.damage = damage;
        }
    }

    private static final Map<UUID, List<Crescent>> CRESCENTS = new HashMap<>();

    /** A crescent flies off from `from` along `dir` (flat or not), cutting all it passes through. */
    static void launch(ServerPlayer player, Vec3 from, Vec3 dir, float damage) {
        ServerLevel level = player.serverLevel();
        MoonQiEntity view = MurimEntities.MOON_QI.get().create(level);
        if (view == null) return;
        view.setPos(from.x, from.y, from.z);
        view.shape(MoonQiEntity.CRESCENT, 1.0F, 1.0F, 3, faceAlong(dir), (int) Math.ceil(CRESCENT_RANGE / CRESCENT_SPEED) + MoonQiEntity.FADE_TICKS + 2);
        level.addFreshEntity(view);
        CRESCENTS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new Crescent(view, from, dir.normalize(), damage));
        level.playSound(null, BlockPos.containing(from), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 0.7F, 1.7F);
    }

    /** The turn that lays the crescent flat and points its edge along `dir`. */
    private static Quaternionf faceAlong(Vec3 dir) {
        float yaw = (float) Math.atan2(dir.x, dir.z), pitch = (float) -Math.asin(Math.max(-1, Math.min(1, dir.y)));
        return new Quaternionf().rotateY(yaw).rotateX(pitch);
    }

    /** One tick of each of a player's crescents: on along its line, cutting what it meets once, broken by a wall. */
    private static void tickCrescents(ServerPlayer player) {
        List<Crescent> list = CRESCENTS.get(player.getUUID());
        if (list == null) return;
        ServerLevel level = player.serverLevel();
        for (Iterator<Crescent> it = list.iterator(); it.hasNext(); ) {
            Crescent c = it.next();
            Vec3 next = c.at.add(c.dir.scale(CRESCENT_SPEED));
            var hit = level.clip(new ClipContext(c.at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            boolean wall = hit.getType() != HitResult.Type.MISS;
            Vec3 end = wall ? hit.getLocation() : next;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c.at, end).inflate(CRESCENT_WIDTH),
                    e -> e != player && e.isAlive() && !c.struck.contains(e.getUUID()))) {
                Vec3 centre = e.getBoundingBox().getCenter();
                Vec3 seg = end.subtract(c.at);
                double t = seg.lengthSqr() < 1e-6 ? 0 : Math.max(0, Math.min(1, centre.subtract(c.at).dot(seg) / seg.lengthSqr()));
                if (centre.distanceTo(c.at.add(seg.scale(t))) > CRESCENT_WIDTH + e.getBbWidth() / 2) continue;
                c.struck.add(e.getUUID());
                if (blow(player, e, c.damage)) {
                    push(e, c.dir.multiply(1, 0, 1).scale(0.5), 0.15);
                    level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y, centre.z, 8, 0.2, 0.3, 0.2, 0.06);
                    CrescentMoonFx.flash(level, centre, 5);
                }
            }
            c.at = end;
            c.travelled += end.distanceTo(c.at.subtract(c.dir.scale(0)));
            c.travelled += CRESCENT_SPEED;
            if (c.view.isAlive()) c.view.setPos(c.at.x, c.at.y, c.at.z);
            if (wall || c.travelled >= CRESCENT_RANGE) {
                if (wall) {
                    level.sendParticles(ParticleTypes.SPLASH, c.at.x, c.at.y, c.at.z, 20, 0.4, 0.3, 0.4, 0.1);
                    CrescentMoonFx.flash(level, c.at, 4);
                }
                if (c.view.isAlive()) c.view.liveFor(MoonQiEntity.FADE_TICKS);
                it.remove();
            }
        }
        if (list.isEmpty()) CRESCENTS.remove(player.getUUID());
    }

    /** Tests: how many crescents this player has flying. */
    public static int crescentsForTests(ServerPlayer player) {
        List<Crescent> list = CRESCENTS.get(player.getUUID());
        return list == null ? 0 : list.size();
    }

    // ---------------------------------------------------------------- techniques

    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (ULTS.containsKey(player.getUUID())) return false;
        if (technique == MartialArts.MOONLIT_CIRCLE) {
            play(player, patch, MurimAnimations.HALBERD_MOONLIT_CIRCLE, technique);
            MartialCombat.later(player, ticks(MurimAnimations.HALBERD_CIRCLE_FROM) + 3, () -> ring(player));
            return true;
        }
        if (technique == MartialArts.TIDE_HOOK) {
            play(player, patch, MurimAnimations.HALBERD_TIDE_HOOK, technique);
            MartialCombat.later(player, ticks(MurimAnimations.HALBERD_HOOK_CATCH), () -> hook(player));
            return true;
        }
        if (technique == MartialArts.FALLING_CRESCENT) {
            play(player, patch, MurimAnimations.HALBERD_FALLING_CRESCENT, technique);
            MartialCombat.later(player, ticks(MurimAnimations.HALBERD_CRESCENT_SLASH), () -> fan(player));
            return true;
        }
        if (technique == MartialArts.NIGHT_OF_THE_FULL_MOON) return fullMoon(player, patch);
        return false;
    }

    private static int ticks(float seconds) {
        return Math.round(seconds * 20);
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation, MartialArts.Technique technique) {
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    // ---------------------------------------------------------------- 1. Moonlit Circle

    private record Ring(MoonQiEntity view, Vec3 centre, long until) { }

    private static final Map<UUID, Ring> RINGS = new HashMap<>();

    /** The ring of moon-water laid on the ground round the body. */
    static void ring(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Ring old = RINGS.remove(player.getUUID());
        if (old != null && old.view.isAlive()) old.view.liveFor(MoonQiEntity.FADE_TICKS);
        Vec3 centre = player.position();
        MoonQiEntity view = MurimEntities.MOON_QI.get().create(level);
        if (view != null) {
            view.setPos(centre.x, centre.y + 0.05, centre.z);
            view.shape(MoonQiEntity.RING, 1.0F, 1.0F, 8, new Quaternionf(), CIRCLE_TICKS);
            level.addFreshEntity(view);
            RINGS.put(player.getUUID(), new Ring(view, centre, level.getGameTime() + CIRCLE_TICKS));
        }
        level.sendParticles(ParticleTypes.SPLASH, centre.x, centre.y + 0.2, centre.z, 80, CIRCLE_RADIUS * 0.6, 0.1, CIRCLE_RADIUS * 0.6, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.8F, 1.3F);
        CrescentMoonFx.flash(level, centre.add(0, 0.5, 0), 8);
    }

    /** The ring's tick: the enemies standing in it slowed, a drop of moon-water rising here and there. */
    private static void tickRing(ServerPlayer player) {
        Ring r = RINGS.get(player.getUUID());
        if (r == null) return;
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() >= r.until || !r.view.isAlive()) {
            RINGS.remove(player.getUUID());
            return;
        }
        if (level.getGameTime() % 5 != 0) return;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(r.centre, r.centre).inflate(CIRCLE_RADIUS, 2, CIRCLE_RADIUS),
                e -> e != player && e.isAlive() && MartialCombat.isEnemy(e, player))) {
            if (e.position().subtract(r.centre).multiply(1, 0, 1).length() > CIRCLE_RADIUS) continue;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, CIRCLE_SLOW));
            level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 0.1, e.getZ(), 4, 0.3, 0, 0.3, 0.05);
        }
        double a = level.random.nextDouble() * Math.PI * 2, d = level.random.nextDouble() * CIRCLE_RADIUS;
        level.sendParticles(ParticleTypes.END_ROD, r.centre.x + Math.cos(a) * d, r.centre.y + 0.2, r.centre.z + Math.sin(a) * d, 1, 0, 0.05, 0, 0.02);
    }

    /** Whether a ring of this player's is on the ground now (tests). */
    public static boolean ringForTests(ServerPlayer player) {
        return RINGS.containsKey(player.getUUID());
    }

    // ---------------------------------------------------------------- 2. Tide Hook

    /** The hook goes out: the foe ahead caught; pulled in to the halberd, or tripped if too heavy. */
    static void hook(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = AzureCloudSword.targetAhead(player, HOOK_REACH);
        Vec3 tip = chest(player).add(flatLook(player).scale(target == null ? HOOK_REACH * 0.6 : 1.0));
        Vec3 to = target == null ? chest(player).add(flatLook(player).scale(HOOK_REACH)) : target.getBoundingBox().getCenter();
        // The tether of moonlight from the halberd to what it caught.
        for (double s = 0; s <= 1; s += 0.08) {
            Vec3 p = tip.lerp(to, s);
            level.sendParticles(new DustParticleOptions(new Vector3f(0.6F, 0.85F, 1.0F), 0.9F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        if (target == null || !MartialCombat.isEnemy(target, player)) return;
        boolean landed = blow(player, target, TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, HOOK_SHARE));
        if (!landed) return;
        MartialProgress.addMastery(player, art(), MartialArts.TIDE_HOOK.id(), 1);
        level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
        CrescentMoonFx.flash(level, to, 5);
        if (heavy(target)) {
            // Too heavy to come: its legs swept from under it.
            stun(target, StunType.KNOCKDOWN);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 0.2, target.getZ(), 30, 0.5, 0.1, 0.5, 0.1);
            return;
        }
        // Pulled in: carried to a block and a half before the halberd over a few ticks, then held a moment.
        for (int i = 1; i <= HOOK_PULL_TICKS; i++) {
            int left = HOOK_PULL_TICKS - i + 1;
            MartialCombat.later(player, ticks(MurimAnimations.HALBERD_HOOK_PULL - MurimAnimations.HALBERD_HOOK_CATCH) + i, () -> {
                Vec3 stop = player.position().add(flatLook(player).scale(1.6));
                Vec3 step = new Vec3(stop.x, target.getY(), stop.z).subtract(target.position()).scale(1.0 / left);
                carryBy(target, step);
            });
        }
        stun(target, StunType.LONG);
    }

    static boolean heavy(LivingEntity e) {
        return e.getMaxHealth() >= HEAVY_HEALTH || e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 0.5;
    }

    private static void stun(LivingEntity e, StunType type) {
        LivingEntityPatch<?> patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(e, LivingEntityPatch.class);
        if (patch != null) patch.applyStun(type, type == StunType.KNOCKDOWN ? 2.0F : 0.8F);
    }

    // ---------------------------------------------------------------- 3. Falling Crescent

    /** The slash ends: three crescents fly off in a fan, along the look (a little up or down if the player looks so). */
    static void fan(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        float damage = TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, FALLING_SHARE);
        for (int k = -1; k <= 1; k++) {
            float yaw = player.getYRot() + k * FAN;
            Vec3 flat = Vec3.directionFromRotation(0, yaw);
            Vec3 dir = new Vec3(flat.x, Math.max(-0.3, Math.min(0.3, look.y)), flat.z).normalize();
            launch(player, chest(player).add(flat.scale(1.2)), dir, damage);
        }
        MartialProgress.addMastery(player, art(), MartialArts.FALLING_CRESCENT.id(), 1);
        CrescentMoonFx.flash(player.serverLevel(), chest(player).add(flatLook(player).scale(1.5)), 9);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.2F);
    }

    // ---------------------------------------------------------------- 4. Mirror Lake Stance

    private static final class Mirror {
        final long since;
        final MoonQiEntity view;

        Mirror(long since, MoonQiEntity view) {
            this.since = since;
            this.view = view;
        }
    }

    private static final Map<UUID, Mirror> MIRRORS = new HashMap<>();
    private static final Map<UUID, Long> MIRROR_ENDED = new HashMap<>();

    public static boolean mirroring(Player player) {
        return MIRRORS.containsKey(player.getUUID());
    }

    /** The key went down: the halberd upright, the mirror of water before the body. Its Qi is paid by the caller. */
    static boolean startMirror(ServerPlayer player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (patch == null || mirroring(player) || MurimAnimations.HALBERD_MIRROR == null || !holdsHalberd(player)) return false;
        patch.playAnimationSynchronized(MurimAnimations.HALBERD_MIRROR, 0.0F);
        ServerLevel level = player.serverLevel();
        MoonQiEntity view = MurimEntities.MOON_QI.get().create(level);
        if (view != null) {
            Vec3 at = mirrorAt(player);
            view.setPos(at.x, at.y, at.z);
            view.shape(MoonQiEntity.MIRROR, 1.0F, 1.0F, 4, mirrorTurn(player), MartialArts.MIRROR_MAX_TICKS + 40);
            level.addFreshEntity(view);
        }
        MIRRORS.put(player.getUUID(), new Mirror(level.getGameTime(), view));
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(MIRROR_SLOW))
            speed.addTransientModifier(new AttributeModifier(MIRROR_SLOW, -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.7F, 1.5F);
        CrescentMoonFx.flash(level, mirrorAt(player), 4);
        return true;
    }

    /** Where the mirror stands: a block before the chest. */
    private static Vec3 mirrorAt(ServerPlayer player) {
        return player.position().add(0, 0.55, 0).add(flatLook(player).scale(0.95));
    }

    /** The mirror turned to face where the player faces (its disc in the plane across the look). */
    private static Quaternionf mirrorTurn(ServerPlayer player) {
        return new Quaternionf().rotateY((float) Math.toRadians(-player.getYRot()));
    }

    /** The stance ends (the key let go, the Qi or the 3 s run out, the halberd put away): back on guard, its cooldown from now. */
    static void endMirror(ServerPlayer player, boolean playEnd) {
        Mirror m = MIRRORS.remove(player.getUUID());
        if (m == null) return;
        MIRROR_ENDED.put(player.getUUID(), player.level().getGameTime());
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(MIRROR_SLOW);
        if (m.view != null && m.view.isAlive()) m.view.liveFor(MoonQiEntity.FADE_TICKS);
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        if (playEnd && patch != null && player.isAlive() && MurimAnimations.HALBERD_MIRROR_END != null)
            patch.playAnimationSynchronized(MurimAnimations.HALBERD_MIRROR_END, 0.0F);
        MartialService.startCooldown(player, MartialArts.MIRROR_LAKE_STANCE);
    }

    /** Whether the stance ended by itself a moment ago (its key's release, arriving after, has nothing to do). */
    static boolean mirrorJustEnded(ServerPlayer player) {
        Long at = MIRROR_ENDED.get(player.getUUID());
        return at != null && player.level().getGameTime() - at <= 20;
    }

    private static void tickMirror(ServerPlayer player) {
        Mirror m = MIRRORS.get(player.getUUID());
        if (m == null) return;
        long held = player.level().getGameTime() - m.since;
        double perTick = MartialArts.MIRROR_QI_PER_SECOND / 20.0;
        boolean qiOut = com.murimblock.qi.QiService.getQi(player) < perTick;
        if (!qiOut && held > 0) com.murimblock.qi.QiService.removeQi(player, perTick);
        if (m.view != null && m.view.isAlive()) {
            Vec3 at = mirrorAt(player);
            m.view.setPos(at.x, at.y, at.z);
            m.view.turnTo(mirrorTurn(player));
        }
        if (held >= MartialArts.MIRROR_MAX_TICKS || qiOut || !holdsHalberd(player) || !player.isAlive()
                || com.murimblock.combat.knockdown.KnockdownService.isHindered(player))
            endMirror(player, true);
    }

    /** What reaches the mirror from the front is stopped, and sent back to its sender as a crescent. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !mirroring(player)) return;
        DamageSource source = event.getSource();
        Entity by = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition() : by != null ? by.position() : null;
        if (from == null || !inFront(player, from)) return;
        event.setCanceled(true);
        reflect(player, by, from);
    }

    /** The push of a blow the mirror stopped is stopped too. */
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !mirroring(player)) return;
        Long at = REFLECTED.get(player.getUUID());
        if (at != null && player.level().getGameTime() - at <= 1) event.setCanceled(true);
    }

    /** A projectile reaching the mirror from the front sinks into it, and a crescent goes back to whoever sent it. */
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player) || !mirroring(player)) return;
        Projectile p = event.getProjectile();
        Vec3 from = p.getDeltaMovement().lengthSqr() > 1e-6 ? p.position().subtract(p.getDeltaMovement().normalize()) : p.position();
        if (!inFront(player, from)) return;
        event.setCanceled(true);
        p.discard();
        reflect(player, p.getOwner(), from);
    }

    private static final Map<UUID, Long> REFLECTED = new HashMap<>();

    private static void reflect(ServerPlayer player, @Nullable Entity sender, Vec3 from) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long last = REFLECTED.get(player.getUUID());
        REFLECTED.put(player.getUUID(), now);
        Vec3 at = mirrorAt(player);
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 24, 0.4, 0.5, 0.1, 0.15);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.2F, 0.8F);
        // One crescent a tick at most, whatever lands at once.
        if (last != null && last == now) return;
        Vec3 target = sender != null ? sender.position().add(0, sender.getBbHeight() * 0.5, 0) : from;
        Vec3 dir = target.subtract(at);
        if (dir.lengthSqr() < 1e-4) dir = flatLook(player);
        launch(player, at, dir.normalize(), TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, MIRROR_SHARE));
        MartialProgress.addMastery(player, art(), MartialArts.MIRROR_LAKE_STANCE.id(), 1);
        CrescentMoonFx.flash(level, at, 6);
    }

    private static boolean inFront(ServerPlayer player, Vec3 from) {
        Vec3 to = from.subtract(player.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4) return true;
        return to.normalize().dot(flatLook(player)) > Math.cos(Math.toRadians(70));
    }

    // ---------------------------------------------------------------- the ultimate: Night of the Full Moon

    private static final class Ult {
        final Vec3 centre;
        final MoonQiEntity moon;
        int ticks;
        int volleys;

        Ult(Vec3 centre, MoonQiEntity moon) {
            this.centre = centre;
            this.moon = moon;
        }
    }

    private static final Map<UUID, Ult> ULTS = new HashMap<>();

    /** The halberd raised to the sky: night falls round, the full moon rises over the cultivator. */
    static boolean fullMoon(ServerPlayer player, PlayerPatch<?> patch) {
        ServerLevel level = player.serverLevel();
        patch.playAnimationSynchronized(MurimAnimations.HALBERD_FULL_MOON, 0.0F);
        Vec3 centre = player.position();
        MoonQiEntity moon = MurimEntities.MOON_QI.get().create(level);
        if (moon != null) {
            moon.setPos(centre.x, centre.y + 4, centre.z);
            moon.shape(MoonQiEntity.MOON, MOON_SIZE, 1.0F, 30, new Quaternionf(), NIGHT_TICKS + 30);
            level.addFreshEntity(moon);
        }
        ULTS.put(player.getUUID(), new Ult(centre, moon));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 2, false, false));
        CrescentMoonFx.night(level, centre, (float) (ULT_RADIUS * 2.5), NIGHT_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2.0F, 0.5F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("technique.murimblock.night_of_the_full_moon.called")
                .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD), true);
        return true;
    }

    private static void ultTick(ServerPlayer player, Ult u) {
        ServerLevel level = player.serverLevel();
        u.ticks++;
        // The moon rises over the first second and a half, then hangs over the place.
        double rise = Math.min(1.0, u.ticks / 30.0);
        double y = u.centre.y + 4 + (MOON_HEIGHT - 4) * (1 - (1 - rise) * (1 - rise));
        if (u.moon != null && u.moon.isAlive()) u.moon.setPos(u.centre.x, y, u.centre.z);
        if (u.ticks == 30) CrescentMoonFx.moonlight(level, new Vec3(u.centre.x, y, u.centre.z), (float) (ULT_RADIUS * 2.2), NIGHT_TICKS - 30);
        if (u.ticks % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, u.centre.x, y, u.centre.z, 4, MOON_SIZE, MOON_SIZE, MOON_SIZE, 0.01);
        int first = ticks(MurimAnimations.HALBERD_MOON_STRIKE);
        if (u.ticks >= first && (u.ticks - first) % VOLLEY_GAP == 0 && u.volleys < VOLLEYS) {
            u.volleys++;
            volley(player, u, y);
        }
        if (u.volleys >= VOLLEYS && u.ticks >= first + VOLLEYS * VOLLEY_GAP + 10) {
            ULTS.remove(player.getUUID());
            if (u.moon != null && u.moon.isAlive()) u.moon.liveFor(20);
        }
    }

    /** A volley of beams from the moon onto the nearest enemies round about (onto the ground where there are none). */
    private static void volley(ServerPlayer player, Ult u, double moonY) {
        ServerLevel level = player.serverLevel();
        List<LivingEntity> foes = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(u.centre, u.centre).inflate(ULT_RADIUS, 6, ULT_RADIUS),
                e -> e != player && e.isAlive() && MartialCombat.isEnemy(e, player) && e.position().subtract(u.centre).multiply(1, 0, 1).length() <= ULT_RADIUS));
        foes.sort((a, b) -> Double.compare(a.distanceToSqr(u.centre), b.distanceToSqr(u.centre)));
        float damage = TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, BEAM_SHARE);
        int beams = 0;
        for (LivingEntity e : foes) {
            if (beams >= BEAMS_A_VOLLEY) break;
            beam(level, e.position(), moonY);
            e.invulnerableTime = 0;
            if (blow(player, e, damage)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                if (u.volleys == 1) MartialProgress.addMastery(player, art(), MartialArts.NIGHT_OF_THE_FULL_MOON.id(), 1);
            }
            beams++;
        }
        for (; beams < 2; beams++) {
            double a = level.random.nextDouble() * Math.PI * 2, d = 2 + level.random.nextDouble() * (ULT_RADIUS - 2);
            beam(level, u.centre.add(Math.cos(a) * d, 0, Math.sin(a) * d), moonY);
        }
        level.playSound(null, BlockPos.containing(u.centre), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4F, 1.0F + level.random.nextFloat() * 0.6F);
    }

    /** A beam of moonlight from the moon down to `ground`. */
    private static void beam(ServerLevel level, Vec3 ground, double moonY) {
        MoonQiEntity beam = MurimEntities.MOON_QI.get().create(level);
        if (beam != null) {
            beam.setPos(ground.x, ground.y, ground.z);
            beam.shape(MoonQiEntity.BEAM, 1.3F, (float) Math.max(2, moonY - ground.y), 2, new Quaternionf(), 10);
            level.addFreshEntity(beam);
        }
        level.sendParticles(ParticleTypes.END_ROD, ground.x, ground.y + 0.3, ground.z, 14, 0.4, 0.2, 0.4, 0.08);
        level.sendParticles(ParticleTypes.SPLASH, ground.x, ground.y + 0.1, ground.z, 20, 0.6, 0.1, 0.6, 0.1);
        CrescentMoonFx.flash(level, ground.add(0, 1, 0), 6);
    }

    public static boolean calling(Player player) {
        return ULTS.containsKey(player.getUUID());
    }

    /** Tests: runs the ultimate to its end now. */
    public static void finishUltForTests(ServerPlayer player) {
        for (int i = 0; i < 200 && ULTS.containsKey(player.getUUID()); i++) ultTick(player, ULTS.get(player.getUUID()));
    }

    // ---------------------------------------------------------------- blows and moves

    /** A blow of the art that is not an animation's: damage from the player; a guard raised in time stops it. */
    static boolean blow(ServerPlayer player, LivingEntity e, float damage) {
        if (!MartialCombat.isEnemy(e, player) || MurimGuardSkill.parriedWithin(e, 6)) return false;
        e.invulnerableTime = 0;
        boolean hurt = e.hurt(player.damageSources().playerAttack(player), damage);
        if (hurt) e.hurtMarked = true;
        return hurt;
    }

    private static Vec3 flatLook(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot()).normalize();
    }

    private static Vec3 chest(ServerPlayer player) {
        return player.position().add(0, 1.2, 0);
    }

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

    // ---------------------------------------------------------------- ticks and state

    static void tick(ServerPlayer player) {
        tickCrescents(player);
        tickRing(player);
        tickMirror(player);
        Ult u = ULTS.get(player.getUUID());
        if (u != null) ultTick(player, u);
    }

    static void forget(@Nullable UUID player) {
        if (player == null) {
            CRESCENTS.values().forEach(l -> l.forEach(c -> c.view.discard()));
            CRESCENTS.clear();
            RINGS.values().forEach(r -> r.view.discard());
            RINGS.clear();
            MIRRORS.values().forEach(m -> { if (m.view != null) m.view.discard(); });
            MIRRORS.clear();
            ULTS.values().forEach(u -> { if (u.moon != null) u.moon.discard(); });
            ULTS.clear();
            MOONS.clear();
            MIRROR_ENDED.clear();
            REFLECTED.clear();
            return;
        }
        List<Crescent> c = CRESCENTS.remove(player);
        if (c != null) c.forEach(x -> x.view.discard());
        Ring r = RINGS.remove(player);
        if (r != null) r.view.discard();
        Mirror m = MIRRORS.remove(player);
        if (m != null && m.view != null) m.view.discard();
        Ult u = ULTS.remove(player);
        if (u != null && u.moon != null) u.moon.discard();
        MOONS.remove(player);
        MIRROR_ENDED.remove(player);
        REFLECTED.remove(player);
    }
}
