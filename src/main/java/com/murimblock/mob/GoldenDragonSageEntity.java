package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * The Golden Dragon in human form (docs/mobs/golden-dragon.md): "the one above all". For now (Enzo, 16/10/2026) he only
 * waits in the Cloud Cliff Sanctuary's main hall, seated in meditation before the altar (SanctuaryPlacement places him):
 * he cannot be hurt, does not move, turns to a player who comes near; spoken to, he talks (GoldenDragonSageDialogue)
 * and makes crossing talismans to the other worlds. His dragon
 * form, his fight and what he gives are still to come.
 */
public class GoldenDragonSageEntity extends PathfinderMob implements com.murimblock.dialogue.DialogueService.Speaker {
    /** The way he faces when nobody is near: set where he is placed (toward the hall's doors in the sanctuary). */
    private float facing;

    public GoldenDragonSageEntity(EntityType<? extends GoldenDragonSageEntity> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createSageAttributes() {
        return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1));
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12, 1.0F));
    }

    /** Nothing harms him, not even the void's edge of a creative player's hand; /kill still removes him. */
    @Override public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override public boolean isPushable() {
        return false;
    }

    @Override protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide) {
            getLookControl().setLookAt(player, 30, 30);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) GoldenDragonSageDialogue.greet(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override public void choose(net.minecraft.server.level.ServerPlayer player, String option) {
        GoldenDragonSageDialogue.choose(player, this, option);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        if (!hasRestriction()) restrictTo(blockPosition(), 2);
        facing = getYRot();
        setPersistenceRequired();
        return result;
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) tag.put("Home", NbtUtils.writeBlockPos(getRestrictCenter()));
        tag.putFloat("Facing", facing);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(home -> restrictTo(home, 2));
        facing = tag.getFloat("Facing");
    }

    /** Where he sits: the centre of his home (the block before the altar), even if something nudged him; facing the doors when alone. */
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && hasRestriction() && tickCount % 20 == 0) {
            BlockPos home = getRestrictCenter();
            if (distanceToSqr(home.getX() + 0.5, getY(), home.getZ() + 0.5) > 0.25) moveTo(home.getX() + 0.5, getY(), home.getZ() + 0.5, getYRot(), getXRot());
            // Nobody near: he turns back to face the doors.
            if (level().getNearestPlayer(this, 12) == null) { setYRot(facing); setYBodyRot(facing); setYHeadRot(facing); }
        }
    }
}
