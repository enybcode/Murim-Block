package com.murimblock.cultivation;

/**
 * How a breath of the meditation QTE went, judged by the client; each changes Harmony by its own amount. Plain Java,
 * so the rules stay testable; it travels as its ordinal.
 */
public enum BreathResult {
    MISS(CultivationRules.HARMONY_MISS),
    GOOD(CultivationRules.HARMONY_GOOD),
    PERFECT(CultivationRules.HARMONY_PERFECT);

    private final double harmony;

    BreathResult(double harmony) {
        this.harmony = harmony;
    }

    /** The result sent as this number, any out-of-range number read as the nearest end. */
    public static BreathResult byId(int id) {
        return values()[Math.clamp(id, 0, values().length - 1)];
    }

    public double harmony() {
        return harmony;
    }
}
