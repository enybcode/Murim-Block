package com.murimblock.client;

import com.murimblock.client.MobFaces.Face;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobFacesTest {
    private static int count(int id, int ticks, boolean angry, Face... wanted) {
        int n = 0;
        for (int t = 0; t < ticks; t++) {
            Face f = MobFaces.face(id, t, angry, false, false, false);
            for (Face w : wanted) if (f == w) n++;
        }
        return n;
    }

    @Test
    void aCalmBanditBlinksEveryFewSecondsButNotOften() {
        // A minute: about 17 windows, each with one blink of three ticks (sometimes a second, shorter one).
        int closed = count(7, 1200, false, Face.BLINK);
        assertTrue(closed >= 17 * 3 && closed <= 17 * 5 + 5, "eyes closed for " + closed + " ticks in a minute");
    }

    @Test
    void aCalmBanditGlancesBothWaysAndSometimesMutters() {
        assertTrue(count(7, 2400, false, Face.LEFT) > 0, "never glances left");
        assertTrue(count(7, 2400, false, Face.RIGHT) > 0, "never glances right");
        assertTrue(count(7, 2400, false, Face.MUTTER) > 0, "never mutters");
        // Mostly looking ahead.
        assertTrue(count(7, 2400, false, Face.CALM) > 1000, "looks ahead too rarely");
    }

    @Test
    void inCombatTheBrowsKnot() {
        assertEquals(0, count(7, 1200, true, Face.CALM, Face.LEFT, Face.RIGHT, Face.MUTTER), "a calm face in combat");
        assertTrue(count(7, 1200, true, Face.ANGRY) > 600, "rarely angry in combat");
    }

    @Test
    void aStrikeShoutsABlowShutsTheEyesDeathClosesThem() {
        assertEquals(Face.SHOUT, MobFaces.face(7, 5, true, true, false, false));
        assertEquals(Face.ANGRY_BLINK, MobFaces.face(7, 5, true, true, true, false));
        assertEquals(Face.BLINK, MobFaces.face(7, 5, false, false, false, true));
    }

    @Test
    void twoBanditsDoNotBlinkTogether() {
        int together = 0;
        for (int t = 0; t < 1200; t++)
            if (MobFaces.face(7, t, false, false, false, false) == Face.BLINK && MobFaces.face(8, t, false, false, false, false) == Face.BLINK) together++;
        assertTrue(together < 15, "blink in step " + together + " ticks a minute");
    }
}
