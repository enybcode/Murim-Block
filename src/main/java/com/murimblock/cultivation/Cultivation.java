package com.murimblock.cultivation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A player's cultivation, attached to the player and saved through {@link CultivationCodecs}. Plain Java so the rules
 * can be unit tested. Meditation, Calm, the eyes and the anchor are only kept while the player is online.
 */
public final class Cultivation {
    /** What the owner's client sees: the saved fields plus meditation and Calm. */
    public record Snapshot(int realm, int stage, double stageQi, double qi, int maxQi, int meridians,
                           boolean meditating, float calm) {
        public static final Snapshot START = new Cultivation().snapshot();
    }

    private int realm;
    private int stage;
    private double qi;
    /** Grows with meditation up to the stage's peak; how far it is past the stage's floor is the stage gauge. */
    private double maxQi;
    /** Bosses the player has defeated (entity ids): their max Qi bonus is given once. */
    private final Set<String> defeated = new LinkedHashSet<>();
    private int meridians;

    private boolean meditating;
    private double calm;
    private boolean eyesClosed;
    private boolean disturbed;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private boolean dirty = true;

    public Cultivation() {
        this(0, 1, 0, CultivationRules.START_MAX_QI, CultivationRules.START_MAX_QI, 0);
    }

    /**
     * Max Qi is the larger of {@code maxQi} and the stage's floor plus {@code stageQi}: saves from before 31 Oct kept
     * a stage gauge apart from a smaller max Qi, and come back with that gauge turned into max Qi.
     */
    Cultivation(int realm, int stage, double stageQi, double qi, double maxQi, int meridians, List<String> defeated) {
        this(realm, stage, stageQi, qi, maxQi, meridians);
        this.defeated.addAll(defeated);
    }

    Cultivation(int realm, int stage, double stageQi, double qi, double maxQi, int meridians) {
        this.realm = Math.max(0, realm);
        this.stage = Math.clamp(stage, 1, CultivationRules.STAGES);
        this.maxQi = Math.max(maxQi, floor() + Math.clamp(stageQi, 0, gauge()));
        this.qi = Math.clamp(qi, 0, this.maxQi);
        this.meridians = Math.clamp(meridians, 0, CultivationRules.MERIDIANS);
    }

    public Snapshot snapshot() {
        return new Snapshot(realm, stage, stageQi(), qi, maxQi(), meridians, meditating, (float) calm);
    }

    public int realm() {
        return realm;
    }

    public int stage() {
        return stage;
    }

    /** Max Qi gained since the stage's floor, at most the stage's gauge. */
    public double stageQi() {
        return Math.clamp(maxQi - floor(), 0, gauge());
    }

    public double qi() {
        return qi;
    }

    public int maxQi() {
        return (int) maxQi;
    }

    /** Max Qi with the fractions meditation adds (saved). */
    public double maxQiExact() {
        return maxQi;
    }

    public int meridians() {
        return meridians;
    }

    public boolean meditating() {
        return meditating;
    }

    /** How settled the meditation is, 0 to 1: it multiplies the gain. */
    public double calm() {
        return calm;
    }

    public boolean eyesClosed() {
        return eyesClosed;
    }

    /** Whether a hostile is near enough to wear the Calm down (set by the server each second). */
    public boolean disturbed() {
        return disturbed;
    }

    /** How far a point is from where the player sat down. */
    public double driftFrom(double x, double y, double z) {
        return Math.sqrt((x - anchorX) * (x - anchorX) + (y - anchorY) * (y - anchorY) + (z - anchorZ) * (z - anchorZ));
    }

    public int gauge() {
        return CultivationRules.gauge(realm, stage);
    }

    public int floor() {
        return CultivationRules.floor(realm, stage);
    }

    /** Max Qi at which this stage stops growing until the player advances. */
    public int peak() {
        return CultivationRules.peak(realm, stage);
    }

    public boolean gaugeFull() {
        return CultivationRules.gaugeFull(realm, stage, stageQi());
    }

