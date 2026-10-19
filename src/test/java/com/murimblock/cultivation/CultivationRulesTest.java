package com.murimblock.cultivation;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The rules of cultivation: stages and the bottleneck, losses, the Heart Demon, Qi Sense, and the pace of play. */
class CultivationRulesTest {
    private static CultivationData at(CultivationRealm realm, CultivationStage stage) {
        return new CultivationData(realm, stage);
    }

    @Test
    void qiMaxCrossesStagesByItselfButStopsAtTheGateOfARealm() {
        // The first gain at all is a small breakthrough (Early's threshold, 100, is where everyone starts).
        CultivationRules.Growth g = CultivationRules.grow(CultivationData.initial(), 100, 1);
        assertEquals(at(CultivationRealm.QI_SENSING, CultivationStage.MIDDLE), g.data());
        assertEquals(1, g.minorBreakthroughs());
        // A big gain crosses several stages at once, and stops at the gate of Qi Guiding.
        g = CultivationRules.grow(CultivationData.initial(), 100, 500);
        assertEquals(at(CultivationRealm.QI_SENSING, CultivationStage.PEAK), g.data());
        assertEquals(250, g.qiMax(), 1e-9);
        assertTrue(CultivationRules.atBottleneck(g.data(), g.qiMax()));
        // At the bottleneck, nothing more.
        assertEquals(0, CultivationRules.grow(g.data(), g.qiMax(), 50).gained(), 1e-9);
    }

    @Test
    void theFloorOfAStageIsTheQiMaxItTookToReachIt() {
        assertEquals(100, CultivationRules.floorOf(CultivationData.initial()), 1e-9);
        assertEquals(150, CultivationRules.floorOf(at(CultivationRealm.QI_SENSING, CultivationStage.LATE)), 1e-9);
        assertEquals(250, CultivationRules.floorOf(at(CultivationRealm.QI_GUIDING, CultivationStage.EARLY)), 1e-9);
        // Death and deviation never take a cultivator below it.
        assertEquals(250, CultivationRules.afterLoss(at(CultivationRealm.QI_GUIDING, CultivationStage.EARLY), 260, 0.10), 1e-9);
        assertEquals(400 * 0.9, CultivationRules.afterLoss(at(CultivationRealm.QI_GUIDING, CultivationStage.MIDDLE), 400, 0.10), 1e-9);
        assertEquals(212.5, CultivationRules.afterLoss(at(CultivationRealm.QI_SENSING, CultivationStage.PEAK), 250, CultivationRules.DEVIATION_LOSS), 1e-9);
    }

    @Test
    void meditationCondensesMoreInPlacesOfPowerAndLessOnceTheDayIsSpent() {
        double ordinary = CultivationRules.condensed(200, CultivationRealm.QI_SENSING, CultivationRules.Place.ORDINARY, 0, 1200);
        // 1.2 % of Qi Max a minute, at the first realm's pace.
        assertEquals(200 * 0.012 * CultivationRules.growth(CultivationRealm.QI_SENSING), ordinary, 1e-9);
        assertEquals(ordinary * 2, CultivationRules.condensed(200, CultivationRealm.QI_SENSING, CultivationRules.Place.POWER, 0, 1200), 1e-9);
        // Past the day's ten minutes: a quarter.
        assertEquals(ordinary * 0.25, CultivationRules.condensed(200, CultivationRealm.QI_SENSING, CultivationRules.Place.ORDINARY,
                CultivationRules.DAILY_FULL_TICKS, 1200), 1e-9);
        // Later realms condense less for the same Qi Max.
        assertTrue(CultivationRules.condensed(200, CultivationRealm.QI_GUIDING, CultivationRules.Place.ORDINARY, 0, 1200) < ordinary);
    }

