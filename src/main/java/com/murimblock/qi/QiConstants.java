package com.murimblock.qi;

/**
 * Central values used by the Qi foundation.
 */
public final class QiConstants {
    public static final double INITIAL_QI = 100.0;
    public static final double INITIAL_QI_MAX = 100.0;

    public static final double PASSIVE_REGENERATION_RATE_PER_MINUTE = 0.01;
    public static final double ACTIVE_CHARGE_REGENERATION_RATE_PER_MINUTE = 0.05;
    public static final double REGENERATION_PER_MINUTE = PASSIVE_REGENERATION_RATE_PER_MINUTE;
    public static final int TICKS_PER_MINUTE = 1_200;
    public static final int REGENERATION_INTERVAL_TICKS = 20;

    private QiConstants() {
    }
}