    public boolean canAdvance() {
        return CultivationRules.canAdvance(realm, stage, stageQi());
    }

    /** Rises one stage if max Qi reached the peak and the stage is not the last; max Qi grows on from there. */
    public boolean advance() {
        if (!canAdvance()) {
            return false;
        }
        stage++;
        return changed();
    }

    /** A won trial: the next realm, stage 1, max Qi growing on from the last peak. */
    public boolean breakRealm() {
        if (realm >= CultivationRealms.LADDER.size() - 1) {
            return false;
        }
        realm++;
        stage = 1;
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

    /** A lost trial: this share of the max Qi gained in the stage scatters, so the peak is no longer reached. */
    public void scatter(double fraction) {
        maxQi = floor() + stageQi() * (1 - fraction);
        qi = Math.min(qi, maxQi);
        changed();
    }

    /** Opens the next meridian if the realm allows it; returns whether it did. */
    /** Max Qi from outside meditation (kills, later remedies): it stops at the stage's peak too. Returns what was gained. */
    public double gainMaxQi(double amount) {
        double gained = Math.clamp(peak() - maxQi, 0, Math.max(0, amount));
        maxQi += gained;
        changed();
        return gained;
    }

    public void addQi(double amount) {
        qi = Math.clamp(qi + amount, 0, maxQi);
        changed();
    }

    /** A death: {@link CultivationRules#DEATH_LOSS} of max Qi lost for good, never below the stage's floor. */
    public double die() {
        double before = maxQi;
        maxQi = Math.max(Math.min(maxQi, floor()), maxQi * (1 - CultivationRules.DEATH_LOSS));
        qi = Math.min(qi, maxQi);
        changed();
        return before - maxQi;
    }

    /** Remembers a boss defeated; true the first time, when its one-off max Qi is given. */
    public boolean firstDefeat(String boss) {
        return defeated.add(boss);
    }

    public List<String> defeated() {
        return List.copyOf(defeated);
    }

    public boolean openMeridian() {
        if (meridians >= CultivationRules.meridianCap(realm)) {
            return false;
        }
        meridians++;
        return changed();
    }

    /** Grows max Qi (up to the stage's peak) while meditating and refills Qi, for this many seconds. */
    public void gather(double seconds) {
        if (meditating) {
            if (maxQi < peak()) {
                maxQi = Math.min(peak(), maxQi + CultivationRules.meditationGain(calm) * seconds);
            }
            calm = CultivationRules.calmAfter(calm, seconds, eyesClosed, disturbed);
        }
        qi = Math.min(maxQi, qi + CultivationRules.regen(maxQi, meditating) * seconds);
        dirty = true;
    }

    public void startMeditation(double x, double y, double z) {
        meditating = true;
        anchorX = x;
        anchorY = y;
        anchorZ = z;
        resetCalm();
        changed();
    }

    public void stopMeditation() {
        meditating = false;
        resetCalm();
        changed();
    }

    /** The meditating player closed or opened their eyes; Calm only rises with them closed. */
    public void setEyesClosed(boolean closed) {
        if (meditating) {
            eyesClosed = closed;
        }
    }

    public void setDisturbed(boolean disturbed) {
        this.disturbed = disturbed;
    }

    private void resetCalm() {
        calm = 0;
        eyesClosed = false;
        disturbed = false;
    }

    // Operator commands: max Qi is the larger of maxQi and the stage's floor plus stageQi.
    public void set(int realm, int stage, double stageQi, double qi, double maxQi, int meridians) {
        Cultivation c = new Cultivation(realm, stage, stageQi, qi, maxQi, meridians);
        this.realm = c.realm;
        this.stage = c.stage;
        this.qi = c.qi;
        this.maxQi = c.maxQi;
        this.meridians = c.meridians;
        changed();
    }

    /** Sets the Calm of a meditating player (tests and screenshots). */
    public void setCalm(double calm) {
        if (meditating) {
            this.calm = CultivationRules.clamp01(calm);
            changed();
        }
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
