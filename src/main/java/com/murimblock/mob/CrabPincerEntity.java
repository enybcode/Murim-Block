package com.murimblock.mob;

import com.murimblock.integration.epicfight.MurimGuardSkill;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The giant pincer the Crab King calls out of the ground (docs/mobs/crab-king.md, "Pincer from the deep"; Enzo, 16/10:
 * really big, it sweeps, then strikes to its side, and tries to pinch the players): a Blockbench model drawn by GeckoLib
 * some seven blocks tall (scripts/animation/crab-pincer-geo.cjs, client/CrabPincerRenderer), its one animation its
 * whole life, which its blows follow tick for tick:
 * <ol>
 *   <li>a warning ({@link #EMERGE} ticks): the ground cracks and rumbles where it will come; then it bursts up;</li>
 *   <li>the sweep ({@link #SWEEP_FROM} to {@link #SWEEP_TO}): leant almost flat, it sweeps all round its foot at the ground:
 *   whoever is on the ground within {@link #SWEEP_REACH} is struck once and thrown away (jump it);</li>
 *   <li>the strike ({@link #STRIKE}): up again, it slams down on its side: a line {@link #REACH} long there, knocked down;</li>
 *   <li>the pinch ({@link #PINCH}): it lunges to its front, jaws wide, toward the king's target when called, and shuts:
 *   whoever is between them is hurt, thrown up and stunned;</li>
 *   <li>it sinks back and is gone at {@link #END}.</li>
 * </ol>
 * Its blows are the king's (a guard raised in time parries them); never on the king, his crabs or another pincer. Never
 * saved.
 */
public final class CrabPincerEntity extends Entity implements GeoEntity {
    /** Its moments, in ticks (crab-pincer-geo.cjs: 0.6, 1.4 to 2.2, 2.85, 3.7, 4.6 s). */
    public static final int EMERGE = 12, SWEEP_FROM = 28, SWEEP_TO = 44, STRIKE = 57, PINCH = 74, END = 92;
    /** How far it reaches lying down (blocks): its length at its size, curved at its joints. */
    public static final double REACH = 6.0, SWEEP_REACH = 6.2;
    /** The shares of the king's attack: the sweep, the strike, the pinch. */
    public static final float SWEEP_SHARE = 0.6F, STRIKE_SHARE = 1.0F, PINCH_SHARE = 0.9F;
    /**
     * Which way the model leans for a positive x (its pinch) and for a positive z (its strike), as seen in game: its
     * back (-1) or front (+1); its left (+1) or right (-1). The entity is turned so its pinch reaches the aim. Seen in
     * game (16/10): it pinches to its back and strikes to its right (GeckoLib turns as the right hand does).
     */
    static final int PINCH_SIDE = -1, STRIKE_SIDE = -1;
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private LivingEntity owner;
    private float attack = 10;
    /** Where it pinches, from its foot (a unit vector on the ground). */
    private Vec3 aim = new Vec3(0, 0, 1);
    private final Set<UUID> swept = new HashSet<>();

    public CrabPincerEntity(EntityType<? extends CrabPincerEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A giant pincer out of the ground at `at`, pinching toward `toward` (its sweep and strike round about it). */
    public static CrabPincerEntity call(ServerLevel level, CrabKingEntity king, Vec3 at, Vec3 toward) {
        CrabPincerEntity pincer = new CrabPincerEntity(MurimEntities.CRAB_PINCER.get(), level);
        Vec3 d = toward.subtract(at).multiply(1, 0, 1);
        pincer.aim = d.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, king.getYRot()) : d.normalize();
        // Its front where it pinches (its back when the model leans back to pinch).
        Vec3 front = pincer.aim.scale(PINCH_SIDE);
        float yaw = (float) Math.toDegrees(Math.atan2(-front.x, front.z));
        pincer.moveTo(at.x, at.y, at.z, yaw, 0);
        pincer.owner = king;
        pincer.attack = (float) king.getAttributeValue(Attributes.ATTACK_DAMAGE);
        level.addFreshEntity(pincer);
        return pincer;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        int t = tickCount;
        BlockState ground = server.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (t < EMERGE) {
            // The warning: the ground cracking and rumbling where it will come.
            if (!ground.isAir() && t % 2 == 0)
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.05, getZ(), 16, 1.6, 0.02, 1.6, 0.05);
            if (t == 1) server.playSound(null, getX(), getY(), getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.0F, 0.4F);
            if (t == 6) server.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 1.4F, 0.8F);
        } else if (t == EMERGE) {
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.5, getZ(), 120, 1.6, 1.2, 1.6, 0.3);
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 30, 1.6, 0.2, 1.6, 0.06);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.4F, 0.5F);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        if (t == SWEEP_FROM) server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.6F, 0.5F);
        if (t >= SWEEP_FROM && t <= SWEEP_TO) sweep(server);
        if (t == STRIKE) strike(server);
        if (t == PINCH) pinch(server);
        if (t >= END) discard();
    }

    private Predicate<LivingEntity> foe() {
        return e -> e.isAlive() && !e.isSpectator() && e != owner && !(e instanceof CrabKingEntity) && !(e instanceof ArmoredCrabEntity)
                && !(e instanceof CrabManEntity);
    }

    /** A blow of the king's on `victim` (a guard parries it as his): true if it landed. */
    private boolean blow(LivingEntity victim, float share) {
        victim.invulnerableTime = 0;
        boolean hurt = victim.hurt(owner != null ? damageSources().mobAttack(owner) : damageSources().generic(), attack * share);
        return hurt && !MurimGuardSkill.parriedThisTick(victim);
    }

    private static void stun(LivingEntity victim, StunType type, float time) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(victim, LivingEntityPatch.class);
        if (patch != null && victim.isAlive()) patch.applyStun(type, time);
    }

    /** The sweep: whoever is on the ground round it (not above the arm: jumped) is struck once, thrown away. */
    public void sweep(ServerLevel server) {
        AABB round = new AABB(getX() - SWEEP_REACH, getY() - 0.5, getZ() - SWEEP_REACH, getX() + SWEEP_REACH, getY() + 2.4, getZ() + SWEEP_REACH);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, round, foe())) {
            Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
            if (away.length() > SWEEP_REACH || victim.getY() > getY() + 1.2 && !victim.onGround() || !swept.add(victim.getUUID())) continue;
            if (!blow(victim, SWEEP_SHARE)) continue;
            Vec3 push = away.lengthSqr() < 1e-4 ? aim : away.normalize();
            victim.push(push.x * 0.9, 0.35, push.z * 0.9);
            victim.hurtMarked = true;
            stun(victim, StunType.SHORT, 0.4F);
        }
        if (tickCount % 3 == 0) {
            BlockState ground = server.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 30, SWEEP_REACH * 0.6, 0.05, SWEEP_REACH * 0.6, 0.1);
        }
    }

    /** Its side, where the strike falls: square to the aim. */
    public Vec3 strikeSide() {
        Vec3 front = aim.scale(PINCH_SIDE), left = new Vec3(front.z, 0, -front.x);
        return left.scale(STRIKE_SIDE);
    }

    /** The strike: slammed down on its side, along a line {@link #REACH} long: knocked down. */
    public void strike(ServerLevel server) {
        Vec3 side = strikeSide(), from = position();
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.7F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.5F);
        for (double d = 1; d <= REACH; d += 1) {
            Vec3 at = from.add(side.scale(d));
            BlockState ground = server.getBlockState(BlockPos.containing(at.x, getY() - 0.5, at.z));
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, getY() + 0.1, at.z, 14, 0.6, 0.1, 0.6, 0.15);
        }
        AABB box = new AABB(from, from.add(side.scale(REACH))).inflate(1.7, 0, 1.7).expandTowards(0, 3, 0);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, box, foe())) {
            Vec3 rel = victim.position().subtract(from).multiply(1, 0, 1);
            double along = rel.dot(side), off = rel.subtract(side.scale(along)).length();
            if (along < 0 || along > REACH + 0.5 || off > 1.7 || !blow(victim, STRIKE_SHARE)) continue;
            stun(victim, StunType.KNOCKDOWN, 1.4F);
        }
    }

    /** Where the pinch shuts (on the ground). */
    public Vec3 pinchPoint() {
        return position().add(aim.scale(REACH - 0.8));
    }

    /** The pinch: lunged to its front, jaws wide, shut: whoever is between them is hurt, thrown up, stunned. */
    public void pinch(ServerLevel server) {
        Vec3 at = pinchPoint();
        server.playSound(null, at.x, at.y, at.z, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.HOSTILE, 2.0F, 0.4F);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.TURTLE_EGG_CRACK, SoundSource.HOSTILE, 1.6F, 0.4F);
        AABB jaws = new AABB(at.x - 2.0, at.y - 0.5, at.z - 2.0, at.x + 2.0, at.y + 3.5, at.z + 2.0);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, jaws, foe())) {
            if (!blow(victim, PINCH_SHARE)) continue;
            victim.push(0, 0.8, 0);
            victim.hurtMarked = true;
            stun(victim, StunType.LONG, 0.9F);
        }
    }

    public Vec3 aim() {
        return aim;
    }

    /** It is drawn far beyond its box when it leans: culled by the whole reach. */
    @Override public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(REACH + 1, 2, REACH + 1);
    }

    @Override public boolean isPickable() {
        return false;
    }

    @Override public boolean isPushable() {
        return false;
    }

    @Override public boolean shouldBeSaved() {
        return false;
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    // ---------- GeckoLib: its one animation, from its first tick ----------

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(ATTACK)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
