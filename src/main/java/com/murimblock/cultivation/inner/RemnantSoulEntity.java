package com.murimblock.cultivation.inner;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The remnant soul of an old master who dwells in a cultivator's inner world (a classic of the genre: a soul hidden in
 * the dantian). It does not fight and cannot be hurt; spoken to, it gives the day's missions (InnerMissions).
 */
public class RemnantSoulEntity extends PathfinderMob {
    public RemnantSoulEntity(EntityType<? extends RemnantSoulEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
        setCustomName(Component.translatable("entity.murimblock.remnant_soul"));
        setCustomNameVisible(true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
    }

    /** Whose island it dwells on (null for one made by hand). */
    @javax.annotation.Nullable private java.util.UUID owner;

    public void setOwner(java.util.UUID owner) {
        this.owner = owner;
    }

    @Override
    public void tick() {
        super.tick();
        // Only the island's own soul stays (a duplicate left by a rebuild takes itself away).
        if (owner != null && tickCount % 40 == 0 && level() instanceof net.minecraft.server.level.ServerLevel server) {
            java.util.UUID own = InnerWorldData.get(server.getServer().overworld()).soul(owner);
            if (own != null && !own.equals(getUUID())) discard();
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10, 1.0F));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) InnerMissions.talk(server, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
}
