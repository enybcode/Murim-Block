package com.murimblock.cultivation.inner;

import com.murimblock.cultivation.CultivationData;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The Heart Demon (心魔): a cultivator's own darkness, faced in the inner world at each great breakthrough. A dark copy
 * of its owner, wearing their skin and their main style (a sword for the Azure Cloud Sword, bare fists for the Long
 * Fist), as strong as they are, less what they prepared (InnerWorldService). Slain, the realm opens; it wins, the Qi
 * deviates. As a "shadow", the same double half as strong: a sparring mission that costs nothing.
 */
public class HeartDemonEntity extends Monster {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(HeartDemonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> ART = SynchedEntityData.defineId(HeartDemonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SHADOW = SynchedEntityData.defineId(HeartDemonEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable private CultivationData ownerCultivation;
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.murimblock.heart_demon"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public HeartDemonEntity(EntityType<? extends HeartDemonEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 2).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48).add(Attributes.KNOCKBACK_RESISTANCE, 0.3));
    }

    /** Takes its owner's shape: their skin and style, their health and blows scaled by `strength`. */
    public void setup(ServerPlayer owner, double strength, boolean shadow) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(SHADOW, shadow);
        Optional<net.minecraft.resources.ResourceLocation> art = MartialService.data(owner).mainArt();
        entityData.set(ART, art.map(Object::toString).orElse(""));
        ownerCultivation = CultivationService.getCultivation(owner);
        boolean sword = art.filter(MartialArts.AZURE_CLOUD_SWORD::equals).isPresent();
        setItemSlot(EquipmentSlot.MAINHAND, sword ? new ItemStack(Items.IRON_SWORD) : ItemStack.EMPTY);
        setDropChance(EquipmentSlot.MAINHAND, 0);
        double health = Math.max(20, owner.getMaxHealth() * 1.2 * strength);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        setHealth((float) health);
        // Its blows: its owner's own (a bare hand, harder with the realm; the sword adds its own), scaled.
        var bonuses = com.murimblock.cultivation.CultivationRules.bonuses(ownerCultivation.realm());
        double attack = (1 + (sword ? 0 : bonuses.fist())) * (1 + bonuses.damage()) * strength;
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(0.5, attack));
        setCustomName(Component.translatable(shadow ? "entity.murimblock.heart_demon.shadow" : "entity.murimblock.heart_demon.of", owner.getDisplayName()));
        bossBar.setName(getDisplayName());
        setTarget(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(ART, "");
        builder.define(SHADOW, false);
    }

    @Nullable public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    public String art() {
        return entityData.get(ART);
    }

    public boolean shadow() {
        return entityData.get(SHADOW);
    }

    /** The realm its aura shows to Qi Sense: its owner's. */
    public Optional<CultivationData> cultivation() {
        return Optional.ofNullable(ownerCultivation);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                p -> p.getUUID().equals(owner())));
        // Epic Fight installs its attack and chase goals for what it holds (HeartDemonPatch).
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        setAggressive(getTarget() != null);
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 != 0) return;
        // Only ever in its owner's inner world, while they are there.
        UUID owner = owner();
        ServerPlayer player = owner == null ? null : ((ServerLevel) level()).getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level() || !player.isAlive()) discard();
        else if (getTarget() == null) setTarget(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && owner() != null) {
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(owner());
            if (player != null) InnerWorldService.demonSlain(player, this);
        }
    }

    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean shouldDropExperience() { return false; }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WARDEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SOUL_ESCAPE.value(); }
    @Override public float getVoicePitch() { return 0.6F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = owner();
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("Art", art());
        tag.putBoolean("Shadow", shadow());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        entityData.set(ART, tag.getString("Art"));
        entityData.set(SHADOW, tag.getBoolean("Shadow"));
    }
}
