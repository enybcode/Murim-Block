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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The remnant soul of an old master who dwells in a cultivator's inner world (a classic of the genre: a soul hidden in
 * the dantian). It does not fight and cannot be hurt; spoken to, it talks (SoulDialogue): the day's missions, advice, and
 * at the gate of a realm, the trial against oneself. Drawn with
 * GeckoLib: a legless ghost whose robe thins into a tail of mist (scripts/animation/remnant-soul.cjs).
 */
public class RemnantSoulEntity extends PathfinderMob implements GeoEntity, com.murimblock.dialogue.DialogueService.Speaker {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client side: the last tick its renderer let a wisp of Qi go. */
    public int wispTick = -1;

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
        if (owner != null && (tickCount == 1 || tickCount % 20 == 0) && level() instanceof net.minecraft.server.level.ServerLevel server) {
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
        if (player instanceof ServerPlayer server) SoulDialogue.greet(server, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void choose(ServerPlayer player, String option) {
        SoulDialogue.choose(player, this, option);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
}
