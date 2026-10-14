package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.SectLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.registry.entries.EpicFightAttributes;

/**
 * Sensei Yun (云师父), master of the Azure Cloud Sect, who keeps the Misty Bamboo Temple (docs/mobs/azure-sensei.md): an
 * old swordsman in azure robes and a straw hat, the left hand behind his back, the Azure Cloud low in his right. Neutral:
 * he strolls about his hall and watches whoever comes, and only draws on someone who strikes him (then he fights with
 * the Azure Cloud Sword Art, AzureSenseiPatch). Spoken to (right-click), he hands each player their own copy of the
 * Azure Cloud Sword Manual, once (its only source, SectLibrary). He stays where he was placed: he
 * never despawns and keeps to a few blocks around his home.
 * <p>A hard boss: 300 health, armour 12, hardly moved or staggered, 10 a cut. Below half his health he stops holding back
 * (his second phase: quicker, and the art's great techniques, AzureSenseiPatch); left alone he recovers, and back to full
 * health he is calm again. His health shows in an azure boss bar to the players near him, framed in clouds
 * (client/SenseiBossBarClouds). Killed by a player, he drops the Azure Cloud one time in 200 and the Azure Cloud Sword
 * Manual one time in four (loot_table/entities/azure_sensei.json).
 */
public class AzureSenseiEntity extends PathfinderMob {
    /** How far from his home he strolls (blocks). */
    public static final int HOME_RADIUS = 6;
    /** A stroll about once in this many ticks (12 s on average; the vanilla stroll comes every 6 s). */
    static final int STROLL_INTERVAL = 240;

    /** The share of his health below which he fights in earnest (his second phase). */
    public static final float SECOND_PHASE = 0.5F;
    /** Health he recovers each second while nobody fights him. */
    static final float RECOVERY = 2.0F;
    /** His second phase's quickness: half again his speed. */
    private static final ResourceLocation SECOND_PHASE_SPEED = ResourceLocation.fromNamespaceAndPath("murimblock", "sensei_second_phase");

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    public AzureSenseiEntity(EntityType<? extends AzureSenseiEntity> type, Level level) {
        super(type, level);
        xpReward = 100;
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createSenseiAttributes() {
        // His own 4 and the Azure Cloud's 6: 10 a cut, before the art's multipliers.
        return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300).add(Attributes.ARMOR, 12).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.STEP_HEIGHT, 1.0))
                .add(EpicFightAttributes.STUN_ARMOR, 20.0).add(EpicFightAttributes.IMPACT, 2.5);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.6);
        stroll.setInterval(STROLL_INTERVAL);
        goalSelector.addGoal(6, stroll);
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Neutral: only whoever strikes him.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Epic Fight installs his attack and chase goals for the sword in hand (AzureSenseiPatch). No vanilla melee goal.
    }

    /** With a target, he is aggressive: the clients see it (his brows knot, MobFaces). */
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        // Left alone, he recovers; back to full health, he is calm again.
        if (getTarget() == null && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(RECOVERY);
        if (!secondPhase() && getHealth() <= getMaxHealth() * SECOND_PHASE) enterSecondPhase();
        else if (secondPhase() && getHealth() >= getMaxHealth()) leaveSecondPhase();
        bossBar.setProgress(getHealth() / getMaxHealth());
        setAggressive(getTarget() != null);
    }

    // ---- the second phase

    /** Whether he fights in earnest now (below half his health, until he is whole again). */
    public boolean secondPhase() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        return speed != null && speed.hasModifier(SECOND_PHASE_SPEED);
    }

    private void enterSecondPhase() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SECOND_PHASE_SPEED, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        Component words = Component.translatable("message.murimblock.sensei.second_phase");
        for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) player.sendSystemMessage(words);
    }

    private void leaveSecondPhase() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SECOND_PHASE_SPEED);
    }

    // ---- the boss bar

    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    /** Whether this player sees his boss bar. */
    public boolean showsBossBarTo(ServerPlayer player) {
        return bossBar.getPlayers().contains(player);
    }

    /** His boss bar's fill, 0 to 1 (his health). */
    public float bossBarProgress() {
        return bossBar.getProgress();
    }

    /** Spoken to, he gives the manual (not to someone he is fighting). */
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || isAggressive()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            getLookControl().setLookAt(player);
            SectLibrary.give(serverPlayer, "message.murimblock.sensei.given", "message.murimblock.sensei.already");
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Keeps him about his hall, around this block. */
    public void setHome(BlockPos home) {
        restrictTo(home, HOME_RADIUS);
    }

    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.AZURE_CLOUD_SWORD.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        if (!hasRestriction()) setHome(blockPosition());
        setPersistenceRequired();
        return result;
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) tag.put("Home", NbtUtils.writeBlockPos(getRestrictCenter()));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
    }

    // An old man's voice: none at rest, the player's own when hurt.
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
}
