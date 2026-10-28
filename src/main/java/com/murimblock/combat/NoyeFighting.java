package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.TridentItem;

/**
 * How the drowned fights as the Disciple noyé where the disciple's goal does not ({@code docs/mobs/boards/noye.png}):
 * holding a fork (a trident) it throws it like an Olympic javelin thrower, the fork leaving at the clip's release; in
 * the water, empty-handed, it swims up and grabs, the blow landing when its hands close. It takes the place of
 * vanilla's drowned attack goals and keeps their rules: it only hunts what {@link Drowned#okTarget} allows (at night,
 * or prey in the water), throws from up to 10 blocks and waits two seconds between throws. On land and empty-handed
 * the disciple's goal ({@link DiscipleFighting}) fights instead.
 */
public final class NoyeFighting extends Goal {
    private static final ResourceLocation THROW = clip("noye_throw"), GRAB = clip("noye_grab");
    private static final int THROW_LENGTH = 48, RELEASE = 27, GRAB_LENGTH = 18, GRAB_IMPACT = 8;
    private static final double THROW_RANGE = 10, GRAB_RANGE = 2.2, GRAB_REACH = 2.8;
    private static final int THROW_REST = 40, GRAB_REST = 20;

    private final Drowned mob;
    private ResourceLocation move;
    private int time, rest = 20;

    NoyeFighting(Drowned mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static ResourceLocation clip(String name) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "disciple/" + name);
    }

    static boolean armed(Drowned mob) {
        return mob.getMainHandItem().getItem() instanceof TridentItem;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target) && mob.okTarget(target)
                && (armed(mob) || mob.isInWater());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || move != null;
    }

    @Override
    public void start() {
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        if (move != null) {
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
        }
        move = null;
        mob.setAggressive(false);
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30, 30);
        }
        if (move != null) {
            time++;
            if (move == THROW && time == RELEASE && target != null && target.isAlive()) {
                mob.performRangedAttack(target, 1);
            }
            if (move == GRAB && time == GRAB_IMPACT && target != null && mob.distanceTo(target) <= GRAB_REACH) {
                mob.doHurtTarget(target);
            }
            if (time >= (move == THROW ? THROW_LENGTH : GRAB_LENGTH)) {
                rest = move == THROW ? THROW_REST : GRAB_REST;
                move = null;
            }
            return;
        }
        if (target == null) {
            return;
        }
        rest--;
        double distance = mob.distanceTo(target);
        boolean seen = mob.getSensing().hasLineOfSight(target);
        if (armed(mob) && seen && distance <= THROW_RANGE && rest <= 0) {
            begin(THROW);
        } else if (!armed(mob) && mob.isInWater() && distance <= GRAB_RANGE && rest <= 0) {
            begin(GRAB);
        } else if (armed(mob) && seen && distance <= THROW_RANGE) {
            mob.getNavigation().stop();
        } else {
            mob.getNavigation().moveTo(target, 1);
        }
    }

    private void begin(ResourceLocation clip) {
        move = clip;
        time = 0;
        mob.getNavigation().stop();
        CombatServer.anim(mob, Animator.Priority.MIDDLE, clip, false);
    }
}
