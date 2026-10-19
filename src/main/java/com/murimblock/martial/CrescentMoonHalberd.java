package com.murimblock.martial;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.MurimEntities;
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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Crescent Moon Halberd in play (docs/combat/styles/crescent-moon-halberd.md): an orthodox art of the halberd, of the
 * moon and water, an art of cunning (Enzo's choice of 19/10/2026, Series D: "the deceiving reflection"). Its reflections
 * of water (ReflectionEntity) are its owner's own body in translucent water-blue light: mobs turn on them, the first blow
 * shatters them. A spear in hand (any Epic Fight spear; the Crescent Moon Halberd is its own). Its M1 are Epic Fight's
 * spear strikes; its passive, <b>Mirror Image</b>: a dodge leaves a reflection where it began. Its techniques:
 * <ul>
 *   <li><b>Twin Reflection</b>: a reflection steps out on the far side of the foe ahead and strikes with the cultivator,
 *   from behind it (its guard broken), then stays;</li>
 *   <li><b>Shattered Mirror</b>: the halberd turned round, every reflection of the cultivator bursts into shards of
 *   water-glass that cut all round it (with none, the shards burst from the cultivator, weaker);</li>
 *   <li><b>Swap</b>: the cultivator and their nearest reflection change places;</li>
 *   <li><b>Hall of Mirrors</b>: four reflections round the cultivator, each going for a foe of its own, for six seconds;</li>
 *   <li><b>Night of the Full Moon</b>, the ultimate: night falls round, a full moon rises over the cultivator's head and
 *   its beams strike the enemies round about.</li>
 * </ul>
 * The shards and beams shine with Photon's lights (CrescentMoonFx); the moon is a GeckoLib model (MoonQiEntity).
 */
public final class CrescentMoonHalberd {
    /** Mirror Image: how long the reflection a dodge leaves lasts, the time between two, how far mobs turn on it. */
    public static final int IMAGE_TICKS = 80, IMAGE_COOLDOWN = 100;
    public static final double LURE_REACH = 14;
    /** Reflections a cultivator may have at once (the oldest shatters). */
    public static final int MAX_REFLECTIONS = 6;
    /** Twin Reflection: how far it looks for its foe, its blow, how long the reflection stays after. */
    public static final double TWIN_REACH = 9;
    public static final float TWIN_SHARE = 1.2F;
    public static final int TWIN_TICKS = 100;
    /** Shattered Mirror: the shards' reach and blow round each reflection, and round the cultivator when there is none. */
    public static final double SHARD_RADIUS = 3.2;
    public static final float SHARD_SHARE = 1.4F, SELF_SHARD_SHARE = 0.8F;
    /** Swap: how far a reflection may be. */
    public static final double SWAP_REACH = 24;
    /** Hall of Mirrors: its four reflections' time, their blow and how often, how far they look. */
    public static final int HALL_TICKS = 120, HALL_STRIKE_GAP = 20;
    public static final float HALL_SHARE = 0.45F;
    public static final double HALL_REACH = 14;
    /** Night of the Full Moon: its reach, the moon's height, the night's length, its volleys of beams. */
    public static final double ULT_RADIUS = 12, MOON_HEIGHT = 15;
    public static final int NIGHT_TICKS = 150, VOLLEYS = 8, VOLLEY_GAP = 9, BEAMS_A_VOLLEY = 4;
    public static final float BEAM_SHARE = 0.8F, MOON_SIZE = 4.0F;

    private CrescentMoonHalberd() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.CRESCENT_MOON_HALBERD);
    }

    /** Whether this player fights with Mirror Image: the Crescent Moon Halberd is their main style. */
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
        if (CelestialPeakSpear.isStrike(a)) return stanceActive(player) && holdsHalberd(player) ? MartialArts.MIRROR_IMAGE.id() : null;
        if (a == MurimAnimations.HALBERD_MOONLIT_CIRCLE) return MartialArts.SHATTERED_MIRROR.id();
        return null;
    }

    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return CelestialPeakSpear.isStrike(a);
    }

    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean enemy) {
    }

    // ---------------------------------------------------------------- the reflections

    private static final Map<UUID, List<ReflectionEntity>> REFLECTIONS = new HashMap<>();

    /** This player's reflections still standing, oldest first. */
    public static List<ReflectionEntity> reflections(ServerPlayer player) {
        List<ReflectionEntity> list = REFLECTIONS.get(player.getUUID());
        if (list == null) return List.of();
        list.removeIf(r -> !r.isAlive());
        return list;
    }

    /** A reflection of the player at `at`, facing `yaw`, for `ticks`; mobs round turn on it. */
    static ReflectionEntity reflect(ServerPlayer player, Vec3 at, float yaw, int ticks, boolean lure) {
        ServerLevel level = player.serverLevel();
        List<ReflectionEntity> list = REFLECTIONS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        list.removeIf(r -> !r.isAlive());
        while (list.size() >= MAX_REFLECTIONS) list.removeFirst().shatter();
        ReflectionEntity r = MurimEntities.REFLECTION.get().create(level);
        if (r == null) return null;
        r.moveTo(at.x, at.y, at.z, yaw, 0);
        r.setYHeadRot(yaw);
        r.setYBodyRot(yaw);
        r.owned(player, ticks);
        level.addFreshEntity(r);
        list.add(r);
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 30, 0.35, 0.1, 0.35, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 6, 0.25, 0.6, 0.25, 0.02);
        level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.5F, 1.6F);
        if (lure) lure(player, r);
        return r;
    }

    /** The mobs round that were after the player turn on the reflection. */
    private static void lure(ServerPlayer player, ReflectionEntity r) {
        for (Mob m : player.serverLevel().getEntitiesOfClass(Mob.class, r.getBoundingBox().inflate(LURE_REACH),
                m -> m.isAlive() && !(m instanceof ReflectionEntity) && m.getTarget() == player)) m.setTarget(r);
    }

    // ---------------------------------------------------------------- Mirror Image

    private static final Map<UUID, Long> LAST_IMAGE = new HashMap<>();

    static boolean isDodge(AnimationAccessor<? extends StaticAnimation> a) {
        return a != null && (a == Animations.BIPED_ROLL_FORWARD || a == Animations.BIPED_ROLL_BACKWARD || a == Animations.BIPED_STEP_FORWARD
                || a == Animations.BIPED_STEP_BACKWARD || a == Animations.BIPED_STEP_LEFT || a == Animations.BIPED_STEP_RIGHT);
    }

    /** A dodge begins: in the stance, the halberd in hand, a reflection stays where it began (not more than one every 5 s). */
    static void dodged(ServerPlayer player) {
        if (!stanceActive(player) || !holdsHalberd(player)) return;
        long now = player.level().getGameTime();
        Long last = LAST_IMAGE.get(player.getUUID());
        if (last != null && now - last < IMAGE_COOLDOWN) return;
        LAST_IMAGE.put(player.getUUID(), now);
        reflect(player, player.position(), player.getYRot(), IMAGE_TICKS, true);
    }

    public static void dodgedForTests(ServerPlayer player) {
        LAST_IMAGE.remove(player.getUUID());
        dodged(player);
    }

    // ---------------------------------------------------------------- techniques

    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (ULTS.containsKey(player.getUUID())) return false;
        if (technique == MartialArts.TWIN_REFLECTION) return twin(player, patch);
        if (technique == MartialArts.SHATTERED_MIRROR) {
            play(player, patch, MurimAnimations.HALBERD_MOONLIT_CIRCLE, technique);
            MartialCombat.later(player, ticks(MurimAnimations.HALBERD_CIRCLE_FROM) + 4, () -> shatterAll(player));
            return true;
        }
        if (technique == MartialArts.SWAP) return swap(player, patch);
        if (technique == MartialArts.HALL_OF_MIRRORS) return hall(player, patch);
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

    // ---------------------------------------------------------------- 1. Twin Reflection

    /** The great slash; the reflection behind the foe ahead, striking with it. */
    static boolean twin(ServerPlayer player, PlayerPatch<?> patch) {
        LivingEntity target = AzureCloudSword.targetAhead(player, TWIN_REACH);
        Vec3 dir = flatLook(player);
        Vec3 at;
        float yaw;
        if (target != null) {
            Vec3 across = target.position().subtract(player.position()).multiply(1, 0, 1);
            Vec3 d = across.lengthSqr() < 1e-4 ? dir : across.normalize();
            at = target.position().add(d.scale(1.4 + target.getBbWidth() / 2));
            yaw = player.getYRot() + 180;
        } else {
            at = player.position().add(dir.scale(3));
            yaw = player.getYRot();
        }
        patch.playAnimationSynchronized(MurimAnimations.HALBERD_FALLING_CRESCENT, 0.0F);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        ReflectionEntity r = reflect(player, at, yaw, TWIN_TICKS, false);
        MartialCombat.later(player, ticks(MurimAnimations.HALBERD_CRESCENT_SLASH), () -> {
            if (target == null || !target.isAlive() || r == null || !r.isAlive()) return;
            MartialCombat.breakGuard(target);
            if (blow(player, target, TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, TWIN_SHARE))) {
                push(target, target.position().subtract(r.position()).multiply(1, 0, 1).normalize().scale(0.5), 0.1);
                MartialProgress.addMastery(player, art(), MartialArts.TWIN_REFLECTION.id(), 1);
            }
            Vec3 c = target.getBoundingBox().getCenter();
            player.serverLevel().sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 25, 0.3, 0.4, 0.3, 0.2);
            player.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            CrescentMoonFx.flash(player.serverLevel(), c, 6);
            if (target instanceof Mob m && m.getTarget() == player) m.setTarget(r);
        });
        return true;
    }

    // ---------------------------------------------------------------- 2. Shattered Mirror

    /** Every reflection bursts, cutting round itself; with none, the cultivator's own shards, weaker. */
    static void shatterAll(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<ReflectionEntity> list = new ArrayList<>(reflections(player));
        Set<UUID> struck = new HashSet<>();
        if (list.isEmpty()) {
            shards(player, player.position(), SELF_SHARD_SHARE, struck);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
                    player.getX(), player.getY() + 1, player.getZ(), 60, 1.2, 0.6, 1.2, 0.3);
            level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 1.0F);
            CrescentMoonFx.flash(level, player.position().add(0, 1, 0), 7);
            return;
        }
        for (ReflectionEntity r : list) {
            shards(player, r.position(), SHARD_SHARE, struck);
            r.shatter();
        }
        if (!struck.isEmpty()) MartialProgress.addMastery(player, art(), MartialArts.SHATTERED_MIRROR.id(), 1);
        REFLECTIONS.remove(player.getUUID());
    }

    private static void shards(ServerPlayer player, Vec3 at, float share, Set<UUID> struck) {
        ServerLevel level = player.serverLevel();
        float damage = TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, share);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SHARD_RADIUS, 2, SHARD_RADIUS),
                e -> e != player && e.isAlive() && !(e instanceof ReflectionEntity) && e.position().distanceTo(at) <= SHARD_RADIUS + e.getBbWidth() / 2)) {
            if (!struck.add(e.getUUID())) continue;
            if (blow(player, e, damage)) {
                Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
                push(e, (away.lengthSqr() < 1e-4 ? flatLook(player) : away.normalize()).scale(0.6), 0.25);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_STAINED_GLASS.defaultBlockState()), at.x, at.y + 1, at.z,
                40, SHARD_RADIUS * 0.4, 0.5, SHARD_RADIUS * 0.4, 0.4);
    }

    // ---------------------------------------------------------------- 3. Swap

    /** The cultivator and the nearest reflection change places (refused with none within reach). */
    static boolean swap(ServerPlayer player, PlayerPatch<?> patch) {
        ReflectionEntity r = reflections(player).stream().filter(x -> x.distanceTo(player) <= SWAP_REACH)
                .min(Comparator.comparingDouble(x -> x.distanceToSqr(player))).orElse(null);
        if (r == null) return false;
        ServerLevel level = player.serverLevel();
        Vec3 mine = player.position(), theirs = r.position();
        float myYaw = player.getYRot();
        player.teleportTo(level, theirs.x, theirs.y, theirs.z, r.getYRot(), player.getXRot());
        player.resetFallDistance();
        r.moveTo(mine.x, mine.y, mine.z, myYaw, 0);
        r.setYBodyRot(myYaw);
        r.setYHeadRot(myYaw);
        // What was after the cultivator is now after the reflection, there where they stood.
        lure(player, r);
        for (Vec3 p : new Vec3[]{mine, theirs}) {
            level.sendParticles(ParticleTypes.SPLASH, p.x, p.y + 0.5, p.z, 30, 0.3, 0.6, 0.3, 0.15);
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 8, 0.2, 0.5, 0.2, 0.04);
            CrescentMoonFx.flash(level, p.add(0, 1, 0), 5);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        patch.playAnimationSynchronized(MurimAnimations.HALBERD_MIRROR_END, 0.0F);
        MartialProgress.addMastery(player, art(), MartialArts.SWAP.id(), 1);
        return true;
    }

    // ---------------------------------------------------------------- 4. Hall of Mirrors

    private static final class Hall {
        final List<ReflectionEntity> mirrors;
        final long until;

        Hall(List<ReflectionEntity> mirrors, long until) {
            this.mirrors = mirrors;
            this.until = until;
        }
    }

    private static final Map<UUID, Hall> HALLS = new HashMap<>();

    /** Four reflections round the cultivator, each going for a foe of its own. */
    static boolean hall(ServerPlayer player, PlayerPatch<?> patch) {
        patch.playAnimationSynchronized(MurimAnimations.HALBERD_MIRROR, 0.0F);
        MartialCombat.later(player, 16, () -> {
            PlayerPatch<?> p = EpicFightBridge.patch(player);
            if (p != null) p.playAnimationSynchronized(MurimAnimations.HALBERD_MIRROR_END, 0.0F);
        });
        List<ReflectionEntity> mirrors = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            float yaw = player.getYRot() + 45 + i * 90;
            Vec3 at = player.position().add(Vec3.directionFromRotation(0, yaw).scale(2.2));
            ReflectionEntity r = reflect(player, at, yaw, HALL_TICKS, false);
            if (r != null) mirrors.add(r);
        }
        HALLS.put(player.getUUID(), new Hall(mirrors, player.level().getGameTime() + HALL_TICKS));
        for (ReflectionEntity r : mirrors) lure(player, r);
        CrescentMoonFx.flash(player.serverLevel(), player.position().add(0, 1, 0), 10);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    /** Every second: each reflection of the hall steps to a foe of its own and strikes it. */
    private static void tickHall(ServerPlayer player) {
        Hall h = HALLS.get(player.getUUID());
        if (h == null) return;
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        h.mirrors.removeIf(r -> !r.isAlive());
        if (now >= h.until || h.mirrors.isEmpty()) {
            HALLS.remove(player.getUUID());
            return;
        }
        if ((h.until - now) % HALL_STRIKE_GAP != 0) return;
        List<LivingEntity> foes = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(HALL_REACH, 4, HALL_REACH),
                e -> e != player && e.isAlive() && !(e instanceof ReflectionEntity) && MartialCombat.isEnemy(e, player)));
        if (foes.isEmpty()) return;
        foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        float damage = TechniquePower.damage(player, MartialArts.CRESCENT_MOON_HALBERD, HALL_SHARE);
        int i = 0;
        for (ReflectionEntity r : h.mirrors) {
            LivingEntity foe = foes.get(i++ % foes.size());
            // A step through the water to the foe's side, then the blow.
            Vec3 from = r.position(), side = foe.position().subtract(player.position()).multiply(1, 0, 1);
            Vec3 d = side.lengthSqr() < 1e-4 ? flatLook(player) : side.normalize();
            Vec3 to = foe.position().subtract(d.scale(1.3 + foe.getBbWidth() / 2)).add(new Vec3(-d.z, 0, d.x).scale((i % 2 == 0 ? 1 : -1) * 0.8));
            float yaw = (float) Math.toDegrees(Math.atan2(-(foe.getX() - to.x), foe.getZ() - to.z));
            r.moveTo(to.x, foe.getY(), to.z, yaw, 0);
            r.setYBodyRot(yaw);
            r.setYHeadRot(yaw);
            level.sendParticles(ParticleTypes.SPLASH, from.x, from.y + 0.4, from.z, 12, 0.2, 0.4, 0.2, 0.1);
            if (blow(player, foe, damage)) {
                Vec3 c = foe.getBoundingBox().getCenter();
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 14, 0.25, 0.35, 0.25, 0.15);
                if (foe instanceof Mob m) m.setTarget(r);
            }
        }
        if (now % (HALL_STRIKE_GAP * 2) == 0) MartialProgress.addMastery(player, art(), MartialArts.HALL_OF_MIRRORS.id(), 1);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    public static boolean hallForTests(ServerPlayer player) {
        return HALLS.containsKey(player.getUUID());
    }

    /** Tests: the hall strikes now. */
    public static void hallStrikeForTests(ServerPlayer player) {
        Hall h = HALLS.get(player.getUUID());
        if (h == null) return;
        HALLS.put(player.getUUID(), new Hall(h.mirrors, player.level().getGameTime() + HALL_STRIKE_GAP));
        tickHall(player);
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
        if (e instanceof ReflectionEntity || !MartialCombat.isEnemy(e, player) || MurimGuardSkill.parriedWithin(e, 6)) return false;
        e.invulnerableTime = 0;
        boolean hurt = e.hurt(player.damageSources().playerAttack(player), damage);
        if (hurt) e.hurtMarked = true;
        return hurt;
    }

    private static Vec3 flatLook(ServerPlayer player) {
        return Vec3.directionFromRotation(0, player.getYRot()).normalize();
    }

    private static void push(LivingEntity e, Vec3 across, double up) {
        double resist = Math.max(0.2, 1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        e.push(across.x * resist, up * resist, across.z * resist);
        e.hurtMarked = true;
    }

    // ---------------------------------------------------------------- ticks and state

    static void tick(ServerPlayer player) {
        tickHall(player);
        Ult u = ULTS.get(player.getUUID());
        if (u != null) ultTick(player, u);
    }

    static void forget(@Nullable UUID player) {
        if (player == null) {
            REFLECTIONS.values().forEach(l -> l.forEach(ReflectionEntity::discard));
            REFLECTIONS.clear();
            HALLS.clear();
            ULTS.values().forEach(u -> { if (u.moon != null) u.moon.discard(); });
            ULTS.clear();
            LAST_IMAGE.clear();
            return;
        }
        List<ReflectionEntity> r = REFLECTIONS.remove(player);
        if (r != null) r.forEach(ReflectionEntity::discard);
        HALLS.remove(player);
        Ult u = ULTS.remove(player);
        if (u != null && u.moon != null) u.moon.discard();
        LAST_IMAGE.remove(player);
    }
}
