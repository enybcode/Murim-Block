package com.murimblock.mob;

import com.murimblock.client.WaterBallLights;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The crab man's water ball, drawn by GeckoLib from the Blockbench model (crabe/human-crab-animated-v2/water-ball),
 * no Minecraft particles; its reflections glow a little in the dark (GeckoLib glow mask) and its burst flashes
 * light on its surroundings (Photon dynamic light). Life: CHARGING in the claw (follows its owner), FLYING like a slow arrow (vanilla arrow
 * drag and gravity, launched at 0.8 block/tick), then a burst that depends on what it hit: the floor, a wall or a
 * body (it then follows that body while the water runs down it). Big ball: 4 damage, Slowness I 3 s and a small
 * push; the small followers: 1 damage, Slowness I 2 s, a third of the push.
 */
public final class WaterBallEntity extends ThrowableProjectile implements GeoEntity {
    public static final byte CHARGING = 0, FLYING = 1, SPLASH_GROUND = 2, SPLASH_WALL = 3, SPLASH_ENTITY = 4;
    /** Ball radius in blocks at scale 1 (10 units wide in Blockbench). */
    public static final double RADIUS = 5 / 16.0;
    public static final float SPEED = 0.8F;
    public static final double GRAVITY = 0.05;
    /** Push given by the big ball, blocks/tick (a sword without Knockback gives about 0.4). */
    static final double KNOCKBACK = 0.45;
    /** Burst animations last 1.05 s. */
    static final int SPLASH_TICKS = 21;
    /** A ball nobody launched (its owner was interrupted or died) bursts where it is. */
    static final int MAX_CHARGE_TICKS = 40;
    // Claw position of the crab man at the release, from the animated model (mouthPoint, 87.5 % scale), in blocks.
    static final double CLAW_UP = 1.31, CLAW_FORWARD = 0.93, CLAW_RIGHT = 0.45, ORBIT = 0.44;

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(WaterBallEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(WaterBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPLASH_YAW = SynchedEntityData.defineId(WaterBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ORBIT_SLOT = SynchedEntityData.defineId(WaterBallEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation FORM = RawAnimation.begin().thenPlay("form").thenLoop("fly");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation[] SPLASH = {
            RawAnimation.begin().thenPlayAndHold("splash_ground"),
            RawAnimation.begin().thenPlayAndHold("splash_wall"),
            RawAnimation.begin().thenPlayAndHold("splash_entity")};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stateTicks;
    /** Launch waiting for its delay (the small bubbles leave one after the other). */
    @Nullable private Vec3 pendingVelocity;
    private int launchDelay;
    @Nullable private Entity followed;
    private Vec3 followOffset = Vec3.ZERO;

    public WaterBallEntity(EntityType<? extends WaterBallEntity> type, Level level) {
        super(type, level);
    }

    /** A ball forming in the owner's claw; slot 0 is the big one, 1+ the small ones circling it. */
    public static WaterBallEntity charging(LivingEntity owner, float scale, int slot) {
        WaterBallEntity ball = new WaterBallEntity(MurimEntities.WATER_BALL.get(), owner.level());
        ball.setOwner(owner);
        ball.entityData.set(SCALE, scale);
        ball.entityData.set(ORBIT_SLOT, slot);
        ball.refreshDimensions();
        ball.followClaw(owner);
        return ball;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, CHARGING);
        builder.define(SCALE, 1.0F);
        builder.define(SPLASH_YAW, 0.0F);
        builder.define(ORBIT_SLOT, 0);
    }

    public byte state() { return entityData.get(STATE); }
    public float ballScale() { return entityData.get(SCALE); }
    public float splashYaw() { return entityData.get(SPLASH_YAW); }
    public boolean isBig() { return entityData.get(ORBIT_SLOT) == 0; }
    public int slot() { return entityData.get(ORBIT_SLOT); }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) refreshDimensions();
        if (STATE.equals(key)) {
            stateTicks = 0;
            // Flash of light where it bursts (Photon, client only).
            if (level().isClientSide && state() >= SPLASH_GROUND) WaterBallLights.burst(this);
        }
    }

    @Override public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(ballScale());
    }

