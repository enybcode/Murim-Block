package com.murimblock.combat;

import net.minecraft.resources.ResourceLocation;

/** One fighter, the same on client (prediction) and server (authority): its timeline and the move it plays. */
public final class Fighter {
    private final StrikeClock clock = new StrikeClock();
    private final ChainTracker chain = new ChainTracker();
    private ResourceLocation moveId;
    private CombatMove move;
    /** Where the eyes looked at the end of the previous tick, so a hit sweeps from there to the current view. */
    private float lastYaw;
    private float lastPitch;
    /** In combat mode the left click always strikes and never mines; out of it, vanilla. */
    private boolean combatMode;

    /** The fighting style in hand when last checked, so a change of weapon can stop what the old one was doing. */
    private ResourceLocation style;

    public ResourceLocation style() {
        return style;
    }

    public void style(ResourceLocation id) {
        style = id;
    }

    public boolean combatMode() {
        return combatMode;
    }

    public void combatMode(boolean on) {
        combatMode = on;
        if (!on) {
            reset();
        }
    }

    public StrikeClock clock() {
        return clock;
    }

    public ResourceLocation moveId() {
        return moveId;
    }

    /** The strike playing, or null when not attacking. */
    public CombatMove move() {
        return clock.state() == FighterState.ATTACK ? move : null;
    }

    public ChainTracker chain() {
        return chain;
    }

    /** Starts {@code strike}, the move at {@code chainIndex} of the style's chain. */
    public void strike(ResourceLocation id, CombatMove strike, int chainIndex) {
        moveId = id;
        move = strike;
        CombatMove.Phases phases = strike.phases();
        clock.strike(phases.windup(), phases.active(), phases.recovery());
        chain.thrown(chainIndex);
    }

    /**
     * A blow taken: the move in progress and the chain stop, and nothing can start for {@code ticks}. False when the
     * move shrugs it off (its {@code armor_ticks}).
     */
    public boolean flinch(int ticks) {
        CombatMove current = move();
        if (current != null && clock.within(current.armorTicks())) {
            return false;
        }
        reset();
        clock.stun(ticks);
        return true;
    }

    public void reset() {
        clock.reset();
        chain.reset();
        moveId = null;
        move = null;
    }

    public float lastYaw() {
        return lastYaw;
    }

    public float lastPitch() {
        return lastPitch;
    }

    public void look(float yaw, float pitch) {
        lastYaw = yaw;
        lastPitch = pitch;
    }
}
