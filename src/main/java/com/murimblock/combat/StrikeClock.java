package com.murimblock.combat;

import java.util.HashSet;
import java.util.Set;

/**
 * The timeline of a fighter in ticks, with no game types so it can be unit tested. Each tick the caller reads the
 * phase, acts on it, then calls {@link #advance()}. Hit-stop freezes the timeline.
 */
public final class StrikeClock {
    private FighterState state = FighterState.IDLE;
    private int windup;
    private int active;
    private int length;
    private int tick;
    private int frozen;
    private int stunned;
    private final Set<Integer> struck = new HashSet<>();

    public FighterState state() {
        return state;
    }

    /** Ticks into the strike at {@code partial} of the current tick; hit-stop holds it still. */
    public float time(float partial) {
        return frozen > 0 ? tick : tick + partial;
    }

    /**
     * Whether {@code next} may start now: a strike once the current one is nearly over ({@code slack} ticks of
     * recovery left), anything else when it outranks or equals what is going on.
     */
    public boolean canEnter(FighterState next, int slack) {
        if (next == FighterState.ATTACK) {
            return canStrike(slack);
        }
        return state == FighterState.IDLE || next.priority() >= state.priority();
    }

    /** Whether a new strike may start now, allowing {@code slack} ticks of recovery left (network lag). */
    public boolean canStrike(int slack) {
        return state == FighterState.IDLE || state == FighterState.ATTACK && remaining() <= slack;
    }

    public void strike(int windupTicks, int activeTicks, int recoveryTicks) {
        state = FighterState.ATTACK;
        windup = windupTicks;
        active = activeTicks;
        length = windupTicks + activeTicks + recoveryTicks;
        tick = 0;
        frozen = 0;
        struck.clear();
    }

    public void stun(int ticks) {
        reset();
        state = FighterState.STUNNED;
        stunned = ticks;
    }

    public void reset() {
        state = FighterState.IDLE;
        tick = 0;
        frozen = 0;
        stunned = 0;
        struck.clear();
    }

    /** Whether the strike is still within its first {@code ticks} (a move that shrugs off blows while it winds up). */
    public boolean within(int ticks) {
        return state == FighterState.ATTACK && tick < ticks;
    }

    /** First tick of the strike: the step forward. */
    public boolean starting() {
        return state == FighterState.ATTACK && tick == 0 && frozen == 0;
    }

    /** Whether the blow can land this tick. */
    public boolean active() {
        return state == FighterState.ATTACK && frozen == 0 && tick >= windup && tick < windup + active;
    }

    /** Records a target as struck; false if this strike already hit it. */
    public boolean markStruck(int entityId) {
        return struck.add(entityId);
    }

    public void hitStop(int ticks) {
        frozen = Math.max(frozen, ticks);
    }

    public int remaining() {
        return state == FighterState.ATTACK ? length - tick + frozen : 0;
    }

    public void advance() {
        if (state == FighterState.STUNNED && --stunned <= 0) {
            reset();
        }
        if (state != FighterState.ATTACK) {
            return;
        }
        if (frozen > 0) {
            frozen--;
            return;
        }
        if (++tick >= length) {
            reset();
        }
    }
}
