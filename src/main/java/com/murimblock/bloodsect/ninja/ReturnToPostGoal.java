package com.murimblock.bloodsect.ninja;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

public final class ReturnToPostGoal extends Goal {
    private final BloodNinja ninja;
    private int repath;

    public ReturnToPostGoal(BloodNinja ninja) {
        this.ninja = ninja;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private double distance() {
        return ninja.position().distanceToSqr(ninja.getGuardPost().getBottomCenter());
    }

    @Override public boolean canUse() { return ninja.getTarget() == null && distance() > 10 * 10; }
    @Override public boolean canContinueToUse() { return ninja.getTarget() == null && distance() > 2 * 2; }
    @Override public void start() { repath = 0; }
    @Override public void stop() { ninja.getNavigation().stop(); }

    @Override public void tick() {
        if (--repath <= 0) {
            var post = ninja.getGuardPost();
            ninja.getNavigation().moveTo(post.getX() + .5, post.getY(), post.getZ() + .5, 1.1);
            repath = 10;
        }
    }
}
