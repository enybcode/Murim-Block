package com.murimblock.martial;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Long Fist's rules on a player's martial data: its strikeless passive, its secret, its quests and evolutions. */
class LongFistDataTest {
    private static final MartialArts.Art ART = MartialArts.art(MartialArts.LONG_FIST);

    private static MartialData student() {
        return MartialData.EMPTY.learn(ART);
    }

    @Test
    void theScrollTeachesTigerStepAndAPassiveWithoutStrikesToChoose() {
        MartialData d = student();
        assertEquals(Optional.of(MartialArts.LONG_FIST), d.mainArt());
        assertEquals(Optional.of(MartialArts.STEADY_BREATH), d.passive());
        assertTrue(d.hasTechnique(MartialArts.TIGER_STEP_PUNCH.id()));
        assertFalse(d.hasTechnique(MartialArts.RAPID_FISTS.id()));
        assertEquals(Optional.of(MartialArts.TIGER_STEP_PUNCH.id()), d.slot(0));
        // Epic Fight's own bare-hand strikes: none to list, none to place.
        assertTrue(d.strikes(MartialArts.STEADY_BREATH).isEmpty());
        assertSame(d, d.withStrike(0, MartialArts.DRIFTING_CUT.id()));
    }

    @Test
    void inchForceIsASecretThatNeverGoesOnTheBar() {
        assertTrue(MartialArts.INCH_FORCE.secret());
        MartialData d = student().unlock(MartialArts.INCH_FORCE);
        assertTrue(d.hasTechnique(MartialArts.INCH_FORCE.id()));
        assertFalse(d.bar().contains(Optional.of(MartialArts.INCH_FORCE.id())));
        assertSame(d, d.assign(4, MartialArts.INCH_FORCE.id()));
    }

    @Test
    void theQuestsGoInOrderAndUnlockRapidFistsThenWhirlwindKick() {
        MartialData d = student();
        // First Steps: the punches first; Tiger Step's hits do not count before.
        assertFalse(MartialProgress.advance(d, ART, MartialArts.FIRST_STEPS_TIGER_HITS, 1).moved());
        d = MartialProgress.advance(d, ART, MartialArts.FIRST_STEPS_PUNCHES, 30).data();
        assertFalse(MartialProgress.advance(d, ART, MartialArts.UNBROKEN_FLURRY_FULL, 1).moved(), "the second quest counted during the first");
        MartialProgress.Advance a = MartialProgress.advance(d, ART, MartialArts.FIRST_STEPS_TIGER_HITS, 5);
        assertEquals(MartialArts.RAPID_FISTS, a.unlocked());
        d = a.data();
        assertEquals(Optional.of(MartialArts.RAPID_FISTS.id()), d.slot(1));
        // Unbroken Flurry: ten full flurries.
        a = MartialProgress.advance(d, ART, MartialArts.UNBROKEN_FLURRY_FULL, 10);
        assertEquals(MartialArts.WHIRLWIND_KICK, a.unlocked());
        assertEquals(Optional.of(MartialArts.WHIRLWIND_KICK.id()), a.data().slot(2));
        assertNull(MartialProgress.currentQuest(a.data(), ART));
    }

    @Test
    void theStylesMasteryEvolvesItsTechniquesAt30And60And90() {
        MartialData d = student().unlock(MartialArts.RAPID_FISTS).unlock(MartialArts.WHIRLWIND_KICK);
        // The style's mastery: the passive and the three techniques (the secret aside), averaged.
        d = d.withMastery(MartialArts.STEADY_BREATH.id(), 40).withMastery(MartialArts.TIGER_STEP_PUNCH.id(), 40)
                .withMastery(MartialArts.RAPID_FISTS.id(), 20).withMastery(MartialArts.WHIRLWIND_KICK.id(), 20);
        assertEquals(30, d.styleMastery(ART));
        assertTrue(d.evolved(ART, MartialArts.TIGER_STEP_PUNCH.id()), "Tiger Leap at 30");
        assertFalse(d.evolved(ART, MartialArts.RAPID_FISTS.id()));
        d = d.withMastery(MartialArts.RAPID_FISTS.id(), 100).withMastery(MartialArts.WHIRLWIND_KICK.id(), 60);
        assertEquals(60, d.styleMastery(ART));
        assertTrue(d.evolved(ART, MartialArts.RAPID_FISTS.id()), "Hundred Fists at 60");
        assertFalse(d.evolved(ART, MartialArts.WHIRLWIND_KICK.id()));
        d = d.withMastery(MartialArts.STEADY_BREATH.id(), 100).withMastery(MartialArts.TIGER_STEP_PUNCH.id(), 100).withMastery(MartialArts.WHIRLWIND_KICK.id(), 60);
        assertEquals(90, d.styleMastery(ART));
        assertTrue(d.evolved(ART, MartialArts.WHIRLWIND_KICK.id()), "Tornado Kick at 90");
    }

    @Test
    void aSecondArtKeepsTheFirstAsMainStyleUntilChosen() {
        MartialData d = MartialData.EMPTY.learn(MartialArts.art(MartialArts.AZURE_CLOUD_SWORD)).learn(ART);
        assertEquals(Optional.of(MartialArts.AZURE_CLOUD_SWORD), d.mainArt());
        // Both arts' techniques share the bar.
        assertEquals(Optional.of(MartialArts.CLOUD_STEP_SLASH.id()), d.slot(0));
        assertEquals(Optional.of(MartialArts.TIGER_STEP_PUNCH.id()), d.slot(1));
        MartialData fist = d.withMainArt(MartialArts.LONG_FIST);
        assertEquals(Optional.of(MartialArts.STEADY_BREATH), fist.passive());
        assertEquals(MartialArts.FLOWING_CLOUD_STANCE.defaults(), fist.withMainArt(MartialArts.AZURE_CLOUD_SWORD).strikes(MartialArts.FLOWING_CLOUD_STANCE));
    }
}
