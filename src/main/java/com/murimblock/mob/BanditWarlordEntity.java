package com.murimblock.mob;

import com.murimblock.integration.epicfight.BanditWarlordAnimations;
import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.world.structure.site.WarlordCrater;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The Bandit Warlord (docs/mobs/bandit-warlord.md), the great bandit camps' boss: a mountain of muscle who fights with
 * his fists, drawn twice a man's size (client/BanditWarlordRenderer). He is not in the world until a camp's whole
 * garrison is dead: then he falls from the sky onto its plaza like a meteor, wrapped in fire (BanditCampCount,
 * {@link #fallFromSky}); where he lands the camp becomes a crater for good (WarlordCrater), and he rises and roars. He
 * cannot be hurt while he falls and lands. Also a spawn egg and /summon (no crater then).
 * <p>Very tough (400 health, armour 12, barely moved by blows) and slow, his blows the Mountain-Shattering Fist's
 * (BanditWarlordPatch): a straight right that throws back, two fists on the ground whose shockwave throws everyone round
 * him up ({@link #earthShockwave}), a charge that knocks down, and an uppercut that throws its target high
 * ({@link #skyLaunch}); and two that break the ground (ShatteredGround: it comes back fifteen seconds later): the Mountain
 * Collapse, a fissure running out from his fist toward his foe ({@link #fissureStep}), and the Thousand-Jin Drop, a leap
 * onto his foe and a crater where he lands ({@link #leapLand}). Every one can be parried. Under half his health he roars and grows quicker, once. His health
 * shows in a purple boss bar to the players near him. He never despawns.
 */
public class BanditWarlordEntity extends Monster {
    /** The Earth Breaker's shockwave: its reach around the point before him, and its share of his attack. */
    public static final double SHOCKWAVE_RADIUS = 5.5;
    public static final float SHOCKWAVE_DAMAGE = 0.7F;
    /** Ticks before a shockwave or a launch in which a blow may have struck or been parried. */
    private static final int BLOW_WINDOW = 12;
    /** How fast the Sky Breaker throws its target up (blocks a tick: some seven blocks high). */
    public static final double SKY_LAUNCH = 1.35;
    /** His fall: how fast he comes down (blocks a tick, at most), and how far his landing throws everyone back. */
    public static final double FALL_SPEED = 1.15, IMPACT_REACH = 10;
    private static final ResourceLocation ENRAGED_SPEED = ResourceLocation.fromNamespaceAndPath("murimblock", "warlord_enraged");

    /** The Collapse's fissure: how far it runs (blocks, one a tick), its width either side, its share of his attack. */
    public static final int FISSURE_LENGTH = 14, FISSURE_HALF_WIDTH = 1;
    public static final float FISSURE_DAMAGE = 0.9F;
    /** The Drop: how high he leaps (blocks a tick up), the crater and shockwave where he lands, the shockwave's share. */
    public static final double LEAP_UP = 1.25, LEAP_CRATER = 5.0, LEAP_REACH = 8.0;
    public static final float LEAP_DAMAGE = 1.0F;

    private static final EntityDataAccessor<Boolean> METEOR = SynchedEntityData.defineId(BanditWarlordEntity.class, EntityDataSerializers.BOOLEAN);
    /** Ticks into his transformation (WarlordTransformation), -1 before: seen by the clients, who swell and light him. */
    private static final EntityDataAccessor<Integer> TRANSFORM = SynchedEntityData.defineId(BanditWarlordEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    /** The camp he falls on (its centre, BlockPos.asLong), if any: its crater opens where he lands. */
    @Nullable private Long camp;
    /** The camp he fell on, kept after his landing: the Son of the Demon's arena rises over it. */
    @Nullable private Long home;
    /** Ticks left of the landing, meanwhile untouchable and still. */
    private int landing;
    private boolean enraged, shockwaveSent, launched;
    private int stepIn;
    /** The fissure now running: from where, which way, how far it has gone (-1: none), and who it has struck. */
    @Nullable private Vec3 fissureFrom, fissureDir;
    private int fissureAt = -1;
    private final java.util.Set<LivingEntity> fissureStruck = new java.util.HashSet<>();
    private boolean collapseSent, leapt;
    /** The leap: his speed along the ground while he flies, and ticks in the air (-1: on the ground). */
    private Vec3 leapAlong = Vec3.ZERO;
    private int airborne = -1;

    public BanditWarlordEntity(EntityType<? extends BanditWarlordEntity> type, Level level) {
        super(type, level);
        xpReward = 100;
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createWarlordAttributes() {
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400).add(Attributes.ARMOR, 12).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.STEP_HEIGHT, 1.0))
                .add(EpicFightAttributes.STUN_ARMOR, 16.0).add(EpicFightAttributes.IMPACT, 3.0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 14));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        // Epic Fight installs his attack and chase goals for his fists (BanditWarlordPatch).
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(METEOR, false);
        builder.define(TRANSFORM, -1);
    }

    // ---------------------------------------------------------------- the fall

    /**
     * Sends him down from the sky like a meteor, from where he stands now: he dives, wrapped in fire, untouchable, and
     * lands with an impact; on a camp ({@code camp}: its centre, BlockPos.asLong, or null) its crater opens there.
     */
    public void fallFromSky(@Nullable Long camp) {
        this.camp = camp;
        this.home = camp;
        entityData.set(METEOR, true);
        setTarget(null);
        setDeltaMovement(0, -0.5, 0);
    }

    /** Whether he is falling from the sky now (seen by the clients). */
    public boolean isMeteor() {
        return entityData.get(METEOR);
    }

    /** Whether he is falling or landing: untouchable, still. */
    public boolean isArriving() {
        return isMeteor() || landing > 0 || transforming();
    }

    // ---------------------------------------------------------------- his transformation

    /** The camp he fell on (its centre, BlockPos.asLong), or null (an egg, /summon). */
    @Nullable public Long camp() {
        return home;
    }

    /** Whether he is becoming the Son of the Demon (untouchable, still, his bar gone). */
    public boolean transforming() {
        return entityData.get(TRANSFORM) >= 0;
    }

    /** Ticks into his transformation, -1 before. */
    public int transformTicks() {
        return entityData.get(TRANSFORM);
    }

    public void setTransformTicks(int ticks) {
        entityData.set(TRANSFORM, ticks);
    }

    /** The film begins: no foe, no blow, no bar; the Son of the Demon takes his place at its flash. */
    public void beginTransformation() {
        entityData.set(TRANSFORM, 0);
        // Still while the film plays: no looking about, no step; only the film moves him.
        setNoAi(true);
        setHealth(1);
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        bossBar.setVisible(false);
        fissureAt = -1;
        airborne = -1;
    }

    /**
     * A blow that would kill him begins his transformation instead (WarlordTransformation): his death is the Son of the
     * Demon's. Only /kill and the void (what pierces invulnerability) end him here.
     */
    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel server && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (!transforming()) com.murimblock.world.structure.site.WarlordTransformation.begin(server, this);
            setHealth(1);
            return;
        }
        super.die(source);
    }

    @Override public boolean isInvulnerableTo(DamageSource source) {
        return isArriving() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    /** No target while he arrives: no blow before he has risen. */
    @Override public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(isArriving() ? null : target);
    }

    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override public boolean fireImmune() {
        return true;
    }

    @Override public void tick() {
        super.tick();
        // While the film plays his body faces where he faces, on every side (the film frames him by it).
        if (transforming()) { yBodyRot = yBodyRotO = getYRot(); yHeadRot = yHeadRotO = getYRot(); }
        if (!(level() instanceof ServerLevel server)) return;
        if (isMeteor()) meteorTick(server);
        else if (landing > 0) landing--;
        if (!isArriving()) steps(server);
    }

    private void meteorTick(ServerLevel server) {
        Vec3 v = getDeltaMovement();
        setDeltaMovement(0, Math.max(-FALL_SPEED, Math.min(v.y, 0) - 0.08), 0);
        fallDistance = 0;
        // The fire round him and the trail of flame and smoke above.
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 24, 0.9, 1.2, 0.9, 0.04);
        server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.5, getZ(), 4, 0.6, 1.0, 0.6, 0.0);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 4.5, getZ(), 12, 0.7, 2.0, 0.7, 0.02);
        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 6, getZ(), 3, 0.4, 1.0, 0.4, 0.02);
        if (tickCount % 6 == 0) server.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.5F);
        if (onGround() || isInWater()) land(server);
    }

    /**
     * The impact: a blast of fire and dust, the ground shaking; everyone near is thrown back (not hurt: the entrance is
     * the camp's, its crater the blow); he lands fist first, rises and roars (warlord_land), untouchable meanwhile.
     */
    public void land(ServerLevel server) {
        entityData.set(METEOR, false);
        landing = Math.round(BanditWarlordAnimations.LAND_LENGTH * 20);
        fallDistance = 0;
        setDeltaMovement(Vec3.ZERO);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(BanditWarlordAnimations.LAND, 0.0F);
        BlockPos below = blockPosition().below();
        BlockState ground = server.getBlockState(below);
        if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.2, getZ(), 200, 4.0, 0.3, 4.0, 0.3);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 2, 1.5, 0.2, 1.5, 0.0);
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 160, 5.0, 0.2, 5.0, 0.12);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 80, 6.0, 1.0, 6.0, 0.05);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 120, 8.0, 0.2, 8.0, 0.08);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6.0F, 0.5F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 6.0F, 0.6F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.5F);
        for (LivingEntity near : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(IMPACT_REACH), e -> e != this && e.isAlive())) {
            Vec3 away = near.position().subtract(position()).multiply(1, 0, 1);
            double d = away.length();
            if (d > IMPACT_REACH) continue;
            Vec3 push = d < 1e-3 ? Vec3.directionFromRotation(0, getYRot()) : away.scale(1 / d);
            double k = 1.6 * (1 - d / IMPACT_REACH) + 0.4;
            near.push(push.x * k, 0.5 + 0.4 * k, push.z * k);
            near.hurtMarked = true;
        }
        if (camp != null) WarlordCrater.open(server, BlockPos.of(camp), getBlockY() - 1);
        camp = null;
    }

    // ---------------------------------------------------------------- his blows

    @Override public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server && !isArriving()) blowsTick(server);
    }

    private void blowsTick(ServerLevel server) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        var playing = main == null || main.isEnd() ? null : main.getRealAnimation();
        if (playing != BanditWarlordAnimations.EARTH_BREAKER) shockwaveSent = false;
        else if (!shockwaveSent && main.getElapsedTime() >= BanditWarlordAnimations.EARTH_IMPACT) {
            shockwaveSent = true;
            earthShockwave(server);
        }
        if (playing != BanditWarlordAnimations.COLLAPSE) collapseSent = false;
        else if (!collapseSent && main.getElapsedTime() >= BanditWarlordAnimations.COLLAPSE_IMPACT) {
            collapseSent = true;
            startFissure(server);
        }
        if (fissureAt >= 0) fissureStep(server);
        if (playing != BanditWarlordAnimations.LEAP) leapt = airborne >= 0 && leapt;
        else if (!leapt && main.getElapsedTime() >= BanditWarlordAnimations.LEAP_JUMP) {
            leapt = true;
            leap();
        }
        if (airborne >= 0) airTick(server);
        if (playing != BanditWarlordAnimations.SKY_BREAKER) launched = false;
        else if (!launched && main.getElapsedTime() >= BanditWarlordAnimations.SKY_IMPACT + 0.1F) {
            launched = true;
            skyLaunch();
        }
    }

    /**
     * The Earth Breaker's fists meet the ground: everyone round him (not the bandits) takes a share of his attack and is
     * thrown away and up. It is a blow from him: a guard raised in time parries it; who the fists themselves just struck,
     * or who just parried them, is spared.
     */
    public void earthShockwave(ServerLevel server) {
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 at = position().add(look.scale(2.0));
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * SHOCKWAVE_DAMAGE;
        AABB area = new AABB(at, at).inflate(SHOCKWAVE_RADIUS, 2.0, SHOCKWAVE_RADIUS);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive()
                && !(e instanceof MountainBanditEntity) && !(e instanceof BanditChiefEntity) && e.distanceToSqr(at) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS)) {
            boolean struck = victim.getLastHurtByMob() == this && victim.tickCount - victim.getLastHurtByMobTimestamp() <= BLOW_WINDOW;
            if (struck || MurimGuardSkill.parriedWithin(victim, BLOW_WINDOW)) continue;
            if (victim.hurt(damageSources().mobAttack(this), damage)) {
                Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
                Vec3 push = away.lengthSqr() < 1e-4 ? look : away.normalize();
                victim.push(push.x * 0.9, 0.8, push.z * 0.9);
                victim.hurtMarked = true;
            }
        }
        BlockState ground = server.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 120, 2.6, 0.1, 2.6, 0.2);
        server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 4, 1.4, 0.1, 1.4, 0.0);
        server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.1, at.z, 40, 3.0, 0.05, 3.0, 0.05);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.8F);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    // ---------------------------------------------------------------- the Mountain Collapse

    /** The fist meets the ground: the fissure starts two blocks before him and runs toward where he faces. */
    public void startFissure(ServerLevel server) {
        fissureDir = Vec3.directionFromRotation(0, getYRot()).multiply(1, 0, 1).normalize();
        fissureFrom = position().add(fissureDir.scale(2.0));
        fissureAt = 0;
        fissureStruck.clear();
        server.playSound(null, fissureFrom.x, fissureFrom.y, fissureFrom.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.6F);
    }

    /**
     * One block more of the fissure: the ground there and either side split open two deep (and whatever stands on it
     * blown away), dust and stones thrown up; whoever stands on it takes a share of his attack and is thrown up, once.
     */
    public void fissureStep(ServerLevel server) {
        if (fissureFrom == null || fissureDir == null || fissureAt >= FISSURE_LENGTH) { fissureAt = -1; return; }
        Vec3 at = fissureFrom.add(fissureDir.scale(fissureAt));
        Vec3 across = new Vec3(-fissureDir.z, 0, fissureDir.x);
        int ground = surface(server, BlockPos.containing(at.x, getY(), at.z));
        for (int w = -FISSURE_HALF_WIDTH; w <= FISSURE_HALF_WIDTH; w++) {
            Vec3 p = at.add(across.scale(w));
            for (int y = ground + 2; y >= ground - 1; y--) com.murimblock.combat.ShatteredGround.smash(server, BlockPos.containing(p.x, y, p.z));
        }
        BlockState dust = server.getBlockState(BlockPos.containing(at.x, ground - 2, at.z));
        if (!dust.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dust), at.x, ground + 0.5, at.z, 30, 0.8, 0.4, 0.8, 0.35);
        if (fissureAt % 3 == 0) {
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, ground + 0.6, at.z, 1, 0.2, 0.1, 0.2, 0.0);
            server.playSound(null, at.x, ground, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9F, 0.7F);
        }
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * FISSURE_DAMAGE;
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.7, 2.5, 1.7), e -> e != this && e.isAlive()
                && !(e instanceof MountainBanditEntity) && !(e instanceof BanditChiefEntity) && !fissureStruck.contains(e))) {
            fissureStruck.add(victim);
            if (MurimGuardSkill.parriedWithin(victim, BLOW_WINDOW)) continue;
            if (victim.hurt(damageSources().mobAttack(this), damage)) {
                victim.setDeltaMovement(victim.getDeltaMovement().x * 0.4 + fissureDir.x * 0.3, 0.95, victim.getDeltaMovement().z * 0.4 + fissureDir.z * 0.3);
                victim.hurtMarked = true;
            }
        }
        fissureAt++;
    }

    /** The top of the ground at this column, near this height (the block a body would stand on). */
    private static int surface(ServerLevel server, BlockPos near) {
        for (int y = near.getY() + 3; y >= near.getY() - 5; y--) {
            BlockPos p = new BlockPos(near.getX(), y, near.getZ());
            if (!server.getBlockState(p).getCollisionShape(server, p).isEmpty()) return y;
        }
        return near.getY() - 1;
    }

    // ---------------------------------------------------------------- the Thousand-Jin Drop

    /** Up and toward his foe (or eight blocks ahead): high enough to come down on it in some thirty ticks. */
    public void leap() {
        LivingEntity target = getTarget();
        Vec3 to = target != null ? target.position() : position().add(Vec3.directionFromRotation(0, getYRot()).scale(8));
        Vec3 flat = to.subtract(position()).multiply(1, 0, 1);
        double flight = 2 * LEAP_UP / 0.08 * 0.9;
        leapAlong = flat.scale(1 / flight);
        if (leapAlong.length() > 0.9) leapAlong = leapAlong.normalize().scale(0.9);
        setDeltaMovement(leapAlong.x, LEAP_UP, leapAlong.z);
        hurtMarked = true;
        airborne = 0;
        getNavigation().stop();
        if (level() instanceof ServerLevel server) {
            BlockState ground = server.getBlockState(blockPosition().below());
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.1, getZ(), 40, 1.0, 0.1, 1.0, 0.2);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
    }

    private void airTick(ServerLevel server) {
        airborne++;
        fallDistance = 0;
        getNavigation().stop();
        Vec3 v = getDeltaMovement();
        setDeltaMovement(leapAlong.x, v.y, leapAlong.z);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 2, 0.4, 0.4, 0.4, 0.01);
        if (airborne > 4 && (onGround() || isInWater()) || airborne > 120) leapLand(server);
    }

    /**
     * He comes down fist first: a crater round him (ShatteredGround), and a shockwave that hurts everyone round him (not
     * the bandits) and throws them away and up; a guard raised in time parries it.
     */
    public void leapLand(ServerLevel server) {
        airborne = -1;
        leapt = false;
        setDeltaMovement(Vec3.ZERO);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(BanditWarlordAnimations.LEAP_LAND, 0.0F);
        BlockPos ground = blockPosition().below();
        BlockState state = server.getBlockState(ground);
        com.murimblock.combat.ShatteredGround.crater(server, ground, LEAP_CRATER, 3);
        if (!state.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX(), getY() + 0.2, getZ(), 160, 3.0, 0.3, 3.0, 0.35);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 80, 5.0, 0.2, 5.0, 0.08);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.6F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4F, 0.5F);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * LEAP_DAMAGE;
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(LEAP_REACH, 3, LEAP_REACH), e -> e != this && e.isAlive()
                && !(e instanceof MountainBanditEntity) && !(e instanceof BanditChiefEntity) && e.distanceToSqr(this) <= LEAP_REACH * LEAP_REACH)) {
            if (MurimGuardSkill.parriedWithin(victim, BLOW_WINDOW)) continue;
            float share = (float) (1 - 0.5 * Math.sqrt(victim.distanceToSqr(this)) / LEAP_REACH);
            if (victim.hurt(damageSources().mobAttack(this), damage * share)) {
                Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
                Vec3 push = away.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, getYRot()) : away.normalize();
                victim.push(push.x * 1.1 * share, 0.7 * share + 0.2, push.z * 1.1 * share);
                victim.hurtMarked = true;
            }
        }
    }

    /** The Sky Breaker's fist came through: whoever it has just struck (not who parried it) is thrown high. */
    public void skyLaunch() {
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5), e -> e != this && e.isAlive()
                && e.getLastHurtByMob() == this && e.tickCount - e.getLastHurtByMobTimestamp() <= BLOW_WINDOW)) {
            victim.setDeltaMovement(victim.getDeltaMovement().x * 0.3, SKY_LAUNCH, victim.getDeltaMovement().z * 0.3);
            victim.hurtMarked = true;
        }
    }

    /** His heavy steps: heard and felt, two in his walk's 1.2 s. */
    private void steps(ServerLevel server) {
        boolean walking = onGround() && getDeltaMovement().horizontalDistanceSqr() > 4.0E-4;
        if (!walking) stepIn = 4;
        else if (--stepIn <= 0) {
            stepIn = 12;
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.3F, 0.6F);
            BlockState ground = server.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.05, getZ(), 12, 0.6, 0.02, 0.6, 0.05);
        }
    }

    // ---------------------------------------------------------------- the boss bar, his rage

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

    public boolean isEnraged() {
        return enraged;
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        setAggressive(getTarget() != null);
        if (!enraged && getHealth() < getMaxHealth() / 2 && level() instanceof ServerLevel server) enrage(server);
    }

    /** Under half his health, once: he roars and grows quicker. */
    public void enrage(ServerLevel server) {
        enraged = true;
        quicken();
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(BanditWarlordAnimations.ROAR, 0.0F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    private void quicken() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(ENRAGED_SPEED))
            speed.addPermanentModifier(new AttributeModifier(ENRAGED_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    // ---------------------------------------------------------------- saving

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (isMeteor()) tag.putBoolean("Meteor", true);
        if (camp != null) tag.putLong("Camp", camp);
        if (home != null) tag.putLong("Home", home);
        if (transforming()) tag.putBoolean("Transforming", true);
        if (landing > 0) tag.putInt("Landing", landing);
        if (enraged) tag.putBoolean("Enraged", true);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(METEOR, tag.getBoolean("Meteor"));
        camp = tag.contains("Camp") ? tag.getLong("Camp") : null;
        home = tag.contains("Home") ? tag.getLong("Home") : null;
        // A film cut off by a stop does not resume: he wakes, one blow from his transformation.
        if (tag.getBoolean("Transforming")) setNoAi(false);
        landing = tag.getInt("Landing");
        enraged = tag.getBoolean("Enraged");
        if (enraged) quicken();
    }

    // A big man's voice: the ravager's and the vindicator's, deep.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VINDICATOR_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.6F; }
}
