package com.murimblock.cultivation;

import com.murimblock.qi.QiConstants;
import java.util.Optional;

/**
 * The rules of cultivation (docs/cultivation/qi-cultivation.md), pure numbers and decisions that the services apply and
 * the tests check: how the reserve comes back, how Qi Max grows (meditation, combat, elixirs, missions), the stage
 * thresholds and the bottleneck before a realm, what a realm gives, what the Heart Demon's strength depends on, what
 * Qi Sense lets a cultivator read of another. Every balance figure lives here (and in {@link CultivationProgression}).
 */
public final class CultivationRules {
    private CultivationRules() {
    }

    // ---------------------------------------------------------------- the reserve

    /** Share of Qi Max the reserve gets back per minute: out of combat mode, in combat mode, meditating. */
    public static final double REGEN_IDLE = 0.05, REGEN_COMBAT = 0.02, REGEN_MEDITATION = 0.40;

    /** Minimum share of Qi Max needed to open the inner world from meditation. */
    public static final double INNER_WORLD_MIN_RESERVE_FRACTION = 0.80;

    // ---------------------------------------------------------------- Qi Max: meditation

    /** Once the reserve is full, meditation condenses this share of Qi Max per minute into Qi Max (before multipliers). */
    public static final double CONDENSATION = 0.012;
    /** Each realm condenses a little less than the one before (later realms take longer). */
    public static final double REALM_EFFICIENCY = 0.6;
    /** Per in-game day (20 minutes), the first ten minutes of condensation count in full, the rest at a quarter. */
    public static final int DAILY_FULL_TICKS = 10 * QiConstants.TICKS_PER_MINUTE;
    public static final double TIRED_FACTOR = 0.25;
    public static final long DAY_TICKS = 24_000L;

    /** Where one meditates: the multiplier of its condensation, and how it weakens the Heart Demon. */
    public enum Place {
        ORDINARY(1.0, 0.0), MURIM_LAND(1.25, 0.10), HIGH(1.5, 0.10), POWER(2.0, 0.20), INNER_WORLD(1.5, 0.0);

        public final double condensation;
        public final double demonWeakening;