    @Override protected double getDefaultGravity() {
        return GRAVITY;
    }

    // ---------- life ----------

    @Override public void tick() {
        stateTicks++;
        byte state = state();
        if (state == CHARGING) {
            baseTick();
            Entity owner = getOwner();
            if (owner == null || !owner.isAlive()) {
                if (!level().isClientSide) discard();
                return;
            }
            followClaw(owner);
            if (pendingVelocity != null && --launchDelay <= 0 && !level().isClientSide) {
                setDeltaMovement(pendingVelocity);
                hasImpulse = true;
                pendingVelocity = null;
                entityData.set(STATE, FLYING);
            } else if (stateTicks > MAX_CHARGE_TICKS && !level().isClientSide) {
                splash(SPLASH_GROUND, getYRot(), null);
            }
            return;
        }
        if (state >= SPLASH_GROUND) {
            baseTick();
            setDeltaMovement(Vec3.ZERO);
            if (followed != null && followed.isAlive()) setPos(followed.position().add(followOffset));
            if (stateTicks >= SPLASH_TICKS && !level().isClientSide) discard();
            return;
        }
        super.tick();
        if (!level().isClientSide && tickCount > 200) discard();
    }

    /** In the claw, a little in front of the crab man; the small ones circle the big one (a turn per second). */
    void followClaw(Entity owner) {
        float yaw = owner instanceof LivingEntity living ? living.yBodyRot : owner.getYRot();
        double rad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 claw = owner.position().add(0, CLAW_UP, 0).add(forward.scale(CLAW_FORWARD)).add(right.scale(CLAW_RIGHT));
        int slot = entityData.get(ORBIT_SLOT);
        if (slot > 0) {
            double a = Math.toRadians(slot * 90 - 70 + (owner.tickCount + stateTicks) * 18);
            claw = claw.add(right.scale(Math.cos(a) * ORBIT)).add(0, Math.sin(a) * ORBIT, 0);
        }
        // The model is drawn around the middle of the hitbox.
        setPos(claw.x, claw.y - getBbHeight() / 2, claw.z);
        setDeltaMovement(Vec3.ZERO);
    }

