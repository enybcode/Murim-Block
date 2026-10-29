package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * How the skeleton fights as the Archer impérial ({@code docs/mobs/archer-imperial.png}, storyboard
 * {@code docs/mobs/boards/archer.png}), timed on its clips ({@code skeletal/animations/archer}): the aimed shot (an
 * arrow from the quiver, nock, draw, the drawn string held 13 ticks as the signal, loose), the volley of two arrows
 * lobbed over the cover its prey hides behind, the side-stepping retreat when its prey comes within 6 blocks and the
 * bow bash when it is touching it. It runs ahead of vanilla's bow goal while the skeleton holds a bow; every client
 * sees the move it chose.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class ArcherFighting extends Goal {
    /** A move: its clip, length (ticks) and the ticks something happens (an arrow leaves, the bash lands). */
    public enum Move {
        SHOT("shot", 40, 32),
        VOLLEY("volley", 46, 24, 38),
        BASH("bash", 24, 9);

        final ResourceLocation clip;
        final int length;
        final int[] at;

        Move(String clip, int length, int... at) {
            this.clip = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "archer/" + clip);
            this.length = length;
            this.at = at;
        }
    }

    private static final ResourceLocation RETREAT = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,
            "archer/retreat");
    /** Shooting range, the distance it backs away under, the distance it stops backing at, bash reach (blocks). */
    private static final double RANGE = 16, TOO_CLOSE = 6, SAFE = 8, BASH_REACH = 2.4;
    /** The drawn string shows from this tick of the shot (the bow's pulling texture). */
    private static final int DRAW_FROM = 13, VOLLEY_DRAW_FROM = 13;
    /** Ticks between moves, and how long a hidden prey is still worth a volley. */
    private static final int REST = 20, REST_RANDOM = 20, MEMORY = 100;

    /** Dev: told of each move an archer starts. */
    public static java.util.function.BiConsumer<AbstractSkeleton, Move> probe;

    private final AbstractSkeleton mob;
    private Move move;
    private int time, rest = 20, unseen, retreatPath;
    private boolean retreating;
    private Vec3 lastSeen;

    ArcherFighting(AbstractSkeleton mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** The skeleton and the stray (the Esprit de l'Archer) fight as the archer; wither skeletons keep vanilla's ways. */
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getType() != EntityType.SKELETON
                && event.getEntity().getType() != EntityType.STRAY
                || !(event.getEntity() instanceof AbstractSkeleton skeleton)) {
            return;
        }
        boolean installed = skeleton.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof ArcherFighting);
        if (!installed) {
            // Ahead of vanilla's bow goal (priority 4), which then waits while this one runs.
            skeleton.goalSelector.addGoal(3, new ArcherFighting(skeleton));
        }
    }

    private boolean armed() {
        return mob.getMainHandItem().getItem() instanceof BowItem;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return armed() && target != null && target.isAlive() && mob.canAttack(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || move != null;
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        unseen = 0;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        mob.stopUsingItem();
        if (move != null) {
            end(true);
        }
        stopRetreat();
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
        boolean sees = mob.getSensing().hasLineOfSight(target);
        if (sees) {
            unseen = 0;
            lastSeen = target.position();
        } else {
            unseen++;
        }
        mob.getLookControl().setLookAt(target, 30, 30);
        double distance = mob.distanceTo(target);
        rest--;
        if (distance < BASH_REACH && rest <= 0) {
            stopRetreat();
            begin(Move.BASH);
            return;
        }
        // Too close: it backs away, facing its prey, until it has room again.
        if (distance < TOO_CLOSE || retreating && distance < SAFE) {
            retreat(target);
            return;
        }
        stopRetreat();
        if (distance > RANGE || !sees && unseen > MEMORY) {
            if (mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(target, 1);
            }
            return;
        }
        mob.getNavigation().stop();
        if (rest > 0) {
            return;
        }
        if (sees) {
            begin(Move.SHOT);
        } else if (lastSeen != null) {
            begin(Move.VOLLEY);
        }
    }

    private void retreat(LivingEntity target) {
        if (!retreating) {
            retreating = true;
            CombatServer.anim(mob, Animator.Priority.MIDDLE, RETREAT, false);
            retreatPath = 0;
        }
        if (--retreatPath <= 0) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, 8, 4, target.position());
            if (away != null) {
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1);
            }
            retreatPath = 15;
        }
        // Backing away, it keeps its face (and bow) to its prey.
        mob.getLookControl().setLookAt(target, 30, 30);
        mob.setYRot(mob.getYHeadRot());
        mob.yBodyRot = mob.getYHeadRot();
    }

    private void stopRetreat() {
        if (retreating) {
            retreating = false;
            mob.getNavigation().stop();
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
        }
    }

    private void begin(Move next) {
        move = next;
        time = 0;
        mob.getNavigation().stop();
        CombatServer.anim(mob, Animator.Priority.MIDDLE, next.clip, false);
        if (probe != null) {
            probe.accept(mob, next);
        }
    }

    private void play(LivingEntity target) {
        time++;
        if (target != null) {
            if (move == Move.VOLLEY && lastSeen != null) {
                mob.getLookControl().setLookAt(lastSeen.x, lastSeen.y + 1, lastSeen.z, 30, 30);
            } else {
                mob.getLookControl().setLookAt(target, 30, 30);
            }
            mob.setYRot(mob.getYHeadRot());
            mob.yBodyRot = mob.getYHeadRot();
        }
        // The bow's drawn texture while the string is pulled back (vanilla shows it while the item is in use).
        if (move != Move.BASH && (time == DRAW_FROM || move == Move.VOLLEY && time == VOLLEY_DRAW_FROM + 15)) {
            mob.startUsingItem(InteractionHand.MAIN_HAND);
        }
        for (int at : move.at) {
            if (time == at && target != null) {
                act(target);
            }
        }
        if (time >= move.length) {
            end(false);
        }
    }

    private void act(LivingEntity target) {
        switch (move) {
            case SHOT -> {
                mob.stopUsingItem();
                mob.performRangedAttack(target, BowItem.getPowerForTime(20));
            }
            case VOLLEY -> {
                mob.stopUsingItem();
                lob(lastSeen == null ? target.position() : lastSeen);
            }
            case BASH -> {
                Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
                if (mob.distanceTo(target) <= BASH_REACH + target.getBbWidth() / 2) {
                    float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    if (target.hurt(mob.damageSources().mobAttack(mob), damage)) {
                        target.knockback(0.6, -toward.x, -toward.z);
                    }
                }
            }
        }
    }

    /** An arrow lobbed high so it falls on {@code at}: over the cover its prey hides behind. */
    private void lob(Vec3 at) {
        ItemStack bow = mob.getMainHandItem();
        AbstractArrow arrow = ProjectileUtil.getMobArrow(mob, new ItemStack(Items.ARROW), 1, bow);
        double dx = at.x - mob.getX(), dz = at.z - mob.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        // About 45° up, a little faster the further it is; the second arrow lands a little further.
        double spread = 1 + 0.12 * (time > 30 ? 1 : 0);
        arrow.shoot(dx * spread, at.y - arrow.getY() + flat * spread, dz * spread,
                (float) (0.6 + flat * 0.045), 6);
        if (mob.getType() == EntityType.STRAY && arrow instanceof net.minecraft.world.entity.projectile.Arrow tipped) {
            // The Esprit's arrows of ice, as vanilla's stray (Stray.getArrow).
            tipped.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 600));
        }
        mob.playSound(SoundEvents.SKELETON_SHOOT, 1, 1 / (mob.getRandom().nextFloat() * 0.4f + 0.8f));
        mob.level().addFreshEntity(arrow);
    }

    private void end(boolean broken) {
        mob.stopUsingItem();
        if (broken) {
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
        }
        move = null;
        rest = REST + mob.getRandom().nextInt(REST_RANDOM);
    }
}
