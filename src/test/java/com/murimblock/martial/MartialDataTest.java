package com.murimblock.martial;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Azure Cloud Sword Art's rules on a player's martial data: main style, M1 slots, secret, quests, mastery. */
class MartialDataTest {
    private static final MartialArts.Art ART = MartialArts.art(MartialArts.AZURE_CLOUD_SWORD);

    private static MartialData student() {
        return MartialData.EMPTY.learn(ART);
    }

    @Test
    void theFirstArtLearnedBecomesTheMainStyleWithTheDefaultStrikes() {
        MartialData d = student();
        assertEquals(Optional.of(MartialArts.AZURE_CLOUD_SWORD), d.mainArt());
        assertEquals(Optional.of(MartialArts.FLOWING_CLOUD_STANCE), d.passive());
        assertEquals(MartialArts.FLOWING_CLOUD_STANCE.defaults(), d.strikes(MartialArts.FLOWING_CLOUD_STANCE));
        assertTrue(d.strikes().isEmpty(), "the defaults are not written until changed");
    }

    @Test
    void theFourthStrikeAlwaysEndsTheChain() {
        MartialData d = student();
        // A launcher in the fourth slot: allowed.
        MartialData rising = d.withStrike(3, MartialArts.RISING_CLOUD.id());
        assertEquals(MartialArts.RISING_CLOUD.id(), rising.strikes(MartialArts.FLOWING_CLOUD_STANCE).get(3));
        // An opener in the fourth slot: refused, unchanged.
        assertSame(d, d.withStrike(3, MartialArts.PIERCING_MIST.id()));
        // Moving the finisher to the first slot would put an opener last: refused.
        assertSame(d, d.withStrike(0, MartialArts.FALLING_RAIN.id()));
        // A strike already placed swaps with the one in the target slot.
        MartialData swapped = d.withStrike(0, MartialArts.PIERCING_MIST.id());
        assertEquals(List.of(MartialArts.PIERCING_MIST.id(), MartialArts.RETURNING_CUT.id(), MartialArts.DRIFTING_CUT.id(), MartialArts.FALLING_RAIN.id()),
                swapped.strikes(MartialArts.FLOWING_CLOUD_STANCE));
        // A strike of no passive, or without a main style: refused.
        assertSame(d, d.withStrike(1, MartialArts.CLOUD_STEP_SLASH.id()));
        assertSame(MartialData.EMPTY, MartialData.EMPTY.withStrike(0, MartialArts.WIND_STEP.id()));
    }

    @Test
    void aSecretTechniqueNeverGoesOnTheBar() {
        MartialData d = student().unlock(MartialArts.SWALLOW_RETURNS);
        assertTrue(d.hasTechnique(MartialArts.SWALLOW_RETURNS.id()));
        assertFalse(d.bar().contains(Optional.of(MartialArts.SWALLOW_RETURNS.id())));
        assertSame(d, d.assign(4, MartialArts.SWALLOW_RETURNS.id()));
    }

    @Test
    void forgettingTheArtForgetsEverythingOfIt() {
        MartialData d = student().receivedManual(MartialArts.AZURE_CLOUD_SWORD)
                .withStrike(3, MartialArts.RISING_CLOUD.id())
                .withMastery(MartialArts.FLOWING_CLOUD_STANCE.id(), 12)
                .withProgress(MartialArts.PATIENT_BLADE_PARRIES, 4)
                .withProgress(MartialArts.STANCE_HITS, 3)
                .unlock(MartialArts.SWALLOW_RETURNS);
        assertEquals(MartialData.EMPTY, d.forgetArt(ART));
    }