        Place(double condensation, double demonWeakening) {
            this.condensation = condensation;
            this.demonWeakening = demonWeakening;
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Height above which meditation counts as "high" (thin air of the peaks). */
    public static final int HIGH_Y = 140;

    public static double realmEfficiency(CultivationRealm realm) {
        return Math.pow(REALM_EFFICIENCY, realm.ordinal());
    }

    /**
     * Qi Max condensed over `ticks` of meditation with a full reserve, `condensedToday` ticks of condensation already
     * done this in-game day.
     */
    public static double condensed(double qiMax, CultivationRealm realm, Place place, int condensedToday, int ticks) {
        int full = Math.max(0, Math.min(ticks, DAILY_FULL_TICKS - condensedToday));
        double weighted = full + (ticks - full) * TIRED_FACTOR;
        return qiMax * CONDENSATION * place.condensation * realmEfficiency(realm) * weighted / QiConstants.TICKS_PER_MINUTE;
    }

    // ---------------------------------------------------------------- Qi Max: combat, elixirs, missions

    /** Share of the Qi spent on techniques, and of a kill's reward, that goes to Qi Max. */
    public static final double TECHNIQUE_SHARE = 0.05, KILL_SHARE = 0.05;

    /** Qi Max given once, the first time a player defeats each boss. */
    public static double firstVictoryQiMax(String entityId) {
        return switch (entityId) {
            case "murimblock:bandit_chief" -> 30;
            case "murimblock:bandit_warlord" -> 40;
            case "murimblock:son_of_the_demon" -> 60;
            case "murimblock:azure_sensei" -> 30;
            case "murimblock:crab_king" -> 30;
            case "minecraft:elder_guardian" -> 100;
            case "minecraft:warden" -> 250;
            case "minecraft:wither" -> 400;
            case "minecraft:ender_dragon" -> 600;
            default -> 0;
        };
    }

    /** The Qi Gathering Pill's Qi Max (before toxicity), and the toxicity each pill adds; toxicity fades 1 a minute. */
    public static final double PILL_QI_MAX = 10, PILL_TOXICITY = 30, TOXICITY_DECAY_PER_MINUTE = 1, TOXICITY_POISON = 100;
    /** Above this toxicity the Heart Demon is stronger. */
    public static final double TOXICITY_HEAVY = 50;
    /** How long a Realm-Breaking Pill helps the next great breakthrough (ticks). */
    public static final int BREAKING_PILL_TICKS = 10 * QiConstants.TICKS_PER_MINUTE;

    /** A pill's effect left by the toxicity already there (0 to 1). */
    public static double pillEfficacy(double toxicity) {
        return Math.max(0, 1 - toxicity / TOXICITY_POISON);
    }

    /** The span of Qi Max the current realm covers (from entering it to the threshold of the next). */
    public static double realmSpan(CultivationRealm realm) {
        CultivationData start = new CultivationData(realm, CultivationStage.EARLY);
        double from = floorOf(start);
        double to = CultivationProgression.getRequiredQiMaxForNext(new CultivationData(realm, CultivationStage.PEAK)).orElse(from * 2);
        return Math.max(1, to - from);
    }

    /** A mission's reward: this share of the current realm's span (so it keeps its weight from realm to realm). */
    public static double missionReward(CultivationRealm realm, double share) {
        return Math.round(realmSpan(realm) * share * 10) / 10.0;
    }

    public static final double STORM_SHARE = 0.03, SPARRING_SHARE = 0.05;

    // ---------------------------------------------------------------- stages and breakthroughs

    /** The Qi Max it took to reach this stage: death and deviation never take a cultivator below it. */
    public static double floorOf(CultivationData data) {
        double floor = QiConstants.INITIAL_QI_MAX;
        CultivationData at = CultivationData.initial();
        while (!at.equals(data)) {
            Optional<Double> required = CultivationProgression.getRequiredQiMaxForNext(at);
            Optional<CultivationData> next = CultivationProgression.getNext(at);
            if (required.isEmpty() || next.isEmpty()) break;
            floor = Math.max(floor, required.get());
            at = next.get();
        }
        return floor;
    }

    /** Whether the next step from here is a great breakthrough (from the Peak of a realm to the next realm). */
    public static boolean nextIsMajor(CultivationData data) {
        return CultivationProgression.getBreakthroughType(data).filter(BreakthroughType.MAJOR::equals).isPresent();
    }

    /** Whether the bottleneck is reached: at the Peak, Qi Max at the threshold of the next realm. */
    public static boolean atBottleneck(CultivationData data, double qiMax) {
        return nextIsMajor(data) && CultivationProgression.getRequiredQiMaxForNext(data).map(r -> qiMax >= r - 1e-6).orElse(false);
    }

    /** What a gain of Qi Max does: the new Qi Max (held at the bottleneck), the stage reached, how many small breakthroughs. */
    public record Growth(double qiMax, CultivationData data, int minorBreakthroughs, double gained) {
    }

    /**
     * Adds Qi Max: every stage threshold crossed is a small breakthrough, at once; before a new realm, Qi Max stops at
     * the threshold (the bottleneck) until the great breakthrough.
     */
    public static Growth grow(CultivationData data, double qiMax, double amount) {
        double target = qiMax + Math.max(0, amount);
        CultivationData at = data;
        int minors = 0;
        while (true) {
            Optional<Double> required = CultivationProgression.getRequiredQiMaxForNext(at);
            if (required.isEmpty()) break;
            if (nextIsMajor(at)) {
                target = Math.min(target, Math.max(qiMax, required.get()));
                break;
            }
            if (target < required.get()) break;
            at = CultivationProgression.getNext(at).orElse(at);
            minors++;
        }
        return new Growth(target, at, minors, target - qiMax);
    }

    /** Qi Max lost at death (a share), never below the stage reached. */
    public static final double DEATH_LOSS = QiConstants.DEATH_QI_MAX_LOSS;
    /** Qi Max lost by a deviation of the Qi (a failed great breakthrough), never below the stage reached. */
    public static final double DEVIATION_LOSS = 0.15;
    /** How long the Qi Deviation lasts (ticks). */
    public static final int DEVIATION_TICKS = 5 * QiConstants.TICKS_PER_MINUTE;

    public static double afterLoss(CultivationData data, double qiMax, double share) {
        return Math.max(Math.min(qiMax, floorOf(data)), qiMax * (1 - share));
    }

    // ---------------------------------------------------------------- the Heart Demon

    /** What weakens or strengthens the Heart Demon of a great breakthrough. */
    public record Preparation(boolean breakingPill, Place place, int failures, double toxicity) {
    }

    /** The Heart Demon's strength (its health and damage, as a share of the cultivator's): 0.4 to 1.3. */
    public static double demonStrength(Preparation p) {
        double s = 1.0;
        if (p.breakingPill()) s -= 0.30;
        s -= p.place().demonWeakening;
        s -= Math.min(0.30, 0.10 * Math.max(0, p.failures()));
        if (p.toxicity() > TOXICITY_HEAVY) s += 0.20;
        return Math.max(0.4, Math.min(1.3, s));
    }

    /** A shadow sparring (a mission): a weaker double, that never costs anything. */
    public static final double SHADOW_STRENGTH = 0.5;

    // ---------------------------------------------------------------- what a realm gives

    /**
     * At the Early stage of each realm (index = realm ordinal): bonus health, damage (share of the total), speed (share),
     * bare-hand damage, armour and armour toughness. Each stage after it brings a quarter of the way to the next realm's.
     */
    static final double[] HEALTH = {0, 4, 8, 12, 16, 24, 32, 40, 50};
    static final double[] DAMAGE = {0, 0.05, 0.10, 0.15, 0.20, 0.30, 0.40, 0.50, 0.60};
    static final double[] SPEED = {0, 0.02, 0.04, 0.06, 0.08, 0.10, 0.12, 0.14, 0.15};
    static final double[] FIST = {0, 1, 2, 3, 4, 6, 8, 10, 12};
    static final double[] ARMOR = {0, 2, 4, 6, 8, 11, 14, 17, 20};
    static final double[] TOUGHNESS = {0, 0, 1, 1, 2, 3, 4, 5, 6};

    public record Bonuses(double health, double damage, double speed, double fist, double armor, double toughness) {
    }

    /** What a realm gives at its Early stage. */
    public static Bonuses bonuses(CultivationRealm realm) {
        return bonuses(new CultivationData(realm, CultivationStage.EARLY));
    }

    /**
     * What a cultivator's body has at their realm and stage: the more their Qi, the more hearts, strength and defence.
     * Between two realms the bonuses grow by quarters (health in whole hearts); the last realm has no stage to grow by.
     */
    public static Bonuses bonuses(CultivationData data) {
        int i = data.realm().ordinal(), n = Math.min(i + 1, HEALTH.length - 1);
        double t = data.stage().ordinal() / 4.0;
        double health = Math.floor((HEALTH[i] + (HEALTH[n] - HEALTH[i]) * t) / 2) * 2;
        return new Bonuses(health, lerp(DAMAGE, i, n, t), lerp(SPEED, i, n, t), lerp(FIST, i, n, t), lerp(ARMOR, i, n, t), lerp(TOUGHNESS, i, n, t));
    }

    private static double lerp(double[] values, int i, int n, double t) {
        return Math.round((values[i] + (values[n] - values[i]) * t) * 1000) / 1000.0;
    }

    // ---------------------------------------------------------------- the weak against the strong

    /** Each stage the attacker stands below the one they strike takes this share off the blow, up to the most. */
    public static final double SHIELD_PER_STAGE = 0.05, SHIELD_MAX = 0.75;

    /** A cultivation as one number: four stages a realm. */
    public static int level(CultivationData data) {
        return data.realm().ordinal() * CultivationStage.values().length + data.stage().ordinal();
    }

    /**
     * How much of a blow a cultivator shrugs off when it comes from someone below them (a player, or a creature with no
     * cultivation at all, counted as the first stage): 5 % a stage, a whole realm 20 %, at most 75 %. Nothing against an
     * equal or a stronger one.
     */
    public static double shield(CultivationData defender, CultivationData attacker) {
        int gap = level(defender) - level(attacker);
        return gap <= 0 ? 0 : Math.min(SHIELD_MAX, gap * SHIELD_PER_STAGE);
    }

    // ---------------------------------------------------------------- Qi Sense

    /** Qi Sense opens at this realm. */
    public static final CultivationRealm SENSE_REALM = CultivationRealm.QI_CONDENSATION;
    /** How far Qi Sense reaches (blocks), and what it costs (Qi per second). */
    public static final double SENSE_RANGE = 16, SENSE_QI_PER_SECOND = 1;

    /** What a cultivator reads of another's aura. */
    public enum Reading {
        /** The seal and the exact stage. */
        CLEAR,
        /** The seal, the stage uncertain (same realm). */
        FUZZY,
        /** The seal smudged: one realm above. */
        VEILED,
        /** Nothing readable, a crushing aura: two realms above or more. */
        ABYSS
    }

    /** The cultivation Qi Sense reads in a creature that is not a player (none: it has no aura to read). */
    public static Optional<CultivationData> creatureRealm(String entityId) {
        return Optional.ofNullable(switch (entityId) {
            case "murimblock:armored_crab" -> new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.EARLY);
            case "murimblock:mountain_bandit" -> new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.MIDDLE);
            case "murimblock:crab_man" -> new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.LATE);
            case "murimblock:bandit_chief" -> new CultivationData(CultivationRealm.QI_GUIDING, CultivationStage.PEAK);
            case "murimblock:bandit_warlord" -> new CultivationData(CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);
            case "murimblock:son_of_the_demon" -> new CultivationData(CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);
            case "murimblock:crab_king" -> new CultivationData(CultivationRealm.QI_CONDENSATION, CultivationStage.LATE);
            case "minecraft:elder_guardian" -> new CultivationData(CultivationRealm.QI_LIQUIDATION, CultivationStage.MIDDLE);
            case "minecraft:warden" -> new CultivationData(CultivationRealm.QI_CRYSTALLIZATION, CultivationStage.PEAK);
            case "minecraft:wither" -> new CultivationData(CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationStage.EARLY);
            case "minecraft:ender_dragon" -> new CultivationData(CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
            default -> null;
        });
    }

    public static Reading reading(CultivationRealm viewer, CultivationRealm target) {
        int gap = target.ordinal() - viewer.ordinal();
        if (gap < 0) return Reading.CLEAR;
        if (gap == 0) return Reading.FUZZY;
        if (gap == 1) return Reading.VEILED;
        return Reading.ABYSS;
    }
}
