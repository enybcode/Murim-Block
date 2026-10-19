package com.murimblock.mob;

import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.RavenFamiliarEntity;
import com.murimblock.martial.RavenFx;
import com.murimblock.martial.RavenParticles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The Raven Master (鸦主, docs/mobs/raven-master.md): a wandering master of the Raven Messenger Art, the black lacquered
 * crow-beaked half mask over his face, long white hair, the iron fan open before his chest and his shadow raven on his
 * left shoulder (scripts/animation/raven-master-3d.cjs). Not the Crow Master of the Raven Tower, who sells the art's
 * scroll: this one keeps it, and gives it to whoever beats him.
 * <p>He shows himself (RavenMasterSightings): far off, perched on a high exposed spot in a player's sight, still, a few
 * ravens circling over him, watching. That is all he does while nobody meets his eyes: after 20 to 30 s, or if the
 * player comes within {@link #APPROACH} blocks, he takes off in a cloud of ravens and is gone. Looked in the eyes by the one
 * he watches (their look on his face, as an Enderman's stare; or struck by anyone), he bursts into ravens, comes out beside that player and
 * fights:
 * <ul>
 *   <li>the iron fan's strikes (RavenMasterPatch), every one parryable;</li>
 *   <li>the shadow door: through a door of the Raven Halls, out behind his foe (RavenFx doors at both ends);</li>
 *   <li>the raven swarm: the fan pointed, a swarm of ravens flies at his foe and pecks four times, each a blow from him
 *   that a guard takes and a perfect parry scatters;</li>
 *   <li>below half his health, his second phase: quicker, his doors and swarms more often, the swarm larger.</li>
 * </ul>
 * If he loses sight of his foe for {@link #LOST_SIGHT} ticks, or his foe flees beyond {@link #FLED} blocks, he disperses:
 * the encounter is over. A hard boss: 300 health, armour 10, hardly moved, his health in a purple boss bar while he
 * fights (hidden while he watches). Killed by a player he always leaves the Raven Art's scroll
 * (loot_table/entities/raven_master.json), and the first victory gives Qi Max (CultivationRules.firstVictoryQiMax).
 * <p>Brought by the sightings he is fleeting: never saved, gone when the encounter is. From a spawn egg or /summon he
 * stays where he is put, watching whoever is nearest, and goes back to watching after a fight instead of vanishing.
 */
public class RavenMasterEntity extends Monster {
    /** What he is doing: watching from afar, or fighting the one who met his eyes. */
    public enum Mood { WATCHING, HUNTING }

    /** How long he watches before he is gone (ticks: 20 to 30 s). */
    public static final int WATCH_MIN = 400, WATCH_MAX = 600;
    /** How near the watched player may come without meeting his eyes before he leaves (blocks). */
    public static final double APPROACH = 20;
    /** How far a stare reaches (blocks), and how wide it is (the Enderman's: 1 - this / distance of the cosine). */
    public static final double STARE_RANGE = 64, STARE_TOLERANCE = 0.025;
    /** Ticks out of his sight before he disperses; how far his foe may flee (blocks). */
    public static final int LOST_SIGHT = 200;
    public static final double FLED = 48;
    /** The share of his health below which he fights in earnest (his second phase). */
    public static final float SECOND_PHASE = 0.5F;
    /** Health he recovers each second while nobody fights him. */
    static final float RECOVERY = 2.0F;
    /** The ravens circling over him while he watches. */
    static final int CIRCLING = 3;
    /** The swarm: its ravens (more in his second phase), how fast it flies (blocks a tick), its pecks and their spacing (ticks), each a share of his attack, its reach round its heart. */
    static final int SWARM_RAVENS = 5, SWARM_RAVENS_EARNEST = 8, PECKS = 4, PECK_EVERY = 6;
    static final double SWARM_SPEED = 0.8, SWARM_REACH = 1.9;
    public static final float PECK_DAMAGE = 0.35F;
    /** Ticks into the gestures when the door opens and when the swarm takes off (MurimAnimations' raven moves). */
    static final int STEP_THROUGH = 4, SWARM_OFF = 6;

    private static final ResourceLocation SECOND_PHASE_SPEED = ResourceLocation.fromNamespaceAndPath("murimblock", "raven_master_second_phase");

    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private Mood mood = Mood.WATCHING;
    /** Brought by the sightings: gone after watching or after the fight, never saved. */
    private boolean fleeting;
    /** The player he shows himself to (fleeting only). */
    @Nullable private UUID watched;
    private int watchLeft = WATCH_MAX, unseen, doorIn = 80, swarmIn = 160;
    /** A door step under way: the tick he goes through (-1: none). */
    private int stepAt = -1;
    private final List<RavenFamiliarEntity> circling = new ArrayList<>();
    // ---- the swarm under way
    private final List<RavenFamiliarEntity> swarm = new ArrayList<>();
    private int swarmOffAt = -1, swarmTicks = -1, pecksDealt;
    /** Pecks of his swarms that struck something, all told. */
    private int pecksLanded;
    @Nullable private Vec3 swarmHeart;
    /** Ravens of a finished swarm flying off, and when they are gone. */
    private final List<RavenFamiliarEntity> leaving = new ArrayList<>();
    private int leavingUntil;

    public RavenMasterEntity(EntityType<? extends RavenMasterEntity> type, Level level) {
        super(type, level);
        xpReward = 100;
        setCanPickUpLoot(false);
        bossBar.setVisible(false);
    }

    public static AttributeSupplier.Builder createRavenMasterAttributes() {
        // His own 6 and the iron fan's 3: 9 a strike, before the moves' multipliers.
        return EpicFightMobSupport.humanoidAttributes(Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300).add(Attributes.ARMOR, 10).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.STEP_HEIGHT, 1.0))
                .add(EpicFightAttributes.STUN_ARMOR, 16.0).add(EpicFightAttributes.IMPACT, 2.2);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        // No wandering and no target of his own: he watches (customServerAiStep) until someone meets his eyes. Epic Fight
        // installs his attack and chase goals for the fan in hand (RavenMasterPatch).
    }

    // ---------------------------------------------------------------- his state

    public Mood mood() {
        return mood;
    }

    public boolean hunting() {
        return mood == Mood.HUNTING;
    }

    public boolean fleeting() {
        return fleeting;
    }

    /** Shows him to this player for `ticks` (the sightings): he is fleeting from now on. */
    public void watch(ServerPlayer player, int ticks) {
        fleeting = true;
        watched = player.getUUID();
        watchLeft = ticks;
        mood = Mood.WATCHING;
    }

    /** Whether he fights in earnest now (below half his health, until he is whole again). */
    public boolean secondPhase() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        return speed != null && speed.hasModifier(SECOND_PHASE_SPEED);
    }

    /**
     * Whether this player looks him in the eyes: their look within the Enderman's stare of his eyes, within
     * {@link #STARE_RANGE} blocks, nothing between them. Only players who can be fought (not creative, not spectators).
     */
    public boolean staredAtBy(Player player) {
        if (player.isSpectator() || player.isCreative() || !player.isAlive()) return false;
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(getX() - player.getX(), getEyeY() - player.getEyeY(), getZ() - player.getZ());
        double distance = to.length();
        if (distance > STARE_RANGE || distance < 1.0E-3) return false;
        return view.dot(to.scale(1 / distance)) > 1.0 - STARE_TOLERANCE / distance && player.hasLineOfSight(this);
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (mood == Mood.WATCHING) watchTick(level);
        else huntTick(level);
        if (isRemoved()) return;
        swarmTick(level);
        if (!leaving.isEmpty() && tickCount >= leavingUntil) {
            leaving.forEach(Entity::discard);
            leaving.clear();
        }
        if (!secondPhase() && getHealth() <= getMaxHealth() * SECOND_PHASE) enterSecondPhase(level);
        else if (secondPhase() && getHealth() >= getMaxHealth()) leaveSecondPhase();
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setVisible(mood == Mood.HUNTING);
        setAggressive(mood == Mood.HUNTING && getTarget() != null);
    }

    // ---------------------------------------------------------------- watching

    @Nullable private ServerPlayer watchedPlayer(ServerLevel level) {
        if (fleeting) {
            ServerPlayer player = watched == null ? null : level.getServer().getPlayerList().getPlayer(watched);
            return player != null && player.level() == level ? player : null;
        }
        Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), STARE_RANGE, p -> !p.isSpectator() && !((Player) p).isCreative());
        return nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private void watchTick(ServerLevel level) {
        getNavigation().stop();
        setTarget(null);
        ServerPlayer player = watchedPlayer(level);
        if (fleeting && (player == null || !player.isAlive())) {
            vanish(level);
            return;
        }
        if (player != null) {
            getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ(), 10, 10);
            yBodyRot = Mth.approachDegrees(yBodyRot, yHeadRot, 4);
        }
        circleTick(level);
        // The one he watches, meeting his eyes (anyone else starts the fight by striking him).
        if (player != null && staredAtBy(player)) {
            ambush(level, player);
            return;
        }
        if (!fleeting) {
            if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(RECOVERY);
            return;
        }
        if (--watchLeft <= 0 || player.distanceToSqr(this) < APPROACH * APPROACH) vanish(level);
    }

    /** The ravens circling over his head while he watches, a slow wide ring, each a third of the way round from the next. */
    private void circleTick(ServerLevel level) {
        circling.removeIf(Entity::isRemoved);
        while (circling.size() < CIRCLING) {
            RavenFamiliarEntity raven = raven(level, getEyePosition().add(0, 1.5, 0));
            if (raven == null) break;
            circling.add(raven);
        }
        if (tickCount % 8 != 0) return;
        for (int i = 0; i < circling.size(); i++) {
            double angle = (tickCount + 8) * 0.07 + i * Math.PI * 2 / CIRCLING;
            double radius = 2.6 + 0.4 * Math.sin(tickCount * 0.05 + i);
            circling.get(i).sendTo(position().add(Math.cos(angle) * radius, 3.4 + 0.5 * Math.sin(angle * 2 + i), Math.sin(angle) * radius), 8);
        }
    }

    /** Met in the eyes (or struck while he watched): a cloud of ravens, and he comes out beside that player to fight. */
    public void ambush(ServerLevel level, ServerPlayer player) {
        burst(level, position().add(0, 1.0, 0));
        Vec3 spot = besidePlayer(level, player);
        if (spot != null) {
            teleportTo(spot.x, spot.y, spot.z);
            burst(level, spot.add(0, 1.0, 0));
            RavenFx.door(level, spot, player.getYRot() + 180, 14);
        }
        faceTowards(player);
        engage(player);
        player.displayClientMessage(Component.translatable("message.murimblock.raven_master.met_eyes"), true);
    }

    /** He fights this one from now on, where he stands (his first door and swarm a few seconds off). */
    public void engage(LivingEntity target) {
        // His circling ravens scatter, up and away.
        for (RavenFamiliarEntity raven : circling) {
            if (raven.isRemoved()) continue;
            raven.sendTo(raven.position().add(random.nextGaussian() * 6, 5 + random.nextDouble() * 5, random.nextGaussian() * 6), 16);
            leaving.add(raven);
        }
        leavingUntil = tickCount + 18;
        circling.clear();
        mood = Mood.HUNTING;
        unseen = 0;
        doorIn = 60 + random.nextInt(40);
        swarmIn = 100 + random.nextInt(60);
        setTarget(target);
    }

    /** Where he comes out by the player: a few blocks ahead of their look, somewhere he can stand; null if nowhere. */
    @Nullable Vec3 besidePlayer(ServerLevel level, LivingEntity player) {
        Vec3 ahead = Vec3.directionFromRotation(0, player.getYRot());
        for (double distance : new double[]{3.5, 2.5, 5.0})
            for (int turn : new int[]{0, 35, -35, 70, -70, 180}) {
                Vec3 at = player.position().add(ahead.yRot((float) Math.toRadians(turn)).scale(distance));
                Vec3 stand = standAt(level, at, (int) Math.floor(player.getY()));
                if (stand != null) return stand;
            }
        return null;
    }

    /** A place to stand in the column of `at`, from two blocks over `y` to three under; null if none. */
    @Nullable private Vec3 standAt(ServerLevel level, Vec3 at, int y) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos feet = BlockPos.containing(at.x, y + dy, at.z);
            if (level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) continue;
            Vec3 to = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            AABB box = getDimensions(getPose()).makeBoundingBox(to);
            if (level.noCollision(this, box) && !level.containsAnyLiquid(box)) return to;
        }
        return null;
    }

    private void faceTowards(Entity entity) {
        double dx = entity.getX() - getX(), dz = entity.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // Struck while he watched (an arrow from afar): he comes for whoever did it.
        if (hurt && mood == Mood.WATCHING && isAlive() && source.getEntity() instanceof ServerPlayer player && level() instanceof ServerLevel level)
            ambush(level, player);
        return hurt;
    }

    // ---------------------------------------------------------------- fighting

    private void huntTick(ServerLevel level) {
        LivingEntity target = getTarget();
        boolean gone = target == null || !target.isAlive() || target.level() != level
                || target instanceof Player p && (p.isCreative() || p.isSpectator());
        if (!gone) unseen = hasLineOfSight(target) ? 0 : unseen + 1;
        if (gone || unseen > LOST_SIGHT || distanceToSqr(target) > FLED * FLED) {
            disperse(level);
            return;
        }
        getLookControl().setLookAt(target, 30, 30);
        boolean earnest = secondPhase();
        if (stepAt >= 0 && tickCount >= stepAt) {
            stepAt = -1;
            shadowStep(level, target);
        }
        double distance = distanceTo(target);
        if (stepAt < 0 && --doorIn <= 0 && free() && (distance > 6 || random.nextInt(3) == 0)) {
            doorIn = earnest ? 60 + random.nextInt(50) : 120 + random.nextInt(80);
            startShadowStep();
        }
        if (swarmOffAt < 0 && swarmTicks < 0 && --swarmIn <= 0 && free() && distance < 16 && hasLineOfSight(target)) {
            swarmIn = earnest ? 180 + random.nextInt(60) : 300 + random.nextInt(100);
            startSwarm();
        }
    }

    /** Whether he is free to start a move (no strike or gesture of his playing). */
    private boolean free() {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        return patch == null || !patch.getEntityState().inaction();
    }

    private void play(AnimationAccessor<? extends StaticAnimation> animation) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        if (patch != null && animation != null) patch.playAnimationSynchronized(animation, 0.0F);
    }

    /** The fan drawn across him: he goes through a shadow door a moment later (STEP_THROUGH). */
    public void startShadowStep() {
        play(MurimAnimations.RAVEN_HALLS_STEP);
        stepAt = tickCount + STEP_THROUGH;
    }

    /** Through a door of the Raven Halls where he stands, out of another behind his foe, facing them. */
    public boolean shadowStep(ServerLevel level, LivingEntity target) {
        Vec3 behind = target.position().add(Vec3.directionFromRotation(0, target.getYRot()).scale(-2.2));
        Vec3 spot = standAt(level, behind, (int) Math.floor(target.getY()));
        if (spot == null) spot = besidePlayer(level, target);
        if (spot == null) return false;
        RavenFx.door(level, position(), getYRot(), 12);
        teleportTo(spot.x, spot.y, spot.z);
        RavenFx.door(level, spot, target.getYRot(), 12);
        faceTowards(target);
        return true;
    }

    /** The fan pointed at his foe: a swarm of ravens takes off a moment later (SWARM_OFF) and flies at them. */
    public void startSwarm() {
        play(MurimAnimations.RAVEN_EXCHANGE);
        swarmOffAt = tickCount + SWARM_OFF;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 1.6F, 0.45F);
    }

    /** Whether a swarm of his flies now. */
    public boolean swarming() {
        return swarmTicks >= 0;
    }

    /** How many pecks of his swarms have struck something. */
    public int pecksLanded() {
        return pecksLanded;
    }

    private void swarmTick(ServerLevel level) {
        if (swarmOffAt >= 0 && tickCount >= swarmOffAt) {
            swarmOffAt = -1;
            swarmTicks = 0;
            pecksDealt = 0;
            swarmHeart = getEyePosition();
            int count = secondPhase() ? SWARM_RAVENS_EARNEST : SWARM_RAVENS;
            for (int i = 0; i < count; i++) {
                RavenFamiliarEntity raven = raven(level, swarmHeart.add(random.nextGaussian() * 0.3, random.nextGaussian() * 0.3, random.nextGaussian() * 0.3));
                if (raven != null) swarm.add(raven);
            }
            RavenFx.feathers(level, swarmHeart, 14, 0.8);
        }
        if (swarmTicks < 0 || swarmHeart == null) return;
        LivingEntity target = getTarget();
        swarmTicks++;
        // The swarm flies at its foe, then hangs round them pecking, following them slowly.
        if (target != null && target.isAlive()) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(swarmHeart);
            double speed = pecksDealt == 0 && to.length() > 1.2 ? SWARM_SPEED : 0.22;
            if (to.length() > 1.0E-3) swarmHeart = swarmHeart.add(to.scale(Math.min(1, speed / to.length())));
        }
        for (int i = 0; i < swarm.size(); i++) {
            if ((swarmTicks + i) % 3 != 0) continue;
            double angle = swarmTicks * 0.45 + i * Math.PI * 2 / swarm.size();
            double radius = 0.7 + 0.35 * Math.sin(swarmTicks * 0.3 + i * 1.7);
            swarm.get(i).sendTo(swarmHeart.add(Math.cos(angle) * radius, Math.sin(angle * 1.5 + i) * 0.5 - 0.4, Math.sin(angle) * radius), 3);
        }
        level.sendParticles(RavenParticles.FEATHER.get(), swarmHeart.x, swarmHeart.y, swarmHeart.z, 2, 0.5, 0.4, 0.5, 0.03);
        boolean arrived = target != null && swarmHeart.distanceTo(target.position().add(0, target.getBbHeight() * 0.6, 0)) < SWARM_REACH;
        if (arrived && swarmTicks % PECK_EVERY == 0) {
            pecksDealt++;
            boolean scattered = false;
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(swarmHeart, swarmHeart).inflate(SWARM_REACH),
                    e -> e != this && e.isAlive() && !(e instanceof RavenMasterEntity))) {
                // A blow from him: a raised guard takes it, a perfect parry scatters the swarm.
                if (victim.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * PECK_DAMAGE)) pecksLanded++;
                if (MurimGuardSkill.parriedThisTick(victim)) scattered = true;
            }
            level.playSound(null, swarmHeart.x, swarmHeart.y, swarmHeart.z, SoundEvents.PARROT_HURT, SoundSource.HOSTILE, 1.0F, 0.5F);
            if (scattered || pecksDealt >= PECKS) endSwarm(level);
        } else if (swarmTicks > 80) endSwarm(level);
    }

    /** The swarm breaks up, its ravens flying off upward, gone a moment later. */
    private void endSwarm(ServerLevel level) {
        if (swarmHeart != null) RavenFx.feathers(level, swarmHeart, 12, 1.2);
        for (RavenFamiliarEntity raven : swarm) {
            if (raven.isRemoved()) continue;
            raven.sendTo(raven.position().add(random.nextGaussian() * 4, 6 + random.nextDouble() * 4, random.nextGaussian() * 4), 14);
            leaving.add(raven);
        }
        leavingUntil = tickCount + 16;
        swarm.clear();
        swarmTicks = -1;
        swarmHeart = null;
    }

    // ---------------------------------------------------------------- his phases

    private void enterSecondPhase(ServerLevel level) {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SECOND_PHASE_SPEED, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (mood != Mood.HUNTING) return;
        burst(level, position().add(0, 1.0, 0));
        Component words = Component.translatable("message.murimblock.raven_master.second_phase");
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) player.sendSystemMessage(words);
    }

    private void leaveSecondPhase() {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SECOND_PHASE_SPEED);
    }

    // ---------------------------------------------------------------- gone

    /** The encounter is over: fleeting, he is gone in ravens; placed, he goes back to watching where he stands. */
    public void disperse(ServerLevel level) {
        LivingEntity target = getTarget();
        if (target instanceof ServerPlayer player && player.isAlive())
            player.displayClientMessage(Component.translatable("message.murimblock.raven_master.dispersed"), true);
        if (swarmTicks >= 0) endSwarm(level);
        setTarget(null);
        if (fleeting) {
            vanish(level);
            return;
        }
        mood = Mood.WATCHING;
        unseen = 0;
    }

    /** He takes off in a cloud of ravens and is gone. */
    public void vanish(ServerLevel level) {
        burst(level, position().add(0, 1.0, 0));
        discard();
    }

    /**
     * A cloud of ravens bursting at `at`: feathers, black smoke and ink, sent even to players far off (he is seen from
     * 80 blocks), and the flapping of many wings.
     */
    public static void burst(ServerLevel level, Vec3 at) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) > 160 * 160) continue;
            level.sendParticles(player, RavenParticles.FEATHER.get(), true, at.x, at.y, at.z, 50, 0.7, 0.9, 0.7, 0.12);
            level.sendParticles(player, ParticleTypes.LARGE_SMOKE, true, at.x, at.y, at.z, 30, 0.5, 0.8, 0.5, 0.03);
            level.sendParticles(player, ParticleTypes.SQUID_INK, true, at.x, at.y, at.z, 24, 0.5, 0.8, 0.5, 0.08);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PARROT_FLY, SoundSource.HOSTILE, 4.0F, 0.55F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    @Nullable private RavenFamiliarEntity raven(ServerLevel level, Vec3 at) {
        RavenFamiliarEntity raven = MurimEntities.RAVEN_FAMILIAR.get().create(level);
        if (raven == null) return null;
        raven.owner(getUUID()).setPos(at.x, at.y, at.z);
        level.addFreshEntity(raven);
        return raven;
    }

    @Override public void die(DamageSource source) {
        super.die(source);
        if (source.getEntity() instanceof ServerPlayer player) RavenMasterSightings.markBeaten(player);
    }

    @Override public void remove(RemovalReason reason) {
        circling.forEach(Entity::discard);
        swarm.forEach(Entity::discard);
        leaving.forEach(Entity::discard);
        super.remove(reason);
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

    /** Whether this player sees his boss bar now (near him, and he fights). */
    public boolean showsBossBarTo(ServerPlayer player) {
        return bossBar.isVisible() && bossBar.getPlayers().contains(player);
    }

    // ---------------------------------------------------------------- spawning and saving

    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.IRON_FAN.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return result;
    }

    /** Never despawned by the game: the sightings and his encounters say when he goes. */
    @Override public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** A fleeting one is never saved: back in a world, he would watch no one. */
    @Override public boolean shouldBeSaved() {
        return !fleeting && super.shouldBeSaved();
    }

    /** He is seen from far off (80 blocks and more), farther than a man's size would be drawn. */
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Fleeting", fleeting);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fleeting = tag.getBoolean("Fleeting");
    }

    // A man's voice under the mask; a raven's cry at rest.
    @Override protected SoundEvent getAmbientSound() { return mood == Mood.WATCHING ? null : SoundEvents.PARROT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
}
