package com.murimblock.cultivation.inner;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A mote of scattered Qi floating over the inner world (the mission "Gather the Scattered Qi"): its owner gathers it by
 * touching it. Drawn as a bright glowing point, bobbing.
 */
public class QiMoteEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(QiMoteEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** It fades away after this long if not gathered (ticks). */
    public static final int LIFETIME = 2400;

    public QiMoteEntity(EntityType<? extends QiMoteEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setOwner(UUID owner) {
        entityData.set(OWNER, Optional.of(owner));
    }

    @Nullable public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount > LIFETIME) {
            discard();
            return;
        }
        UUID owner = owner();
        ServerPlayer player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level()) {
            if (tickCount > 40) discard();
            return;
        }
        if (player.getBoundingBox().inflate(0.6).contains(position())) {
            InnerMissions.moteGathered(player, this);
            discard();
        }
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = owner();
        if (owner != null) tag.putUUID("Owner", owner);
    }
}
