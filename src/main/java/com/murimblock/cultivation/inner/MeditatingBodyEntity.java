package com.murimblock.cultivation.inner;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The body a cultivator leaves seated in the lotus while their mind is in the inner world (InnerWorldService). Drawn
 * with its owner's skin, its aura still burning. It cannot be killed, but a blow on it tears the cultivator back out,
 * with a deviation of the Qi: the body is the price of the journey, and a safe place to leave it matters. It goes away
 * when its owner comes back (or finds it gone).
 */
public class MeditatingBodyEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(MeditatingBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(MeditatingBodyEntity.class, EntityDataSerializers.STRING);
    /** The aura shown on the body: the owner's main style (empty: none), and that the reserve was full. */
    private static final EntityDataAccessor<String> ART = SynchedEntityData.defineId(MeditatingBodyEntity.class, EntityDataSerializers.STRING);

    public MeditatingBodyEntity(EntityType<? extends MeditatingBodyEntity> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public void setup(ServerPlayer owner) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(OWNER_NAME, owner.getGameProfile().getName());
        entityData.set(ART, com.murimblock.martial.MartialService.data(owner).mainArt().map(Object::toString).orElse(""));
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0);
        setYHeadRot(owner.getYRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(OWNER_NAME, "");
        builder.define(ART, "");
    }

    @Nullable public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    public String ownerName() {
        return entityData.get(OWNER_NAME);
    }

    public String art() {
        return entityData.get(ART);
    }

    @Override
    public Component getName() {
        return Component.literal(ownerName());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || tickCount % 20 != 0) return;
        // Its owner online and no longer on the journey this body is for: it has no reason to stay.
        UUID owner = owner();
        ServerPlayer player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (owner == null || (player != null && !InnerWorldService.isBodyOf(player, getUUID()))) discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source)) return false;
        UUID owner = owner();
        ServerPlayer player = owner == null ? null : ((ServerLevel) level()).getServer().getPlayerList().getPlayer(owner);
        if (player != null && source.getEntity() != player) InnerWorldService.bodyHit(player);
        return true;
    }

    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isAttackable() { return true; }
    @Override public boolean shouldShowName() { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        entityData.set(ART, tag.getString("Art"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = owner();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName());
        tag.putString("Art", art());
    }
}
