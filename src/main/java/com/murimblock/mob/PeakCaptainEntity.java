package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.integration.epicfight.PeakCaptainPatch;
import com.murimblock.item.MurimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.registry.entries.EpicFightAttributes;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The Captain of the Peak Guard (docs/mobs/peak-captain.md), the Orthodox Alliance's spear boss: in cloth
 * (scripts/animation/peak-captain-3d.cjs: a long azure coat, a white cape that swings, client/ClothLayer), wearing the
 * Peak Guard's armour and holding the Crimson Dragon Spear, both items of their own. Never spawned naturally (an egg,
 * /summon); in the world only on the leitai of the Alliance's arena (world/structure/site/AllianceArena), where he stands
 * guard at his post and goes back to it after a fight.
 * <p>
 * Neutral, as the Alliance's soldiers: he attacks no one first. Struck, he turns on whoever struck him and calls the
 * soldiers near him; he stays angry 30 to 50 seconds after the last blow. Strong (180 health, his armour's 20 points,
 * hard to stagger); every move can be parried. His health shows in a boss bar. He never loses his gear by the usual
 * drop: his loot table leaves the Celestial Peak Spear's manual and one piece of the armour every time, the spear now
 * and then.
 * <p>
 * Three phases ({@link Phase}):
 * <ol>
 *   <li>SPEAR: Epic Fight's spear moves (PeakCaptainPatch);</li>
 *   <li>RETREAT: at half his health he leaps back out of the fight to his post and calls the Peak Guard: two waves of
 *   spear guards drop from the sky around him (three, then four once the first are down), set on whoever he fights.
 *   Out of the fight, behind his men, blows glance off him. It lasts until both waves are down, a minute at most;</li>
 *   <li>ART: he comes back with the Celestial Peak Spear's techniques (docs/combat/styles/celestial-peak-spear.md).</li>
 * </ol>
 */
public class PeakCaptainEntity extends PathfinderMob implements NeutralMob {
    /** How long he stays angry after the last blow, and how often, angry, he calls the soldiers about him. */
    private static final UniformInt ANGER_TIME = TimeUtil.rangeOfSeconds(30, 50), ALERT_INTERVAL = TimeUtil.rangeOfSeconds(4, 6);

    /** The fight's three phases. */
    public enum Phase { SPEAR, RETREAT, ART }

    /** The share of his health at which he retreats and calls his men. */
    public static final float RETREAT_AT = 0.5F;
    /** The two waves of spear guards, one after the other. */
    public static final int[] WAVES = {3, 4};
    /** Ticks: from the retreat to the first wave; the longest a wave is waited for; the retreat's longest. */
    public static final int FIRST_CALL = 20, WAVE_PATIENCE = 30 * 20, RETREAT_LONGEST = 60 * 20;
    /** How far around his post his men land (blocks), and the highest they drop from. */
    static final double DROP_RING = 6.0;
    static final int DROP_HEIGHT = 18;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private int angerTime, ticksUntilNextAlert;
    @Nullable private UUID angerTarget;
    /** Where he stands guard (his spawn point): he goes back there when he has no one to fight. */
    @Nullable private BlockPos post;
    private Phase phase = Phase.SPEAR;
    /** In the retreat: ticks since it began, the waves called so far, the men of the last one, and his foe. */
    private int retreatTicks, wavesCalled, waveTicks;
    private final java.util.List<UUID> waveMen = new java.util.ArrayList<>();
    @Nullable private UUID foe;

