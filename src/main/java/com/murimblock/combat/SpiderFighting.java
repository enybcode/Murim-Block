package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * How the spider fights as the Fileuse de linceuls ({@code docs/mobs/fileuse-linceuls.png}, storyboard
 * {@code docs/mobs/boards/spider.png}), timed on its clips ({@code skeletal/animations/spider}): from 4 to 10 blocks
 * it rears up, holds 9 ticks and spits a ball of silk that slows its prey 3 seconds; on a slowed prey 3 to 6 blocks off
 * it leaps; at touch it bites. It replaces vanilla's spider attack and leap (it still only hunts when vanilla's spider
 * would, in the dark or once struck); every client sees the move it chose. The cave spider fights the same as the Bête
 * des Cinq Venins ({@code docs/mobs/bete-cinq-venins.png}), with venom for silk: its spit and its bite poison, and it
 * leaps on a poisoned prey.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class SpiderFighting extends Goal {
    /** A move: its clip, length (ticks) and the tick it acts (the silk leaves, the leap starts, the fangs close). */
    public enum Move {
        SPIT("spit", 30, 18),
        BITE("bite", 16, 7),
        LEAP("leap", 24, 7);

        final ResourceLocation clip;
        final int length, at;

        Move(String clip, int length, int at) {
            this.clip = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "spider/" + clip);
            this.length = length;
            this.at = at;
        }
    }

    /** The tag on its silk, which slows whoever it hits. */
    private static final String WEB = "murimblock_web";
    /** The tag on the Bête des Cinq Venins' spit, which poisons whoever it hits. */
    private static final String VENOM = "murimblock_venom";
    private static final double SPIT_FROM = 4, SPIT_UNTIL = 10, LEAP_FROM = 3, LEAP_UNTIL = 6, BITE_REACH = 1.9;
    private static final int REST = 16, REST_RANDOM = 16, SPIT_REST = 90, SLOW_TICKS = 60;
    /** The leap lands this many ticks after it starts; the bite at the landing. */
    private static final int LEAP_BITE = 14;

    /** Dev: told of each move a spider starts. */
    public static java.util.function.BiConsumer<Spider, Move> probe;

    private final Spider mob;
    /** A cave spider: venom for silk. */
    private final boolean venomous;
    private Move move;
    private int time, rest = 10, spitRest, path;
    /** Where it stands while it rears, spits or bites. */
    private Vec3 anchor;

    SpiderFighting(Spider mob) {
        this.mob = mob;
        venomous = mob.getType() == EntityType.CAVE_SPIDER;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** The spider fights as the Fileuse, the cave spider as the Bête des Cinq Venins. */
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getType() != EntityType.SPIDER
                && event.getEntity().getType() != EntityType.CAVE_SPIDER
                || !(event.getEntity() instanceof Spider spider)) {
            return;
        }
        boolean installed = spider.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof SpiderFighting);
        if (!installed) {
            spider.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal || goal instanceof LeapAtTargetGoal);
            spider.goalSelector.addGoal(3, new SpiderFighting(spider));
        }
    }

    /**
     * The Bête des Cinq Venins' sac bursts as it dies (docs/mobs/boards/venins_death.png): it leaves a puddle of
     * venom, a cloud of Poison on the ground from the burst (its tick 10) for 5 s.
     */
    @SubscribeEvent
    public static void died(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().getType() != EntityType.CAVE_SPIDER || event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity spider = event.getEntity();
        var cloud = new net.minecraft.world.entity.AreaEffectCloud(spider.level(), spider.getX(), spider.getY(),
                spider.getZ());
        cloud.setOwner(spider);
        cloud.setRadius(1.2f);
        cloud.setWaitTime(10);
        cloud.setDuration(100);
        cloud.setRadiusPerTick(0);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, SLOW_TICKS, 0));
        spider.level().addFreshEntity(cloud);
    }

    /** Its silk slows what it hits; the venom poisons it. */
    @SubscribeEvent
    public static void web(ProjectileImpactEvent event) {
        if (event.getProjectile().getTags().contains(WEB) && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target && !target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 2));
        } else if (event.getProjectile().getTags().contains(VENOM)
                && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target && !target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, SLOW_TICKS, 0));
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
        boolean sees = mob.getSensing().hasLineOfSight(target);
        rest--;
        spitRest--;
        if (rest <= 0 && sees) {
            // Slowed by its silk, or poisoned by its venom.
            boolean slowed = target.hasEffect(venomous ? MobEffects.POISON : MobEffects.MOVEMENT_SLOWDOWN);
            if (distance < BITE_REACH + target.getBbWidth() / 2) {
                begin(Move.BITE);
                return;
            }
            if (slowed && distance >= LEAP_FROM && distance <= LEAP_UNTIL && mob.onGround()) {
                begin(Move.LEAP);
                return;
            }
            if (!slowed && spitRest <= 0 && distance >= SPIT_FROM && distance <= SPIT_UNTIL) {
                begin(Move.SPIT);
                return;
            }
        }
        if (--path <= 0) {
            mob.getNavigation().moveTo(target, 1);
            path = 8;
        }
    }

    private void begin(Move next) {
        move = next;
        time = 0;
        anchor = mob.position();
        mob.getNavigation().stop();
        CombatServer.anim(mob, Animator.Priority.MIDDLE, next.clip, false);
        if (probe != null) {
            probe.accept(mob, next);
        }
    }

    private void play(LivingEntity target) {
        time++;
        if (move != Move.LEAP || time < move.at) {
            // Rearing, spitting or biting, it stands: stopping the path leaves the move control walking on to its last
            // waypoint, so it is held too (the leap's jump is let go).
            hold();
        }
        if (target != null && time <= move.at) {
            mob.getLookControl().setLookAt(target, 30, 30);
            mob.setYRot(mob.getYHeadRot());
            mob.yBodyRot = mob.getYHeadRot();
        }
        if (target != null) {
            if (time == move.at) {
                switch (move) {
                    case SPIT -> spit(target);
                    case BITE -> bite(target);
                    case LEAP -> {
                        Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
                        Vec3 push = toward.normalize().scale(Math.min(0.9, toward.length() * 0.17));
                        mob.setDeltaMovement(push.x, 0.42, push.z);
                        mob.hasImpulse = true;
                    }
                }
            }
            if (move == Move.LEAP && time == LEAP_BITE) {
                bite(target);
            }
        }
        if (time >= move.length) {
            end(false);
        }
    }

    private void hold() {
        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0);
        mob.setSpeed(0);
        mob.setZza(0);
        mob.setXxa(0);
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
        // Something in the move control still walks it on a little each tick: it is put back where it stood.
        if (anchor != null) {
            mob.setPos(anchor.x, mob.getY(), anchor.z);
        }
    }

    private void spit(LivingEntity target) {
        Snowball silk = new Snowball(mob.level(), mob);
        silk.setItem(new ItemStack(venomous ? Items.SLIME_BALL : Items.COBWEB));
        silk.addTag(venomous ? VENOM : WEB);
        silk.setPos(mob.getX(), mob.getEyeY() - 0.1, mob.getZ());
        double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
        double dy = target.getY(0.5) - silk.getY();
        silk.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.12, dz, 1.2f, 2);
        mob.playSound(SoundEvents.SPIDER_AMBIENT, 1, 0.6f);
        mob.level().addFreshEntity(silk);
        spitRest = SPIT_REST;
    }

    /** The fangs close: a hit if the prey is within reach and in front (guard, parry and dodge apply). */
    private void bite(LivingEntity target) {
        Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
        Vec3 facing = Vec3.directionFromRotation(0, mob.yBodyRot);
        boolean inFront = toward.lengthSqr() < 1e-4 || toward.normalize().dot(facing) > 0.3;
        if (mob.distanceTo(target) > BITE_REACH + 0.6 + target.getBbWidth() / 2 || !inFront) {
            return;
        }
        float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (target.hurt(mob.damageSources().mobAttack(mob), damage)) {
            mob.setLastHurtMob(target);
            int poison = switch (mob.level().getDifficulty()) {
                case NORMAL -> 7;
                case HARD -> 15;
                default -> 0;
            };
            if (venomous && poison > 0) {
                // Vanilla's cave spider bite (CaveSpider.doHurtTarget).
                target.addEffect(new MobEffectInstance(MobEffects.POISON, poison * 20, 0), mob);
            }
        }
    }

    private void end(boolean broken) {
        if (broken) {
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
        }
        move = null;
        rest = REST + mob.getRandom().nextInt(REST_RANDOM);
    }
}
