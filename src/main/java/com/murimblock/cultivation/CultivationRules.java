package com.murimblock.cultivation;

/**
 * The numbers of cultivation, kept apart from Minecraft so they can be tested and tuned. See
 * {@code docs/cultivation/cultivation.md}.
 */
public final class CultivationRules {
    public static final int STAGES = 9;
    public static final int MERIDIANS = 13;
    public static final int START_MAX_QI = 100;
    /** Stage Qi gathered each second of meditation, before Harmony. */
    public static final double MEDITATION_GAIN = 2.0;
    /** Full Harmony multiplies the meditation gain by 1 + this. */
    public static final double HARMONY_BONUS = 1.5;
    /** Qi refilled each second outside meditation, and while meditating. */
    public static final double QI_REGEN = 0.5;
    public static final double QI_REGEN_MEDITATING = 2.5;
    /** Each stage gained adds this share of max Qi. */
    public static final double STAGE_MAX_QI_BONUS = 0.10;
    public static final int MERIDIANS_PER_REALM = 4;

    private CultivationRules() {
    }

    /** Stage Qi needed to fill the gauge of this stage: 100 at Body Tempering 1, x1.25 a stage, x3 a realm. */
    public static int gauge(int realm, int stage) {
        return (int) Math.round(100 * Math.pow(1.25, stage - 1) * Math.pow(3, realm));
    }

    /** Stage Qi gathered per second of meditation for a Harmony between 0 and 1. */
    public static double meditationGain(double harmony) {
        return MEDITATION_GAIN * (1 + HARMONY_BONUS * clamp01(harmony));
    }

    /** Meridians a realm can hold open: four per realm reached, up to all thirteen. */
    public static int meridianCap(int realm) {
        return Math.min(MERIDIANS, MERIDIANS_PER_REALM * (realm + 1));
    }

    /** Meridians the breakthrough trial out of this realm asks for. */
    public static int meridiansForTrial(int realm) {
        return meridianCap(realm);
    }

    public static boolean gaugeFull(int realm, int stage, double stageQi) {
        return stageQi >= gauge(realm, stage);
    }

    /** Whether the player may press Advance: gauge full below the last stage. The last stage needs the trial. */
    public static boolean canAdvance(int realm, int stage, double stageQi) {
        return stage < STAGES && gaugeFull(realm, stage, stageQi);
    }

    public static int maxQiAfterStage(int maxQi) {
        return (int) Math.round(maxQi * (1 + STAGE_MAX_QI_BONUS));
    }

    static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
