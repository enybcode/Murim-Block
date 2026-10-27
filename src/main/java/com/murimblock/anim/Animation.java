package com.murimblock.anim;

/**
 * A clip with its settings from the clip set's sidecar JSON.
 *
 * @param id         {@code <set>/<clip>}, for example {@code fists/jab}
 * @param loop       whether it starts again at the end
 * @param transition seconds to blend into it from the current pose (used by the animator, step 5)
 * @param mask       the joints it moves (used by the animator's layers, step 5)
 */
public record Animation(String id, Clip clip, boolean loop, float transition, JointMask mask) {
    public static final float DEFAULT_TRANSITION = 0.15f;
}
