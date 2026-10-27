package com.murimblock.shenfa;

import java.util.List;

/**
 * How a clip moves the body itself: the root joint's travel is taken out of the drawn pose and moves the entity
 * instead, so a lunge carries the body forward and walls stop it. From the sidecar JSON: {@code "root_motion": true},
 * or {@code {"vertical": true, "no_gravity": [[0.1, 0.3]]}}.
 *
 * @param vertical  whether the root's rise and fall moves the body too (a leap); otherwise only along the ground
 * @param noGravity windows (seconds into the clip, from and to) in which the body does not fall
 */
public record RootMotion(boolean vertical, List<float[]> noGravity) {
    public static final RootMotion GROUND = new RootMotion(false, List.of());

    public RootMotion {
        noGravity = List.copyOf(noGravity);
    }

    /** Whether the body hangs at {@code time} seconds into the clip. */
    public boolean noGravityAt(float time) {
        for (float[] window : noGravity) {
            if (time >= window[0] && time <= window[1]) {
                return true;
            }
        }
        return false;
    }
}