    public PeakCaptainEntity(EntityType<? extends PeakCaptainEntity> type, Level level) {
        super(type, level);
        xpReward = 50;
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createCaptainAttributes() {
        // No armour or attack of his own: the Peak Guard's armour gives 20 (toughness 8), the spear its 6.
        return EpicFightMobSupport.humanoidAttributes(PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.0))
                .add(EpicFightAttributes.STUN_ARMOR, 8.0).add(EpicFightAttributes.IMPACT, 1.8);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // He does not wander: he stands at his post, and goes back to it after a fight.
        goalSelector.addGoal(5, new ReturnToPostGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Only whoever strikes him (and whoever he is still angry at); Epic Fight installs his spear's attack and chase.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    /** His whole gear: the Peak Guard's armour and the Crimson Dragon Spear, none of it dropped the usual way. */
    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(MurimItems.PEAK_GUARD_HELM.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(MurimItems.PEAK_GUARD_LAMELLAR.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(MurimItems.PEAK_GUARD_TASSETS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(MurimItems.PEAK_GUARD_GREAVES.get()));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.CRIMSON_DRAGON_SPEAR.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        post = blockPosition();
        return result;
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

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        updatePersistentAnger((ServerLevel) level(), true);
        if (getTarget() != null) maybeAlertSoldiers();
        if (phase == Phase.RETREAT) retreatTick((ServerLevel) level());
        // With a target, he is aggressive: the clients see it (his face frowns, MobFaces).
        setAggressive(getTarget() != null);
    }

    // ---- the phases

    public Phase phase() {
        return phase;
    }

    /** His men of the wave now fighting, those still alive. */
    public java.util.List<AllianceSoldierEntity> waveMen() {
        if (!(level() instanceof ServerLevel server)) return java.util.List.of();
        return waveMen.stream().map(server::getEntity).filter(e -> e instanceof AllianceSoldierEntity s && s.isAlive())
                .map(e -> (AllianceSoldierEntity) e).toList();
    }

    public int wavesCalled() {
        return wavesCalled;
    }

    /** Out of the fight, behind his men, blows glance off him (all but those that ignore invulnerability). */
    @Override public boolean hurt(DamageSource source, float amount) {
        if (phase == Phase.RETREAT && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (!level().isClientSide) playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F);
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && phase == Phase.SPEAR && getHealth() <= getMaxHealth() * RETREAT_AT && level() instanceof ServerLevel server)
            retreat(server, source.getEntity() instanceof LivingEntity attacker ? attacker : getTarget());
        return hurt;
    }

    /** Phase 2: he leaps back out of the fight, toward his post, and calls his men. */
    public void retreat(ServerLevel server, @Nullable LivingEntity from) {
        phase = Phase.RETREAT;
        retreatTicks = 0;
        wavesCalled = 0;
        waveMen.clear();
        foe = from == null ? null : from.getUUID();
        withPatch(PeakCaptainPatch::holdBack);
        getNavigation().stop();
        // The leap: onto his post when he has one (it lands him there, on his leitai, never off its edge); else a bound
        // back from his foe.
        if (post != null) leapTo(Vec3.atBottomCenterOf(post));
        else {
            Vec3 away = from != null ? position().subtract(from.position()).multiply(1, 0, 1) : Vec3.ZERO;
            away = away.lengthSqr() < 1.0E-4 ? getLookAngle().reverse().multiply(1, 0, 1).normalize() : away.normalize();
            setDeltaMovement(away.x * 0.6, 0.5, away.z * 0.6);
            hurtMarked = true;
        }
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4.0F, 1.0F);
        for (ServerPlayer player : server.getPlayers(p -> p.distanceToSqr(this) < 48 * 48))
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("entity.murimblock.peak_captain.call"), true);
    }

    private void retreatTick(ServerLevel server) {
        retreatTicks++;
        waveTicks++;
        LivingEntity enemy = foe == null ? null : server.getEntity(foe) instanceof LivingEntity e && e.isAlive() ? e : null;
        if (enemy != null) getLookControl().setLookAt(enemy, 30, 30);
        boolean waveDown = waveMen().isEmpty() || waveTicks > WAVE_PATIENCE;
        if (wavesCalled == 0 && retreatTicks >= FIRST_CALL) callWave(server, WAVES[0], enemy);
        else if (wavesCalled > 0 && wavesCalled < WAVES.length && waveDown) callWave(server, WAVES[wavesCalled], enemy);
        else if (wavesCalled == WAVES.length && waveDown || retreatTicks > RETREAT_LONGEST) takeUpTheArt(server, enemy);
    }

