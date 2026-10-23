package com.murimblock.client.cultivation;

import com.murimblock.cultivation.BreathResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The rhythm of the breathing QTE (validated mockup "Breathing V2", 24 Oct): random patterns of inhales, exhales and
 * holds scroll towards a frame. An inhale or exhale is judged on the press when its mark reaches the frame; a hold is
 * judged on the release at the end of its bar, the key having been held from its start. Times are in seconds.
 */
public final class BreathTrack {
    public static final float SHORT = 1.0f;
    public static final float NORMAL = 1.6f;
    public static final float LONG = 2.6f;
    /** Full Harmony shortens every beat by this share. */
    public static final float TEMPO_AT_FULL_HARMONY = 0.25f;
    public static final float PERFECT_WINDOW = 0.07f;
    public static final float GOOD_WINDOW = 0.16f;
    /** Presses farther than this from any mark are ignored; a mark left unpressed this long is missed. */
    public static final float RANGE = 0.3f;
    /** A hold counts only if the key went down at most this long after its bar started. */
    public static final float HOLD_START = 0.25f;
    /** Lung levels: the patterns start and end with empty lungs. */
    public static final float EMPTY = 0.4f;
    public static final float FULL = 1;
    private static final float LEAD_IN = 1.5f;
    private static final float AHEAD = 6;

    public enum Kind { INHALE, EXHALE, HOLD }

    /** The word the HUD shows for a judgement, as a lang key suffix. */
    public enum Word {
        PERFECT, GOOD, EARLY, LATE, MISSED, TOO_SHORT, TOO_LONG, NOT_HELD;

