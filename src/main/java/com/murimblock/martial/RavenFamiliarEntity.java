package com.murimblock.martial;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Raven Messenger Art's familiar (docs/combat/styles/raven-art.md): the Shadow Raven, a big black raven with amber
 * eyes and a messenger's tube on its back (geo/raven_familiar, scripts/animation/raven-familiar.cjs). It belongs to
 * someone (a student of the art, or the Crow Master) and keeps by them: flying at their shoulder while they move, and
 * landing on the ground beside them when they stand still a while. Sent ahead (Halls Step), it flies to a place first,
 * and the door opens where it is. It cannot be struck, touches nothing and is never saved: the art calls it back.
 */
public class RavenFamiliarEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(RavenFamiliarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> PERCHED = SynchedEntityData.defineId(RavenFamiliarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation PERCH = RawAnimation.begin().thenLoop("animation.raven_familiar.perch");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.raven_familiar.fly");
    /** How long its owner stands still before it lands beside them (ticks). */
    static final int LAND_AFTER = 50;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable private Vec3 sentTo;
    private int sentUntil, still;
    @Nullable private Vec3 ownerWas;

    public RavenFamiliarEntity(EntityType<? extends RavenFamiliarEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty()).define(PERCHED, false);
    }

    public RavenFamiliarEntity owner(UUID owner) {
        entityData.set(OWNER, Optional.of(owner));
        return this;
    }

    public Optional<UUID> ownerId() {
        return entityData.get(OWNER);
    }

    /** Whether it sits on the ground now (else it flies). */
    public boolean perched() {
        return entityData.get(PERCHED);
    }

    @Nullable LivingEntity owner() {
        if (!(level() instanceof ServerLevel level)) return null;
        UUID id = ownerId().orElse(null);
        if (id == null) return null;
        Player player = level.getServer().getPlayerList().getPlayer(id);
        if (player != null) return player;
        return level.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    /** Flies to this place, there in `ticks` ticks, and stays a moment. */
    public void sendTo(Vec3 at, int ticks) {
        sentTo = at;
        sentUntil = tickCount + Math.max(1, ticks) + 10;
        entityData.set(PERCHED, false);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 0.8F, 0.7F);
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        LivingEntity owner = owner();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            discard();
            return;
        }
        Vec3 target;
        double speed;
        boolean land = false;
        if (sentTo != null && tickCount < sentUntil) {
            target = sentTo.add(0, 0.6, 0);
            speed = Math.max(0.6, position().distanceTo(target) / Math.max(1, sentUntil - 10 - tickCount));
        } else {
            sentTo = null;
            Vec3 at = owner.position();
            still = ownerWas != null && ownerWas.distanceToSqr(at) < 0.0004 && owner.onGround() ? still + 1 : 0;
            ownerWas = at;
            float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw)), ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
            if (still > LAND_AFTER) {
                target = at.add(right.scale(1.3)).add(ahead.scale(0.4));
                land = true;
            } else {
                double bob = Math.sin(tickCount * 0.12) * 0.15;
                target = at.add(right.scale(0.95)).add(ahead.scale(-0.35)).add(0, owner.getBbHeight() + 0.25 + bob, 0);
            }
            speed = Math.max(0.12, position().distanceTo(target) * 0.3);
        }
        Vec3 to = target.subtract(position());
        double d = to.length();
        if (d > 24) {
            // Lost behind: it comes back through the halls.
            RavenFx.feathers((ServerLevel) level(), target, 8, 0.4);
            setPos(target.x, target.y, target.z);
        } else if (d > 0.01) {
            Vec3 step = to.scale(Math.min(1, speed / d));
            setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
            if (step.horizontalDistanceSqr() > 0.0025) setYRot((float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90F);
        }
        boolean down = land && d < 0.15;
        if (down) setYRot(owner.getYRot());
        if (down != perched()) entityData.set(PERCHED, down);
        if (!down && random.nextInt(400) == 0) RavenFx.feathers((ServerLevel) level(), position().add(0, 0.3, 0), 1, 0.1);
        if (random.nextInt(900) == 0) level().playSound(null, getX(), getY(), getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.5F, 0.45F);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 64 * 64; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> state.setAndContinue(perched() ? PERCH : FLY)));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
