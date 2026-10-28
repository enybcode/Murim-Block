package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * How the zombie fights as the Disciple déchu ({@code docs/mobs/disciple-dechu.png}): it walks up, stands in its half
 * guard about two blocks off and attacks about every two seconds with one of three moves, timed on its clips
 * ({@code skeletal/animations/disciple}): the clumsy haymaker, the double blow (a jab then the haymaker; always after
 * two of its blows were blocked, sometimes anyway) and the lunge when its prey backs away. Each blow lands at the
 * clip's impact, through the guard, parry and dodge of {@link CombatServer}; in a crowd around the prey it only
 * strikes, so the fight stays readable. Every client sees the move it chose.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class DiscipleFighting extends Goal {
    /** A move: its clip, length and impacts (ticks), each impact's share of the attack damage, its reach. */
    public enum Move {
        STRIKE("strike", 18, new int[] {5}, new float[] {1}, 2.4),
        DOUBLE("double", 22, new int[] {3, 10}, new float[] {0.67f, 1}, 2.4),
        LUNGE("lunge", 26, new int[] {9, 10, 11, 12}, new float[] {1.33f}, 1.9);

        final ResourceLocation clip;
        final int length;
        final int[] impacts;
        final float[] damage;
        final double reach;

        Move(String clip, int length, int[] impacts, float[] damage, double reach) {
            this.clip = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "disciple/" + clip);
            this.length = length;
            this.impacts = impacts;
            this.damage = damage;
            this.reach = reach;
        }
    }

    /**
     * Where it waits in its guard between moves, how close it steps in to strike, and how far it lunges from (blocks,
     * centre to centre).
     */
    private static final double GUARD_DISTANCE = 3.0, CLOSE_IN = 1.8, LUNGE_FROM = 3.2, LUNGE_UNTIL = 5.5;
    /** Its prey is backing away when it got this much further in the last half second. */
    private static final double BACKING = 0.5;
    /** The lunge answers a retreat: it may come this many ticks before the next move is due. */
    private static final int LUNGE_EARLY = 20;
    /** Ticks before a move when it steps in. */
    private static final int STEP_IN = 12;
    /** Ticks between moves (about two seconds, a little random so a crowd does not strike in step). */
    private static final int REST = 34, REST_RANDOM = 16;
    private static final int LUNGE_REST = 100;
    /** The share of moves that are the double blow even when nothing was blocked. */
    private static final float DOUBLE_CHANCE = 0.25f;

    /** Dev: told of each move a disciple starts. */
    public static java.util.function.BiConsumer<Mob, Move> probe;

    private final Mob mob;
    private Move move;
    private int time, rest = 20, lungeRest, blocked, path;
    private boolean landed;
    /** The distance to its prey over the last half second, a tick apart (a ring). */
    private final double[] distances = new double[10];
    private int ticks;

    DiscipleFighting(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** The zombie (the plain one: not the husk, drowned or our own zombies) fights as the disciple. */
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getType() != EntityType.ZOMBIE
                || !(event.getEntity() instanceof Zombie zombie)) {
            return;
        }
        boolean installed = zombie.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof DiscipleFighting);
        if (!installed) {
            zombie.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal);
            zombie.goalSelector.addGoal(2, new DiscipleFighting(zombie));
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
        mob.setAggressive(true);
        path = 0;
        LivingEntity target = mob.getTarget();
        ticks = 0;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        if (move != null) {
            end(true);
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
        if (state != null && (state.stunTicks > 0 || state.hitStop > 0)) {
            // Reeling from a blow or a parry: the move is lost.
            if (move != null) {
                end(true);
            }
            return;
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
        double halfSecondAgo = ticks >= distances.length ? distances[ticks % distances.length] : distance;
        distances[ticks++ % distances.length] = distance;
        boolean backing = distance - halfSecondAgo > BACKING;
        rest--;
        if (rest == STEP_IN) {
            path = 0;
        }
        lungeRest--;
        // It waits in its guard a little off, and steps in as its next move comes.
        double keep = rest <= STEP_IN ? CLOSE_IN : GUARD_DISTANCE;
        if (distance > keep + 0.3) {
            if (--path <= 0) {
                mob.getNavigation().moveTo(target, 1);
                path = 10;
            }
        } else {
            mob.getNavigation().stop();
            path = 0;
        }
        if (rest > LUNGE_EARLY || !mob.getSensing().hasLineOfSight(target)) {
            return;
        }
        Move next = choose(target, distance, backing);
        if (rest > 0 && next != Move.LUNGE) {
            return;
        }
        if (next != null) {
            begin(next);
        }
    }

    private Move choose(LivingEntity target, double distance, boolean backing) {
        if (distance >= LUNGE_FROM && distance <= LUNGE_UNTIL && backing && lungeRest <= 0 && !crowded(target)) {
            return Move.LUNGE;
        }
        if (distance > Move.STRIKE.reach + target.getBbWidth() / 2) {
            return null;
        }
        if (crowded(target)) {
            return Move.STRIKE;
        }
        if (blocked >= 2 || mob.getRandom().nextFloat() < DOUBLE_CHANCE) {
            return Move.DOUBLE;
        }
        return Move.STRIKE;
    }

    /** Two other disciples already fighting the same prey close by. */
    private boolean crowded(LivingEntity target) {
        return mob.level().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(5),
                other -> other != mob && other.getType() == EntityType.ZOMBIE && other.isAlive()
                        && other.getTarget() == target).size() >= 2;
    }

    private void begin(Move next) {
        move = next;
        if (next == Move.DOUBLE) {
            blocked = 0;
        }
        time = 0;
        landed = false;
        mob.getNavigation().stop();
        CombatServer.anim(mob, Animator.Priority.MIDDLE, next.clip, false);
        if (probe != null) {
            probe.accept(mob, next);
        }
    }

    private void play(LivingEntity target) {
        time++;
        boolean windUp = time < move.impacts[0];
        if (target != null && windUp) {
            // It turns to its prey while it winds up; once the blow is thrown, it is thrown.
            mob.getLookControl().setLookAt(target, 30, 30);
            mob.setYRot(mob.getYHeadRot());
            mob.yBodyRot = mob.getYHeadRot();
        }
        if (move == Move.LUNGE && time == 6 && target != null) {
            Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
            double speed = Mth.clamp((toward.length() - 1.2) * 0.16, 0.3, 0.75);
            Vec3 push = toward.normalize().scale(speed);
            mob.setDeltaMovement(push.x, 0.36, push.z);
            mob.hasImpulse = true;
        }
        for (int i = 0; i < move.impacts.length; i++) {
            if (time == move.impacts[i] && target != null && !(move == Move.LUNGE && landed)) {
                impact(target, move.damage[Math.min(i, move.damage.length - 1)]);
            }
        }
        if (time >= move.length) {
            end(false);
        }
    }

    /** A blow lands if the prey is within reach and in front; guard, parry and dodge are the damage event's. */
    private void impact(LivingEntity target, float share) {
        Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
        Vec3 facing = Vec3.directionFromRotation(0, mob.yBodyRot);
        boolean inFront = toward.lengthSqr() < 1e-4 || toward.normalize().dot(facing) > 0.35;
        if (mob.distanceTo(target) > move.reach + target.getBbWidth() / 2 || !inFront) {
            return;
        }
        float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * share;
        boolean hurt = target.hurt(mob.damageSources().mobAttack(mob), damage);
        landed = true;
        if (hurt) {
            target.knockback(move == Move.LUNGE ? 0.6 : 0.35, -toward.x, -toward.z);
            mob.setLastHurtMob(target);
        } else {
            blocked++;
        }
    }

    private void end(boolean broken) {
        if (broken) {
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
        }
        if (move == Move.LUNGE) {
            lungeRest = LUNGE_REST;
        }
        move = null;
        rest = REST + mob.getRandom().nextInt(REST_RANDOM);
    }
}
