package com.murimblock.shenfa;

import java.util.Map;

/**
 * A clip with its settings from the clip set's sidecar JSON.
 *
 * @param id         {@code <set>/<clip>}, for example {@code fists/jab}
 * @param loop       whether it starts again at the end
 * @param transition seconds to blend into it from the current pose, and to fade it out
 * @param mask       the joints it moves on a layer
 * @param masks      other masks by living motion ({@link LivingMotion#key()}), for example the arms only while walking
 * @param additive   laid on top of the pose as a change from its own first frame (breathing, a flinch)
 * @param moveSpeed  for a motion clip, the speed (blocks per second, or climbed per second) it plays at at speed 1,
 *                   so its playback follows the body's real speed and the feet do not slide; 0 when not tied
 */
public record Animation(String id, Clip clip, boolean loop, float transition, JointMask mask,
                        Map<String, JointMask> masks, boolean additive, float moveSpeed) {
    public static final float DEFAULT_TRANSITION = 0.15f;

    public Animation {
        masks = Map.copyOf(masks);
    }

    /** A plain clip: no other masks, not additive, not tied to a speed. */
    public Animation(String id, Clip clip, boolean loop, float transition, JointMask mask) {
        this(id, clip, loop, transition, mask, Map.of(), false, 0);
    }

    /** The joints it moves while the body is in {@code motion}. */
    public JointMask mask(String motion) {
        return masks.getOrDefault(motion, mask);
    }
}
