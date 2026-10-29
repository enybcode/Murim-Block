package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * How the Enderman fights as the Moine du Vide ({@code docs/mobs/moine-vide.png}, storyboard v3
 * {@code docs/mobs/boards/moine.png}), timed on its clips ({@code skeletal/animations/moine}). Slow, heavy and readable:
 * every move has its sign and leaves an open window after it, when he can neither strike nor step away.
 * <ul>
 * <li>Angered, he first tears off his blindfold and rosary (he cannot be hurt until he has).
 * <li>Within 3 blocks, the Void palm (never twice in a row): 7 damage and a 3 block push, then 15 ticks open.
 * <li>From 5 to 10 blocks, the bead volley (every 6 s): the beads fly in a line, a step aside dodges them; 20 ticks
 * open after, his beads away.
 * <li>Beyond 10 blocks, or when vanilla would have him teleport (an arrow, a stare), the Void step (every 8 s): he
 * appears 2 blocks before his prey and readies the same palm.
 * <li>Struck three times in a row, the bead burst pushes away (2 damage), then 20 ticks open.
 * <li>A parried palm staggers him (the combat's parry) and he takes half again as much while he reels.
 * </ul>
 * In melee he never teleports when struck: vanilla's teleports are cancelled while he fights, but in water or rain.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class MoineFighting extends Goal {
    /** A move: its clip, length (ticks) and the tick it acts (the palm lands, the beads leave, the burst). */
    public enum Move {
        ANGER("anger", 28, 13),
        PALM("palm", 40, 14),
        VOLLEY("volley", 46, 15),
        STEP("step", 40, 24),
        BURST("burst", 30, 10),
        STAGGER("stagger", 20, 0);

        final ResourceLocation clip;
        final int length, at;

        Move(String clip, int length, int at) {
            this.clip = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "moine/" + clip);
            this.length = length;
            this.at = at;
        }
    }

    private static final double PALM_REACH = 3.2, VOLLEY_FROM = 5, VOLLEY_UNTIL = 10, STEP_FROM = 10;
    private static final int PALM_REST = 50, VOLLEY_REST = 120, STEP_REST = 160, BURST_REST = 60, REST = 10;
    /** The step arrives on this tick, the palm then lands at Move.STEP.at. */
    private static final int STEP_ARRIVES = 9;
    /** Blows in a row (each within this many ticks of the last) before the burst. */
    private static final int BURST_HITS = 3, IN_A_ROW = 40;
    private static final float PALM_DAMAGE = 7, VOLLEY_DAMAGE = 4, BURST_DAMAGE = 2, STAGGERED = 1.5f;
    /** How fast he walks (vanilla's angry enderman runs). */
    private static final double WALK = 0.55;
    private static final Map<EnderMan, MoineFighting> FIGHTING = new WeakHashMap<>();

    /** Dev: told of each move a monk starts. */
    public static java.util.function.BiConsumer<EnderMan, Move> probe;

    private final EnderMan mob;
    private Move move;
    private int time, rest, palmRest, volleyRest = 40, stepRest = 60, burstRest, path;
    private boolean angered, stunned, wantsStep, volleyHit;
    private int hits;
    private long lastHit;
    private Vec3 anchor, volleyFrom, volleyAim;

    MoineFighting(EnderMan mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** The Enderman fights as the Moine: his goal in place of vanilla's melee, no freezing when looked at. */
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getType() != EntityType.ENDERMAN
                || !(event.getEntity() instanceof EnderMan enderman)) {
            return;
        }
        boolean installed = enderman.goalSelector.getAvailableGoals().stream()
                .anyMatch(goal -> goal.getGoal() instanceof MoineFighting);
        if (!installed) {
            enderman.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal
                    || goal.getClass().getSimpleName().equals("EndermanFreezeWhenLookedAt"));
            enderman.goalSelector.addGoal(2, new MoineFighting(enderman));
        }
    }

    /** While he fights he only teleports by his own step, or out of water and rain; a wish to flee far calls the step. */
    @SubscribeEvent
    public static void teleport(EntityTeleportEvent.EnderEntity event) {
        if (!(event.getEntityLiving() instanceof EnderMan enderman) || enderman.isInWaterRainOrBubble()) {
            return;
        }
        MoineFighting fighting = FIGHTING.get(enderman);
        if (fighting != null && enderman.getTarget() != null) {
            event.setCanceled(true);
            if (enderman.distanceTo(enderman.getTarget()) > 4) {
                fighting.wantsStep = true;
            }
        }
    }

    /** Untouchable while he tears his blindfold off; reeling, he takes half again as much; three blows call the burst. */
    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof EnderMan enderman)) {
            return;
        }
        MoineFighting fighting = FIGHTING.get(enderman);
        if (fighting == null) {
            return;
        }
        if (fighting.move == Move.ANGER && fighting.time < Move.ANGER.at) {
            event.setCanceled(true);
            return;
        }
        CombatState state = CombatServer.existing(enderman);
        if (fighting.move == Move.STAGGER || state != null && state.stunTicks > 0) {
            event.setAmount(event.getAmount() * STAGGERED);
        }
        if (event.getSource().getEntity() instanceof LivingEntity && fighting.move == null) {
            // Blows in his open windows are free: only those taken while he is free to answer count.
            long now = enderman.level().getGameTime();
            fighting.hits = now - fighting.lastHit <= IN_A_ROW ? fighting.hits + 1 : 1;
            fighting.lastHit = now;
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
        hits = 0;
        if (!angered) {
            angered = true;
            begin(Move.ANGER);
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
            // Calmed down: he prays again, and will tear his blindfold off again.
            angered = false;
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
                // Parried or staggered: he reels, his beads drifting low.
                move = null;
                begin(Move.STAGGER);
            }
            stunned = true;
            hold();
            return;
        }
        stunned = false;
        palmRest--;
        volleyRest--;
        stepRest--;
        burstRest--;
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
        if (--rest <= 0) {
            if (hits >= BURST_HITS && burstRest <= 0 && distance < 4) {
                begin(Move.BURST);
                return;
            }
            if (distance <= PALM_REACH + target.getBbWidth() / 2 && palmRest <= 0) {
                begin(Move.PALM);
                return;
            }
            if ((distance > STEP_FROM || wantsStep) && stepRest <= 0) {
                begin(Move.STEP);
                return;
            }
            if (sees && distance >= VOLLEY_FROM && distance <= VOLLEY_UNTIL && volleyRest <= 0) {
                begin(Move.VOLLEY);
                return;
            }
        }
        wantsStep = false;
        if (distance > 2.4) {
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
            case PALM -> palmRest = PALM_REST;
            case VOLLEY -> {
                volleyRest = VOLLEY_REST;
                volleyHit = false;
            }
            case STEP -> {
                stepRest = STEP_REST;
                wantsStep = false;
            }
            case BURST -> {
                burstRest = BURST_REST;
                hits = 0;
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
                case PALM -> {
                    if (time == move.at) {
                        palm(target);
                    }
                }
                case STEP -> {
                    if (time == STEP_ARRIVES) {
                        arrive(target);
                    } else if (time == move.at) {
                        palm(target);
                    }
                }
                case VOLLEY -> volley(target);
                case BURST -> {
                    if (time == move.at) {
                        burst(target);
                    }
                }
                default -> {
                }
            }
        }
        if (time >= move.length) {
            end(false);
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

    /** The palm lands: a hit if the prey is within reach and in front (guard, parry and dodge apply), and a push. */
    private void palm(LivingEntity target) {
        Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
        Vec3 facing = Vec3.directionFromRotation(0, mob.yBodyRot);
        boolean inFront = toward.lengthSqr() < 1e-4 || toward.normalize().dot(facing) > 0.3;
        mob.playSound(SoundEvents.ENDERMAN_SCREAM, 0.8f, 0.6f);
        if (mob.distanceTo(target) > PALM_REACH + 0.6 + target.getBbWidth() / 2 || !inFront) {
            return;
        }
        if (target.hurt(mob.damageSources().mobAttack(mob), PALM_DAMAGE)) {
            mob.setLastHurtMob(target);
            target.knockback(1.3, mob.getX() - target.getX(), mob.getZ() - target.getZ());
            target.hurtMarked = true;
        }
    }

    /** The step: he appears 2 blocks before his prey, on the ground, facing it. */
    private void arrive(LivingEntity target) {
        Vec3 back = mob.position().subtract(target.position()).multiply(1, 0, 1);
        Vec3 side = back.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : back.normalize();
        for (double turn : new double[] {0, 0.6, -0.6, 1.2, -1.2}) {
            Vec3 dir = side.yRot((float) turn);
            Vec3 to = target.position().add(dir.scale(2));
            AABB box = mob.getDimensions(mob.getPose()).makeBoundingBox(to);
            if (mob.level().noCollision(mob, box) && !mob.level().noCollision(mob, box.move(0, -0.2, 0))) {
                mob.level().playSound(null, mob.xo, mob.yo, mob.zo, SoundEvents.ENDERMAN_TELEPORT, mob.getSoundSource(),
                        1, 1);
                mob.teleportTo(to.x, to.y, to.z);
                anchor = mob.position();
                face(target);
                mob.playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
                return;
            }
        }
    }

    /** The volley: aimed when the beads leave, it flies about 0.9 blocks a tick; the first bead to reach the prey hits. */
    private void volley(LivingEntity target) {
        if (time == move.at) {
            volleyFrom = mob.position().add(0, 1.8, 0);
            volleyAim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(volleyFrom).normalize();
            mob.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.6f);
        }
        if (volleyFrom == null || volleyHit || time <= move.at || time > move.at + 12) {
            return;
        }
        Vec3 bead = volleyFrom.add(volleyAim.scale((time - move.at) * 0.9));
        if (target.getBoundingBox().inflate(0.35).contains(bead)) {
            volleyHit = true;
            if (target.hurt(mob.damageSources().mobAttack(mob), VOLLEY_DAMAGE)) {
                mob.setLastHurtMob(target);
            }
        }
    }

    /** The burst: whoever stands within 3.5 blocks is pushed away. */
    private void burst(LivingEntity target) {
        mob.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.7f);
        if (mob.distanceTo(target) <= 3.5 && target.hurt(mob.damageSources().mobAttack(mob), BURST_DAMAGE)) {
            target.knockback(1.6, mob.getX() - target.getX(), mob.getZ() - target.getZ());
            target.hurtMarked = true;
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
}
