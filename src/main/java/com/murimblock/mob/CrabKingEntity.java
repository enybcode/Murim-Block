package com.murimblock.mob;

import com.murimblock.integration.epicfight.CrabKingAnimations;
import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

/**
 * The Crab King (蟹王), the boss of the crab art (docs/mobs/crab-king.md): a martial artist the crab men's sickness took
 * whole, drawn twice a man's size, four blocks tall (client/CrabKingRenderer). Not spawned in the world: he waits in a
 * structure (a spawn egg, /summon, until it is built), and never despawns. Tough (260 health, armour 14, his shell barely
 * moved by blows) and slow, walking heavily (his steps are heard), his pincers his weapons: a sweep, a lunging snap and a
 * slam whose landing sends a shockwave (CrabKingPatch), every one parryable; and the grab: what the great pincer catches
 * is held between the jaws (CrabKingGrip), lifted before his face, squeezed three times, and thrown far: it lands
 * knocked down. A guard does not stop the grab; a perfect parry as the pincer shuts makes it miss. A player he holds or
 * has thrown can do nothing until it lands ({@link #isHeld}). Where it lands, an impact like an explosion; thrown into
 * a wall, it leaves a crater in it (Enzo, 16/10; not where mobs may not grief). Meanwhile: no attack, guard, roll, item, technique or meditation, no
 * moving. The pincers from the deep: he plunges both pincers into the ground and holds them there while two giant ones
 * (CrabPincerEntity), some seven blocks tall, burst up before his target, one on either side of it, the ground shaking
 * (client/CrabPincerShake), each sweeping all round, striking to its side and trying to pinch it. The mist: he breathes
 * a thick mist round him (CrabMistEntity) and is gone into it, unseen, until he strikes, is struck or it thins. The victim plays its own animations (crab_king_victim, crab_king_thrown), and a player sees it as a film
 * (client/GrabCinematic). Under half his health he roars and calls two armoured crabs out of the ground,
 * once. He breathes under water and moves faster in it. His health shows in a boss bar to the players near him. The crab
 * art's manual will be his drop when the art is made.
 */
public class CrabKingEntity extends Monster {
    /** The slam's shockwave: its reach around the landing point, and its share of his attack. */
    public static final double SHOCKWAVE_RADIUS = 4.6;
    public static final float SHOCKWAVE_DAMAGE = 0.6F;
    /** Ticks before the shockwave in which the slam's pincers may have struck or been parried. */
    private static final int SLAM_WINDOW = 12;
    /** How many armoured crabs he calls, under half his health. */
    public static final int CRABS_CALLED = 2;
    /** The grab: its catch, each squeeze and the slam to the ground, as shares of his attack; the squeezes' ticks into the hold. */
    public static final float GRAB_CATCH_DAMAGE = 0.3F, GRAB_SQUEEZE_DAMAGE = 0.25F, GRAB_THROW_DAMAGE = 0.5F, GRAB_LANDING_DAMAGE = 0.4F;
    /** The squeezes, seconds into the hold (crab-king-moves.cjs SQUEEZES). */
    public static final float[] SQUEEZES = {0.85F, 1.25F, 1.6F};
    /** How far around the jaws a body is caught (blocks). */
    private static final double CATCH_REACH = 1.4;
    /** The throw: how fast ahead and up the victim leaves the pincer (blocks a tick): it lands some eight blocks away. */
    private static final double THROW_AHEAD = 1.35, THROW_UP = 0.62;
    /** The crater a body thrown into a wall leaves: its radius (blocks). */
    public static final double CRATER_RADIUS = 1.7;

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean shockwaveSent;
    /** Whether he has roared and called his crabs (saved: once in his life). */
    private boolean enraged;
    /** Whether the grab now playing has shut its pincer yet; who the pincer holds (null: no one), for how long, slammed yet. */
    private boolean grabShut, slammedDown;
    @Nullable private LivingEntity held;
    /** Who he has thrown and is still flying, and for how many ticks. */
    private final java.util.Map<LivingEntity, Integer> thrown = new java.util.HashMap<>();
    /** Which squeezes have been dealt in the hold now playing. */
    private int squeezesDealt;
    /** The players a Crab King holds now, and which: server only. */
    private static final java.util.Map<java.util.UUID, java.lang.ref.WeakReference<CrabKingEntity>> HOLDERS = new java.util.concurrent.ConcurrentHashMap<>();
    private int holdTicks;
    /**
     * The pincers from the deep: how far before their target (toward him) they stand, either side of it; where they come
     * with no target, before him (blocks); the second how many ticks after the first.
     */
    public static final double PINCER_BEFORE = 2.0, PINCER_AHEAD = 8.0;
    public static final int PINCER_EVERY = 6;
    /** The pincers still to come: when (his tick count) and where. */
    private final java.util.List<Object[]> pincersToCome = new java.util.ArrayList<>();
    private boolean burrowed;
    /** The mist: whether the one now playing has burst out, whether he is gone into it, the mist he is hidden in. */
    private boolean misted, hidden;
    @Nullable private CrabMistEntity mist;
    /** Ticks to his next heavy step while he walks. */
    private int stepIn;