    @Test
    void oldSavesLoadAndNewDataRoundTrips() {
        // A save from the first slice: no main style, strikes or progress.
        String old = "{\"arts\":[\"murimblock:azure_cloud_sword\"],\"techniques\":[\"murimblock:cloud_step_slash\"],"
                + "\"bar\":[\"murimblock:cloud_step_slash\"],\"mastery\":{\"murimblock:cloud_step_slash\":7}}";
        MartialData loaded = MartialData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(old)).getOrThrow();
        assertTrue(loaded.knowsArt(MartialArts.AZURE_CLOUD_SWORD));
        assertEquals(7, loaded.mastery(MartialArts.CLOUD_STEP_SLASH.id()));
        assertTrue(loaded.mainArt().isEmpty());
        MartialData full = student().withStrike(3, MartialArts.RISING_CLOUD.id()).withProgress(MartialArts.PATIENT_BLADE_PARRIES, 6);
        var json = MartialData.CODEC.encodeStart(JsonOps.INSTANCE, full).getOrThrow();
        assertEquals(full, MartialData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void questStepsCountOnlyInOrderAndUnlockTheirTechnique() {
        MartialData d = student();
        // The dash hits do not count before the parries are done.
        MartialProgress.Advance early = MartialProgress.advance(d, ART, MartialArts.PATIENT_BLADE_DASH_HITS, 3);
        assertFalse(early.moved());
        // The second quest waits for the first.
        assertFalse(MartialProgress.advance(d, ART, MartialArts.SEA_OF_CLOUDS_BELL, 1).moved());
        MartialProgress.Advance parries = MartialProgress.advance(d, ART, MartialArts.PATIENT_BLADE_PARRIES, 25);
        assertEquals(10, parries.data().progress(MartialArts.PATIENT_BLADE_PARRIES), "capped at the goal");
        assertNull(parries.unlocked());
        MartialProgress.Advance dash = MartialProgress.advance(parries.data(), ART, MartialArts.PATIENT_BLADE_DASH_HITS, 5);
        assertEquals(MartialArts.THOUSAND_DRIZZLE, dash.unlocked());
        assertTrue(dash.data().hasTechnique(MartialArts.THOUSAND_DRIZZLE.id()));
        assertEquals(MartialArts.ABOVE_THE_SEA_OF_CLOUDS, MartialProgress.currentQuest(dash.data(), ART));
        MartialProgress.Advance bell = MartialProgress.advance(dash.data(), ART, MartialArts.SEA_OF_CLOUDS_BELL, 1);
        MartialProgress.Advance combos = MartialProgress.advance(bell.data(), ART, MartialArts.SEA_OF_CLOUDS_COMBOS, 3);
        assertEquals(MartialArts.HEAVEN_SPLITTING_CRESCENT, combos.unlocked());
        assertNull(MartialProgress.currentQuest(combos.data(), ART), "no quest left");
    }

    @Test
    void theStyleMasteryIsTheAverageAndTechniquesEvolveWithIt() {
        MartialData d = student().withMastery(MartialArts.FLOWING_CLOUD_STANCE.id(), 60).withMastery(MartialArts.CLOUD_STEP_SLASH.id(), 60);
        // Passive 60, Cloud Step 60, the two locked techniques 0: 30. The secret does not count.
        assertEquals(30, d.styleMastery(ART));
        assertTrue(d.evolved(ART, MartialArts.CLOUD_STEP_SLASH.id()));
        assertFalse(d.evolved(ART, MartialArts.THOUSAND_DRIZZLE.id()), "locked, and the style is below 60");
        MartialData before = student().withMastery(MartialArts.FLOWING_CLOUD_STANCE.id(), 60).withMastery(MartialArts.CLOUD_STEP_SLASH.id(), 59);
        assertEquals(List.of(MartialArts.THOUSAND_LI_CLOUD_STEP), MartialProgress.evolutionsReached(ART, before, d));
        assertEquals(List.of(), MartialProgress.evolutionsReached(ART, d, d.withMastery(MartialArts.CLOUD_STEP_SLASH.id(), 61)));
    }

    @Test
    void theCrescentsLeapTakesAPlayerSevenBlocksUp() {
        double top = MartialCombat.riseAfter(AzureCloudSword.CRESCENT_LEAP, 100);
        assertEquals(AzureCloudSword.CRESCENT_RISE, top, 0.1, "the leap's height");
        assertEquals(top, MartialCombat.riseAfter(AzureCloudSword.CRESCENT_LEAP, AzureCloudSword.CARRY_TICKS), 1e-9,
                "the carry lasts until the top of the leap");
    }

    @Test
    void aComboLosesEightPercentAHitDownToFortyPercent() {
        assertEquals(1.0F, MartialCombat.comboFactor(0));
        assertEquals(0.92F, MartialCombat.comboFactor(1), 1e-6);
        assertEquals(0.6F, MartialCombat.comboFactor(5), 1e-6);
        assertEquals(0.4F, MartialCombat.comboFactor(30), 1e-6);
    }
}
