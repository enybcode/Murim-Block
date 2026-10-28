package com.murimblock.cultivation;

/**
 * A player's cultivation, attached to the player and saved through {@link CultivationCodecs}. Plain Java so the rules
 * can be unit tested. Meditation, Harmony and the anchor are only kept while the player is online.
 */
public final class Cultivation {
    /** What the owner's client sees: the saved fields plus meditation and Harmony. */
    public record Snapshot(int realm, int stage, double stageQi, double qi, int maxQi, int meridians,
                           boolean meditating, float harmony) {
        public static final Snapshot START = new Cultivation().snapshot();
    }

    private int realm;
    private int stage;
    private double stageQi;
    private double qi;
    private int maxQi;
    private int meridians;

    private boolean meditating;
    private double harmony;
    private int combo;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private boolean dirty = true;
    private int lastBreathTick = Integer.MIN_VALUE / 2;

    public Cultivation() {
        this(0, 1, 0, CultivationRules.START_MAX_QI, CultivationRules.START_MAX_QI, 0);
    }

    Cultivation(int realm, int stage, double stageQi, double qi, int maxQi, int meridians) {
        this.realm = Math.max(0, realm);
        this.stage = Math.clamp(stage, 1, CultivationRules.STAGES);
        this.stageQi = Math.max(0, stageQi);
        this.maxQi = Math.max(1, maxQi);
        this.qi = Math.clamp(qi, 0, this.maxQi);
        this.meridians = Math.clamp(meridians, 0, CultivationRules.MERIDIANS);
    }

    public Snapshot snapshot() {
        return new Snapshot(realm, stage, stageQi, qi, maxQi, meridians, meditating, (float) harmony);
    }

    public int realm() {
        return realm;
    }

    public int stage() {
        return stage;
    }

    public double stageQi() {
        return stageQi;
    }

    public double qi() {
        return qi;
    }

    public int maxQi() {
        return maxQi;
    }

    public int meridians() {
        return meridians;
    }

    public boolean meditating() {
        return meditating;
    }

    public double harmony() {
        return harmony;
    }

    /** Breaths in a row without a miss since the last miss or the start of meditation. */
    public int combo() {
        return combo;
    }

    /** How far a point is from where the player sat down. */
    public double driftFrom(double x, double y, double z) {
        return Math.sqrt((x - anchorX) * (x - anchorX) + (y - anchorY) * (y - anchorY) + (z - anchorZ) * (z - anchorZ));
    }

    public int gauge() {
        return CultivationRules.gauge(realm, stage);
    }

    public boolean gaugeFull() {
        return CultivationRules.gaugeFull(realm, stage, stageQi);
    }

    public int lastBreathTick() {
        return lastBreathTick;
    }

    public boolean canAdvance() {
        return CultivationRules.canAdvance(realm, stage, stageQi);
    }

    /** Rises one stage if the gauge is full and the stage is not the last; returns whether it did. */
    public boolean advance() {
        if (!canAdvance()) {
            return false;
        }
        stage++;
        stageQi = 0;
        maxQi = CultivationRules.maxQiAfterStage(maxQi);
        return changed();
    }

    /** A won trial: the next realm, stage 1, an empty gauge, and the stage's Max Qi bonus. */
    public boolean breakRealm() {
        if (realm >= CultivationRealms.LADDER.size() - 1) {
            return false;
        }
        realm++;
        stage = 1;
        stageQi = 0;
        maxQi = CultivationRules.maxQiAfterStage(maxQi);
        return changed();
    }

    /** Spends {@code amount} Qi (a dodge, a guarded blow); false, and nothing spent, when it is not there. */
    public boolean spend(double amount) {
        if (qi < amount) {
            return false;
        }
        qi -= amount;
        return changed();
    }

    /** Empties the Qi (a broken guard). */
    public void drain() {
        qi = 0;
        changed();
    }

    /** A lost trial: this share of the gathered stage Qi scatters, so the gauge is no longer full. */
    public void scatter(double fraction) {
        stageQi = Math.max(0, stageQi * (1 - fraction));
        changed();
    }

    /** Opens the next meridian if the realm allows it; returns whether it did. */
    public boolean openMeridian() {
        if (meridians >= CultivationRules.meridianCap(realm)) {
            return false;
        }
        meridians++;
        return changed();
    }

    /** Gathers stage Qi (capped at the full gauge) and refills Qi for this many seconds. */
    public void gather(double seconds) {
        if (meditating) {
            stageQi = Math.min(gauge(), stageQi + CultivationRules.meditationGain(harmony, combo) * seconds);
        }
        qi = Math.min(maxQi, qi + (meditating ? CultivationRules.QI_REGEN_MEDITATING : CultivationRules.QI_REGEN) * seconds);
        dirty = true;
    }

    public void startMeditation(double x, double y, double z) {
        meditating = true;
        anchorX = x;
        anchorY = y;
        anchorZ = z;
        harmony = 0;
        combo = 0;
        changed();
    }

    public void stopMeditation() {
        meditating = false;
        harmony = 0;
        combo = 0;
        changed();
    }

    /** A breath of the QTE: perfect and good beats raise Harmony and the combo, a miss lowers Harmony and ends it. */
    public void breathe(BreathResult result, int tick) {
        lastBreathTick = tick;
        harmony = CultivationRules.clamp01(harmony + result.harmony());
        combo = result == BreathResult.MISS ? 0 : combo + 1;
        changed();
    }

    // Operator commands.
    public void set(int realm, int stage, double stageQi, double qi, int maxQi, int meridians) {
        Cultivation c = new Cultivation(realm, stage, stageQi, qi, maxQi, meridians);
        this.realm = c.realm;
        this.stage = c.stage;
        this.stageQi = Math.min(c.stageQi, gauge());
        this.qi = c.qi;
        this.maxQi = c.maxQi;
        this.meridians = c.meridians;
        changed();
    }

    /** Whether something the client shows changed since the last sync; clears the flag. */
    public boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    private boolean changed() {
        dirty = true;
        return true;
    }
}
