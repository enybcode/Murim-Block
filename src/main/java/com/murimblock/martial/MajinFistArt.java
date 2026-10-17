package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.combat.ShatteredGround;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.majin.CruelSunEntity;
import com.murimblock.mob.majin.MajinFist;
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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Majin Fist in a player's hands (docs/combat/styles/majin-fist.md), the Son of the Demon's art, the Pride of the
 * Sun: bare hands and the sun's fire. Its M1 are Epic Fight's bare-hand strikes; its passive, Pride of the Sun, sets each
 * target that a strike lands on alight, and strikes harder by day under the open sky. Its techniques play the Son of the
 * Demon's own moves at a man's size (scripts/animation/majin-fist.cjs) and deal their blows here, on the server:
 * <ul>
 *   <li><b>Sunfire Fist</b>: the straight right, a burst of flame before the fist (twice as wide evolved: Solar Fist);</li>
 *   <li><b>Pride Flare</b>: a wave of solar flame running along the ground (farther and wider evolved: Inferno Flare);</li>
 *   <li><b>Cruel Sun</b>: a small sun grown over the palm and hurled where the player looks (two suns evolved: Twin Suns);</li>
 *   <li><b>Noon Judgment</b>: golden circles under up to five foes, then pillars of sunlight on them;</li>
 *   <li><b>Midday Meteor</b>: a leap onto where the player looks, a crater, a ring of fire running out;</li>
 *   <li><b>Grand Noon</b>, the secret: Sunfire Fist cast crouching under the noon sun, a pillar of sunlight on its target;</li>
 *   <li><b>The One</b>, the ultimate: a sun born over the body, 20 s of its strength and heat, then brought down where the
 *   player looks.</li>
 * </ul>
 * Its finish is Noon Sentence: a sun raised over the body and brought down on it. Every blow can be parried.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class MajinFistArt {
    /** Pride of the Sun: how long a strike sets alight (ticks), how much harder by day under the sky. */
    public static final int STRIKE_FIRE_TICKS = 40;
    public static final float DAYLIGHT_FACTOR = 1.15F;
    /** Sunfire Fist: its burst's reach (evolved), its share. */
    public static final double SUNFIRE_REACH = 2.5, SUNFIRE_REACH_EVOLVED = 4;
    public static final float SUNFIRE_SHARE = 1.2F;
    /** Pride Flare: its steps (evolved), how wide (evolved), its share. */
    public static final int FLARE_STEPS = 10, FLARE_STEPS_EVOLVED = 14;
    public static final double FLARE_WIDTH = 4, FLARE_WIDTH_EVOLVED = 6;
    public static final float FLARE_SHARE = 0.8F;
    /** Cruel Sun: how far it may be hurled, its burst, its share. */
    public static final double SUN_REACH = 24, SUN_RADIUS = 3.5;
    public static final float SUN_SHARE = 1.8F;
    /** Noon Judgment: how far it reaches, how many foes, its pillars' reach and share. */
    public static final double JUDGMENT_REACH = 16, PILLAR_RADIUS = 2.5;
    public static final int JUDGMENT_TARGETS = 5;
    public static final float PILLAR_SHARE = 1.5F;
    /** Midday Meteor: how far, how long in the air (ticks), its landing's crater, shares. */
    public static final double METEOR_REACH = 14, METEOR_CRATER = 3, METEOR_RADIUS = 4;
    public static final int METEOR_TICKS = 18;
    public static final float METEOR_SHARE = 1.8F, RING_SHARE = 0.7F;
    /** The One: how long, its strength, its heat's reach and share a second; the great sun's fall. */
    public static final int ONE_TICKS = 400;
    public static final double ONE_SPEED = 0.3, ONE_STRENGTH = 0.25, ONE_HEAT = 6, ONE_FALL_REACH = 20, ONE_RADIUS = 7;
    public static final float ONE_HEAT_SHARE = 0.25F, ONE_FALL_SHARE = 3.0F;
    /** Grand Noon: its pillar's reach and share. */
    public static final float GRAND_NOON_SHARE = 3.0F;
    /** The finish's sun (ticks in): raised, brought down (it kills). */
    public static final int FINISH_SUN_TICK = 12, FINISH_HIT_TICK = 32;

    private static final ResourceLocation ONE_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "majin_the_one_speed"),
            ONE_STRENGTH_ID = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "majin_the_one_strength");
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.8F, 0.25F), 1.8F);

    private MajinFistArt() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.MAJIN_FIST);
    }

    /** Whether this player fights with Pride of the Sun: the Majin Fist is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.MAJIN_FIST::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.MAJIN_FIST::equals).isPresent();
    }

    public static boolean bareHanded(Player player) {
        return player.getMainHandItem().isEmpty();
    }

    /** The main style, bare-handed: the forearms' guard, and the art's finish. */
    public static boolean guardsBareHanded(Player player) {
        return stanceActive(player) && bareHanded(player);
    }

    public static boolean finishes(ServerPlayer executor) {
        return guardsBareHanded(executor);
    }

    /** By day, under the open sky: the sun's strength. */
    public static boolean sunlit(Player player) {
        long time = player.level().getDayTime() % 24000;
        return time < 12500 && player.level().canSeeSky(player.blockPosition().above());
    }

    /** The noon sun (in game, 11:00 to 13:00) over a body under the open sky: Grand Noon. */
    public static boolean noon(Player player) {
        long time = player.level().getDayTime() % 24000;
        return time >= 5000 && time <= 7000 && player.level().canSeeSky(player.blockPosition().above());
    }

    // ---------------------------------------------------------------- swings

    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return LongFist.isStrike(a);
    }

    /** What an animation counts for in this art: the passive (bare-hand strikes in the stance) or a technique. */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (isStrike(a)) return guardsBareHanded(player) ? MartialArts.PRIDE_OF_THE_SUN.id() : null;
        if (a == MurimAnimations.MJF_SUNFIRE) return MartialArts.SUNFIRE_FIST.id();
        if (a == MurimAnimations.MJF_FLARE) return MartialArts.PRIDE_FLARE.id();
        if (a == MurimAnimations.MJF_CONJURE) return MartialArts.CRUEL_SUN.id();
        if (a == MurimAnimations.MJF_JUDGMENT) return MartialArts.NOON_JUDGMENT.id();
        return null;
    }

    /** Pride of the Sun: a strike by day under the sky lands harder. */
    static float factorBefore(ServerPlayer player, AnimationAccessor<? extends StaticAnimation> animation) {
        return isStrike(animation) && sunlit(player) ? DAYLIGHT_FACTOR : 1;
    }

    /** What one of the art's hits does besides its damage: a strike sets its target alight. */
    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean enemy) {
        if (!isStrike(animation) || MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target)) return;
        if (enemy && target.getRemainingFireTicks() <= 0) MartialProgress.count(player, MartialArts.KINDLING_BURNS, 1);
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), STRIKE_FIRE_TICKS));
        player.serverLevel().sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
    }

    // ---------------------------------------------------------------- techniques

    /** Starts one of the art's techniques. Bare hands are checked before. */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (LEAPS.containsKey(player.getUUID())) return false;
        MartialData data = MartialService.data(player);
        boolean evolved = data.evolved(art(), technique.id());
        if (technique == MartialArts.SUNFIRE_FIST) {
            if (player.isShiftKeyDown() && noon(player)) {
                grandNoon(player, patch);
                return true;
            }
            play(player, patch, MurimAnimations.MJF_SUNFIRE, technique);
            MartialCombat.later(player, ticks(MurimAnimations.MJF_SUNFIRE_IMPACT), () -> sunfire(player, evolved));
            return true;
        }
        if (technique == MartialArts.PRIDE_FLARE) {
            play(player, patch, MurimAnimations.MJF_FLARE, technique);
            MartialCombat.later(player, 6, () -> {
                Vec3 palm = player.getEyePosition().add(player.getLookAngle().scale(0.6));
                MajinFist.glow(player.serverLevel(), palm, 0.3F, 1.6F, 8);
            });
            MartialCombat.later(player, ticks(MurimAnimations.MJF_FLARE_AT), () -> flare(player, evolved));
            return true;
        }
        if (technique == MartialArts.CRUEL_SUN) {
            play(player, patch, MurimAnimations.MJF_CONJURE, technique);
            List<CruelSunEntity> suns = new ArrayList<>();
            MartialCombat.later(player, 7, () -> {
                suns.add(conjure(player, 0, 1.6F));
                if (evolved) suns.add(conjure(player, 1.6, 1.2F));
            });
            MartialCombat.later(player, ticks(MurimAnimations.MJF_HURL_AT), () -> hurl(player, suns));
            return true;
        }
        if (technique == MartialArts.NOON_JUDGMENT) {
            play(player, patch, MurimAnimations.MJF_JUDGMENT, technique);
            List<Vec3> spots = new ArrayList<>();
            MartialCombat.later(player, 12, () -> spots.addAll(markJudgment(player)));
            MartialCombat.later(player, ticks(MurimAnimations.MJF_PILLARS_AT), () -> strikeJudgment(player, spots));
            return true;
        }
        if (technique == MartialArts.MIDDAY_METEOR) {
            play(player, patch, MurimAnimations.MJF_LEAP, technique);
            MartialCombat.later(player, ticks(MurimAnimations.MJF_SPRING_AT), () -> leap(player));
            return true;
        }
        if (technique == MartialArts.THE_ONE) {
            patch.playAnimationSynchronized(MurimAnimations.MJF_THE_ONE, 0.0F);
            theOne(player);
            return true;
        }
        return false;
    }

    private static int ticks(float seconds) {
        return Math.round(seconds * 20);
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation, MartialArts.Technique technique) {
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
    }

    private static Vec3 ahead(Player player) {
        return Vec3.directionFromRotation(0, player.getYRot()).multiply(1, 0, 1).normalize();
    }

    /** Where the player looks, on the ground, at most {@code reach} blocks off (flat). */
    static Vec3 aim(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(reach + 16));
        var hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? player.position().add(ahead(player).scale(reach)) : hit.getLocation();
        Vec3 flat = at.subtract(player.position()).multiply(1, 0, 1);
        boolean onGround = hit.getType() != HitResult.Type.MISS && flat.length() <= reach;
        if (flat.length() > reach) at = player.position().add(flat.normalize().scale(reach));
        // Where the look meets the ground: there (under a roof as well); else the ground under the point, near the body's height.
        if (onGround) return at;
        BlockPos.MutableBlockPos q = BlockPos.containing(at.x, player.getY() + 6, at.z).mutable();
        for (int i = 0; i < 24 && player.level().getBlockState(q.below()).getCollisionShape(player.level(), q.below()).isEmpty(); i++) q.move(0, -1, 0);
        return new Vec3(at.x, q.getY(), at.z);
    }

    /** A blow of the art: damage from the player, sets alight; a guard raised in time stops it. */
    static boolean blow(ServerPlayer player, LivingEntity e, float share, int fireTicks) {
        if (!MartialCombat.isEnemy(e, player) || MurimGuardSkill.parriedWithin(e, 6)) return false;
        boolean hurt = e.hurt(player.damageSources().playerAttack(player), TechniquePower.damage(player, MartialArts.MAJIN_FIST, share));
        if (hurt) {
            e.hurtMarked = true;
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), fireTicks));
        }
        return hurt;
    }

    /** All enemies within {@code radius} of {@code at} struck once, thrown back and up. Returns how many. */
    static int blast(ServerPlayer player, Vec3 at, double radius, float share, double up, int fireTicks) {
        int n = 0;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 3, radius), e -> e != player && e.isAlive())) {
            double d = Math.sqrt(e.distanceToSqr(at.x, e.getY(), at.z));
            if (d > radius || !blow(player, e, share, fireTicks)) continue;
            Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
            Vec3 push = away.lengthSqr() < 1e-4 ? ahead(player) : away.normalize();
            double k = 1 - d / radius * 0.6;
            e.push(push.x * 0.8 * k, up * k, push.z * 0.8 * k);
            n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- Sunfire Fist, Grand Noon

    static void sunfire(ServerPlayer player, boolean evolved) {
        ServerLevel level = player.serverLevel();
        double reach = evolved ? SUNFIRE_REACH_EVOLVED : SUNFIRE_REACH;
        Vec3 at = player.position().add(ahead(player).scale(1.6 + reach * 0.5));
        blast(player, at, reach, SUNFIRE_SHARE, 0.3, 60);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.2, at.z, (int) (reach * 20), reach * 0.4, 0.5, reach * 0.4, 0.08);
        level.playSound(null, BlockPos.containing(at), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
        MajinFist.glow(level, at.add(0, 1.2, 0), 0.6F, (float) reach * 1.6F, 10);
        MajinFist.flash(level, at.add(0, 1.2, 0), (float) reach * 3, 2.5F, 6);
        MartialProgress.addMastery(player, art(), MartialArts.SUNFIRE_FIST.id(), 1);
    }

    /** Grand Noon: the uppercut of sunlight. The first time, the secret is revealed. */
    static void grandNoon(ServerPlayer player, PlayerPatch<?> patch) {
        play(player, patch, MurimAnimations.MJF_SUNFIRE, MartialArts.SUNFIRE_FIST);
        MartialData data = MartialService.data(player);
        if (!data.hasTechnique(MartialArts.GRAND_NOON.id())) {
            MartialService.set(player, data.unlock(MartialArts.GRAND_NOON));
            player.sendSystemMessage(Component.translatable("technique.murimblock.grand_noon.discovered"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
        MartialCombat.later(player, ticks(MurimAnimations.MJF_SUNFIRE_IMPACT), () -> {
            ServerLevel level = player.serverLevel();
            LivingEntity target = nearestAhead(player, 12);
            Vec3 at = target != null ? target.position() : player.position().add(ahead(player).scale(4));
            pillar(level, at, 34, 4.5F);
            blast(player, at, 3, GRAND_NOON_SHARE, 0.9, 100);
            level.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F, 0.8F);
            level.playSound(null, BlockPos.containing(at), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 1.2F);
            MajinFist.flash(level, at.add(0, 4, 0), 16, 8, 14);
        });
    }

    @Nullable private static LivingEntity nearestAhead(ServerPlayer player, double reach) {
        Vec3 dir = ahead(player);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach),
                        e -> e != player && e.isAlive() && MartialCombat.isEnemy(e, player)
                                && e.position().subtract(player.position()).multiply(1, 0, 1).normalize().dot(dir) > 0.5)
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    /** A pillar of sunlight down on {@code at}: balls of light stacked into the sky, fading. */
    private static void pillar(ServerLevel level, Vec3 at, int height, float width) {
        for (int y = 0; y <= height; y += 2) MajinFist.glow(level, at.add(0, y + 0.5, 0), width * 0.7F, width + (y == 0 ? 4 : 0), 14);
        for (int y = 0; y < height; y += 2) level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 3, 0.3, 0.6, 0.3, 0.01);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 40, 1.2, 0.3, 1.2, 0.1);
    }

    // ---------------------------------------------------------------- Pride Flare

    static void flare(ServerPlayer player, boolean evolved) {
        Vec3 origin = player.position(), dir = ahead(player);
        int steps = evolved ? FLARE_STEPS_EVOLVED : FLARE_STEPS;
        double width = evolved ? FLARE_WIDTH_EVOLVED : FLARE_WIDTH;
        Set<UUID> struck = new HashSet<>();
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.4F, 0.6F);
        MajinFist.flash(player.serverLevel(), player.getEyePosition(), 10, 3, 6);
        for (int i = 0; i < steps; i++) {
            final double d = 2 + 1.5 * i;
            MartialCombat.later(player, i + 1, () -> {
                ServerLevel level = player.serverLevel();
                Vec3 at = origin.add(dir.scale(d));
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(width / 2 + 1, 2, width / 2 + 1), e -> e != player && e.isAlive())) {
                    Vec3 rel = e.position().subtract(origin).multiply(1, 0, 1);
                    double along = rel.dot(dir), across = Math.abs(rel.x * dir.z - rel.z * dir.x);
                    if (along < d - 1.6 || along > d || across > width / 2 || struck.contains(e.getUUID())) continue;
                    if (blow(player, e, FLARE_SHARE, 80)) {
                        struck.add(e.getUUID());
                        e.push(dir.x * 0.6, 0.3, dir.z * 0.6);
                        MartialProgress.count(player, MartialArts.SPREADING_FLAME_HITS, 1);
                    }
                }
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.6, at.z, 40, width * 0.4, 0.6, width * 0.4, 0.06);
                level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 2, width * 0.3, 0.1, width * 0.3, 0);
                if (((int) (d * 2)) % 3 == 0) MajinFist.glow(level, at.add(0, 0.9, 0), 0.8F, (float) width, 9);
            });
        }
        MartialCombat.later(player, steps + 2, () -> MartialProgress.addMastery(player, art(), MartialArts.PRIDE_FLARE.id(), 1));
    }

    // ---------------------------------------------------------------- Cruel Sun

    /** A sun grown over the palm ({@code side}: off to the left, for the second of Twin Suns). */
    static CruelSunEntity conjure(ServerPlayer player, double side, float size) {
        ServerLevel level = player.serverLevel();
        CruelSunEntity sun = MurimEntities.CRUEL_SUN.get().create(level);
        if (sun == null) return null;
        sun.setSize(0.2F);
        sun.hold(player, 3.4 + side * 0.4, -side * 0.6, size, 18);
        level.addFreshEntity(sun);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
        return sun;
    }

    static void hurl(ServerPlayer player, List<CruelSunEntity> suns) {
        Vec3 target = aim(player, SUN_REACH);
        for (int i = 0; i < suns.size(); i++) {
            CruelSunEntity sun = suns.get(i);
            if (sun == null || !sun.isAlive()) continue;
            Vec3 to = i == 0 ? target : target.add(ahead(player).cross(new Vec3(0, 1, 0)).scale(3.5));
            sun.hurl(to, 4, 14 + 4 * i, SUN_RADIUS, SUN_SHARE * (i == 0 ? 1 : 0.7F));
        }
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.2F, 1.2F);
        MartialProgress.addMastery(player, art(), MartialArts.CRUEL_SUN.id(), 1);
    }

    /** A player's sun bursting where it falls: the ball of fire, the blast, the crater, its light. */
    public static void sunBurst(ServerPlayer player, ServerLevel level, Vec3 at, double radius, float share, float size) {
        MajinFist.glow(level, at.add(0, 1, 0), size, (float) radius * 3.2F, size > 4 ? 22 : 14);
        int n = blast(player, at, radius, share, 0.8, 100);
        if (n > 0) MartialProgress.count(player, MartialArts.SUNS_WRATH_HITS, n);
        ShatteredGround.crater(level, BlockPos.containing(at.x, at.y - 0.5, at.z), Math.max(2, radius * 0.6), size > 4 ? 3 : 2);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, (int) (radius * 30), radius * 0.5, 0.8, radius * 0.5, 0.2);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 2, at.z, (int) (radius * 8), radius * 0.4, 1.2, radius * 0.4, 0.04);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.5F, 0.6F);
        MajinFist.flash(level, at.add(0, 2, 0), (float) radius * 4, size > 4 ? 9 : 5, size > 4 ? 18 : 10);
    }

    // ---------------------------------------------------------------- Noon Judgment

    static List<Vec3> markJudgment(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Vec3> spots = new ArrayList<>();
        level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(JUDGMENT_REACH),
                        e -> e != player && e.isAlive() && MartialCombat.isEnemy(e, player) && e.distanceTo(player) <= JUDGMENT_REACH)
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(player))).limit(JUDGMENT_TARGETS)
                .forEach(e -> spots.add(e.position()));
        if (spots.isEmpty()) spots.add(aim(player, JUDGMENT_REACH));
        for (int k = 0; k < 20; k += 2) {
            final int when = k;
            MartialCombat.later(player, k + 1, () -> {
                for (Vec3 s : spots) for (int i = 0; i < 20; i++) {
                    double a = Math.PI * 2 * i / 20 + when * 0.1;
                    level.sendParticles(GOLD, s.x + Math.cos(a) * PILLAR_RADIUS, s.y + 0.15, s.z + Math.sin(a) * PILLAR_RADIUS, 1, 0, 0, 0, 0);
                }
            });
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 1.0F);
        return spots;
    }

    static void strikeJudgment(ServerPlayer player, List<Vec3> spots) {
        ServerLevel level = player.serverLevel();
        int n = 0;
        for (Vec3 s : spots) {
            pillar(level, s, 26, 3F);
            n += blast(player, s, PILLAR_RADIUS, PILLAR_SHARE, 0.4, 80);
            level.playSound(null, BlockPos.containing(s), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.4F, 1.0F);
            MajinFist.flash(level, s.add(0, 3, 0), 10, 5, 10);
        }
        if (n > 0) MartialProgress.count(player, MartialArts.HIGH_NOON_HITS, n);
        MartialProgress.addMastery(player, art(), MartialArts.NOON_JUDGMENT.id(), 1);
    }

    // ---------------------------------------------------------------- Midday Meteor

    private static final class Leap {
        final Vec3 from, to;
        int ticks;

        Leap(Vec3 from, Vec3 to) {
            this.from = from;
            this.to = to;
        }
    }

    private static final Map<UUID, Leap> LEAPS = new HashMap<>();

    static void leap(ServerPlayer player) {
        LEAPS.put(player.getUUID(), new Leap(player.position(), aim(player, METEOR_REACH)));
        player.setNoGravity(true);
        MartialCombat.noFallFor(player, 80);
        var patch = com.murimblock.integration.epicfight.EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(MurimAnimations.MJF_DIVE, 0.0F);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void leapTick(ServerPlayer player, Leap l) {
        ServerLevel level = player.serverLevel();
        l.ticks++;
        double k = Math.min(1, l.ticks / (double) METEOR_TICKS);
        Vec3 at = l.from.lerp(l.to, k).add(0, 9 * 4 * k * (1 - k), 0);
        if (level.noCollision(player, player.getBoundingBox().move(at.subtract(player.position())))) player.connection.teleport(at.x, at.y, at.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 8, 0.4, 0.6, 0.4, 0.03);
        if (l.ticks % 2 == 0) for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16, r = 4 * (1 - k) + 1;
            level.sendParticles(ParticleTypes.FLAME, l.to.x + Math.cos(a) * r, l.to.y + 0.2, l.to.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        if (k < 1 && !(l.ticks > METEOR_TICKS / 2 && player.onGround())) return;
        LEAPS.remove(player.getUUID());
        player.setNoGravity(false);
        var patch = com.murimblock.integration.epicfight.EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(MurimAnimations.MSF_LAND, 0.0F);
        Vec3 to = player.position();
        ShatteredGround.crater(level, BlockPos.containing(to.x, to.y - 0.5, to.z), METEOR_CRATER, 2);
        blast(player, to, METEOR_RADIUS, METEOR_SHARE, 0.7, 80);
        MajinFist.glow(level, to.add(0, 1, 0), 1.5F, 10F, 14);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, to.x, to.y + 0.5, to.z, 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 0.6F);
        MajinFist.flash(level, to.add(0, 2, 0), 14, 6, 12);
        Set<UUID> ring = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            final double r = 3 + i * 1.1;
            MartialCombat.later(player, i + 1, () -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(r + 1, 2, r + 1), e -> e != player && e.isAlive() && e.onGround())) {
                    double d = Math.sqrt(e.distanceToSqr(to.x, e.getY(), to.z));
                    if (d < r - 1.1 || d > r + 0.5 || ring.contains(e.getUUID())) continue;
                    if (blow(player, e, RING_SHARE, 60)) ring.add(e.getUUID());
                }
                int balls = (int) Math.max(6, r * 1.4);
                for (int n = 0; n < balls; n++) {
                    double a = Math.PI * 2 * n / balls;
                    level.sendParticles(ParticleTypes.FLAME, to.x + Math.cos(a) * r, to.y + 0.3, to.z + Math.sin(a) * r, 2, 0.1, 0.2, 0.1, 0.02);
                    if (n % 2 == 0) MajinFist.glow(level, to.add(Math.cos(a) * r, 0.7, Math.sin(a) * r), 0.6F, 1.8F, 7);
                }
            });
        }
        MartialProgress.addMastery(player, art(), MartialArts.MIDDAY_METEOR.id(), 1);
    }

    // ---------------------------------------------------------------- The One

    private static final class One {
        final CruelSunEntity sun;
        int ticks;

        One(CruelSunEntity sun) {
            this.sun = sun;
        }
    }

    private static final Map<UUID, One> ONES = new HashMap<>();

    public static boolean inTheOne(Player player) {
        return ONES.containsKey(player.getUUID());
    }

    static void theOne(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        CruelSunEntity sun = MurimEntities.CRUEL_SUN.get().create(level);
        if (sun != null) {
            sun.setSize(0.4F);
            sun.hold(player, 8, 1.5, 3.2F, 60);
            level.addFreshEntity(sun);
        }
        ONES.put(player.getUUID(), new One(sun));
        empower(player, true);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ONE_TICKS, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ONE_TICKS + 100, 0, false, false));
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8F, 1.4F);
        MajinFist.flash(level, player.position().add(0, 7, 0), 30, 8, 24);
        player.displayClientMessage(Component.translatable("majin.murimblock.the_one").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD), true);
    }

    private static void oneTick(ServerPlayer player, One one) {
        ServerLevel level = player.serverLevel();
        one.ticks++;
        if (one.ticks % 4 == 0) level.sendParticles(GOLD, player.getX(), player.getY() + 1, player.getZ(), 10, 1.0, 1.0, 1.0, 0.02);
        if (one.ticks % 20 == 0) {
            // Its heat: every enemy near burns.
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ONE_HEAT),
                    e -> e != player && e.isAlive() && e.distanceTo(player) <= ONE_HEAT)) blow(player, e, ONE_HEAT_SHARE, 40);
            level.sendParticles(ParticleTypes.SMALL_FLAME, player.getX(), player.getY() + 1, player.getZ(), 30, ONE_HEAT * 0.5, 0.6, ONE_HEAT * 0.5, 0.01);
        }
        if (one.ticks < ONE_TICKS && player.isAlive()) return;
        ONES.remove(player.getUUID());
        empower(player, false);
        if (!player.isAlive() || one.sun == null || !one.sun.isAlive()) {
            if (one.sun != null) one.sun.discard();
            return;
        }
        // The great sun brought down where the player looks.
        Vec3 target = aim(player, ONE_FALL_REACH);
        one.sun.setSize(4.5F);
        one.sun.hurl(target, 8, 30, ONE_RADIUS, ONE_FALL_SHARE);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.2F, 1.2F);
        MartialProgress.addMastery(player, art(), MartialArts.THE_ONE.id(), 1);
    }

    private static void empower(ServerPlayer player, boolean on) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (speed != null) {
            speed.removeModifier(ONE_SPEED_ID);
            if (on) speed.addTransientModifier(new AttributeModifier(ONE_SPEED_ID, ONE_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (attack != null) {
            attack.removeModifier(ONE_STRENGTH_ID);
            if (on) attack.addTransientModifier(new AttributeModifier(ONE_STRENGTH_ID, ONE_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // ---------------------------------------------------------------- every tick, kills, state

    /** Every tick of a player: their leap and The One, if any. */
    static void tick(ServerPlayer player) {
        Leap l = LEAPS.get(player.getUUID());
        if (l != null) {
            if (!player.isAlive() || l.ticks > 60) { LEAPS.remove(player.getUUID()); player.setNoGravity(false); }
            else leapTick(player, l);
        }
        One one = ONES.get(player.getUUID());
        if (one != null) oneTick(player, one);
    }

    /** Pride Incarnate: enemies slain burning, by a player of the art. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !stanceActive(player)) return;
        LivingEntity victim = event.getEntity();
        if (victim.getRemainingFireTicks() > 0 && MartialCombat.isEnemy(victim, player)) MartialProgress.count(player, MartialArts.PRIDE_INCARNATE_SLAIN, 1);
    }

    /** Tests: the leap lands now; The One ends now. */
    public static void landForTests(ServerPlayer player) {
        Leap l = LEAPS.get(player.getUUID());
        if (l != null) { l.ticks = METEOR_TICKS - 1; leapTick(player, l); }
    }

    public static boolean leaping(Player player) {
        return LEAPS.containsKey(player.getUUID());
    }

    // ---------------------------------------------------------------- the finish: Noon Sentence

    /** A sun raised over the body, then brought down on it as a pillar of sunlight (it kills). */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        ServerLevel level = executor.serverLevel();
        Vec3 at = victim.position();
        if (elapsed == FINISH_SUN_TICK) {
            MajinFist.glow(level, at.add(0, 6, 0), 0.4F, 3.5F, FINISH_HIT_TICK - FINISH_SUN_TICK);
            level.playSound(null, victim.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.0F);
        }
        if (elapsed == FINISH_HIT_TICK) {
            pillar(level, at, 20, 3F);
            ShatteredGround.crater(level, victim.blockPosition().below(), 2.2, 2);
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 3, 0.5, 0.1, 0.5, 0);
            level.playSound(null, victim.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.6F, 0.9F);
            MajinFist.flash(level, at.add(0, 3, 0), 12, 6, 12);
        }
    }

    static void forget(@Nullable UUID player) {
        if (player == null) {
            LEAPS.clear();
            ONES.values().forEach(o -> { if (o.sun != null) o.sun.discard(); });
            ONES.clear();
        } else {
            LEAPS.remove(player);
            One o = ONES.remove(player);
            if (o != null && o.sun != null) o.sun.discard();
        }
    }
}