    public CrabKingEntity(EntityType<? extends CrabKingEntity> type, Level level) {
        super(type, level);
        xpReward = 60;
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createCrabKingAttributes() {
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 260).add(Attributes.ARMOR, 14).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.ATTACK_DAMAGE, 11).add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.33))
                .add(EpicFightAttributes.STUN_ARMOR, 14.0).add(EpicFightAttributes.IMPACT, 2.8);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(ArmoredCrabEntity.class, CrabManEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Epic Fight installs his attack and chase goals for his empty hands (CrabKingPatch).
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, type, data);
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ---- a crab: he breathes under water (the entity type tag minecraft:can_breathe_under_water), and is quicker in it

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (isInWater() && tickCount % 20 == 0) addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        // His heavy steps: two in his walk's 1.6 s, heard and felt.
        boolean walking = onGround() && getDeltaMovement().horizontalDistanceSqr() > 4.0E-4;
        if (!walking) stepIn = 4;
        else if (--stepIn <= 0) {
            stepIn = 16;
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.3F, 0.55F);
            BlockState ground = server.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), getX(), getY() + 0.05, getZ(), 12, 0.6, 0.02, 0.6, 0.05);
        }
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

    public boolean isEnraged() {
        return enraged;
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        setAggressive(getTarget() != null);
        if (!enraged && getHealth() < getMaxHealth() / 2 && level() instanceof ServerLevel server) enrage(server);
    }

    /** Under half his health, once: he roars and two armoured crabs come out of the ground around him. */
    public void enrage(ServerLevel server) {
        enraged = true;
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(CrabKingAnimations.ROAR, 0.0F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 0.6F);
        for (int i = 0; i < CRABS_CALLED; i++) {
            double a = getYRot() * Math.PI / 180 + (i == 0 ? 1.6 : -1.6);
            Vec3 at = position().add(-Math.sin(a) * 2.5, 0, Math.cos(a) * 2.5);
            ArmoredCrabEntity crab = MurimEntities.ARMORED_CRAB.get().create(server);
            if (crab == null) continue;
            crab.moveTo(at.x, getY(), at.z, getYRot(), 0);
            crab.finalizeSpawn(server, server.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            if (getTarget() != null) crab.setTarget(getTarget());
            server.addFreshEntity(crab);
            BlockState ground = server.getBlockState(BlockPos.containing(at.x, getY() - 0.5, at.z));
            if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, getY() + 0.1, at.z, 30, 0.5, 0.1, 0.5, 0.1);
        }
    }

    // ---- the slam's shockwave

    @Override public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            shockwaveTick(server);
            grabTick(server);
            burrowTick(server);
            mistTick(server);
        }
    }

    // ---- the mist

    private void mistTick(ServerLevel server) {
        AnimationPlayer main = mainPlayer();
        var playing = main == null ? null : main.getRealAnimation();
        boolean breathing = playing == CrabKingAnimations.MIST && !main.isEnd();
        if (!breathing) misted = false;
        else {
            float t = main.getElapsedTime();
            if (!misted && t >= CrabKingAnimations.MIST_RELEASE) {
                misted = true;
                breatheMist(server);
            }
            if (!hidden && misted && t >= CrabKingAnimations.MIST_GONE) hide(server);
        }
        // Gone into it until he strikes (an attack of his begins), or it thins out.
        if (hidden) {
            boolean striking = playing != null && playing != CrabKingAnimations.MIST && main != null && !main.isEnd()
                    && (playing.get() instanceof yesman.epicfight.api.animation.types.AttackAnimation
                        || playing == CrabKingAnimations.GRAB || playing == CrabKingAnimations.BURROW);
            if (striking || mist == null || mist.isRemoved() || mist.tickCount > CrabMistEntity.LIFETIME - CrabMistEntity.FADE) reveal(server);
        }
    }

    /** The mist bursts out of him: a thick cloud round him (CrabMistEntity), with a great sigh. */
    public void breatheMist(ServerLevel server) {
        if (mist != null && !mist.isRemoved()) mist.discard();
        mist = new CrabMistEntity(MurimEntities.CRAB_MIST.get(), server);
        mist.moveTo(getX(), getY(), getZ(), 0, 0);
        server.addFreshEntity(mist);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2, getZ(), 80, 2.5, 1.5, 2.5, 0.12);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE, 1.4F, 0.5F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.4F, 0.4F);
    }

    /** Gone into the mist: unseen (as any invisible thing: no model, no shadow, no name). His steps are still heard. */
    public void hide(ServerLevel server) {
        hidden = true;
        setInvisible(true);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2, getZ(), 40, 1.2, 1.5, 1.2, 0.05);
    }

    /** Out of the mist: seen again, in a burst of it and spray. */
    public void reveal(ServerLevel server) {
        if (!hidden) return;
        hidden = false;
        setInvisible(false);
        server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2, getZ(), 50, 1.2, 1.6, 1.2, 0.15);
        server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 2, getZ(), 40, 1.0, 1.4, 1.0, 0.2);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.TURTLE_AMBIENT_LAND, SoundSource.HOSTILE, 1.6F, 0.4F);
    }

    /** Whether he is gone into his mist now. */
    public boolean isHidden() {
        return hidden;
    }

    /** Struck in the mist, he is seen again. */
    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && hidden && level() instanceof ServerLevel server) reveal(server);
        return hurt;
    }

    // ---- pincers from the deep

    private void burrowTick(ServerLevel server) {
        AnimationPlayer main = mainPlayer();
        boolean burrowing = main != null && main.getRealAnimation() == CrabKingAnimations.BURROW && !main.isEnd();
        if (!burrowing) burrowed = false;
        else if (!burrowed && main.getElapsedTime() >= CrabKingAnimations.BURROW_PLUNGE) {
            burrowed = true;
            callPincers(server, getTarget());
        }
        for (var it = pincersToCome.iterator(); it.hasNext(); ) {
            Object[] p = it.next();
            if (tickCount < (int) p[0]) continue;
            it.remove();
            CrabPincerEntity.call(server, this, (Vec3) p[1], (Vec3) p[2]);
        }
    }

    /**
     * The pincers plunged in (Enzo, 16/10: "two, before the player"): two giant ones out of the ground before `target`,
     * one on its left and one on its right, each where its pinch reaches it (CrabPincerEntity.REACH); the second a little
     * after the first. With no target, before him. Each on the ground there.
     */
    public void callPincers(ServerLevel server, @Nullable LivingEntity target) {
        Vec3 line = target != null ? target.position().subtract(position()).multiply(1, 0, 1) : ahead();
        line = line.lengthSqr() < 1e-4 ? ahead() : line.normalize();
        Vec3 aimAt = target != null ? target.position() : position().add(line.scale(PINCER_AHEAD));
        Vec3 across = new Vec3(-line.z, 0, line.x);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 1.4F, 0.6F);
        double reach = CrabPincerEntity.REACH - 0.8, side = Math.sqrt(Math.max(0, reach * reach - PINCER_BEFORE * PINCER_BEFORE));
        for (int n = 0; n < 2; n++) {
            Vec3 at = ground(server, aimAt.subtract(line.scale(PINCER_BEFORE)).add(across.scale(n == 0 ? side : -side)));
            if (at != null) pincersToCome.add(new Object[] {tickCount + n * PINCER_EVERY, at, aimAt});
        }
    }

    /** The ground at a point: the top of the first solid block a few blocks above or below his feet (null: none). */
    @Nullable private Vec3 ground(ServerLevel server, Vec3 at) {
        BlockPos.MutableBlockPos p = BlockPos.containing(at.x, getY() + 3, at.z).mutable();
        for (int i = 0; i < 9; i++, p.move(0, -1, 0)) {
            if (server.getBlockState(p).isAir() && !server.getBlockState(p.below()).getCollisionShape(server, p.below()).isEmpty())
                return new Vec3(at.x, p.getY(), at.z);
        }
        return null;
    }

    /** The pincers still to come from his last burrow. */
    public int pincersToCome() {
        return pincersToCome.size();
    }

    // ---- the grab

    private AnimationPlayer mainPlayer() {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        return patch == null ? null : patch.getAnimator().getPlayerFor(null);
    }

    /**
     * Whether a Crab King holds this player now: it can do nothing (KnockdownService.isHindered counts it). Checked
     * against the king himself, so a king killed, unloaded or broken off never leaves anyone stuck.
     */
    public static boolean isHeld(Player player) {
        var ref = HOLDERS.get(player.getUUID());
        CrabKingEntity king = ref == null ? null : ref.get();
        if (king != null && king.isAlive() && !king.isRemoved() && (king.held == player || king.thrown.containsKey(player))) return true;
        if (ref != null) HOLDERS.remove(player.getUUID());
        return false;
    }

    /** A humanoid's patch (a player, a biped mob): it can play the victim's animations. */
    @Nullable private static LivingEntityPatch<?> humanoid(LivingEntity victim) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(victim, LivingEntityPatch.class);
        // Each patch has its own copy of the biped skeleton: any humanoid one plays them.
        return patch != null && patch.getArmature() instanceof yesman.epicfight.model.armature.HumanoidArmature ? patch : null;
    }

    /** The catch pinned: off any mount, its own animation on the hold's clock (a humanoid), counted held. */
    private void pin(LivingEntity victim) {
        victim.stopRiding();
        if (victim instanceof Player player) HOLDERS.put(player.getUUID(), new java.lang.ref.WeakReference<>(this));
        LivingEntityPatch<?> patch = humanoid(victim);
        if (patch != null) patch.playAnimationSynchronized(CrabKingAnimations.VICTIM, 0.0F);
    }

    /** The pincer lets go; broken off (not the throw), the catch is freed with a short stun that ends its pinned pose. */
    private void letGo(boolean brokenOff) {
        LivingEntity victim = held;
        held = null;
        if (victim == null) return;
        if (brokenOff) {
            if (victim instanceof Player player) HOLDERS.remove(player.getUUID());
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(victim, LivingEntityPatch.class);
            if (patch != null && victim.isAlive()) patch.applyStun(StunType.SHORT, 0.3F);
        }
    }

    @Override public void remove(RemovalReason reason) {
        letGo(true);
        for (LivingEntity v : thrown.keySet()) if (v instanceof Player p) HOLDERS.remove(p.getUUID());
        thrown.clear();
        super.remove(reason);
    }

    @Override public void die(DamageSource source) {
        letGo(true);
        super.die(source);
    }

    /** Who the great pincer holds now (null: no one). */
    @Nullable public LivingEntity held() {
        return held;
    }

    private void grabTick(ServerLevel server) {
        flightTick(server);
        AnimationPlayer main = mainPlayer();
        var playing = main == null ? null : main.getRealAnimation();
        if (playing == CrabKingAnimations.GRAB && !main.isEnd()) {
            if (!grabShut && main.getElapsedTime() >= CrabKingAnimations.GRAB_CATCH) {
                grabShut = true;
                // Caught: the hold starts playing from the next tick on.
                if (catchBefore(server)) return;
            }
        } else grabShut = false;
        if (held == null) return;
        // Let go when the hold is over or broken off (the hold may take a tick to replace the grab).
        boolean holding = playing == CrabKingAnimations.GRAB_HOLD && !main.isEnd() || holdTicks < 2 && playing == CrabKingAnimations.GRAB;
        if (!holding || !held.isAlive() || !isAlive() || held.level() != level()) {
            letGo(true);
            return;
        }
        holdTicks++;
        float t = playing == CrabKingAnimations.GRAB_HOLD ? main.getElapsedTime() : 0;
        if (t >= CrabKingAnimations.GRAB_RELEASE) {
            if (!slammedDown) throwAway(server);
            return;
        }
        holdAt(t);
        if (squeezesDealt < SQUEEZES.length && t >= SQUEEZES[squeezesDealt]) {
            squeezesDealt++;
            held.invulnerableTime = 0;
            held.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * GRAB_SQUEEZE_DAMAGE);
            server.playSound(null, held.getX(), held.getY(), held.getZ(), SoundEvents.TURTLE_EGG_CRACK, SoundSource.HOSTILE, 1.4F, 0.5F);
            server.playSound(null, held.getX(), held.getY(), held.getZ(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    /** Where the jaws are now, from CrabKingGrip's point (right, front, up from his feet): in the world. */
    private Vec3 grip(float[] g) {
        return position().add(right().scale(g[0])).add(ahead().scale(g[1])).add(0, g[2], 0);
    }

    private Vec3 ahead() {
        return Vec3.directionFromRotation(0, yBodyRot);
    }

    /** His right, square to where he faces. */
    private Vec3 right() {
        Vec3 f = ahead();
        return new Vec3(-f.z, 0, f.x);
    }

    /**
     * The pincer shuts: the nearest body before him (not his crabs) is caught, hurt a little, and held. A guard does not
     * stop it; a perfect parry just then does.
     */
    /** Where the jaws shut on a body, before him (CrabKingGrip.CATCH): the middle of what they catch. */
    public Vec3 catchPoint() {
        return grip(com.murimblock.integration.epicfight.CrabKingGrip.CATCH);
    }

    public boolean catchBefore(ServerLevel server) {
        Vec3 at = catchPoint();
        LivingEntity caught = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(CATCH_REACH, at.y - getY(), CATCH_REACH),
                e -> e != this && e.isAlive() && !e.isSpectator() && !(e instanceof ArmoredCrabEntity) && !(e instanceof CrabManEntity)
                        && !(e instanceof Player p && p.getAbilities().invulnerable))) {
            double d = e.distanceToSqr(at);
            if (d < nearest) { nearest = d; caught = e; }
        }
        server.playSound(null, at.x, at.y, at.z, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.HOSTILE, 1.4F, 0.5F);
        if (caught == null) return false;
        caught.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * GRAB_CATCH_DAMAGE);
        if (MurimGuardSkill.parriedThisTick(caught) || !caught.isAlive()) return false;
        held = caught;
        holdTicks = 0;
        squeezesDealt = 0;
        slammedDown = false;
        pin(caught);
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(CrabKingAnimations.GRAB_HOLD, 0.0F);
        return true;
    }

    /**
     * The catch held between the jaws at t seconds into the hold: its middle on the grip (CrabKingGrip), facing him.
     */
    private void holdAt(float t) {
        Vec3 at = grip(com.murimblock.integration.epicfight.CrabKingGrip.at(t)).add(0, -held.getBbHeight() / 2, 0);
        float facing = yBodyRot + 180;
        if (held instanceof ServerPlayer player) player.connection.teleport(at.x, at.y, at.z, facing, player.getXRot());
        else {
            held.teleportTo(at.x, at.y, at.z);
            held.setYRot(facing);
            held.setYBodyRot(facing);
            held.setYHeadRot(facing);
        }
        held.setDeltaMovement(Vec3.ZERO);
        held.fallDistance = 0;
    }

    /** The throw: the pincer opens at the end of its fling, the catch flies ahead and up, flailing, until it lands. */
    private void throwAway(ServerLevel server) {
        slammedDown = true;
        LivingEntity victim = held;
        letGo(false);
        victim.invulnerableTime = 0;
        victim.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * GRAB_THROW_DAMAGE);
        if (!victim.isAlive()) {
            if (victim instanceof Player p) HOLDERS.remove(p.getUUID());
            return;
        }
        Vec3 fling = ahead().scale(THROW_AHEAD).add(0, THROW_UP, 0);
        victim.setDeltaMovement(fling);
        victim.hurtMarked = true;
        victim.fallDistance = 0;
        if (victim instanceof ServerPlayer player) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        LivingEntityPatch<?> patch = humanoid(victim);
        if (patch != null) patch.playAnimationSynchronized(CrabKingAnimations.THROWN, 0.0F);
        thrown.put(victim, 0);
        server.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.4F, 0.6F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8F, 0.8F);
    }

    /** Those flying from his throw: on the ground again, hurt and knocked down, free. */
    private void flightTick(ServerLevel server) {
        if (thrown.isEmpty()) return;
        var it = thrown.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            LivingEntity v = e.getKey();
            int ticks = e.getValue() + 1;
            e.setValue(ticks);
            if (!v.isAlive() || v.isRemoved() || v.level() != level()) {
                if (v instanceof Player p) HOLDERS.remove(p.getUUID());
                it.remove();
                continue;
            }
            v.fallDistance = 0;
            // Into a wall: the flight ends there, in a crater.
            // (A wall close by is struck on the very first tick of the flight.)
            boolean wall = v.horizontalCollision;
            if (!wall && !(ticks > 3 && (v.onGround() || v.isInWater())) && ticks < 100) continue;
            it.remove();
            if (v instanceof Player p) HOLDERS.remove(p.getUUID());
            v.invulnerableTime = 0;
            v.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * GRAB_LANDING_DAMAGE);
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(v, LivingEntityPatch.class);
            if (patch != null && v.isAlive()) patch.applyStun(StunType.KNOCKDOWN, 1.8F);
            impact(server, v, wall);
        }
    }

    /**
     * Where a thrown body lands: a blast of dust and air and the sound of an explosion; against a wall, a crater
     * knocked into it round the point it struck (no drops; not unbreakable blocks, nor where mobs may not grief).
     */
    private void impact(ServerLevel server, LivingEntity v, boolean wall) {
        Vec3 at = v.position().add(0, v.getBbHeight() * (wall ? 0.5 : 0.1), 0);
        if (wall) {
            Vec3 heading = v.getDeltaMovement().multiply(1, 0, 1);
            if (heading.lengthSqr() < 1e-4) heading = v.position().subtract(position()).multiply(1, 0, 1);
            Vec3 into = heading.lengthSqr() < 1e-4 ? ahead() : heading.normalize();
            Vec3 centre = at.add(into.scale(0.9));
            if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(server, this)) crater(server, centre);
            at = centre.subtract(into.scale(0.6));
        }
        BlockState under = server.getBlockState(BlockPos.containing(at.x, v.getY() - 0.5, at.z));
        if (!under.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), at.x, at.y, at.z, 70, 1.0, 0.4, 1.0, 0.25);
        server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, 0.5, 0.3, 0.5, 0.0);
        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y, at.z, 14, 0.9, 0.4, 0.9, 0.03);
        server.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 30, 1.4, 0.1, 1.4, 0.12);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.9F);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_BIG_FALL, SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    /** A crater: the blocks within the radius of the centre knocked out, its rim ragged (the outer shell only in part). */
    private void crater(ServerLevel server, Vec3 centre) {
        int r = (int) Math.ceil(CRATER_RADIUS);
        BlockPos c = BlockPos.containing(centre);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            double d = Math.sqrt(p.distToCenterSqr(centre));
            if (d > CRATER_RADIUS) continue;
            // The rim ragged: past three quarters of the radius, about half the blocks stay.
            if (d > CRATER_RADIUS * 0.75 && ((p.getX() * 31 + p.getY() * 17 + p.getZ() * 7) & 1) == 0) continue;
            BlockState state = server.getBlockState(p);
            if (state.isAir() || state.getDestroySpeed(server, p) < 0 || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) continue;
            server.destroyBlock(p, false, this);
        }
    }

    /** Whether this body is still flying from his throw. */
    public boolean isFlying(LivingEntity victim) {
        return thrown.containsKey(victim);
    }

    private void shockwaveTick(ServerLevel server) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        boolean slamming = main != null && main.getRealAnimation() == CrabKingAnimations.SLAM && !main.isEnd();
        if (!slamming) {
            shockwaveSent = false;
            return;
        }
        if (shockwaveSent || main.getElapsedTime() < CrabKingAnimations.SLAM_IMPACT) return;
        shockwaveSent = true;
        shockwave(server);
    }

    /**
     * The pincers meet the ground: everyone near the landing point (not his crabs) takes a share of his attack and is
     * thrown back and up. A guard raised in time parries it; who the pincers just struck or who just parried them is
     * spared: the slam is one blow.
     */
    public void shockwave(ServerLevel server) {
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 at = position().add(look.scale(2.9));
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * SHOCKWAVE_DAMAGE;
        AABB area = new AABB(at, at).inflate(SHOCKWAVE_RADIUS, 1.5, SHOCKWAVE_RADIUS);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive()
                && !(e instanceof ArmoredCrabEntity) && !(e instanceof CrabManEntity) && e.distanceToSqr(at) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS)) {
            boolean struck = victim.getLastHurtByMob() == this && victim.tickCount - victim.getLastHurtByMobTimestamp() <= SLAM_WINDOW;
            if (struck || MurimGuardSkill.parriedWithin(victim, SLAM_WINDOW)) continue;
            if (victim.hurt(damageSources().mobAttack(this), damage)) {
                Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
                Vec3 push = away.lengthSqr() < 1e-4 ? look : away.normalize();
                victim.push(push.x * 0.8, 0.45, push.z * 0.8);
                victim.hurtMarked = true;
            }
        }
        BlockState ground = server.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 70, 1.6, 0.1, 1.6, 0.15);
        server.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.3, at.z, 40, 1.4, 0.2, 1.4, 0.2);
        server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.1, at.z, 20, 1.8, 0.05, 1.8, 0.04);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.7F, 1.2F);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.TURTLE_EGG_CRACK, SoundSource.HOSTILE, 1.4F, 0.5F);
    }

    // ---- saved

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", enraged);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enraged = tag.getBoolean("Enraged");
        // The mist is never saved: loaded, he is seen.
        setInvisible(false);
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }

    // A shell creaking, a deep clicking voice.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.TURTLE_AMBIENT_LAND; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.TURTLE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.TURTLE_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.5F; }
}
