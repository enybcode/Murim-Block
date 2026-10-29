package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * The slime as the Sceau de jade vivant ({@code docs/mobs/sceau-jade.png}, storyboard {@code docs/mobs/boards/seal.png}):
 * it still hops as vanilla's slime and hurts at touch, and the big and middle ones also strike with the seal's blow.
 * Within reach of its prey it braces 8 ticks, rises over it, hangs there flat while its shadow grows under the prey
 * (ticks 12 to 22: the time to step out), then slams down; whatever is under it is hurt (its attack damage) and, under
 * the big one, slowed 2 s. The client draws the red mark it leaves ({@code SealStamp}).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class SealFighting extends Goal {
    public static final ResourceLocation CLIP = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "seal/strike");
    /** Its braced crouch ends, its rise ends, it drops; the clip's length. */
    public static final int CROUCH = 8, RISE = 12, DROP = 22, LENGTH = 34;
    /** The specks of jade it sheds where vanilla's slime sheds slime. */
    public static final ParticleOptions JADE = new BlockParticleOption(ParticleTypes.BLOCK,
            Blocks.OXIDIZED_COPPER.defaultBlockState());
    /** Whether a slime is drawn as the Sceau (set by the client, which knows its looks): its specks are jade then. */
    public static Predicate<Entity> jade = entity -> false;
    /** Dev: told of each blow a seal starts. */
    public static java.util.function.Consumer<Slime> probe;
    private static final double FROM = 1, UNTIL = 4.5;
    private static final int REST = 50, REST_RANDOM = 40, SLOW_TICKS = 40;

    private final Slime mob;
    private int time = -1, next;
    private boolean landed;
    private Vec3 start, over;

    SealFighting(Slime mob) {
        this.mob = mob;
        next = mob.tickCount + REST;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getType() != EntityType.SLIME
                || !(event.getEntity() instanceof Slime slime)) {
            return;
        }
        if (slime.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof SealFighting)) {
            // Ahead of vanilla's slime attack (2), which keeps hopping it toward its prey between blows.
            slime.goalSelector.addGoal(1, new SealFighting(slime));
        }
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (mob.getSize() < 2 || target == null || !target.isAlive() || !mob.canAttack(target) || !mob.onGround()
                || mob.isInWater() || mob.tickCount < next) {
            return false;
        }
        double distance = target.position().subtract(mob.position()).horizontalDistance();
        return distance >= FROM && distance <= UNTIL + mob.getSize() * 0.25 && Math.abs(target.getY() - mob.getY()) < 2
                && mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return time >= 0 && time < LENGTH && mob.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        time = 0;
        landed = false;
        start = mob.position();
        over = null;
        CombatServer.anim(mob, Animator.Priority.MIDDLE, CLIP, false);
        if (probe != null) {
            probe.accept(mob);
        }
    }

    @Override
    public void stop() {
        mob.setNoGravity(false);
        if (time < LENGTH) {
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
        }
        time = -1;
        next = mob.tickCount + REST + mob.getRandom().nextInt(REST_RANDOM);
    }

    @Override
    public void tick() {
        CombatState state = CombatServer.existing(mob);
        if (state != null && state.stunTicks > 0 && time < CROUCH) {
            time = LENGTH;
            return;
        }
        time++;
        LivingEntity target = mob.getTarget();
        if (time <= CROUCH) {
            hold();
            if (target != null) {
                mob.getLookControl().setLookAt(target, 30, 30);
            }
            return;
        }
        if (time <= DROP) {
            // It rises over its prey, following it until the top, then hangs there.
            mob.setNoGravity(true);
            if (time <= RISE && target != null) {
                over = target.position().add(0, 2.2 + mob.getSize() * 0.35, 0);
            }
            if (over == null) {
                over = start.add(0, 3, 0);
            }
            float rise = Mth.clamp((time - CROUCH) / (float) (RISE - CROUCH), 0, 1);
            Vec3 to = start.lerp(over, 1 - (1 - rise) * (1 - rise));
            mob.setDeltaMovement(to.subtract(mob.position()));
            mob.hasImpulse = true;
            return;
        }
        if (time == DROP + 1) {
            mob.setNoGravity(false);
            mob.setDeltaMovement(0, -1.4, 0);
            mob.hasImpulse = true;
        }
        if (!landed && (mob.onGround() || mob.isInWater() || time >= DROP + 8)) {
            landed = true;
            slam();
        }
        if (landed) {
            hold();
        }
    }

    /** It lands: whatever is under it is struck (guard, parry and dodge apply), slowed under the big one. */
    private void slam() {
        mob.resetFallDistance();
        float damage = (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity victim : mob.level().getEntitiesOfClass(LivingEntity.class,
                mob.getBoundingBox().inflate(0.3, 0.6, 0.3), victim -> victim != mob && !(victim instanceof Slime))) {
            if (victim.hurt(mob.damageSources().mobAttack(mob), damage)) {
                mob.setLastHurtMob(victim);
                if (mob.getSize() >= 4) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1), mob);
                }
            }
        }
    }

    /** It stands where it is: no hop (the move control is set to strafe nowhere), no sliding. */
    private void hold() {
        mob.getMoveControl().strafe(0, 0);
        mob.setJumping(false);
        mob.setZza(0);
        mob.setXxa(0);
        Vec3 motion = mob.getDeltaMovement();
        mob.setDeltaMovement(0, Math.min(motion.y, 0), 0);
    }
}