    @Test
    void preparationWeakensTheHeartDemonAndToxicityStrengthensIt() {
        var bare = new CultivationRules.Preparation(false, CultivationRules.Place.ORDINARY, 0, 0);
        assertEquals(1.0, CultivationRules.demonStrength(bare), 1e-9);
        var ready = new CultivationRules.Preparation(true, CultivationRules.Place.POWER, 1, 0);
        assertEquals(0.4, CultivationRules.demonStrength(ready), 1e-9);
        var poisoned = new CultivationRules.Preparation(false, CultivationRules.Place.ORDINARY, 0, 60);
        assertEquals(1.2, CultivationRules.demonStrength(poisoned), 1e-9);
        assertEquals(0.4, CultivationRules.demonStrength(new CultivationRules.Preparation(true, CultivationRules.Place.POWER, 9, 0)), 1e-9);
    }

    @Test
    void pillsLoseTheirEffectWithToxicity() {
        assertEquals(1.0, CultivationRules.pillEfficacy(0), 1e-9);
        assertEquals(0.4, CultivationRules.pillEfficacy(60), 1e-9);
        assertEquals(0.0, CultivationRules.pillEfficacy(120), 1e-9);
    }

    @Test
    void qiSenseReadsWeakerAurasAndNotStrongerOnes() {
        assertEquals(CultivationRules.Reading.CLEAR, CultivationRules.reading(CultivationRealm.QI_CONDENSATION, CultivationRealm.QI_SENSING));
        assertEquals(CultivationRules.Reading.FUZZY, CultivationRules.reading(CultivationRealm.QI_CONDENSATION, CultivationRealm.QI_CONDENSATION));
        assertEquals(CultivationRules.Reading.VEILED, CultivationRules.reading(CultivationRealm.QI_CONDENSATION, CultivationRealm.QI_LIQUIDATION));
        assertEquals(CultivationRules.Reading.ABYSS, CultivationRules.reading(CultivationRealm.QI_CONDENSATION, CultivationRealm.CORE_FORMATION));
    }

    @Test
    void everyRealmGivesMoreThanTheOneBefore() {
        CultivationRules.Bonuses previous = CultivationRules.bonuses(CultivationRealm.QI_SENSING);
        assertEquals(0, previous.health());
        for (CultivationRealm realm : CultivationRealm.values()) {
            if (realm == CultivationRealm.QI_SENSING) continue;
            CultivationRules.Bonuses b = CultivationRules.bonuses(realm);
            assertTrue(b.health() > previous.health() && b.damage() > previous.damage() && b.fist() > previous.fist() && b.armor() > previous.armor(),
                    "not more at " + realm);
            previous = b;
        }
    }

    @Test
    void everyStageGivesAtLeastAsMuchAsTheOneBefore() {
        CultivationRules.Bonuses previous = CultivationRules.bonuses(CultivationData.initial());
        for (CultivationRealm realm : CultivationRealm.values()) {
            for (CultivationStage stage : CultivationStage.values()) {
                CultivationRules.Bonuses b = CultivationRules.bonuses(new CultivationData(realm, stage));
                assertTrue(b.health() >= previous.health() && b.damage() >= previous.damage() && b.armor() >= previous.armor()
                        && b.toughness() >= previous.toughness() && b.fist() >= previous.fist(), "less at " + realm + " " + stage);
                assertEquals(0, b.health() % 2, "health not in whole hearts at " + realm + " " + stage);
                previous = b;
            }
        }
        // Halfway through Qi Guiding (Late): halfway to Qi Condensation's.
        CultivationRules.Bonuses late = CultivationRules.bonuses(new CultivationData(CultivationRealm.QI_GUIDING, CultivationStage.LATE));
        assertEquals(6, late.health());
        assertEquals(3, late.armor(), 1e-9);
        assertEquals(0.075, late.damage(), 1e-9);
    }

