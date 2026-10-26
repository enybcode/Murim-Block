package com.murimblock.combat;

import net.minecraft.resources.ResourceLocation;

/** One fighter, the same on client (prediction) and server (authority): its timeline and the move it plays. */
public final class Fighter {
    private final StrikeClock clock = new StrikeClock();
    private ResourceLocation moveId;
    private CombatMove move;
    /** Where the eyes looked at the end of the previous tick, so a hit sweeps from there to the current view. */
    private float lastYaw;
    private float lastPitch;
    /** In combat mode the left click always strikes and never mines; out of it, vanilla. */
    private boolean combatMode;

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

    public void strike(ResourceLocation id, CombatMove strike) {
        moveId = id;
        move = strike;
        CombatMove.Phases phases = strike.phases();
        clock.strike(phases.windup(), phases.active(), phases.recovery());
    }

    public void reset() {
        clock.reset();
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