        public String key() {
            return "hud.murimblock.breath." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Beats as pairs of letters: i(nhale), o(exhale), h(old), then S(hort), N(ormal) or L(ong). */
    private record Pattern(int weight, float minHarmony, String beats) {
    }

    private static final Pattern[] PATTERNS = {
            new Pattern(3, 0, "iNoNiNoN"), // calm
            new Pattern(2, 0, "iNhNoNhS"), // box breathing
            new Pattern(2, 0.2f, "iSoSiSoS"), // quick
            new Pattern(2, 0, "iLhLoL"), // deep
            new Pattern(2, 0.3f, "iShSoL"), // sip
            new Pattern(1, 0.5f, "iSoNiLoS"), // stagger
    };

    public static final class Beat {
        private final Kind kind;
        private final float start;
        private final float end;
        private final float from;
        private final float to;
        private BreathResult result;

        Beat(Kind kind, float start, float end, float from, float to) {
            this.kind = kind;
            this.start = start;
            this.end = end;
            this.from = from;
            this.to = to;
        }

        public Kind kind() {
            return kind;
        }

        public float start() {
            return start;
        }

        public float end() {
            return end;
        }

        /** Null until judged. */
        public BreathResult result() {
            return result;
        }
    }

    public record Judgement(Beat beat, BreathResult result, Word word) {
    }

    private final Random random;
    private final List<Beat> beats = new ArrayList<>();
    private float cursor = LEAD_IN;

    public BreathTrack(Random random) {
        this.random = random;
    }

    public void reset(float time) {
        beats.clear();
        cursor = time + LEAD_IN;
    }

    public List<Beat> beats() {
        return beats;
    }

    /** Adds patterns until the track runs a few seconds past this time, and drops beats long gone. */
    public void fill(float time, float harmony) {
        while (cursor < time + AHEAD) {
            addPattern(harmony);
        }
        beats.removeIf(b -> b.end < time - 5);
    }

    private void addPattern(float harmony) {
        int total = 0;
        for (Pattern p : PATTERNS) {
            total += harmony >= p.minHarmony ? p.weight : 0;
        }
        int roll = random.nextInt(total);
        Pattern pick = PATTERNS[0];
        for (Pattern p : PATTERNS) {
            if (harmony >= p.minHarmony && (roll -= p.weight) < 0) {
                pick = p;
                break;
            }
        }
        float tempo = 1 - TEMPO_AT_FULL_HARMONY * Math.clamp(harmony, 0, 1);
        float level = EMPTY;
        for (int i = 0; i < pick.beats.length(); i += 2) {
            Kind kind = switch (pick.beats.charAt(i)) {
                case 'i' -> Kind.INHALE;
                case 'o' -> Kind.EXHALE;
                default -> Kind.HOLD;
            };
            float length = tempo * switch (pick.beats.charAt(i + 1)) {
                case 'S' -> SHORT;
                case 'L' -> LONG;
                default -> NORMAL;
            };
            float from = level;
            if (kind != Kind.HOLD) {
                level = kind == Kind.INHALE ? FULL : EMPTY;
            }
            beats.add(new Beat(kind, cursor, cursor + length, from, level));
            cursor += length;
        }
    }

    /** The beat running at this time, or null before the first. */
    public Beat current(float time) {
        for (Beat b : beats) {
            if (time >= b.start && time < b.end) {
                return b;
            }
        }
        return null;
    }

    /** How full the lungs are, from {@link #EMPTY} to {@link #FULL}, eased like a breath. */
    public float level(float time) {
        Beat b = current(time);
        if (b == null) {
            return beats.isEmpty() || time < beats.getFirst().start ? EMPTY : beats.getLast().to;
        }
        double x = (time - b.start) / (b.end - b.start);
        return (float) (b.from + (b.to - b.from) * (1 - Math.cos(Math.PI * x)) / 2);
    }

    /** A press of the key: judges the nearest inhale or exhale mark, or returns null if none is close. */
    public Judgement press(float time) {
        Beat best = null;
        for (Beat b : beats) {
            if (b.result == null && b.kind != Kind.HOLD && Math.abs(time - b.end) <= RANGE
                    && (best == null || Math.abs(time - b.end) < Math.abs(time - best.end))) {
                best = b;
            }
        }
        if (best == null) {
            return null;
        }
        float delta = time - best.end;
        return judge(best, grade(delta), delta < 0 ? Word.EARLY : Word.LATE);
    }

    /** A release of the key pressed at downAt: judges the hold running now, or returns null. */
    public Judgement release(float time, float downAt) {
        for (Beat b : beats) {
            if (b.result == null && b.kind == Kind.HOLD && time >= b.start - 0.2f && time <= b.end + RANGE) {
                if (downAt > b.start + HOLD_START) {
                    return judge(b, BreathResult.MISS, Word.NOT_HELD);
                }
                if (time - b.end < -GOOD_WINDOW) {
                    return judge(b, BreathResult.MISS, Word.TOO_SHORT);
                }
                return judge(b, grade(time - b.end), Word.TOO_LONG);
            }
        }
        return null;
    }

    /** Misses every beat whose chance has passed; down and downAt tell whether a hold is still held. */
    public List<Judgement> expire(float time, boolean down, float downAt) {
        List<Judgement> missed = new ArrayList<>();
        for (Beat b : beats) {
            if (b.result == null && time > b.end + RANGE) {
                Word word = b.kind != Kind.HOLD ? Word.MISSED
                        : down && downAt <= b.start + HOLD_START ? Word.TOO_LONG : Word.NOT_HELD;
                missed.add(judge(b, BreathResult.MISS, word));
            }
        }
        return missed;
    }

    private static BreathResult grade(float delta) {
        float a = Math.abs(delta);
        return a <= PERFECT_WINDOW ? BreathResult.PERFECT : a <= GOOD_WINDOW ? BreathResult.GOOD : BreathResult.MISS;
    }

    private static Judgement judge(Beat b, BreathResult result, Word missWord) {
        b.result = result;
        Word word = result == BreathResult.PERFECT ? Word.PERFECT : result == BreathResult.GOOD ? Word.GOOD : missWord;
        return new Judgement(b, result, word);
    }
}
