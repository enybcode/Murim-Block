package com.murimblock.innerworld;

import com.murimblock.registry.MurimEntities;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The body a player leaves seated in meditation while their spirit is in the inner world: their skin, seated where
 * they sat. Any blow on it tears the player back. It removes itself once its owner is back.
 */
public final class MeditatingBody extends LivingEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(MeditatingBody.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int CHECK_INTERVAL = 20;

    public MeditatingBody(EntityType<? extends MeditatingBody> type, Level level) {
        super(type, level);
    }

    public static MeditatingBody seat(ServerPlayer player) {
        MeditatingBody body = new MeditatingBody(MurimEntities.MEDITATING_BODY.get(), player.level());
        body.entityData.set(OWNER, Optional.of(player.getUUID()));
        body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        body.setYHeadRot(player.getYRot());
        body.setYBodyRot(player.getYRot());
        body.setCustomName(player.getName());
        player.serverLevel().addFreshEntity(body);
        return body;
    }

    public Optional<UUID> owner() {
        return entityData.get(OWNER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && tickCount % CHECK_INTERVAL == 0) {
            // Left behind (owner back by another way, or died): vanish. Kept while the owner is offline.
            ServerPlayer owner = owner().map(id -> level.getServer().getPlayerList().getPlayer(id)).orElse(null);
            if (owner().isEmpty() || owner != null && !getUUID().equals(InnerWorld.state(owner).body())) {
                discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (level() instanceof ServerLevel level) {
            owner().map(id -> level.getServer().getPlayerList().getPlayer(id))
                    .ifPresent(owner -> InnerWorld.bodyStruck(owner, this));
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        owner().ifPresent(id -> tag.putUUID("owner", id));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("owner")) {
            entityData.set(OWNER, Optional.of(tag.getUUID("owner")));
        }
    }
}