    /** Launches toward `target` (at its chest) like a slow arrow, after `delayTicks`; null target: straight ahead. */
    public void launch(@Nullable LivingEntity target, float speed, float yawSpreadDegrees, int delayTicks) {
        Entity owner = getOwner();
        Vec3 from = position().add(0, getBbHeight() / 2, 0);
        Vec3 velocity;
        if (target != null) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
            velocity = aim(from, to, speed);
        } else {
            float yaw = owner != null ? owner.getYRot() : getYRot();
            velocity = Vec3.directionFromRotation(-4, yaw).scale(speed);
        }
        if (yawSpreadDegrees != 0) velocity = velocity.yRot((float) Math.toRadians(yawSpreadDegrees));
        pendingVelocity = velocity;
        launchDelay = delayTicks;
    }

    /**
     * Velocity that reaches `to` at `speed` blocks/tick horizontally, under the projectile's own drag (0.99) and
     * gravity (0.05): ticks to cover the distance, then the vertical speed found by bisection.
     */
    public static Vec3 aim(Vec3 from, Vec3 to, float speed) {
        double dx = to.x - from.x, dz = to.z - from.z, dy = to.y - from.y;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-4) return new Vec3(0, -speed, 0);
        double k = 1 - 0.01 * d / speed;
        double ticks = k > 0.05 ? Math.log(k) / Math.log(0.99) : 60;
        double low = -2, high = 2;
        for (int i = 0; i < 40; i++) {
            double mid = (low + high) / 2;
            if (heightAfter(mid, ticks) < dy) low = mid; else high = mid;
        }
        return new Vec3(dx / d * speed, (low + high) / 2, dz / d * speed);
    }

    /** Height gained after a fractional number of ticks: move, then drag, then gravity, like ThrowableProjectile. */
    static double heightAfter(double vy, double ticks) {
        double y = 0;
        int whole = (int) ticks;
        for (int i = 0; i < whole; i++) {
            y += vy;
            vy = vy * 0.99 - GRAVITY;
        }
        return y + vy * (ticks - whole);
    }

    // ---------- impacts ----------

    @Override protected boolean canHitEntity(Entity target) {
        // Never its thrower's side: the crab man and his armored crabs.
        if (target instanceof WaterBallEntity || target instanceof CrabManEntity || target instanceof ArmoredCrabEntity) return false;
        return state() == FLYING && super.canHitEntity(target);
    }

    @Override protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || state() != FLYING) return;
        Entity target = result.getEntity();
        Vec3 dir = horizontalDirection();
        Entity owner = getOwner();
        boolean big = isBig();
        target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), big ? 4.0F : 1.0F);
        // A ball caught on a perfect parry only splashes on the blade; on a normal parry it does not slow and pushes half.
        boolean parried = target instanceof LivingEntity l && MurimGuardSkill.parriedThisTick(l);
        boolean guarded = target instanceof LivingEntity l && MurimGuardSkill.guardedThisTick(l);
        if (target instanceof LivingEntity living && !parried) {
            if (!guarded) living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, big ? 60 : 40, 0), owner);
            // Small push along the flight. A direct push: Epic Fight replaces the vanilla hurt knockback with its
            // stun system, which gives nothing for a projectile.
            double resist = living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            double push = (big ? KNOCKBACK : KNOCKBACK / 3) * Math.max(0, 1 - resist) * (guarded ? 0.5 : 1);
            if (push > 0) {
                living.push(dir.x * push, 0.1 * push / KNOCKBACK, dir.z * push);
                living.hurtMarked = true;
            }
        }
        // Water: puts the fire out.
        target.clearFire();
        // The burst sits in front of the body at chest height and follows it while the water runs down.
        double s = ballScale();
        Vec3 centre = new Vec3(target.getX(), target.getY() + Math.min(1.25, target.getBbHeight() * 0.7), target.getZ())
                .subtract(dir.scale(9.5 / 16 * s));
        splash(SPLASH_ENTITY, yawOf(dir), target);
        placeCentre(centre);
        followed = target;
        followOffset = position().subtract(target.position());
    }

    @Override protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide || state() != FLYING) return;
        Direction face = result.getDirection();
        double r = RADIUS * ballScale();
        if (face == Direction.UP) {
            splash(SPLASH_GROUND, yawOf(horizontalDirection()), null);
            placeCentre(result.getLocation().add(0, r, 0));
        } else {
            // Walls and ceilings: the water clings, spreads and runs down. Model -Z points into the surface.
            Vec3 normal = face.getAxis().isHorizontal() ? Vec3.atLowerCornerOf(face.getNormal()) : horizontalDirection().scale(-1);
            splash(SPLASH_WALL, yawOf(normal.scale(-1)), null);
            placeCentre(result.getLocation().add(normal.scale(r)));
        }
    }

    private void splash(byte kind, float yaw, @Nullable Entity target) {
        entityData.set(SPLASH_YAW, yaw);
        entityData.set(STATE, kind);
        stateTicks = 0;
        pendingVelocity = null;
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
    }

    private void placeCentre(Vec3 centre) {
        setPos(centre.x, centre.y - getBbHeight() / 2, centre.z);
    }

    private Vec3 horizontalDirection() {
        Vec3 v = getDeltaMovement().multiply(1, 0, 1);
        if (v.lengthSqr() < 1.0E-6) v = Vec3.directionFromRotation(0, getYRot()).multiply(1, 0, 1);
        return v.normalize();
    }

    /** Render yaw (degrees, around +Y) that turns the model's -Z toward `dir`. */
    static float yawOf(Vec3 dir) {
        return (float) Math.toDegrees(Mth.atan2(-dir.x, -dir.z));
    }

    // ---------- saving: a ball is short-lived and never needs to survive a reload ----------

    @Override public boolean shouldBeSaved() {
        return false;
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }

    // ---------- GeckoLib ----------

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            byte s = state.getAnimatable().state();
            if (s >= SPLASH_GROUND) return state.setAndContinue(SPLASH[s - SPLASH_GROUND]);
            return state.setAndContinue(s == CHARGING ? FORM : FLY);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
