package com.murimblock.martial;

import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.MurimEntities;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * Qi sent off a blade by the Azure Cloud Sword Art, flying straight (no gravity). Three kinds:
 * <ul>
 *   <li>{@link #WAVE}: Thousand Drizzle with no enemy within its thrusts' reach. A great ghost of the sword, point first
 *   (drawn by QiBladeRenderer); stops on the first body it meets, without pushing it, and interrupts what that body was
 *   doing (a short stun);</li>
 *   <li>{@link #NEEDLE}: Storm of Needles' last thrust. Thin, goes through everything up to 8 blocks;</li>
 *   <li>{@link #CRESCENT}: Azure Heaven Crescent. A tall vertical blade going through everything up to 10 blocks.</li>
 * </ul>
 * The needle and the crescent are drawn with pale azure specks for now (no model).
 * Its damage is a share of its owner's attack (so of the sword in hand), as a projectile: a guard parries it like an arrow.
 * A hit counts for its technique's mastery like the technique's own hits.
 */
public final class QiBladeEntity extends Projectile {
    public static final byte WAVE = 0, NEEDLE = 1, CRESCENT = 2;
    /** Speed (blocks/tick), range (blocks), share of the owner's attack, by kind. */
    private static final double[] SPEED = {1.1, 1.6, 1.2}, RANGE = {8, 8, 10}, DAMAGE = {0.8, 0.5, 1.0};
    /** Half-width and height of what it sweeps, by kind. */
    private static final double[] HALF_WIDTH = {0.45, 0.25, 0.5}, HEIGHT = {0.9, 0.4, 2.2};
    private static final DustParticleOptions AZURE = new DustParticleOptions(new Vector3f(0.62F, 0.85F, 1.0F), 1.0F);
    private static final DustParticleOptions AZURE_BRIGHT = new DustParticleOptions(new Vector3f(0.85F, 0.95F, 1.0F), 0.7F);

    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(QiBladeEntity.class, EntityDataSerializers.BYTE);

    private double travelled;
    private float damage;
    @Nullable private ResourceLocation technique;
    private final Set<UUID> struck = new HashSet<>();

    public QiBladeEntity(EntityType<? extends QiBladeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A blade of Qi leaving `owner` toward where they look (level), at chest height. */
    public static QiBladeEntity send(ServerPlayer owner, byte kind, ResourceLocation technique) {
        QiBladeEntity blade = new QiBladeEntity(MurimEntities.QI_BLADE.get(), owner.level());
        blade.setOwner(owner);
        blade.entityData.set(KIND, kind);
        blade.technique = technique;
        blade.damage = (float) (owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * DAMAGE[kind]);
        Vec3 dir = Vec3.directionFromRotation(0, owner.getYRot());
        double y = kind == CRESCENT ? owner.getY() : owner.getY() + owner.getBbHeight() * 0.55 - HEIGHT[kind] / 2;
        // The wave's position is its point, which leaves a little ahead of the blade in hand.
        double ahead = kind == WAVE ? 1.2 : 0.8;
        blade.moveTo(owner.getX() + dir.x * ahead, y, owner.getZ() + dir.z * ahead, owner.getYRot(), 0);
        blade.setDeltaMovement(dir.scale(SPEED[kind]));
        owner.level().addFreshEntity(blade);
        return blade;
    }

    /** How many ticks a blade of this kind flies before it ends (its range at its speed). */
    public static int lifetime(byte kind) {
        return (int) Math.ceil(RANGE[kind] / SPEED[kind]);
    }

    /** The height of what a blade of this kind sweeps, from its position up (blocks). */
    public static double sweptHeight(byte kind) {
        return HEIGHT[kind];
    }

    public byte kind() {
        return entityData.get(KIND);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, WAVE);
    }

    @Override public void tick() {
        super.tick();
        byte kind = kind();
        Vec3 from = position(), motion = getDeltaMovement();
        if (level().isClientSide) {
            // Each client flies it itself, smoothly: a straight line at its kind's speed along its heading (the server's
            // position updates would only make it jerk; the server decides what it hits and when it ends).
            motion = Vec3.directionFromRotation(0, getYRot()).scale(SPEED[kind]);
            Vec3 to = from.add(motion);
            specks(kind, to, motion);
            setPos(to);
            return;
        }
        Vec3 to = from.add(motion);
        // A solid block ends it.
        HitResult wall = level().clip(new ClipContext(from.add(0, HEIGHT[kind] / 2, 0), to.add(0, HEIGHT[kind] / 2, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() == HitResult.Type.BLOCK) {
            discard();
            return;
        }
        // What it sweeps this tick (a box around its path, wide as the blade, from its foot up to its height).
        AABB swept = new AABB(from, to).inflate(HALF_WIDTH[kind], 0, HALF_WIDTH[kind]).expandTowards(0, HEIGHT[kind], 0);
        for (Entity e : level().getEntities(this, swept, this::canHitEntity)) {
            if (!(e instanceof LivingEntity target) || !struck.add(target.getUUID())) continue;
            strike(target);
            if (kind == WAVE) {
                discard();
                return;
            }
        }
        setPos(to);
        travelled += motion.length();
        if (travelled >= RANGE[kind] || tickCount > 40) discard();
    }

    /** The client flies it on its own (see tick): the server's position updates are not followed. */
    @Override public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (position().distanceToSqr(x, y, z) > 9) setPos(x, y, z);
    }

    @Override protected boolean canHitEntity(Entity target) {
        return target.isAlive() && target != getOwner() && target.isPickable() && !target.isSpectator()
                && !(getOwner() != null && target.isPassengerOfSameVehicle(getOwner()));
    }

    private void strike(LivingEntity target) {
        Entity owner = getOwner();
        boolean hurt = target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), damage);
        if (!hurt) return;
        // The wave interrupts: what the target was doing stops. The crescent staggers. Not a guard that took it.
        boolean guarded = MurimGuardSkill.parriedThisTick(target) || MurimGuardSkill.guardedThisTick(target);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        if (patch != null && kind() != NEEDLE && !guarded) patch.applyStun(kind() == WAVE ? StunType.SHORT : StunType.LONG, kind() == WAVE ? 0.4F : 0.6F);
        if (owner instanceof ServerPlayer player && technique != null) MartialCombat.qiBladeHit(player, target, technique);
    }

    /** Pale azure specks along the blade (each client draws its own, nothing heavy goes through the network). */
    private void specks(byte kind, Vec3 at, Vec3 motion) {
        Vec3 dir = motion.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : motion.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        switch (kind) {
            case WAVE -> {
                // Drawn as a ghost sword (QiBladeRenderer), no specks.
            }
            case NEEDLE -> {
                for (int i = 0; i < 4; i++) {
                    Vec3 p = at.subtract(dir.scale(i * 0.4)).add(0, HEIGHT[kind] * 0.5, 0);
                    level().addParticle(i == 0 ? AZURE_BRIGHT : AZURE, p.x, p.y, p.z, 0, 0, 0);
                }
            }
            default -> {
                // A tall crescent standing on its points.
                for (int i = -5; i <= 5; i++) {
                    double k = i / 5.0;
                    Vec3 p = at.add(dir.scale(-0.4 * (1 - k * k))).add(0, HEIGHT[kind] * (0.5 + 0.5 * k), 0);
                    level().addParticle(Mth.abs(i) % 2 == 0 ? AZURE : AZURE_BRIGHT, p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
    }

    // ---------- a blade lives a second or two and never needs to survive a reload ----------

    @Override public boolean shouldBeSaved() {
        return false;
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
}
