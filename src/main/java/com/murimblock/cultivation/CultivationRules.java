package com.murimblock.cultivation;

/**
 * The numbers of cultivation, kept apart from Minecraft so they can be tested and tuned. See
 * {@code docs/cultivation/cultivation.md}.
 */
public final class CultivationRules {
    public static final int STAGES = 9;
    public static final int MERIDIANS = 13;
    public static final int START_MAX_QI = 100;
    /** Max Qi gained each second of meditation, before Calm: very slow, so a realm takes many hours of sitting;
     * killing and remedies will be other ways later. */
    public static final double MEDITATION_GAIN = 0.02;
    /** Full Calm multiplies the meditation gain by 1 + this. */
    public static final double CALM_BONUS = 2.0;
    /** Seconds of closed eyes, undisturbed, from no Calm to full; and seconds a near hostile takes to empty it. */
    public static final double CALM_RISE_SECONDS = 120;
    public static final double CALM_FALL_SECONDS = 30;
    /** A hostile mob this close stops the Calm from rising and wears it down. */
    public static final double CALM_ENEMY_RANGE = 16;
    /** Calm reaching each tier: Steady Breath, Flowing Qi, Still Heart. */
    private static final double[] CALM_TIERS = {0.25, 0.5, 1};
    /** Share of max Qi refilled each second outside meditation, and while meditating. */
    public static final double QI_REGEN = 0.005;
    public static final double QI_REGEN_MEDITATING = 0.025;
    public static final int MERIDIANS_PER_REALM = 4;

    private CultivationRules() {
    }

    /** Max Qi a stage adds, from its floor to its peak: 100 at Body Tempering 1, x1.25 a stage, x3 a realm. */
    public static int gauge(int realm, int stage) {
        return (int) Math.round(100 * Math.pow(1.25, stage - 1) * Math.pow(3, realm));
    }

    /** Max Qi gained per second of meditation for a Calm between 0 and 1: x1 to x3. */
    public static double meditationGain(double calm) {
        return MEDITATION_GAIN * (1 + CALM_BONUS * clamp01(calm));
    }

    /**
     * Calm after {@code seconds} more of meditation: it rises while the eyes are closed and no hostile is near,
     * holds while they are open, and wears down while a hostile is near.
     */
    public static double calmAfter(double calm, double seconds, boolean eyesClosed, boolean disturbed) {
        if (disturbed) {
            return clamp01(calm - seconds / CALM_FALL_SECONDS);
        }
        return eyesClosed ? clamp01(calm + seconds / CALM_RISE_SECONDS) : clamp01(calm);
    }

    /** Calm tier, 0 (none) to 3 (Still Heart). */
    public static int calmTier(double calm) {
        int tier = 0;
        while (tier < CALM_TIERS.length && calm >= CALM_TIERS[tier]) {
            tier++;
        }
        return tier;
    }

    /** Meridians a realm can hold open: four per realm reached, up to all thirteen. */
    public static int meridianCap(int realm) {
        return Math.min(MERIDIANS, MERIDIANS_PER_REALM * (realm + 1));
    }

    /** Meridians the breakthrough trial out of this realm asks for. */
    public static int meridiansForTrial(int realm) {
        return meridianCap(realm);
    }

    /** Whether {@code stageQi}, the max Qi gained since the stage's floor, reached the stage's peak. */
    public static boolean gaugeFull(int realm, int stage, double stageQi) {
        return stageQi >= gauge(realm, stage);
    }

    /** Whether the player may press Advance: gauge full below the last stage. The last stage needs the trial. */
    public static boolean canAdvance(int realm, int stage, double stageQi) {
        return stage < STAGES && gaugeFull(realm, stage, stageQi);
    }

    /** Max Qi a stage starts from: the start, plus what every stage before it added. */
    public static int floor(int realm, int stage) {
        int floor = START_MAX_QI;
        for (int r = 0; r <= realm; r++) {
            for (int s = 1; s <= (r < realm ? STAGES : stage - 1); s++) {
                floor += gauge(r, s);
            }
        }
        return floor;
    }

    /** Max Qi at which a stage stops growing until the player advances: the stage's peak. */
    public static int peak(int realm, int stage) {
        return floor(realm, stage) + gauge(realm, stage);
    }

    /** Qi refilled per second for this max Qi. */
    public static double regen(double maxQi, boolean meditating) {
        return maxQi * (meditating ? QI_REGEN_MEDITATING : QI_REGEN);
    }

    static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
