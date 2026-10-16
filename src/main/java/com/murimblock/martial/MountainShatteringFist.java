package com.murimblock.martial;

import com.murimblock.Murimblock;
import com.murimblock.combat.ShatteredGround;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Mountain-Shattering Fist in play (docs/combat/styles/mountain-shattering-fist.md), the Bandit Warlord's art: bare
 * hands, slow and heavy, every blow striking all round and breaking the ground (it comes back fifteen seconds later:
 * {@link ShatteredGround}; nothing drops, nothing breaks in a Murim site). Its M1 are Epic Fight's bare-hand strikes; its
 * passive, Earthshaker, shakes the ground round each one that lands ({@link #SPLASH_SHARE} to all near the target), and
 * bare hands break stone and wood three times as fast. Every technique picks its variant on the server, then plays its
 * animation (scripts/animation/mountain-fist.cjs), whose boxes deal the main damage:
 * <ul>
 *   <li><b>Mountain-Crushing Punch</b>: a straight right; the target is thrown back, all in a cone of 5 blocks (7 evolved:
 *   Mountain-Splitting Punch) take a share, and a tunnel of 2 x 2 opens 4 blocks ahead (6 evolved);</li>
 *   <li><b>Earth Breaker</b>: both fists on the ground: all within 5 blocks thrown up and slowed, a crater of 3 blocks round
 *   (4, and a second wave, evolved: Earthquake);</li>
 *   <li><b>Bull Charge</b>: a rush of some four blocks, knocking down whatever it meets, walls burst through (2 wide, 3
 *   high; 3 wide evolved: Stampede);</li>
 *   <li>from the air, any of them: a dive fist first, a crater where it lands;</li>
 *   <li><b>Sky Breaker</b>, the secret: Mountain-Crushing Punch cast crouching, an uppercut that throws its target to the
 *   sky, the ground bursting under it;</li>
 *   <li><b>Heaven-Falling Mountain</b>, the ultimate: up into the sky, a moment's hold while the ground glows where it
 *   will land, then down like a meteor onto that point (12 blocks off at most), a crater of 8 blocks.</li>
 * </ul>
 * Its finish is Mountain Burial: the body driven into the ground in three blows.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class MountainShatteringFist {
    /** Earthshaker: the share of the attack each strike that lands deals to all others this near its target. */
    public static final float SPLASH_SHARE = 0.3F;
    public static final double SPLASH_REACH = 1.5;
    /** Bare hands break stone and wood this many times faster. */
    public static final float STONE_HANDS = 3.0F;
    /** Mountain-Crushing Punch: the cone's reach and the tunnel's length (evolved), the cone's share, the push. */
    public static final double CONE = 5, CONE_EVOLVED = 7;
    public static final int TUNNEL = 4, TUNNEL_EVOLVED = 6;
    public static final float CONE_SHARE = 0.8F;
    /** Earth Breaker: the crater's radius (evolved), how long it slows; the second wave's share. */
    public static final double EARTH_CRATER = 3, EARTH_CRATER_EVOLVED = 4;
    public static final int EARTH_SLOW_TICKS = 60;
    public static final float QUAKE_SHARE = 0.6F;
    /** From the air: how fast the dive comes down, the landing's crater and its share. */
    public static final double DIVE_SPEED = 1.6;
    public static final float DIVE_SHARE = 1.0F;
    /** Heaven-Falling Mountain: the rise, the hold (ticks), how far off it may come down, its crater, its shares. */
    public static final double ULT_RISE = 2.2, ULT_REACH = 12, ULT_CRATER = 8, ULT_CORE = 3, ULT_DIVE = 2.2;
    public static final int ULT_HOLD = 10, ULT_CRATER_DEPTH = 4;
    public static final float ULT_CORE_SHARE = 3.0F, ULT_SHARE = 1.5F;
    /** The finish's blows (ticks in): the last one kills. */
    public static final int[] FINISH_BLOWS = {9, 16, 28};
    public static final int FINISH_HIT_TICK = 28;
    /** Enemies one technique must strike to count for Shake the Earth. */
    public static final int GROUP = 3;

    private MountainShatteringFist() {
    }

    static MartialArts.Art art() {
        return MartialArts.art(MartialArts.MOUNTAIN_SHATTERING_FIST);
    }

    /** Whether this player fights with Earthshaker: the Mountain-Shattering Fist is their main style. */
    public static boolean stanceActive(Player player) {
        if (player.level().isClientSide)
            return com.murimblock.cultivation.MeditationService.aura(player).mainArt().filter(MartialArts.MOUNTAIN_SHATTERING_FIST::equals).isPresent();
        return MartialService.data(player).mainArt().filter(MartialArts.MOUNTAIN_SHATTERING_FIST::equals).isPresent();
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

    // ---------------------------------------------------------------- swings

    static boolean isStrike(AnimationAccessor<? extends StaticAnimation> a) {
        return LongFist.isStrike(a);
    }

    /** What an animation counts for in this art: the passive (bare-hand strikes in the stance) or a technique. */
    @Nullable static ResourceLocation creditOf(Player player, AnimationAccessor<? extends StaticAnimation> a) {
        if (a == null) return null;
        if (isStrike(a)) return guardsBareHanded(player) ? MartialArts.EARTHSHAKER.id() : null;
        if (a == MurimAnimations.MSF_CRUSHER || a == MurimAnimations.MSF_SKY_BREAKER) return MartialArts.MOUNTAIN_CRUSHING_PUNCH.id();
        if (a == MurimAnimations.MSF_EARTH_BREAKER) return MartialArts.EARTH_BREAKER.id();
        if (a == MurimAnimations.MSF_BULL_CHARGE) return MartialArts.BULL_CHARGE.id();
        return null;
    }

    static boolean knocksDown(AnimationAccessor<? extends StaticAnimation> a) {
        return a == MurimAnimations.MSF_EARTH_BREAKER || a == MurimAnimations.MSF_BULL_CHARGE;
    }

    // ---------------------------------------------------------------- techniques

    /** Starts one of the art's techniques in the variant the moment calls for. Bare hands are checked before. */
    static boolean perform(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        if (FLIGHTS.containsKey(player.getUUID())) return false;
        MartialData data = MartialService.data(player);
        boolean evolved = data.evolved(art(), technique.id());
        ServerLevel level = player.serverLevel();
        HITS.put(player.getUUID(), new HashSet<>());
        if (technique == MartialArts.HEAVEN_FALLING_MOUNTAIN) {
            ascend(player, patch);
            return true;
        }
        if (AzureCloudSword.airborne(player)) {
            dive(player, patch, technique);
            return true;
        }
        if (technique == MartialArts.MOUNTAIN_CRUSHING_PUNCH) {
            if (player.isShiftKeyDown()) {
                skyBreaker(player, patch);
                return true;
            }
            play(player, patch, MurimAnimations.MSF_CRUSHER, technique);
            MartialCombat.later(player, ticks(MurimAnimations.MSF_CRUSHER_IMPACT), () -> crusherImpact(player, evolved));
            countGroupLater(player, 30);
            return true;
        }
        if (technique == MartialArts.EARTH_BREAKER) {
            play(player, patch, MurimAnimations.MSF_EARTH_BREAKER, technique);
            int at = ticks(MurimAnimations.MSF_EARTH_IMPACT);
            MartialCombat.later(player, at, () -> earthImpact(player, evolved ? EARTH_CRATER_EVOLVED : EARTH_CRATER));
            if (evolved) MartialCombat.later(player, at + 10, () -> quake(player));
            countGroupLater(player, 40);
            return true;
        }
        if (technique == MartialArts.BULL_CHARGE) {
            play(player, patch, MurimAnimations.MSF_BULL_CHARGE, technique);
            for (int t = ticks(MurimAnimations.MSF_CHARGE_FROM); t <= ticks(MurimAnimations.MSF_CHARGE_TO); t++)
                MartialCombat.later(player, t, () -> burstThrough(player, evolved));
            level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.2F);
            countGroupLater(player, 36);
            return true;
        }
        return false;
    }

    private static int ticks(float seconds) {
        return Math.round(seconds * 20);
    }

    private static void play(ServerPlayer player, PlayerPatch<?> patch, AnimationAccessor<? extends StaticAnimation> animation,
                             MartialArts.Technique technique) {
        MartialCombat.expect(player, animation, technique.id(), false);
        patch.playAnimationSynchronized(animation, 0.0F);
    }

    /** Sky Breaker: the uppercut. The first time, the secret is revealed. */
    static void skyBreaker(ServerPlayer player, PlayerPatch<?> patch) {
        play(player, patch, MurimAnimations.MSF_SKY_BREAKER, MartialArts.MOUNTAIN_CRUSHING_PUNCH);
        MartialData data = MartialService.data(player);
        if (!data.hasTechnique(MartialArts.SKY_BREAKER.id())) {
            MartialService.set(player, data.unlock(MartialArts.SKY_BREAKER));
            player.sendSystemMessage(Component.translatable("technique.murimblock.sky_breaker.discovered"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
    }

    /** The punch is through: a tunnel opens ahead, all in the cone take a share. */
    static void crusherImpact(ServerPlayer player, boolean evolved) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = Vec3.directionFromRotation(0, player.getYRot()).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        int broken = 0, length = evolved ? TUNNEL_EVOLVED : TUNNEL;
        for (int k = 1; k <= length; k++) for (int w = 0; w <= 1; w++) for (int y = 0; y <= 1; y++) {
            Vec3 p = player.position().add(dir.scale(k + 0.5)).add(side.scale(w - 0.5));
            if (ShatteredGround.smash(level, BlockPos.containing(p.x, player.getY() + y, p.z))) broken++;
        }
        credit(player, broken, true);
        double reach = evolved ? CONE_EVOLVED : CONE;
        float damage = TechniquePower.damage(player, MartialArts.MOUNTAIN_SHATTERING_FIST, CONE_SHARE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach), e -> e != player && e.isAlive())) {
            Vec3 to = e.position().subtract(player.position()).multiply(1, 0, 1);
            double d = to.length();
            if (d > reach || d < 1e-3 || to.normalize().dot(dir) < 0.6 || justStruck(player, e)) continue;
            if (blow(player, e, damage)) e.push(dir.x * 0.6, 0.15, dir.z * 0.6);
        }
        Vec3 fist = player.position().add(dir.scale(1.5)).add(0, 1.1, 0);
        for (int i = 0; i < 12; i++) {
            Vec3 p = fist.add(dir.scale(i * reach / 12));
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.2 + i * 0.08, 0.2 + i * 0.05, 0.2 + i * 0.08, 0.02);
        }
        level.playSound(null, BlockPos.containing(fist), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.1F);
    }

    /** The fists meet the ground: a crater round the point before the body, dust and stones thrown up. */
    static void earthImpact(ServerPlayer player, double radius) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(Vec3.directionFromRotation(0, player.getYRot()).scale(1.2));
        BlockPos ground = BlockPos.containing(at.x, player.getY() - 0.5, at.z);
        BlockState state = level.getBlockState(ground);
        credit(player, ShatteredGround.crater(level, ground, radius, 2), false);
        if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, player.getY() + 0.2, at.z, 120, radius * 0.8, 0.3, radius * 0.8, 0.3);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, player.getY() + 0.3, at.z, 3, radius * 0.4, 0.1, radius * 0.4, 0);
        level.playSound(null, ground, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.7F);
        LongFist.ring(level, at, radius + 1);
    }

    /** Earthquake: a second wave, wider, shaking all within six blocks once more. */
    static void quake(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        float damage = TechniquePower.damage(player, MartialArts.MOUNTAIN_SHATTERING_FIST, QUAKE_SHARE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6, 2, 6), e -> e != player && e.isAlive() && e.distanceToSqr(player) <= 36))
            if (blow(player, e, damage)) e.push(0, 0.5, 0);
        LongFist.ring(level, player.position(), 6);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    /** One tick of the charge: whatever stands before the shoulder, 2 wide (3 evolved) and 3 high, bursts. */
    static void burstThrough(ServerPlayer player, boolean evolved) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = Vec3.directionFromRotation(0, player.getYRot()).normalize(), side = new Vec3(-dir.z, 0, dir.x);
        int broken = 0;
        double[] offsets = evolved ? new double[] {-1, 0, 1} : new double[] {-0.5, 0.5};
        for (double w : offsets) for (int y = 0; y <= 2; y++) {
            Vec3 p = player.position().add(dir.scale(1.2)).add(side.scale(w));
            if (ShatteredGround.smash(level, BlockPos.containing(p.x, player.getY() + y + 0.1, p.z))) broken++;
        }
        if (broken > 0) level.playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 0.8F, 0.7F);
        credit(player, broken, false);
    }

    // ---------------------------------------------------------------- flights: the dive, the ultimate

    private enum Stage { DIVE, RISE, HOLD, FALL }

    private static final class Flight {
        final MartialArts.Technique technique;
        Stage stage;
        int ticks;
        Vec3 target = Vec3.ZERO;

        Flight(MartialArts.Technique technique, Stage stage) {
            this.technique = technique;
            this.stage = stage;
        }
    }

    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();

    /** From the air: down fist first, a crater where the body lands. */
    static void dive(ServerPlayer player, PlayerPatch<?> patch, MartialArts.Technique technique) {
        patch.playAnimationSynchronized(MurimAnimations.MSF_FALLING_FIST, 0.0F);
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(m.x * 0.2, -DIVE_SPEED, m.z * 0.2);
        player.hurtMarked = true;
        MartialCombat.noFallFor(player, 100);
        FLIGHTS.put(player.getUUID(), new Flight(technique, Stage.DIVE));
    }

    /** The ultimate: a crouch, then up into the sky. */
    static void ascend(ServerPlayer player, PlayerPatch<?> patch) {
        patch.playAnimationSynchronized(MurimAnimations.MSF_ASCEND, 0.0F);
        MartialCombat.later(player, 6, () -> {
            player.setDeltaMovement(0, ULT_RISE, 0);
            player.hurtMarked = true;
            player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.6, 0.1, 0.6, 0);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.8F);
        });
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 3, false, false));
        MartialCombat.noFallFor(player, 300);
        FLIGHTS.put(player.getUUID(), new Flight(MartialArts.HEAVEN_FALLING_MOUNTAIN, Stage.RISE));
    }

    /** Every tick of a player: their flight, if any. */
    static void tick(ServerPlayer player) {
        Flight f = FLIGHTS.get(player.getUUID());
        if (f == null) return;
        ServerLevel level = player.serverLevel();
        f.ticks++;
        if (!player.isAlive() || f.ticks > 260) { land(player, f, false); return; }
        switch (f.stage) {
            case DIVE -> {
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 2, 0.3, 0.3, 0.3, 0.01);
                if (f.ticks > 2 && (player.onGround() || player.isInWater())) land(player, f, true);
            }
            case RISE -> {
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
                if (f.ticks > 8 && player.getDeltaMovement().y <= 0.3 || f.ticks > 30) {
                    f.stage = Stage.HOLD;
                    f.ticks = 0;
                    f.target = aim(player);
                    player.setNoGravity(true);
                }
            }
            case HOLD -> {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                glow(level, f.target);
                if (f.ticks >= ULT_HOLD) {
                    f.stage = Stage.FALL;
                    f.ticks = 0;
                    player.setNoGravity(false);
                    var patch = com.murimblock.integration.epicfight.EpicFightBridge.patch(player);
                    if (patch != null) patch.playAnimationSynchronized(MurimAnimations.MSF_FALLING_FIST, 0.0F);
                    level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0F, 0.5F);
                }
            }
            case FALL -> {
                Vec3 to = f.target.subtract(player.position());
                Vec3 v = to.lengthSqr() < 1e-3 ? new Vec3(0, -ULT_DIVE, 0) : to.normalize().scale(ULT_DIVE);
                player.setDeltaMovement(v.x, Math.min(v.y, -0.6), v.z);
                player.hurtMarked = true;
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 14, 0.5, 0.8, 0.5, 0.03);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 2.5, player.getZ(), 4, 0.3, 0.8, 0.3, 0.01);
                glow(level, f.target);
                if (f.ticks > 2 && (player.onGround() || player.isInWater())) land(player, f, true);
            }
        }
    }

    /** Where the ultimate comes down: where the player looks, on the ground, 12 blocks off at most. */
    static Vec3 aim(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(80));
        var hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(ULT_REACH)) : hit.getLocation();
        Vec3 flat = at.subtract(player.position()).multiply(1, 0, 1);
        if (flat.length() > ULT_REACH) at = player.position().add(flat.normalize().scale(ULT_REACH));
        int top = player.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z));
        return new Vec3(at.x, Math.min(top, player.getY()), at.z);
    }

    /** The ring glowing on the ground where the ultimate will land. */
    private static void glow(ServerLevel level, Vec3 at) {
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2 * i / 20 + level.getGameTime() * 0.1;
            level.sendParticles(ParticleTypes.FLAME, at.x + Math.cos(a) * 3, at.y + 0.1, at.z + Math.sin(a) * 3, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 1, 1.5, 0, 1.5, 0);
    }

    /** The landing of a dive or of the ultimate: the crater, the blow all round, the dust; then the flight is over. */
    static void land(ServerPlayer player, Flight f, boolean impact) {
        FLIGHTS.remove(player.getUUID());
        player.setNoGravity(false);
        if (!impact) return;
        ServerLevel level = player.serverLevel();
        var patch = com.murimblock.integration.epicfight.EpicFightBridge.patch(player);
        if (patch != null) patch.playAnimationSynchronized(MurimAnimations.MSF_LAND, 0.0F);
        boolean ult = f.technique == MartialArts.HEAVEN_FALLING_MOUNTAIN;
        double radius = ult ? ULT_CRATER : f.technique == MartialArts.EARTH_BREAKER ? 4 : f.technique == MartialArts.BULL_CHARGE ? 3 : 2.5;
        BlockPos ground = player.blockPosition().below();
        BlockState state = level.getBlockState(ground);
        credit(player, ShatteredGround.crater(level, ground, radius, ult ? ULT_CRATER_DEPTH : 2), f.technique == MartialArts.MOUNTAIN_CRUSHING_PUNCH);
        float core = TechniquePower.damage(player, MartialArts.MOUNTAIN_SHATTERING_FIST, ult ? ULT_CORE_SHARE : DIVE_SHARE);
        float edge = TechniquePower.damage(player, MartialArts.MOUNTAIN_SHATTERING_FIST, ult ? ULT_SHARE : DIVE_SHARE * 0.6F);
        double reach = radius + 1;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach, 3, reach), e -> e != player && e.isAlive())) {
            double d = Math.sqrt(e.distanceToSqr(player));
            if (d > reach) continue;
            if (blow(player, e, d <= (ult ? ULT_CORE : radius / 2) ? core : edge)) {
                Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
                Vec3 push = away.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, player.getYRot()) : away.normalize();
                double k = 1 - d / reach;
                e.push(push.x * (0.6 + k), 0.4 + 0.5 * k, push.z * (0.6 + k));
                if (ult) e.igniteForSeconds(3);
            }
        }
        if (!state.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), player.getX(), player.getY() + 0.2, player.getZ(), ult ? 300 : 80, radius * 0.6, 0.3, radius * 0.6, 0.35);
        level.sendParticles(ult ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5, player.getZ(), ult ? 2 : 2, radius * 0.3, 0.1, radius * 0.3, 0);
        if (ult) level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.3, player.getZ(), 160, radius * 0.6, 0.2, radius * 0.6, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, ult ? 3.0F : 1.4F, ult ? 0.5F : 0.8F);
        if (ult) level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.7F);
        LongFist.ring(level, player.position(), radius + 1);
        MartialProgress.addMastery(player, art(), f.technique.id(), 1);
        countGroupLater(player, 2);
    }

    /** Tests: a dive or the ultimate lands here now (on the server a mock player does not fall by itself). */
    public static void landForTests(ServerPlayer player, MartialArts.Technique technique) {
        HITS.put(player.getUUID(), new HashSet<>());
        land(player, new Flight(technique, Stage.FALL), true);
    }

    /** Whether this player is in one of the art's flights now (tests). */
    public static boolean flying(Player player) {
        return FLIGHTS.containsKey(player.getUUID());
    }

    // ---------------------------------------------------------------- hits

    /** The enemies each player's last technique has struck (for Shake the Earth). */
    private static final Map<UUID, Set<UUID>> HITS = new HashMap<>();

    /** A blow of the art that is not an animation's: damage from the player, a guard raised in time stops it. */
    static boolean blow(ServerPlayer player, LivingEntity e, float damage) {
        if (!MartialCombat.isEnemy(e, player) || MurimGuardSkill.parriedWithin(e, 6)) return false;
        boolean hurt = e.hurt(player.damageSources().playerAttack(player), damage);
        if (hurt) {
            e.hurtMarked = true;
            Set<UUID> hits = HITS.get(player.getUUID());
            if (hits != null) hits.add(e.getUUID());
        }
        return hurt;
    }

    /** Whether the technique's own box has just struck this body (it then takes no second share). */
    private static boolean justStruck(ServerPlayer player, LivingEntity e) {
        return e.getLastHurtByMob() == player && e.tickCount - e.getLastHurtByMobTimestamp() <= 10;
    }

    /** After a while, whether the technique struck enough enemies at once for Shake the Earth. */
    private static void countGroupLater(ServerPlayer player, int ticks) {
        MartialCombat.later(player, ticks, () -> {
            Set<UUID> hits = HITS.remove(player.getUUID());
            if (hits != null && hits.size() >= GROUP) MartialProgress.count(player, MartialArts.SHAKE_THE_EARTH_GROUPS, 1);
        });
    }

    /** Blocks broken: for Heaven's Weight always, for Shatter the Stone by Mountain-Crushing Punch. */
    private static void credit(ServerPlayer player, int broken, boolean punch) {
        if (broken <= 0) return;
        MartialProgress.count(player, MartialArts.HEAVENS_WEIGHT_BLOCKS, broken);
        if (punch) MartialProgress.count(player, MartialArts.SHATTER_THE_STONE_BLOCKS, broken);
    }

    /** What one of the art's hits does besides its damage. */
    static void onHit(ServerPlayer player, LivingEntity target, AnimationAccessor<? extends StaticAnimation> animation, boolean enemy) {
        if (enemy && !isStrike(animation)) {
            Set<UUID> hits = HITS.get(player.getUUID());
            if (hits != null) hits.add(target.getUUID());
        }
        if (MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target)) return;
        ServerLevel level = player.serverLevel();
        double resist = Math.max(0.2, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (isStrike(animation)) {
            // Earthshaker: the ground shakes round the target.
            float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * SPLASH_SHARE);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SPLASH_REACH, 0.5, SPLASH_REACH),
                    e -> e != player && e != target && e.isAlive())) blow(player, e, damage);
            BlockState under = level.getBlockState(target.blockPosition().below());
            if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), target.getX(), target.getY() + 0.1, target.getZ(), 8, 0.6, 0.05, 0.6, 0.1);
        } else if (animation == MurimAnimations.MSF_CRUSHER) {
            Vec3 dir = Vec3.directionFromRotation(0, player.getYRot());
            for (int i = 0; i < PUSH_STEPS.length; i++) {
                Vec3 step = new Vec3(dir.x * PUSH_STEPS[i] * resist, i < 2 ? 0.15 : 0, dir.z * PUSH_STEPS[i] * resist);
                MartialCombat.later(player, i + 1, () -> carry(target, step));
            }
        } else if (animation == MurimAnimations.MSF_EARTH_BREAKER) {
            for (int i = 0; i < EARTH_STEPS.length; i++) {
                Vec3 step = new Vec3(0, EARTH_STEPS[i] * resist, 0);
                MartialCombat.later(player, i + 1, () -> carry(target, step));
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EARTH_SLOW_TICKS, 1));
            target.hurtMarked = true;
        } else if (animation == MurimAnimations.MSF_SKY_BREAKER) {
            for (int i = 0; i < SKY_STEPS.length; i++) {
                Vec3 step = new Vec3(0, SKY_STEPS[i] * Math.max(0.5, resist), 0);
                MartialCombat.later(player, i + 1, () -> carry(target, step));
            }
            // The column of earth bursting under it.
            BlockPos under = target.blockPosition().below();
            BlockState state = level.getBlockState(under);
            for (int y = 0; y < 2; y++) ShatteredGround.smash(level, under.below(y));
            if (!state.isAir()) for (int y = 0; y < 5; y++)
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), target.getX(), target.getY() + y, target.getZ(), 14, 0.4, 0.3, 0.4, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 1.2F);
        }
    }

    /**
     * The flights of the bodies struck (blocks a tick): Epic Fight's stun cancels any speed given to a body, so it is
     * carried by hand, as Inch Force's: the punch's push back (some four blocks), the Earth Breaker's toss (about one and a
     * half), the Sky Breaker's throw to the sky (some eight).
     */
    static final double[] PUSH_STEPS = {1.1, 0.9, 0.8, 0.6, 0.4, 0.2};
    static final double[] EARTH_STEPS = {0.6, 0.45, 0.3, 0.15};
    static final double[] SKY_STEPS = {1.5, 1.3, 1.1, 1.0, 0.85, 0.7, 0.6, 0.45, 0.3, 0.2, 0.1};

    /** One tick of a flight: the body moved by {@code step} (a mob through its collisions; a player by a teleport), its fall forgiven. */
    private static void carry(LivingEntity target, Vec3 step) {
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

    // ---------------------------------------------------------------- Stone Hands

    /** In the stance, bare hands break stone and wood three times faster. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        if (!guardsBareHanded(player)) return;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE)) event.setNewSpeed(event.getNewSpeed() * STONE_HANDS);
    }

    // ---------------------------------------------------------------- the finish: Mountain Burial

    /** Three blows of the fist driving the body into the ground; the last opens a small crater under it and kills. */
    public static void finishTick(ServerPlayer executor, ServerPlayer victim, int elapsed) {
        ServerLevel level = executor.serverLevel();
        for (int i = 0; i < FINISH_BLOWS.length; i++) {
            if (elapsed != FINISH_BLOWS[i]) continue;
            boolean last = i == FINISH_BLOWS.length - 1;
            BlockState under = level.getBlockState(victim.blockPosition().below());
            if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), victim.getX(), victim.getY() + 0.2, victim.getZ(), last ? 120 : 40, 1.0, 0.2, 1.0, 0.25);
            level.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.2, victim.getZ(), last ? 3 : 1, 0.4, 0.1, 0.4, 0);
            level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, last ? 1.4F : 0.7F, last ? 0.6F : 1.0F);
            if (last) {
                ShatteredGround.crater(level, victim.blockPosition().below(), 2.2, 2);
                // Rocks falling back onto the body.
                for (int k = 0; k < 12; k++)
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                            victim.getX(), victim.getY() + 2 + k * 0.2, victim.getZ(), 6, 0.8, 0.2, 0.8, 0.05);
            }
        }
    }

    // ---------------------------------------------------------------- state

    static void forget(@Nullable UUID player) {
        if (player == null) { FLIGHTS.clear(); HITS.clear(); }
        else { FLIGHTS.remove(player); HITS.remove(player); }
    }
}
