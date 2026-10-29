package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * How the Iron Golem behaves as the Gardien-talisman ({@code docs/mobs/gardien-talisman.png}, storyboard v2
 * {@code docs/mobs/boards/golem.png}), timed on its clips ({@code skeletal/animations/golem}).
 * <ul>
 * <li>A statue: it never moves (no stroll, no flower, no looking about) and leaves monsters be.
 * <li>Whoever harms a villager near it (or the statue itself, or a villager's sworn enemy as vanilla reckons it) wakes
 * it: dust falls, its eyes and runes light, then it walks after that one alone, never running.
 * <li>Within reach, the sky fist (vanilla's blow, which throws up), then 15 ticks open.
 * <li>Kept close too long, the earth bell: both fists on the ground, the ground fractures round it; everyone on the
 * ground within 3.5 blocks (but villagers and golems) takes 6 damage, is thrown back a little and
 * slowed for a second; leaping avoids it; then 20 ticks open.
 * <li>From 4 to 8 blocks, the chain (every 8 s): 4 damage and pulled back before it, then 20 ticks open. A parried
 * chain breaks and staggers it; it takes half again as much while it reels.
 * <li>With no one left to punish it walks back to its place and turns to stone again.
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class GolemFighting extends Goal {
    /** A move: its clip, length (ticks) and the tick it acts. */
    public enum Move {
        WAKE("wake", 24, 14),
        SKY("sky", 36, 14),
        BELL("bell", 40, 16),
        CHAIN("chain", 44, 14),
        STAGGER("stagger", 20, 0);

        final ResourceLocation clip;
        final int length, at;

        Move(String clip, int length, int at) {
            this.clip = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "golem/" + clip);
            this.length = length;
            this.at = at;
        }
    }

    private static final double SKY_REACH = 2.8, BELL_RADIUS = 3.5, BELL_AHEAD = 1.4, CHAIN_FROM = 4, CHAIN_UNTIL = 8;
    private static final int SKY_REST = 30, BELL_REST = 120, CHAIN_REST = 160, REST = 10;
    /** Ticks kept within reach before the bell. */
    private static final int CLOSE = 40;
    private static final double CLOSE_RANGE = 4.5;
    private static final float BELL_DAMAGE = 6, CHAIN_DAMAGE = 4, STAGGERED = 1.5f;
    private static final double WALK = 0.6, HOME_WALK = 0.5;
    /** How far a harmed villager's cry reaches. */
    private static final double CRY = 32;
    private static final String HOME = "murimblock_home";
    /** How far its prey may run before it lets go. */
    private static final double GIVE_UP = 24;
    private static final Map<IronGolem, GolemFighting> FIGHTING = new WeakHashMap<>();

    /** Dev: told of each move a guardian starts. */
    public static java.util.function.BiConsumer<IronGolem, Move> probe;

    private final IronGolem mob;
    private Move move;
    private int time, rest, skyRest, bellRest = 80, chainRest = 40, close, path;
    private boolean woken, stunned, chained;
    /** Whether its sky fist has come since it woke: the bell never before it. */
    private boolean struck;
    private Vec3 anchor;

    GolemFighting(IronGolem mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** The Iron Golem as the Gardien: a statue at its place, its own fight, no wandering, no hunting monsters. */
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getType() != EntityType.IRON_GOLEM
                || !(event.getEntity() instanceof IronGolem golem)) {
            return;
        }
        if (golem.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof GolemFighting)) {
            return;
        }
        golem.goalSelector.removeAllGoals(goal -> true);
        golem.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?>);
        golem.goalSelector.addGoal(1, new GolemFighting(golem));
        golem.goalSelector.addGoal(2, new Statue(golem));
        CompoundTag data = golem.getPersistentData();
        if (!data.contains(HOME)) {
            CompoundTag home = new CompoundTag();
            home.putDouble("x", golem.getX());
            home.putDouble("y", golem.getY());
            home.putDouble("z", golem.getZ());
            home.putFloat("yaw", golem.getYRot());
            data.put(HOME, home);
        }
    }

    /** A villager harmed: every guardian near wakes against the one who did it. */
    @SubscribeEvent
    public static void villagerHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractVillager villager) || villager.level().isClientSide()
                || !(event.getSource().getEntity() instanceof LivingEntity culprit) || culprit instanceof IronGolem
                || culprit == villager) {
            return;
        }
        for (IronGolem golem : villager.level().getEntitiesOfClass(IronGolem.class,
                villager.getBoundingBox().inflate(CRY))) {
            if (golem.getTarget() == null && golem.canAttack(culprit)) {
                golem.setTarget(culprit);
            }
        }
    }

    /** Reeling, it takes half again as much. */
    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof IronGolem golem && FIGHTING.get(golem) instanceof GolemFighting fighting) {
            CombatState state = CombatServer.existing(golem);
            if (fighting.move == Move.STAGGER || state != null && state.stunTicks > 0) {
                event.setAmount(event.getAmount() * STAGGERED);
            }
        }
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || move != null;
    }

    @Override
    public void start() {
        FIGHTING.put(mob, this);
        mob.setAggressive(true);
        path = 0;
        close = 0;
        if (!woken) {
            woken = true;
            begin(Move.WAKE);
        }
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        if (move != null) {
            end(true);
        }
        if (mob.getTarget() == null) {
            // No one left to punish: it goes back to stone, and will wake again.
            woken = false;
            FIGHTING.remove(mob);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        CombatState state = CombatServer.existing(mob);
        boolean stun = state != null && (state.stunTicks > 0 || state.hitStop > 0);
        if (stun) {
            if (!stunned && state.stunTicks > 0) {
                // Its chain parried, or staggered: it reels, the stone cracking.
                move = null;
                begin(Move.STAGGER);
            }
            stunned = true;
            hold();
            return;
        }
        stunned = false;
        skyRest--;
        bellRest--;
        chainRest--;
        if (target != null && move != Move.WAKE) {
            // Kept close: counted through its own moves too, the prey thrown up and landing near again (not while it
            // wakes: the bell never comes first).
            close = mob.distanceTo(target) <= CLOSE_RANGE ? close + 1 : 0;
        }
        if (move != null) {
            play(target);
            return;
        }
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30, 30);
        double distance = mob.distanceTo(target);
        double reach = SKY_REACH + target.getBbWidth() / 2;
        if (--rest <= 0) {
            if (close >= CLOSE && bellRest <= 0 && struck) {
                begin(Move.BELL);
                return;
            }
            if (distance <= reach && skyRest <= 0) {
                begin(Move.SKY);
                return;
            }
            if (distance >= CHAIN_FROM && distance <= CHAIN_UNTIL && chainRest <= 0
                    && mob.getSensing().hasLineOfSight(target)) {
                begin(Move.CHAIN);
                return;
            }
        }
        if (distance > 2.2) {
            if (--path <= 0) {
                mob.getNavigation().moveTo(target, WALK);
                path = 8;
            }
        } else {
            mob.getNavigation().stop();
        }
    }

    private void begin(Move next) {
        move = next;
        time = 0;
        anchor = mob.position();
        mob.getNavigation().stop();
        CombatServer.anim(mob, Animator.Priority.MIDDLE, next.clip, false);
        switch (next) {
            case WAKE -> struck = false;
            case SKY -> {
                skyRest = SKY_REST;
                struck = true;
            }
            case BELL -> {
                bellRest = BELL_REST;
                close = 0;
            }
            case CHAIN -> {
                chainRest = CHAIN_REST;
                chained = false;
            }
            default -> {
            }
        }
        if (probe != null) {
            probe.accept(mob, next);
        }
    }

    private void play(LivingEntity target) {
        time++;
        hold();
        if (target != null && time <= move.at) {
            face(target);
        }
        if (target != null) {
            switch (move) {
                case SKY -> {
                    if (time == move.at) {
                        sky(target);
                    }
                }
                case BELL -> {
                    if (time == move.at) {
                        bell(target);
                    }
                }
                case CHAIN -> chain(target);
                default -> {
                }
            }
        }
        if (time >= move.length) {
            end(false);
        }
    }

    /** Whether {@code body} stands on the ground (a leap clears the bell): something solid just under its feet. */
    private static boolean standing(LivingEntity body) {
        return body.onGround() || !body.level().noCollision(body, body.getBoundingBox().move(0, -0.2, 0));
    }

    private boolean inFront(LivingEntity target, double cos) {
        Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
        Vec3 facing = Vec3.directionFromRotation(0, mob.yBodyRot);
        return toward.lengthSqr() < 1e-4 || toward.normalize().dot(facing) > cos;
    }

    /** The sky fist: vanilla's blow (its damage, the throw upward), if the prey is in reach and in front. */
    private void sky(LivingEntity target) {
        if (mob.distanceTo(target) <= SKY_REACH + 0.6 + target.getBbWidth() / 2 && inFront(target, 0.3)) {
            mob.doHurtTarget(target);
        } else {
            mob.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1, 0.8f);
        }
    }

    /**
     * The earth bell: the ground fractures round the fists (the clip's own event); everyone standing on it within its
     * radius is struck and thrown back a little, its prey or not, but the villagers it guards and other golems.
     */
    private void bell(LivingEntity target) {
        Vec3 at = mob.position().add(Vec3.directionFromRotation(0, mob.yBodyRot).scale(BELL_AHEAD));
        mob.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 0.6f);
        for (LivingEntity victim : mob.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BELL_RADIUS, 1.5,
                BELL_RADIUS), victim -> victim != mob && standing(victim) && victim.position().distanceTo(at) <= BELL_RADIUS
                && !(victim instanceof net.minecraft.world.entity.npc.AbstractVillager)
                && !(victim instanceof net.minecraft.world.entity.animal.AbstractGolem))) {
            if (victim.hurt(mob.damageSources().mobAttack(mob), BELL_DAMAGE)) {
                mob.setLastHurtMob(victim);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4), mob);
                Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? Vec3.ZERO : away.normalize().scale(0.45);
                victim.push(away.x, 0.3, away.z);
                victim.hurtMarked = true;
            }
        }
    }

    /** The chain: thrown on its tick at a prey in front within reach; if it holds, the prey is pulled back before it. */
    private void chain(LivingEntity target) {
        if (time == move.at) {
            if (mob.distanceTo(target) <= CHAIN_UNTIL + 0.5 && inFront(target, 0.7)
                    && mob.getSensing().hasLineOfSight(target)
                    && target.hurt(mob.damageSources().mobAttack(mob), CHAIN_DAMAGE)) {
                mob.setLastHurtMob(target);
                CombatState state = CombatServer.existing(mob);
                chained = state == null || state.stunTicks <= 0;
            }
        } else if (chained && time > move.at && time <= move.at + 10) {
            Vec3 before = mob.position().add(Vec3.directionFromRotation(0, mob.yBodyRot).scale(2));
            Vec3 pull = before.subtract(target.position()).multiply(1, 0, 1);
            if (pull.length() < 0.4) {
                chained = false;
                return;
            }
            Vec3 speed = pull.scale(0.3);
            if (speed.length() > 0.9) {
                speed = speed.normalize().scale(0.9);
            }
            target.setDeltaMovement(speed.x, Math.max(target.getDeltaMovement().y, 0.08), speed.z);
            target.hurtMarked = true;
        }
    }

    private void face(LivingEntity target) {
        mob.getLookControl().setLookAt(target, 30, 30);
        double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
    }

    private void hold() {
        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0);
        mob.setSpeed(0);
        mob.setZza(0);
        mob.setXxa(0);
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
        if (anchor != null) {
            mob.setPos(anchor.x, mob.getY(), anchor.z);
        }
    }

    private void end(boolean broken) {
        if (broken) {
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
        }
        move = null;
        anchor = null;
        rest = REST;
    }

    /** At peace: back to its place, turned as it stood, and still as stone. */
    static final class Statue extends Goal {
        private final IronGolem mob;
        private int path;

        Statue(IronGolem mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target != null && (!target.isAlive() || !mob.canAttack(target) || mob.distanceTo(target) > GIVE_UP)) {
                // Dead, out of its reach (a creative player) or gone far: it lets go.
                mob.setTarget(null);
            }
            return mob.getTarget() == null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            CompoundTag home = mob.getPersistentData().getCompound(HOME);
            if (home.isEmpty()) {
                return;
            }
            Vec3 place = new Vec3(home.getDouble("x"), home.getDouble("y"), home.getDouble("z"));
            if (mob.position().distanceToSqr(place) > 0.6 * 0.6) {
                if (--path <= 0) {
                    if (!mob.getNavigation().moveTo(place.x, place.y, place.z, HOME_WALK)
                            && mob.position().distanceToSqr(place) > 64) {
                        // No way back: this is its place now.
                        home.putDouble("x", mob.getX());
                        home.putDouble("y", mob.getY());
                        home.putDouble("z", mob.getZ());
                    }
                    path = 20;
                }
                return;
            }
            mob.getNavigation().stop();
            float yaw = home.getFloat("yaw");
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
            mob.yBodyRot = yaw;
            mob.setXRot(0);
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
        }
    }
}
