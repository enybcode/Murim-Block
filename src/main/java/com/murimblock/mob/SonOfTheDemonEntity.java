package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The Son of the Demon (docs/mobs/bandit-warlord.md), the Bandit Warlord's second phase: his own legs and a colossus of
 * golden Qi risen from his waist, some seven blocks tall (client/SonOfTheDemonRenderer), on his own armature
 * (integration/epicfight/ColossusAnimations). Born of the Warlord's transformation (WarlordTransformation): while the
 * film plays he is {@link #awakening}, untouchable and without a foe; then the fight on the meteor in the sky. A golden
 * aura burns round him. His art is the Majin Fist, given one blow at a time by Enzo (for now one heavy straight right,
 * SonOfTheDemonPatch). His bar reads his title, "Son of the Demon"; he drops what the Warlord drops. Never despawns.
 */
public class SonOfTheDemonEntity extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKENING = SynchedEntityData.defineId(SonOfTheDemonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.25F), 2.0F);

    /** His title over the boss bar (Enzo: small, alone, no "Bandit Warlord"). */
    public static Component title() {
        return Component.translatable("bossbar.murimblock.son_of_the_demon").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private final ServerBossEvent bossBar = new ServerBossEvent(title(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
    /** The camp he was born in (its centre, BlockPos.asLong), if any. */
    @Nullable private Long camp;
    /** The middle of the arena in the sky he fights in (where he landed), if any: who falls off it is brought back. */
    @Nullable private net.minecraft.core.BlockPos arena;
    /** The Majin Fist's move he is playing and its beats run. */
    private final com.murimblock.mob.majin.MajinFist.State art = new com.murimblock.mob.majin.MajinFist.State();

    public SonOfTheDemonEntity(EntityType<? extends SonOfTheDemonEntity> type, Level level) {
        super(type, level);
        xpReward = 200;
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500).add(Attributes.ARMOR, 14).add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.5))
                .add(yesman.epicfight.registry.entries.EpicFightAttributes.STUN_ARMOR, 20.0)
                .add(yesman.epicfight.registry.entries.EpicFightAttributes.IMPACT, 3.5);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKENING, false);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override public boolean fireImmune() {
        return true;
    }

    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ---------------------------------------------------------------- his awakening (the film)

    /** While the transformation's film plays: untouchable, no foe, his bar hidden. */
    public void setAwakening(boolean awakening) {
        entityData.set(AWAKENING, awakening);
        // Still as a statue while the film plays: no looking about, no stroll; only the film moves him.
        setNoAi(awakening);
        if (awakening) getNavigation().stop();
        bossBar.setVisible(!awakening);
        if (awakening) setTarget(null);
    }

    /** His bar shown before the fight begins (the film's last shot: his title over it). */
    public void revealBar() {
        bossBar.setVisible(true);
    }

    public boolean awakening() {
        return entityData.get(AWAKENING);
    }

    public void setCamp(@Nullable Long camp) {
        this.camp = camp;
    }

    public void setArena(@Nullable net.minecraft.core.BlockPos arena) {
        this.arena = arena;
    }

    @Nullable public net.minecraft.core.BlockPos arena() {
        return arena;
    }

    /** His death takes the arena in the sky with it (ArenaCollapse): everyone and his loot are set down below first. */
    @Override public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && arena != null && isDeadOrDying()) com.murimblock.world.structure.site.ArenaCollapse.begin(server, arena);
    }

    /** How far below the arena's floor a fall counts as falling off it (blocks). */
    public static final int FALLEN = 24;
    /** What falling off the arena costs (health), before being brought back onto its floor. */
    public static final float FALL_COST = 6;

    /**
     * Whoever falls off the arena in the sky (some {@link #FALLEN} blocks below its floor, near it) is brought back onto its
     * floor, on firm rock near its rim, at a cost of {@link #FALL_COST} health; and so is he.
     */
    private void keepInArena(ServerLevel server) {
        if (arena == null || tickCount % 5 != 0) return;
        int floor = arena.getY();
        for (ServerPlayer p : server.players()) {
            double dx = p.getX() - arena.getX() - 0.5, dz = p.getZ() - arena.getZ() - 0.5;
            if (dx * dx + dz * dz > 160 * 160 || p.getY() > floor - FALLEN || p.isSpectator()) continue;
            net.minecraft.core.BlockPos back = com.murimblock.world.structure.site.WarlordTransformation.firm(server, arena, 30, 44, Math.atan2(dx, dz), 0);
            p.teleportTo(server, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, p.getYRot(), p.getXRot());
            p.fallDistance = 0;
            p.hurt(damageSources().fellOutOfWorld(), FALL_COST);
            server.playSound(null, back, SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.6F);
            server.sendParticles(ParticleTypes.CLOUD, back.getX() + 0.5, back.getY() + 0.5, back.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.05);
        }
        if (getY() < floor - 10) teleportTo(arena.getX() + 0.5, floor, arena.getZ() + 0.5);
    }

    @Override public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(awakening() ? null : target);
    }

    @Override public boolean isInvulnerableTo(DamageSource source) {
        return awakening() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    /** His golden aura: Qi rising round his body, sparks at his fists and hair. */
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && !awakening()) {
            com.murimblock.mob.majin.MajinFist.tick(this, server, art);
            keepInArena(server);
        }
        // While the film plays his body faces where he faces, on every side (the film frames him by it).
        if (awakening()) { yBodyRot = yBodyRotO = getYRot(); yHeadRot = yHeadRotO = getYRot(); }
        if (!(level() instanceof ServerLevel server) || tickCount % 2 != 0) return;
        double h = getBbHeight(), w = getBbWidth();
        server.sendParticles(GOLD, getX(), getY() + h * 0.5, getZ(), 6, w * 0.6, h * 0.45, w * 0.6, 0.02);
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + h * 0.4, getZ(), 2, w * 0.7, h * 0.4, w * 0.7, 0.04);
        if (tickCount % 6 == 0) server.sendParticles(ParticleTypes.FLAME, getX(), getY() + h * 0.95, getZ(), 3, 0.6, 0.3, 0.6, 0.02);
    }

    // ---------------------------------------------------------------- the boss bar

    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    public boolean showsBossBarTo(ServerPlayer player) {
        return bossBar.getPlayers().contains(player) && bossBar.isVisible();
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        setAggressive(getTarget() != null);
    }

    // ---------------------------------------------------------------- saving

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (awakening()) tag.putBoolean("Awakening", true);
        if (camp != null) tag.putLong("Camp", camp);
        if (arena != null) tag.putLong("Arena", arena.asLong());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // A film cut off by a stop does not resume: he wakes ready to fight.
        setAwakening(false);
        camp = tag.contains("Camp") ? tag.getLong("Camp") : null;
        arena = tag.contains("Arena") ? net.minecraft.core.BlockPos.of(tag.getLong("Arena")) : null;
    }

    // A voice from the depths: the warden's and the ravager's, low.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WARDEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WARDEN_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.55F; }
}