    /** A wave of spear guards drops from the sky around his post, set on his foe. */
    private void callWave(ServerLevel server, int count, @Nullable LivingEntity enemy) {
        wavesCalled++;
        waveTicks = 0;
        waveMen.clear();
        if (enemy == null) enemy = server.getNearestPlayer(this, 24);
        Vec3 centre = post != null ? Vec3.atBottomCenterOf(post) : position();
        double start = getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double a = start + i * Math.PI * 2 / count;
            Vec3 at = dropPoint(server, centre.x + Math.cos(a) * DROP_RING, centre.y, centre.z + Math.sin(a) * DROP_RING);
            // No ground there (a drop, a wall): he lands beside the captain instead.
            if (at == null) at = dropPoint(server, getX() + Math.cos(a) * 1.5, getY(), getZ() + Math.sin(a) * 1.5);
            if (at == null) at = position().add(0, 1, 0);
            AllianceSoldierEntity man = MurimEntities.ALLIANCE_SPEAR_GUARD.get().create(server);
            if (man == null) continue;
            float yaw = enemy != null ? (float) Math.toDegrees(Math.atan2(-(enemy.getX() - at.x), enemy.getZ() - at.z)) : getYRot();
            man.moveTo(at.x, at.y, at.z, yaw, 0);
            man.finalizeSpawn(server, server.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            man.dropFromSky();
            if (enemy != null) {
                man.setTarget(enemy);
                man.setPersistentAngerTarget(enemy.getUUID());
                man.startPersistentAngerTimer();
            }
            server.addFreshEntity(man);
            waveMen.add(man.getUUID());
        }
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4.0F, 1.2F);
    }

    /**
     * Where a man dropping near (x, y, z) starts: above the ground there (searched a few blocks up and down from y), as
     * high as the open sky allows, DROP_HEIGHT at most; null when there is no firm ground with room to stand there.
     */
    @Nullable static Vec3 dropPoint(ServerLevel server, double x, double y, double z) {
        BlockPos.MutableBlockPos p = BlockPos.containing(x, y + 4, z).mutable();
        int lowest = p.getY() - 12;
        while (p.getY() > lowest && !(server.getBlockState(p.below()).isFaceSturdy(server, p.below(), net.minecraft.core.Direction.UP)
                && server.getBlockState(p).getCollisionShape(server, p).isEmpty() && server.getBlockState(p.above()).getCollisionShape(server, p.above()).isEmpty()))
            p.move(0, -1, 0);
        if (p.getY() <= lowest) return null;
        int ground = p.getY(), top = ground;
        while (top - ground < DROP_HEIGHT && server.getBlockState(p.setY(top + 1)).getCollisionShape(server, p).isEmpty()
                && server.getBlockState(p.setY(top + 2)).getCollisionShape(server, p).isEmpty()) top++;
        return new Vec3(Math.floor(x) + 0.5, top, Math.floor(z) + 0.5);
    }

    /** Phase 3: back into the fight with the Celestial Peak Spear's techniques. */
    public void takeUpTheArt(ServerLevel server, @Nullable LivingEntity enemy) {
        phase = Phase.ART;
        withPatch(PeakCaptainPatch::takeUpTheArt);
        if (enemy != null) setTarget(enemy);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    private void withPatch(java.util.function.Consumer<PeakCaptainPatch> action) {
        PeakCaptainPatch patch = yesman.epicfight.world.capabilities.EpicFightCapabilities.getEntityPatch(this, PeakCaptainPatch.class);
        if (patch != null) action.accept(patch);
    }

    // ---- anger, as the Alliance's soldiers

    /** Every few seconds while he sees his foe, the soldiers about him who are not fighting join in. */
    private void maybeAlertSoldiers() {
        if (ticksUntilNextAlert > 0) {
            ticksUntilNextAlert--;
            return;
        }
        LivingEntity foe = getTarget();
        if (foe != null && getSensing().hasLineOfSight(foe)) {
            double range = getAttributeValue(Attributes.FOLLOW_RANGE);
            level().getEntitiesOfClass(AllianceSoldierEntity.class, AABB.unitCubeFromLowerCorner(position()).inflate(range, 10.0, range), EntitySelector.NO_SPECTATORS)
                    .stream().filter(soldier -> soldier.getTarget() == null && !soldier.isAlliedTo(foe))
                    .forEach(soldier -> soldier.setTarget(foe));
        }
        ticksUntilNextAlert = ALERT_INTERVAL.sample(getRandom());
    }

    @Override public void setTarget(@Nullable LivingEntity target) {
        if (getTarget() == null && target != null) ticksUntilNextAlert = 0;
        if (target instanceof Player player) setLastHurtByPlayer(player);
        super.setTarget(target);
    }

    @Override public int getRemainingPersistentAngerTime() { return angerTime; }
    @Override public void setRemainingPersistentAngerTime(int time) { angerTime = time; }
    @Override @Nullable public UUID getPersistentAngerTarget() { return angerTarget; }
    @Override public void setPersistentAngerTarget(@Nullable UUID target) { angerTarget = target; }
    @Override public void startPersistentAngerTimer() { setRemainingPersistentAngerTime(ANGER_TIME.sample(getRandom())); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        addPersistentAngerSaveData(tag);
        if (post != null) tag.put("Post", NbtUtils.writeBlockPos(post));
        tag.putString("Phase", phase.name());
        tag.putInt("RetreatTicks", retreatTicks);
        tag.putInt("WavesCalled", wavesCalled);
        net.minecraft.nbt.ListTag men = new net.minecraft.nbt.ListTag();
        for (UUID man : waveMen) men.add(NbtUtils.createUUID(man));
        tag.put("WaveMen", men);
        if (foe != null) tag.putUUID("Foe", foe);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readPersistentAngerSaveData(level(), tag);
        post = NbtUtils.readBlockPos(tag, "Post").orElse(null);
        try {
            phase = tag.contains("Phase") ? Phase.valueOf(tag.getString("Phase")) : Phase.SPEAR;
        } catch (IllegalArgumentException e) {
            phase = Phase.SPEAR;
        }
        retreatTicks = tag.getInt("RetreatTicks");
        wavesCalled = tag.getInt("WavesCalled");
        waveMen.clear();
        for (var man : tag.getList("WaveMen", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) waveMen.add(NbtUtils.loadUUID(man));
        foe = tag.hasUUID("Foe") ? tag.getUUID("Foe") : null;
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }

    /** Where he stands guard, or null (never placed). */
    @Nullable public BlockPos post() {
        return post;
    }

    /** With no one to fight, he walks back to his post and faces the way he faced there. */
    /** Flight time of his leaps (ticks). */
    static final int LEAP_TICKS = 16;

    /**
     * A leap that lands him on {@code to}: up and across in LEAP_TICKS, by the game's own gravity (0.08 a tick) and the
     * drag of the air (0.98 up and down, 0.91 across). Far off, he leaps no further than 12 blocks.
     */
    public void leapTo(Vec3 to) {
        Vec3 across = to.subtract(position()).multiply(1, 0, 1);
        double far = Math.min(across.length(), 12), rise = to.y - getY();
        double spread = (1 - Math.pow(0.91, LEAP_TICKS)) / 0.09;
        double vy = Math.max(0.42, (rise + 0.04 * LEAP_TICKS * LEAP_TICKS) / LEAP_TICKS);
        Vec3 v = across.lengthSqr() < 1.0E-4 ? Vec3.ZERO : across.normalize().scale(far / spread);
        getNavigation().stop();
        setDeltaMovement(v.x, vy, v.z);
        hurtMarked = true;
        hasImpulse = true;
    }

    static final class ReturnToPostGoal extends Goal {
        private final PeakCaptainEntity captain;
        /** Ticks he has made no headway toward his post: he leaps onto it when walking will not do (a raised leitai). */
        private int stuck;
        private double best;

        ReturnToPostGoal(PeakCaptainEntity captain) {
            this.captain = captain;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() {
            return captain.post != null && (captain.getTarget() == null || captain.phase == Phase.RETREAT)
                    && captain.post.distToCenterSqr(captain.position()) > 2.5 * 2.5;
        }

        @Override public boolean canContinueToUse() {
            return canUse();
        }

        @Override public boolean requiresUpdateEveryTick() {
            return true;
        }


        @Override public void start() {
            stuck = 0;
            best = Double.MAX_VALUE;
            walk();
        }

        private void walk() {
            captain.getNavigation().moveTo(captain.post.getX() + 0.5, captain.post.getY(), captain.post.getZ() + 0.5, 0.8);
        }

        @Override public void tick() {
            double now = captain.post.distToCenterSqr(captain.position());
            if (now < best - 0.25) {
                best = now;
                stuck = 0;
            } else stuck++;
            if (captain.getNavigation().isDone() && stuck % 10 == 0) walk();
            // Two seconds without getting nearer (no way up, a wall): one leap, from the ground, onto the post.
            if (stuck >= 40 && captain.onGround()) {
                captain.leapTo(Vec3.atBottomCenterOf(captain.post));
                stuck = 0;
                best = Double.MAX_VALUE;
            }
        }
    }

    // A commander's voice: the vindicator's, a little deeper.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VINDICATOR_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VINDICATOR_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VINDICATOR_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.85F; }
}
