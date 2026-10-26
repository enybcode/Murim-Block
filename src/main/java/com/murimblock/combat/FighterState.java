package com.murimblock.combat;

/**
 * What a fighter is doing; only one at a time. A state may cut another of lower priority: a blow taken (stunned)
 * cuts everything, a dash cuts a guard or a strike, a guard cuts a strike.
 */
public enum FighterState {
    IDLE(0),
    ATTACK(1),
    GUARD(2),
    DASH(3),
    STUNNED(4);

    private final int priority;

    FighterState(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
