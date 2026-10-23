package com.murimblock.client.cultivation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murimblock.client.cultivation.BreathTrack.Beat;
import com.murimblock.client.cultivation.BreathTrack.Kind;
import com.murimblock.client.cultivation.BreathTrack.Word;
import com.murimblock.cultivation.BreathResult;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BreathTrackTest {
    private static BreathTrack track() {
        BreathTrack track = new BreathTrack(new Random(1));
        track.reset(0);
        track.fill(0, 0);
        return track;
    }

    private static Beat first(BreathTrack track, Kind kind) {
        return track.beats().stream().filter(b -> b.kind() == kind).findFirst().orElseThrow();
    }

    @Test
    void patternsAlternateBreathsAndStartFromEmptyLungs() {
        BreathTrack track = track();
        assertEquals(Kind.INHALE, track.beats().getFirst().kind());
        assertEquals(BreathTrack.EMPTY, track.level(0), 1e-6);
        Kind lastBreath = Kind.EXHALE;
        for (Beat b : track.beats()) {
            if (b.kind() != Kind.HOLD) {
                assertTrue(b.kind() != lastBreath, "two inhales or exhales in a row");
                lastBreath = b.kind();
            }
        }
        Beat in = first(track, Kind.INHALE);
        assertEquals(BreathTrack.FULL, track.level(in.end()), 1e-4);
    }

    @Test
    void pressesAreGradedByDistanceToTheMark() {
        BreathTrack track = track();
        Beat in = track.beats().getFirst();
        assertNull(track.press(in.end() - 0.5f));
        assertEquals(BreathResult.PERFECT, track.press(in.end() + 0.05f).result());
        Beat out = track.beats().get(1).kind() == Kind.HOLD ? track.beats().get(2) : track.beats().get(1);
        assertEquals(BreathResult.GOOD, track.press(out.end() - 0.12f).result());
    }

    @Test
    void anEarlyPressMissesAndAnUnpressedMarkExpires() {
        BreathTrack track = track();
        Beat in = track.beats().getFirst();
        BreathTrack.Judgement early = track.press(in.end() - 0.25f);
        assertEquals(BreathResult.MISS, early.result());
        assertEquals(Word.EARLY, early.word());

        BreathTrack other = track();
        Beat mark = other.beats().getFirst();
        var missed = other.expire(mark.end() + BreathTrack.RANGE + 0.01f, false, 0);
        assertEquals(Word.MISSED, missed.getFirst().word());
    }

    @Test
    void holdsNeedTheKeyFromTheStartUntilTheEnd() {
        BreathTrack track = new BreathTrack(new Random(1));
        track.reset(0);
        Beat hold = null;
        for (float t = 0; hold == null; t += 10) {
            track.fill(t, 0);
            hold = track.beats().stream().filter(b -> b.kind() == Kind.HOLD && b.result() == null).findFirst()
                    .orElse(null);
        }
        assertEquals(BreathResult.PERFECT, track.release(hold.end(), hold.start()).result());
    }

    @Test
    void releasingAHoldTooSoonMisses() {
        BreathTrack track = new BreathTrack(new Random(1));
        track.reset(0);
        Beat hold = null;
        for (float t = 0; hold == null; t += 10) {
            track.fill(t, 0);
            hold = track.beats().stream().filter(b -> b.kind() == Kind.HOLD && b.result() == null).findFirst()
                    .orElse(null);
        }
        assertEquals(Word.TOO_SHORT, track.release(hold.start() + 0.1f, hold.start()).word());
    }
}