    @Test
    void blowsFromBelowLandLighter() {
        CultivationData qiCondensation = new CultivationData(CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
        // A creature with no cultivation counts as the first stage: two realms below, 40 % off.
        assertEquals(0.40, CultivationRules.shield(qiCondensation, CultivationData.initial()), 1e-9);
        assertEquals(0.05, CultivationRules.shield(qiCondensation, new CultivationData(CultivationRealm.QI_GUIDING, CultivationStage.PEAK)), 1e-9);
        assertEquals(0, CultivationRules.shield(qiCondensation, qiCondensation), 1e-9);
        assertEquals(0, CultivationRules.shield(CultivationData.initial(), qiCondensation), 1e-9);
        assertEquals(CultivationRules.SHIELD_MAX, CultivationRules.shield(new CultivationData(CultivationRealm.VOID_ENLIGHTENMENT, CultivationStage.PEAK),
                CultivationData.initial()), 1e-9);
    }

    // ---------------------------------------------------------------- the pace of play

    /**
     * A typical player, minute by minute: each in-game day (20 min) three minutes of meditation in a Murim land (half
     * a minute to fill the reserve), the motes drawn in (`motes`: about four fifths more), 5 Qi of techniques a minute (more with each realm), a kill a minute (better prey
     * with each realm), a pill every hour, one mission every other day, the Bandit Chief beaten at an hour and a half;
     * the Heart Demon beaten at the first try. Returns the minutes it takes to reach each realm.
     */
    static List<Integer> simulate(int realms, double motes) {
        CultivationData data = CultivationData.initial();
        double qiMax = 100;
        int condensedToday = 0;
        List<Integer> reached = new ArrayList<>();
        for (int minute = 1; minute <= 60 * 200 && reached.size() < realms; minute++) {
            int ofDay = minute % 20;
            if (ofDay == 0) condensedToday = 0;
            double gain = 0;
            int r = data.realm().ordinal();
            if (ofDay >= 17 && ofDay < 20) {
                // The first half minute fills the reserve; then it condenses.
                int ticks = ofDay == 17 ? 600 : 1200;
                gain += CultivationRules.condensed(qiMax, data.realm(), CultivationRules.Place.MURIM_LAND, condensedToday, ticks) * motes;
                condensedToday += ticks;
            } else {
                gain += (5 + 2 * r) * CultivationRules.TECHNIQUE_SHARE * CultivationRules.growth(data.realm());
                gain += 5 * (1 + r) * CultivationRules.KILL_SHARE * CultivationRules.growth(data.realm());
            }
            if (minute % 60 == 0) gain += CultivationRules.PILL_QI_MAX;
            if (minute % 40 == 0) gain += CultivationRules.missionReward(data.realm(), CultivationRules.STORM_SHARE);
            if (minute == 90) gain += CultivationRules.firstVictoryQiMax("murimblock:bandit_chief");
            CultivationRules.Growth g = CultivationRules.grow(data, qiMax, gain);
            data = g.data();
            qiMax = g.qiMax();
            if (CultivationRules.atBottleneck(data, qiMax)) {
                data = CultivationProgression.getNext(data).orElseThrow();
                reached.add(minute);
            }
        }
        return reached;
    }

    /**
     * The pace Enzo asked for (17/10/2026), for a player who draws in the motes: Qi Guiding in about half an hour, Qi
     * Condensation in two hours, Qi Liquidation in five, Qi Crystallization in nine, Foundation Establishment in fifteen;
     * each realm longer than the last; a player who lets the motes be is slower, but not by half.
     */
    @Test
    void theRealmsComeAtThePaceAskedForAndEachTakesLonger() {
        List<Integer> reached = simulate(5, 1.8), idle = simulate(5, 1.0);
        System.out.println("Typical player reaches realms 2 to 6 at minutes " + reached + " (letting the motes be: " + idle + ")");
        assertEquals(5, reached.size(), "realms not reached: " + reached);
        int[] asked = {30, 120, 300, 540, 900};
        for (int i = 0; i < asked.length; i++)
            assertTrue(Math.abs(reached.get(i) - asked[i]) <= asked[i] * 0.15, "realm " + (i + 2) + " at " + reached.get(i) + " min, asked " + asked[i] + " (all: " + reached + ")");
        int previous = reached.get(0);
        for (int i = 1; i < reached.size(); i++) {
            int took = reached.get(i) - reached.get(i - 1);
            assertTrue(took > previous, "realm " + (i + 2) + " took " + took + " min, not more than " + previous + " (all: " + reached + ")");
            previous = took;
        }
        for (int i = 0; i < asked.length; i++) assertTrue(idle.get(i) < reached.get(i) * 1.5, "letting the motes be is too slow: " + idle);
    }
}
