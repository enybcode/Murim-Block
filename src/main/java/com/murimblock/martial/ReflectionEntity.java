package com.murimblock.martial;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * A reflection of water of a Crescent Moon Halberd cultivator (CrescentMoonHalberd): it stands where it was left (or
 * where a technique sends it), drawn by client/ReflectionRenderer as its owner's own body, in the very pose they hold,
 * in translucent water-blue light. Mobs round about turn on it; the first blow struck at it by someone (but its owner)
 * shatters it into shards of water-glass. It lives a few seconds and is never saved.
 */
public class ReflectionEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int life = 80;

    public ReflectionEntity(EntityType<? extends ReflectionEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    public ReflectionEntity owned(Player owner, int ticks) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        life = ticks;
        return this;
    }

    public Optional<UUID> owner() {
        return entityData.get(OWNER);
    }

    @Nullable public Player ownerPlayer() {
        return owner().map(id -> level().getPlayerByUUID(id)).orElse(null);
    }

    public boolean ownedBy(Player player) {
        return owner().filter(player.getUUID()::equals).isPresent();
    }

    /** Lives this many more ticks from now. */
    public void liveFor(int ticks) {
        life = tickCount + ticks;
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount >= life || ownerPlayer() == null) discard();
    }

    /** A blow (but its owner's) shatters it. */
    @Override public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return false;
        if (source.getEntity() instanceof Player p && ownedBy(p)) return false;
        // Only a blow struck by someone: not a fall, a wall, fire or the void.
        if (source.getEntity() == null && source.getDirectEntity() == null) return false;
        shatter();
        return true;
    }

    /** Bursts into shards of water-glass and is gone (the damage it does, if any, is the technique's). */
    public void shatter() {
        if (!(level() instanceof ServerLevel level) || isRemoved()) return;
        double x = getX(), y = getY() + 1.0, z = getZ();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()), x, y, z, 50, 0.35, 0.6, 0.35, 0.25);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_STAINED_GLASS.defaultBlockState()), x, y, z, 20, 0.3, 0.5, 0.3, 0.2);
        level.sendParticles(ParticleTypes.SPLASH, x, y, z, 40, 0.4, 0.6, 0.4, 0.2);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.3, 0.5, 0.3, 0.06);
        level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.3F);
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6F, 1.6F);
        CrescentMoonFx.flash(level, position().add(0, 1, 0), 5);
        discard();
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(net.minecraft.world.entity.Entity entity) { }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean shouldShowName() { return false; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { }
    @Override public void readAdditionalSaveData(CompoundTag tag) { }
}
