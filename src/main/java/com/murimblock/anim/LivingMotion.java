package com.murimblock.anim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What the body is doing, which picks the base layer's clip. A motion without a clip falls back to a simpler one. */
public enum LivingMotion {
    IDLE(null),
    WALK(IDLE),
    RUN(WALK),
    SNEAK(IDLE),
    SNEAK_WALK(SNEAK),
    JUMP(IDLE),
    FALL(JUMP),
    SWIM(IDLE),
    CLIMB(IDLE),
    RIDE(IDLE),
    SLEEP(IDLE),
    GLIDE(FALL),
    SPIN(GLIDE);

    private final LivingMotion fallback;

    LivingMotion(LivingMotion fallback) {
        this.fallback = fallback;
    }

    /** The name used in JSON files: {@code idle}, {@code sneak_walk}... */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** This motion, then its fallbacks down to {@link #IDLE}. */
    public List<LivingMotion> chain() {
        List<LivingMotion> chain = new ArrayList<>();
        for (LivingMotion motion = this; motion != null; motion = motion.fallback) {
            chain.add(motion);
        }
        return chain;
    }

    public static LivingMotion byKey(String key) {
        for (LivingMotion motion : values()) {
            if (motion.key().equals(key)) {
                return motion;
            }
        }
        return null;
    }
}
